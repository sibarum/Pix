package dev.sibarum.pix;

import dev.vexelray.gui.core.Feed;
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

    /** Between the details: a dot with a space each side, so the gap is part of what is measured. */
    static final String DOT = " · ";

    private final TitleBar titleBar;
    private final Node stage;
    private final Gui gui;
    private final Node[] layers = new Node[2];
    private final Node note;
    private final IconButton previous;
    private final IconButton next;
    /** The columns Previous and Next are in, which fade with the bar. */
    private final Node previousSide;
    private final Node nextSide;
    private final Node bar;
    private final IconButton keep;
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

        previousSide = side(gui, previous, Length.ZERO);
        nextSide = side(gui, next, Length.percent(100f));
        stage = gui.box()
                .width(Length.FILL).height(Length.grow(1f))
                .direction(Direction.COLUMN).justify(Justify.CENTER).alignItems(AlignItems.CENTER)
                .clip(true)
                .children(layers[0], layers[1], note, previousSide, nextSide);
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

        keep = new IconButton(gui, Icon.PIN_UP, "Keep shown").toggle(true).size(TOOL, TOOL_ICON)
                .tip("Keep shown");
        gui.landmark(Landmarks.KEEP, keep.node());

        // The bar is the root's last child, so floating it puts it over the bottom of the stage, which then has the
        // whole window under the title bar; unfloated, it takes room of its own below the stage (keep).
        bar = gui.row().width(Length.percent(100f)).alignItems(AlignItems.CENTER).padding(Length.rem(0.5f), MARGIN)
                .gap(MARGIN).background(gui.theme().color(Role.CHROME)).scroll(false, false)
                .children(words, strip, keep.node());
        gui.landmark(Landmarks.BAR, bar);
        keep(false);

        gui.root().direction(Direction.COLUMN)
                .background(gui.theme().color(Role.PAGE))
                .children(titleBar.node(), stage, bar);
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

    /** The address Keep shown presses with. */
    String keepAddress() {
        return keep.address();
    }

    /** Keep shown on: the bar in the window's flow, below the picture; off, floating over the picture's bottom. */
    void keep(boolean kept) {
        keep.on(kept).icon(kept ? Icon.PIN_DOWN : Icon.PIN_UP);
        if (kept) {
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
     * in it; with none, it shows nothing and holds no feed.
     */
    void layer(int i, Feed feed, float widthPercent, float heightPercent) {
        Node n = layers[i];
        if (feed == null) {
            n.image((Feed) null).visible(false);
            return;
        }
        n.image(feed).visible(true);
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

    /** What is said in a picture's place, which the view shows and hides by shading it. */
    Node noteNode() {
        return note;
    }

    /** Full screen, the title bar gone so the picture has the whole display; or the window again, with it. */
    void fullScreen(boolean on) {
        titleBar.node().visible(!on);
    }

    /**
     * The picture's name in the title bar and the bar; with none, the bar asks for one, since a click on the name
     * opens a picture.
     */
    void name(String name) {
        titleBar.title(name.isEmpty() ? Pix.TITLE : name + " — " + Pix.TITLE);
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
