package dev.sibarum.pix;

import dev.vexelray.fields.sdg.SdgFile;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.core.ImageSdg;
import dev.vexelray.gui.core.Node;
import dev.vexelray.gui.core.layout.Length;
import dev.vexelray.gui.core.style.Paint;
import dev.vexelray.gui.core.style.Role;
import dev.vexelray.gui.widget.TitleBar;
import sibarum.probe.Log;

import java.io.IOException;
import java.io.InputStream;

/**
 * The name drawn as the suite canvas's wordmark: RASTERWARE in Press Start 2P's capitals, which sit on an eight-pixel
 * grid, so the whole word is {@link #W} by {@link #H} square pixels. It is those pixels and not the font: one drawing,
 * rather than a typeface carried for ten letters.
 *
 * <p>The pixels are drawn in {@code src/main/rc/wordmark.svg} (the face) and {@code wordmark-split.svg} (the tape the
 * splash wears), each pixel's square traced exactly, and the icon plugin's pair goal makes them SDGs,
 * {@code /wordmark.sdgb} and {@code /wordmark-split.sdgb}. So the mark is drawn as SDG: its pixels square and sharp at
 * any scale, a whole one or not.
 *
 * <p><b>Its face follows the theme.</b> In the title bar the face is in the role {@code tint}, the node's own text
 * colour, which the bar hands over as a paint ({@link TitleBar.Wordmark#ink}): resolved every frame, so a theme swapped
 * later reaches it and the pointer lifts it into the accent as it does a caption glyph. On the splash it is in the
 * theme's {@code ink}.
 */
final class Wordmark {

    private static final Log LOG = Log.of("pix.wordmark");

    /** The mark's size, in its own pixels. */
    static final int W = 79;
    static final int H = 7;

    /** One of its pixels on the screen: the canvas's title-bar size, a 12 px font of eight-pixel cells. */
    static final float PX = 1.5f;

    /** The tape's drawing, in units of a quarter pixel: two units each side for the ghosts, one under for the cyan. */
    static final int SPLIT_SCALE = 4;
    static final int SPLIT_W = W * SPLIT_SCALE + 4;
    static final int SPLIT_H = H * SPLIT_SCALE + 1;

    private Wordmark() {
    }

    /** The face's drawing, its pixels in the role {@code tint}; null, said once, if it is not on the class path. */
    static ImageSdg face() {
        return read("/wordmark.sdgb");
    }

    /** The tape's drawing: the red and the cyan ghost, screened where they cross, and the face in the theme's ink. */
    static ImageSdg split() {
        return read("/wordmark-split.sdgb");
    }

    /** A box showing the face, {@link #PX} dp to its pixel, lit in {@code ink}. */
    static Node node(Gui gui, Paint ink) {
        Node n = gui.box().role("wordmark").size(Length.dp(W * PX), Length.dp(H * PX)).textColor(ink);
        ImageSdg face = face();
        return face == null ? n : n.sdg(face);
    }

    /**
     * The wordmark for the title bar's lockup: the face, lit in whatever ink the bar hands it. The ink is the node's
     * text colour, a paint the renderer resolves in each frame's theme and fades as it fades text.
     */
    static TitleBar.Wordmark bar(Gui gui) {
        Node node = node(gui, Role.INK);
        return new TitleBar.Wordmark() {
            @Override
            public Node node() {
                return node;
            }

            @Override
            public void ink(Paint ink) {
                node.textColor(ink);
            }
        };
    }

    private static ImageSdg read(String name) {
        try (InputStream in = Wordmark.class.getResourceAsStream(name)) {
            if (in == null) {
                LOG.warn("no " + name + " on the class path (the icon plugin's pair goal makes it); no wordmark");
                return null;
            }
            return ImageSdg.of(SdgFile.read(in.readAllBytes()));
        } catch (IOException | RuntimeException e) {
            LOG.warn("the wordmark " + name + " could not be read; no wordmark", e);
            return null;
        }
    }
}
