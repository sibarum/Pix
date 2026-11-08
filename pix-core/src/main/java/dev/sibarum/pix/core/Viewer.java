package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.Caption;
import dev.sibarum.pix.core.Messages.Clear;
import dev.sibarum.pix.core.Messages.Edge;
import dev.sibarum.pix.core.Messages.Filmstrip;
import dev.sibarum.pix.core.Messages.Go;
import dev.sibarum.pix.core.Messages.HoldThumb;
import dev.sibarum.pix.core.Messages.Hold;
import dev.sibarum.pix.core.Messages.ListFolder;
import dev.sibarum.pix.core.Messages.Listed;
import dev.sibarum.pix.core.Messages.Open;
import dev.sibarum.pix.core.Messages.Pictured;
import dev.sibarum.pix.core.Messages.Read;
import dev.sibarum.pix.core.Messages.ReadThumb;
import dev.sibarum.pix.core.Messages.Recall;
import dev.sibarum.pix.core.Messages.Recalled;
import dev.sibarum.pix.core.Messages.Release;
import dev.sibarum.pix.core.Messages.ReleaseThumb;
import dev.sibarum.pix.core.Messages.Show;
import dev.sibarum.pix.core.Messages.Span;
import dev.sibarum.pix.core.Messages.Step;
import dev.sibarum.pix.core.Messages.Thumb;
import dev.sibarum.pix.core.Messages.Thumbnailed;
import dev.sibarum.pix.core.Messages.SaveTrace;
import dev.sibarum.pix.core.Messages.Trace;
import dev.sibarum.pix.core.Messages.Written;
import dev.sibarum.pix.core.Messages.Traced;
import dev.sibarum.pix.core.Messages.Tracing;
import dev.sibarum.pix.core.Messages.WriteTrace;
import dev.sibarum.pix.core.Messages.Convert;
import dev.sibarum.pix.core.Messages.WriteCopy;
import dev.vexelray.codec.image.Pictures;
import dev.vexelray.framework.api.Component;
import dev.vexelray.framework.api.Hears;
import dev.vexelray.framework.api.Publishes;
import dev.vexelray.framework.core.PickFile;
import dev.vexelray.framework.core.Picked;
import dev.vexelray.framework.core.WindowOpened;
import sibarum.atchung.Inbox;
import sibarum.atchung.Ledger;
import sibarum.atchung.Out;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Which picture is wanted, what is read and when, and which pictures are kept: every decision Rasterware makes, as declared
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
 *
 * <h2>Opened the same way</h2>
 *
 * <p>Ctrl+O and Ctrl+Shift+O ({@link Open}) ask the framework for the OS's dialog ({@link PickFile}), and what it
 * answers ({@link Picked}) is taken as the command line's place is: a picture shown before its folder is listed, a
 * folder on its first picture. The picture from before stays until the new one is shown; a folder with none is shown
 * as nothing ({@link Clear}). What was held from the folder before is let go as the budget needs the room.
 */
@Component
@Hears({WindowOpened.class, Recalled.class, Listed.class, Step.class, Edge.class, Go.class, Span.class, Open.class,
        Picked.class, Pictured.class, Thumbnailed.class, Tracing.class, Traced.class, SaveTrace.class,
        Written.class, Convert.class})
@Publishes({Recall.class, ListFolder.class, Read.class, ReadThumb.class, PickFile.class, Hold.class, Show.class,
        Clear.class, Release.class, HoldThumb.class, ReleaseThumb.class, Caption.class, Filmstrip.class, Trace.class,
        WriteTrace.class, WriteCopy.class})
public final class Viewer {

    static final int CAPACITY = 64;

    /** What the caption says of a folder not listed yet. */
    static final String READING_FOLDER = "reading the folder…";

    /** Where reading ahead looks, from the picture wanted, nearest first: the next, the previous, the one after next. */
    static final List<Integer> AHEAD = List.of(1, -1, 2);

    /** How many thumbnails past each end of the filmstrip are kept, so a few steps back find the strip filled. */
    static final int THUMBS_BEYOND = 16;

