package dev.sibarum.pix;

/**
 * How the picture is looked at: how far it is zoomed and where it has been moved, held where it can be seen. A value,
 * replaced at each gesture; the view's asked half reads it at each frame's moment and draws it.
 *
 * <p>In layout px from the stage's centre: the picture's centre is at {@code (x, y)}, and its size is {@code zoom}
 * times the size it fits at ({@link Fit}), {@code fitW} by {@code fitH}, on a stage {@code stageW} by {@code stageH}.
 *
 * <h2>Eased, in closed form</h2>
 *
 * <p>A gesture goes {@code from} where the picture is drawn at that moment {@code to} where it is going, starting at
 * {@code since}, on {@code System.nanoTime()}'s clock, which is a frame's {@code monoNanos}. Between them it is at
 * {@code from + (to - from) * u}, where {@code u = 1 - e^(-t / EASE)} closes on one. So a frame is drawn from this value
 * and its moment alone, with nothing stepped or kept, and a frame that comes late is where it should be. Zoom and
 * centre move by the same {@code u}, which keeps the point under the pointer under it all the way: zoomed about a
 * point, the centre is linear in the zoom.
 *
 * <h2>Held where it can be seen</h2>
 *
 * <p>Along each axis a picture no larger than the stage stays inside it, and a larger one may be moved until only
 * {@link #KEEP} of the stage still shows it ({@link #limit}, 0.1's rule). A zoom out that would leave it past that goes
 * where it is held instead, so the point under the pointer slides on the way.
 *
 * @param from   where the gesture started, as drawn at {@code since}
 * @param to     where it is going, and where the picture rests
 * @param since  when the gesture started, or {@link #AT_REST}
 * @param fitW   the picture's width at zoom one, layout px
 * @param fitH   its height at zoom one
 * @param stageW the stage's width, layout px
 * @param stageH its height
 */
record Lens(Pose from, Pose to, long since, float fitW, float fitH, float stageW, float stageH) {

    /** A zoom over the fitted size, and the picture's centre from the stage's, layout px. */
    record Pose(float zoom, float x, float y) {

        /** Fitted, in the middle. */
        static final Pose FITTED = new Pose(1f, 0f, 0f);
    }

    /** The least of the stage, along each axis, that a picture larger than it must still cover. */
    static final float KEEP = 0.25f;
    /** How far one wheel notch zooms. */
    static final float NOTCH = 1.25f;
    /** The smallest and largest zoom, over the fitted size. */
    static final float MIN_ZOOM = 0.1f;
    static final float MAX_ZOOM = 50f;
    /** How fast a gesture closes on where it is going: its time constant, ns. 0.1's zoom eased at the same. */
    static final long EASE = 70_000_000L;
    /** How long a gesture is drawn moving: past seven time constants it is within a thousandth, and drawn arrived. */
    static final long SETTLE = 7 * EASE;
    /** {@code since} for a lens with nothing under way. */
    static final long AT_REST = Long.MIN_VALUE;

    /** A picture fitted to the stage and in its middle, at rest. */
    static Lens fitted(float fitW, float fitH, float stageW, float stageH) {
        return new Lens(Pose.FITTED, Pose.FITTED, AT_REST, fitW, fitH, stageW, stageH);
    }

    // ------------------------------------------------------------------ drawn: no allocation, every frame

    /** Whether a gesture is still under way at {@code now}. */
    boolean moving(long now) {
        return since != AT_REST && now - since < SETTLE;
    }

    float zoom(long now) {
        return value(from.zoom(), to.zoom(), now);
    }

    float x(long now) {
        return value(from.x(), to.x(), now);
    }

    float y(long now) {
        return value(from.y(), to.y(), now);
    }

    private float value(float a, float b, long now) {
        if (!moving(now)) {
            return b;
        }
        if (now <= since) {
            return a;
        }
        double u = 1.0 - Math.exp(-(double) (now - since) / EASE);
        return (float) (a + (b - a) * u);
    }

