package dev.sibarum.pix;

import dev.vexelray.gui.core.style.Oklab;
import dev.vexelray.gui.core.style.Palette;
import dev.vexelray.gui.core.style.Relief;
import dev.vexelray.gui.core.style.Shading;
import dev.vexelray.gui.core.style.Theme;

/**
 * The palette, as anchors.
 *
 * <h2>A palette is a construction, not a list of colours</h2>
 *
 * <p>{@link Palette} takes a handful of anchors and derives the rest: surfaces are an even ladder from the page
 * at a fixed lightness step, and text is the ink blended towards the page at a fixed rate. That is the point --
 * every surface in the application stays in step with every other one for free, and a design change is a
 * number here rather than fifteen hex codes spread across the tree.
 *
 * <p>So when a design has fifteen authored colours, the first job is to <b>measure them in Oklab</b> and find
 * out how many decisions they actually are. Usually far fewer, and the ones that do not fit the construction
 * are the interesting ones -- declare those as roles of your own, at their measured values, and note the gap
 * rather than absorbing it. A theme that silently rounds a designer's colour is a theme nobody can check.
 *
 * <h2>Rasterware's own: a plum black and one pink</h2>
 *
 * <p>Measured from the viewer mockup of 2026-11-01. Its greys are one decision: every one of them sits on a hue of
 * about -53°, and the page ({@code #0d0a11}), the bar ({@code #120d17}), a raised tile ({@code #1b1421}) and its
 * hover ({@code #241b2d}) land on a ladder of 0.018 lightness from the page, at levels 0, 1, 3 and 5. Its text is an
 * ink of the same hue fading a third of the way to the page ({@code #9b8fa8}). The pink ({@code #f99bd1}) is the
 * accent, and the deeper rose of its renders ({@code #c4528f}) is the fill, which the label takes dark, as the
 * mockup's pressed tool does.
 *
 * <p>The gap: the mockup's greys gain chroma as they climb (0.016 at the page, 0.036 at the hover), and the ladder,
 * stepping from the page to an ink as grey as the page, keeps theirs flat. So a raised surface here is a little
 * greyer than drawn. {@code LookTest} holds each authored colour to within the distance this costs.
 */
final class Look {

    // ---------------------------------------------------------------- anchors

    /** The page: the colour behind everything, and what the frame clears to. {@code #0d0a11}. */
    private static final Oklab PAGE = Oklab.polar(0.1522, 0.0155, -55.85);

    /** Primary text, the faintest lilac. {@code #f1eaf5}. */
    private static final Oklab INK = Oklab.polar(0.9457, 0.0164, -46.39);

    /** The one chromatic decision: the wanted cell, the name under the pointer, a focus ring. {@code #f99bd1}. */
    private static final Oklab ACCENT = Oklab.polar(0.8007, 0.1295, -15.02);

    /** The fill of a filled control, and the border of anything the accent has claimed. {@code #c4528f}. */
    private static final Oklab ACTION = Oklab.polar(0.6034, 0.1599, -10.29);

    /** The close button's hover. {@code #e0476f}. */
    private static final Oklab DANGER = Oklab.polar(0.6235, 0.1900, 8.96);

    /** Shadows: near-black, at the page's hue rather than a neutral grey. */
    private static final Oklab DEPTH = Oklab.polar(0.1000, 0.0120, -55.85);

    /** How far one surface is from the next: the bar is one step from the page, a raised tile three. */
    private static final double STEP = 0.0182;

    /** How fast the ink fades towards the page: text(1) is DIM ({@code #9b8fa8}), text(2) is FAINT. */
    private static final double FADE = 0.349;

    /** How dark a shadow is. */
    private static final double SHADOW_ALPHA = 0.60;

    static final Palette PALETTE =
            new Palette(PAGE, STEP, INK, FADE, ACCENT, ACTION, DANGER, DEPTH, SHADOW_ALPHA);

    /**
     * Lit surfaces on, letterpress off.
     *
     * <p>The edge light is what gives a dark panel its glint. Letterpress is the opposite call: it buys
     * contrast for white-on-fill labels by spending crispness, which small text can least afford. Both are
     * decisions to make against a real design rather than defaults to leave alone.
     */
    static final Theme THEME = Theme.of(PALETTE, Shading.ON_DARK, Relief.STANDARD, true, false);

    private Look() {
    }
}
