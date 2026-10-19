package dev.sibarum.pix;

import dev.vexelray.codec.image.Decoded;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** What the previewer reads ahead, pure, over the folder's images; and that the file shown always gets an answer. */
class PreviewerTest {

    /** One red pixel, as a binary PPM: an image imagelib reads with nothing to write it but these bytes. */
    private static final byte[] RED = {'P', '6', '\n', '1', ' ', '1', '\n', '2', '5', '5', '\n', (byte) 255, 0, 0};

    @Test
    void anEmptyOrBrokenFileSaysWhyRatherThanReadingForGood(@TempDir Path dir) throws Exception {
        Path empty = Files.write(dir.resolve("empty.png"), new byte[0]);
        Path broken = Files.writeString(dir.resolve("broken.jpg"), "not a photograph", StandardCharsets.US_ASCII);
        Path gone = dir.resolve("gone.png");
        ExecutorService offload = Executors.newSingleThreadExecutor();
        try {
            Model m = new Model();
            new Previewer(offload, m);
            for (Path p : List.of(empty, broken, gone)) {
                m.open(dir, p);
                Decoded d = landed(m, p);
                assertFalse(d.image(), p.toString());
                assertFalse(d.identity().isBlank(), "a reason for " + p.getFileName());
            }
        } finally {
            offload.shutdownNow();
        }
    }

    @Test
    void theFileShownOpenedAgainIsReadAgain(@TempDir Path dir) throws Exception {
        Path a = Files.writeString(dir.resolve("a.ppm"), "half written", StandardCharsets.US_ASCII);
        ExecutorService offload = Executors.newSingleThreadExecutor();
        try {
            Model m = new Model();
            new Previewer(offload, m);
            m.open(dir, a);
            assertFalse(landed(m, a).image(), "not an image yet");
            Files.write(a, RED);
            m.open(dir, a);
            Decoded d = landed(m, a);
            assertTrue(d.image(), "the file replaced and opened again is shown as it is now: " + d.identity());
        } finally {
            offload.shutdownNow();
        }
    }

    /** The picture of {@code p} once it lands, failing after a while if it never does. */
    private static Decoded landed(Model m, Path p) throws InterruptedException {
        for (int i = 0; i < 1000; i++) {
            Doc d = m.doc();
            if (p.equals(d.at()) && d.preview() != null) {
                return d.preview();
            }
            Thread.sleep(10);
        }
        fail("no picture of " + p + " ever landed: it would read for good");
        return null;
    }

    private static final Path A = Path.of("a.png");
    private static final Path B = Path.of("b.jpg");
    private static final Path C = Path.of("c.gif");

    @Test
    void readingAheadIsNextThenPreviousThenTheOneAfterNext() {
        Path d = Path.of("d.webp");
        List<Path> four = List.of(A, B, C, d);
        assertEquals(List.of(C, A, d), Previewer.around(four, B));
        assertEquals(List.of(B, C), Previewer.around(four, A), "nothing before the first");
        assertEquals(List.of(), Previewer.around(four, Path.of("notes.txt")));
    }

    @Test
    void theFilmstripsReachIsWhatAroundLeavesWithinThree() {
        List<Path> eight = new ArrayList<>();
        for (char c = 'a'; c <= 'h'; c++) {
            eight.add(Path.of(c + ".png"));
        }
        Path d = Path.of("d.png");
        assertEquals(List.of(Path.of("b.png"), Path.of("g.png"), Path.of("a.png")), Previewer.reach(eight, d));
        assertEquals(List.of(Path.of("d.png")), Previewer.reach(eight, Path.of("a.png")),
                "from the first, only three after: around has the next two");
        assertEquals(List.of(), Previewer.reach(eight, Path.of("notes.txt")));
    }
}
