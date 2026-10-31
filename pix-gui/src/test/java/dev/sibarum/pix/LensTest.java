package dev.sibarum.pix;

import dev.sibarum.pix.Lens.Pose;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The lens alone: a value and the moments it is read at, so every frame of a gesture is a line here. A picture fitted
 * 800 by 450 on a stage 1000 by 600, unless a test says otherwise.
 */
class LensTest {

    private static final long T = 1_000_000_000L;
    private static final float CLOSE = 1e-3f;

    private static Lens lens() {
        return Lens.fitted(800f, 450f, 1000f, 600f);
    }

    @Test
    void aFittedPictureIsInTheMiddleAndAtRest() {
        Lens l = lens();
        assertFalse(l.moving(T));
        assertEquals(Pose.FITTED, l.at(T));
    }

    /** What the wheel promises: the point of the picture under the pointer stays under it on every frame. */
    @Test
    void theWheelZoomsAboutThePointerOnEveryFrameOfTheEase() {
        float px = 200f;
        float py = -100f;
        Lens l = lens().wheel(3f, px, py, T);
        assertTrue(l.moving(T + 10_000_000L));
        float pictureX = px;   // fitted in the middle at zoom one, the point under the pointer is the pointer's
        float pictureY = py;
        for (long dt = 0; dt <= Lens.SETTLE; dt += 10_000_000L) {
            Pose at = l.at(T + dt);
            assertEquals(pictureX, (px - at.x()) / at.zoom(), CLOSE, "x at +" + dt / 1_000_000 + " ms");
            assertEquals(pictureY, (py - at.y()) / at.zoom(), CLOSE, "y at +" + dt / 1_000_000 + " ms");
        }
        assertEquals((float) Math.pow(Lens.NOTCH, 3), l.at(T + Lens.SETTLE).zoom(), CLOSE);
    }

    @Test
    void aGestureArrivesExactlyAndThenRests() {
        Lens l = lens().wheel(1f, 0f, 0f, T);
        assertEquals(1f, l.at(T).zoom(), "it starts where it was drawn");
        assertTrue(l.at(T + Lens.EASE).zoom() > 1f);
        assertFalse(l.moving(T + Lens.SETTLE));
        assertEquals(Lens.NOTCH, l.at(T + Lens.SETTLE).zoom(), "drawn arrived, not a thousandth short");
    }

    @Test
    void notchesTurnedWhileOneEasesAddUp() {
        Lens l = lens().wheel(1f, 0f, 0f, T).wheel(1f, 0f, 0f, T + Lens.EASE / 2);
        assertEquals(Lens.NOTCH * Lens.NOTCH, l.to().zoom(), CLOSE);
        assertEquals(l.wheel(0f, 0f, 0f, T).at(T + Lens.EASE / 2), l.from(), "the second starts where the first was");
    }

    @Test
    void theZoomStopsAtItsLimits() {
        assertEquals(Lens.MAX_ZOOM, lens().wheel(100f, 0f, 0f, T).to().zoom());
        assertEquals(Lens.MIN_ZOOM, lens().wheel(-100f, 0f, 0f, T).to().zoom());
    }

    @Test
    void aPictureNoLargerThanTheStageStaysInsideIt() {
        Lens l = lens().moved(500f, 500f);
        assertEquals(100f, l.to().x(), "800 wide on 1000: 100 each side");
        assertEquals(75f, l.to().y(), "450 high on 600: 75 above and below");
        assertEquals(l.to(), l.at(T), "a drag is at once");
    }

    @Test
    void aLargerPictureMovesUntilAQuarterOfTheStageStillShowsIt() {
        Lens l = lens().wheel(4f, 0f, 0f, T);
        long rest = T + Lens.SETTLE;
        float w = l.at(rest).zoom() * 800f;
        Lens far = l.moved(100_000f, 0f);
        assertEquals(w / 2f + 1000f / 2f - Lens.KEEP * 1000f, far.at(rest).x(), CLOSE,
                "its left edge a quarter of the stage in from the right");
    }

    @Test
    void theLimitsMeetWhereThePictureIsTheStagesSize() {
        assertEquals(0f, Lens.limit(1000f, 1000f));
        assertEquals(0f, Lens.limit(1000.01f, 1000f), 0.02f);
        assertEquals(Lens.limit(999.99f, 1000f), Lens.limit(1000.01f, 1000f), 0.02f);
    }

    @Test
    void aZoomOutNearAnEdgeEndsWhereThePictureIsHeld() {
        Lens l = lens().wheel(4f, 0f, 0f, T);
        l = l.moved(100_000f, 0f).wheel(-4f, 400f, 0f, T + Lens.SETTLE);
        assertEquals(1f, l.to().zoom(), CLOSE);
        assertEquals(100f, l.to().x(), CLOSE, "back inside the stage, not where the pointer would have put it");
    }

    @Test
    void fitGoesBackToTheMiddleFromWhereItIsDrawn() {
        Lens l = lens().wheel(4f, 300f, 100f, T);
        long mid = T + Lens.EASE;
        Lens fit = l.fit(mid);
        assertEquals(l.at(mid), fit.at(mid), "no jump");
        assertEquals(Pose.FITTED, fit.at(mid + Lens.SETTLE));
    }

    @Test
    void aSmallerStageHoldsThePictureAgain() {
        Lens l = lens().moved(100f, 0f).measured(400f, 225f, 500f, 300f);
        assertEquals(50f, l.to().x(), "400 on 500: 50 each side now");
    }

    @Test
    void anUnmeasuredLensIgnoresTheWheel() {
        Lens l = Lens.fitted(0f, 0f, 0f, 0f);
        assertEquals(l, l.wheel(1f, 0f, 0f, T));
    }
    /** A pinch follows the fingers at once, about the pointer, and its steps multiply. */
    @Test
    void aPinchZoomsAboutThePointerAtOnceAndItsStepsMultiply() {
        float px = 200f;
        float py = -100f;
        Lens l = lens().pinched(1.5f, px, py, T).pinched(1.2f, px, py, T + 10_000_000L);
        assertFalse(l.moving(T + 10_000_000L), "no ease to trail the hand");
        Pose at = l.at(T + 10_000_000L);
        assertEquals(1.8f, at.zoom(), CLOSE);
        assertEquals(px, (px - at.x()) / at.zoom(), CLOSE, "the point under the pointer stays under it");
        assertEquals(py, (py - at.y()) / at.zoom(), CLOSE);

        Lens back = l.pinched(1f / 1.8f, px, py, T + 20_000_000L);
        assertEquals(1f, back.at(T + 20_000_000L).zoom(), CLOSE, "pinching together undoes spreading");
    }

    @Test
    void aPinchStopsAtTheZoomsLimitsAndTakesOverAWheelsEase() {
        assertEquals(Lens.MAX_ZOOM, lens().pinched(1000f, 0f, 0f, T).to().zoom(), CLOSE);
        assertEquals(Lens.MIN_ZOOM, lens().pinched(0.001f, 0f, 0f, T).to().zoom(), CLOSE);
        Lens easing = lens().wheel(3f, 0f, 0f, T);
        float drawn = easing.at(T + Lens.EASE).zoom();
        assertEquals(drawn * 2f, easing.pinched(2f, 0f, 0f, T + Lens.EASE).to().zoom(), CLOSE,
                "from where it is drawn, not where the wheel was going");
    }
}
