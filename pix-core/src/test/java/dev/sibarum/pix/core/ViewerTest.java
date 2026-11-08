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
import dev.sibarum.pix.core.Messages.TraceSettings;
import dev.sibarum.pix.core.Messages.Written;
import dev.sibarum.pix.core.Messages.Traced;
import dev.sibarum.pix.core.Messages.Tracing;
import dev.sibarum.pix.core.Messages.WriteTrace;
import dev.sibarum.pix.core.Messages.Convert;
import dev.sibarum.pix.core.Messages.Format;
import dev.sibarum.pix.core.Messages.WriteCopy;
import dev.vexelray.framework.core.PickFile;
import dev.vexelray.framework.core.Picked;
import dev.vexelray.framework.core.WindowOpened;
import dev.vexelray.gui.core.Feed;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sibarum.atchung.Atchung;
import sibarum.atchung.Fatal;
import sibarum.atchung.Inbox;
import sibarum.atchung.Tick;
import sibarum.atchung.Topic;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The viewer, alone: no window and no jobs. It owns an inbox on a tick of this test's, as it will own one on the
 * framework's; each test publishes what the view or a job would, runs the tick, and reads what the viewer sent. The
 * jobs' answers are published by hand, which is what makes the interesting orders (an answer that arrives after the
 * person has moved on) as easy to test as the common one.
 */
class ViewerTest {

    private static final Path FOLDER = Path.of("/pictures").toAbsolutePath();
    private static final Path A = FOLDER.resolve("a.png");
    private static final Path B = FOLDER.resolve("b.png");
    private static final Path C = FOLDER.resolve("c.png");
    private static final Path D = FOLDER.resolve("d.png");

    private Fatal previous;
    private Atchung bus;
    private Tick tick;
    private final List<Object> sent = new ArrayList<>();

    @BeforeEach
    void build() {
        previous = Atchung.fatal();
        Atchung.onFatal(Fatal.THROW);
        bus = Atchung.create();
        tick = bus.tick(null);
        new Viewer(tick.inbox("viewer"));
        Inbox heard = tick.inbox("heard");
        for (Topic<?> t : List.of(Messages.RECALL_TOPIC, Messages.LIST_FOLDER_TOPIC, Messages.READ_TOPIC,
                Messages.HOLD_TOPIC, Messages.SHOW_TOPIC, Messages.RELEASE_TOPIC, Messages.CAPTION_TOPIC,
                Messages.READ_THUMB_TOPIC, Messages.HOLD_THUMB_TOPIC, Messages.RELEASE_THUMB_TOPIC,
                Messages.FILMSTRIP_TOPIC, Messages.CLEAR_TOPIC, PickFile.TOPIC, Messages.TRACE_TOPIC,
                Messages.WRITE_TRACE_TOPIC, Messages.WRITE_COPY_TOPIC)) {
            listen(heard, t);
        }
    }

    @AfterEach
    void restoreTheFatalPolicy() {
        Atchung.onFatal(previous);
    }

    private <T> void listen(Inbox heard, Topic<T> topic) {
        heard.subscribe(topic, sent::add, 256);
    }

    private <T> void tell(Topic<T> topic, T message) {
        bus.publish(topic, message);
        tick.run(System.nanoTime() + 60_000_000_000L);
    }

    private <T> List<T> took(Class<T> kind) {
        List<T> out = sent.stream().filter(kind::isInstance).map(kind::cast).toList();
        sent.removeIf(kind::isInstance);
        return out;
    }

    private static Pictured read(Path p) {
        return read(p, 2, 2);
    }

    /** A picture read, said to be {@code w} by {@code h}; its feed is small, since only the size is counted. */
    private static Pictured read(Path p, int w, int h) {
        return new Pictured(p, Optional.of(Feed.of(2, 2, 1, false)), w, h, "PNG image", 100, thumb(p));
    }

    private static Thumb thumb(Path p) {
        return new Thumb(p, Optional.of(Feed.of(2, 2, 1, false)), 2, 2);
    }

