package dev.sibarum.pix;

import dev.vexelray.gui.core.Feed;
import dev.vexelray.gui.core.ImageSdg;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.core.Node;
import dev.vexelray.gui.core.NodeEvent;
import dev.vexelray.gui.core.input.CursorShape;
import dev.vexelray.gui.core.input.InteractionState;
import dev.vexelray.gui.core.layout.LayoutEnums.AlignItems;
import dev.vexelray.gui.core.layout.LayoutEnums.Direction;
import dev.vexelray.gui.core.layout.LayoutEnums.Justify;
import dev.vexelray.gui.core.layout.Length;
import dev.vexelray.gui.core.style.Role;
import dev.vexelray.gui.core.text.Ellipsis;
import dev.vexelray.gui.icons.Icon;
import dev.vexelray.gui.widget.IconButton;
import dev.vexelray.gui.widget.TitleBar;

import java.util.List;

/**
 * The window: the framework's title bar, the stage the picture is fitted into with Previous and Next at its sides, and
 * the bar floating over its bottom, or below it with Keep shown: the words, the filmstrip and the tools (docs/layout.md).
 *
 * <h2>Controls in rem, words in dp</h2>
 *
 * <p>The window's zoom (Ctrl+= / Ctrl+-) scales rem and not dp. So everything that is a control — the buttons, the
 * bar's padding, the room around them — is in rem, and every word Rasterware writes is in dp: making the controls smaller
 * never makes the name too small to read. This is the framework's rule turned round on purpose (its own is type in rem,
 * gutters in dp), because what the zoom is for here is room for the picture, not reading.
 *
 * <p>It holds no state but what the pointer is on, which it paints as each widget does its own: a hover lifts what
 * can be clicked and a press shows at once, and the framework fades the colours between. The picture shown is the view's to know, which details fit is the view's to decide
 * ({@link Details}), and what each label says is what the viewer last told the view. Built in the {@code TREE} phase,
 * before there is a window, and needing none, so a test can build the real tree headless.
 */
final class Ui {

    /** The name, and what is said in a picture's place. */
    static final Length NAME_TEXT = Length.dp(14f);

    /** What the name says while nothing is wanted: a click on it opens one. */
    static final String OPEN_A_PICTURE = "Open a picture…";

    /** The details under the name. */
    static final Length DETAIL_TEXT = Length.dp(12f);

    /** Between the name and the details. */
    static final Length LINE_GAP = Length.dp(2f);

    /** Previous and Next: the button, and the chevron in it. */
    static final Length SIDE = Length.rem(2.5f);
    static final Length SIDE_ICON = Length.rem(1.5f);

    /** Between the window's edges and the controls along them. */
    static final float MARGIN_REM = 0.75f;
    static final Length MARGIN = Length.rem(MARGIN_REM);

    /** A tool in the bar: the button, and its icon. */
    static final float TOOL_REM = 2f;
    static final Length TOOL = Length.rem(TOOL_REM);
    static final Length TOOL_ICON = Length.rem(1.25f);

    /** What the zoom says while the picture is fitted, and the room it has: as wide as {@code 3200%}. */
    static final String FIT = "Fit";
    static final Length ZOOM_LEVEL_W = Length.dp(48f);

    /** Between the details: a dot with a space each side, so the gap is part of what is measured. */
    static final String DOT = " · ";

