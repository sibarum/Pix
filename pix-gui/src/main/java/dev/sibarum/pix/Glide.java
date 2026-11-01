package dev.sibarum.pix;

/**
 * The filmstrip's glide: at a step, every thumbnail starts where it was drawn before, so many cells along, and eases
 * into its new cell over the throw's time ({@link Toss#LENGTH}), so the strip reads as film pulled through rather than
 * a set of cards that changed. A value, replaced at each step and read at each frame's moment, as the {@link Toss} is.
 *
 * <p>0.1's feel: out-cubic, and never from further than {@code reach}, the strip's half width, past which nothing on it
 * is seen anyway. A step taken mid-glide starts from where the cells are drawn, so the strip never jumps.
 *
 * @param at   when it started, on {@code System.nanoTime()}'s clock
 * @param from where the cells started, along x from where layout puts them, layout px
 */
record Glide(long at, float from) {

    /** Nothing gliding: every cell where layout puts it. */
    static final Glide NONE = new Glide(Lens.AT_REST, 0f);

    /**
     * A glide {@code shift} cells along, each {@code slot} px, from wherever this one has the cells at {@code now}, and
     * no further than {@code reach} px either way. No shift leaves this one going.
     */
    Glide shifted(int shift, float slot, float reach, long now) {
        if (shift == 0) {
            return this;
        }
        float start = Math.max(-reach, Math.min(reach, offset(now) + shift * slot));
        return new Glide(now, start);
    }

    /** Whether it is still moving at {@code now}. */
    boolean moving(long now) {
        return from != 0f && now - at < Toss.LENGTH;
    }

    /** Where the cells are drawn at {@code now}, along x from where layout puts them, layout px. */
    float offset(long now) {
        if (!moving(now)) {
            return 0f;
        }
        float u = now <= at ? 1f : 1f - (float) (now - at) / Toss.LENGTH;
        return from * u * u * u;
    }
}
