package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.Caption;
import dev.sibarum.pix.core.Messages.Edge;
import dev.sibarum.pix.core.Messages.Hold;
import dev.sibarum.pix.core.Messages.ListFolder;
import dev.sibarum.pix.core.Messages.Listed;
import dev.sibarum.pix.core.Messages.Pictured;
import dev.sibarum.pix.core.Messages.Read;
import dev.sibarum.pix.core.Messages.Recall;
import dev.sibarum.pix.core.Messages.Recalled;
import dev.sibarum.pix.core.Messages.Release;
import dev.sibarum.pix.core.Messages.Show;
import dev.sibarum.pix.core.Messages.Step;
import dev.vexelray.framework.api.Component;
import dev.vexelray.framework.api.Hears;
import dev.vexelray.framework.api.Publishes;
import dev.vexelray.framework.core.WindowOpened;
import sibarum.atchung.Inbox;
import sibarum.atchung.Ledger;
import sibarum.atchung.Out;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Which picture is wanted, what is read and when, and which pictures are kept: every decision Pix makes, as declared
 * steps on one state.
 *
 * <h2>One read at a time</h2>
 *
 * <p>The person can step through a folder faster than pictures decode: an arrow key held down asks for a dozen a
 * second. So the viewer asks {@link ReadPicture} for one picture and asks for no other until that one is answered.
 * Meanwhile each step only moves what is {@code wanted}. When the answer lands it is shown if it is still the one
 * wanted, and kept if not, and the next read is of whatever is wanted <em>now</em>. Holding an arrow through a
 * folder decodes the picture it stopped on and at most one on the way. This is a decision about state, not a thread:
 * {@code reading} is a field, and the job is the only thing that waits.
 *
 * <h2>Read ahead, and keep what was read</h2>
 *
 * <p>With the wanted picture shown and nothing out, the one read goes to the pictures the person is likely to want
 * next ({@link #AHEAD}: the next, the previous, the one after next), so a step finds its picture waiting and is shown
 * at once, with no read. A real step is never queued behind more than the one read already out.
 *
 * <p>Every picture read is held, by the view, which is the part that can hold a feed: the viewer's state names only
 * paths ({@link Held}). The view is told to {@link Hold} each one, to {@link Show} one it holds, and to
 * {@link Release} one the viewer lets go. What is held is bounded by {@link #BUDGET}, in the pixels read: past it the
 * least recently shown goes first, but never the picture shown, the one wanted, or the neighbours read ahead, which
 * would only be read again. Those few at their largest come to well under the budget, so it always holds them.
 *
 * <h2>The file asked for first, then its folder</h2>
 *
 * <p>{@code pix <file>} shows the file before its folder has been read: the read and the listing go out together, and
 * whichever lands first does its part. Until the listing lands, the caption says the folder is being read, and nothing
 * is read ahead, since there are no neighbours yet.
 */
@Component
@Hears({WindowOpened.class, Recalled.class, Listed.class, Step.class, Edge.class, Pictured.class})
@Publishes({Recall.class, ListFolder.class, Read.class, Hold.class, Show.class, Release.class, Caption.class})
public final class Viewer {

    static final int CAPACITY = 64;

    /** What the caption says of a folder not listed yet. */
    static final String READING_FOLDER = "reading the folder…";

    /** Where reading ahead looks, from the picture wanted, nearest first: the next, the previous, the one after next. */
    static final List<Integer> AHEAD = List.of(1, -1, 2);

    /**
     * The pixels the view may hold, counted as {@link #bytes} counts them: a few dozen photographs at the size they are
     * read. 0.1 kept its decoded pictures to the same budget.
     */
    static final long BUDGET = 256L * 1024 * 1024;

    /** A picture the view holds, and what it costs. */
    record Held(Path path, long bytes) {
    }

    /**
     * Everything the viewer knows, in one value.
     *
     * @param folder   the folder stepped through, once known
     * @param pictures its pictures in order, once listed; empty before then
     * @param listed   whether the listing has landed
     * @param at       the wanted picture's place in {@code pictures}, or -1 when it is not among them (yet)
     * @param wanted   the picture the person is looking for
     * @param reading  the picture asked of {@link ReadPicture} and not yet answered
     * @param shown    the picture last sent to be shown
     * @param held     the pictures the view holds, least recently shown first
     */
    record State(Optional<Path> folder, List<Path> pictures, boolean listed, int at, Optional<Path> wanted,
                 Optional<Path> reading, Optional<Path> shown, List<Held> held) {

        State {
            pictures = List.copyOf(pictures);
            held = List.copyOf(held);
        }

        static State initial() {
            return new State(Optional.empty(), List.of(), false, -1, Optional.empty(), Optional.empty(),
                    Optional.empty(), List.of());
        }

        State inFolder(Path value) {
            return new State(Optional.of(value), List.of(), false, -1, wanted, reading, shown, held);
        }

        State listed(List<Path> value, int place) {
            return new State(folder, value, true, place, wanted, reading, shown, held);
        }

        State wanting(Path value, int place) {
            return new State(folder, pictures, listed, place, Optional.of(value), reading, shown, held);
        }

        State reading(Optional<Path> value) {
            return new State(folder, pictures, listed, at, wanted, value, shown, held);
        }

        State shown(Path value) {
            return new State(folder, pictures, listed, at, wanted, reading, Optional.of(value), held);
        }

        State held(List<Held> value) {
            return new State(folder, pictures, listed, at, wanted, reading, shown, value);
        }

        boolean holds(Path p) {
            return held.stream().anyMatch(h -> h.path().equals(p));
        }

        long holding() {
            return held.stream().mapToLong(Held::bytes).sum();
        }
    }

    private final Ledger<State> ledger;

    public Viewer(Inbox inbox) {
        Ledger.Builder<State> b = Ledger.of(State.initial());
        b.on(WindowOpened.TOPIC, b.step("begin", Viewer::begin))
                .on(Messages.RECALLED_TOPIC, b.step("recalled", Viewer::recalled))
                .on(Messages.LISTED_TOPIC, b.step("listed", Viewer::listed))
                .on(Messages.STEP_TOPIC, b.step("step", Viewer::step))
                .on(Messages.EDGE_TOPIC, b.step("edge", Viewer::edge))
                .on(Messages.PICTURED_TOPIC, b.step("pictured", Viewer::pictured));
        ledger = b.build(inbox, "viewer", CAPACITY);
    }

    /** The state as last committed: for reading, from any thread. */
    public State state() {
        return ledger.state().value();
    }

    // ------------------------------------------------------------------ the steps

    /** The main window exists: ask where to start. Any other window opening (the framework's About) changes nothing. */
    static State begin(State s, WindowOpened opened, Out out) {
        if (opened.main()) {
            out.send(Messages.RECALL_TOPIC, new Recall());
        }
        return s;
    }

    /** Read the file asked for, if any, and list its folder, both at once. */
    static State recalled(State s, Recalled r, Out out) {
        State next = s.inFolder(r.folder());
        if (r.file().isPresent()) {
            next = want(next, r.file().get(), -1, out);
        }
        out.send(Messages.LIST_FOLDER_TOPIC, new ListFolder(r.folder()));
        return next;
    }

    /**
     * The folder's pictures: find the one wanted among them, or want the first if nothing is yet. Its neighbours are
     * known now, so reading ahead can start.
     */
    static State listed(State s, Listed l, Out out) {
        if (s.folder().isEmpty() || !s.folder().get().equals(l.folder())) {
            return s;
        }
        int place = s.wanted().map(l.pictures()::indexOf).orElse(-1);
        State next = s.listed(l.pictures(), place);
        if (next.wanted().isEmpty() && !l.pictures().isEmpty()) {
            return want(next, l.pictures().getFirst(), 0, out);
        }
        caption(next, out);
        return advance(next, out);
    }

    /** On, or back, through the folder, stopping at its ends. */
    static State step(State s, Step m, Out out) {
        if (s.pictures().isEmpty()) {
            return s;
        }
        int from = s.at() < 0 ? 0 : s.at();
        int place = Math.clamp((long) from + m.delta(), 0, s.pictures().size() - 1);
        return place == s.at() ? s : want(s, s.pictures().get(place), place, out);
    }

    /** The folder's first picture, or its last. */
    static State edge(State s, Edge m, Out out) {
        if (s.pictures().isEmpty()) {
            return s;
        }
        int place = m.last() ? s.pictures().size() - 1 : 0;
        return place == s.at() ? s : want(s, s.pictures().get(place), place, out);
    }

    /** A read landed: hold it, show it if it is wanted, and read whatever is wanted, or ahead, next. */
    static State pictured(State s, Pictured p, Out out) {
        State next = s.reading(Optional.empty());
        if (!next.holds(p.path())) {
            next = hold(next, p, out);
        }
        return advance(next, out);
    }

    // ------------------------------------------------------------------ what the steps share

    /** Want {@code path}: say so, and show it if it is held, or read it unless a read is already out. */
    private static State want(State s, Path path, int place, Out out) {
        State next = s.wanting(path, place);
        caption(next, out);
        return advance(next, out);
    }

    /**
     * Show the wanted picture if it is held and not shown yet; then, if nothing is being read, read the wanted picture
     * if it is not held, or else the nearest neighbour that is not.
     */
    private static State advance(State s, Out out) {
        State next = s;
        Optional<Path> wanted = next.wanted();
        if (wanted.isPresent() && !wanted.equals(next.shown()) && next.holds(wanted.get())) {
            out.send(Messages.SHOW_TOPIC, new Show(wanted.get()));
            next = touched(next.shown(wanted.get()), wanted.get());
        }
        if (next.reading().isPresent()) {
            return next;
        }
        State now = next;
        Optional<Path> read = wanted.filter(w -> !now.holds(w)).or(() -> ahead(now));
        if (read.isEmpty()) {
            return next;
        }
        out.send(Messages.READ_TOPIC, new Read(read.get()));
        return next.reading(read);
    }

    /** The nearest of the wanted picture's neighbours ({@link #AHEAD}) that is not held, if any. */
    private static Optional<Path> ahead(State s) {
        return neighbours(s).stream().filter(p -> !s.holds(p)).findFirst();
    }

    /** The pictures {@link #AHEAD} of the wanted one, in that order: none until it has a place in a listed folder. */
    private static List<Path> neighbours(State s) {
        List<Path> out = new ArrayList<>();
        if (s.at() < 0) {
            return out;
        }
        for (int d : AHEAD) {
            int i = s.at() + d;
            if (i >= 0 && i < s.pictures().size()) {
                out.add(s.pictures().get(i));
            }
        }
        return out;
    }

    /**
     * Have the view hold {@code p}, as the most recent, then let go of the least recently shown until what is held is
     * within {@link #BUDGET}, sparing the picture shown, the one wanted and its neighbours.
     */
    private static State hold(State s, Pictured p, Out out) {
        out.send(Messages.HOLD_TOPIC, new Hold(p));
        List<Held> held = new ArrayList<>(s.held());
        held.add(new Held(p.path(), bytes(p.width(), p.height())));
        State next = s.held(held);
        List<Path> spared = new ArrayList<>(neighbours(next));
        next.shown().ifPresent(spared::add);
        next.wanted().ifPresent(spared::add);
        long holding = next.holding();
        for (var it = held.iterator(); it.hasNext() && holding > BUDGET; ) {
            Held h = it.next();
            if (!spared.contains(h.path())) {
                it.remove();
                holding -= h.bytes();
                out.send(Messages.RELEASE_TOPIC, new Release(h.path()));
            }
        }
        return next.held(held);
    }

    /** {@code p}, which is held, moved to the most recently shown. */
    private static State touched(State s, Path p) {
        List<Held> held = new ArrayList<>(s.held());
        for (int i = 0; i < held.size(); i++) {
            if (held.get(i).path().equals(p)) {
                held.add(held.remove(i));
                break;
            }
        }
        return s.held(held);
    }

    /** What a picture {@code w} by {@code h} costs held: its pixels, and the third again its smaller levels add. */
    static long bytes(int w, int h) {
        return (long) w * h * 4 * 4 / 3;
    }

    private static void caption(State s, Out out) {
        String name = s.wanted().map(Viewer::name).orElse("");
        String place;
        if (!s.listed()) {
            place = READING_FOLDER;
        } else if (s.pictures().isEmpty()) {
            place = "no pictures here";
        } else if (s.at() < 0) {
            place = s.pictures().size() + (s.pictures().size() == 1 ? " picture" : " pictures");
        } else {
            place = (s.at() + 1) + " of " + s.pictures().size();
        }
        out.send(Messages.CAPTION_TOPIC, new Caption(name, place));
    }

    private static String name(Path p) {
        Path n = p.getFileName();
        return n == null ? p.toString() : n.toString();
    }
}
