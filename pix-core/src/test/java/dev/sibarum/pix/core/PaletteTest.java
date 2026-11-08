package dev.sibarum.pix.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaletteTest {

    private static byte[] pixels(int... rgbaThenCount) {
        int n = 0;
        for (int i = 1; i < rgbaThenCount.length; i += 2) {
            n += rgbaThenCount[i];
        }
        byte[] out = new byte[n * 4];
        int at = 0;
        for (int i = 0; i < rgbaThenCount.length; i += 2) {
            for (int k = 0; k < rgbaThenCount[i + 1]; k++) {
                int c = rgbaThenCount[i];
                out[at++] = (byte) (c >>> 24);
                out[at++] = (byte) (c >>> 16);
                out[at++] = (byte) (c >>> 8);
                out[at++] = (byte) c;
            }
        }
        return out;
    }

    @Test
    void theFullestColoursComeFirstAndNearShadesAreOne() {
        byte[] px = pixels(0x24102aff, 50, 0xffe8c4ff, 20, 0x26122cff, 30, 0x8a3a6eff, 10, 0x00000000, 99);
        // 0x26122c falls in 0x24102a's bin, whose mean is still 0x24102a: one colour, the fullest.
        assertEquals(List.of(0x24102aff, 0xffe8c4ff, 0x8a3a6eff), Palette.of(px));
    }

    @Test
    void atMostFiveAndNothingFromNothing() {
        byte[] px = pixels(0xff0000ff, 1, 0x00ff00ff, 1, 0x0000ffff, 1, 0xffff00ff, 1, 0x00ffffff, 1, 0xff00ffff, 1);
        assertEquals(5, Palette.of(px).size());
        assertEquals(List.of(), Palette.of(new byte[0]));
    }
}
