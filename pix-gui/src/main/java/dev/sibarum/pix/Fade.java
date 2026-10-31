package dev.sibarum.pix;

/**
 * How far the controls are shown: fully while the pointer moves, fading out once it has rested {@link #REST}, and
 * fading back as soon as it moves again (layout.md, <i>The window</i>). A value, replaced when the pointer moves, rests
 * on a control or leaves the window; the view's asked half reads it at each frame's moment and draws the controls at
 * {@link #opacity}.
 *
 * <h2>In closed form</h2>
 *
 * <p>From {@code at}, the controls rise from {@code from} to one over {@link #IN}; from {@code until} they fall from
 * wherever that left them to nothing over {@link #OUT}. So a frame is drawn from this value and its moment alone, with
 * nothing stepped, and a frame that comes late is where it should be. A new fade starts from the opacity the old one
 * had at that moment, so nothing jumps. Times are {@code System.nanoTime()}'s, which is a frame's {@code monoNanos}.
 *
 * @param from  the opacity at {@code at}
 * @param at    when this fade started
 * @param until when the controls start to fade out, or {@link #HELD}
 */
record Fade(float from, long at, long until) {

    /** How long the pointer rests before the controls fade. */
    static final long REST = 2_000_000_000L;
    /** How long the controls take to come back, and to go. */
    static final long IN = 120_000_000L;
    static final long OUT = 400_000_000L;
    /** {@code until} for controls that do not fade: the pointer is on one, or Keep shown is on. */
    static final long HELD = Long.MAX_VALUE;
    /** What {@link #next} says when nothing will change until the fade is replaced. */
    static final long STILL = Long.MAX_VALUE;

    /** Shown at {@code now}, fading once {@link #REST} has passed: how Pix starts. */
    static Fade shown(long now) {
        return new Fade(1f, now, now + REST);
    }

    /** Back, from where they are drawn at {@code now}, and fading again once the pointer has rested. */
    Fade woken(long now) {
        return new Fade(opacity(now), now, now + REST);
    }

    /** Back, and kept. */
    Fade held(long now) {
        return new Fade(opacity(now), now, HELD);
    }

    /** Going now, from where they are drawn: the pointer left the window. */
    Fade gone(long now) {
        return new Fade(opacity(now), now, now);
    }

    /** How opaque the controls are at {@code now}, from nothing to one. */
    float opacity(long now) {
        if (now < until) {
            return rise(now);
        }
        return Math.max(0f, rise(until) - (float) (now - until) / OUT);
    }

    /** As drawn with motion reduced: shown until they would start to fade, then gone. */
    float opacityReduced(long now) {
        return now < until ? 1f : 0f;
    }

    /**
     * How long from {@code now} until the opacity next changes: zero while it is changing, the wait while it is held
     * at one for now, {@link #STILL} once nothing will change.
     */
    long next(long now, boolean reduced) {
        if (now < until) {
            boolean rising = !reduced && rise(now) < 1f;
            return rising ? 0L : until == HELD ? STILL : until - now;
        }
        return !reduced && opacity(now) > 0f ? 0L : STILL;
    }

    private float rise(long now) {
        return Math.min(1f, from + (float) (now - at) / IN);
    }
}
