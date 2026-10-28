package dev.sibarum.pix;

import dev.sibarum.suite.pictures.Sizes;
import dev.vexelray.codec.image.Decoded;
import dev.vexelray.codec.image.Lru;
import dev.vexelray.codec.image.Picture;
import dev.vexelray.codec.image.Pictures;
import dev.vexelray.codec.image.Reasons;
import dev.vexelray.codec.image.Stamp;
import dev.vexelray.codec.video.Videos;
import dev.vexelray.gui.core.Gui;
import sibarum.probe.Log;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
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
 * <p><b>The file shown always gets an answer.</b> It is asked for whenever it is shown with no picture and is not
 * already being read: a step to it, or the same file opened again (a file replaced on disk, reopened to see the new
 * one), which nothing moving would otherwise miss. A read that fails anyway lands its reason, rather than leaving the
 * file reading for good.
 *
 * <p><b>Decoded images are kept</b>, least recently used first out, so going back finds them without decoding again.
 * The key is the file's {@link Stamp}, so a file changed since it was decoded is decoded again.
 *
 * <p><b>An idle worker reads ahead</b>: the next image, then the previous, then the one after next, at the viewer's
 * size, so stepping through finds them waiting; then, at the thumbnail's size only, the rest of what the filmstrip
 * shows ({@link #reach}), so its thumbnails fill in. Reading ahead stops the moment a real request arrives, and each
 * image it reads is announced to {@link #onRead}, since no model change says so.
 *
 * <p><b>A video is a picture that plays</b> ({@link #decode}): its first frame, read through FFmpeg, kept and read
 * ahead like any picture, and the motion the viewer plays it by. Its thumbnail is a poster, a still from a little way
 * in.
 *
 * <p>Vexplore's previewer, for a window that is only ever the viewer: every picture is wanted at both sizes.
 */
final class Previewer {

    /** Decoded pixels kept for going back: a few photographs at the viewer's size, many thumbnails. */
    static final long BUDGET = 256L * 1024 * 1024;

    private static final Log LOG = Log.of("pix");

    private final Executor offload;
    private final Lru<Object, Decoded> images = new Lru<>(BUDGET, Decoded::bytes, p -> { });

    // Guarded by this.
    private Path last;
    private List<Path> lastPictures;
    private Path wanted;
    /** The file shown, asked for and not landed yet: a document that still has no picture of it waits for that one. */
    private Path asking;
    private boolean running;
    private final Deque<Ahead> ahead = new ArrayDeque<>();

    private final Model model;
    private volatile Consumer<Path> read = p -> { };

    /** A read ahead: an image, and whether it is wanted at the viewer's size or only as a thumbnail. */
    private record Ahead(Path path, boolean big) {
    }

    Previewer(Gui gui, Model model) {
        this(gui.offload(), model);
    }

    /** Reading on {@code offload}: the {@code Gui}'s lane, or a test's own. */
    Previewer(Executor offload, Model model) {
        this.offload = offload;
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
    Decoded kept(Path path) {
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
            // picture already asked for is not asked for again. Opening the file shown again clears its picture
            // without moving, so that is asked for too.
            boolean moved = !Objects.equals(want, last);
            boolean ask = moved || want != null && doc.preview() == null && !want.equals(asking);
            if (!ask && doc.pictures() == lastPictures) {
                return;
            }
            last = want;
            lastPictures = doc.pictures();
            ahead.clear();
            ahead.addAll(next);
            if (want == null) {
                wanted = null;
                asking = null;
                return;
            }
            if (ask) {
                wanted = want;
                asking = want;
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
        try {
            offload.execute(this::work);
        } catch (RejectedExecutionException e) {
            running = false;   // shutting down: there is nothing left to read for
        }
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
                if (real) {
                    Decoded p;
                    try {
                        p = preview(path, big);
                    } catch (RuntimeException | LinkageError | OutOfMemoryError | StackOverflowError e) {
                        // The codec gives a reason rather than throwing. Should something under it throw anyway (a
                        // decoder that will not load, a file too large or too deep for it), the file shown says so,
                        // instead of reading for good with the worker gone.
                        LOG.warn("{} did not decode: {}", path, e.toString());
                        p = Decoded.none(path, Reasons.failed(e));
                    }
                    // Before it lands, so the file opened again meanwhile is asked for again rather than lost.
                    synchronized (this) {
                        if (path.equals(asking)) {
                            asking = null;
                        }
                    }
                    model.previewed(path, p);
                } else {
                    try {
                        preview(path, big);
                        read.accept(path);
                    } catch (RuntimeException | LinkageError | OutOfMemoryError | StackOverflowError e) {
                        // Only a guess at what is wanted next: passed over, and the file shown, which may be waiting
                        // behind it, is still read. If it is stepped to, it is read again and says why.
                        LOG.debug("{} was not read ahead: {}", path, e.toString());
                    }
                }
            }
        } catch (RuntimeException | Error e) {
            synchronized (this) {
                running = false;
                // Whatever is still asked for is not stranded behind a worker that is gone: a new one takes it.
                start();
            }
            throw e;
        }
    }

    private Decoded preview(Path path, boolean big) {
        try {
            Decoded kept = images.get(Stamp.of(path));
            if (kept != null && (!big || viewable(path, kept))) {
                return kept;
            }
        } catch (IOException e) {
            // Gone or unreadable: Pictures says so.
        }
        Decoded fresh = decode(path, big);
        if (fresh.picture() != null) {
            images.put(fresh.picture().key(), fresh);
        }
        return fresh;
    }

    /**
     * {@code path}'s picture: at the thumbnail's size, and the viewer's as well when {@code big}. A video at the
     * viewer's size is its first frame and the motion that plays it, as an animated GIF is; as a thumbnail, its poster,
     * one still frame a tenth of the way in.
     */
    static Decoded decode(Path path, boolean big) {
        if (Videos.named(path)) {
            return big ? Videos.poster(path, Sizes.VIEW, true) : Videos.poster(path, Sizes.THUMBNAIL);
        }
        return Pictures.of(path, Sizes.of(big));
    }

    /**
     * Whether {@code d}, kept for {@code path}, will do for the viewer: {@link #large}, and, for a video, the one that
     * plays and not a thumbnail's poster, which a small video's would otherwise pass for.
     */
    static boolean viewable(Path path, Decoded d) {
        return large(d) && (!Videos.named(path) || d.picture().plays());
    }

    /**
     * Whether {@code d} will do at the viewer's size: it has a picture at that size, or its one picture is as large as
     * the viewer's would be, as a video's poster or a small image's thumbnail is.
     */
    static boolean large(Decoded d) {
        if (d.picture(1) != null) {
            return true;
        }
        Picture p = d.picture();
        return p != null && !p.animated() && Math.max(p.frameWidth(), p.frameHeight())
                >= Math.min(Sizes.VIEW, Math.max(p.sourceWidth(), p.sourceHeight()));
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
