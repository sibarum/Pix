package dev.sibarum.pix;

import dev.vexelray.gui.core.Feed;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** The splash's pictures: the mark the About window condenses, and the scanlines over it. */
class SplashTest {

    @Test
    void theMarkIsTheBakeAboutUses() {
        Feed mark = Splash.mark();
        assertNotNull(mark, "/pix.pair should be on the classpath; the icon plugin's pair goal bakes it");
        assertEquals(Splash.MARK_SIZE, mark.width());
        assertEquals(Splash.MARK_SIZE, mark.height());
    }

    @Test
    void theScanlinesAreOneRowToADpOfTheLockup() {
        Feed scan = Splash.scanlines(Splash.SCAN_ROWS);
        assertEquals(1, scan.width());
        assertEquals(Splash.SCAN_ROWS, scan.height());
    }
}
