package dev.sibarum.pix.core;

import dev.vexelray.codec.image.Picture;

import java.util.ArrayList;
import java.util.List;

/**
 * A picture's palette for the Details panel (docs/details.md): up to {@link #MOST} colours, most of the picture first,
 * from its thumbnail, which is read anyway. Each pixel at least half opaque goes into a bin of four bits a channel; the
 * fullest bins' mean colours are taken, fullest first, passing over one too near a colour taken already, so a picture
 * of one blue in many shades gives one blue and not five.
 */
final class Palette {

    /** How many colours a palette has at most. */
    static final int MOST = 5;

    /** Two colours nearer than this (Euclidean, of 255 a channel) are one, for a palette. */
    static final double NEAR = 40;

    private Palette() {
    }

    /** {@code p}'s palette, {@code 0xRRGGBBAA} each; empty for no picture, or one with nothing opaque. */
    static List<Integer> of(Picture p) {
        if (p == null) {
            return List.of();
        }
        return of(ReadPicture.firstFrame(p));
    }

    /** The palette of straight RGBA8 pixels. */
    static List<Integer> of(byte[] rgba) {
        int bins = 1 << 12;
        long[] count = new long[bins];
        long[] r = new long[bins];
        long[] g = new long[bins];
        long[] b = new long[bins];
        for (int i = 0; i + 3 < rgba.length; i += 4) {
            if ((rgba[i + 3] & 0xff) < 128) {
                continue;
            }
            int rr = rgba[i] & 0xff, gg = rgba[i + 1] & 0xff, bb = rgba[i + 2] & 0xff;
            int bin = (rr >> 4) << 8 | (gg >> 4) << 4 | bb >> 4;
            count[bin]++;
            r[bin] += rr;
            g[bin] += gg;
            b[bin] += bb;
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < bins; i++) {
            if (count[i] > 0) {
                order.add(i);
            }
        }
        order.sort((x, y) -> Long.compare(count[y], count[x]));
        List<int[]> chosen = new ArrayList<>();
        for (int bin : order) {
            int[] c = {(int) (r[bin] / count[bin]), (int) (g[bin] / count[bin]), (int) (b[bin] / count[bin])};
            if (chosen.stream().noneMatch(k -> distance(k, c) < NEAR)) {
                chosen.add(c);
                if (chosen.size() == MOST) {
                    break;
                }
            }
        }
        List<Integer> out = new ArrayList<>(chosen.size());
        for (int[] c : chosen) {
            out.add(c[0] << 24 | c[1] << 16 | c[2] << 8 | 0xff);
        }
        return out;
    }

    private static double distance(int[] a, int[] b) {
        int dr = a[0] - b[0], dg = a[1] - b[1], db = a[2] - b[2];
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }
}
