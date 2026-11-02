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
 * <h2>Rasterware's own: a blue black and one blue</h2>
 *
 * <p>Two designs, one construction. The viewer mockup of 2026-11-01 settled the shape: its greys are one decision,
 * every one of them on a single hue, the page ({@code #0d0a11}), the bar ({@code #120d17}), a raised tile
 * ({@code #1b1421}) and its hover ({@code #241b2d}) on a ladder of 0.018 lightness from the page, at levels 0, 1, 3
 * and 5; its text an ink of the same hue fading a third of the way to the page ({@code #9b8fa8}); a light accent
 * and a deeper fill, which the label takes dark, as the mockup's pressed tool does. It drew all of that in plum and
 * pink, at a hue of about -53°.
 *
 * <p>The suite icon canvas's branding then moved Rasterware to blue: its icon, its wordmark and every tint on its
 * boards sit on one hue, -115° ({@link #HUE}; the canvas calls it 245°), and its blue ({@code #80c5ff}) has the
 * mockup pink's lightness to the thousandth. So the mockup is kept and turned: every anchor at the lightness and
 * chroma it was measured at, on the brand's hue, and the accent the brand's blue itself.
 *
 * <p>The gaps. The mockup's greys gain chroma as they climb (0.016 at the page, 0.036 at the hover), and the ladder,
 * stepping from the page to an ink as grey as the page, keeps theirs flat, so a raised surface is a little greyer
 * than drawn. And the fill's chroma (0.16) is past what sRGB holds at this hue and lightness, so it is 0.14.
 * {@code LookTest} holds each authored colour, turned, to within the distance these cost.
 */
final class Look {

    // ---------------------------------------------------------------- anchors

    /** The brand's hue, in Oklab degrees: every tint of the icon and the wordmark sits on it. */
    static final double HUE = -115.0;

    /** The page: the colour behind everything, and what the frame clears to. {@code #060c12}. */
    private static final Oklab PAGE = Oklab.polar(0.1522, 0.0155, HUE);

    /** Primary text, the faintest blue. {@code #e4eff8}. */
    private static final Oklab INK = Oklab.polar(0.9457, 0.0164, HUE);

    /** The one chromatic decision: the wanted cell, the name under the pointer, a focus ring. {@code #80c5ff}. */
    private static final Oklab ACCENT = Oklab.polar(0.7998, 0.1083, -114.91);

    /** The fill of a filled control, and the border of anything the accent has claimed. {@code #2087cf}. */
    private static final Oklab ACTION = Oklab.polar(0.6034, 0.1400, HUE);

    /** The close button's hover, the one colour off the brand's hue. {@code #e0476f}. */
    private static final Oklab DANGER = Oklab.polar(0.6235, 0.1900, 8.96);

    /** Shadows: near-black, at the page's hue rather than a neutral grey. */
    private static final Oklab DEPTH = Oklab.polar(0.1000, 0.0120, HUE);

    /** How far one surface is from the next: the bar is one step from the page, a raised tile three. */
    private static final double STEP = 0.0182;

    /** How fast the ink fades towards the page: text(1) is DIM (about {@code #8298ac}), text(2) is FAINT. */
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
