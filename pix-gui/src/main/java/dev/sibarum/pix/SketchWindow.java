package dev.sibarum.pix;

import dev.sibarum.pix.core.Messages.Brush;
import dev.vexelray.framework.shell.Window;
import dev.vexelray.gui.core.Feed;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.core.Node;
import dev.vexelray.gui.core.layout.LayoutEnums.AlignItems;
import dev.vexelray.gui.core.layout.LayoutEnums.Direction;
import dev.vexelray.gui.core.layout.LayoutEnums.Justify;
import dev.vexelray.gui.core.layout.Length;
import dev.vexelray.gui.core.style.Role;
import dev.vexelray.gui.widget.Button;
import dev.vexelray.gui.widget.Segment;
import dev.vexelray.gui.widget.Slider;

/**
 * The SDG editor's window (docs/sdg-editor.md): a bar saying what is being edited, with Save and what the last save
 * said; under it the brush (paint or erase, its size, strength and hardness) and the picture to trace over; then the
 * stage, where the field is shown fitted at its own aspect, over the picture if there is one. A satellite of the main
 * window, declared by name ({@code Recipes.sketchWindow}) and opened by Create in the New SDG window.
 *
 * <p>Holds no state but what it shows, as {@link Ui} does not; {@link SketchView} decides what that is.
 */
final class SketchWindow {

    /** The window's name, in a {@code WindowCommand}, a view's {@code window} and to a script ({@code sdg/...}). */
    static final String KEY = "sdg";
    static final String TITLE = "SDG";
    static final int W = 1040;
    static final int H = 760;

    /** A brush slider's width, and its value's. */
    static final Length SLIDER_W = Length.rem(7f);
    static final Length VALUE_W = Length.rem(2.75f);

    private final Gui gui;
    private final Node title;
    private final Node status;
    private final Button save;
    private final Node tools;
    private final Segment<Boolean> erase;
    private final Slider size;
    private final Slider strength;
    private final Slider hardness;
    private final Node sizeValue;
    private final Node strengthValue;
    private final Node hardnessValue;
    private final Button trace;
    private final Node stage;
    /** The field's box, fitted to the stage; the picture traced over, fitted in it; the field, over the whole box. */
    private final Node frame;
    private final Node backdrop;
    private final Node field;

    SketchWindow(Window window) {
        gui = window.gui();
        title = gui.text("").wordWrap(false).textSize(TracePanel.TITLE_TEXT).bold()
                .textColor(gui.theme().color(Role.INK));
        gui.landmark(Landmarks.SKETCH_TITLE, title);
        status = gui.text("").wordWrap(false).textSize(Ui.DETAIL_TEXT).textColor(gui.theme().color(Role.DIM))
                .width(Length.grow(1f));
        gui.landmark(Landmarks.SKETCH_STATUS, status);
        save = new Button(gui, "Save").kind(Button.Kind.PRIMARY).name(Landmarks.SKETCH_SAVE).enabled(false);
        gui.landmark(Landmarks.SKETCH_SAVE, save.node());
        Node bar = gui.row().width(Length.FILL).padding(Length.rem(0.5f), Ui.MARGIN).gap(Length.rem(0.75f))
                .alignItems(AlignItems.CENTER).scroll(false, false).background(gui.theme().color(Role.PANEL))
                .children(title, status, save.node());

        erase = new Segment<Boolean>(gui).option("Paint", false).option("Erase", true).name(Landmarks.SKETCH_ERASE);
        gui.landmark(Landmarks.SKETCH_ERASE, erase.node());
        size = slider(Landmarks.SKETCH_SIZE, SketchView.sizeSlider(Brush.FIRST.radius()));
        strength = slider(Landmarks.SKETCH_STRENGTH, Brush.FIRST.strength());
        hardness = slider(Landmarks.SKETCH_HARDNESS, Brush.FIRST.hardness());
        sizeValue = value();
        strengthValue = value();
        hardnessValue = value();
        trace = new Button(gui, "Trace over…").kind(Button.Kind.SECONDARY).name(Landmarks.SKETCH_TRACE);
        gui.landmark(Landmarks.SKETCH_TRACE, trace.node());
        Node spring = gui.box().width(Length.grow(1f));
        tools = gui.row().width(Length.FILL).padding(Length.rem(0.375f), Ui.MARGIN).gap(Length.rem(0.625f))
                .alignItems(AlignItems.CENTER).scroll(false, false).background(gui.theme().color(Role.PANEL))
                .children(erase.node(), label("Size"), size.node(), sizeValue, label("Strength"), strength.node(),
                        strengthValue, label("Hardness"), hardness.node(), hardnessValue, spring, trace.node())
                .visible(false);

        backdrop = gui.box().visible(false);
        field = gui.box().floatAt(Length.dp(0f), Length.dp(0f)).width(Length.percent(100f))
                .height(Length.percent(100f));
        gui.landmark(Landmarks.SKETCH_FIELD, field);
        frame = gui.box().alignItems(AlignItems.CENTER).justify(Justify.CENTER).scroll(false, false)
                .children(backdrop, field).visible(false);
        stage = gui.box().width(Length.FILL).height(Length.grow(1f)).clip(true).scroll(false, false)
                .alignItems(AlignItems.CENTER).justify(Justify.CENTER).padding(Ui.MARGIN)
                .background(gui.theme().color(Role.WELL)).children(frame);
        Node root = gui.root().direction(Direction.COLUMN).background(gui.theme().color(Role.PAGE));
        if (window.titleBar() != null) {
            root.children(window.titleBar().node(), bar, tools, stage);
        } else {
            root.children(bar, tools, stage);
        }
    }

