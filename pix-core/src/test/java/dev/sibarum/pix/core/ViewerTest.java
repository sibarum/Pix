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
                Messages.SHOW_TOPIC, Messages.CAPTION_TOPIC)) {
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
        return new Pictured(p, Optional.of(Feed.of(2, 2, 1, false)), 2, 2, "PNG image");
    }

    /** Started on {@code file} in the folder, its read answered, and the folder listed. */
    private void startedOn(Path file) {
        tell(Messages.RECALLED_TOPIC, new Recalled(FOLDER, Optional.of(file)));
        tell(Messages.PICTURED_TOPIC, read(file));
        tell(Messages.LISTED_TOPIC, new Listed(FOLDER, List.of(A, B, C, D)));
        sent.clear();
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
        assertEquals(C, took(Show.class).getFirst().path());

        tell(Messages.LISTED_TOPIC, new Listed(FOLDER, List.of(A, B, C, D)));
        assertEquals(new Caption("c.png", "3 of 4"), took(Caption.class).getLast());
        assertEquals(List.of(), took(Read.class), "the picture shown is not read again");
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

    @Test
    void stepsStopAtTheFoldersEnds() {
        startedOn(A);
        tell(Messages.STEP_TOPIC, new Step(-1));
        assertEquals(List.of(), took(Read.class));
        tell(Messages.EDGE_TOPIC, new Edge(true));
        assertEquals(List.of(new Read(D)), took(Read.class));
        tell(Messages.PICTURED_TOPIC, read(D));
        tell(Messages.STEP_TOPIC, new Step(5));
        assertEquals(List.of(), took(Read.class));
        assertEquals(new Caption("d.png", "4 of 4"), took(Caption.class).getLast());
    }

    /** An arrow held down: one read out at a time, and only the picture it stops on is shown. */
    @Test
    void holdingAnArrowReadsWhereItStopsAndAtMostOneOnTheWay() {
        startedOn(A);
        tell(Messages.STEP_TOPIC, new Step(1));
        tell(Messages.STEP_TOPIC, new Step(1));
        tell(Messages.STEP_TOPIC, new Step(1));
        assertEquals(List.of(new Read(B)), took(Read.class), "one read at a time");
        assertEquals(new Caption("d.png", "4 of 4"), took(Caption.class).getLast(), "the caption keeps up");

        tell(Messages.PICTURED_TOPIC, read(B));
        assertEquals(List.of(), took(Show.class), "b was passed, so it is dropped");
        assertEquals(List.of(new Read(D)), took(Read.class), "and what is wanted now is read");

        tell(Messages.PICTURED_TOPIC, read(D));
        List<Show> shown = took(Show.class);
        assertEquals(1, shown.size());
        assertEquals(D, shown.getFirst().path());
    }

    @Test
    void steppingBackToThePictureShownReadsNothing() {
        startedOn(A);
        tell(Messages.STEP_TOPIC, new Step(1));
        tell(Messages.STEP_TOPIC, new Step(-1));
        assertEquals(List.of(new Read(B)), took(Read.class));
        tell(Messages.PICTURED_TOPIC, read(B));
        assertEquals(List.of(), took(Show.class));
        assertEquals(List.of(), took(Read.class), "a is still on screen");
    }

    @Test
    void aFileThatIsNotAPictureIsShownAsWhyNot() {
        tell(Messages.RECALLED_TOPIC, new Recalled(FOLDER, Optional.of(A)));
        tell(Messages.PICTURED_TOPIC, new Pictured(A, Optional.empty(), 0, 0, "not an image"));
        Show s = took(Show.class).getFirst();
        assertTrue(s.feed().isEmpty());
        assertEquals("not an image", s.identity());
    }

    @Test
    void aListingOfAnotherFolderIsIgnored() {
        tell(Messages.RECALLED_TOPIC, new Recalled(FOLDER, Optional.empty()));
        tell(Messages.LISTED_TOPIC, new Listed(Path.of("/elsewhere").toAbsolutePath(), List.of(A)));
        assertEquals(List.of(), took(Read.class));
    }
}