    private final TitleBar titleBar;
    private final Node stage;
    private final Gui gui;
    private final Node[] layers = new Node[2];
    private final Node note;
    /** What the stage shows while no picture is wanted, and whether it is in the stage. */
    private final Splash splash;
    private boolean splashed;
    private final IconButton previous;
    private final IconButton next;
    /** The columns Previous and Next are in, which fade with the bar. */
    private final Node previousSide;
    private final Node nextSide;
    private final Node bar;
    /** The seek track and its playhead, and how tall the track is. */
    private final Node seek;
    private final Node head;
    static final Length SEEK_H = Length.rem(0.4f);
    private final IconButton keep;
    /** Trace, in the bar's tools, and the panel it opens. */
    private final IconButton trace;
    private final TracePanel tracePanel;
    private final ConvertPanel convertPanel;
    private boolean converting;
    /** Layers, in the bar for a drawing, and the panel it opens. */
    private final IconButton layersTool;
    private final LayersPanel layersPanel;
    private boolean layering;
    /** Details, in the bar's tools whenever a picture is, and the panel it opens. */
    private final IconButton detailsTool;
    private final DetailsPanel detailsPanel;
    private boolean informing;
    /** What the picture shown is: a raster, which Trace takes, or a drawing, which Layers takes; or neither. */
    private boolean raster;
    private boolean drawing;
    private boolean kept;
    private boolean tracing;
    /** The bar's zoom: − and + either side of how large the picture is. */
    private final IconButton zoomOut;
    private final IconButton zoomIn;
    private final Node zoomLevel;
    private final Node zoom;
    private final Node name;
    private final Node words;
    /** Whether the name is a picture's, rather than asking for one; where the pointer is on it. */
    private boolean named;
    private InteractionState nameState = InteractionState.NORMAL;
    private final Node details;
    /** The details in {@link Details}'s order, and the dot before each but the first. */
    private final Node[] items;
    private final Node[] dots;
    /** The filmstrip, between the words and the tools, and its cells: each a box, the thumbnail in it, and its name. */
    private final Node strip;
    private final Node[] cells;
    private final Node[] thumbs;
    private final Node[] cellNames;
    /** Which cell is the picture wanted, which are past the folder's ends, and where the pointer is on each. */
    private final boolean[] wanted = new boolean[Strip.MOST];
    private final boolean[] there = new boolean[Strip.MOST];
    private final InteractionState[] cellStates = new InteractionState[Strip.MOST];

