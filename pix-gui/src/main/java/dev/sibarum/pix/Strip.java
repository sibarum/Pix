package dev.sibarum.pix;

/**
 * How many thumbnails the filmstrip shows, and how wide it is: the bar's middle, which gives way first
 * (layout.md, <i>The window</i>). Pure arithmetic on what the view has measured, so it is tested alone, as
 * {@link Details} is.
 *
 * <p>The strip takes at most {@link #SHARE} of what the bar has for the words and itself, so the name keeps the rest,
 * and shows an odd number of cells, so the picture wanted is in the middle. Fewer than {@link #FEWEST} and it shows
 * none, and the name has the bar to itself.
 */
final class Strip {

    /** A cell: its width and height, in rem, and the gap between cells. */
    static final float CELL_W = 2.75f;
    static final float CELL_H = 2.25f;
    static final float GAP = 0.25f;

    /** The most cells built, and so shown: seven each side. */
    static final int MOST = 15;

    /** The fewest worth showing: one each side. */
    static final int FEWEST = 3;

    /** The share of the bar's room the strip may take. */
    static final float SHARE = 0.5f;

    private Strip() {
    }

    /**
     * The cells shown, given {@code room}, the px the words and the strip have between them, and {@code remPx}, a rem
     * in px. Odd, from {@link #FEWEST} to {@link #MOST}, or zero.
     */
    static int cells(float room, float remPx) {
        if (!(room > 0f) || !(remPx > 0f)) {
            return 0;
        }
        int n = (int) Math.floor((room * SHARE + GAP * remPx) / ((CELL_W + GAP) * remPx));
        n = Math.min(n, MOST);
        if (n % 2 == 0) {
            n--;
        }
        return n < FEWEST ? 0 : n;
    }

    /** The strip's width in px for {@code cells} cells: the cells and the gaps between them. */
    static float width(int cells, float remPx) {
        return cells == 0 ? 0f : (cells * CELL_W + (cells - 1) * GAP) * remPx;
    }

    /**
     * The index into a filmstrip's paths shown in cell {@code cell} of {@code cells}, the middle one being the picture
     * wanted at {@code at}; -1 for a cell past either end of the paths, which is left empty.
     */
    static int path(int cell, int cells, int at, int paths) {
        int i = at + cell - cells / 2;
        return i >= 0 && i < paths ? i : -1;
    }
}