    /**
     * Started on {@code file}: its read answered and the folder listed, nothing more. What was sent is forgotten but
     * for the read ahead the listing started, which is still out.
     */
    private void startedOn(Path file) {
        tell(Messages.RECALLED_TOPIC, new Recalled(FOLDER, Optional.of(file)));
        took(Read.class);
        tell(Messages.PICTURED_TOPIC, read(file));
        tell(Messages.LISTED_TOPIC, new Listed(FOLDER, List.of(A, B, C, D)));
        sent.removeIf(m -> !(m instanceof Read));
    }

    /** Answer every read the viewer asks for, as it asks, until it asks for none; the reads, in the order asked. */
    private List<Path> answerAll(int w, int h) {
        List<Path> asked = new ArrayList<>();
        for (List<Read> reads = took(Read.class); !reads.isEmpty(); reads = took(Read.class)) {
            for (Read r : reads) {
                asked.add(r.path());
                tell(Messages.PICTURED_TOPIC, read(r.path(), w, h));
            }
        }
        return asked;
    }

    private List<Path> answerAll() {
        return answerAll(2, 2);
    }

    private List<Path> shown() {
        return took(Show.class).stream().map(Show::path).toList();
    }

    private List<Path> held() {
        return took(Hold.class).stream().map(h -> h.picture().path()).toList();
    }

    @Test
    void theMainWindowOpeningAsksWhereToStart() {
        tell(WindowOpened.TOPIC, new WindowOpened(dev.vexelray.framework.core.CloseRequested.MAIN));
        assertEquals(List.of(new Recall()), took(Recall.class));
        tell(WindowOpened.TOPIC, new WindowOpened("about"));
        assertEquals(List.of(), took(Recall.class), "the About window is not a start");
    }

    @Test
    void theFileAskedForIsReadWhileItsFolderIsListed() {
        tell(Messages.RECALLED_TOPIC, new Recalled(FOLDER, Optional.of(C)));
        assertEquals(List.of(new Read(C)), took(Read.class));
        assertEquals(List.of(new ListFolder(FOLDER)), took(ListFolder.class));
        assertEquals(new Caption("c.png", Viewer.READING_FOLDER), took(Caption.class).getLast());

        tell(Messages.PICTURED_TOPIC, read(C));
        assertEquals(List.of(C), held());
        assertEquals(List.of(C), shown());
        assertEquals(List.of(), took(Read.class), "no neighbours to read ahead until the folder is listed");

        tell(Messages.LISTED_TOPIC, new Listed(FOLDER, List.of(A, B, C, D)));
        assertEquals(new Caption("c.png", "3 of 4"), took(Caption.class).getLast());
        assertEquals(List.of(new Read(D)), took(Read.class), "c is not read again; its neighbour is read ahead");
    }

    @Test
    void aFolderAskedForShowsItsFirstPicture() {
        tell(Messages.RECALLED_TOPIC, new Recalled(FOLDER, Optional.empty()));
        assertEquals(List.of(), took(Read.class));
        tell(Messages.LISTED_TOPIC, new Listed(FOLDER, List.of(A, B)));
        assertEquals(List.of(new Read(A)), took(Read.class));
        assertEquals(new Caption("a.png", "1 of 2"), took(Caption.class).getLast());
    }

    @Test
    void anEmptyFolderSaysSo() {
        tell(Messages.RECALLED_TOPIC, new Recalled(FOLDER, Optional.empty()));
        tell(Messages.LISTED_TOPIC, new Listed(FOLDER, List.of()));
        assertEquals(new Caption("", "no pictures here"), took(Caption.class).getLast());
        tell(Messages.STEP_TOPIC, new Step(1));
        assertEquals(List.of(), took(Read.class));
    }

    /** Idle, the viewer reads the next picture, then the previous, then the one after next, and then stops. */
    @Test
    void readingAheadGoesOnThenBackThenTwoOn() {
        startedOn(B);
        assertEquals(List.of(C, A, D), answerAll());
        assertEquals(List.of(), shown(), "a picture read ahead is held, not shown");
        assertEquals(List.of(C, A, D), held());
    }

