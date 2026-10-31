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
                Messages.HOLD_TOPIC, Messages.SHOW_TOPIC, Messages.RELEASE_TOPIC, Messages.CAPTION_TOPIC)) {
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
        return new Pictured(p, Optional.of(Feed.of(2, 2, 1, false)), w, h, "PNG image", 100);
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
        tell(Messages.PICTURED_TOPIC, new Pictured(A, Optional.empty(), 0, 0, "not an image", 100));
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
}
