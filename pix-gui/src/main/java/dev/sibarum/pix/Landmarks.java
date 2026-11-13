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

    /**
     * Layers, in the bar's tools for a drawing, and the Layers panel (docs/layers.md): its rows front first
     * ({@code layers.0} and on), the picked layer's name, its role, the peel and how far apart, the light page, what it
     * says, and Save roles. On the stage, the backmost layer's node.
     */
    static final String LAYERS = "bar.layers";
    static final String LAYERS_PANEL = "layers";
    static final String LAYER_PICKED = "layers.picked";
    static final String LAYER_ROLE = "layers.role";
    static final String LAYERS_PEEL = "layers.peel";
    static final String LAYERS_APART = "layers.apart";
    static final String LAYERS_LIGHT = "layers.light";
    static final String LAYERS_STATUS = "layers.status";
    static final String LAYERS_SAVE = "layers.save";
    static final String LAYERS_STAGE = "stage.layer";

    /**
     * Details, in the bar's tools, and the Details panel (docs/details.md): the picture's name, a value per row
     * ({@code details.format}, {@code details.dimensions}, ...), and the palette's swatches.
     */
    static final String DETAILS = "bar.details";
    static final String DETAILS_PANEL = "details";
    static final String DETAILS_NAME = "details.name";
    static final String DETAILS_PALETTE = "details.palette";

    static String detail(String row) {
        return DETAILS_PANEL + "." + row.toLowerCase(java.util.Locale.ROOT).replace(' ', '-');
    }

    static String layer(int i) {
        return LAYERS_PANEL + "." + i;
    }

    static String convertTo(dev.sibarum.pix.core.Messages.Format f) {
        return CONVERT_PANEL + "." + f.extension();
    }
    static final String TRACE_LINE = "stage.trace-line";

    /** The seek track under the stage while a picture plays, and its playhead. */
    static final String SEEK = "seek";
    static final String SEEK_HEAD = "seek.head";

    /**
     * The title bar's File, Edit and View menus, by the names they are declared under (a script opens one as
     * {@code TitleBar.menuLandmark} of its name); and the New SDG window's controls, each {@code new-sdg/<landmark>} to a script: the unit, the width,
     * the height and the DPI, what that makes in texels, where the field starts, the mode, and the buttons.
     */
    static final String FILE_MENU = "file";
    static final String EDIT_MENU = "edit";
    static final String VIEW_MENU = "view";
    static final String NEW_UNIT = "new.unit";
    static final String NEW_WIDTH = "new.width";
    static final String NEW_HEIGHT = "new.height";
    static final String NEW_DPI = "new.dpi";
    static final String NEW_TEXELS = "new.texels";
    static final String NEW_STARTS = "new.starts";
    static final String NEW_MODE = "new.mode";
    static final String NEW_CREATE = "new.create";
    static final String NEW_CANCEL = "new.cancel";

    /**
     * The editor's window, each {@code sdg/<landmark>} to a script: what is being edited (mode and size), the field, its
     * Save, and what the last save said.
     */
    static final String SKETCH_TITLE = "sketch.title";
    static final String SKETCH_FIELD = "sketch.field";
    static final String SKETCH_SAVE = "sketch.save";
    static final String SKETCH_STATUS = "sketch.status";

    /** The editor's brush: paint or erase, its size, strength and hardness; and Trace over…, for a picture under it. */
    static final String SKETCH_ERASE = "sketch.erase";
    static final String SKETCH_SIZE = "sketch.size";
    static final String SKETCH_STRENGTH = "sketch.strength";
    static final String SKETCH_HARDNESS = "sketch.hardness";
    static final String SKETCH_SNAP = "sketch.snap";
    static final String SKETCH_TRACE = "sketch.trace";

    private Landmarks() {
    }
}