    /** Where the picture is drawn at {@code now}. */
    Pose at(long now) {
        return new Pose(zoom(now), x(now), y(now));
    }

    // ------------------------------------------------------------------ the gestures

    /**
     * The wheel, {@code notches} turned (positive zooms in), with the pointer at {@code (px, py)} from the stage's
     * centre: zoom about the pointer, from where the picture is drawn now. Notches turned while one eases add up.
     */
    Lens wheel(float notches, float px, float py, long now) {
        if (notches == 0f || !(fitW > 0f) || !(fitH > 0f)) {
            return this;
        }
        Pose at = at(now);
        float zoom = clamp(to.zoom() * (float) Math.pow(NOTCH, notches), MIN_ZOOM, MAX_ZOOM);
        float k = zoom / at.zoom();
        return toward(at, held(new Pose(zoom, px - (px - at.x()) * k, py - (py - at.y()) * k)), now);
    }

    /**
     * A pinch, the fingers' spread changed by {@code factor} (above one spreading), with the pointer at {@code (px, py)}
     * from the stage's centre: zoomed about the pointer at once, as far as the fingers went, since the hand is on the
     * picture and an ease would trail it. From where the picture is drawn now, so a pinch begun while the wheel's zoom
     * eases takes over from there.
     */
    Lens pinched(float factor, float px, float py, long now) {
        if (!(factor > 0f) || factor == 1f || !(fitW > 0f) || !(fitH > 0f)) {
            return this;
        }
        Pose at = at(now);
        float zoom = clamp(at.zoom() * factor, MIN_ZOOM, MAX_ZOOM);
        float k = zoom / at.zoom();
        Pose p = held(new Pose(zoom, px - (px - at.x()) * k, py - (py - at.y()) * k));
        return new Lens(p, p, AT_REST, fitW, fitH, stageW, stageH);
    }

    /** Moved by a drag, {@code (dx, dy)} layout px: at once, and still held. A gesture under way goes on from there. */
    Lens moved(float dx, float dy) {
        Pose a = held(new Pose(from.zoom(), from.x() + dx, from.y() + dy));
        Pose b = held(new Pose(to.zoom(), to.x() + dx, to.y() + dy));
        return new Lens(a, b, since, fitW, fitH, stageW, stageH);
    }

    /** Fitted again, and back to the middle, from where the picture is drawn now. */
    Lens fit(long now) {
        return toward(at(now), Pose.FITTED, now);
    }

    /** The stage or the fitted size changed: the zoom and the centre kept, held on the new stage. */
    Lens measured(float fitW, float fitH, float stageW, float stageH) {
        Lens l = new Lens(from, to, since, fitW, fitH, stageW, stageH);
        return new Lens(l.held(from), l.held(to), since, fitW, fitH, stageW, stageH);
    }

    private Lens toward(Pose at, Pose goal, long now) {
        return new Lens(at, goal, now, fitW, fitH, stageW, stageH);
    }

    /** {@code p} with its centre moved, as little as can be, to where it can be seen. */
    Pose held(Pose p) {
        float lx = limit(p.zoom() * fitW, stageW);
        float ly = limit(p.zoom() * fitH, stageH);
        return new Pose(p.zoom(), clamp(p.x(), -lx, lx), clamp(p.y(), -ly, ly));
    }

    /**
     * How far a picture {@code p} long may move from the centre of a stage {@code v} long: held inside it while it is
     * no longer, and while it is longer, until only {@link #KEEP} of the stage shows it. The slack beyond the picture's
     * own overhang grows with that overhang, so the two meet where {@code p == v}.
     */
    static float limit(float p, float v) {
        if (p <= v) {
            return (v - p) / 2f;
        }
        return (p - v) / 2f + Math.min(p - v, (1f - KEEP) * v);
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : Math.min(v, hi);
    }
}
