package dev.sibarum.pix;

import dev.vexelray.gui.core.ImageSdg;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The splash's pictures: the mark's drawing, and the scanlines over it. */
class SplashTest {

    @Test
    void theMarkIsTheDrawingThePairGoalMakes() {
        ImageSdg mark = Splash.mark();
        assertNotNull(mark, "/pix.sdgb should be on the classpath; the icon plugin's pair goal makes it");
        assertEquals(mark.drawing().width(), mark.drawing().height(), "a square mark");
        assertTrue(mark.drawing().layers().size() > 1, "in colour: more than one layer");
    }

    @Test
    void theScanlinesAreOneLineInThreeDpOfTheLockup() {
        assertEquals(24, Splash.scanlines(Splash.SCAN_ROWS), "a line at the top of every three dp of 72");
        assertEquals(1, Splash.scanlines(1), "the first row is a line");
        assertEquals(2, Splash.scanlines(4), "and so is the fourth");
    }
}
