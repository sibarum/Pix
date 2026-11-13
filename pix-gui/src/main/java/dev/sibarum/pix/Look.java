package dev.sibarum.pix;

import dev.vexelray.canvas.Color;
import dev.vexelray.gui.core.style.Oklab;
import dev.vexelray.gui.core.style.Palette;
import dev.vexelray.gui.core.style.Relief;
import dev.vexelray.gui.core.style.Role;
import dev.vexelray.gui.core.style.Shading;
import dev.vexelray.gui.core.style.Theme;

/**
 * The two looks, dark and light, as anchors: the person chooses between them in the app menu (Theme, then Light or
 * Dark), and the framework remembers the choice and re-themes every window with it. Rasterware opens in the dark one.
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
 *
 * <h2>The light look is authored, not reflected</h2>
 *
 * <p>No design has drawn Rasterware light, so its light look is the dark one's decisions made again for a white page,
 * on the same hue: the brand's blue is the only colour, and every grey leans to it. A mirror of the dark page (L 0.15)
 * would land at 0.85, a grey, where a light page wants 0.975; the brand's blue, at L 0.80, does not read on white and has
 * to come down to about half that; and a shadow that is depth on the dark page is a smudge on a white one. Each anchor
 * below says why it is where it is, and {@code LookTest} holds both looks to WCAG contrast.
 *
 * <h2>Write the role, never the colour</h2>
 *
 * <p>The look can change while Rasterware runs. A node handed a {@link dev.vexelray.gui.core.style.Role} is repainted
 * in the new look, and fades to it; a node handed {@code gui.theme().color(role)} keeps the colour it was given,
 * forever. So the views write roles. A picture's own pixels are data and keep their colours in either look, and so is
 * the backdrop a picture is converted onto and a drawing layer's swatch.
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

    static final Palette DARK_PALETTE =
            new Palette(PAGE, STEP, INK, FADE, ACCENT, ACTION, DANGER, DEPTH, SHADOW_ALPHA);

    /**
     * Lit surfaces on, letterpress off.
     *
     * <p>The edge light is what gives a dark panel its glint. Letterpress is the opposite call: it buys
     * contrast for white-on-fill labels by spending crispness, which small text can least afford. Both are
     * decisions to make against a real design rather than defaults to leave alone.
     */
    static final Theme DARK = Theme.of(DARK_PALETTE, Shading.ON_DARK, Relief.STANDARD, true, false);

    // ---------------------------------------------------------------- the light look

    /** A near-white page on the brand's hue, tinted just enough that the two looks read as one family. */
    private static final Oklab LIGHT_PAGE = Oklab.polar(0.9750, 0.0060, HUE);

    /** Primary text: a deep blue slate rather than black, at the ink's hue, as the dark ink is the faintest blue. */
    private static final Oklab LIGHT_INK = Oklab.polar(0.2400, 0.0300, HUE);

    /** The brand's blue brought down from L 0.80 to 0.52, where it reads on white as text does, inside sRGB. */
    private static final Oklab LIGHT_ACCENT = Oklab.polar(0.5200, 0.1300, -114.91);

    /** A filled control's fill: deeper than the accent, so the white label light shading gives it has room. */
    private static final Oklab LIGHT_ACTION = Oklab.polar(0.4700, 0.1200, HUE);

    /** The close button's red, darker for the same reason as the accent, and still the one colour off the hue. */
    private static final Oklab LIGHT_DANGER = Oklab.polar(0.5300, 0.1800, 8.96);

    /** Shadows at the page's hue, a little lifted: on white, depth is a hint rather than a void. */
    private static final Oklab LIGHT_DEPTH = Oklab.polar(0.2000, 0.0120, HUE);

    /** Surfaces step down from the page, further than the dark look's 0.018: the eye parts near-whites less well. */
    private static final double LIGHT_STEP = 0.0250;

    /** The ink fades slower than in the dark look, so dim text keeps its contrast against a white page. */
    private static final double LIGHT_FADE = 0.300;

    /** Under a third of the dark look's: a shadow at 0.60 on white is a smudge. */
    private static final double LIGHT_SHADOW_ALPHA = 0.18;

    static final Palette LIGHT_PALETTE = new Palette(LIGHT_PAGE, LIGHT_STEP, LIGHT_INK, LIGHT_FADE, LIGHT_ACCENT,
            LIGHT_ACTION, LIGHT_DANGER, LIGHT_DEPTH, LIGHT_SHADOW_ALPHA);

    /** Light shading, and no edge light, which on a white panel reads as a smear rather than a glint. */
    static final Theme LIGHT = Theme.of(LIGHT_PALETTE, Shading.ON_LIGHT, Relief.STANDARD, false, false);

    // ---------------------------------------------------------------- roles of our own

    /** How much of a scanline shows over the lockup. */
    private static final float SCAN_ALPHA = 0.45f;

    /** Black, as the scanlines were drawn: on the dark page a line all but vanishes, and darkens the mark. */
    private static final Color DARK_SCANLINE = Color.rgba(0f, 0f, 0f, SCAN_ALPHA);

    /** The light page's own colour: a line vanishes on it as a black one does on the dark page, and lightens the mark. */
    private static final Color LIGHT_SCANLINE = Color.withAlpha(LIGHT_PAGE.toColor(), SCAN_ALPHA);

    /**
     * The splash's scanlines across the lockup (Splash): black lines on the light page would be a grey band across the
     * whole lockup, gaps and all, where on the dark page they show only over the mark. So a line is the page's own
     * extreme in each look, with an answer in each, and follows the person's choice.
     */
    static final Role SCANLINE = p -> p.equals(LIGHT_PALETTE) ? LIGHT_SCANLINE : DARK_SCANLINE;

    private Look() {
    }
}
