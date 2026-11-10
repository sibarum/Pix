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
     * The field as a picture at most {@code side} px on its longer side, sampled at the nearest texel: RGBA8, as
     * {@link Preview}. In grey, inside light; or, {@code over} a picture traced, as a shade over the outside that lets
     * the inside show the picture through ({@link #paint}).
     */
    static Preview preview(float[] field, int width, int height, int side, boolean over) {
        double k = Math.min(1.0, side / (double) Math.max(width, height));
        int pw = Math.max(1, (int) Math.round(width * k));
        int ph = Math.max(1, (int) Math.round(height * k));
        Preview p = new Preview(pw, ph, new byte[pw * ph * 4]);
        paint(field, width, height, p, new Rect(0, 0, width, height), over);
        return p;
    }

    /** A preview's size and its pixels. */
    record Preview(int width, int height, byte[] rgba) {
    }

    /** The darkest a shade over the outside goes, of 255: enough to see the picture under it, dimmed. */
    static final int SHADE = 190;

    /**
     * The preview's pixels over {@code dirty}, a rectangle of texels, made again from the field: the part a dab changed.
     * Grey, inside light and opaque; or {@code over} a picture, the outside shaded in near black and the inside clear.
     */
    static void paint(float[] field, int width, int height, Preview p, Rect dirty, boolean over) {
        int pw = p.width();
        int ph = p.height();
        int px0 = Math.max(0, (int) Math.floor(dirty.x0() * (double) pw / width) - 1);
        int py0 = Math.max(0, (int) Math.floor(dirty.y0() * (double) ph / height) - 1);
        int px1 = Math.min(pw, (int) Math.ceil(dirty.x1() * (double) pw / width) + 1);
        int py1 = Math.min(ph, (int) Math.ceil(dirty.y1() * (double) ph / height) + 1);
        byte[] rgba = p.rgba();
        for (int y = py0; y < py1; y++) {
            int ty = Math.min(height - 1, (int) ((y + 0.5) * height / ph));
            for (int x = px0; x < px1; x++) {
                int tx = Math.min(width - 1, (int) ((x + 0.5) * width / pw));
                int v = quantize(field[ty * width + tx]);
                int o = (y * pw + x) * 4;
                if (over) {
                    rgba[o] = 10;
                    rgba[o + 1] = 12;
                    rgba[o + 2] = 16;
                    rgba[o + 3] = (byte) ((255 - v) * SHADE / 255);
                } else {
                    rgba[o] = (byte) v;
                    rgba[o + 1] = (byte) v;
                    rgba[o + 2] = (byte) v;
                    rgba[o + 3] = (byte) 255;
                }
            }
        }
    }

    /** A rectangle of texels, {@code [x0, x1) × [y0, y1)}; empty when either side is not positive. */
    record Rect(int x0, int y0, int x1, int y1) {

        static final Rect NONE = new Rect(0, 0, 0, 0);

        boolean empty() {
            return x1 <= x0 || y1 <= y0;
        }

        Rect union(Rect o) {
            if (empty()) {
                return o;
            }
            if (o.empty()) {
                return this;
            }
            return new Rect(Math.min(x0, o.x0), Math.min(y0, o.y0), Math.max(x1, o.x1), Math.max(y1, o.y1));
        }
    }

    /**
     * What one stroke has done so far, so its dabs do not pile up: for each texel, its value when the stroke first
     * reached it ({@code base}) and the most of the brush that has reached it since ({@code reach}, 0 for not yet).
     * A texel is the base moved toward the brush's value by the strength times its reach, so a stroke at 50 % makes
     * 50 % wherever it passes, however many of its dabs overlap there; a second stroke goes on from there. Two arrays the
     * field's size, kept for its life and cleared over what the last stroke touched when the next begins.
     */
    record Stroke(float[] base, float[] reach) {

        static Stroke of(int width, int height) {
            return new Stroke(new float[width * height], new float[width * height]);
        }

        /** A new stroke: nothing reached over {@code last}, where the stroke before reached. */
        void begin(int width, Rect last) {
            for (int y = last.y0(); y < last.y1(); y++) {
                java.util.Arrays.fill(reach, y * width + last.x0(), y * width + last.x1(), 0f);
            }
        }
    }

    /**
     * One dab of {@code brush} at {@code (cx, cy)}, texels from the field's top left (a texel's centre is at its index
     * and a half), as part of {@code stroke}: each texel within the radius is reached by the falloff there, whole
     * inside the hardness and easing to nothing at the rim, and moves from its base toward the brush's value, 1 or 0,
     * by the strength times the most that has reached it ({@link Stroke}). Returns the texels it touched.
     */
    static Rect dab(float[] field, Stroke stroke, int width, int height, Messages.Brush brush, float cx, float cy) {
        float r = brush.radius();
        int x0 = Math.max(0, (int) Math.floor(cx - r));
        int y0 = Math.max(0, (int) Math.floor(cy - r));
        int x1 = Math.min(width, (int) Math.ceil(cx + r));
        int y1 = Math.min(height, (int) Math.ceil(cy + r));
        Rect touched = new Rect(x0, y0, x1, y1);
        if (touched.empty()) {
            return Rect.NONE;
        }
        float target = brush.erase() ? 0f : 1f;
        float hard = brush.hardness();
        float[] base = stroke.base();
        float[] reach = stroke.reach();
        for (int y = y0; y < y1; y++) {
            float dy = y + 0.5f - cy;
            for (int x = x0; x < x1; x++) {
                float dx = x + 0.5f - cx;
                float t = (float) Math.sqrt(dx * dx + dy * dy) / r;
                if (t >= 1f) {
                    continue;
                }
                float w = falloff(t, hard);
                int i = y * width + x;
                if (w <= reach[i]) {
                    continue;
                }
                if (reach[i] == 0f) {
                    base[i] = field[i];
                }
                reach[i] = w;
                field[i] = base[i] + (target - base[i]) * brush.strength() * w;
            }
        }
        return touched;
    }

    /** How much of a dab reaches {@code t} of the way to its rim: whole within {@code hard}, then a smooth step down. */
    static float falloff(float t, float hard) {
        if (t <= hard) {
            return 1f;
        }
        float s = 1f - (t - hard) / (1f - hard);
        return s * s * (3f - 2f * s);
    }

    /**
     * Dabs from {@code (fx, fy)} to {@code (tx, ty)}, a quarter of the radius apart and the last at the end, not the
     * first, which the dab before it made. Returns the texels they touched.
     */
    static Rect line(float[] field, Stroke stroke, int width, int height, Messages.Brush brush, float fx, float fy,
                     float tx, float ty) {
        float spacing = Math.max(1f, brush.radius() / 4f);
        double length = Math.hypot(tx - fx, ty - fy);
        int steps = Math.max(1, (int) Math.ceil(length / spacing));
        Rect touched = Rect.NONE;
        for (int s = 1; s <= steps; s++) {
            float k = s / (float) steps;
            touched = touched.union(dab(field, stroke, width, height, brush, fx + (tx - fx) * k,
                    fy + (ty - fy) * k));
        }
        return touched;
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