    private Slider slider(String landmark, float value) {
        Slider s = new Slider(gui, value).name(landmark);
        s.node().width(SLIDER_W);
        gui.landmark(landmark, s.node());
        return s;
    }

    private Node label(String text) {
        return gui.text(text).wordWrap(false).textSize(Ui.DETAIL_TEXT).textColor(gui.theme().color(Role.DIM));
    }

    private Node value() {
        return gui.text("").wordWrap(false).width(VALUE_W).textSize(Ui.DETAIL_TEXT)
                .textColor(gui.theme().color(Role.INK));
    }

    /**
     * The field's preview, its box fitted at {@code fit} of the stage; null shows none. The feed is written again as the
     * field is painted, so this is said once a field.
     */
    void field(Feed feed, Fit fit) {
        if (feed == null) {
            frame.visible(false);
            return;
        }
        field.image(feed);
        frame.width(Length.percent(fit.widthPercent())).height(Length.percent(fit.heightPercent())).visible(true);
    }

    /** The picture traced over, fitted at {@code fit} of the field's box; null shows none. */
    void backdrop(Feed feed, Fit fit) {
        if (feed == null) {
            backdrop.image((Feed) null).visible(false);
            return;
        }
        backdrop.image(feed).width(Length.percent(fit.widthPercent())).height(Length.percent(fit.heightPercent()))
                .visible(true);
    }

    /** The brush's row shown, for a mode that paints; its Trace over…, for one that traces. */
    void tools(boolean paints, boolean traces) {
        tools.visible(paints);
        trace.node().visible(traces);
    }

    /** The brush as it is shown: its sliders put back and what each says. */
    void brush(Brush b, String sizeSays, String strengthSays, String hardnessSays) {
        erase.show(b.erase());
        size.show(SketchView.sizeSlider(b.radius()));
        strength.show(b.strength());
        hardness.show(b.hardness());
        sizeValue.text(sizeSays);
        strengthValue.text(strengthSays);
        hardnessValue.text(hardnessSays);
    }

    /** What is being edited: the mode, and the size as the person gave it. */
    void title(String text) {
        title.text(text);
    }

    /** What the last save said, or what a save is doing; Save offered or not. */
    void status(String text, boolean danger, boolean canSave, String saveLabel) {
        status.text(text).textColor(gui.theme().color(danger ? Role.DANGER : Role.DIM));
        save.label(saveLabel).enabled(canSave);
    }

    Node stage() {
        return stage;
    }

    /** The field's box, which a drag on paints. */
    Node fieldNode() {
        return field;
    }

    String saveAddress() {
        return save.address();
    }

    String traceAddress() {
        return trace.address();
    }

    String eraseAddress() {
        return erase.address();
    }

    String sizeAddress() {
        return size.address();
    }

    String strengthAddress() {
        return strength.address();
    }

    String hardnessAddress() {
        return hardness.address();
    }

    /** This window's tree: for a test. */
    Gui gui() {
        return gui;
    }
}
