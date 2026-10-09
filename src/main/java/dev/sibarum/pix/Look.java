package dev.sibarum.pix;

import dev.vexelray.canvas.Color;
import dev.vexelray.gui.core.style.Oklab;
import dev.vexelray.gui.core.style.Palette;
import dev.vexelray.gui.core.style.Relief;
import dev.vexelray.gui.core.style.Role;
import dev.vexelray.gui.core.style.Shading;
import dev.vexelray.gui.core.style.Theme;

/**
 * Colour, decided once. Pix wears Vexplore's palette, from the Vexplore design system's tokens (its
 * {@code tokens.json}; the token names are written beside each value here so a search for one finds both): its viewer
 * began as Vexplore's, and a picture opened from the explorer should look the same in either window. Only the tokens
 * the viewer draws with are here.
 *
 * <p><b>Violet is "you chose this"</b> ({@code accent}, about 258 degrees): a chosen tile and focus. Vex owns cyan in
 * the suite.
 *
 * <h2>Where the ladder lands</h2>
 * The surface ladder starts at {@code bg-0} and steps 0.03 in Oklab lightness, which puts the framework's levels
 * within 0.01 of the tokens a widget would have used: level 1 is {@code bg-1}, 2 is {@code card}, 3 is
 * {@code raised}/{@code hover}, 4 is {@code tree-active}/{@code line}. The ink fades from {@code text-hi} so that
 * level 1 is {@code text-dim} and level 2 is {@code text-disabled}. {@code LookTest} pins each of these.
 */
final class Look {

    // ---------------------------------------------------------------- anchors, from the tokens

    private static final Oklab PAGE = of(0x0e0c14);      // bg-0
    private static final Oklab INK = of(0xece8f5);       // text-hi
    private static final Oklab VIOLET = of(0xb08fff);    // accent
    private static final Oklab DANGER = of(0xe06a7a);    // danger

    /** Lightness per rung: bg-0, bg-1, card, raised, line are rungs 0 to 4 of this. */
    private static final double STEP = 0.03;
    /** text-hi to text-dim is 0.387 of the way to the page; the next rung lands on text-disabled. */
    private static final double FADE = 0.387;
    private static final double SHADOW_ALPHA = 0.55;

    /** Shadows are drawn in the ground itself. */
    static final Palette PALETTE = new Palette(PAGE, STEP, INK, FADE, VIOLET, VIOLET, DANGER, PAGE, SHADOW_ALPHA);

    static final Theme THEME = Theme.of(PALETTE, Shading.ON_DARK, Relief.STANDARD, false, false);

    // ---------------------------------------------------------------- tokens the ladders do not reach

    /** {@code chrome}: the panels the controls sit on. */
    static final Role CHROME = token(0x13101a);

    /** {@code line-strong}: the border of a control, 3:1 against every surface it sits on. */
    static final Role LINE_STRONG = token(0x70669a);

    /** {@code text}: a control's word, and what a picture is ("PNG image · 1920 × 1080"). */
    static final Role TEXT = token(0xbdb6cc);

    /** The chosen control's word and icon, and the filmstrip's frame around the picture shown: a light violet. */
    static final Role CHOSEN_INK = token(0xd9ccff);

    /**
     * The chosen control: an opaque violet fill ("interactive means opaque"), edged in {@code accent} and labelled in
     * {@link #CHOSEN_INK}. The fill is the viewer mockups' chosen-button violet.
     */
    static final Role CHOSEN = token(0x3d2f78);

    private static Role token(int rgb) {
        Color c = Color.rgb(rgb);
        return p -> c;
    }

    private static Oklab of(int rgb) {
        return Oklab.of(Color.rgb(rgb));
    }

    private Look() {
    }
}
