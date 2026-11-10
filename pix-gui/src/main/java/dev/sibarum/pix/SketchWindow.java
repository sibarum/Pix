package dev.sibarum.pix;

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

/**
 * The SDG editor's window (docs/sdg-editor.md): a bar saying what is being edited, with Save and what the last save
 * said, over the stage the field is shown on, fitted at its own aspect. A satellite of the main window, declared by
 * name ({@code Recipes.sketchWindow}) and opened by Create in the New SDG window.
 *
 * <p>Holds no state but what it shows, as {@link Ui} does not; {@link SketchView} decides what that is.
 */
final class SketchWindow {

    /** The window's name, in a {@code WindowCommand}, a view's {@code window} and to a script ({@code sdg/...}). */
    static final String KEY = "sdg";
    static final String TITLE = "SDG";
    static final int W = 960;
    static final int H = 720;

    private final Gui gui;
    private final Node title;
    private final Node status;
    private final Button save;
    private final Node stage;
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
        field = gui.box().visible(false);
        gui.landmark(Landmarks.SKETCH_FIELD, field);
        stage = gui.box().width(Length.FILL).height(Length.grow(1f)).clip(true).scroll(false, false)
                .alignItems(AlignItems.CENTER).justify(Justify.CENTER).padding(Ui.MARGIN)
                .background(gui.theme().color(Role.WELL)).children(field);
        Node root = gui.root().direction(Direction.COLUMN).background(gui.theme().color(Role.PAGE));
        if (window.titleBar() != null) {
            root.children(window.titleBar().node(), bar, stage);
        } else {
            root.children(bar, stage);
        }
    }

    /** The field's preview, fitted at {@code widthPercent × heightPercent} of the stage; null shows none. */
    void field(Feed feed, Fit fit) {
        if (feed == null) {
            field.visible(false);
            return;
        }
        field.image(feed).width(Length.percent(fit.widthPercent())).height(Length.percent(fit.heightPercent()))
                .visible(true);
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

    String saveAddress() {
        return save.address();
    }

    /** This window's tree: for a test. */
    Gui gui() {
        return gui;
    }
}
