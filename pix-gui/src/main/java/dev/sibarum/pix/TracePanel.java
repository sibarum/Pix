package dev.sibarum.pix;

import dev.sibarum.pix.core.Messages.TraceSettings;
import dev.vexelray.canvas.Color;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.core.ImageSdg;
import dev.vexelray.gui.core.Node;
import dev.vexelray.gui.core.layout.LayoutEnums.AlignItems;
import dev.vexelray.gui.core.layout.LayoutEnums.Direction;
import dev.vexelray.gui.core.layout.LayoutEnums.Justify;
import dev.vexelray.gui.core.layout.Length;
import dev.vexelray.gui.core.style.Role;
import dev.vexelray.gui.icons.Icon;
import dev.vexelray.gui.widget.Button;
import dev.vexelray.gui.widget.IconButton;
import dev.vexelray.gui.widget.Lattice;
import dev.vexelray.gui.widget.Segment;
import dev.vexelray.gui.widget.Slider;

import java.util.List;

/**
 * The Trace panel and the split it makes of the stage (docs/trace.md): at the stage's right, the settings a picture is
 * traced with, what the trace comes to, and Save; on the stage, the trace drawn over the picture right of a line, so the
 * two are compared where they meet.
 *
 * <h2>One picture drawn twice</h2>
 *
 * <p>The trace is laid out the picture's size, at the top left of a box that covers the stage right of the line and clips
 * to it, and drawn moved from there to where the picture is laid out ({@link View#draw}), at the picture's own zoom and
 * offset: the same size about the same centre, it scales with the picture exactly, and the line is the only place they
 * differ. Not centred in the box: a node wider than its box is laid out from its start, so where it would land is
 * known only by measuring, and a measured box is where it is drawn, offset and all.
 *
 * <p>Like {@link Ui}, it holds no state but what it shows, and every word is in dp, every control in rem.
 */
final class TracePanel {

    /** The panel's width. */
    static final Length WIDTH = Length.rem(17f);

    /** A small heading over each setting. */
    static final Length LABEL_TEXT = Length.dp(11f);

    /** The title. */
    static final Length TITLE_TEXT = Length.dp(15f);

    /** A swatch of the trace's palette. */
    static final Length SWATCH = Length.rem(1.25f);

    /** The line between the picture and the trace, and its grip. */
    static final Length LINE_W = Length.dp(2f);
    static final float GRIP_REM = 0.75f;
    static final Length GRIP_W = Length.rem(GRIP_REM);
    static final Length GRIP_H = Length.rem(2.5f);

    /** How near the line, in rem either side, a drag on the stage moves the line rather than the picture. */
    static final float GRAB_REM = 1f;

    /** What the panel says while a trace for the settings shown is out. */
    static final String TRACING = "Tracing…";

    private final Gui gui;
    private final Node panel;
    private final Node onStage;
    private final Node compare;
    private final Node traced;
    private final Node line;
    private final Node before;
    private final Node after;
    private final Slider colours;
    private final Node coloursValue;
    private final Node[] swatches = new Node[TraceSettings.MOST];
    private final Segment<Boolean> gradients;
    private final Node gradientsValue;
    private final Node beforeBytes;
    private final Node afterBytes;
    private final Node layers;
    private final Node status;
    private final IconButton close;
    private final Button cancel;
    private final Button save;

