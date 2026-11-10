package dev.sibarum.pix.core;

import dev.vexelray.fields.art.Raster;
import dev.vexelray.fields.convert.Convert;
import dev.vexelray.fields.sdg.Sdg;
import dev.vexelray.fields.sdg.Trace;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.DoubleBinaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The corner fit on fields whose edges are known exactly, then on a real trace. */
class CornerFitTest {

    /** A field {@code size} square sampled from a signed distance (negative inside), as the tracer stores one. */
    private static float[] field(int size, DoubleBinaryOperator sd) {
        float[] v = new float[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                v[y * size + x] = (float) Math.max(0, Math.min(255, 127.5 - sd.applyAsDouble(x, y) * 255 / 8));
            }
        }
        return v;
    }

    /** A square from 10 to 40 each way, its corners rounded to {@code r}. */
    private static DoubleBinaryOperator square(double r) {
        return (x, y) -> {
            double qx = Math.abs(x - 25) - (15 - r), qy = Math.abs(y - 25) - (15 - r);
            return Math.hypot(Math.max(qx, 0), Math.max(qy, 0)) + Math.min(Math.max(qx, qy), 0) - r;
        };
    }

    @Test
    void aDiscIsOneRingAtItsRadius() {
        List<double[]> rings = CornerFit.rings(field(50, (x, y) -> Math.hypot(x - 25, y - 25) - 12), 50, 50, 127.5f);
        assertEquals(1, rings.size());
        double[] r = rings.getFirst();
        for (int i = 0; i < r.length; i += 2) {
            assertEquals(12, Math.hypot(r[i] - 25, r[i + 1] - 25), 0.05);
        }
    }

    @Test
    void aSquareWithAHoleIsTwoRings() {
        DoubleBinaryOperator sq = square(0);
        List<double[]> rings = CornerFit.rings(field(50, (x, y) -> Math.max(sq.applyAsDouble(x, y),
                8 - Math.hypot(x - 25, y - 25))), 50, 50, 127.5f);
        assertEquals(2, rings.size());
    }

    @Test
    void roundedCornersAreFoundWhereTheSidesMeet() {
        double[] ring = CornerFit.rings(field(50, square(1.5)), 50, 50, 127.5f).getFirst();
        List<CornerFit.Corner> corners = CornerFit.corners(ring, CornerFit.Options.SHARP);
        assertEquals(4, corners.size());
        for (CornerFit.Corner c : corners) {
            assertEquals(0, Math.min(Math.abs(c.x() - 10), Math.abs(c.x() - 40)), 0.1, c.toString());
            assertEquals(0, Math.min(Math.abs(c.y() - 10), Math.abs(c.y() - 40)), 0.1, c.toString());
        }
    }

    @Test
    void aSquareIsFourLines() {
        double[] ring = CornerFit.rings(field(50, square(1.5)), 50, 50, 127.5f).getFirst();
        CornerFit.Outline o = CornerFit.fit(ring, CornerFit.Options.SHARP);
        assertEquals(4, o.corners());
        assertEquals(4, o.pieces().size(), o.svg());
        assertTrue(o.pieces().stream().allMatch(p -> p.length == 2), o.svg());
    }

    @Test
    void roundKeepsTheCornersAsCurves() {
        double[] ring = CornerFit.rings(field(50, square(1.5)), 50, 50, 127.5f).getFirst();
        assertEquals(0, CornerFit.fit(ring, CornerFit.Options.ROUND).corners());
    }

    @Test
    void aCircleHasNoCornersAndFewCurves() {
        double[] ring = CornerFit.rings(field(50, (x, y) -> Math.hypot(x - 25, y - 25) - 6), 50, 50, 127.5f).getFirst();
        CornerFit.Outline o = CornerFit.fit(ring, CornerFit.Options.SHARP);
        assertEquals(0, o.corners(), o.svg());
        assertTrue(o.pieces().size() <= 4, o.svg());
    }

    /** A real trace of a red square on white: its fitted corners are the square's, in the drawing's units. */
    @Test
    void aTracedSquareKeepsItsCorners() {
        int w = 64, h = 64;
        byte[] rgba = new byte[w * h * 4];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean in = x >= 16 && x < 48 && y >= 20 && y < 44;
                int i = (y * w + x) * 4;
                rgba[i] = (byte) 0xff;
                rgba[i + 1] = (byte) (in ? 0 : 0xff);
                rgba[i + 2] = (byte) (in ? 0 : 0xff);
                rgba[i + 3] = (byte) 0xff;
            }
        }
        Trace.Options d = Trace.Options.DEFAULT;
        Sdg sdg = Convert.rasterToSdg(new Trace.Options(2, d.texels(), d.spread(), d.under(), 2, 1e9))
                .apply(new Raster(w, h, rgba));
        Sdg.Layer red = sdg.layers().stream()
                .filter(l -> l.paint() instanceof Sdg.Paint.Solid s && (s.rgba() >>> 8) == 0xff0000).findFirst().orElseThrow();
        List<double[]> rings = CornerTrace.rings(sdg, red.stencil());
        assertEquals(1, rings.size());
        List<CornerFit.Corner> corners = CornerFit.corners(rings.getFirst(), CornerFit.Options.SHARP);
        assertEquals(4, corners.size(), corners.toString());
        for (CornerFit.Corner c : corners) {
            assertEquals(0, Math.min(Math.abs(c.x() - 16), Math.abs(c.x() - 48)), 0.25, corners.toString());
            assertEquals(0, Math.min(Math.abs(c.y() - 20), Math.abs(c.y() - 44)), 0.25, corners.toString());
        }
    }
}