    /**
     * The pixels the view may hold, counted as {@link #bytes} counts them: a few dozen photographs at the size they are
     * read. 0.1 kept its decoded pictures to the same budget.
     */
    static final long BUDGET = 256L * 1024 * 1024;

    /** A picture the view holds, and what it costs. */
    record Held(Path path, long bytes) {
    }

    /** A dialog asked for: its id, and whether it is for a folder rather than a picture. */
    record Picking(long id, boolean folder) {
    }

    /** What the open dialog offers: the files a folder is listed for. */
    static final List<PickFile.Filter> PICTURES = List.of(new PickFile.Filter("Pictures", Pictures.extensions()));

    /**
     * The Trace panel's part of the state (docs/trace.md): the trace the person wants now, none with the panel closed;
     * the one asked of {@link TracePicture} and not answered, of which there is at most one; and the trace last asked for
     * while the panel was open, which is not asked for twice. Closing the panel forgets it, since the view lets its
     * drawing go.
     */
    record Tracer(Optional<Trace> wanted, Optional<Trace> out, Optional<Trace> last) {

        static final Tracer NONE = new Tracer(Optional.empty(), Optional.empty(), Optional.empty());

        Tracer wanting(Optional<Trace> value) {
            return new Tracer(value, out, value.isEmpty() ? Optional.empty() : last);
        }

        Tracer out(Optional<Trace> value) {
            return new Tracer(wanted, value, last);
        }

