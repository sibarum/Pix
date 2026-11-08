package dev.sibarum.pix;

import dev.vexelray.gui.core.Node;

/**
 * How far one node is shown, eased: what Rasterware shows and hides itself (a cell, a thumbnail read, the note, the words when
 * they change) comes and goes over {@link #LENGTH} rather than in a frame. The framework already fades a colour and a
 * node added or removed (its {@code WindowMotion}); an opacity or an image set by the application is the
 * application's to ease. A value, read by the view's asked half at each frame's moment, as the {@link Fade} is.
 *
 * <p>Linear, as the framework's colour fades are: a shade arrives nowhere. A change mid-way starts from where it is
 * drawn.
 *
 * @param node the node shaded
 * @param at   when it started, on {@code System.nanoTime()}'s clock
 * @param from how far it was shown then
 * @param to   how far it is going
 */
record Shade(Node node, long at, float from, float to) {

    /** How long a shade takes, ns: a little longer than the framework's colour fade, since it is a whole thing. */
    static final long LENGTH = 150_000_000L;

    /** {@code node} at {@code to}, at once: drawn there from the first frame. */
    static Shade at(Node node, float to) {
        return new Shade(node, Lens.AT_REST, to, to);
    }

    /** Going to {@code to} from wherever it is drawn at {@code now}; this one, if it is going there already. */
    Shade toward(float to, long now) {
        return to == this.to ? this : new Shade(node, now, opacity(now), to);
    }

    /** From {@code from}, wherever it is now, to {@code to}: a thing shown again, as new. */
    Shade from(float from, float to, long now) {
        return new Shade(node, now, Math.min(from, opacity(now)), to);
    }

    /** Whether it is still going at {@code now}. */
    boolean moving(long now) {
        return at != Lens.AT_REST && from != to && now - at < LENGTH;
    }

    /** How far it is shown at {@code now}. */
    float opacity(long now) {
        if (!moving(now)) {
            return to;
        }
        float q = now <= at ? 0f : (float) (now - at) / LENGTH;
        return from + (to - from) * q;
    }
}
