package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.Caption;
import dev.sibarum.pix.core.Messages.Edge;
import dev.sibarum.pix.core.Messages.ListFolder;
import dev.sibarum.pix.core.Messages.Listed;
import dev.sibarum.pix.core.Messages.Pictured;
import dev.sibarum.pix.core.Messages.Read;
import dev.sibarum.pix.core.Messages.Recall;
import dev.sibarum.pix.core.Messages.Recalled;
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
import java.util.List;
import java.util.Optional;

/**
 * Which picture is wanted, and when it is read: every decision Pix makes, as declared steps on one state.
 *
 * <h2>One read at a time</h2>
 *
 * <p>The person can step through a folder faster than pictures decode: an arrow key held down asks for a dozen a
 * second. So the viewer asks {@link ReadPicture} for one picture and asks for no other until that one is answered.
 * Meanwhile each step only moves what is {@code wanted}. When the answer lands it is shown if it is still the one
 * wanted, and dropped if not, and the next read is of whatever is wanted <em>now</em>. Holding an arrow through a
 * folder decodes the picture it stopped on and at most one on the way. This is a decision about state, not a thread:
 * {@code reading} is a field, and the job is the only thing that waits.
 *
 * <p>A dropped picture's feed was never drawn, so it holds no texture, only the memory it was written into, which goes
 * with it.
 *
 * <h2>The file asked for first, then its folder</h2>
 *
 * <p>{@code pix <file>} shows the file before its folder has been read: the read and the listing go out together, and
 * whichever lands first does its part. Until the listing lands, the caption says the folder is being read.
 */
@Component
@Hears({WindowOpened.class, Recalled.class, Listed.class, Step.class, Edge.class, Pictured.class})
@Publishes({Recall.class, ListFolder.class, Read.class, Show.class, Caption.class})
public final class Viewer {

    static final int CAPACITY = 64;

    /** What the caption says of a folder not listed yet. */
    static final String READING_FOLDER = "reading the folder…";

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
     */
    record State(Optional<Path> folder, List<Path> pictures, boolean listed, int at, Optional<Path> wanted,
                 Optional<Path> reading, Optional<Path> shown) {

        State {
            pictures = List.copyOf(pictures);
        }

        static State initial() {
            return new State(Optional.empty(), List.of(), false, -1, Optional.empty(), Optional.empty(),
                    Optional.empty());
        }

        State inFolder(Path value) {
            return new State(Optional.of(value), List.of(), false, -1, wanted, reading, shown);
        }

        State listed(List<Path> value, int place) {
            return new State(folder, value, true, place, wanted, reading, shown);
        }

        State wanting(Path value, int place) {
            return new State(folder, pictures, listed, place, Optional.of(value), reading, shown);
        }

        State reading(Optional<Path> value) {
            return new State(folder, pictures, listed, at, wanted, value, shown);
        }

        State shown(Path value) {
            return new State(folder, pictures, listed, at, wanted, reading, Optional.of(value));
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

    /** The folder's pictures: find the one wanted among them, or want the first if nothing is yet. */
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
        return next;
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

    /** A read landed: show it if it is still wanted, and read whatever is wanted now if that is something else. */
    static State pictured(State s, Pictured p, Out out) {
        State next = s.reading(Optional.empty());
        if (next.wanted().isPresent() && next.wanted().get().equals(p.path())) {
            out.send(Messages.SHOW_TOPIC, new Show(p.path(), p.feed(), p.width(), p.height(), p.identity()));
            next = next.shown(p.path());
        }
        return readWanted(next, out);
    }

    // ------------------------------------------------------------------ what the steps share

    /** Want {@code path}: say so, and read it unless a read is already out, whose answer will ask for it. */
    private static State want(State s, Path path, int place, Out out) {
        State next = s.wanting(path, place);
        caption(next, out);
        return readWanted(next, out);
    }

    /** Ask for the wanted picture, if nothing is being read and it is not already shown. */
    private static State readWanted(State s, Out out) {
        if (s.reading().isPresent() || s.wanted().isEmpty() || s.wanted().equals(s.shown())) {
            return s;
        }
        out.send(Messages.READ_TOPIC, new Read(s.wanted().get()));
        return s.reading(s.wanted());
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