        Tracer last(Optional<Trace> value) {
            return new Tracer(wanted, out, value);
        }
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
     * @param radius   how many pictures the filmstrip shows each side of the one wanted ({@link Span})
     * @param thumbs   the thumbnails the view holds
     * @param picks    the last dialog id given out
     * @param picking  the dialog asked for and not yet answered
     * @param tracer   what the Trace panel wants traced, and what is being traced
     * @param saving   the picture a file is being written beside, a trace saved or a conversion, if any
     */
    record State(Optional<Path> folder, List<Path> pictures, boolean listed, int at, Optional<Path> wanted,
                 Optional<Path> reading, Optional<Path> shown, List<Held> held, int radius, List<Path> thumbs,
                 long picks, Optional<Picking> picking, Tracer tracer, Optional<Path> saving) {

        State {
            pictures = List.copyOf(pictures);
            held = List.copyOf(held);
            thumbs = List.copyOf(thumbs);
        }

        static State initial() {
            return new State(Optional.empty(), List.of(), false, -1, Optional.empty(), Optional.empty(),
                    Optional.empty(), List.of(), 0, List.of(), 0L, Optional.empty(), Tracer.NONE,
                    Optional.empty());
        }

        /** In a folder not listed yet, wanting nothing in it so far. */
        State inFolder(Path value) {
            return new State(Optional.of(value), List.of(), false, -1, Optional.empty(), reading, shown, held, radius,
                    thumbs, picks, picking, tracer, saving);
        }

        State listed(List<Path> value, int place) {
            return new State(folder, value, true, place, wanted, reading, shown, held, radius, thumbs, picks, picking,
                    tracer, saving);
        }

        State wanting(Path value, int place) {
            return new State(folder, pictures, listed, place, Optional.of(value), reading, shown, held, radius,
                    thumbs, picks, picking, tracer, saving);
        }

        State reading(Optional<Path> value) {
            return new State(folder, pictures, listed, at, wanted, value, shown, held, radius, thumbs, picks, picking,
                    tracer, saving);
        }

        State shown(Optional<Path> value) {
            return new State(folder, pictures, listed, at, wanted, reading, value, held, radius, thumbs, picks,
                    picking, tracer, saving);
        }

        State held(List<Held> value) {
            return new State(folder, pictures, listed, at, wanted, reading, shown, value, radius, thumbs, picks,
                    picking, tracer, saving);
        }

        State radius(int value) {
            return new State(folder, pictures, listed, at, wanted, reading, shown, held, value, thumbs, picks,
                    picking, tracer, saving);
        }

        State thumbs(List<Path> value) {
            return new State(folder, pictures, listed, at, wanted, reading, shown, held, radius, value, picks,
                    picking, tracer, saving);
        }

        /** Asking for a dialog with the next id, or, with none, no longer asking. */
        State picking(Optional<Picking> value) {
            long ids = value.map(Picking::id).orElse(picks);
            return new State(folder, pictures, listed, at, wanted, reading, shown, held, radius, thumbs, ids, value,
                    tracer, saving);
        }

        State tracer(Tracer value) {
            return new State(folder, pictures, listed, at, wanted, reading, shown, held, radius, thumbs, picks,
                    picking, value, saving);
        }

        /** Writing a file beside {@code value}, a trace's save or a conversion; none, writing nothing. */
        State saving(Optional<Path> value) {
            return new State(folder, pictures, listed, at, wanted, reading, shown, held, radius, thumbs, picks,
                    picking, tracer, value);
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
                .on(Messages.GO_TOPIC, b.step("go", Viewer::go))
                .on(Messages.SPAN_TOPIC, b.step("span", Viewer::span))
                .on(Messages.OPEN_TOPIC, b.step("open", Viewer::open))
                .on(Picked.TOPIC, b.step("picked", Viewer::picked))
                .on(Messages.PICTURED_TOPIC, b.step("pictured", Viewer::pictured))
                .on(Messages.THUMBNAILED_TOPIC, b.step("thumbnailed", Viewer::thumbnailed))
                .on(Messages.TRACING_TOPIC, b.step("tracing", Viewer::tracing))
                .on(Messages.TRACED_TOPIC, b.step("traced", Viewer::traced))
                .on(Messages.SAVE_TRACE_TOPIC, b.step("save trace", Viewer::saveTrace))
                .on(Messages.CONVERT_TOPIC, b.step("convert", Viewer::convert))
                .on(Messages.WRITTEN_TOPIC, b.step("trace written", Viewer::written));
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
        if (next.wanted().isEmpty() && next.shown().isPresent()) {
            out.send(Messages.CLEAR_TOPIC, new Clear());
            next = next.shown(Optional.empty());
        }
        caption(next, out);
        strip(next, out);
        return advance(letThumbsGo(next, out), out);
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

    /** A thumbnail clicked: that picture, if it is still in the folder. */
    static State go(State s, Go m, Out out) {
        int place = s.pictures().indexOf(m.path());
        return place < 0 || place == s.at() ? s : want(s, m.path(), place, out);
    }

    /** The filmstrip shows {@code radius} each side now: say what it shows, and read the thumbnails it lacks. */
    static State span(State s, Span m, Out out) {
        int radius = Math.max(0, m.radius());
        if (radius == s.radius()) {
            return s;
        }
        State next = s.radius(radius);
        strip(next, out);
        return advance(next, out);
    }

    /**
     * Ask the OS for a picture, or a folder, starting in the folder open. One dialog at a time: a second Ctrl+O while
     * one is up is ignored.
     */
    static State open(State s, Open m, Out out) {
        if (s.picking().isPresent()) {
            return s;
        }
        Picking p = new Picking(s.picks() + 1, m.folder());
        String start = s.folder().map(Path::toString).orElse("");
        out.send(PickFile.TOPIC, p.folder() ? PickFile.folder(p.id(), start) : PickFile.open(p.id(), start, PICTURES));
        return s.picking(Optional.of(p));
    }

    /**
     * The dialog answered: a picture picked is wanted at once and its folder listed, a folder picked is listed and
     * opened on its first, just as when the command line names them ({@link #recalled}). Cancelled, nothing changes.
     */
    static State picked(State s, Picked m, Out out) {
        Optional<Picking> asked = s.picking().filter(p -> p.id() == m.id());
        if (asked.isEmpty()) {
            return s;
        }
        State next = s.picking(Optional.empty());
        Optional<Path> path = m.isCancelled() ? Optional.empty() : Begin.path(m.path());
        if (path.isEmpty()) {
            return next;
        }
        Path p = path.get();
        Path parent = p.getParent();
        if (asked.get().folder() || parent == null) {
            return recalled(next, new Recalled(p, Optional.empty()), out);
        }
        return recalled(next, new Recalled(parent, Optional.of(p)), out);
    }

    /** A read landed: hold it and its thumbnail, show it if it is wanted, and read whatever is wanted, or ahead, next. */
    static State pictured(State s, Pictured p, Out out) {
        State next = s.reading(Optional.empty());
        if (!next.holds(p.path())) {
            next = hold(next, p, out);
        }
        next = holdThumb(next, p.thumb(), out);
        return advance(next, out);
    }

    /** A thumbnail read on its own landed: hold it, and read on. */
    static State thumbnailed(State s, Thumbnailed t, Out out) {
        return advance(holdThumb(s.reading(Optional.empty()), t.thumb(), out), out);
    }

    /**
     * The Trace panel wants this traced now, or, with no path, nothing: the panel closed. Traced at once unless a trace
     * is already out, in which case the answer to that one asks for this ({@link #traced}).
     */
    static State tracing(State s, Tracing m, Out out) {
        Optional<Trace> wanted = m.path().map(p -> new Trace(p, m.settings()));
        return traceNext(s.tracer(s.tracer().wanting(wanted)), out);
    }

    /** A trace landed: the view shows it if it is still wanted. Trace whatever is wanted now, if it is not this. */
    static State traced(State s, Traced m, Out out) {
        Trace answered = new Trace(m.path(), m.settings());
        if (!s.tracer().out().equals(Optional.of(answered))) {
            return s;
        }
        return traceNext(s.tracer(s.tracer().out(Optional.empty())), out);
    }

    /** Save the trace wanted beside its picture, at full quality. One save at a time; nothing wanted, nothing saved. */
    static State saveTrace(State s, SaveTrace m, Out out) {
        Optional<Trace> wanted = s.tracer().wanted();
        if (wanted.isEmpty() || s.saving().isPresent()) {
            return s;
        }
        out.send(Messages.WRITE_TRACE_TOPIC, new WriteTrace(wanted.get().path(), wanted.get().settings()));
        return s.saving(Optional.of(wanted.get().path()));
    }

    /**
     * Write a picture beside itself in another format (docs/convert.md). One file written at a time, a trace's save or a
     * conversion: a second Save while one is out is ignored.
     */
    static State convert(State s, Convert m, Out out) {
        if (s.saving().isPresent()) {
            return s;
        }
        out.send(Messages.WRITE_COPY_TOPIC, new WriteCopy(m.path(), m.to()));
        return s.saving(Optional.of(m.path()));
    }

    /**
     * A file written beside a picture, a drawing traced or a conversion: its folder listed again and the file wanted, as
     * a picture opened is; the picture from before stays shown until it is. Not written, the view says why, and nothing
     * else changes.
     */
    static State written(State s, Written m, Out out) {
        if (!s.saving().equals(Optional.of(m.picture()))) {
            return s;
        }
        State next = s.saving(Optional.empty());
        if (m.file().isEmpty() || m.file().get().getParent() == null) {
            return next;
        }
        return recalled(next, new Recalled(m.file().get().getParent(), m.file()), out);
    }

    /** With no trace out, trace the one wanted, unless it is what was traced last. */
    private static State traceNext(State s, Out out) {
        Tracer t = s.tracer();
        if (t.out().isPresent() || t.wanted().isEmpty() || t.wanted().equals(t.last())) {
            return s;
        }
        out.send(Messages.TRACE_TOPIC, t.wanted().get());
        return s.tracer(t.out(t.wanted()).last(t.wanted()));
    }

    // ------------------------------------------------------------------ what the steps share

    /** Want {@code path}: say so, and show it if it is held, or read it unless a read is already out. */
    private static State want(State s, Path path, int place, Out out) {
        State next = s.wanting(path, place);
        caption(next, out);
        strip(next, out);
        return advance(letThumbsGo(next, out), out);
    }

    /**
     * Show the wanted picture if it is held and not shown yet; then, if nothing is being read, read the wanted picture
     * if it is not held, or else the nearest neighbour that is not.
     */
    private static State advance(State s, Out out) {
        State next = s;
        Optional<Path> wanted = next.wanted();
        if (wanted.isPresent() && !wanted.equals(next.shown()) && next.holds(wanted.get())) {
            out.send(Messages.SHOW_TOPIC, new Show(wanted.get(), side(next, next.shown(), wanted.get())));
            next = touched(next.shown(wanted), wanted.get());
        }
        if (next.reading().isPresent()) {
            return next;
        }
        State now = next;
        Optional<Path> read = wanted.filter(w -> !now.holds(w)).or(() -> ahead(now));
        if (read.isPresent()) {
            out.send(Messages.READ_TOPIC, new Read(read.get()));
            return next.reading(read);
        }
        Optional<Path> thumb = unthumbed(now);
        if (thumb.isPresent()) {
            out.send(Messages.READ_THUMB_TOPIC, new ReadThumb(thumb.get()));
            return next.reading(thumb);
        }
        return next;
    }

    /**
     * Which side {@code to} is from {@code from} in the folder: 1 later, -1 earlier, 0 for none shown before, the same
     * picture, or either not among the folder's (a picture opened from another, or one shown before its folder is
     * listed). 0.1's {@code PictureViewer.direction}.
     */
    static int side(State s, Optional<Path> from, Path to) {
        int a = from.map(s.pictures()::indexOf).orElse(-1);
        int b = s.pictures().indexOf(to);
        return a < 0 || b < 0 ? 0 : Integer.signum(b - a);
    }

    /** The nearest picture in the filmstrip, either side of the one wanted, with no thumbnail held. */
    private static Optional<Path> unthumbed(State s) {
        if (s.at() < 0) {
            return Optional.empty();
        }
        for (int d = 1; d <= s.radius(); d++) {
            for (int i : new int[] {s.at() + d, s.at() - d}) {
                if (i >= 0 && i < s.pictures().size() && !s.thumbs().contains(s.pictures().get(i))) {
                    return Optional.of(s.pictures().get(i));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Have the view hold {@code t}. A picture whose thumbnail is held already, read whole since, has the new one held
     * in its place, the view closing the old: the viewer never touches a feed.
     */
    private static State holdThumb(State s, Thumb t, Out out) {
        out.send(Messages.HOLD_THUMB_TOPIC, new HoldThumb(t));
        if (s.thumbs().contains(t.path())) {
            return s;
        }
        List<Path> thumbs = new ArrayList<>(s.thumbs());
        thumbs.add(t.path());
        return letThumbsGo(s.thumbs(thumbs), out);
    }

    /**
     * Let go of every thumbnail further from the picture wanted than the filmstrip shows and {@link #THUMBS_BEYOND}
     * more, so a step back finds the strip filled, and what is held stays a few dozen however long the folder.
     */
    private static State letThumbsGo(State s, Out out) {
        if (s.at() < 0) {
            return s;
        }
        int keep = s.radius() + THUMBS_BEYOND;
        List<Path> thumbs = new ArrayList<>();
        for (Path p : s.thumbs()) {
            int i = s.pictures().indexOf(p);
            if (i >= 0 && Math.abs(i - s.at()) <= keep) {
                thumbs.add(p);
            } else {
                out.send(Messages.RELEASE_THUMB_TOPIC, new ReleaseThumb(p));
            }
        }
        return thumbs.size() == s.thumbs().size() ? s : s.thumbs(thumbs);
    }

    /** What the filmstrip shows: the pictures within the radius of the one wanted, once it has a place. */
    private static void strip(State s, Out out) {
        if (s.at() < 0 || s.radius() == 0) {
            out.send(Messages.FILMSTRIP_TOPIC, new Filmstrip(List.of(), 0));
            return;
        }
        int from = Math.max(0, s.at() - s.radius());
        int to = Math.min(s.pictures().size(), s.at() + s.radius() + 1);
        out.send(Messages.FILMSTRIP_TOPIC, new Filmstrip(s.pictures().subList(from, to), s.at() - from));
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