    @Test
    void aStepToAPictureReadAheadShowsItWithoutAReadAndReadsOnFromThere() {
        startedOn(A);
        answerAll();
        tell(Messages.STEP_TOPIC, new Step(1));
        assertEquals(List.of(B), shown(), "at once, in the same step");
        assertEquals(List.of(new Read(D)), took(Read.class), "c and a are held, so d is next");
    }

    @Test
    void stepsStopAtTheFoldersEnds() {
        startedOn(A);
        answerAll();
        tell(Messages.STEP_TOPIC, new Step(-1));
        assertEquals(List.of(), took(Read.class));
        assertEquals(List.of(), shown());
        tell(Messages.EDGE_TOPIC, new Edge(true));
        assertEquals(List.of(new Read(D)), took(Read.class));
        tell(Messages.PICTURED_TOPIC, read(D));
        assertEquals(List.of(D), shown());
        tell(Messages.STEP_TOPIC, new Step(5));
        assertEquals(List.of(), took(Read.class));
        assertEquals(new Caption("d.png", "4 of 4"), took(Caption.class).getLast());
    }

    /**
     * An arrow held down: one read out at a time, so a step waits behind at most the one already out (here, b read
     * ahead), and only the picture it stops on is shown.
     */
    @Test
    void holdingAnArrowReadsWhereItStopsAndAtMostOneOnTheWay() {
        startedOn(A);
        assertEquals(List.of(new Read(B)), took(Read.class), "b is read ahead");
        tell(Messages.STEP_TOPIC, new Step(1));
        tell(Messages.STEP_TOPIC, new Step(1));
        tell(Messages.STEP_TOPIC, new Step(1));
        assertEquals(List.of(), took(Read.class), "one read at a time");
        assertEquals(new Caption("d.png", "4 of 4"), took(Caption.class).getLast(), "the caption keeps up");

        tell(Messages.PICTURED_TOPIC, read(B));
        assertEquals(List.of(), shown(), "b was passed, so it is only held");
        assertEquals(List.of(new Read(D)), took(Read.class), "and what is wanted now is read");

        tell(Messages.PICTURED_TOPIC, read(D));
        assertEquals(List.of(D), shown());
        assertEquals(List.of(new Read(C)), took(Read.class), "then d's one neighbour not held");
    }

    @Test
    void steppingBackToThePictureShownShowsNothingNew() {
        startedOn(A);
        assertEquals(List.of(new Read(B)), took(Read.class));
        tell(Messages.STEP_TOPIC, new Step(1));
        tell(Messages.STEP_TOPIC, new Step(-1));
        assertEquals(List.of(), took(Read.class), "b, read ahead, is still out");
        tell(Messages.PICTURED_TOPIC, read(B));
        assertEquals(List.of(), shown(), "a is still on screen");
        assertEquals(List.of(new Read(C)), took(Read.class), "and reading ahead goes on");
    }

    @Test
    void aPictureLandedAfterThePersonMovedOnIsThereWhenTheyComeBack() {
        startedOn(A);
        tell(Messages.EDGE_TOPIC, new Edge(true));
        tell(Messages.PICTURED_TOPIC, read(B));
        tell(Messages.PICTURED_TOPIC, read(D));
        took(Read.class);
        tell(Messages.STEP_TOPIC, new Step(-2));
        assertEquals(List.of(D, B), shown(), "b was held when it landed, so going back finds it");
    }

    @Test
    void aFileThatIsNotAPictureIsHeldAndShownAsWhyNot() {
        tell(Messages.RECALLED_TOPIC, new Recalled(FOLDER, Optional.of(A)));
        tell(Messages.PICTURED_TOPIC, new Pictured(A, Optional.empty(), 0, 0, "not an image", 100, new Thumb(A, Optional.empty(), 0, 0)));
        Pictured held = took(Hold.class).getFirst().picture();
        assertTrue(held.feed().isEmpty());
        assertEquals("not an image", held.identity());
        assertEquals(List.of(A), shown());
    }

    @Test
    void aListingOfAnotherFolderIsIgnored() {
        tell(Messages.RECALLED_TOPIC, new Recalled(FOLDER, Optional.empty()));
        tell(Messages.LISTED_TOPIC, new Listed(Path.of("/elsewhere").toAbsolutePath(), List.of(A)));
        assertEquals(List.of(), took(Read.class));
    }

