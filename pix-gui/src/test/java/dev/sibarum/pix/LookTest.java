package dev.sibarum.pix;

import dev.vexelray.canvas.Color;
import dev.vexelray.gui.core.style.Oklab;
import dev.vexelray.gui.core.style.Role;
import dev.vexelray.gui.core.style.Theme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Both looks, each held to what a look has to be true of, whichever way its page faces.
 *
 * <p>Worth having from the first day rather than the day a colour goes wrong, because a palette is derived:
 * one anchor moved by a tenth changes every surface and every shade of text at once, and nothing on screen
 * says which change did it. These are the checks that would have caught it.
 *
 * <p>Every check is written as a <b>distance from the page</b>, not as "lighter" or "darker", so one test holds the
 * dark look and the light one alike. Contrast is WCAG's ratio: 4.5 for body text, 3 for what only has to be seen.
 * Its own role, the splash's scanline, is checked to vanish on its page in each look. Otherwise it paints framework roles:
 * the ones it writes text and marks in (ink, dim, accent, danger; the accent as the playhead and a picked layer's ring).
 *
 * <p>The dark look has a design (the viewer mockup of 2026-11-01, turned to the blue of the suite canvas's branding),
 * so the last test <b>pins the gap between the design and the construction</b>: each authored colour against what the
 * palette derives, with the difference written down. A gap that is measured is a known quantity; a gap that is absorbed
 * is a surprise waiting for a review. The light look has no design yet, so it has no such test.
 */
class LookTest {

    static Stream<Theme> looks() {
        return Stream.of(Look.DARK, Look.LIGHT);
    }

    @ParameterizedTest
    @MethodSource("looks")
    void theInkReadsAgainstThePage(Theme look) {
        assertReads(look, Role.INK, 7.0, "primary text");
        assertReads(look, Role.DIM, 4.5, "dim text: the bar's details, a panel's labels, the splash's key");
    }

    /** Every other text colour Rasterware writes, against the page, and dim against the bar it mostly sits on. */
    @ParameterizedTest
    @MethodSource("looks")
    void everyTextColourItWritesReads(Theme look) {
        assertReads(look, Role.ACCENT, 4.5, "the accent: the name under the pointer, an open badge");
        assertReads(look, Role.DANGER, 4.5, "danger: a status that says what went wrong");
        assertTrue(contrast(look.color(Role.DIM), look.color(Role.CHROME)) >= 4.5,
                "dim text should read on the bar it sits on");
    }

    /** What only has to be seen: the playhead on the seek bar's track, and a picked layer's ring on a panel. */
    @ParameterizedTest
    @MethodSource("looks")
    void theMarksItDrawsAreSeen(Theme look) {
        assertTrue(contrast(look.color(Role.ACCENT), look.color(Role.CHROME)) >= 3.0,
                "the accent should be seen on a panel, as a picked layer's ring is");
        assertNotEquals(look.color(Role.ACCENT), look.color(Role.DIM),
                "the playhead should not vanish into the track it runs on");
    }

    @ParameterizedTest
    @MethodSource("looks")
    void surfacesStepAwayFromThePage(Theme look) {
        float page = luminance(look.color(Role.PAGE));
        float panel = Math.abs(luminance(look.color(Role.PANEL)) - page);
        float raised = Math.abs(luminance(look.color(Role.RAISED)) - page);
        assertTrue(panel > 0, "a panel should stand off the page");
        assertTrue(raised > panel, "a raised surface should stand further off the page than a panel");
    }

    @ParameterizedTest
    @MethodSource("looks")
    void textFadesInSteps(Theme look) {
        float page = luminance(look.color(Role.PAGE));
        float ink = Math.abs(luminance(look.color(Role.INK)) - page);
        float dim = Math.abs(luminance(look.color(Role.DIM)) - page);
        float faint = Math.abs(luminance(look.color(Role.FAINT)) - page);
        assertTrue(ink > dim, "dim text should be quieter than primary text");
        assertTrue(dim > faint, "faint text should be quieter than dim text");
    }

    @ParameterizedTest
    @MethodSource("looks")
    void theAccentIsNotTheInk(Theme look) {
        assertNotEquals(look.color(Role.INK), look.color(Role.ACCENT),
                "the accent should be a colour, not the text shade");
    }

    /** The two looks are two, facing opposite ways. */
    @ParameterizedTest
    @MethodSource("looks")
    void theLooksFaceOppositeWays(Theme look) {
        float page = luminance(look.color(Role.PAGE));
        float ink = luminance(look.color(Role.INK));
        assertTrue(look == Look.DARK ? ink > page : ink < page,
                "the dark look's ink is above its page, the light one's below");
    }

    /**
     * The splash's scanlines, Rasterware's one role of its own: an answer in each look, each all but invisible on its
     * own page (so the lines show only across the mark), and keeping the dark look's black exactly as it was drawn.
     */
    @ParameterizedTest
    @MethodSource("looks")
    void theScanlinesVanishOnThePage(Theme look) {
        Color line = look.color(Look.SCANLINE);
        Color page = look.color(Role.PAGE);
        Color over = Color.rgba(page.r() + (line.r() - page.r()) * line.a(), page.g() + (line.g() - page.g()) * line.a(),
                page.b() + (line.b() - page.b()) * line.a(), 1f);
        assertTrue(contrast(over, page) < 1.3, "a scanline should all but vanish on the page it lies on");
        assertNotEquals(Look.DARK.color(Look.SCANLINE), Look.LIGHT.color(Look.SCANLINE),
                "the scanline has an answer in each look");
        assertTrue(Look.DARK.color(Look.SCANLINE).equals(Color.rgba(0f, 0f, 0f, 0.45f)),
                "the dark look's scanline is the black it was drawn in");
    }

    /** Both pages on the brand's hue, so the two looks read as one family. */
    @ParameterizedTest
    @MethodSource("looks")
    void thePageSitsOnTheBrandsHue(Theme look) {
        Oklab page = Oklab.of(look.color(Role.PAGE));
        double hue = Math.toDegrees(Math.atan2(page.b(), page.a()));
        assertTrue(Math.abs(hue - Look.HUE) < 15.0, "the page's hue is " + hue + "°, not the brand's " + Look.HUE);
    }

    /**
     * The viewer mockup's colours turned to the brand's hue, each against the role the dark palette derives for it, and
     * how far apart the two may be in Oklab. Most are anchors, so all but equal; the climbing surfaces and the dim ink
     * are the ladder's, and their allowance is the chroma the ladder does not climb with; the fill's is the chroma
     * sRGB does not hold at this hue (Look's javadoc). The accent is the brand's blue itself, and the close button's
     * red is left where it was drawn.
     */
    @Test
    void theMockupsColoursTurnedToTheBrandsHueAreWhatTheDarkPaletteDerives() {
        assertAll(
                near(turned(0x0d0a11), Role.PAGE, 0.001),
                near(turned(0x120d17), Role.CHROME, 0.006),
                near(turned(0x1b1421), Role.RAISED, 0.009),
                near(turned(0xf1eaf5), Role.INK, 0.001),
                near(turned(0x9b8fa8), Role.DIM, 0.025),
                near(Oklab.of(Color.rgb(0x80c5ff)), Role.ACCENT, 0.001),
                near(turned(0xc4528f), Role.ACTION, 0.021),
                near(Oklab.of(Color.rgb(0xe0476f)), Role.DANGER, 0.001));
    }

    /** {@code hex} at its own lightness and chroma, on the brand's hue. */
    private static Oklab turned(int hex) {
        Oklab o = Oklab.of(Color.rgb(hex));
        return Oklab.polar(o.l(), Math.hypot(o.a(), o.b()), Look.HUE);
    }

    private static Executable near(Oklab drawn, Role role, double allowed) {
        return () -> {
            Oklab derived = Oklab.of(Look.DARK.color(role));
            double d = Math.sqrt(Math.pow(drawn.l() - derived.l(), 2) + Math.pow(drawn.a() - derived.a(), 2)
                    + Math.pow(drawn.b() - derived.b(), 2));
            assertTrue(d <= allowed, String.format("%s's %s is %.4f from what the palette derives, past %.4f",
                    role, drawn, d, allowed));
        };
    }

    private static void assertReads(Theme look, Role role, double least, String what) {
        double ratio = contrast(look.color(role), look.color(Role.PAGE));
        assertTrue(ratio >= least, what + " is " + String.format("%.2f", ratio) + ":1 against the page in the "
                + (look == Look.DARK ? "dark" : "light") + " look; it needs " + least + ":1");
    }

    /** WCAG's contrast ratio between two colours. */
    private static double contrast(Color a, Color b) {
        double la = relative(a);
        double lb = relative(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    /** WCAG's relative luminance, from sRGB. */
    private static double relative(Color c) {
        return 0.2126 * linear(c.r()) + 0.7152 * linear(c.g()) + 0.0722 * linear(c.b());
    }

    private static double linear(float channel) {
        return channel <= 0.04045 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
    }

    /** Rough perceived brightness -- enough to order two shades, which is all the ordering checks need. */
    private static float luminance(Color c) {
        return 0.2126f * c.r() + 0.7152f * c.g() + 0.0722f * c.b();
    }
}