    Ui(Gui gui, TitleBar titleBar) {
        this.titleBar = titleBar;
        // The name as the bar's wordmark, after the mark in its lockup and before the picture's name: the title gives
        // way when the window is narrow, the lockup keeps its size, and the bar inks it as the pointer comes and goes.
        TitleBar.Wordmark wordmark = Wordmark.bar(gui);
        titleBar.wordmark(wordmark);
        gui.landmark(Landmarks.WORDMARK, wordmark.node());
        // The zoom sizes the controls, and what they give up is the picture's: the window keeps its size.
        gui.zoomSizesWindow(false);

        this.gui = gui;
        // The view throws and crossfades the pictures itself (Toss): the framework leaves their look alone.
        for (int i = 0; i < layers.length; i++) {
            layers[i] = gui.box().visible(false).paintCuts(true);
        }
        gui.landmark(Landmarks.PICTURE, layers[0]);
        // Always there, and shown as far as the view shades it (Shade), so it comes and goes gradually.
        note = gui.text("").textSize(NAME_TEXT).textColor(gui.theme().color(Role.DIM));
        gui.landmark(Landmarks.NOTE, note);

        previous = new IconButton(gui, Icon.CHEVRON_LEFT, "Previous").size(SIDE, SIDE_ICON).tip("Previous (←)");
        next = new IconButton(gui, Icon.CHEVRON_RIGHT, "Next").size(SIDE, SIDE_ICON).tip("Next (→)");
        gui.landmark(Landmarks.PREVIOUS, previous.node());
        gui.landmark(Landmarks.NEXT, next.node());

        splash = new Splash(gui);
        tracePanel = new TracePanel(gui);
        convertPanel = new ConvertPanel(gui);
        layersPanel = new LayersPanel(gui);
        detailsPanel = new DetailsPanel(gui);

        previousSide = side(gui, previous, Length.ZERO);
        nextSide = side(gui, next, Length.percent(100f));
        // The splash is there from the start, since nothing is wanted until the folder is listed, and Previous and
        // Next are not (splash).
        stage = gui.box()
                .width(Length.grow(1f)).height(Length.percent(100f))
                .direction(Direction.COLUMN).justify(Justify.CENTER).alignItems(AlignItems.CENTER)
                .clip(true)
                .children(layers[0], layers[1]);
        // The trace and its line lie over the picture's layers, which front() keeps the first two children.
        for (Node n : layersPanel.overStage()) {
            stage.append(n);
        }
        for (Node n : tracePanel.overStage()) {
            stage.append(n);
        }
        stage.append(convertPanel.panel());
        stage.append(note);
        stage.append(splash.node());
        stage.append(previousSide.visible(false));
        stage.append(nextSide.visible(false));
        splashed = true;
        gui.landmark(Landmarks.STAGE, stage);

        name = gui.text(OPEN_A_PICTURE).role("button").wordWrap(false).ellipsis(Ellipsis.MIDDLE).width(Length.FILL)
                .textSize(NAME_TEXT);
        gui.landmark(Landmarks.BAR_NAME, name);
        gui.cursor(name, CursorShape.POINTER);
        gui.hear(name, NodeEvent.STATE, s -> {
            nameState = s;
            paintName();
        });
        paintName();
        items = new Node[Details.COUNT];
        dots = new Node[Details.COUNT];
        String[] landmarks = {Landmarks.BAR_KIND, Landmarks.BAR_BYTES, Landmarks.BAR_PLACE};
        details = gui.row().width(Length.FILL).alignItems(AlignItems.CENTER).clip(true).scroll(false, false);
        for (int i = 0; i < items.length; i++) {
            items[i] = detail(gui, "");
            gui.landmark(landmarks[i], items[i]);
            if (i > 0) {
                dots[i] = detail(gui, DOT);
                details.append(dots[i]);
            }
            details.append(items[i]);
        }
        words = gui.column().width(Length.grow(1f)).gap(LINE_GAP).scroll(false, false).children(name, details);

        cells = new Node[Strip.MOST];
        thumbs = new Node[Strip.MOST];
        cellNames = new Node[Strip.MOST];
        strip = gui.row().width(Length.ZERO).height(Length.rem(Strip.CELL_H)).gap(Length.rem(Strip.GAP))
                .alignItems(AlignItems.CENTER).clip(true).scroll(false, false).visible(false);
        gui.landmark(Landmarks.STRIP, strip);
        for (int i = 0; i < cells.length; i++) {
            thumbs[i] = gui.box().hitInert(true).paintCuts(true);   // slid and faded by the view (Glide, Shade)
            cellNames[i] = gui.text("").hitInert(true).visible(false).paintCuts(true);   // slides with its cell
            cells[i] = gui.box().role("button").width(Length.rem(Strip.CELL_W)).height(Length.rem(Strip.CELL_H))
                    .direction(Direction.COLUMN).justify(Justify.CENTER).alignItems(AlignItems.CENTER)
                    .corner(Length.rem(0.25f)).clip(true).scroll(false, false)
                    .background(gui.theme().color(Role.WELL)).children(thumbs[i], cellNames[i]);
            cellStates[i] = InteractionState.NORMAL;
            int at = i;
            gui.hear(cells[i], NodeEvent.STATE, s -> {
                cellStates[at] = s;
                paintCell(at);
            });
            paintCell(i);
            gui.landmark(Landmarks.cell(i), cells[i]);
            strip.append(cells[i]);
        }

        zoomOut = new IconButton(gui, Icon.MINUS, "Zoom out").size(TOOL, TOOL_ICON).tip("Zoom out");
        zoomIn = new IconButton(gui, Icon.PLUS, "Zoom in").size(TOOL, TOOL_ICON).tip("Zoom in");
        zoomLevel = gui.text(FIT).wordWrap(false).textSize(DETAIL_TEXT).textColor(gui.theme().color(Role.INK));
        gui.landmark(Landmarks.ZOOM_OUT, zoomOut.node());
        gui.landmark(Landmarks.ZOOM_IN, zoomIn.node());
        gui.landmark(Landmarks.ZOOM, zoomLevel);
        // As wide as its longest word, so the buttons either side stay put as it changes.
        Node level = gui.row().width(ZOOM_LEVEL_W).justify(Justify.CENTER).alignItems(AlignItems.CENTER)
                .scroll(false, false).children(zoomLevel);
        zoom = gui.row().alignItems(AlignItems.CENTER).scroll(false, false)
                .children(zoomOut.node(), level, zoomIn.node());

        keep = new IconButton(gui, Icon.PIN_UP, "Keep shown").toggle(true).size(TOOL, TOOL_ICON)
                .tip("Keep shown");
        gui.landmark(Landmarks.KEEP, keep.node());
        trace = new IconButton(gui, Icon.BEZIER, "Trace to SDG").toggle(true).size(TOOL, TOOL_ICON)
                .tip("Trace to SDG (T)");
        gui.landmark(Landmarks.TRACE, trace.node());
        layersTool = new IconButton(gui, PixIcon.LAYERS, "Layers").toggle(true).size(TOOL, TOOL_ICON)
                .tip("Layers (L)");
        gui.landmark(Landmarks.LAYERS, layersTool.node());
        detailsTool = new IconButton(gui, Icon.INFO, "Details").toggle(true).size(TOOL, TOOL_ICON)
                .tip("Details (I)");
        gui.landmark(Landmarks.DETAILS, detailsTool.node());

        // The bar is the root's last child, so floating it puts it over the bottom of the stage, which then has the
        // whole window under the title bar; unfloated, it takes room of its own below the stage (keep). The zoom
        // joins it with the first picture wanted (splash).
        bar = gui.row().width(Length.percent(100f)).alignItems(AlignItems.CENTER).padding(Length.rem(0.5f), MARGIN)
                .gap(MARGIN).background(gui.theme().color(Role.CHROME)).scroll(false, false)
                .children(convertPanel.badge(), words, strip, zoom.visible(false), trace.node().visible(false),
                        layersTool.node().visible(false), detailsTool.node().visible(false), keep.node());
        gui.landmark(Landmarks.BAR, bar);
        keep(false);

        // The stage and, while a picture is traced, the Trace panel at its right.
        Node middle = gui.row().width(Length.FILL).height(Length.grow(1f)).scroll(false, false)
                .children(stage, tracePanel.panel(), layersPanel.panel(), detailsPanel.panel());

        // A picture that plays: a thin track along the bar's top edge, clicked to seek, and the playhead on it, moved by
        // the view's asked half as the picture plays (View.draw). Floated in the bar, so it goes where the bar goes,
        // over the stage or below it, and fades with it. Shown only while one plays.
        head = gui.box().width(SEEK_H).height(Length.percent(100f)).corner(Length.rem(0.15f))
                .background(gui.theme().color(Role.ACCENT)).floatAt(Length.ZERO, Length.ZERO).hitInert(true);
        seek = gui.row().width(Length.percent(100f)).height(SEEK_H).background(gui.theme().color(Role.DIM))
                .floatAt(Length.ZERO, Length.ZERO).scroll(false, false).visible(false).children(head);
        bar.append(seek);
        gui.landmark(Landmarks.SEEK, seek);
        gui.landmark(Landmarks.SEEK_HEAD, head);

        gui.root().direction(Direction.COLUMN)
                .background(gui.theme().color(Role.PAGE))
                .children(titleBar.node(), middle, bar);
    }