    /**
     * Pictures of 60 MB each against a budget of 256: four fit, so the fifth lets go of the least recently shown one
     * that is neither shown, wanted nor read ahead, and that one is read again when it is wanted again.
     */
    @Test
    void pastTheBudgetTheLeastRecentlyShownIsLetGoAndReadAgainWhenWanted() {
        int side = 3354;
        long each = Viewer.bytes(side, side);
        assertTrue(4 * each <= Viewer.BUDGET && 5 * each > Viewer.BUDGET, "the sizes this test needs");
        List<Path> folder = IntStream.range(0, 8).mapToObj(i -> FOLDER.resolve("p" + i + ".png")).toList();

        tell(Messages.RECALLED_TOPIC, new Recalled(FOLDER, Optional.of(folder.get(0))));
        took(Read.class);
        tell(Messages.PICTURED_TOPIC, read(folder.get(0), side, side));
        tell(Messages.LISTED_TOPIC, new Listed(FOLDER, folder));
        assertEquals(List.of(folder.get(1), folder.get(2)), answerAll(side, side));
        tell(Messages.STEP_TOPIC, new Step(1));
        assertEquals(List.of(folder.get(3)), answerAll(side, side));
        assertEquals(List.of(), took(Release.class), "four held, within the budget");

        tell(Messages.STEP_TOPIC, new Step(1));
        assertEquals(List.of(folder.get(4)), answerAll(side, side));
        assertEquals(List.of(new Release(folder.get(0))), took(Release.class),
                "p0 was shown longest ago; p1, p3 and p4 are p2's neighbours");

        tell(Messages.EDGE_TOPIC, new Edge(false));
        assertEquals(List.of(new Read(folder.get(0))), took(Read.class));
    }
    /** A folder of {@code n} pictures, p0 to p{n-1}, started on p{@code at} with its own read answered. */
    private List<Path> startedIn(int n, int at) {
        List<Path> folder = IntStream.range(0, n).mapToObj(i -> FOLDER.resolve("p" + i + ".png")).toList();
        tell(Messages.RECALLED_TOPIC, new Recalled(FOLDER, Optional.of(folder.get(at))));
        took(Read.class);
        tell(Messages.PICTURED_TOPIC, read(folder.get(at)));
        tell(Messages.LISTED_TOPIC, new Listed(FOLDER, folder));
        return folder;
    }

    /**
     * The filmstrip shows what the view has room for: the pictures its span either side, and the thumbnails of those
     * not read ahead are read after the neighbours, one at a time, nearest first.
     */
    @Test
    void aSpanShowsTheStripAndReadsItsThumbnailsAfterTheNeighbours() {
        List<Path> p = startedIn(8, 3);
        assertEquals(List.of(p.get(4), p.get(2), p.get(5)), answerAll());
        assertEquals(List.of(), took(ReadThumb.class), "no span, no thumbnails read");
        assertEquals(List.of(p.get(3), p.get(4), p.get(2), p.get(5)),
                took(HoldThumb.class).stream().map(h -> h.thumb().path()).toList(), "a read brings its thumbnail");

        tell(Messages.SPAN_TOPIC, new Span(2));
        assertEquals(new Filmstrip(p.subList(1, 6), 2), took(Filmstrip.class).getLast());
        assertEquals(List.of(new ReadThumb(p.get(1))), took(ReadThumb.class), "p2, p4 and p5 came with their reads");
        tell(Messages.THUMBNAILED_TOPIC, new Thumbnailed(thumb(p.get(1))));
        assertEquals(List.of(p.get(1)), took(HoldThumb.class).stream().map(h -> h.thumb().path()).toList());
        assertEquals(List.of(), took(ReadThumb.class), "the strip is filled");
        assertEquals(List.of(), took(Read.class));
    }

