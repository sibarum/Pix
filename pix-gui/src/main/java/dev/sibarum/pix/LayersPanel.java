package dev.sibarum.pix;

import dev.vexelray.canvas.Color;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.core.ImageSdg;
import dev.vexelray.gui.core.Node;
import dev.vexelray.gui.core.input.CursorShape;
import dev.vexelray.gui.core.layout.LayoutEnums.AlignItems;
import dev.vexelray.gui.core.layout.LayoutEnums.Direction;
import dev.vexelray.gui.core.layout.LayoutEnums.Justify;
import dev.vexelray.gui.core.layout.Length;
import dev.vexelray.gui.core.style.Role;
import dev.vexelray.gui.icons.Icon;
import dev.vexelray.gui.widget.Button;
import dev.vexelray.gui.widget.IconButton;
import dev.vexelray.gui.widget.Segment;
import dev.vexelray.gui.widget.Slider;

import java.util.ArrayList;
import java.util.List;

/**
 * The Layers panel and what it draws on the stage (docs/layers.md): at the stage's right, a drawing's layers front to
 * back, the one picked, its theme role, the peel, the light page and Save roles; on the stage, a node for each layer,
 * laid out as the picture's layer is, which the view draws at the picture's zoom, fanned apart by the peel and faint
 * but for the one picked.
 *
 * <p>Like {@link Ui}, it holds no state but what it shows. Every row and every layer node is made here once and shown or
 * hidden, since a node taken out cannot be put back (FN-19).
 */
final class LayersPanel {

    /** The most layers drawn one a node, and the most rows listed. A drawing with more is drawn whole. */
    static final int MOST_DRAWN = 32;
    static final int MOST_LISTED = 64;

    /** The roles the panel offers a solid layer: SDG's names for the framework's ink, accent and dim (SdgRoles). */
    static final List<String> ROLES = List.of("", "foreground", "accent", "muted");

    private final Gui gui;
    private final Node panel;
    private final Node size;
    private final IconButton close;
    private final Node[] rows = new Node[MOST_LISTED];
    private final Node[] rowSwatches = new Node[MOST_LISTED];
    private final Node[] rowNames = new Node[MOST_LISTED];
    private final Node[] rowTags = new Node[MOST_LISTED];
    private final Node more;
    private final Node cardSwatch;
    private final Node cardName;
    private final Node cardDetail;
    private final Node roleSection;
    private final Segment<String> role;
    private final IconButton peel;
    private final Slider apart;
    private final IconButton light;
    private final Node status;
    private final Button save;
    private final Node page;
    private final Node[] drawn = new Node[MOST_DRAWN];

