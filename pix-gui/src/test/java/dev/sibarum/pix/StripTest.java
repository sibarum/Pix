package dev.sibarum.pix;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StripTest {

    /** A rem at zoom one and density one. */
    private static final float REM = 16f;

    /** The px {@code cells} cells take, and the gaps between them. */
    private static float of(int cells) {
        return Strip.width(cells, REM);
    }

    @Test
    void theStripTakesAtMostHalfTheRoomInAnOddNumberOfCells() {
        assertEquals(5, Strip.cells(2 * of(5), REM), "exactly five fit in half");
        assertEquals(5, Strip.cells(2 * of(6), REM), "six fit, five are shown, so one is in the middle");
        assertEquals(3, Strip.cells(2 * of(5) - 1, REM));
    }

    @Test
    void fewerThanThreeIsNoneAndFifteenIsTheMost() {
        assertEquals(0, Strip.cells(2 * of(3) - 1, REM), "the name has the bar to itself");
        assertEquals(3, Strip.cells(2 * of(3), REM));
        assertEquals(Strip.MOST, Strip.cells(10_000f, REM));
        assertEquals(0, Strip.cells(0f, REM));
        assertEquals(0, Strip.cells(500f, 0f), "not measured yet");
    }

    @Test
    void theControlsSmallerFitMoreCellsInTheSameRoom() {
        assertEquals(3, Strip.cells(400f, REM));
        assertEquals(7, Strip.cells(400f, REM / 2));
    }

    @Test
    void theMiddleCellIsThePictureWantedAndCellsPastTheEndsAreEmpty() {
        assertEquals(2, Strip.path(2, 5, 2, 5));
        assertEquals(0, Strip.path(0, 5, 2, 5));
        assertEquals(-1, Strip.path(0, 5, 0, 3), "at the folder's start, the left cells are empty");
        assertEquals(2, Strip.path(4, 5, 0, 3));
        assertEquals(-1, Strip.path(4, 5, 2, 3), "and at its end the right ones");
    }

    @Test
    void aStepMovesEveryPictureOneCellAndAJumpPastTheStripMovesNone() {
        List<Path> f = List.of(Path.of("a"), Path.of("b"), Path.of("c"), Path.of("d"), Path.of("e"), Path.of("f"));
        assertEquals(1, Strip.shift(f.subList(0, 5), 2, f.subList(1, 6), 2), "on: each came from the cell to the right");
        assertEquals(-1, Strip.shift(f.subList(1, 6), 2, f.subList(0, 5), 2));
        assertEquals(1, Strip.shift(f.subList(0, 3), 0, f.subList(0, 4), 1), "clipped at the folder's start");
        assertEquals(0, Strip.shift(f.subList(0, 3), 1, f.subList(0, 3), 1), "a thumbnail read, nothing moved");
        assertEquals(0, Strip.shift(f.subList(0, 2), 0, f.subList(4, 6), 1), "too far to have moved anything seen");
    }
}