    TracePanel(Gui gui) {
        this.gui = gui;
        Color dim = gui.theme().color(Role.DIM);

        // ---- on the stage
        traced = gui.box().paintCuts(true);
        compare = gui.box().width(Length.percent(50f)).height(Length.percent(100f))
                .direction(Direction.ROW).justify(Justify.START).alignItems(AlignItems.START)
                .clip(true).scroll(false, false).hitInert(true).floatAt(Length.percent(50f), Length.ZERO)
                .visible(false).children(traced);
        gui.landmark(Landmarks.TRACED, traced);
        Node bar = gui.box().width(LINE_W).height(Length.percent(100f)).background(Role.INK);
        Node grip = gui.box().size(GRIP_W, GRIP_H).corner(Length.rem(0.375f)).background(Role.INK)
                .floatAt(Length.ZERO, Length.percent(50f));
        line = gui.box().width(GRIP_W).height(Length.percent(100f)).hitInert(true).scroll(false, false)
                .direction(Direction.COLUMN).alignItems(AlignItems.CENTER).justify(Justify.CENTER)
                .floatAt(Length.percent(50f), Length.ZERO).visible(false).children(bar, grip);
        gui.landmark(Landmarks.TRACE_LINE, line);
        before = tag(gui, "");
        after = tag(gui, "");
        before.floatAt(Length.ZERO, Length.ZERO);
        after.floatAt(Length.percent(100f), Length.ZERO);
        // The split moves the box and the line by layout while the view moves the trace by its draw offset at once:
        // the window's slide would put a second clock on the first, and the right side would wave as the line is
        // dragged. So neither the box, nor the line, nor the trace in the box glides; they are where the split is.
        compare.childMotion(false);
        onStage = gui.box().width(Length.percent(100f)).height(Length.percent(100f)).floatAt(Length.ZERO, Length.ZERO)
                .hitInert(true).scroll(false, false).childMotion(false).children(compare, before, after, line);

        // ---- the panel
        Node title = gui.text("Trace to SDG").wordWrap(false).textSize(TITLE_TEXT).bold()
                .textColor(gui.theme().color(Role.INK));
        Node hint = gui.text("Drag the line to compare").wordWrap(false).textSize(Ui.DETAIL_TEXT).textColor(dim);
        close = new IconButton(gui, Icon.CLOSE, "Close").size(Ui.TOOL, Ui.TOOL_ICON).tip("Close (Esc)");
        Node heading = gui.row().width(Length.FILL).alignItems(AlignItems.START).scroll(false, false)
                .children(gui.column().width(Length.grow(1f)).gap(Ui.LINE_GAP).scroll(false, false)
                        .children(title, hint), close.node());

        coloursValue = value(gui, "");
        colours = new Slider(gui, slider(TraceSettings.FIRST.colours()))
                .lattice(Lattice.intervals(TraceSettings.MOST - TraceSettings.FEWEST));
        colours.node().width(Length.FILL);
        gui.landmark(Landmarks.TRACE_COLOURS, colours.node());
        Node palette = gui.row().width(Length.FILL).gap(Length.rem(0.25f)).scroll(false, false);
        for (int i = 0; i < swatches.length; i++) {
            swatches[i] = gui.box().size(SWATCH, SWATCH).corner(Length.rem(0.25f))
                    .border(Length.dp(1f), gui.theme().color(Role.LINE)).visible(false);
            palette.append(swatches[i]);
        }

        gradientsValue = value(gui, "");
        gradients = new Segment<Boolean>(gui).option("Flatten", false).option("Keep", true)
                .select(TraceSettings.FIRST.texture());
        gui.landmark(Landmarks.TRACE_GRADIENTS, gradients.node());

        beforeBytes = value(gui, "");
        afterBytes = value(gui, "");
        layers = value(gui, "");
        gui.landmark(Landmarks.TRACE_AFTER, afterBytes);
        gui.landmark(Landmarks.TRACE_LAYERS, layers);
        Node sums = gui.column().width(Length.FILL).gap(Length.rem(0.375f)).padding(Length.rem(0.75f))
                .corner(Length.rem(0.5f)).background(Role.WELL).scroll(false, false)
                .children(sum(gui, "Before", beforeBytes), sum(gui, "After", afterBytes), sum(gui, "Layers", layers),
                        sum(gui, "Sharp to", value(gui, "any size")));

        status = gui.text("").textSize(Ui.DETAIL_TEXT).textColor(dim).width(Length.FILL);
        gui.landmark(Landmarks.TRACE_STATUS, status);

        cancel = new Button(gui, "Cancel").kind(Button.Kind.SECONDARY);
        save = new Button(gui, "Save").kind(Button.Kind.PRIMARY);
        gui.landmark(Landmarks.TRACE_SAVE, save.node());
        Node buttons = gui.row().width(Length.FILL).justify(Justify.END).gap(Length.rem(0.5f)).scroll(false, false)
                .children(cancel.node(), save.node());

        panel = gui.column().width(WIDTH).height(Length.percent(100f)).padding(Length.rem(1f))
                .gap(Length.rem(1.25f)).background(Role.CHROME).scroll(false, true).visible(false)
                .children(heading,
                        section(gui, "Colours", coloursValue, colours.node(), palette),
                        section(gui, "Gradients", gradientsValue,
                                gui.row().scroll(false, false).children(gradients.node())),
                        sums, status, buttons);
        gui.landmark(Landmarks.TRACE_PANEL, panel);
    }

    /** A heading in small capitals and what it is set to, over the control that sets it. */
    private static Node section(Gui gui, String name, Node value, Node... controls) {
        Node head = gui.row().width(Length.FILL).justify(Justify.SPACE_BETWEEN).scroll(false, false)
                .children(label(gui, name), value);
        Node out = gui.column().width(Length.FILL).gap(Length.rem(0.5f)).scroll(false, false).children(head);
        for (Node c : controls) {
            out.append(c);
        }
        return out;
    }

    private static Node sum(Gui gui, String name, Node value) {
        return gui.row().width(Length.FILL).justify(Justify.SPACE_BETWEEN).scroll(false, false)
                .children(label(gui, name), value);
    }

    private static Node label(Gui gui, String text) {
        return gui.text(text.toUpperCase(java.util.Locale.ROOT)).wordWrap(false).textSize(LABEL_TEXT).bold()
                .textColor(gui.theme().color(Role.DIM));
    }

