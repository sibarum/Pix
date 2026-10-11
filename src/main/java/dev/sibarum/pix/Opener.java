package dev.sibarum.pix;

import dev.sibarum.pix.files.Folder;
import dev.vexelray.codec.image.Pictures;
import dev.vexelray.framework.shell.Dialogs;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.nfd.FileDialog;

import java.nio.file.Path;
import java.util.List;

/**
 * Opens a place: the one on the command line, or one picked in the OS's open dialog (Ctrl+O or a click
 * on the name) or its folder dialog (Ctrl+Shift+O).
 *
 * <p>Opening is the document's {@link Model#open} and the folder's listing read off the frame loop, so the file picked
 * is shown before the folder around it has been read, the same as at startup.
 *
 * <p>The dialogs are the framework's {@link Dialogs}, which need no window to be handed out: a dialog asked for before
 * there is one waits for it. Where a dialog runs is theirs to decide: on Windows its own thread, so the window keeps
 * drawing behind it. The answer arrives on the handler lane, and a failure to show the dialog is a cancel, which here
 * is nothing.
 */
final class Opener {

    private static final List<FileDialog.Filter> IMAGES =
            List.of(new FileDialog.Filter("Images", Pictures.extensions()));

    private final Gui gui;
    private final Model model;
    private final Dialogs dialogs;

    Opener(Gui gui, Model model, Dialogs dialogs) {
        this.gui = gui;
        this.model = model;
        this.dialogs = dialogs;
    }

    /** Open {@code place}, and read its folder off the frame loop. */
    void open(Startup.Place place) {
        model.open(place.folder(), place.file());
        gui.offload().execute(() -> model.listed(place.folder(), Folder.images(place.folder())));
    }

    /** Ask for a picture, starting in the folder open, and open it among its folder's images. */
    void file() {
        dialogs.openFile(model.doc().folder(), IMAGES, this::picked, () -> { });
    }

    /** Ask for a folder, starting at the one open, and open it on its first image. */
    void folder() {
        dialogs.openFolder(model.doc().folder(), this::picked, () -> { });
    }

    private void picked(Path p) {
        open(Startup.place(List.of(p.toString())));
    }
}
