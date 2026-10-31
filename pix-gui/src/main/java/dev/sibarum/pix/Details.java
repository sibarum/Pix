package dev.sibarum.pix;

import java.util.Locale;

/**
 * The details under the name, and which of them a narrow bar keeps (docs/layout.md).
 *
 * <p>Three, in this order: what the picture shown is and its own size ({@code PNG image · 1920 × 1080}), the file's
 * size, and where it is in its folder ({@code 12 of 340}). When they do not all fit, they go whole, one at a time, the
 * file's size first and then what the picture is, rather than any being cut through; the place stays, as the name
 * does. Nothing here touches a node: the widths are measured by the layout and handed in, so the rule is tested alone.
 */
final class Details {

    static final int KIND = 0;
    static final int BYTES = 1;
    static final int PLACE = 2;
    static final int COUNT = 3;

    /** The order the details go in, first to go first. The place is not among them. */
    private static final int[] DROPS = {BYTES, KIND};

    private Details() {
    }

    /**
     * Which details are drawn in {@code room} px: those that say something, less the ones {@link #DROPS} names, in
     * turn, until the rest and the dots between them fit.
     *
     * @param widths each detail's width, as last laid out
     * @param said   whether each says anything; one that says nothing is never drawn
     * @param dot    the width of the dot between two
     */
    static boolean[] shown(float[] widths, boolean[] said, float dot, float room) {
        boolean[] shown = said.clone();
        for (int drop : DROPS) {
            if (width(widths, shown, dot) <= room) {
                break;
            }
            shown[drop] = false;
        }
        return shown;
    }

    /** How wide the details {@code shown} are, with a dot between each two. */
    static float width(float[] widths, boolean[] shown, float dot) {
        float w = 0f;
        int n = 0;
        for (int i = 0; i < widths.length; i++) {
            if (shown[i]) {
                w += widths[i];
                n++;
            }
        }
        return n == 0 ? 0f : w + (n - 1) * dot;
    }

    /** A file's size as Explorer says it, in powers of 1024: {@code 812 bytes}, {@code 3.2 MB}, {@code 14 MB}. */
    static String bytes(long b) {
        if (b < 0) {
            return "";
        }
        if (b < 1024) {
            return b + (b == 1 ? " byte" : " bytes");
        }
        String[] units = {"KB", "MB", "GB", "TB"};
        double v = b;
        int u = -1;
        do {
            v /= 1024;
            u++;
        } while (v >= 1024 && u < units.length - 1);
        return (v < 10 ? String.format(Locale.ROOT, "%.1f", v) : String.valueOf(Math.round(v))) + " " + units[u];
    }
}
