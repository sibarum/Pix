package dev.sibarum.pix;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A step's throw, read at its moments: where each picture is drawn, and how far it is shown. */
class TossTest {

    private static final long AT = 1_000_000_000L;
    private static final float SPAN = 800f;

    /** On to the next picture: in from the right, the one before at rest in the middle and wholly shown. */
    private static final Toss NEXT = Toss.thrown(1, AT, SPAN, Lens.Pose.FITTED, 1f);

    @Test
    void nothingThrownIsThePictureAtRestAndNothingGoingOut() {
        assertFalse(Toss.NONE.moving(AT));
        assertEquals(0f, Toss.NONE.in(AT));
        assertEquals(1f, Toss.NONE.inOpacity(AT));
        assertEquals(0f, Toss.NONE.outOpacity(AT));
    }

    @Test
    void itStartsAWholeWidthOffOnItsSideWithTheOneBeforeWhereItWas() {
        assertTrue(NEXT.moving(AT));
        assertEquals(SPAN, NEXT.in(AT), 1e-3f);
        assertEquals(0f, NEXT.inOpacity(AT), 1e-6f);
        assertEquals(0f, NEXT.out(AT), 1e-3f);
        assertEquals(1f, NEXT.outOpacity(AT), 1e-6f);

        Toss back = Toss.thrown(-1, AT, SPAN, Lens.Pose.FITTED, 1f);
        assertEquals(-SPAN, back.in(AT), 1e-3f, "an earlier picture comes in from the left");
    }

    @Test
    void theNewOneIsSolidWithinAQuarterAndSlowsAsItLands() {
        long quarter = AT + Toss.LENGTH / 4;
        assertEquals(1f, NEXT.inOpacity(quarter), 1e-6f);
        assertTrue(NEXT.in(quarter) < SPAN * 0.35f, "out-quart: most of the way in by a quarter");
        float early = NEXT.in(AT + Toss.LENGTH / 10) - NEXT.in(AT + Toss.LENGTH / 5);
        float late = NEXT.in(AT + Toss.LENGTH * 7 / 10) - NEXT.in(AT + Toss.LENGTH * 8 / 10);
        assertTrue(early > late, "it slows as it lands");
        assertTrue(NEXT.out(quarter) < 0f, "the one before goes the other way");
    }

    @Test
    void atItsEndTheNewOneRestsAndTheOldOneIsGone() {
        long end = AT + Toss.LENGTH;
        assertFalse(NEXT.moving(end));
        assertEquals(0f, NEXT.in(end));
        assertEquals(1f, NEXT.inOpacity(end));
        assertEquals(0f, NEXT.outOpacity(end));
        assertEquals(-SPAN, NEXT.out(end - 1), 1f, "a whole width out the other way");
    }

    @Test
    void oneCaughtMidThrowGoesOutFromWhereItWasDrawn() {
        // The picture coming in was 300 px short of the middle and half shown when the next step came.
        Toss caught = Toss.thrown(1, AT, SPAN, new Lens.Pose(2f, 300f, -40f), 0.5f);
        assertEquals(300f, caught.out(AT), 1e-3f);
        assertEquals(0.5f, caught.outOpacity(AT), 1e-6f);
        assertEquals(-SPAN, caught.out(AT + Toss.LENGTH - 1), 1f);
    }

    @Test
    void theOneGoingOutWantedBackComesBackFromWhereItGotTo() {
        // On its way out, 200 px left of the middle and still 0.9 shown, when the step back came.
        Toss back = new Toss(-1, AT, SPAN, -200f, 0.9f, Lens.Pose.FITTED, 0.3f);
        assertEquals(-200f, back.in(AT), 1e-3f, "not a whole width off");
        assertEquals(0.9f, back.inOpacity(AT), 1e-6f, "and not from nothing");
        assertEquals(0f, back.in(AT + Toss.LENGTH), 1e-3f);
        assertEquals(1f, back.inOpacity(AT + Toss.LENGTH / 4), 1e-6f);
    }

    @Test
    void aCrossfadeMovesNothingAndFadesTheOneBeforeWhereItWas() {
        Toss swap = Toss.crossfade(AT, new Lens.Pose(2f, 120f, -40f), 1f);
        assertTrue(swap.moving(AT));
        assertEquals(0f, swap.in(AT));
        assertEquals(0f, swap.inOpacity(AT), 1e-6f, "the new one fades up");
        assertEquals(1f, swap.inOpacity(AT + Toss.LENGTH / 4), 1e-6f);
        assertEquals(120f, swap.out(AT + Toss.LENGTH / 2), 1e-6f, "the old one stays where it was drawn");
        assertTrue(swap.outOpacity(AT + Toss.LENGTH / 2) < 1f);
        assertFalse(swap.moving(AT + Toss.LENGTH));
        assertEquals(0f, swap.outOpacity(AT + Toss.LENGTH));
    }
}
