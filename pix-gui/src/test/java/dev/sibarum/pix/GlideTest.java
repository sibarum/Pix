package dev.sibarum.pix;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The filmstrip's glide, read at its moments: where the cells are drawn. */
class GlideTest {

    private static final long AT = 1_000_000_000L;
    private static final float SLOT = 48f;
    private static final float REACH = 3 * SLOT;

    @Test
    void aStepOnStartsEachThumbnailACellRightWhereItWasAndEasesItIn() {
        Glide on = Glide.NONE.shifted(1, SLOT, REACH, AT);
        assertTrue(on.moving(AT));
        assertEquals(SLOT, on.offset(AT), 1e-3f);
        float early = on.offset(AT + Toss.LENGTH / 10) - on.offset(AT + Toss.LENGTH / 5);
        float late = on.offset(AT + Toss.LENGTH * 7 / 10) - on.offset(AT + Toss.LENGTH * 8 / 10);
        assertTrue(early > late, "it slows as it lands");
        assertFalse(on.moving(AT + Toss.LENGTH));
        assertEquals(0f, on.offset(AT + Toss.LENGTH));
        assertEquals(-SLOT, Glide.NONE.shifted(-1, SLOT, REACH, AT).offset(AT), 1e-3f, "back: from the left");
    }

    @Test
    void aStepMidGlideStartsFromWhereTheCellsAreAndNeverPastTheReach() {
        Glide on = Glide.NONE.shifted(1, SLOT, REACH, AT);
        long mid = AT + Toss.LENGTH / 4;
        Glide again = on.shifted(1, SLOT, REACH, mid);
        assertEquals(on.offset(mid) + SLOT, again.offset(mid), 1e-3f);
        assertEquals(REACH, Glide.NONE.shifted(9, SLOT, REACH, AT).offset(AT), 1e-3f);
    }

    @Test
    void noShiftLeavesTheGlideGoingAndNothingIsStill() {
        Glide on = Glide.NONE.shifted(1, SLOT, REACH, AT);
        assertSame(on, on.shifted(0, SLOT, REACH, AT + 1));
        assertFalse(Glide.NONE.moving(AT));
        assertEquals(0f, Glide.NONE.offset(AT));
    }
}
