package dev.sibarum.pix;

import dev.sibarum.pix.core.Messages.CreateSdg;
import dev.sibarum.pix.core.Messages.EditMode;
import dev.sibarum.pix.core.Messages.Sizing;
import dev.sibarum.pix.core.Messages.Unit;
import dev.vexelray.framework.api.Subscribe;
import dev.vexelray.framework.core.WindowCommand;
import dev.vexelray.gui.core.input.Menu;
import dev.vexelray.gui.core.input.Shortcut;
import dev.vexelray.gui.keymap.Keymap;
import dev.vexelray.gui.widget.Button;
import dev.vexelray.gui.widget.NumberField;
import dev.vexelray.gui.widget.Segment;
import sibarum.atchung.Sender;
import sibarum.tactroller.api.Key;
import sibarum.tactroller.api.Modifier;

/**
 * The New menu and the New SDG window's view (docs/sdg-editor.md): the menu and Ctrl+N declared as the window they
 * open, the choices the person makes kept and shown back, and Create told to the {@code Sketcher} as {@link CreateSdg},
 * with the editor's window opened in this one's place.
 *
 * <p>It decides nothing about the field: what a size makes, and whether it can be made, is {@link Sizing}'s. A view of
 * its own, keeping to its own widgets by their address, as MainFrame's Open window's is.
 */
@dev.vexelray.framework.api.View
final class NewSdgView {

    /** The keymap's id for New SDG…, which the menu's line names so it says the chord the person has for it. */
    static final String NEW_SDG = "file.new-sdg";

    private final NewSdgWindow page;
    private final Sender<WindowCommand> windows;
    private final Sender<CreateSdg> create;

    private Sizing sizing = Sizing.FIRST;
    private boolean inside = true;
    private EditMode mode = EditMode.STENCIL;

    NewSdgView(NewSdgWindow page, Ui ui, Keymap keys, Sender<WindowCommand> windows, Sender<CreateSdg> create) {
        this.page = page;
        this.windows = windows;
        this.create = create;
        WindowCommand open = WindowCommand.show(NewSdgWindow.KEY);
        keys.action(NEW_SDG, "New SDG…", Shortcut.of(Key.N, Modifier.CONTROL)).sends(windows, open);
        ui.newMenu(Menu.builder().item("SDG…", windows, open).command(NEW_SDG).build());
        page.show(sizing, inside, mode);
    }

    @Subscribe
    public void pressed(Button.Pressed p) {
        if (p.button().equals(page.cancelAddress())) {
            windows.send(WindowCommand.hide(NewSdgWindow.KEY));
        } else if (p.button().equals(page.createAddress()) && sizing.why().isEmpty()) {
            create.send(new CreateSdg(sizing, inside, mode));
            windows.send(WindowCommand.hide(NewSdgWindow.KEY));
            windows.send(WindowCommand.show(SketchWindow.KEY));
        }
    }

    /** The width, the height or the DPI typed or stepped. */
    @Subscribe
    public void typed(NumberField.Changed c) {
        if (c.field().equals(page.widthAddress())) {
            sizing = new Sizing(c.value(), sizing.height(), sizing.unit(), sizing.dpi());
        } else if (c.field().equals(page.heightAddress())) {
            sizing = new Sizing(sizing.width(), c.value(), sizing.unit(), sizing.dpi());
        } else if (c.field().equals(page.dpiAddress())) {
            sizing = new Sizing(sizing.width(), sizing.height(), sizing.unit(), c.value());
        } else {
            return;
        }
        page.show(sizing, inside, mode);
    }

    /** The unit, where the field starts, or the mode chosen. A new unit says the same size in it. */
    @Subscribe
    public void chose(Segment.Changed c) {
        if (c.segment().equals(page.unitAddress()) && c.value() instanceof Unit u) {
            sizing = sizing.in(u);
        } else if (c.segment().equals(page.startsAddress()) && c.value() instanceof Boolean b) {
            inside = b;
        } else if (c.segment().equals(page.modeAddress()) && c.value() instanceof EditMode m) {
            mode = m;
        } else {
            return;
        }
        page.show(sizing, inside, mode);
    }
}
