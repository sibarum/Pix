package dev.sibarum.pix;

import dev.vexelray.fields.sdg.Sdg;
import dev.vexelray.gui.core.ImageSdg;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** The wordmark's drawings: ten letters of Press Start 2P, seven pixels high, the face in a theme role. */
class WordmarkTest {

    @Test
    void theMarkIsTenEightPixelCellsLessTheLastOnesBlankColumn() {
        assertEquals(10 * 8 - 1, Wordmark.W);
        assertEquals(7, Wordmark.H, "Press Start 2P's capitals stand seven pixels on an eight-pixel cell");
    }

    @Test
    void theFaceIsADrawingTheSizeOfTheMarkLitInTheNodesInk() {
        ImageSdg face = Wordmark.face();
        assertNotNull(face, "/wordmark.sdgb should be on the class path; the icon plugin's pair goal makes it");
        assertEquals(Wordmark.W, face.source().width(), 1e-9);
        assertEquals(Wordmark.H, face.source().height(), 1e-9);
        assertEquals(Map.of("face", ImageSdg.TINT), roles(face.source()));
    }

    @Test
    void theTapeHasRoomForItsGhostsAndItsFaceIsInk() {
        assertEquals(Wordmark.W * 4 + 4, Wordmark.SPLIT_W, "two units left for the red ghost, two right for the cyan");
        assertEquals(Wordmark.H * 4 + 1, Wordmark.SPLIT_H, "a unit under for the cyan ghost");
        ImageSdg tape = Wordmark.split();
        assertNotNull(tape, "/wordmark-split.sdgb should be on the class path; the icon plugin's pair goal makes it");
        assertEquals(Wordmark.SPLIT_W, tape.source().width(), 1e-9);
        assertEquals(Wordmark.SPLIT_H, tape.source().height(), 1e-9);
        assertEquals(Map.of("face", "ink", "red", "", "cyan", "", "both", ""), roles(tape.source()));
    }

    private static Map<String, String> roles(Sdg sdg) {
        return sdg.layers().stream().collect(Collectors.toMap(Sdg.Layer::name,
                l -> ((Sdg.Paint.Solid) l.paint()).role()));
    }
}
