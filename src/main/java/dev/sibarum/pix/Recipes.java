package dev.sibarum.pix;

import dev.sibarum.suite.update.Updates;
import dev.sibarum.suite.view.SuiteLook;
import dev.sibarum.suite.view.UpdateNotice;
import dev.vexelray.codec.image.gui.Textures;
import dev.vexelray.framework.api.Configuration;
import dev.vexelray.framework.api.MainThread;
import dev.vexelray.framework.api.Provides;
import dev.vexelray.framework.core.Launch;
import dev.vexelray.framework.shell.Appearance;
import dev.vexelray.framework.shell.Apps;
import dev.vexelray.framework.shell.Dialogs;
import dev.vexelray.framework.shell.Shell;
import dev.vexelray.framework.shell.Windows;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.core.WindowInstrument;
import dev.vexelray.gui.core.app.AppHome;
import dev.vexelray.gui.core.app.GuiApp;
import dev.vexelray.gui.core.app.Settings;
import dev.vexelray.gui.core.input.Shortcut;
import dev.vexelray.gui.core.layout.Length;
import dev.vexelray.gui.keymap.Keymap;
import dev.vexelray.gui.keymap.KeymapWindow;
import dev.vexelray.gui.krono.KronoGui;
import dev.vexelray.gui.widget.TitleBar;
import sibarum.tactroller.api.Key;
import sibarum.tactroller.api.Modifier;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * What this application builds — one recipe per part — and nothing about when.
 *
 * <p>{@code PixAppWiring} is written by {@code vexelray-framework-processor} while this project compiles and calls
 * each method below once, in the phase its parameters put it in: a part's phase is the latest phase of anything it
 * takes. So the look and the model exist before the {@code Gui} does, the tree waits for the {@code Gui} and its title
 * bar, and the binding waits for the window.
 *
 * <p>The automation sockets are not here either: they come from {@code AutomationStarter}, which {@link PixApp} names
 * in its {@code @VexelApp}.
 */
@Configuration
final class Recipes {

    /** The look, and the smallest window this UI is still coherent in — a floor, not the design size. */
    @Provides
    Appearance look() {
        return Appearance.of(SuiteLook.THEME, Length.rem(Pix.MIN_W_EM), Length.rem(Pix.MIN_H_EM));
    }

    /** The one authoritative state, built before the tree because every control is a view onto it. */
    @Provides
    Model model() {
        return new Model();
    }

    /**
     * The reader. A part of its own, and taken by {@link #viewer}, because a listener has to exist before the state it
     * listens to is seeded: the wiring builds what a recipe <em>takes</em> first, so taking it is how the order is
     * said.
     */
    @Provides
    Previewer previewer(Gui gui, Model model) {
        return new Previewer(gui, model);
    }

    /** Opening a place, and the dialogs that pick one: the framework's, which need no window to be handed out. */
    @Provides
    Opener opener(Gui gui, Model model, Dialogs dialogs) {
        return new Opener(gui, model, dialogs);
    }

    /**
     * The tree, which is the viewer. Needs the {@code Gui}, its clock and the title bar, and no window. The first
     * place is opened here, last: the file asked for is shown before the folder around it has been read.
     */
    @Provides
    Viewer viewer(Gui gui, KronoGui krono, Model model, Previewer previewer, Textures textures, TitleBar titleBar,
                  Opener opener, Launch launch, Windows windows, Settings settings, Keymap keys) {
        Viewer viewer = new Viewer(gui, krono, model, previewer, textures, titleBar, opener, windows.main(), settings,
                keys);
        zoomKeys(gui, keys);
        keys.action("file.open", "Open a picture", Shortcut.of(Key.O, Modifier.CONTROL)).on(opener::file);
        keys.action("file.open-folder", "Open a folder", Shortcut.of(Key.O, Modifier.CONTROL, Modifier.SHIFT))
                .on(opener::folder);
        opener.open(Startup.place(launch.rest()));
        updates(gui, model, viewer);
        return viewer;
    }

    /**
     * Every command, by name, on the keys and touchpad gestures the user gives it: the viewer's and Pix's own. What
     * the user changes is kept in a store of its own ({@code keys}), beside the settings, so neither writes back
     * over the other.
     */
    @Provides
    Keymap keys(Gui gui) {
        return new Keymap(gui).persist(AppHome.of(Pix.APP).settings("keys"));
    }

    /**
     * Word of a newer version, over the top right of the picture, when this is the installed copy: Update and restart
     * closes Pix, lets the installer's updater replace it, and opens it again on the picture that was shown.
     */
    private static void updates(Gui gui, Model model, Viewer viewer) {
        Updates.of(Apps.registryDir(), Pix.APP).ifPresent(u -> {
            UpdateNotice notice = new UpdateNotice(gui, u, viewer::close, () -> {
                Doc d = model.doc();
                Path place = d.at() != null ? d.at() : d.folder();
                return place == null ? List.of() : List.of(place.toString());
            }, "");
            gui.root().append(notice.node());
            notice.start();
        });
    }

    /**
     * Switches the viewer on once its textures have the device, and off on close. Taking the binding is what puts
     * this after it: {@code ImageStarter} binds the {@code Textures} to the window, and a viewer shown before then
     * would have nothing to upload into. Full screen is the window's, so it is wired here too: asked for on an input
     * thread, done on the main thread, which owns the window.
     */
    @Provides
    @MainThread
    AutoCloseable windowBinding(Textures.Binding textures, Viewer viewer, Shell shell, Keymap keys) {
        keyBindingsButton(shell, keys);
        GuiApp app = shell.app();
        viewer.screen(on -> app.post(() -> app.window().fullscreen(on)));
        viewer.active(true);
        return () -> {
            viewer.screen(null);
            viewer.active(false);
        };
    }

    /**
     * The key-bindings button in the title bar, after whatever the bar already shows: it opens the editor where every
     * command can be given other keys or gestures. Here, with the shell, because the framework puts the look's tools in
     * the bar once the window exists, and this has to come after. From the bar and not the look, so the automation
     * button, which joins the strip the same way, is kept rather than replaced.
     */
    private static void keyBindingsButton(Shell shell, Keymap keys) {
        if (!shell.appearance().drawsOwnFrame()) {
            return;
        }
        List<WindowInstrument> tools = new ArrayList<>(shell.titleBar().instruments());
        tools.add(KeymapWindow.instrument(shell.app(), shell.gui(), keys));
        shell.titleBar().instruments(List.copyOf(tools));
    }

    /** Ctrl+= / Ctrl+- / Ctrl+0, and the numpad's three: the controls' size, not the picture's. */
    private static void zoomKeys(Gui gui, Keymap keys) {
        keys.action("ui.zoom-in", "Larger controls", Shortcut.of(Key.EQUAL, Modifier.CONTROL),
                Shortcut.of(Key.NUMPAD_ADD, Modifier.CONTROL)).on(gui::zoomIn);
        keys.action("ui.zoom-out", "Smaller controls", Shortcut.of(Key.MINUS, Modifier.CONTROL),
                Shortcut.of(Key.NUMPAD_SUBTRACT, Modifier.CONTROL)).on(gui::zoomOut);
        keys.action("ui.zoom-reset", "Controls at their own size", Shortcut.of(Key.DIGIT_0, Modifier.CONTROL),
                Shortcut.of(Key.NUMPAD_0, Modifier.CONTROL)).on(gui::resetZoom);
    }
}
