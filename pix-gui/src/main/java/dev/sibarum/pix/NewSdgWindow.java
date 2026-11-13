package dev.sibarum.pix;

import dev.sibarum.pix.core.Messages.EditMode;
import dev.sibarum.pix.core.Messages.Sizing;
import dev.sibarum.pix.core.Messages.Unit;
import dev.vexelray.framework.shell.Window;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.core.Node;
import dev.vexelray.gui.core.layout.LayoutEnums.AlignItems;
import dev.vexelray.gui.core.layout.LayoutEnums.Direction;
import dev.vexelray.gui.core.layout.LayoutEnums.Justify;
import dev.vexelray.gui.core.layout.Length;
import dev.vexelray.gui.core.style.Role;
import dev.vexelray.gui.widget.Button;
import dev.vexelray.gui.widget.NumberField;
import dev.vexelray.gui.widget.Segment;

/**
 * The New SDG window (docs/sdg-editor.md): how big the drawing is, in pixels or in a length at a DPI, what its field
 * starts as, and which mode it is edited in; then Create or Cancel. A satellite of the main window, declared by name
 * ({@code Recipes.newSdgWindow}), opened by the New menu or Ctrl+N as a {@code WindowCommand}, as MainFrame's Open
 * window is.
 *
 * <p>Holds no state but what it shows: what the person has chosen is {@link NewSdgView}'s.
 */
final class NewSdgWindow {

    /** The window's name, in a {@code WindowCommand} and to a script ({@code new-sdg/...}). */
    static final String KEY = "new-sdg";
    static final String TITLE = "New SDG";
    static final int W = 460;
    static final int H = 500;

    /** The labels' column. */
    static final Length LABEL_W = Length.rem(5f);

    private final Gui gui;
    private final Segment<Unit> unit;
    private final NumberField width;
    private final NumberField height;
    private final NumberField dpi;
    private final Node dpiRow;
    private final Node texels;
    private final Segment<Boolean> starts;
    private final Segment<EditMode> mode;
    private final Node modeSays;
    private final Button cancel;
    private final Button create;

    NewSdgWindow(Window window) {
        gui = window.gui();
        unit = new Segment<Unit>(gui);
        for (Unit u : Unit.values()) {
            unit.option(u.label(), u);
        }
        unit.name(Landmarks.NEW_UNIT);
        width = new NumberField(gui, Sizing.FIRST.width(), 1, 0.01, Sizing.MOST_SIDE).name(Landmarks.NEW_WIDTH);
        height = new NumberField(gui, Sizing.FIRST.height(), 1, 0.01, Sizing.MOST_SIDE).name(Landmarks.NEW_HEIGHT);
        dpi = new NumberField(gui, Sizing.FIRST.dpi(), 1, 1, 9600).name(Landmarks.NEW_DPI);
        texels = gui.text("").textSize(Ui.DETAIL_TEXT).width(Length.FILL);
        gui.landmark(Landmarks.NEW_TEXELS, texels);
        starts = new Segment<Boolean>(gui).option("Inside", true).option("Outside", false).name(Landmarks.NEW_STARTS);
        mode = new Segment<EditMode>(gui);
        for (EditMode m : EditMode.values()) {
            mode.option(m.label(), m);
        }
        mode.name(Landmarks.NEW_MODE);
        modeSays = gui.text("").textSize(Ui.DETAIL_TEXT).textColor(Role.DIM).width(Length.FILL);
        cancel = new Button(gui, "Cancel").kind(Button.Kind.SECONDARY).name(Landmarks.NEW_CANCEL);
        create = new Button(gui, "Create").kind(Button.Kind.PRIMARY).name(Landmarks.NEW_CREATE);
        gui.landmark(Landmarks.NEW_UNIT, unit.node());
        gui.landmark(Landmarks.NEW_WIDTH, width.node());
        gui.landmark(Landmarks.NEW_HEIGHT, height.node());
        gui.landmark(Landmarks.NEW_DPI, dpi.node());
        gui.landmark(Landmarks.NEW_STARTS, starts.node());
        gui.landmark(Landmarks.NEW_MODE, mode.node());
        gui.landmark(Landmarks.NEW_CANCEL, cancel.node());
        gui.landmark(Landmarks.NEW_CREATE, create.node());

        dpiRow = row("DPI", dpi.node());
        Node buttons = gui.row().width(Length.FILL).justify(Justify.END).gap(Length.rem(0.5f)).scroll(false, false)
                .children(cancel.node(), create.node());
        Node spring = gui.box().width(Length.FILL).height(Length.grow(1f));
        Node body = gui.column().width(Length.FILL).height(Length.grow(1f)).padding(Length.rem(1f))
                .gap(Length.rem(0.75f)).alignItems(AlignItems.START).scroll(false, false)
                .children(row("Size in", unit.node()), row("Width", width.node()), row("Height", height.node()), dpiRow,
                        row("", texels), row("Starts", starts.node()), row("Mode", mode.node()), row("", modeSays),
                        spring, buttons);
        Node root = gui.root().direction(Direction.COLUMN).background(Role.PAGE);
        if (window.titleBar() != null) {
            root.children(window.titleBar().node(), body);
        } else {
            root.children(body);
        }
    }

    /** A label and its control, side by side. */
    private Node row(String label, Node control) {
        Node name = gui.text(label).wordWrap(false).width(LABEL_W).textSize(Ui.NAME_TEXT)
                .textColor(Role.DIM);
        return gui.row().width(Length.FILL).gap(Length.rem(0.75f)).alignItems(AlignItems.CENTER).scroll(false, false)
                .children(name, control);
    }

    /**
     * The choices shown as {@code sizing}, {@code inside} and {@code m}, without telling anyone: what the view keeps, put
     * back. The DPI only for a length; what the size makes in texels, or why it cannot be made, and Create only when
     * it can.
     */
    void show(Sizing sizing, boolean inside, EditMode m) {
        unit.show(sizing.unit());
        width.show(sizing.width());
        height.show(sizing.height());
        dpi.show(sizing.dpi());
        dpiRow.visible(sizing.unit() != Unit.PX);
        String why = sizing.why();
        texels.text(why.isEmpty() ? sizing.texelsWide() + " × " + sizing.texelsHigh() + " texels" : why)
                .textColor(why.isEmpty() ? Role.DIM : Role.DANGER);
        create.enabled(why.isEmpty());
        starts.show(inside);
        mode.show(m);
        modeSays.text(m.says());
    }

    String unitAddress() {
        return unit.address();
    }

    String widthAddress() {
        return width.address();
    }

    String heightAddress() {
        return height.address();
    }

    String dpiAddress() {
        return dpi.address();
    }

    String startsAddress() {
        return starts.address();
    }

    String modeAddress() {
        return mode.address();
    }

    String cancelAddress() {
        return cancel.address();
    }

    String createAddress() {
        return create.address();
    }

    /** This window's tree: for a test. */
    Gui gui() {
        return gui;
    }
}
