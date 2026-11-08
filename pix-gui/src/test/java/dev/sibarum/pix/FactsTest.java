package dev.sibarum.pix;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FactsTest {

    @Test
    void theFormatIsTheIdentityWithoutItsSize() {
        assertEquals("PNG image", Facts.format("PNG image · 883 × 590"));
        assertEquals("SDG drawing · 5 layers", Facts.format("SDG drawing · 5 layers · 480 × 320"));
        assertArrayEquals(new int[] {480, 320}, Facts.size("SDG drawing · 5 layers · 480 × 320"));
    }

    @Test
    void anAspectReducesWhenItStaysSmallAndIsADecimalWhenNot() {
        assertEquals("3:2", Facts.aspect(480, 320));
        assertEquals("16:9", Facts.aspect(1920, 1080));
        assertEquals("1:1", Facts.aspect(64, 64));
        assertEquals("1.50:1", Facts.aspect(883, 590));
        assertEquals("1:1.50", Facts.aspect(590, 883));
    }

    @Test
    void aFolderUnderHomeIsSaidFromIt() {
        Path home = Path.of("/home/me").toAbsolutePath();
        assertEquals("~/Pictures/Screenshots", Facts.folder(home.resolve("Pictures/Screenshots/a.png"), home));
        assertEquals("~", Facts.folder(home.resolve("a.png"), home));
        Path other = Path.of("/data/a.png").toAbsolutePath();
        assertEquals(other.getParent().toString(), Facts.folder(other, home));
    }
}