    /** A column the stage's height, floating at {@code x}, with {@code button} halfway up it. */
    private static Node side(Gui gui, IconButton button, Length x) {
        return gui.column().width(Length.AUTO).height(Length.percent(100f)).justify(Justify.CENTER)
                .padding(Length.ZERO, MARGIN).scroll(false, false).floatAt(x, Length.ZERO)
                .children(button.node());
    }

    private static Node detail(Gui gui, String text) {
        return gui.text(text).wordWrap(false).textSize(DETAIL_TEXT).textColor(gui.theme().color(Role.DIM));
    }

    Node stage() {
        return stage;
    }

    /**
     * The picture's two layers: one in front, the other the picture before, flung out as a step's new one is thrown
     * in. Each laid out fitted, and drawn zoomed, moved and faded by the view's asked half.
     */
    Node layer(int i) {
        return layers[i];
    }

    /** The row the details are in, whose width is the room they have. */
    Node details() {
        return details;
    }

    /** The {@code i}th detail, in {@link Details}'s order. */
    Node item(int i) {
        return items[i];
    }

    /** The dot before the {@code i}th detail; there is none before the first. */
    Node dot(int i) {
        return dots[i];
    }

    /** What fades when the pointer rests: the bar, and the columns Previous and Next are in. */
    List<Node> faded() {
        return List.of(bar, previousSide, nextSide);
    }

    /** What the pointer may rest on without the controls fading: the bar, Previous and Next. */
    Node[] controls() {
        return new Node[] {bar, previous.node(), next.node()};
    }

    /** The bar, whose content width is what the words, the strip and the tools share. */
    Node bar() {
        return bar;
    }

    /** The seek track, shown while a picture plays: clicked to go there. */
    Node seek() {
        return seek;
    }

    /** The playhead on the seek track, which the view's asked half moves. */
    Node head() {
        return head;
    }

    /** Keep shown's button, which is {@link #TOOL} wide: how the view knows a rem in px. */
    Node keepNode() {
        return keep.node();
    }

    /** The filmstrip's {@code i}th cell, left to right. */
    Node cell(int i) {
        return cells[i];
    }

    /** Every cell, shown or not: what a step glides along. */
    List<Node> cells() {
        return List.of(cells);
    }

    /** Show {@code count} cells, the strip {@code widthPercent} of the bar; none hides it. */
    void strip(int count, float widthPercent) {
        strip.visible(count > 0).width(Length.percent(widthPercent));
        for (int i = 0; i < cells.length; i++) {
            cells[i].visible(i < count);
        }
    }