    private static Node value(Gui gui, String text) {
        return gui.text(text).wordWrap(false).textSize(Ui.DETAIL_TEXT).textColor(gui.theme().color(Role.INK));
    }

    /** A word on the stage, in a pill so it reads over any picture. */
    private static Node tag(Gui gui, String text) {
        return gui.text(text).wordWrap(false).textSize(Ui.DETAIL_TEXT).textColor(gui.theme().color(Role.INK))
                .padding(Length.rem(0.25f), Length.rem(0.5f)).corner(Length.rem(0.75f)).background(Role.CHROME)
                .hitInert(true).visible(false);
    }

    /** The slider's place, 0 to 1, for {@code colours}. */
    static float slider(int colours) {
        return (colours - TraceSettings.FEWEST) / (float) (TraceSettings.MOST - TraceSettings.FEWEST);
    }

    /** The colours the slider's {@code value}, 0 to 1, names. */
    static int colours(double value) {
        return TraceSettings.FEWEST + (int) Math.round(value * (TraceSettings.MOST - TraceSettings.FEWEST));
    }

    /** The panel, at the stage's right. */
    Node panel() {
        return panel;
    }

    /** What goes on the stage, over the picture and under Previous and Next: the trace, the line, the two tags. */
    List<Node> overStage() {
        return List.of(onStage);
    }

    /** The trace's node, which the view draws at the picture's zoom and offset, moved left as the class says. */
    Node traced() {
        return traced;
    }

    /** The line, which the view draws moved left half its width, so its middle is at the split. */
    Node line() {
        return line;
    }

    /** The panel shown, or hidden with the stage whole again. */
    void open(boolean open) {
        panel.visible(open);
        if (!open) {
            drawing(null, 0f, 0f);
        }
    }

    /**
     * The trace to compare, laid out {@code widthPercent} by {@code heightPercent} of the stage, as the picture is; none
     * leaves the picture whole. Shown with the line and the tags; nothing of them without it.
     */
    void drawing(ImageSdg drawing, float widthPercent, float heightPercent) {
        boolean on = drawing != null;
        traced.sdg(drawing);
        compare.visible(on);
        line.visible(on);
        before.visible(on);
        after.visible(on);
        fit(widthPercent, heightPercent);
    }

    /** The split at {@code percent} of the stage from its left: where the trace starts and the line is. */
    void split(float percent) {
        splitPercent = percent;
        compare.width(Length.percent(100f - percent)).floatAt(Length.percent(percent), Length.ZERO);
        line.floatAt(Length.percent(percent), Length.ZERO);
        size();
    }

    /** The trace at the picture's size, {@code widthPercent} by {@code heightPercent} of the stage. */
    void fit(float widthPercent, float heightPercent) {
        this.widthPercent = widthPercent;
        this.heightPercent = heightPercent;
        size();
    }

    /** As wide as the picture in a box the split makes narrower than the stage. */
    private void size() {
        float box = Math.max(1f, 100f - splitPercent);
        traced.width(Length.percent(widthPercent * 100f / box)).height(Length.percent(heightPercent));
    }

    private float splitPercent = 50f;
    private float widthPercent;
    private float heightPercent;

    /** What the stage's tags say: the picture's kind and size, and the trace's. */
    void tags(String picture, String trace) {
        before.text(picture);
        after.text(trace);
    }

    /** The settings shown, as the person last set them. */
    void settings(TraceSettings s) {
        coloursValue.text(String.valueOf(s.colours()));
        gradientsValue.text(s.texture() ? "Kept as texture" : "Flattened");
    }

    /** What the trace comes to: its palette, its size as a file, its layers; or nothing yet. */
    void result(List<Integer> palette, String bytes, String layerCount) {
        for (int i = 0; i < swatches.length; i++) {
            boolean on = i < palette.size();
            swatches[i].visible(on);
            if (on) {
                int c = palette.get(i);
                swatches[i].background(Color.rgba(((c >>> 24) & 0xff) / 255f, ((c >>> 16) & 0xff) / 255f,
                        ((c >>> 8) & 0xff) / 255f, 1f));
            }
        }
        afterBytes.text(bytes);
        layers.text(layerCount);
    }

    /** The picture's own size on disk. */
    void before(String bytes) {
        beforeBytes.text(bytes);
    }

    /** What is said under the sums: {@link #TRACING}, why a trace or a save failed, or nothing. */
    void status(String text) {
        status.text(text);
    }

    /** Save names the file it will write; while a save is out it says so and cannot be pressed again. */
    void save(String name, boolean saving) {
        save.label(saving ? "Saving…" : "Save " + name).enabled(!saving);
    }

    String colourAddress() {
        return colours.address();
    }

    String gradientsAddress() {
        return gradients.address();
    }

    String closeAddress() {
        return close.address();
    }

    String cancelAddress() {
        return cancel.address();
    }

    String saveAddress() {
        return save.address();
    }
}
