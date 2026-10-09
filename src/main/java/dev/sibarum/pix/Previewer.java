package dev.sibarum.pix;

import dev.sibarum.pix.files.Pictures;
import dev.sibarum.pix.files.Preview;
import dev.sibarum.pix.files.Stamp;
import dev.vexelray.gui.core.Gui;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Reads the picture of whatever the document shows, and reads ahead of it.
 *
 * <p>Reading a file is disk work, so it runs on {@link Gui#offload()} and lands through {@link Model#previewed},
 * which drops it if the viewer has moved on. It listens to the model rather than being called by the viewer, so a step
 * from anywhere (a key, the filmstrip, a script) is followed the same way.
 *
 * <p><b>Latest wins.</b> One worker, one slot. Each new step overwrites the slot rather than queueing behind the last,
 * so holding an arrow key through a folder of photographs decodes the one it stops on when the worker comes free, not
 * every one passed on the way. The decode already running is not abandoned (imagelib has no cancel); it lands, is
 * dropped as stale, and the worker goes straight to the newest request.
 *
 * <p><b>Decoded images are kept</b>, least recently used first out, so going back finds them without decoding again.
 * The key is the file's {@link Stamp}, so a file changed since it was decoded is decoded again.
 *
 * <p><b>An idle worker reads ahead</b>: the next image, then the previous, then the one after next, at the viewer's
 * size, so stepping through finds them waiting; then, at the thumbnail's size only, the rest of what the filmstrip
 * shows ({@link #reach}), so its thumbnails fill in. Reading ahead stops the moment a real request arrives, and each
 * image it reads is announced to {@link #onRead}, since no model change says so.
 *
 * <p>Vexplore's previewer, for a window that is only ever the viewer: every picture is wanted at both sizes.
 */
final class Previewer {

    /** Decoded pixels kept for going back: a few photographs at the viewer's size, many thumbnails. */
    static final long BUDGET = 256L * 1024 * 1024;

    private final Gui gui;
    private final Lru<Stamp, Preview> images = new Lru<>(BUDGET, Preview::bytes, p -> { });

    // Guarded by this.
    private Path last;
    private List<Path> lastPictures;
    private Path wanted;
    private boolean running;
    private final Deque<Ahead> ahead = new ArrayDeque<>();

    private final Model model;
    private volatile Consumer<Path> read = p -> { };

    /** A read ahead: an image, and whether it is wanted at the viewer's size or only as a thumbnail. */
    private record Ahead(Path path, boolean big) {
    }

    Previewer(Gui gui, Model model) {
        this.gui = gui;
        this.model = model;
        model.onChange(this::follow);
    }

    /** Who to tell when a read ahead lands, on the worker that read it. One listener: the viewer. */
    void onRead(Consumer<Path> listener) {
        read = listener == null ? p -> { } : listener;
    }

    /**
     * The picture already decoded for {@code path}, or null if there is none or the file has changed since. Reads the
     * file's stamp, so not on the GUI thread.
     */
    Preview kept(Path path) {
        try {
            return images.get(Stamp.of(path));
        } catch (IOException e) {
            return null;
        }
    }

    private void follow(Doc doc) {
        Path want = doc.at();
        List<Ahead> next = new ArrayList<>();
        if (want != null) {
            around(doc.pictures(), want).forEach(p -> next.add(new Ahead(p, true)));
            reach(doc.pictures(), want).forEach(p -> next.add(new Ahead(p, false)));
        }
        synchronized (this) {
            // A picture arriving changes neither; the listing landing changes only who the neighbours are, so the
            // picture already asked for is not asked for again.
            boolean moved = !Objects.equals(want, last);
            if (!moved && doc.pictures() == lastPictures) {
                return;
            }
            last = want;
            lastPictures = doc.pictures();
            ahead.clear();
            ahead.addAll(next);
            if (want == null) {
                wanted = null;
                return;
            }
            if (moved) {
                wanted = want;
            }
            start();
        }
    }

    /** Start the worker if it is not running. Holding this. */
    private void start() {
        if (running || (wanted == null && ahead.isEmpty())) {
            return;
        }
        running = true;
        gui.offload().execute(this::work);
    }

    /** Drain the slot, then read ahead, until there is nothing to do. */
    private void work() {
        try {
            while (true) {
                Path path;
                boolean real;
                boolean big;
                synchronized (this) {
                    path = wanted;
                    wanted = null;
                    real = path != null;
                    big = true;
                    if (!real) {
                        Ahead a = ahead.poll();
                        path = a == null ? null : a.path();
                        big = a != null && a.big();
                    }
                    if (path == null) {
                        running = false;
                        return;
                    }
                }
                Preview p = preview(path, big);
                if (real) {
                    model.previewed(path, p);
                } else {
                    read.accept(path);
                }
            }
        } catch (RuntimeException | Error e) {
            synchronized (this) {
                running = false;
            }
            throw e;
        }
    }

    private Preview preview(Path path, boolean big) {
        try {
            Preview kept = images.get(Stamp.of(path));
            if (kept != null && (!big || kept.large() != null)) {
                return kept;
            }
        } catch (IOException e) {
            // Gone or unreadable: Pictures says so.
        }
        Preview fresh = Pictures.of(path, big);
        if (fresh.picture() != null) {
            images.put(fresh.picture().stamp(), fresh);
        }
        return fresh;
    }

    /** The images worth reading ahead of {@code at}, nearest first: next, previous, the one after next. */
    static List<Path> around(List<Path> pictures, Path at) {
        return near(pictures, at, 1, -1, 2);
    }

    /**
     * The rest of what the filmstrip shows around {@code at}, beyond {@link #around}: two before, then three after and
     * three before. Wanted as thumbnails only.
     */
    static List<Path> reach(List<Path> pictures, Path at) {
        return near(pictures, at, -2, 3, -3);
    }

    private static List<Path> near(List<Path> pictures, Path at, int... offsets) {
        int i = pictures.indexOf(at);
        if (i < 0) {
            return List.of();
        }
        List<Path> out = new ArrayList<>(offsets.length);
        for (int d : offsets) {
            int j = i + d;
            if (j >= 0 && j < pictures.size()) {
                out.add(pictures.get(j));
            }
        }
        return out;
    }
}
