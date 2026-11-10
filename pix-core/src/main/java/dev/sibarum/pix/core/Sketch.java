package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.EditMode;
import dev.sibarum.pix.core.Messages.Sizing;
import dev.vexelray.fields.sdg.Sdg;
import dev.vexelray.fields.sdg.Sdg.Group;
import dev.vexelray.fields.sdg.Sdg.Layer;
import dev.vexelray.fields.sdg.Sdg.Mask;
import dev.vexelray.fields.sdg.Sdg.Page;
import dev.vexelray.fields.sdg.Sdg.Paint;
import dev.vexelray.fields.sdg.Sdg.Stencil;

import java.util.List;

/**
 * A field being drawn by hand, turned into what is shown and what is saved (docs/sdg-editor.md). Arithmetic on an array
 * and nothing else: the {@link Sketcher} owns the array and calls these on its lane.
 *
 * <p><b>The field</b> is one float a texel, row by row from the top, in the encoding a stencil's texel has (sdg.md §2):
 * 1 the far inside, 0 the far outside, ½ the edge. A mask's alpha is the same number, so one array serves every mode,
 * and only what is saved differs.
 */
final class Sketch {

    /**
     * How far, in texels, a stencil's field reaches each side of its edge: what the tracer writes, so a drawing made
     * here and a traced one are drawn alike.
     */
    static final double RANGE = 8;

    /** The ink a drawing made here is painted in: black, or the theme's foreground where the reader has one. */
    static final Paint.Solid INK = new Paint.Solid(0x000000FF, "foreground");

    private Sketch() {
    }

    /** A field of {@code width × height} texels, all the far inside or all the far outside. */
    static float[] filled(int width, int height, boolean inside) {
        float[] field = new float[width * height];
        java.util.Arrays.fill(field, inside ? 1f : 0f);
        return field;
    }

    /**
     * The field as a grey picture, inside light, at most {@code side} px on its longer side, sampled at the nearest
     * texel: RGBA8, {@code [width, height, pixels]} as {@link Preview}.
     */
    static Preview preview(float[] field, int width, int height, int side) {
        double k = Math.min(1.0, side / (double) Math.max(width, height));
        int pw = Math.max(1, (int) Math.round(width * k));
        int ph = Math.max(1, (int) Math.round(height * k));
        byte[] rgba = new byte[pw * ph * 4];
        for (int y = 0; y < ph; y++) {
            int ty = Math.min(height - 1, (int) ((y + 0.5) * height / ph));
            for (int x = 0; x < pw; x++) {
                int tx = Math.min(width - 1, (int) ((x + 0.5) * width / pw));
                byte v = (byte) quantize(field[ty * width + tx]);
                int o = (y * pw + x) * 4;
                rgba[o] = v;
                rgba[o + 1] = v;
                rgba[o + 2] = v;
                rgba[o + 3] = (byte) 255;
            }
        }
        return new Preview(pw, ph, rgba);
    }

    /** A preview's size and its pixels. */
    record Preview(int width, int height, byte[] rgba) {
    }

    /**
     * The drawing the field is saved as, in {@code mode} (docs/sdg-editor.md, <i>The document</i>). One page of the
     * field's size: red is the field, green and blue are the far inside everywhere, alpha is the field for a mask and
     * opaque otherwise.
     *
     * <ul>
     *   <li>A heightmap or a stencil is one layer, its stencil the field (red), painted {@link #INK}.</li>
     *   <li>A mask is a group: one layer covering the canvas in {@link #INK} (its stencil the inside, green), masked by
     *   the {@code alpha} of a layer painted from the page, whose alpha is the field.</li>
     * </ul>
     */
    static Sdg drawing(float[] field, int width, int height, Sizing sizing, EditMode mode) {
        boolean mask = mode == EditMode.MASK;
        byte[] rgba = new byte[width * height * 4];
        for (int i = 0; i < width * height; i++) {
            int v = quantize(field[i]);
            int o = i * 4;
            rgba[o] = (byte) v;
            rgba[o + 1] = (byte) 255;
            rgba[o + 2] = (byte) 255;
            rgba[o + 3] = (byte) (mask ? v : 255);
        }
        double scale = sizing.scale();
        List<Page> pages = List.of(new Page(width, height, rgba));
        Stencil whole = new Stencil(Stencil.SDF, 0, 1, 0, 0, width, height, 0, 0, scale, RANGE);
        List<Layer> layers;
        if (mask) {
            Layer alpha = new Layer("alpha", whole, new Paint.Sampled(0, 0, 0, width, height, 0, 0, scale), 1f);
            Layer ink = new Layer("ink", whole, INK, 1f);
            layers = List.of(Layer.group("mask", 1f, new Group(List.of(ink), null, new Mask(Mask.ALPHA,
                    List.of(alpha)))));
        } else {
            Stencil edge = new Stencil(Stencil.SDF, 0, 0, 0, 0, width, height, 0, 0, scale, RANGE);
            layers = List.of(new Layer(mode == EditMode.HEIGHTMAP ? "height" : "stencil", edge, INK, 1f));
        }
        return new Sdg(sizing.width(), sizing.height(), pages, layers);
    }

    /** A field's value as a byte, 0 to 255, rounded. */
    static int quantize(float v) {
        return Math.round(Math.clamp(v, 0f, 1f) * 255f);
    }
}
