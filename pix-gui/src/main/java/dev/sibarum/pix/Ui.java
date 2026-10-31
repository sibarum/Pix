package dev.sibarum.pix;

import dev.vexelray.gui.core.Feed;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.core.Node;
import dev.vexelray.gui.core.input.CursorShape;
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
 * bar's padding, the room around them — is in rem, and every word Pix writes is in dp: making the controls smaller
 * never makes the name too small to read. This is the framework's rule turned round on purpose (its own is type in rem,
 * gutters in dp), because what the zoom is for here is room for the picture, not reading.
 *
 * <p>It holds no state. The picture shown is the view's to know, which details fit is the view's to decide
 * ({@link Details}), and what each label says is what the viewer last told the view. Built in the {@code TREE} phase,
 * before there is a window, and needing none, so a test can build the real tree headless.
 */
final class Ui {

    /** The name, and what is said in a picture's place. */
    static final Length NAME_TEXT = Length.dp(14f);

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
    private final Node picture;
    private final Node note;
    private final IconButton previous;
    private final IconButton next;
    /** The columns Previous and Next are in, which fade with the bar. */
    private final Node previousSide;
    private final Node nextSide;
    private final Node bar;
    private final IconButton keep;
    private final Node name;
    private final Node details;
    /** The details in {@link Details}'s order, and the dot before each but the first. */
    private final Node[] items;
    private final Node[] dots;
    /** The filmstrip, between the words and the tools, and its cells: each a box, the thumbnail in it, and its name. */
    private final Node strip;
    private final Node[] cells;
    private final Node[] thumbs;
    private final Node[] cellNames;

    Ui(Gui gui, TitleBar titleBar) {
        this.titleBar = titleBar;
        // The zoom sizes the controls, and what they give up is the picture's: the window keeps its size.
        gui.zoomSizesWindow(false);

        picture = gui.box();
        gui.landmark(Landmarks.PICTURE, picture);
        note = gui.text("").textSize(NAME_TEXT).textColor(gui.theme().color(Role.DIM)).visible(false);
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
                .children(picture, note, previousSide, nextSide);
        gui.landmark(Landmarks.STAGE, stage);

        name = gui.text("").wordWrap(false).ellipsis(Ellipsis.MIDDLE).width(Length.FILL)
                .textSize(NAME_TEXT).textColor(gui.theme().color(Role.INK));
        gui.landmark(Landmarks.BAR_NAME, name);
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
        Node words = gui.column().width(Length.grow(1f)).gap(LINE_GAP).scroll(false, false).children(name, details);

        cells = new Node[Strip.MOST];
        thumbs = new Node[Strip.MOST];
        cellNames = new Node[Strip.MOST];
        strip = gui.row().width(Length.ZERO).height(Length.rem(Strip.CELL_H)).gap(Length.rem(Strip.GAP))
                .alignItems(AlignItems.CENTER).clip(true).scroll(false, false).visible(false);
        gui.landmark(Landmarks.STRIP, strip);
        for (int i = 0; i < cells.length; i++) {
            thumbs[i] = gui.box().hitInert(true);
            cellNames[i] = gui.text("").hitInert(true).visible(false);
            cells[i] = gui.box().role("button").width(Length.rem(Strip.CELL_W)).height(Length.rem(Strip.CELL_H))
                    .direction(Direction.COLUMN).justify(Justify.CENTER).alignItems(AlignItems.CENTER)
                    .corner(Length.rem(0.25f)).clip(true).scroll(false, false)
                    .background(gui.theme().color(Role.WELL)).children(thumbs[i], cellNames[i]);
            gui.cursor(cells[i], CursorShape.POINTER);
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

    /** The picture's node: laid out fitted, and drawn zoomed and moved by the view's asked half. */
    Node picture() {
        return picture;
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
        Node c = cells[i];
        boolean there = !name.isEmpty();
        c.opacity(there ? 1f : 0f).hitInert(!there)
                .border(Length.rem(wanted ? 0.125f : 0f), wanted ? Role.ACCENT : Role.LINE);
        cellNames[i].text(name);
        if (feed != null && fit != null) {
            thumbs[i].image(feed).width(Length.percent(fit.widthPercent())).height(Length.percent(fit.heightPercent()))
                    .visible(true);
        } else {
            thumbs[i].image((Feed) null).visible(false);
        }
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
     * Show {@code feed}, drawn {@code widthPercent} by {@code heightPercent} of the stage and centred in it, or, with no
     * feed, {@code why} in its place.
     */
    void picture(Feed feed, float widthPercent, float heightPercent, String why) {
        if (feed != null) {
            picture.image(feed).width(Length.percent(widthPercent)).height(Length.percent(heightPercent))
                    .visible(true);
            note.visible(false);
        } else {
            picture.image((Feed) null).visible(false);
            note.text(why).visible(true);
        }
    }

    /** Draw the picture at a new size, the stage having changed under it. */
    void fit(float widthPercent, float heightPercent) {
        picture.width(Length.percent(widthPercent)).height(Length.percent(heightPercent));
    }

    /** The picture's name in the title bar and the bar. */
    void name(String name) {
        titleBar.title(name.isEmpty() ? Pix.TITLE : name + " — " + Pix.TITLE);
        this.name.text(name);
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
