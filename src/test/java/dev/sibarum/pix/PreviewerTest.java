package dev.sibarum.pix;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** What the previewer reads ahead: pure, over the folder's images. */
class PreviewerTest {

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
