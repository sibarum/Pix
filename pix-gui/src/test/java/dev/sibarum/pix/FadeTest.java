package dev.sibarum.pix;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FadeTest {

    private static final long T = 1_000_000_000_000L;
    private static final float EPS = 1e-4f;

    @Test
    void shownTheControlsStayUntilThePointerHasRestedThenGo() {
        Fade f = Fade.shown(T);
        assertEquals(1f, f.opacity(T), EPS);
        assertEquals(1f, f.opacity(T + Fade.REST - 1), EPS);
        assertEquals(0.5f, f.opacity(T + Fade.REST + Fade.OUT / 2), EPS);
        assertEquals(0f, f.opacity(T + Fade.REST + Fade.OUT), EPS);
        assertEquals(0f, f.opacity(T + 10 * Fade.REST), EPS);
    }

    @Test
    void wokenTheyRiseFromWhereTheyWereNotFromNothing() {
        long half = T + Fade.REST + Fade.OUT / 2;
        Fade f = Fade.shown(T).woken(half);
        assertEquals(0.5f, f.opacity(half), EPS, "nothing jumps");
        assertEquals(1f, f.opacity(half + Fade.IN / 2), EPS);
        assertEquals(1f, f.opacity(half + Fade.REST - 1), EPS, "and rest again before going");
        assertEquals(0f, f.opacity(half + Fade.REST + Fade.OUT), EPS);
    }

    @Test
    void heldTheyNeverGoAndGoneTheyGoAtOnceFromWhereTheyAre() {
        long late = T + 100 * Fade.REST;
        assertEquals(1f, Fade.shown(T).held(T).opacity(late), EPS);
        Fade gone = Fade.shown(T).gone(T);
        assertEquals(0.5f, gone.opacity(T + Fade.OUT / 2), EPS);
        assertEquals(0f, gone.opacity(T + Fade.OUT), EPS);
    }

    @Test
    void theNextChangeIsDueWhileMovingAtTheEndOfTheRestAndNeverOnceStill() {
        Fade f = Fade.shown(T);
        assertEquals(Fade.REST, f.next(T, false), "waits out the rest");
        assertEquals(0L, f.next(T + Fade.REST + 1, false), "fading: every frame");
        assertEquals(Fade.STILL, f.next(T + Fade.REST + Fade.OUT, false), "gone: nothing more");
        assertEquals(0L, f.gone(T + Fade.REST + Fade.OUT).woken(T + Fade.REST + Fade.OUT)
                .next(T + Fade.REST + Fade.OUT, false), "rising: every frame");
        assertEquals(Fade.STILL, f.held(T).next(T, false), "held at one");
    }

    @Test
    void withMotionReducedTheyAreShownOrGoneAndNeverBetween() {
        Fade f = Fade.shown(T);
        assertEquals(1f, f.opacityReduced(T + Fade.REST - 1), EPS);
        assertEquals(0f, f.opacityReduced(T + Fade.REST), EPS);
        assertEquals(Fade.STILL, f.next(T + Fade.REST, true));
        assertEquals(Fade.REST, f.next(T, true));
    }
}
