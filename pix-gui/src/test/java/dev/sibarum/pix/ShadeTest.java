package dev.sibarum.pix;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A shade, read at its moments: how far a node is shown. */
class ShadeTest {

    private static final long AT = 1_000_000_000L;

    @Test
    void atOnceIsThereFromTheFirstFrame() {
        Shade s = Shade.at(null, 0f);
        assertFalse(s.moving(AT));
        assertEquals(0f, s.opacity(AT));
    }

    @Test
    void itComesUpEvenlyOverItsLengthAndAChangeMidWayStartsFromWhereItIs() {
        Shade up = Shade.at(null, 0f).toward(1f, AT);
        assertTrue(up.moving(AT));
        assertEquals(0.5f, up.opacity(AT + Shade.LENGTH / 2), 1e-3f);
        Shade down = up.toward(0f, AT + Shade.LENGTH / 2);
        assertEquals(0.5f, down.opacity(AT + Shade.LENGTH / 2), 1e-3f, "no jump");
        assertEquals(0f, down.opacity(AT + Shade.LENGTH * 3 / 2), 1e-6f);
        assertSame(down, down.toward(0f, AT + 1), "already going there");
    }

    @Test
    void shownAgainItStartsFromALittleOrFromLessIfItIsLess() {
        Shade whole = Shade.at(null, 1f);
        assertEquals(0.3f, whole.from(0.3f, 1f, AT).opacity(AT), 1e-6f);
        Shade dipped = whole.from(0f, 1f, AT);
        assertEquals(0.1f, dipped.from(0.3f, 1f, AT + Shade.LENGTH / 10).opacity(AT + Shade.LENGTH / 10), 1e-3f);
    }
}