    /**
     * Cell {@code i}: {@code feed} fitted in it as {@code fit} says and named {@code name}, marked when it is the
     * picture wanted; with no name, an empty cell past the folder's end, drawn as nothing and never pressed. A name
     * with no feed is a file that is not a picture: the cell is there, and empty.
     */
    void cell(int i, Feed feed, Fit fit, String name, boolean wanted) {
        there[i] = !name.isEmpty();
        this.wanted[i] = wanted;
        cells[i].hitInert(!there[i]);
        paintCell(i);
        cellNames[i].text(name);
        if (feed != null && fit != null) {
            thumbs[i].image(feed).width(Length.percent(fit.widthPercent())).height(Length.percent(fit.heightPercent()))
                    .visible(true);
        } else {
            thumbs[i].image((Feed) null).visible(false);
        }
    }

    /** Cell {@code i}'s thumbnail, which the view fades up as it is read. */
    Node thumb(int i) {
        return thumbs[i];
    }

    /**
     * A cell's edge, always there so marking one moves nothing: the accent on the picture wanted, which is not
     * pressed; on the others, nothing until the pointer is on one, a line then, and ink while it is pressed.
     */
    private void paintCell(int i) {
        boolean live = there[i] && !wanted[i];
        InteractionState s = live ? cellStates[i] : InteractionState.NORMAL;
        Role edge = wanted[i] ? Role.ACCENT
                : s == InteractionState.PRESSED ? Role.INK
                : s == InteractionState.HOVER ? Role.DIM
                : Role.NONE;
        cells[i].border(Length.rem(0.125f), edge)
                .background(s == InteractionState.NORMAL ? Role.WELL : Role.WELL.at(s));
        gui.cursor(cells[i], live ? CursorShape.POINTER : CursorShape.DEFAULT);
    }

    /** The bar's zoom, whose width the filmstrip leaves it. */
    Node zoomNode() {
        return zoom;
    }

    /** What the zoom says: {@link #FIT}, or a percent of the picture's own pixels. */
    void zoomLevel(String text) {
        zoomLevel.text(text);
    }

    /** The addresses − and + press with. */
    String zoomOutAddress() {
        return zoomOut.address();
    }

    String zoomInAddress() {
        return zoomIn.address();
    }

    /** The address Keep shown presses with. */
    String keepAddress() {
        return keep.address();
    }

    /** Keep shown on: the bar in the window's flow, below the picture; off, floating over the picture's bottom. */
    void keep(boolean kept) {
        keep.on(kept).icon(kept ? Icon.PIN_DOWN : Icon.PIN_UP);
        this.kept = kept;
        flow();
    }

    /**
     * The Trace panel open beside the stage, or closed. Open, the bar is in the window's flow as Keep shown puts it, so
     * it never covers the panel's buttons.
     */
    void tracing(boolean open) {
        tracing = open;
        trace.on(open);
        tracePanel.open(open);
        flow();
    }

    /** The Convert panel open over the stage, with the bar in the window's flow below it as Trace has it; or closed. */
    void converting(boolean open) {
        converting = open;
        convertPanel.open(open);
        flow();
    }

    /** The Layers panel open beside the stage, the bar in the flow below it as for Trace; or closed. */
    void layering(boolean open) {
        layering = open;
        layersTool.on(open);
        layersPanel.open(open);
        flow();
    }

    /** The Details panel open beside the stage, the bar in the flow below it; or closed. */
    void informing(boolean open) {
        informing = open;
        detailsTool.on(open);
        detailsPanel.open(open);
        flow();
    }

    DetailsPanel detailsPanel() {
        return detailsPanel;
    }

    String detailsAddress() {
        return detailsTool.address();
    }

    LayersPanel layersPanel() {
        return layersPanel;
    }

    String layersAddress() {
        return layersTool.address();
    }

    /** The picture shown is a raster, a drawing, or neither: Trace shows for the one, Layers for the other. */
    void kind(boolean raster, boolean drawing) {
        this.raster = raster;
        this.drawing = drawing;
        tools();
    }

    private void tools() {
        trace.node().visible(!splashed && raster);
        layersTool.node().visible(!splashed && drawing);
        detailsTool.node().visible(!splashed && (raster || drawing));
    }

    /** The format badge and the Convert panel. */
    ConvertPanel convertPanel() {
        return convertPanel;
    }

    /** The Trace panel and the split it makes of the stage. */
    TracePanel tracePanel() {
        return tracePanel;
    }