    @Test
    void aStepReadsThePictureBeforeAnyThumbnail() {
        List<Path> p = startedIn(8, 3);
        tell(Messages.SPAN_TOPIC, new Span(3));
        answerAll();
        assertEquals(List.of(new ReadThumb(p.get(1))), took(ReadThumb.class));
        tell(Messages.STEP_TOPIC, new Step(-3));
        assertEquals(List.of(), took(Read.class), "the thumbnail read is still out");
        tell(Messages.THUMBNAILED_TOPIC, new Thumbnailed(thumb(p.get(1))));
        assertEquals(List.of(new Read(p.get(0))), took(Read.class), "then the picture wanted, not the next thumbnail");
        assertEquals(new Filmstrip(p.subList(0, 4), 0), took(Filmstrip.class).getLast(), "clipped at the folder's start");
    }

    @Test
    void aThumbnailClickedIsWantedAndOneNotInTheFolderIsIgnored() {
        List<Path> p = startedIn(8, 3);
        answerAll();
        shown();
        tell(Messages.GO_TOPIC, new Go(p.get(5)));
        assertEquals(new Caption("p5.png", "6 of 8"), took(Caption.class).getLast());
        assertEquals(List.of(p.get(5)), shown(), "read ahead, so at once");
        tell(Messages.GO_TOPIC, new Go(FOLDER.resolve("gone.png")));
        assertEquals(List.of(), took(Caption.class));
    }

    @Test
    void thumbnailsFarFromThePictureWantedAreLetGo() {
        List<Path> p = startedIn(40, 0);
        tell(Messages.SPAN_TOPIC, new Span(1));
        answerAll();
        took(ReleaseThumb.class);
        tell(Messages.EDGE_TOPIC, new Edge(true));
        List<Path> gone = took(ReleaseThumb.class).stream().map(ReleaseThumb::path).toList();
        assertEquals(List.of(p.get(0), p.get(1), p.get(2)), gone, "39 away, past the strip and the 16 kept beyond it");
    }

    @Test
    void openAsksForAPictureOrAFolderStartingInTheFolderOpenAndOneAtATime() {
        startedOn(A);
        tell(Messages.OPEN_TOPIC, new Open(false));
        assertEquals(List.of(PickFile.open(1, FOLDER.toString(), Viewer.PICTURES)), took(PickFile.class));
        tell(Messages.OPEN_TOPIC, new Open(true));
        assertEquals(List.of(), took(PickFile.class), "one dialog at a time");
        tell(Picked.TOPIC, Picked.cancelled(1));
        assertEquals(List.of(), took(ListFolder.class), "a cancel changes nothing");
        tell(Messages.OPEN_TOPIC, new Open(true));
        assertEquals(List.of(PickFile.folder(2, FOLDER.toString())), took(PickFile.class));
    }

    @Test
    void aPicturePickedIsReadWhileItsFolderIsListed() {
        startedOn(A);
        Path other = Path.of("/elsewhere").toAbsolutePath();
        Path x = other.resolve("x.png");
        tell(Messages.OPEN_TOPIC, new Open(false));
        tell(Picked.TOPIC, new Picked(7, x.toString()));
        assertEquals(List.of(), took(ListFolder.class), "an answer to a dialog not asked for is ignored");
        tell(Picked.TOPIC, new Picked(1, x.toString()));
        assertEquals(List.of(new ListFolder(other)), took(ListFolder.class));
        assertEquals(new Caption("x.png", Viewer.READING_FOLDER), took(Caption.class).getLast());
        answerAll();
        assertEquals(List.of(x), shown());
        tell(Messages.LISTED_TOPIC, new Listed(other, List.of(x, other.resolve("y.png"))));
        assertEquals(new Caption("x.png", "1 of 2"), took(Caption.class).getLast());
    }

    @Test
    void aFolderPickedOpensOnItsFirstPictureAndLetsTheOldThumbnailsGo() {
        startedOn(A);
        tell(Messages.SPAN_TOPIC, new Span(1));
        answerAll();
        shown();
        took(ReleaseThumb.class);
        Path other = Path.of("/elsewhere").toAbsolutePath();
        Path x = other.resolve("x.png");
        tell(Messages.OPEN_TOPIC, new Open(true));
        tell(Picked.TOPIC, new Picked(1, other.toString()));
        assertEquals(List.of(new ListFolder(other)), took(ListFolder.class));
        assertEquals(List.of(), took(Read.class), "nothing to read until the folder is listed");
        tell(Messages.LISTED_TOPIC, new Listed(other, List.of(x)));
        assertEquals(new Caption("x.png", "1 of 1"), took(Caption.class).getLast());
        answerAll();
        assertEquals(List.of(x), shown());
        assertEquals(List.of(A, B, C), took(ReleaseThumb.class).stream().map(ReleaseThumb::path).sorted().toList(),
                "every thumbnail read in the folder before, those read ahead too");
    }