    LayersPanel(Gui gui) {
        this.gui = gui;
        Role dim = Role.DIM;
        Role ink = Role.INK;

        // ---- on the stage: a light page under the layers, and a node a layer, back first
        page = gui.box().width(Length.percent(100f)).height(Length.percent(100f)).hitInert(true)
                .floatAt(Length.ZERO, Length.ZERO).visible(false);
        for (int i = 0; i < drawn.length; i++) {
            drawn[i] = gui.box().hitInert(true).paintCuts(true).visible(false);
        }
        gui.landmark(Landmarks.LAYERS_STAGE, drawn[0]);

        // ---- the panel
        Node title = gui.text("Layers").wordWrap(false).textSize(TracePanel.TITLE_TEXT).bold().textColor(ink);
        size = gui.text("").wordWrap(false).textSize(Ui.DETAIL_TEXT).textColor(dim);
        close = new IconButton(gui, Icon.CLOSE, "Close").size(Ui.TOOL, Ui.TOOL_ICON).tip("Close (Esc)");
        Node heading = gui.row().width(Length.FILL).alignItems(AlignItems.START).scroll(false, false)
                .children(gui.column().width(Length.grow(1f)).gap(Ui.LINE_GAP).scroll(false, false)
                        .children(title, size), close.node());

        Node list = gui.column().width(Length.FILL).gap(Length.rem(0.25f)).scroll(false, false);
        for (int i = 0; i < rows.length; i++) {
            rowSwatches[i] = gui.box().size(Length.rem(1.75f), Length.rem(1.75f)).corner(Length.rem(0.375f))
                    .border(Length.dp(1f), Role.LINE).hitInert(true);
            rowNames[i] = gui.text("").wordWrap(false).textSize(Ui.DETAIL_TEXT).textColor(ink).hitInert(true);
            rowTags[i] = gui.text("").wordWrap(false).textSize(TracePanel.LABEL_TEXT).textColor(dim).hitInert(true);
            rows[i] = gui.row().role("button").width(Length.FILL).alignItems(AlignItems.CENTER).gap(Length.rem(0.625f))
                    .padding(Length.rem(0.375f), Length.rem(0.5f)).corner(Length.rem(0.5f)).scroll(false, false)
                    .visible(false)
                    .children(rowSwatches[i], gui.column().width(Length.grow(1f)).gap(Ui.LINE_GAP)
                            .scroll(false, false).hitInert(true).children(rowNames[i], rowTags[i]));
            gui.cursor(rows[i], CursorShape.POINTER);
            gui.landmark(Landmarks.layer(i), rows[i]);
            list.append(rows[i]);
        }
        more = gui.text("").textSize(Ui.DETAIL_TEXT).textColor(dim).visible(false);
        list.append(more);

        cardSwatch = gui.box().size(Length.rem(2.5f), Length.rem(2.5f)).corner(Length.rem(0.5f))
                .border(Length.dp(1f), Role.LINE);
        cardName = gui.text("").wordWrap(false).bold().textSize(Ui.NAME_TEXT).textColor(ink);
        cardDetail = gui.text("").textSize(Ui.DETAIL_TEXT).textColor(dim).width(Length.FILL);
        gui.landmark(Landmarks.LAYER_PICKED, cardName);
        Node cardHead = gui.row().width(Length.FILL).alignItems(AlignItems.CENTER).gap(Length.rem(0.75f))
                .scroll(false, false).children(cardSwatch, gui.column().width(Length.grow(1f)).gap(Ui.LINE_GAP)
                        .scroll(false, false).children(cardName, cardDetail));
        role = new Segment<String>(gui).option("none", "").option("foreground", "foreground")
                .option("accent", "accent").option("muted", "muted");
        gui.landmark(Landmarks.LAYER_ROLE, role.node());
        roleSection = gui.column().width(Length.FILL).gap(Length.rem(0.5f)).scroll(false, false)
                .children(label(gui, "Follows theme colour"), gui.row().scroll(false, false).children(role.node()));
        Node card = gui.column().width(Length.FILL).gap(Length.rem(0.875f)).padding(Length.rem(0.875f))
                .corner(Length.rem(0.75f)).background(Role.WELL).scroll(false, false).children(cardHead, roleSection);

        peel = new IconButton(gui, PixIcon.LAYERS, "Peel layers apart").toggle(true).size(Ui.TOOL, Ui.TOOL_ICON)
                .tip("Peel layers apart");
        gui.landmark(Landmarks.LAYERS_PEEL, peel.node());
        apart = new Slider(gui, 1f);
        apart.node().width(Length.grow(1f));
        gui.landmark(Landmarks.LAYERS_APART, apart.node());
        light = new IconButton(gui, Icon.THEME_LIGHT, "Preview on a light page").toggle(true)
                .size(Ui.TOOL, Ui.TOOL_ICON).tip("Preview on a light page");
        gui.landmark(Landmarks.LAYERS_LIGHT, light.node());
        Node tools = gui.row().width(Length.FILL).alignItems(AlignItems.CENTER).gap(Length.rem(0.5f))
                .scroll(false, false).children(peel.node(), apart.node(), light.node());

        status = gui.text("").textSize(Ui.DETAIL_TEXT).textColor(dim).width(Length.FILL).visible(false);
        gui.landmark(Landmarks.LAYERS_STATUS, status);
        save = new Button(gui, "Save roles").kind(Button.Kind.PRIMARY).enabled(false);
        gui.landmark(Landmarks.LAYERS_SAVE, save.node());
        Node buttons = gui.row().width(Length.FILL).justify(Justify.END).scroll(false, false).children(save.node());

        panel = gui.column().width(TracePanel.WIDTH).height(Length.percent(100f)).padding(Length.rem(1f))
                .gap(Length.rem(1f)).background(Role.CHROME).direction(Direction.COLUMN).scroll(false, true)
                .visible(false).children(heading, list, card, tools, status, buttons);
        gui.landmark(Landmarks.LAYERS_PANEL, panel);
    }

    private static Node label(Gui gui, String text) {
        return gui.text(text.toUpperCase(java.util.Locale.ROOT)).wordWrap(false).textSize(TracePanel.LABEL_TEXT)
                .bold().textColor(Role.DIM);
    }

    /** The panel, at the stage's right. */
    Node panel() {
        return panel;
    }

