package dev.sibarum.pix.core;

import dev.vexelray.fields.Field;
import dev.vexelray.fields.FieldKind;
import dev.vexelray.fields.Framing;
import dev.vexelray.fields.convert.Convert;
import dev.vexelray.fields.sdg.Sdg;
import dev.vexelray.fields.shape.Bounds;
import dev.vexelray.fields.shape.Shape;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A prototype (FN-22): a trace whose edges are curves with their corners kept, made from parts the stack already has.
 * The stack's tracer finds the regions and cuts each one's edge between the pixels as a distance field. Here each
 * layer's field is contoured and fitted ({@link CornerFit}), the curves made a {@code Shape}, and the shape stored as a
 * multi-channel field ({@code Convert.shapeToMsdf}), whose three channels keep a corner sharp at any zoom however few
 * its texels. The layers' fields are packed onto one page, in the tracer's order.
 *
 * <p>Only solid layers: a layer painted as a texture is left out, so this is a trace with Gradients flattened.
 */
final class CornerTrace {

    /** The stencil byte at a region's edge; above it is inside. */
    static final float EDGE = 127.5f;

    /** Where a texel's sample sits in it, in texels from its corner: the tracer samples a texel at its centre. */
    static final double SAMPLE_AT = 0.5;

    /** Texels round each layer's field, and the distance a field spans, in texels. */
    static final int MARGIN = 4;
    static final double RANGE = 4;

    /** The width the layers' fields are packed into, unless one is wider. */
    static final int PAGE = 1024;

    private CornerTrace() {
    }

    /** {@code traced}'s solid layers fitted, each stored as an MSDF at {@code density} texels a unit. */
    static Sdg fitted(Sdg traced, CornerFit.Options o, double density) {
        List<Sdg.Layer> layers = new ArrayList<>();
        List<Placed> placed = new ArrayList<>();
        for (Sdg.Layer l : traced.layers()) {
            if (!(l.paint() instanceof Sdg.Paint.Solid paint) || !Sdg.Stencil.SDF.equals(l.stencil().kind())) {
                continue;
            }
            Shape shape = shape(traced, l.stencil(), o);
            if (shape.contours().isEmpty()) {
                continue;
            }
            Bounds b = shape.bounds();
            int w = (int) Math.ceil((b.xMax() - b.xMin()) * density) + 2 * MARGIN;
            int h = (int) Math.ceil((b.yMax() - b.yMin()) * density) + 2 * MARGIN;
            Framing f = new Framing(density, b.xMin() - MARGIN / density, b.yMin() - MARGIN / density, false);
            Field field = Convert.shapeToMsdf(f, w, h).apply(shape);
            Sdg one = Convert.fieldToSdg(FieldKind.MSDF, RANGE, paint).apply(field);
            Sdg.Stencil st = one.layers().getFirst().stencil();
            placed.add(new Placed(one.pages().get(st.page()), st));
            layers.add(new Sdg.Layer(l.name(), new Sdg.Stencil(Sdg.Stencil.MSDF, 0, st.channel(), 0, 0, st.width(),
                    st.height(), f.originX(), f.originY(), f.scale(), st.range()), paint, l.opacity()));
        }
        Sdg.Page page = pack(placed, layers);
        return new Sdg(traced.width(), traced.height(), page == null ? List.of() : List.of(page), layers);
    }

    /** A layer's field on the page it came on, before packing. */
    private record Placed(Sdg.Page page, Sdg.Stencil stencil) {
    }