    @Test
    void aFolderPickedWithNoPicturesShowsNothing() {
        startedOn(A);
        Path empty = Path.of("/empty").toAbsolutePath();
        tell(Messages.OPEN_TOPIC, new Open(true));
        took(PickFile.class);
        tell(Picked.TOPIC, new Picked(1, empty.toString()));
        tell(Messages.LISTED_TOPIC, new Listed(empty, List.of()));
        assertEquals(List.of(new Clear()), took(Clear.class));
        assertEquals(new Caption("", "no pictures here"), took(Caption.class).getLast());
        tell(Messages.OPEN_TOPIC, new Open(false));
        assertEquals(List.of(PickFile.open(2, empty.toString(), Viewer.PICTURES)), took(PickFile.class));
    }

    @Test
    void aPictureShownSaysWhichSideOfTheOneBeforeItIs() {
        tell(Messages.RECALLED_TOPIC, new Recalled(FOLDER, Optional.of(B)));
        tell(Messages.PICTURED_TOPIC, read(B));
        assertEquals(List.of(new Show(B, 0)), took(Show.class), "the first picture has nothing to come in beside");
        tell(Messages.LISTED_TOPIC, new Listed(FOLDER, List.of(A, B, C, D)));
        answerAll();
        tell(Messages.STEP_TOPIC, new Step(1));
        assertEquals(List.of(new Show(C, 1)), took(Show.class), "later in the folder: in from the right");
        tell(Messages.EDGE_TOPIC, new Edge(false));
        answerAll();
        assertEquals(List.of(new Show(A, -1)), took(Show.class), "earlier: in from the left");
    }

    // ------------------------------------------------------------------ tracing (docs/trace.md)

    private static final TraceSettings SIX = TraceSettings.FIRST;
    private static final TraceSettings FOUR = new TraceSettings(4, true);
    private static final TraceSettings FLAT = new TraceSettings(4, false);

    private void wantTrace(Path p, TraceSettings s) {
        tell(Messages.TRACING_TOPIC, new Tracing(Optional.of(p), s));
    }

    private void answerTrace(Trace t) {
        tell(Messages.TRACED_TOPIC, new Traced(t.path(), t.settings(), Optional.empty(), 0, 0, List.of(), ""));
    }

    @Test
    void theTraceWantedIsTracedAtOnce() {
        wantTrace(A, SIX);
        assertEquals(List.of(new Trace(A, SIX)), took(Trace.class));
    }

    /** Dragging the colours slider asks for a trace at every stop; only the one out and the last are traced. */
    @Test
    void whileATraceIsOutOnlyTheLatestSettingsAreTracedNext() {
        wantTrace(A, SIX);
        took(Trace.class);
        wantTrace(A, FOUR);
        wantTrace(A, FLAT);
        assertEquals(List.of(), took(Trace.class), "one trace out at a time");
        answerTrace(new Trace(A, SIX));
        assertEquals(List.of(new Trace(A, FLAT)), took(Trace.class), "four colours with gradients was passed by");
        answerTrace(new Trace(A, FLAT));
        assertEquals(List.of(), took(Trace.class), "what is wanted is what was traced");
    }

    @Test
    void theSameTraceIsNotAskedForTwiceButIsAgainOnceThePanelReopens() {
        wantTrace(A, SIX);
        answerTrace(took(Trace.class).getFirst());
        wantTrace(A, SIX);
        assertEquals(List.of(), took(Trace.class));
        tell(Messages.TRACING_TOPIC, new Tracing(Optional.empty(), SIX));
        wantTrace(A, SIX);
        assertEquals(List.of(new Trace(A, SIX)), took(Trace.class), "the view let its drawing go when it closed");
    }