    /** What goes on the stage over the picture: the light page, then a node a layer, back first. */
    List<Node> overStage() {
        List<Node> out = new ArrayList<>();
        out.add(page);
        out.addAll(List.of(drawn));
        return out;
    }

    /** The layer nodes, back first: what the view draws at the picture's zoom, fanned and faded. */
    List<Node> drawnNodes() {
        return List.of(drawn);
    }

    Node row(int i) {
        return rows[i];
    }

    /** The panel shown, or hidden with every layer node and the light page. */
    void open(boolean open) {
        panel.visible(open);
        if (!open) {
            for (Node n : drawn) {
                n.sdg(null).visible(false);
            }
            page.visible(false);
        }
    }

    /**
     * Each layer node framed as a sheet while the peel fans them, the picked one in the accent, so the stack reads as
     * sheets rather than shapes over shapes; unframed while they lie together, as the drawing.
     */
    void frames(boolean on, int picked) {
        for (int i = 0; i < drawn.length; i++) {
            if (!on) {
                drawn[i].border(Length.ZERO, Role.NONE);
            } else if (i == picked) {
                drawn[i].border(Length.dp(2f), Role.ACCENT);
            } else {
                drawn[i].border(Length.dp(1f), Role.EDGE);
            }
        }
    }

    /** What the panel says of the drawing as a whole: its size in its own units, and how many layers. */
    void size(String text) {
        size.text(text);
    }

    /**
     * Layer node {@code i} (back first) draws {@code drawing}, laid out as the picture is, {@code widthPercent} by
     * {@code heightPercent} of the stage and centred; with none, nothing.
     */
    void drawn(int i, ImageSdg drawing, float widthPercent, float heightPercent) {
        Node n = drawn[i];
        n.sdg(drawing).visible(drawing != null);
        if (drawing != null) {
            n.width(Length.percent(widthPercent)).height(Length.percent(heightPercent))
                    .floatAt(Length.percent((100f - widthPercent) / 2f), Length.percent((100f - heightPercent) / 2f));
        }
    }

    /** Row {@code i} (front first): its swatch ({@code 0xRRGGBBAA}, or none), name, kind and paint, and picked. */
    void row(int i, Integer swatch, String name, String tags, boolean picked) {
        rows[i].visible(true).background(picked ? Role.HIGHLIGHT : Role.NONE);
        paintSwatch(rowSwatches[i], swatch);
        rowNames[i].text(name);
        rowTags[i].text(tags);
    }

    /** Rows from {@code count} on hidden, and how many more there are than are listed. */
    void rows(int count, int beyond) {
        for (int i = count; i < rows.length; i++) {
            rows[i].visible(false);
        }
        more.text("and " + beyond + " more, not listed").visible(beyond > 0);
    }

    /** The picked layer's card; {@code roleable} for a solid one, which may follow a theme colour. */
    void picked(Integer swatch, String name, String detail, boolean roleable, String roleNow) {
        paintSwatch(cardSwatch, swatch);
        cardName.text(name);
        cardDetail.text(detail);
        roleSection.visible(roleable);
        role.show(ROLES.contains(roleNow) ? roleNow : "");
    }

    /**
     * The peel on or off and how far apart; the light page on or off, in the light look's page colour. A colour, not a
     * role, on purpose: it previews that look whichever look the window is in.
     */
    void tools(boolean peeled, float apartValue, boolean lightPage) {
        peel.on(peeled);
        apart.show(apartValue);
        light.on(lightPage);
        page.visible(lightPage).background(Look.LIGHT.color(Role.PAGE));
    }

    /** Save roles, pressable while there are roles to save and none is being saved. */
    void save(boolean changed, boolean saving) {
        save.label(saving ? "Saving…" : "Save roles").enabled(changed && !saving);
    }

    void status(String text) {
        status.text(text).visible(!text.isEmpty());
    }

    private static void paintSwatch(Node n, Integer rgba) {
        if (rgba == null) {
            n.background(Role.WELL);
            return;
        }
        int c = rgba;
        n.background(Color.rgba(((c >>> 24) & 0xff) / 255f, ((c >>> 16) & 0xff) / 255f, ((c >>> 8) & 0xff) / 255f,
                (c & 0xff) / 255f));
    }

    String closeAddress() {
        return close.address();
    }

    String roleAddress() {
        return role.address();
    }

    String peelAddress() {
        return peel.address();
    }

    String apartAddress() {
        return apart.address();
    }

    String lightAddress() {
        return light.address();
    }

    String saveAddress() {
        return save.address();
    }
}