    /** The fields onto one page, in rows; each layer's stencil moved to where its field went. */
    private static Sdg.Page pack(List<Placed> placed, List<Sdg.Layer> layers) {
        if (placed.isEmpty()) {
            return null;
        }
        int width = PAGE;
        for (Placed p : placed) {
            width = Math.max(width, p.stencil().width());
        }
        int[] xs = new int[placed.size()], ys = new int[placed.size()];
        int x = 0, y = 0, row = 0;
        for (int i = 0; i < placed.size(); i++) {
            Sdg.Stencil s = placed.get(i).stencil();
            if (x + s.width() > width) {
                x = 0;
                y += row;
                row = 0;
            }
            xs[i] = x;
            ys[i] = y;
            x += s.width();
            row = Math.max(row, s.height());
        }
        int height = y + row;
        byte[] rgba = new byte[width * height * 4];
        for (int i = 0; i < placed.size(); i++) {
            Placed p = placed.get(i);
            Sdg.Stencil s = p.stencil();
            for (int r = 0; r < s.height(); r++) {
                System.arraycopy(p.page().rgba(), ((s.y() + r) * p.page().width() + s.x()) * 4, rgba,
                        ((ys[i] + r) * width + xs[i]) * 4, s.width() * 4);
            }
            Sdg.Layer l = layers.get(i);
            Sdg.Stencil t = l.stencil();
            layers.set(i, new Sdg.Layer(l.name(), new Sdg.Stencil(t.kind(), 0, t.channel(), xs[i], ys[i], t.width(),
                    t.height(), t.originX(), t.originY(), t.scale(), t.range()), l.paint(), l.opacity()));
        }
        return new Sdg.Page(width, height, rgba);
    }

    /** A layer's edge fitted, as a shape in the drawing's units. */
    static Shape shape(Sdg traced, Sdg.Stencil st, CornerFit.Options o) {
        Shape.Builder b = Shape.builder();
        for (double[] ring : rings(traced, st)) {
            CornerFit.Outline out = CornerFit.fit(ring, o);
            b.moveTo(out.startX(), out.startY());
            for (double[] p : out.pieces()) {
                if (p.length == 2) {
                    b.lineTo(p[0], p[1]);
                } else {
                    b.cubicTo(p[0], p[1], p[2], p[3], p[4], p[5]);
                }
            }
            b.close();
        }
        return b.build();
    }

    /** {@code traced}'s solid layers fitted, as SVG: to look at the curves themselves. */
    static String svg(Sdg traced, CornerFit.Options o) {
        StringBuilder b = new StringBuilder();
        b.append(String.format(Locale.ROOT,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%s\" height=\"%s\" viewBox=\"0 0 %s %s\">\n",
                traced.width(), traced.height(), traced.width(), traced.height()));
        for (Sdg.Layer l : traced.layers()) {
            if (!(l.paint() instanceof Sdg.Paint.Solid paint) || !Sdg.Stencil.SDF.equals(l.stencil().kind())) {
                continue;
            }
            StringBuilder d = new StringBuilder();
            for (double[] ring : rings(traced, l.stencil())) {
                d.append(CornerFit.fit(ring, o).svg());
            }
            if (d.isEmpty()) {
                continue;
            }
            int rgba = paint.rgba();
            b.append(String.format(Locale.ROOT, "<path fill=\"#%06x\" fill-opacity=\"%.3f\" fill-rule=\"evenodd\" d=\"%s\"/>\n",
                    rgba >>> 8, ((rgba & 0xff) / 255.0) * l.opacity(), d));
        }
        return b.append("</svg>\n").toString();
    }

    /** The stencil's level lines, in the drawing's units. */
    static List<double[]> rings(Sdg sdg, Sdg.Stencil st) {
        Sdg.Page page = sdg.pages().get(st.page());
        int w = st.width(), h = st.height();
        float[] v = new float[w * h];
        byte[] px = page.rgba();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                v[y * w + x] = px[((st.y() + y) * page.width() + st.x() + x) * 4 + st.channel()] & 0xff;
            }
        }
        List<double[]> rings = CornerFit.rings(v, w, h, EDGE);
        for (double[] r : rings) {
            for (int i = 0; i < r.length; i += 2) {
                r[i] = st.originX() + (r[i] + SAMPLE_AT) / st.scale();
                r[i + 1] = st.originY() + (r[i + 1] + SAMPLE_AT) / st.scale();
            }
        }
        return rings;
    }
}