    @Test
    void aClosedPanelTracesNothingMoreWhenTheTraceOutLands() {
        wantTrace(A, SIX);
        took(Trace.class);
        tell(Messages.TRACING_TOPIC, new Tracing(Optional.empty(), SIX));
        answerTrace(new Trace(A, SIX));
        assertEquals(List.of(), took(Trace.class));
    }

    @Test
    void saveWritesTheTraceWantedOnceAndNothingWithThePanelClosed() {
        tell(Messages.SAVE_TRACE_TOPIC, new SaveTrace());
        assertEquals(List.of(), took(WriteTrace.class), "nothing wanted");
        wantTrace(A, FOUR);
        tell(Messages.SAVE_TRACE_TOPIC, new SaveTrace());
        tell(Messages.SAVE_TRACE_TOPIC, new SaveTrace());
        assertEquals(List.of(new WriteTrace(A, FOUR)), took(WriteTrace.class), "one save at a time");
    }

    @Test
    void aDrawingWrittenIsListedWithItsFolderAndShown() {
        startedOn(A);
        answerAll();
        Path drawing = FOLDER.resolve("a.sdg");
        wantTrace(A, FOUR);
        tell(Messages.SAVE_TRACE_TOPIC, new SaveTrace());
        took(ListFolder.class);
        took(WriteTrace.class);
        tell(Messages.WRITTEN_TOPIC, new Written(A, Optional.of(drawing), ""));
        assertEquals(List.of(new ListFolder(FOLDER)), took(ListFolder.class));
        assertEquals(List.of(new Read(drawing)), took(Read.class));
        tell(Messages.SAVE_TRACE_TOPIC, new SaveTrace());
        assertEquals(1, took(WriteTrace.class).size(), "the save answered, another may go");
    }

    @Test
    void aDrawingNotWrittenChangesNothing() {
        startedOn(A);
        answerAll();
        wantTrace(A, FOUR);
        tell(Messages.SAVE_TRACE_TOPIC, new SaveTrace());
        tell(Messages.WRITTEN_TOPIC, new Written(A, Optional.empty(), "disk full"));
        assertEquals(List.of(), took(ListFolder.class));
        assertEquals(List.of(), took(Read.class));
    }

    // ------------------------------------------------------------------ converting (docs/convert.md)

    @Test
    void aConversionIsWrittenAndThenShownWithItsFolder() {
        startedOn(A);
        answerAll();
        took(ListFolder.class);
        tell(Messages.CONVERT_TOPIC, new Convert(A, Format.SDG));
        assertEquals(List.of(new WriteCopy(A, Format.SDG)), took(WriteCopy.class));
        Path copy = FOLDER.resolve("a.sdg");
        tell(Messages.WRITTEN_TOPIC, new Written(A, Optional.of(copy), ""));
        assertEquals(List.of(new ListFolder(FOLDER)), took(ListFolder.class));
        assertEquals(List.of(new Read(copy)), took(Read.class));
    }

    @Test
    void oneFileIsWrittenAtATimeWhetherTracedOrConverted() {
        wantTrace(A, FOUR);
        tell(Messages.SAVE_TRACE_TOPIC, new SaveTrace());
        tell(Messages.CONVERT_TOPIC, new Convert(B, Format.PNG));
        assertEquals(List.of(), took(WriteCopy.class), "the trace's save is out");
        tell(Messages.WRITTEN_TOPIC, new Written(A, Optional.empty(), "disk full"));
        tell(Messages.CONVERT_TOPIC, new Convert(B, Format.PNG));
        assertEquals(List.of(new WriteCopy(B, Format.PNG)), took(WriteCopy.class));
    }

    @Test
    void anAnswerForAnotherPictureDoesNotEndTheSaveOut() {
        tell(Messages.CONVERT_TOPIC, new Convert(A, Format.PNG));
        took(WriteCopy.class);
        tell(Messages.WRITTEN_TOPIC, new Written(B, Optional.empty(), ""));
        tell(Messages.CONVERT_TOPIC, new Convert(C, Format.PNG));
        assertEquals(List.of(), took(WriteCopy.class));
    }
}