    /** The address Trace presses with. */
    String traceAddress() {
        return trace.address();
    }

    /** The bar below the picture while Keep shown is on or the Trace panel is open; else floating over it. */
    private void flow() {
        if (kept || tracing || converting || layering || informing) {
            bar.unfloat();
        } else {
            bar.floatAt(Length.ZERO, Length.percent(100f));
        }
    }

    /** The address Previous presses with. */
    String previousAddress() {
        return previous.address();
    }

    /** The address Next presses with. */
    String nextAddress() {
        return next.address();
    }

    /**
     * Layer {@code i} shows {@code feed}, laid out {@code widthPercent} by {@code heightPercent} of the stage and centred
     * in it; with none, it shows nothing and holds no feed. With a {@code drawing}, the drawing is drawn instead, sharp at
     * any zoom, and the feed (its poster) is not.
     */
    void layer(int i, Feed feed, ImageSdg drawing, float widthPercent, float heightPercent) {
        Node n = layers[i];
        if (feed == null) {
            n.image((Feed) null).sdg(null).visible(false);
            return;
        }
        if (drawing != null) {
            n.image((Feed) null).sdg(drawing).visible(true);
        } else {
            n.image(feed).sdg(null).visible(true);
        }
        place(n, widthPercent, heightPercent);
    }

    /** Layer {@code i} at a new size, the stage having changed under it. */
    void fit(int i, float widthPercent, float heightPercent) {
        place(layers[i], widthPercent, heightPercent);
    }

    /** Floated, so the two layers can lie over each other, and centred by where it floats. */
    private static void place(Node n, float widthPercent, float heightPercent) {
        n.width(Length.percent(widthPercent)).height(Length.percent(heightPercent))
                .floatAt(Length.percent((100f - widthPercent) / 2f), Length.percent((100f - heightPercent) / 2f));
    }

    /**
     * Layer {@code i} in front of the other, the newest picture drawn last, and still under Previous and Next; and the
     * one the {@code picture} landmark names.
     */
    void front(int i) {
        stage.insert(layers[i], 1);
        gui.landmark(Landmarks.PICTURE, layers[i]);
    }

    /** {@code why} said in the picture's place, for a file that is not a picture; or, with null, nothing said. */
    void note(String why) {
        if (why != null) {
            note.text(why);
        }
    }

    /**
     * The splash in the stage, after the note, and the controls that act on a picture out of the window: Previous,
     * Next and the zoom, which have nothing to do while there is none. Or the other way round. Shown and hidden, so
     * what is hidden takes neither room nor the pointer and comes back as it was: removing a node releases it, and
     * one removed cannot be put back (FN-19).
     */
    void splash(boolean shown) {
        if (shown == splashed) {
            return;
        }
        splashed = shown;
        splash.node().visible(shown);
        previousSide.visible(!shown);
        nextSide.visible(!shown);
        zoom.visible(!shown);
        tools();
    }

    /** The address the splash's button presses with. */
    String splashOpenAddress() {
        return splash.openAddress();
    }

    /** What is said in a picture's place, which the view shows and hides by shading it. */
    Node noteNode() {
        return note;
    }

    /** Full screen, the title bar gone so the picture has the whole display; or the window again, with it. */
    void fullScreen(boolean on) {
        titleBar.node().visible(!on);
    }

    /**
     * The picture's name in the title bar, before the wordmark, and in the bar; with none, the title bar has the mark
     * alone and the bar asks for a picture, since a click on the name opens one.
     */
    void name(String name) {
        titleBar.title(name);
        this.name.text(name.isEmpty() ? OPEN_A_PICTURE : name);
        named = !name.isEmpty();
        paintName();
    }

    /** The name in ink, or dim while it asks for a picture; in the accent under the pointer, deeper while pressed. */
    private void paintName() {
        Role ink = named ? Role.INK : Role.DIM;
        name.textColor(nameState == InteractionState.NORMAL ? ink : Role.ACCENT.at(nameState));
    }


    /** The name, which opens a picture when clicked. */
    Node nameNode() {
        return name;
    }

    /** What the {@code i}th detail says. */
    void detail(int i, String text) {
        items[i].text(text);
    }

    /** Which details are drawn, and the dots between those that are. */
    void shown(boolean[] shown) {
        boolean before = false;
        for (int i = 0; i < items.length; i++) {
            items[i].visible(shown[i]);
            if (dots[i] != null) {
                dots[i].visible(shown[i] && before);
            }
            before |= shown[i];
        }
    }
}
