package dev.sibarum.pix;

import dev.vexelray.canvas.Color;
import dev.vexelray.canvas.ImageFilter;
import dev.vexelray.gui.core.Feed;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.core.Node;
import dev.vexelray.gui.core.layout.Length;
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

    private static byte channel(float v) {
        return (byte) Math.round(Math.max(0f, Math.min(1f, v)) * 255f);
    }
}
