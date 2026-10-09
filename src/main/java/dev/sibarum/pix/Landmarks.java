package dev.sibarum.pix;

/**
 * The names an automation script is allowed to depend on. A landmark is a promise: it is what
 * {@code Gui.navigate} and the driving socket's {@code click} and {@code shot} take, so renaming one is an edit
 * to every script that uses it, and adding one is free.
 */
final class Landmarks {

    /** The file shown, and "Image 3 of 17", written at once on every step, so scripts await them. */
    static final String NAME = "name";
    static final String COUNT = "count";
    /** What the file is ("PNG image · 1920 × 1080"), or "reading…" until its picture has arrived. */
    static final String INFO = "info";
    /** The controls, each a tile in the bar. */
    static final String CLOSE = "close";
    static final String PREVIOUS = "previous";
    static final String NEXT = "next";
    /** The Background tile, which opens the backdrop menu, and Keep shown, which stops the bar hiding. */
    static final String BACKGROUND = "background";
    static final String KEEP = "keep";
    /** The backdrop menu's swatches, one per {@code ImageView.Backdrop}: "backdrop.checker" and so on. */
    static final String BACKDROP = "backdrop.";

    private Landmarks() {
    }
}
