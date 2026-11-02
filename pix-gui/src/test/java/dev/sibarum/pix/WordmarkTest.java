package dev.sibarum.pix;

import dev.vexelray.canvas.Color;
import dev.vexelray.gui.core.Feed;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The wordmark's raster: ten letters of Press Start 2P, seven pixels high, lit in the ink it is given. */
class WordmarkTest {

    @Test
    void theRasterIsTenEightPixelCellsLessTheLastOnesBlankColumn() {
        assertEquals(10 * 8 - 1, Wordmark.W);
        assertEquals(7, Wordmark.H, "Press Start 2P's capitals stand seven pixels on an eight-pixel cell");
    }

    @Test
    void itIsAStillTheSizeOfTheRaster() {
        Feed f = Wordmark.feed(Color.rgb(0xf1eaf5));
        assertEquals(Wordmark.W, f.width());
        assertEquals(Wordmark.H, f.height());
        assertEquals(1, f.slots());
    }
}
