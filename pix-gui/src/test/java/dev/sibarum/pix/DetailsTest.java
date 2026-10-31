package dev.sibarum.pix;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Which details a narrow bar keeps, and how a file's size is said. */
class DetailsTest {

    /** What the picture is, 150 px; its size, 50; its place, 60; and a dot, 10 between each two. */
    private static final float[] WIDTHS = {150f, 50f, 60f};
    private static final boolean[] ALL = {true, true, true};

    private static boolean[] shown(float room) {
        return Details.shown(WIDTHS, ALL, 10f, room);
    }

    @Test
    void allFitInRoomForAll() {
        assertArrayEquals(ALL, shown(280f));
    }

    @Test
    void theFileSizeGoesFirstThenWhatThePictureIs() {
        assertArrayEquals(new boolean[]{true, false, true}, shown(279f));
        assertArrayEquals(new boolean[]{true, false, true}, shown(220f));
        assertArrayEquals(new boolean[]{false, false, true}, shown(219f));
    }

    /** The place stays, as the name does, however little room there is: cut at the bar's edge rather than gone. */
    @Test
    void thePlaceStays() {
        assertArrayEquals(new boolean[]{false, false, true}, shown(10f));
    }

    /** One that says nothing is not drawn, and takes no dot. */
    @Test
    void aDetailThatSaysNothingIsNotDrawn() {
        boolean[] said = {false, true, true};
        assertArrayEquals(said, Details.shown(WIDTHS, said, 10f, 120f));
    }

    @Test
    void aFileSizeIsSaidAsExplorerSaysIt() {
        assertEquals("1 byte", Details.bytes(1));
        assertEquals("812 bytes", Details.bytes(812));
        assertEquals("1.0 KB", Details.bytes(1024));
        assertEquals("3.2 MB", Details.bytes(3_355_443));
        assertEquals("14 MB", Details.bytes(14L * 1024 * 1024));
        assertEquals("2.0 GB", Details.bytes(2L * 1024 * 1024 * 1024));
        assertEquals("", Details.bytes(-1));
    }
}
