package dev.sibarum.pix;

import dev.sibarum.pix.files.Folder;
import dev.sibarum.suite.pictures.Pictures;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.core.app.GuiApp;
import dev.vexelray.gui.nfd.FileDialog;
import sibarum.probe.Log;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Opens a place: the one on the command line, or one picked in the OS's open dialog (Ctrl+O or a click
 * on the name) or its folder dialog (Ctrl+Shift+O).
 *
 * <p>Opening is the document's {@link Model#open} and the folder's listing read off the frame loop, so the file picked
 * is shown before the folder around it has been read, the same as at startup.
 *
 * <p>The dialogs need the window's handle, and the window does not exist until after the tree has been built, so this
 * is given its window later, by {@link Recipes#windowBinding}. Before then, and after, a dialog asked for is not shown.
 * Where a dialog runs is {@code FileDialog}'s decision: on Windows its own thread, so the window keeps drawing behind
 * it. The answer arrives on a pool thread, which is where opening runs.
 */
final class Opener {

    private static final Log LOG = Log.of("pix.open");

    private static final List<FileDialog.Filter> IMAGES =
            List.of(new FileDialog.Filter("Images", Pictures.extensions()));

    private final Gui gui;
    private final Model model;
    private volatile GuiApp app;

    Opener(Gui gui, Model model) {
        this.gui = gui;
        this.model = model;
    }

    /** Main thread. Null on the way out. */
    void bind(GuiApp app) {
        this.app = app;
    }

    /** Open {@code place}, and read its folder off the frame loop. */
    void open(Startup.Place place) {
        model.open(place.folder(), place.file());
        gui.offload().execute(() -> model.listed(place.folder(), Folder.images(place.folder())));
    }

    /** Ask for a picture, starting in the folder open, and open it among its folder's images. */
    void file() {
        GuiApp a = app;
        if (a != null) {
            ask(FileDialog.openAsync(a::post, a.windowHandle(), IMAGES, model.doc().folder()));
        }
    }

    /** Ask for a folder, starting at the one open, and open it on its first image. */
    void folder() {
        GuiApp a = app;
        if (a != null) {
            ask(FileDialog.pickFolderAsync(a::post, a.windowHandle(), model.doc().folder()));
        }
    }

    private void ask(CompletableFuture<Optional<Path>> dialog) {
        dialog.whenComplete((picked, failure) -> {
            if (failure != null) {
                LOG.warn("the open dialog could not be shown", failure);
            } else {
                picked.ifPresent(p -> open(Startup.place(List.of(p.toString()))));
            }
        });
    }
}
