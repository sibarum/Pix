package dev.sibarum.pix;

import dev.vexelray.framework.api.Configuration;
import dev.vexelray.framework.api.Provides;
import dev.vexelray.framework.shell.Appearance;
import dev.vexelray.framework.shell.NamedWindow;
import dev.vexelray.framework.shell.Windows;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.core.layout.Length;
import dev.vexelray.gui.widget.TitleBar;

/**
 * What this module builds — one recipe per part — and nothing about when.
 *
 * <h2>The wiring is generated from this</h2>
 *
 * <p>{@code PixAppWiring} is written by {@code vexelray-framework-processor} while this module compiles, and
 * lands in {@code target/generated-sources/annotations}. It calls each method below once, in the phase its
 * parameters put it in: <b>a part's phase is the latest phase of anything it takes.</b> So the look exists before
 * the {@code Gui} does, and the window's contents wait for the {@code Gui}, its clock and its title bar. There is no
 * phase to declare and none to get wrong. Open the generated file after a build to see the order. Nothing here waits
 * for the window itself: what happens once it exists (the viewer asking where to start) is heard as the framework's
 * {@code WindowOpened}, and no part takes the {@code Shell}, which the processor refuses.
 *
 * <h2>Where the rest comes from</h2>
 *
 * <p>Not every part is a recipe. The {@link View} is a {@code @View}: built from its one constructor, run on the GUI
 * thread, reached by messages. The viewer and the jobs that touch the disk are in {@code -core}, and reach the
 * wiring through {@code Core}, which {@code PixApp} names among its starters beside {@code AutomationStarter},
 * the automation sockets.
 *
 * <p>Opening an input backend, installing a clipboard, remembering where the window was, attaching a clock, wiring
 * the frame loop with its wakes and its pacing, parsing the command line, installing the dialogs, and closing
 * everything in the right order are all {@code vexelray-framework}'s. So is closing the parts that are
 * {@code AutoCloseable}.
 *
 * <p>Adding a part is a method here. It returns an interface when other code should be able to swap it; this
 * package's own classes may be returned as they are, because nothing outside the package could hold a call site
 * against them anyway.
 */
@Configuration
final class Recipes {

    /**
     * The look, and the smallest window this UI is still coherent in. Built from nothing the framework makes, so it
     * exists in the first phase and the framework applies it the moment it creates the {@code Gui}: "the theme is
     * applied before the first widget" is structural rather than a comment somebody has to keep obeying.
     */
    @Provides
    Appearance look() {
        return Appearance.of(Look.DARK, Length.rem(Pix.MIN_W_EM), Length.rem(Pix.MIN_H_EM))
                .themes(Look.LIGHT, Look.DARK);
    }

    /**
     * The window's contents. Needs the {@code Gui} and its title bar, and no window — so a test can build the real tree
     * headless ({@code VexelApplication.tree}), which {@code ViewTest} does.
     */
    @Provides
    Ui ui(Gui gui, TitleBar titleBar) {
        return new Ui(gui, titleBar);
    }

    /**
     * The New SDG window (docs/sdg-editor.md): declared by name, a satellite of the main window, its tree built now
     * beside the main one and claimed by the framework once there is a device. {@link NewSdgView} shows it, opened by
     * the New menu or Ctrl+N as a {@code WindowCommand}.
     */
    @Provides
    NewSdgWindow newSdgWindow(Windows windows) {
        return new NewSdgWindow(windows.named(NamedWindow.of(NewSdgWindow.KEY, NewSdgWindow.TITLE, NewSdgWindow.W,
                NewSdgWindow.H).satellite()));
    }

    /** The SDG editor's window, declared as the New SDG window is; {@link SketchView} shows the field in it. */
    @Provides
    SketchWindow sketchWindow(Windows windows) {
        return new SketchWindow(windows.named(NamedWindow.of(SketchWindow.KEY, SketchWindow.TITLE, SketchWindow.W,
                SketchWindow.H).satellite()));
    }

}
