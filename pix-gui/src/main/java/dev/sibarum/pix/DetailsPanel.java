package dev.sibarum.pix;

import dev.vexelray.canvas.Color;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.core.Node;
import dev.vexelray.gui.core.layout.LayoutEnums.AlignItems;
import dev.vexelray.gui.core.layout.Length;
import dev.vexelray.gui.core.style.Role;
import dev.vexelray.gui.core.text.Ellipsis;
import dev.vexelray.gui.icons.Icon;
import dev.vexelray.gui.widget.IconButton;
import dev.vexelray.text.TextLayout;

import java.util.List;

/**
 * The Details panel (docs/details.md): at the stage's right, the picture shown's name, then a row each for what it is,
 * and its palette. What each row says is the view's to work out ({@link Details}); this only shows it.
 */
final class DetailsPanel {

    /** The rows, top to bottom. */
    static final List<String> ROWS = List.of("Format", "Dimensions", "Aspect", "File size", "Modified", "Folder");

    private final Node panel;
    private final Node name;
    private final Node[] values = new Node[ROWS.size()];
    private final Node[] swatches = new Node[5];
    private final Node palette;
    private final IconButton close;

    DetailsPanel(Gui gui) {
        Role dim = Role.DIM;
        Role ink = Role.INK;
        Node title = gui.text("Details").wordWrap(false).textSize(TracePanel.TITLE_TEXT).bold().textColor(ink);
        name = gui.text("").textSize(Ui.DETAIL_TEXT).textColor(dim).width(Length.FILL);
        gui.landmark(Landmarks.DETAILS_NAME, name);
        close = new IconButton(gui, Icon.CLOSE, "Close").size(Ui.TOOL, Ui.TOOL_ICON).tip("Close (Esc)");
        Node heading = gui.row().width(Length.FILL).alignItems(AlignItems.START).scroll(false, false)
                .children(gui.column().width(Length.grow(1f)).gap(Ui.LINE_GAP).scroll(false, false)
                        .children(title, name), close.node());

        Node rows = gui.column().width(Length.FILL).scroll(false, false);
        for (int i = 0; i < values.length; i++) {
            Node label = gui.text(ROWS.get(i)).wordWrap(false).textSize(Ui.DETAIL_TEXT).textColor(dim);
            // Right of its label, as wide as the row leaves it, and a long one (a folder) cut in the middle, so both
            // its start and the folder it is in show, as the bar cuts a name.
            values[i] = gui.text("").wordWrap(false).ellipsis(Ellipsis.MIDDLE).width(Length.grow(1f))
                    .align(TextLayout.HAlign.RIGHT, TextLayout.VAlign.TOP).textSize(Ui.DETAIL_TEXT).textColor(ink);
            gui.landmark(Landmarks.detail(ROWS.get(i)), values[i]);
            rows.append(gui.row().width(Length.FILL).alignItems(AlignItems.START)
                    .gap(Length.rem(1f)).padding(Length.rem(0.625f), Length.ZERO).scroll(false, false)
                    .border(Length.ZERO, Role.NONE).children(label, values[i]));
            if (i < values.length - 1) {
                rows.append(gui.box().width(Length.FILL).height(Length.dp(1f)).background(Role.LINE));
            }
        }

        Node swatchRow = gui.row().width(Length.FILL).gap(Length.rem(0.375f)).scroll(false, false);
        for (int i = 0; i < swatches.length; i++) {
            swatches[i] = gui.box().width(Length.grow(1f)).height(Length.rem(2.5f)).corner(Length.rem(0.375f))
                    .border(Length.dp(1f), Role.LINE).visible(false);
            swatchRow.append(swatches[i]);
        }
        palette = gui.column().width(Length.FILL).gap(Length.rem(0.5f)).scroll(false, false)
                .children(gui.text("PALETTE").wordWrap(false).bold().textSize(TracePanel.LABEL_TEXT).textColor(dim),
                        swatchRow);
        gui.landmark(Landmarks.DETAILS_PALETTE, swatchRow);

        panel = gui.column().width(TracePanel.WIDTH).height(Length.percent(100f)).padding(Length.rem(1f))
                .gap(Length.rem(1.25f)).background(Role.CHROME).scroll(false, true).visible(false)
                .children(heading, rows, palette);
        gui.landmark(Landmarks.DETAILS_PANEL, panel);
    }

    Node panel() {
        return panel;
    }

    void open(boolean open) {
        panel.visible(open);
    }

    /** The picture's name, and each row's value in {@link #ROWS}' order; an empty value says "—". */
    void show(String name, List<String> values) {
        this.name.text(name);
        for (int i = 0; i < this.values.length; i++) {
            String v = i < values.size() ? values.get(i) : "";
            this.values[i].text(v.isEmpty() ? "—" : v);
        }
    }

    /** The palette's swatches, {@code 0xRRGGBBAA}; the section hidden with none. */
    void palette(List<Integer> colours) {
        palette.visible(!colours.isEmpty());
        for (int i = 0; i < swatches.length; i++) {
            boolean on = i < colours.size();
            swatches[i].visible(on);
            if (on) {
                int c = colours.get(i);
                swatches[i].background(Color.rgba(((c >>> 24) & 0xff) / 255f, ((c >>> 16) & 0xff) / 255f,
                        ((c >>> 8) & 0xff) / 255f, 1f));
            }
        }
    }

    String closeAddress() {
        return close.address();
    }
}
