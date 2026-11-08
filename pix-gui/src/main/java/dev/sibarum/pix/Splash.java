package dev.sibarum.pix;

import dev.vexelray.canvas.ImageFilter;
import dev.vexelray.gui.core.Feed;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.vulkan.present.MipChain;
import dev.vexelray.gui.core.Node;
import dev.vexelray.gui.core.layout.LayoutEnums.AlignItems;
import dev.vexelray.gui.core.layout.Length;
import dev.vexelray.gui.core.style.Role;
import dev.vexelray.gui.core.ImageSdg;
import dev.vexelray.fields.sdg.SdgFile;
import dev.vexelray.gui.widget.Button;
import sibarum.probe.Log;

import java.io.IOException;
import java.io.InputStream;

/**
 * What the stage shows while no picture is wanted: the lockup (the mark and the wordmark, worn as the suite canvas's
 * lockup is) and a button that opens a picture. That is the first thing most people see, since Windows keeps pictures
 * in folders under Pictures and not in it, so a launch with no file finds none.
 *
 * <p>The mark is the drawing itself, {@code /pix.sdgb} as the icon plugin makes it from {@code src/main/rc/pix.svg}
 * (the drawing the About window's art pair and the {@code .ico} are converted from too), drawn as SDG: sharp at any
 * display scale. Read from the classpath, which in the native
 * image is the binary itself, once, while the tree is built.
 */
final class Splash {

    private static final Log LOG = Log.of("pix.splash");

    /** The mark's side, and the room between it and the wordmark, as the canvas's lockup has them. */
    static final Length MARK = Length.dp(72f);
    static final Length LOCKUP_GAP = Length.dp(22f);

    /** Scanlines: one dark texel row in three, drawn a dp to the texel over the lockup. */
    static final int SCAN_PERIOD = 3;
    private static final float SCAN_ALPHA = 0.45f;

    /** As many rows as the lockup is dp tall, which is the mark's height. */
    static final int SCAN_ROWS = 72;

    /** The text beside the button saying the key that does the same. */
    static final String KEY = "Ctrl+O";

    private final Node node;
    private final Button open;

    Splash(Gui gui) {
        Node mark = gui.box().size(MARK, MARK);
        ImageSdg art = mark();
        if (art != null) {
            mark.sdg(art);
        }
        Node word = gui.box().role("wordmark")
                .size(Length.dp(Wordmark.SPLIT_W), Length.dp(Wordmark.SPLIT_H));
        ImageSdg tape = Wordmark.split();
        if (tape != null) {
            word.sdg(tape);
        }
        Node scan = gui.box().width(Length.percent(100f)).height(Length.percent(100f)).hitInert(true)
                .floatAt(Length.ZERO, Length.ZERO)
                .image(scanlines(SCAN_ROWS)).imageFilter(ImageFilter.SHARP_BILINEAR, 1, SCAN_ROWS);
        Node lockup = gui.row().alignItems(AlignItems.CENTER).gap(LOCKUP_GAP).scroll(false, false)
                .children(mark, word, scan);

        open = new Button(gui, Ui.OPEN_A_PICTURE).kind(Button.Kind.PRIMARY);
        Node key = gui.text(KEY).wordWrap(false).textSize(Ui.DETAIL_TEXT).textColor(gui.theme().color(Role.DIM));
        Node ask = gui.row().alignItems(AlignItems.CENTER).gap(Ui.MARGIN).scroll(false, false)
                .children(open.node(), key);

        node = gui.column().alignItems(AlignItems.CENTER).gap(Length.rem(2f))
                .scroll(false, false).children(lockup, ask);
        gui.landmark(Landmarks.SPLASH, node);
        gui.landmark(Landmarks.SPLASH_OPEN, open.node());
    }

    Node node() {
        return node;
    }

    /** The address its button presses with. */
    String openAddress() {
        return open.address();
    }

    /** The mark's drawing, {@code /pix.sdgb}; none, said once, if it is missing or does not read. */
    static ImageSdg mark() {
        try (InputStream in = Splash.class.getResourceAsStream("/pix.sdgb")) {
            if (in == null) {
                LOG.warn("no /pix.sdgb on the classpath; the splash shows the wordmark alone");
                return null;
            }
            return ImageSdg.of(SdgFile.read(in.readAllBytes()));
        } catch (IOException | RuntimeException e) {
            LOG.warn("the splash's mark could not be read; it shows the wordmark alone", e);
            return null;
        }
    }

    /** {@code rows} texel rows, one in {@link #SCAN_PERIOD} a dark line and the rest clear: the tape's scanlines. */
    static Feed scanlines(int rows) {
        byte[] rgba = new byte[rows * 4];
        for (int y = 0; y < rows; y += SCAN_PERIOD) {
            rgba[y * 4 + 3] = (byte) Math.round(SCAN_ALPHA * 255f);
        }
        return Feed.still(1, rows, true, MipChain.of(rgba, 1, rows));
    }
}
