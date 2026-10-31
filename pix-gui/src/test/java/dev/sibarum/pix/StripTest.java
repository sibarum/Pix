package dev.sibarum.pix;

import org.junit.jupiter.api.Test;

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
}
