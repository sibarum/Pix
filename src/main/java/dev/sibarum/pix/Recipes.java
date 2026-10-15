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
import dev.vexelray.framework.shell.Windows;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.core.app.Settings;
import dev.vexelray.gui.core.layout.Length;
import dev.vexelray.gui.krono.KronoGui;
import dev.vexelray.gui.widget.TitleBar;
import sibarum.tactroller.api.Key;
import sibarum.tactroller.api.Modifier;

import java.nio.file.Path;
import java.util.List;

/**
 * What this application builds — one recipe per part — and nothing about when.
 *
 * <p>{@code PixAppWiring} is written by {@code vexelray-framework-processor} while this project compiles and calls
 * each method below once, in the phase its parameters put it in: a part's phase is the latest phase of anything it
 * takes. So the look and the model exist before the {@code Gui} does, the tree waits for the {@code Gui} and its title
 * bar, and the binding waits for the window.
 *
 * <p>The driving socket is not here either: it comes from {@code AutomationStarter}, which {@link PixApp} names in its
 * {@code @VexelApp} (debug edition only).
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
                  Opener opener, Launch launch, Windows windows, Settings settings) {
        Viewer viewer = new Viewer(gui, krono, model, previewer, textures, titleBar, opener, windows.main(), settings);
        zoomShortcuts(gui);
        gui.shortcut(Key.O, opener::file, Modifier.CONTROL);
        gui.shortcut(Key.O, opener::folder, Modifier.CONTROL, Modifier.SHIFT);
        opener.open(Startup.place(launch.rest()));
        updates(gui, model, viewer);
        return viewer;
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
     * would have nothing to upload into.
     */
    @Provides
    @MainThread
    AutoCloseable windowBinding(Textures.Binding textures, Viewer viewer) {
        viewer.active(true);
        return () -> viewer.active(false);
    }

    /** Ctrl+= / Ctrl+- / Ctrl+0, and the numpad's three: the controls' size, not the picture's. */
    private static void zoomShortcuts(Gui gui) {
        gui.shortcut(Key.EQUAL, gui::zoomIn, Modifier.CONTROL);
        gui.shortcut(Key.MINUS, gui::zoomOut, Modifier.CONTROL);
        gui.shortcut(Key.DIGIT_0, gui::resetZoom, Modifier.CONTROL);
        gui.shortcut(Key.NUMPAD_ADD, gui::zoomIn, Modifier.CONTROL);
        gui.shortcut(Key.NUMPAD_SUBTRACT, gui::zoomOut, Modifier.CONTROL);
        gui.shortcut(Key.NUMPAD_0, gui::resetZoom, Modifier.CONTROL);
    }
}
