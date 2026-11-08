package dev.sibarum.pix;

/**
 * Every name an automation script may write down, in one place.
 *
 * <p><b>A ref is minted per run; a landmark still means something tomorrow.</b> That is the whole distinction
 * (vexelray-gui/docs/automation.md 5), and it is why these are constants rather than string literals scattered
 * through the view: a landmark is a published contract, so renaming one breaks a script somebody else wrote, and
 * the compiler should be the thing that notices.
 *
 * <p>State is carried in the accessible names where a driver waits on it: {@code await bar.name IMG_0412.jpg} waits
 * for a picture to be wanted, and {@code await bar.place 3 of} for the folder to be listed. A name cut to fit is cut
 * only as drawn, so its accessible name is whole.
 */
final class Landmarks {

    /** The wordmark in the title bar, after the picture's name. */
    static final String WORDMARK = "title.wordmark";

    /** The room the picture is fitted into. */
    static final String STAGE = "stage";

    /** The picture. */
    static final String PICTURE = "picture";

    /** What is said in the picture's place when a file is not a picture Rasterware can show. */
    static final String NOTE = "note";

    /** What the stage shows while no picture is wanted, and its button that opens one. */
    static final String SPLASH = "splash";
    static final String SPLASH_OPEN = "splash.open";

    /** The buttons at the stage's sides. */
    static final String PREVIOUS = "previous";
    static final String NEXT = "next";

    /**
     * The bar along the bottom, and its words: the picture's name; what the picture shown is and its size; the file's
     * size; and where it is in its folder.
     */
    static final String BAR = "bar";
    static final String BAR_NAME = "bar.name";
    static final String BAR_KIND = "bar.kind";
    static final String BAR_BYTES = "bar.bytes";
    static final String BAR_PLACE = "bar.place";

    /** The filmstrip, and its cells left to right: {@code bar.strip.0} and on. */
    static final String STRIP = "bar.strip";

    static String cell(int i) {
        return STRIP + "." + i;
    }

    /**
     * The bar's zoom: − and +, and between them how large the picture is going to be, {@code Fit} or a percent of its
     * own pixels ({@code await bar.zoom 100%}).
     */
    static final String ZOOM_OUT = "bar.zoom-out";
    static final String ZOOM = "bar.zoom";
    static final String ZOOM_IN = "bar.zoom-in";

    /** Keep shown, in the bar's tools: the controls pinned, and given room of their own. */
    static final String KEEP = "bar.keep";

    /** Trace, in the bar's tools: opens the Trace panel on the picture shown (docs/trace.md). */
    static final String TRACE = "bar.trace";

    /**
     * The Trace panel and its parts: the colours slider, the gradients segment, what the trace comes to (its size as a
     * file, its layers), what it says while tracing or when something failed, and Save. On the stage, the trace drawn
     * right of the line, and the line.
     */
    static final String TRACE_PANEL = "trace";
    static final String TRACE_COLOURS = "trace.colours";
    static final String TRACE_GRADIENTS = "trace.gradients";
    static final String TRACE_AFTER = "trace.after";
    static final String TRACE_LAYERS = "trace.layers";
    static final String TRACE_STATUS = "trace.status";
    static final String TRACE_SAVE = "trace.save";
    static final String TRACED = "stage.traced";

    /**
     * The format badge at the start of the bar, which opens the Convert panel (docs/convert.md); the panel, a tile per
     * format ({@code convert.png}, {@code convert.sdg}), why a save failed, and Save.
     */
    static final String FORMAT = "bar.format";
    static final String CONVERT_PANEL = "convert";
    static final String CONVERT_STATUS = "convert.status";
    static final String CONVERT_SAVE = "convert.save";

    static String convertTo(dev.sibarum.pix.core.Messages.Format f) {
        return CONVERT_PANEL + "." + f.extension();
    }
    static final String TRACE_LINE = "stage.trace-line";

    private Landmarks() {
    }
}
