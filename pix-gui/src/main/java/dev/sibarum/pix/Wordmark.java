package dev.sibarum.pix;

import dev.vexelray.canvas.Color;
import dev.vexelray.canvas.ImageFilter;
import dev.vexelray.gui.core.Feed;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.core.Node;
import dev.vexelray.gui.core.layout.Length;
import dev.vexelray.gui.core.style.Paint;
import dev.vexelray.gui.core.style.Role;
import dev.vexelray.gui.widget.TitleBar;
import dev.vexelray.vulkan.present.MipChain;

/**
 * The name drawn as the suite canvas's wordmark: RASTERWARE in Press Start 2P's capitals, which sit on an eight-pixel
 * grid, so the whole word is a raster {@link #W} by {@link #H}. It is that raster and not the font: one picture, shown
 * with square pixels, rather than a typeface carried for ten letters.
 *
 * <p>The pixels were sampled from the font (SIL Open Font License, 1.1) and are written out below, a {@code #} to a
 * pixel lit, so a change to the mark is a change here that can be read in a diff. They are lit in the theme's ink, so
 * the mark follows the palette.
 *
 * <p>Drawn at {@link #PX} a pixel with {@code SHARP_BILINEAR}: square pixels at any scale, a whole one or not, with
 * only their edges blended, so the canvas's 12 px size (a pixel and a half) does not come out uneven.
 */
final class Wordmark {

    private static final String[] ROWS = {
            "######....###....#####...######.#######.######..##.#.##...###...######..#######",
            "##...##..##.##..##...##....##...##......##...##.##.#.##..##.##..##...##.##.....",
            "##...##.##...##.##.........##...##......##...##.##.#.##.##...##.##...##.##.....",
            "##..###.##...##..#####.....##...######..##..###.##.#.##.##...##.##..###.######.",
            "#####...#######......##....##...##......#####...#######.#######.#####...##.....",
            "##.###..##...##.##...##....##...##......##.###..###.###.##...##.##.###..##.....",
            "##..###.##...##..#####.....##...#######.##..###..#...#..##...##.##..###.#######",
    };

    /** The raster's size, in its own pixels. */
    static final int W = ROWS[0].length();
    static final int H = ROWS.length;

    /** One of its pixels on the screen: the canvas's title-bar size, a 12 px font of eight-pixel cells. */
    static final float PX = 1.5f;

    private Wordmark() {
    }

    /** The mark, its pixels lit in {@code ink} on nothing, as a still the size of the raster. */
    static Feed feed(Color ink) {
        byte r = channel(ink.r());
        byte g = channel(ink.g());
        byte b = channel(ink.b());
        byte[] rgba = new byte[W * H * 4];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                if (ROWS[y].charAt(x) == '#') {
                    int i = (y * W + x) * 4;
                    rgba[i] = r;
                    rgba[i + 1] = g;
                    rgba[i + 2] = b;
                    rgba[i + 3] = (byte) 255;
                }
            }
        }
        return Feed.still(W, H, true, MipChain.of(rgba, W, H));
    }

    /** A box showing the mark, {@link #PX} dp to its pixel, in the theme's ink. */
    static Node node(Gui gui, Color ink) {
        return gui.box().role("wordmark")
                .size(Length.dp(W * PX), Length.dp(H * PX))
                .image(feed(ink))
                .imageFilter(ImageFilter.SHARP_BILINEAR, W, H);
    }

    /**
     * The wordmark for the title bar's lockup: the raster, re-lit in whatever ink the bar hands it as the pointer comes
     * and goes. A raster per ink, made the first time that ink is asked for and kept, since the bar asks for the same
     * two or three over and over.
     *
     * <p>The bar hands over a paint, and a raster cannot hold one: its pixels are lit in a colour. So the paint is
     * resolved against the theme drawing when the bar hands it over, and the raster keeps that colour until the bar
     * hands over the next.
     */
    static TitleBar.Wordmark bar(Gui gui) {
        Node node = node(gui, gui.theme().color(Role.INK));
        java.util.Map<Color, Feed> lit = new java.util.concurrent.ConcurrentHashMap<>();
        return new TitleBar.Wordmark() {
            @Override
            public Node node() {
                return node;
            }

            @Override
            public void ink(Paint ink) {
                node.image(lit.computeIfAbsent(ink.in(gui.theme()), Wordmark::feed));
            }
        };
    }

    /** The worn tape's two ghosts, from the canvas's lockup: red a little left, cyan a little right and down. */
    static final Color SPLIT_RED = Color.rgb(0xff3b6b);
    static final Color SPLIT_CYAN = Color.rgb(0x38e8ff);

    /** The lockup's split mark: four texels to a pixel of the raster, so the ghosts can sit half and a quarter off. */
    static final int SPLIT_SCALE = 4;
    static final int SPLIT_W = W * SPLIT_SCALE + 4;
    static final int SPLIT_H = H * SPLIT_SCALE + 1;

    /** How far each ghost is drawn. */
    private static final float SPLIT_ALPHA = 0.85f;

    /**
     * The mark as the lockup draws it (the canvas's "light distortion"): the red and the cyan ghost screened together
     * two texels either side, the cyan one down a texel, and the face over both in {@code face}. Screened as over
     * black, which the page all but is, and handed over with straight alpha, so drawn over the page it comes out as
     * the canvas's screen blend does.
     */
    static Feed split(Color face) {
        byte[] rgba = new byte[SPLIT_W * SPLIT_H * 4];
        for (int y = 0; y < SPLIT_H; y++) {
            for (int x = 0; x < SPLIT_W; x++) {
                boolean red = lit(x, y);
                boolean cyan = lit(x - 4, y - 1);
                boolean lit = lit(x - 2, y);
                float a = 1f - (red ? 1f - SPLIT_ALPHA : 1f) * (cyan ? 1f - SPLIT_ALPHA : 1f);
                if (a <= 0f) {
                    continue;
                }
                int i = (y * SPLIT_W + x) * 4;
                if (lit) {
                    rgba[i] = channel(face.r());
                    rgba[i + 1] = channel(face.g());
                    rgba[i + 2] = channel(face.b());
                    rgba[i + 3] = (byte) 255;
                    continue;
                }
                rgba[i] = channel(screen(red, SPLIT_RED.r(), cyan, SPLIT_CYAN.r()) / a);
                rgba[i + 1] = channel(screen(red, SPLIT_RED.g(), cyan, SPLIT_CYAN.g()) / a);
                rgba[i + 2] = channel(screen(red, SPLIT_RED.b(), cyan, SPLIT_CYAN.b()) / a);
                rgba[i + 3] = channel(a);
            }
        }
        return Feed.still(SPLIT_W, SPLIT_H, true, MipChain.of(rgba, SPLIT_W, SPLIT_H));
    }

    /** Whether texel {@code (x, y)} of the face, placed at the split's origin, is a lit pixel of the raster. */
    private static boolean lit(int x, int y) {
        if (x < 0 || y < 0 || x >= W * SPLIT_SCALE || y >= H * SPLIT_SCALE) {
            return false;
        }
        return ROWS[y / SPLIT_SCALE].charAt(x / SPLIT_SCALE) == '#';
    }

    /** The two ghosts' channel screened over black, premultiplied. */
    private static float screen(boolean red, float r, boolean cyan, float c) {
        return 1f - (1f - (red ? r * SPLIT_ALPHA : 0f)) * (1f - (cyan ? c * SPLIT_ALPHA : 0f));
    }

    private static byte channel(float v) {
        return (byte) Math.round(Math.max(0f, Math.min(1f, v)) * 255f);
    }
}
