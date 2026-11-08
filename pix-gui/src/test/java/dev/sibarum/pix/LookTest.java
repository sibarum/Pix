package dev.sibarum.pix;

import dev.vexelray.canvas.Color;
import dev.vexelray.gui.core.style.Oklab;
import dev.vexelray.gui.core.style.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The palette, held to what a dark theme has to be true of.
 *
 * <p>Worth having from the first day rather than the day a colour goes wrong, because a palette is derived:
 * one anchor moved by a tenth changes every surface and every shade of text at once, and nothing on screen
 * says which change did it. These are the checks that would have caught it.
 *
 * <p>Rasterware has a design now (the viewer mockup of 2026-11-01, turned to the blue of the suite canvas's branding),
 * so the last test <b>pins the gap between the design and the construction</b>: each authored colour against what the
 * palette derives, with the difference written down. A gap that is measured is a known quantity; a gap that is absorbed is a surprise waiting for a
 * review.
 */
class LookTest {

    @Test
    void theInkReadsAgainstThePage() {
        Color page = Look.THEME.color(Role.PAGE);
        Color ink = Look.THEME.color(Role.INK);
        assertTrue(luminance(ink) - luminance(page) > 0.5f,
                "primary text should be far lighter than the page it sits on");
    }

    @Test
    void surfacesClimbAwayFromThePage() {
        float page = luminance(Look.THEME.color(Role.PAGE));
        float panel = luminance(Look.THEME.color(Role.PANEL));
        float raised = luminance(Look.THEME.color(Role.RAISED));
        assertTrue(panel > page, "a panel should sit above the page");
        assertTrue(raised > panel, "a raised surface should sit above a panel");
    }

    @Test
    void textFadesInSteps() {
        float ink = luminance(Look.THEME.color(Role.INK));
        float dim = luminance(Look.THEME.color(Role.DIM));
        float faint = luminance(Look.THEME.color(Role.FAINT));
        assertTrue(ink > dim, "dim text should be quieter than primary text");
        assertTrue(dim > faint, "faint text should be quieter than dim text");
    }

    @Test
    void theAccentIsNotTheInk() {
        assertNotEquals(Look.THEME.color(Role.INK), Look.THEME.color(Role.ACCENT),
                "the accent should be a colour, not the text shade");
    }

    /**
     * The viewer mockup's colours turned to the brand's hue, each against the role the palette derives for it, and
     * how far apart the two may be in Oklab. Most are anchors, so all but equal; the climbing surfaces and the dim ink
     * are the ladder's, and their allowance is the chroma the ladder does not climb with; the fill's is the chroma
     * sRGB does not hold at this hue (Look's javadoc). The accent is the brand's blue itself, and the close button's
     * red is left where it was drawn.
     */
    @Test
    void theMockupsColoursTurnedToTheBrandsHueAreWhatThePaletteDerives() {
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
            Oklab derived = Oklab.of(Look.THEME.color(role));
            double d = Math.sqrt(Math.pow(drawn.l() - derived.l(), 2) + Math.pow(drawn.a() - derived.a(), 2)
                    + Math.pow(drawn.b() - derived.b(), 2));
            assertTrue(d <= allowed, String.format("%s's %s is %.4f from what the palette derives, past %.4f",
                    role, drawn, d, allowed));
        };
    }

    /** Rough perceived brightness -- enough to order two shades, which is all these assertions need. */
    private static float luminance(Color c) {
        return 0.2126f * c.r() + 0.7152f * c.g() + 0.0722f * c.b();
    }
}
