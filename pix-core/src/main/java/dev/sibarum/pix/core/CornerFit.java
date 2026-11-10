package dev.sibarum.pix.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A prototype of the corner fit FN-22 asks the stack for: a sampled distance field's edge turned into curves, its
 * sharp turns kept sharp. Three steps, each a static method so each is tested on its own:
 * <ol>
 *   <li>{@link #rings}: the field's level line, by marching squares, as closed polylines between the samples;</li>
 *   <li>{@link #corners}: where a ring turns sharply over a short run and stays turned over a longer one, the two
 *   sides' lines extended to meet, which is the corner the pixels rounded off;</li>
 *   <li>{@link #fit}: each run between corners fitted with cubic Béziers by least squares (Schneider's method), split
 *   where the error is over the tolerance, a run that is straight said as a line.</li>
 * </ol>
 * No edge detector and no transform: the tracer's regions already are the edges, and their distance fields place them
 * between the pixels. Pure geometry with no widget in it, so it lives in {@code -core}.
 */
final class CornerFit {

    /**
     * How a ring is fitted. {@code corner}: the least turn, in degrees, over {@code reach} units each way that is a
     * corner (180 or more: none, every turn is a curve). {@code tolerance}: how far, in units, a curve may stray from
     * the ring.
     */
    record Options(double corner, double reach, double tolerance) {
        static final Options SHARP = new Options(40, 3, 0.2);
        static final Options ROUND = new Options(180, 3, 0.2);
    }

    /** A ring fitted: where it starts, then each piece, a line ({@code [x, y]}) or a cubic ({@code [x1, y1, x2, y2, x, y]}). */
    record Outline(double startX, double startY, List<double[]> pieces, int corners) {

        /** As SVG path data, closed. */
        String svg() {
            StringBuilder b = new StringBuilder();
            b.append('M').append(n(startX)).append(' ').append(n(startY));
            for (double[] p : pieces) {
                b.append(p.length == 2 ? 'L' : 'C');
                for (int i = 0; i < p.length; i++) {
                    b.append(i == 0 ? "" : " ").append(n(p[i]));
                }
            }
            return b.append('Z').toString();
        }

        private static String n(double v) {
            String s = String.format(Locale.ROOT, "%.2f", v);
            s = s.contains(".") ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
            return s.equals("-0") ? "0" : s;
        }
    }

    private CornerFit() {
    }

    // ------------------------------------------------------------------------------------------------- marching squares

    /**
     * The closed lines where {@code v} (row-major, {@code w × h}) crosses {@code level}, in sample coordinates (sample
     * {@code (i, j)} at {@code (i, j)}), inside above the level. Past the grid is outside, so every ring closes. A ring
     * is {@code [x0, y0, x1, y1, ...]}, its first point not repeated.
     */
    static List<double[]> rings(float[] v, int w, int h, float level) {
        // Segments by the edge they start on; edge ids number the padded grid's cell edges.
        Map<Long, double[]> from = new HashMap<>();
        Map<Long, Long> next = new HashMap<>();
        int[] cx = {0, 1, 1, 0};
        int[] cy = {0, 0, 1, 1};
        float[] c = new float[4];
        long[] edge = new long[4];
        double[][] at = new double[4][];
        boolean[] in = new boolean[4];
        for (int y = -1; y < h; y++) {
            for (int x = -1; x < w; x++) {
                int inside = 0;
                for (int k = 0; k < 4; k++) {
                    c[k] = sample(v, w, h, x + cx[k], y + cy[k]);
                    if (c[k] > level) {
                        inside++;
                    }
                }
                if (inside == 0 || inside == 4) {
                    continue;
                }
                // The cell's boundary walked TL → TR → BR → BL; a crossing is "in" where the walk enters the inside.
                int crossings = 0;
                int[] idx = new int[4];
                for (int k = 0; k < 4; k++) {
                    int k1 = (k + 1) & 3;
                    boolean a = c[k] > level, b = c[k1] > level;
                    if (a != b) {
                        double t = (level - c[k]) / (c[k1] - c[k]);
                        double px = x + cx[k] + (cx[k1] - cx[k]) * t;
                        double py = y + cy[k] + (cy[k1] - cy[k]) * t;
                        at[crossings] = new double[]{px, py};
                        edge[crossings] = edgeId(x, y, k, w);
                        in[crossings] = b;
                        idx[crossings] = k;
                        crossings++;
                    }
                }
                if (crossings == 2) {
                    int out = in[0] ? 1 : 0, enter = 1 - out;
                    link(from, next, edge[out], edge[enter], at[out]);
                } else {
                    // A saddle: four crossings, alternating. Joined through the middle, each "out" meets the next "in";
                    // apart, the one before.
                    float mid = (c[0] + c[1] + c[2] + c[3]) / 4;
                    boolean joined = mid > level;
                    for (int i = 0; i < 4; i++) {
                        if (!in[i]) {
                            int j = joined ? (i + 1) & 3 : (i + 3) & 3;
                            link(from, next, edge[i], edge[j], at[i]);
                        }
                    }
                }
            }
        }
        List<double[]> out = new ArrayList<>();
        while (!from.isEmpty()) {
            long first = from.keySet().iterator().next();
            List<double[]> pts = new ArrayList<>();
            long e = first;
            while (from.containsKey(e)) {
                pts.add(from.remove(e));
                e = next.remove(e);
            }
            if (pts.size() >= 3) {
                double[] ring = new double[pts.size() * 2];
                for (int i = 0; i < pts.size(); i++) {
                    ring[2 * i] = pts.get(i)[0];
                    ring[2 * i + 1] = pts.get(i)[1];
                }
                out.add(ring);
            }
        }
        return out;
    }

    private static void link(Map<Long, double[]> from, Map<Long, Long> next, long a, long b, double[] p) {
        from.put(a, p);
        next.put(a, b);
    }

    private static float sample(float[] v, int w, int h, int x, int y) {
        return x < 0 || y < 0 || x >= w || y >= h ? Float.NEGATIVE_INFINITY : v[y * w + x];
    }

    /** The id of cell {@code (x, y)}'s side {@code k} (top, right, bottom, left), the same from both cells it bounds. */
    private static long edgeId(int x, int y, int k, int w) {
        long stride = w + 2;
        return switch (k) {
            case 0 -> 2 * ((y + 1) * stride + (x + 1));
            case 1 -> 2 * ((y + 1) * stride + (x + 2)) + 1;
            case 2 -> 2 * ((y + 2) * stride + (x + 1));
            default -> 2 * ((y + 1) * stride + (x + 1)) + 1;
        };
    }

    // ------------------------------------------------------------------------------------------------------- corners

    /** A corner: the ring's index nearest it, and where its sides meet. */
    record Corner(int index, double x, double y) {
    }

    /** The corners of {@code ring}: in order along it. */
    static List<Corner> corners(double[] ring, Options o) {
        List<Corner> out = new ArrayList<>();
        int n = ring.length / 2;
        if (o.corner() >= 180 || n < 8) {
            return out;
        }
        double[] s = arc(ring);
        double total = s[n];
        double reach = Math.min(o.reach(), total / 8);
        double[] turn = new double[n];
        for (int i = 0; i < n; i++) {
            turn[i] = turn(ring, s, total, i, reach);
        }
        double min = Math.toRadians(o.corner());
        for (int i = 0; i < n; i++) {
            if (turn[i] < min) {
                continue;
            }
            // The sharpest within reach; a tie goes to the first.
            boolean peak = true;
            for (int d = 1; peak && d < n; d++) {
                int a = Math.floorMod(i - d, n), b = (i + d) % n;
                double back = s[i] - s[a] + (a > i ? total : 0), ahead = s[b] - s[i] + (b < i ? total : 0);
                if (back > reach && ahead > reach) {
                    break;
                }
                if ((back <= reach && turn[a] >= turn[i]) || (ahead <= reach && turn[b] > turn[i])) {
                    peak = false;
                }
            }
            // A corner's sides are straight: twice as far out it has turned little more. A small circle has not.
            if (!peak || turn(ring, s, total, i, 2 * reach) > 1.5 * turn[i]) {
                continue;
            }
            // Across a rounded corner the turn is flat-topped; its middle, not its first point, is the corner's.
            int lo = 0, hi = 0;
            while (lo < n / 4 && turn[Math.floorMod(i - lo - 1, n)] >= turn[i] - FLAT) {
                lo++;
            }
            while (hi < n / 4 && turn[(i + hi + 1) % n] >= turn[i] - FLAT) {
                hi++;
            }
            int c = Math.floorMod(i + (hi - lo) / 2, n);
            double[] p = meet(ring, s, total, c, reach);
            if (p != null) {
                out.add(new Corner(c, p[0], p[1]));
            }
        }
        return out;
    }

    /** The angle between the chord {@code reach} back to point {@code i} and the chord from it {@code reach} ahead. */
    private static double turn(double[] ring, double[] s, double total, int i, double reach) {
        double[] p = point(ring, s, total, s[i]);
        double[] a = point(ring, s, total, s[i] - reach);
        double[] b = point(ring, s, total, s[i] + reach);
        double ux = p[0] - a[0], uy = p[1] - a[1], vx = b[0] - p[0], vy = b[1] - p[1];
        return Math.abs(Math.atan2(ux * vy - uy * vx, ux * vx + uy * vy));
    }

    /**
     * Where the side coming in to point {@code i} meets the side going out, each the line through the ring between one
     * and two reaches away. Null when they are parallel, or meet farther out than a corner rounded by
     * {@link #ROUNDING} would have been cut back: that is a join the picture drew round, or a thin stroke's end, and
     * extending it makes a spike.
     */
    private static double[] meet(double[] ring, double[] s, double total, int i, double reach) {
        double[] a2 = point(ring, s, total, s[i] - 2 * reach), a1 = point(ring, s, total, s[i] - reach);
        double[] b1 = point(ring, s, total, s[i] + reach), b2 = point(ring, s, total, s[i] + 2 * reach);
        double dx = a1[0] - a2[0], dy = a1[1] - a2[1], ex = b2[0] - b1[0], ey = b2[1] - b1[1];
        double den = dx * ey - dy * ex;
        if (Math.abs(den) < 1e-9) {
            return null;
        }
        double t = ((b1[0] - a1[0]) * ey - (b1[1] - a1[1]) * ex) / den;
        double mx = a1[0] + dx * t, my = a1[1] + dy * t;
        // A corner of inner angle θ rounded to radius r has its point r(1/sin(θ/2) − 1) out from the arc.
        double inner = Math.PI - Math.abs(Math.atan2(dx * ey - dy * ex, dx * ex + dy * ey));
        // A hairpin (θ near 0) would allow any distance: a stroke's end is never cut back by more than a few pixels.
        double limit = Math.min(ROUNDING * (1 / Math.sin(inner / 2) - 1) + 0.25, 3 * ROUNDING);
        // How far the ring was cut back: from the meeting point to the ring's nearest point near this one.
        double near = Double.MAX_VALUE;
        for (double d = -reach; d <= reach; d += reach / 16) {
            double[] q = point(ring, s, total, s[i] + d);
            near = Math.min(near, Math.hypot(mx - q[0], my - q[1]));
        }
        return near > limit ? null : new double[]{mx, my};
    }

    /** A ring shorter than this, in units, is left as it was cut: smoothing and snapping are for shapes, not specks. */
    static final double SMALL = 24;

    /** Turns within this of a corner's sharpest, in radians, are the same corner's flat top. */
    static final double FLAT = 0.03;

    /** The most, in units, the tracer's cut rounds a corner by: about a pixel and a half. */
    static final double ROUNDING = 1.5;

    /** Cumulative length at each point, and at the end the whole ring's ({@code n + 1} values). */
    private static double[] arc(double[] ring) {
        int n = ring.length / 2;
        double[] s = new double[n + 1];
        for (int i = 1; i <= n; i++) {
            int j = i % n;
            s[i] = s[i - 1] + Math.hypot(ring[2 * j] - ring[2 * i - 2], ring[2 * j + 1] - ring[2 * i - 1]);
        }
        return s;
    }

    /** The point at length {@code at} along the ring, round it either way. */
    private static double[] point(double[] ring, double[] s, double total, double at) {
        int n = ring.length / 2;
        at = ((at % total) + total) % total;
        int lo = 0, hi = n;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (s[mid] <= at) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        int j = (lo + 1) % n;
        double len = s[lo + 1] - s[lo];
        double t = len <= 0 ? 0 : (at - s[lo]) / len;
        return new double[]{ring[2 * lo] + (ring[2 * j] - ring[2 * lo]) * t,
                ring[2 * lo + 1] + (ring[2 * j + 1] - ring[2 * lo + 1]) * t};
    }

    // ----------------------------------------------------------------------------------------------------------- fit

    /** {@code ring} fitted: its corners found and kept, the runs between them curves. */
    static Outline fit(double[] ring, Options o) {
        int n = ring.length / 2;
        List<double[]> pieces = new ArrayList<>();
        if (arc(ring)[n] < SMALL) {
            // Too small to smooth or snap: a glyph's stroke would be eaten. Kept as the tracer cut it.
            for (int i = 1; i <= n; i++) {
                pieces.add(new double[]{ring[2 * (i % n)], ring[2 * (i % n) + 1]});
            }
            return new Outline(ring[0], ring[1], pieces, 0);
        }
        List<Corner> corners = corners(ring, o);
        if (corners.isEmpty()) {
            // No corner: a closed smooth curve, cut in two at points whose tangent runs on through the cut.
            double[][] whole = run(ring, 0, n, null, null, 0);
            double[] smooth = new double[(whole.length - 1) * 2];
            for (int i = 0; i < whole.length - 1; i++) {
                smooth[2 * i] = whole[i][0];
                smooth[2 * i + 1] = whole[i][1];
            }
            int m = smooth.length / 2, half = m / 2;
            double[] t0 = centre(smooth, 0), th = centre(smooth, half);
            double[][] first = java.util.Arrays.copyOfRange(whole, 0, half + 1);
            double[][] second = java.util.Arrays.copyOfRange(whole, half, whole.length);
            fitCubic(first, t0, neg(th), o.tolerance(), pieces, 0);
            fitCubic(second, th, neg(t0), o.tolerance(), pieces, 0);
            return new Outline(whole[0][0], whole[0][1], pieces, 0);
        }
        double[] s = arc(ring);
        double trim = Math.min(o.reach(), s[n] / 8) * 0.6;
        for (int k = 0; k < corners.size(); k++) {
            Corner a = corners.get(k), b = corners.get((k + 1) % corners.size());
            int end = b.index() > a.index() ? b.index() : b.index() + n;
            double[][] pts = run(ring, a.index(), end, new double[]{a.x(), a.y()}, new double[]{b.x(), b.y()}, trim);
            double[] t1 = sideTangent(pts, true), t2 = sideTangent(pts, false);
            fitCubic(pts, t1, t2, o.tolerance(), pieces, 0);
        }
        Corner first = corners.get(0);
        return new Outline(first.x(), first.y(), pieces, corners.size());
    }

    /**
     * The ring's points from index {@code from} to {@code to} (past the end wraps), as a run to fit. With corners, the
     * run starts and ends at them, and leaves out the points within {@code trim} of each, where the pixels rounded it.
     */
    private static double[][] run(double[] ring, int from, int to, double[] start, double[] end, double trim) {
        int n = ring.length / 2;
        List<double[]> pts = new ArrayList<>();
        if (start != null) {
            pts.add(start);
        }
        for (int i = from; i <= to; i++) {
            int j = i % n;
            double[] p = {ring[2 * j], ring[2 * j + 1]};
            if (start != null && (Math.hypot(p[0] - start[0], p[1] - start[1]) < trim
                    || Math.hypot(p[0] - end[0], p[1] - end[1]) < trim)) {
                continue;
            }
            if (!pts.isEmpty() && Math.hypot(p[0] - pts.getLast()[0], p[1] - pts.getLast()[1]) < 1e-3) {
                continue;
            }
            pts.add(p);
        }
        if (end != null) {
            pts.add(end);
        }
        return smooth(pts.toArray(double[][]::new), start == null);
    }

    /** Passes of {@link #smooth}: about a pixel and a half either way, which is the step the cut grid leaves. */
    static final int SMOOTHING = 4;

    /**
     * {@code d} with the stair the tracer's grid leaves along an edge taken out: each point moved toward its neighbours
     * ({@code ¼, ½, ¼}), {@link #SMOOTHING} times. The ends stay, so corners stay sharp; a run with no corner is a whole
     * ring ({@code closed}), its ends neighbours.
     */
    private static double[][] smooth(double[][] d, boolean closed) {
        int n = d.length;
        if (n < 3) {
            return d;
        }
        double[][] a = new double[n][];
        for (int i = 0; i < n; i++) {
            a[i] = d[i].clone();
        }
        double[][] b = new double[n][2];
        for (int pass = 0; pass < SMOOTHING; pass++) {
            for (int i = 0; i < n; i++) {
                boolean end = i == 0 || i == n - 1;
                if (end && !closed) {
                    b[i][0] = a[i][0];
                    b[i][1] = a[i][1];
                    continue;
                }
                // A closed run repeats its first point at its end, so its neighbours skip that copy.
                double[] p = a[i == 0 ? n - 2 : i - 1], q = a[i == n - 1 ? 1 : i + 1];
                b[i][0] = (p[0] + 2 * a[i][0] + q[0]) / 4;
                b[i][1] = (p[1] + 2 * a[i][1] + q[1]) / 4;
            }
            double[][] t = a;
            a = b;
            b = t;
        }
        return a;
    }

    /** The direction a ring runs through point {@code i}, from its neighbours two either side. */
    private static double[] centre(double[] ring, int i) {
        int n = ring.length / 2;
        int a = Math.floorMod(i - 2, n), b = (i + 2) % n;
        return unit(ring[2 * b] - ring[2 * a], ring[2 * b + 1] - ring[2 * a + 1]);
    }

    /** The direction a side leaves its corner (or comes into it, {@code start} false), over its first few points. */
    private static double[] sideTangent(double[][] pts, boolean start) {
        int last = pts.length - 1;
        int k = Math.min(3, last);
        double[] a = start ? pts[0] : pts[last], b = start ? pts[k] : pts[last - k];
        return unit(b[0] - a[0], b[1] - a[1]);
    }

    /**
     * Schneider's fit of {@code d} to cubics, appended to {@code out}: {@code t1} the direction out of the first point,
     * {@code t2} into the last from it (pointing back along the run).
     */
    private static void fitCubic(double[][] d, double[] t1, double[] t2, double tol, List<double[]> out, int depth) {
        int last = d.length - 1;
        if (last < 1) {
            return;
        }
        if (last == 1 || straight(d, tol)) {
            out.add(new double[]{d[last][0], d[last][1]});
            return;
        }
        double[] u = chord(d);
        double[][] bez = bezier(d, u, t1, t2);
        double[] err = maxError(d, bez, u);
        double tol2 = tol * tol;
        if (err[0] < tol2) {
            out.add(flat(bez));
            return;
        }
        if (err[0] < tol2 * 16) {
            for (int it = 0; it < 6; it++) {
                u = reparameterize(d, u, bez);
                bez = bezier(d, u, t1, t2);
                err = maxError(d, bez, u);
                if (err[0] < tol2) {
                    out.add(flat(bez));
                    return;
                }
            }
        }
        int split = (int) err[1];
        if (depth > 24 || split <= 0 || split >= last) {
            out.add(flat(bez));
            return;
        }
        double[] tc = unit(d[split - 1][0] - d[split + 1][0], d[split - 1][1] - d[split + 1][1]);
        fitCubic(java.util.Arrays.copyOfRange(d, 0, split + 1), t1, tc, tol, out, depth + 1);
        fitCubic(java.util.Arrays.copyOfRange(d, split, d.length), neg(tc), t2, tol, out, depth + 1);
    }

    private static boolean straight(double[][] d, double tol) {
        double[] a = d[0], b = d[d.length - 1];
        double len = Math.hypot(b[0] - a[0], b[1] - a[1]);
        if (len < 1e-9) {
            return false;
        }
        for (double[] p : d) {
            double dist = Math.abs((b[0] - a[0]) * (a[1] - p[1]) - (a[0] - p[0]) * (b[1] - a[1])) / len;
            if (dist > tol * 0.5) {
                return false;
            }
        }
        return true;
    }

    private static double[] chord(double[][] d) {
        double[] u = new double[d.length];
        for (int i = 1; i < d.length; i++) {
            u[i] = u[i - 1] + Math.hypot(d[i][0] - d[i - 1][0], d[i][1] - d[i - 1][1]);
        }
        double total = u[d.length - 1];
        for (int i = 1; i < d.length; i++) {
            u[i] /= total;
        }
        return u;
    }

    /** The least-squares cubic through the run's ends along the given tangents. */
    private static double[][] bezier(double[][] d, double[] u, double[] t1, double[] t2) {
        int last = d.length - 1;
        double[] p0 = d[0], p3 = d[last];
        double c00 = 0, c01 = 0, c11 = 0, x0 = 0, x1 = 0;
        for (int i = 0; i < d.length; i++) {
            double t = u[i], mt = 1 - t;
            double b0 = mt * mt * mt, b1 = 3 * t * mt * mt, b2 = 3 * t * t * mt, b3 = t * t * t;
            double a1x = t1[0] * b1, a1y = t1[1] * b1, a2x = t2[0] * b2, a2y = t2[1] * b2;
            c00 += a1x * a1x + a1y * a1y;
            c01 += a1x * a2x + a1y * a2y;
            c11 += a2x * a2x + a2y * a2y;
            double tx = d[i][0] - (p0[0] * (b0 + b1) + p3[0] * (b2 + b3));
            double ty = d[i][1] - (p0[1] * (b0 + b1) + p3[1] * (b2 + b3));
            x0 += a1x * tx + a1y * ty;
            x1 += a2x * tx + a2y * ty;
        }
        double det = c00 * c11 - c01 * c01;
        double alpha1 = det == 0 ? 0 : (x0 * c11 - x1 * c01) / det;
        double alpha2 = det == 0 ? 0 : (c00 * x1 - c01 * x0) / det;
        double seg = Math.hypot(p3[0] - p0[0], p3[1] - p0[1]);
        // Too short is degenerate; too long (a nearly singular solve) throws the curve far off: either way, a third.
        if (alpha1 < seg * 1e-6 || alpha2 < seg * 1e-6 || alpha1 > seg * 2 || alpha2 > seg * 2
                || Double.isNaN(alpha1) || Double.isNaN(alpha2)) {
            alpha1 = alpha2 = seg / 3;
        }
        return new double[][]{p0, {p0[0] + t1[0] * alpha1, p0[1] + t1[1] * alpha1},
                {p3[0] + t2[0] * alpha2, p3[1] + t2[1] * alpha2}, p3};
    }

    /** The largest squared distance from a point to its place on the curve, and the index of that point. */
    private static double[] maxError(double[][] d, double[][] bez, double[] u) {
        double max = 0;
        int at = d.length / 2;
        for (int i = 1; i < d.length - 1; i++) {
            double[] p = eval(bez, u[i]);
            double dist = (p[0] - d[i][0]) * (p[0] - d[i][0]) + (p[1] - d[i][1]) * (p[1] - d[i][1]);
            if (dist >= max) {
                max = dist;
                at = i;
            }
        }
        return new double[]{max, at};
    }

    /** Each point's parameter moved one Newton step closer to where the curve is nearest it. */
    private static double[] reparameterize(double[][] d, double[] u, double[][] bez) {
        double[] out = new double[u.length];
        for (int i = 0; i < u.length; i++) {
            double t = u[i];
            double[] q = eval(bez, t), q1 = d1(bez, t), q2 = d2(bez, t);
            double dx = q[0] - d[i][0], dy = q[1] - d[i][1];
            double num = dx * q1[0] + dy * q1[1];
            double den = q1[0] * q1[0] + q1[1] * q1[1] + dx * q2[0] + dy * q2[1];
            out[i] = den == 0 ? t : Math.max(0, Math.min(1, t - num / den));
        }
        return out;
    }

    private static double[] eval(double[][] b, double t) {
        double mt = 1 - t;
        double b0 = mt * mt * mt, b1 = 3 * t * mt * mt, b2 = 3 * t * t * mt, b3 = t * t * t;
        return new double[]{b0 * b[0][0] + b1 * b[1][0] + b2 * b[2][0] + b3 * b[3][0],
                b0 * b[0][1] + b1 * b[1][1] + b2 * b[2][1] + b3 * b[3][1]};
    }

    private static double[] d1(double[][] b, double t) {
        double mt = 1 - t;
        double[] r = new double[2];
        for (int k = 0; k < 2; k++) {
            r[k] = 3 * mt * mt * (b[1][k] - b[0][k]) + 6 * mt * t * (b[2][k] - b[1][k]) + 3 * t * t * (b[3][k] - b[2][k]);
        }
        return r;
    }

    private static double[] d2(double[][] b, double t) {
        double[] r = new double[2];
        for (int k = 0; k < 2; k++) {
            r[k] = 6 * (1 - t) * (b[2][k] - 2 * b[1][k] + b[0][k]) + 6 * t * (b[3][k] - 2 * b[2][k] + b[1][k]);
        }
        return r;
    }

    private static double[] flat(double[][] bez) {
        return new double[]{bez[1][0], bez[1][1], bez[2][0], bez[2][1], bez[3][0], bez[3][1]};
    }

    private static double[] unit(double x, double y) {
        double len = Math.hypot(x, y);
        return len == 0 ? new double[]{1, 0} : new double[]{x / len, y / len};
    }

    private static double[] neg(double[] v) {
        return new double[]{-v[0], -v[1]};
    }
}
