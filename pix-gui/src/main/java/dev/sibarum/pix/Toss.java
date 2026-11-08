package dev.sibarum.pix;

/**
 * A step's throw: the new picture thrown in across the stage from the side it is on, and the one before flung out the
 * other way. A value, replaced at each step; the view's asked half reads it at each frame's moment, as it reads the
 * {@link Lens}.
 *
 * <p>0.1's feel, in closed form. Over {@link #LENGTH} the new picture comes in a whole stage width ({@code span}) from
 * its {@code side}, slowing hard as it lands (out-quart), and is solid within the first quarter so it is seen flying;
 * the old one goes out from where it was drawn to a whole width the other way (out-cubic), keeping most of itself until
 * it is nearly off ({@code 1 - q²}). 0.1 also leaned each picture up to 7° as it flew, which waits for the canvas to
 * turn a node (TODO, <i>Look closely</i>).
 *
 * <p>A step taken mid-throw takes over from where things are: the picture coming in becomes the one going out, from
 * the offset and opacity it is drawn at, so {@code from} and {@code alpha} are what the view read off the frame
 * before. And the picture that was going out, wanted back (a step and a step back, quickly), comes back from where it
 * had got to, as far as it was still shown, rather than from a whole width off and from nothing, which blanked the
 * stage for a moment.
 *
 * @param side    1 for a picture later in the folder (in from the right), -1 earlier, 0 for a crossfade in place
 * @param at      when it started, on {@code System.nanoTime()}'s clock
 * @param span    how far the picture going out travels: the stage's width, layout px
 * @param inFrom  where the picture coming in starts, along x from where the lens puts it: a whole width off on its
 *                side, or, for the picture that was going out and is wanted back, where it had got to
 * @param inAlpha how far it is shown as it starts: nothing, or what the one wanted back still had
 * @param from    where the picture going out was drawn when it started: its zoom, and its offset from the stage's
 *                centre
 * @param alpha   how far it was shown then
 */
record Toss(int side, long at, float span, float inFrom, float inAlpha, Lens.Pose from, float alpha) {

    /** How long a throw takes, ns: 0.1's step. */
    static final long LENGTH = 420_000_000L;

    /** Nothing thrown: the picture in front at rest, and nothing going out. */
    static final Toss NONE = new Toss(0, Lens.AT_REST, 0f, 0f, 1f, Lens.Pose.FITTED, 0f);

    /** A picture thrown in from a whole {@code span} off on its {@code side}, from nothing. */
    static Toss thrown(int side, long at, float span, Lens.Pose from, float alpha) {
        return new Toss(side, at, span, side * span, 0f, from, alpha);
    }

    /**
     * A picture put in the other's place without a throw (a first picture, one opened from elsewhere, a file that is
     * not a picture, or nothing): the new one fades up where the lens puts it as a thrown one would, and the one before
     * fades where it was drawn, without moving.
     */
    static Toss crossfade(long at, Lens.Pose from, float alpha) {
        return new Toss(0, at, 0f, 0f, 0f, from, alpha);
    }

    /** Whether it is still going at {@code now}. */
    boolean moving(long now) {
        return at != Lens.AT_REST && now - at < LENGTH;
    }

    /** How far through it is at {@code now}, 0 to 1. */
    private float q(long now) {
        if (!moving(now)) {
            return 1f;
        }
        return now <= at ? 0f : (float) (now - at) / LENGTH;
    }

    /** The picture coming in, along x from where the lens puts it, layout px. */
    float in(long now) {
        float u = 1f - q(now);
        return inFrom * u * u * u * u;
    }

    /** How far the picture coming in is shown. */
    float inOpacity(long now) {
        return inAlpha + (1f - inAlpha) * Math.min(1f, 4f * q(now));
    }

    /** The picture going out, along x from the stage's centre, layout px. */
    float out(long now) {
        if (side == 0) {
            return from.x();
        }
        float u = 1f - q(now);
        float eased = 1f - u * u * u;
        return from.x() + (-side * span - from.x()) * eased;
    }

    /** How far the picture going out is shown: nothing once the throw is over, or with nothing thrown. */
    float outOpacity(long now) {
        if (!moving(now)) {
            return 0f;
        }
        float q = q(now);
        return alpha * (1f - q * q);
    }
}
