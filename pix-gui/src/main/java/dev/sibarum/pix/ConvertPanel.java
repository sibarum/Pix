package dev.sibarum.pix;

import dev.sibarum.pix.core.Messages.Format;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.core.Node;
import dev.vexelray.gui.core.NodeEvent;
import dev.vexelray.gui.core.input.CursorShape;
import dev.vexelray.gui.core.input.InteractionState;
import dev.vexelray.gui.core.layout.LayoutEnums.AlignItems;
import dev.vexelray.gui.core.layout.LayoutEnums.Justify;
import dev.vexelray.gui.core.layout.Length;
import dev.vexelray.gui.core.style.Role;
import dev.vexelray.gui.widget.Button;

/**
 * The format badge at the start of the bar, which is a button, and the Convert panel it opens over the stage's bottom
 * left, just above it (docs/convert.md): a tile per format the stack can write, the picture's own marked as what it is,
 * a line on what the one chosen does, and Save.
 *
 * <p>Like {@link Ui}, it holds no state but what it shows and where the pointer is on the badge; which formats are
 * offered and which is chosen is the view's.
 */
final class ConvertPanel {

    /** The panel's width, and the words in it. */
    static final Length WIDTH = Length.rem(18f);

    private final Gui gui;
    private final Node badge;
    private InteractionState badgeState = InteractionState.NORMAL;
    private boolean open;
    private final Node panel;
    private final Button[] tiles = new Button[Format.values().length];
    private final Node says;
    private final Node status;
    private final Button save;

    ConvertPanel(Gui gui) {
        this.gui = gui;
        badge = gui.text("").role("button").wordWrap(false).textSize(Ui.DETAIL_TEXT).bold()
                .padding(Length.rem(0.125f), Length.rem(0.5f)).corner(Length.rem(0.375f))
                .border(Length.dp(1f), gui.theme().color(Role.LINE)).visible(false);
        gui.cursor(badge, CursorShape.POINTER);
        gui.landmark(Landmarks.FORMAT, badge);
        gui.hear(badge, NodeEvent.STATE, s -> {
            badgeState = s;
            paintBadge();
        });
        paintBadge();

        Node title = gui.text("Convert").wordWrap(false).textSize(TracePanel.TITLE_TEXT).bold()
                .textColor(gui.theme().color(Role.INK));
        Node row = gui.row().gap(Length.rem(0.5f)).scroll(false, false);
        for (Format f : Format.values()) {
            tiles[f.ordinal()] = new Button(gui, f.label()).kind(Button.Kind.SECONDARY).toggle(true);
            gui.landmark(Landmarks.convertTo(f), tiles[f.ordinal()].node());
            row.append(tiles[f.ordinal()].node());
        }
        says = gui.text("").textSize(Ui.DETAIL_TEXT).textColor(gui.theme().color(Role.DIM)).width(Length.FILL);
        status = gui.text("").textSize(Ui.DETAIL_TEXT).textColor(gui.theme().color(Role.DANGER)).width(Length.FILL)
                .visible(false);
        gui.landmark(Landmarks.CONVERT_STATUS, status);
        save = new Button(gui, "Save").kind(Button.Kind.PRIMARY);
        gui.landmark(Landmarks.CONVERT_SAVE, save.node());
        Node buttons = gui.row().width(Length.FILL).justify(Justify.END).scroll(false, false).children(save.node());
        panel = gui.column().width(WIDTH).padding(Length.rem(1f)).gap(Length.rem(0.75f)).corner(Length.rem(0.5f))
                .background(Role.RAISED).border(Length.dp(1f), gui.theme().color(Role.LINE))
                .alignItems(AlignItems.START).scroll(false, false)
                .floatAt(Ui.MARGIN, Length.percent(100f)).visible(false)
                .children(title, row, says, status, buttons);
        gui.landmark(Landmarks.CONVERT_PANEL, panel);
    }

    /** The badge, which says the picture's format and opens the panel. */
    Node badge() {
        return badge;
    }

    /** The panel, floated at the stage's bottom left. */
    Node panel() {
        return panel;
    }

    /** What the badge says; empty hides it, for a file that is not a picture or while none is shown. */
    void format(String label) {
        badge.text(label).visible(!label.isEmpty());
    }

    /** The panel shown, and the badge marked as what opened it; or neither. */
    void open(boolean open) {
        this.open = open;
        panel.visible(open);
        paintBadge();
    }

    /**
     * Format {@code f} offered or not, chosen or not, or the picture's own ({@code current}), which is marked and is
     * nothing to convert to.
     */
    void tile(Format f, boolean offered, boolean chosen, boolean current) {
        Button b = tiles[f.ordinal()];
        b.node().visible(offered || current);
        b.label(current ? f.label() + " · now" : f.label()).enabled(offered && !current).show(chosen);
    }

    /** What the chosen format does to this picture; what Save says; why the last save failed, or nothing. */
    void says(String text, String saveLabel, boolean saving) {
        says.text(text);
        save.label(saving ? "Saving…" : saveLabel).enabled(!saving && !saveLabel.isEmpty());
    }

    /** Why the last save failed, shown only while there is something to say. */
    void status(String text) {
        status.text(text).visible(!text.isEmpty());
    }

    String tileAddress(Format f) {
        return tiles[f.ordinal()].address();
    }

    String saveAddress() {
        return save.address();
    }

    /** The badge's plate: in the accent while the panel is open, lifted under the pointer, pressed at once. */
    private void paintBadge() {
        Role plate = open ? Role.HIGHLIGHT : Role.WELL;
        badge.background(badgeState == InteractionState.NORMAL ? plate : plate.at(badgeState))
                .textColor(open ? Role.ACCENT : Role.INK);
    }
}
