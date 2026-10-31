package dev.sibarum.pix;

import dev.vexelray.gui.core.Feed;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.core.Node;
import dev.vexelray.gui.core.layout.LayoutEnums.AlignItems;
import dev.vexelray.gui.core.layout.LayoutEnums.Direction;
import dev.vexelray.gui.core.layout.LayoutEnums.Justify;
import dev.vexelray.gui.core.layout.Length;
import dev.vexelray.gui.core.style.Role;
import dev.vexelray.gui.widget.StatusBar;
import dev.vexelray.gui.widget.TitleBar;

/**
 * The window: the framework's title bar, the stage the picture is fitted into, and a status line under both.
 *
 * <p>It holds no state. The picture shown is the view's to know, and what each label says is what the viewer last
 * told the view. Built in the {@code TREE} phase, before there is a window, and needing none, so a test can build the
 * real tree headless.
 */
final class Ui {

    private final TitleBar titleBar;
    private final StatusBar status;
    private final Node stage;
    private final Node picture;
    private final Node note;

    Ui(Gui gui, TitleBar titleBar) {
        this.titleBar = titleBar;

        picture = gui.box();
        gui.landmark(Landmarks.PICTURE, picture);
        note = gui.text("").textColor(gui.theme().color(Role.DIM)).visible(false);
        gui.landmark(Landmarks.NOTE, note);

        stage = gui.box()
                .width(Length.FILL).height(Length.grow(1f))
                .direction(Direction.COLUMN).justify(Justify.CENTER).alignItems(AlignItems.CENTER)
                .clip(true)
                .children(picture, note);
        gui.landmark(Landmarks.STAGE, stage);

        status = new StatusBar(gui)
                .slot(Landmarks.STATUS_NAME, StatusBar.Side.LEFT, "")
                .slot(Landmarks.STATUS_PLACE, StatusBar.Side.RIGHT, "")
                .minWidth(Landmarks.STATUS_PLACE, Length.rem(7));
        gui.landmark(Landmarks.STATUS, status.node());
        gui.landmark(Landmarks.STATUS_NAME, status.slot(Landmarks.STATUS_NAME));
        gui.landmark(Landmarks.STATUS_PLACE, status.slot(Landmarks.STATUS_PLACE));

        gui.root().direction(Direction.COLUMN)
                .background(gui.theme().color(Role.PAGE))
                .children(titleBar.node(), stage, status.node());
    }

    Node stage() {
        return stage;
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

    /** The picture's name in the title bar and the status line, and its place in its folder. */
    void caption(String name, String place) {
        titleBar.title(name.isEmpty() ? Pix.TITLE : name + " — " + Pix.TITLE);
        status.text(Landmarks.STATUS_NAME, name);
        status.text(Landmarks.STATUS_PLACE, place);
    }
}
