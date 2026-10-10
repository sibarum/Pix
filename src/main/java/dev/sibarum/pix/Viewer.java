package dev.sibarum.pix;

import dev.sibarum.suite.pictures.Decoded;
import dev.sibarum.suite.pictures.Picture;
import dev.sibarum.suite.view.PictureViewer;
import dev.sibarum.suite.view.PictureViewer.Showing;
import dev.sibarum.suite.view.Textures;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.core.app.GuiApp;
import dev.vexelray.gui.krono.KronoGui;
import dev.vexelray.gui.widget.TitleBar;

import java.nio.file.Path;

/**
 * The window: the suite's {@link PictureViewer} as the application's whole tree, on the framework's {@code Gui} and
 * title bar, showing what the {@link Doc} says.
 *
 * <p><b>It draws the document and keeps no place of its own.</b> A step changes {@link Doc#at}, and what is shown is
 * whatever the document's picture is, so a step from a key, the filmstrip or a script is drawn the same way. Until the
 * new picture lands the old one stays up, under the new name and count.
 *
 * <p>Close closes the window, which is quitting; that needs the {@code GuiApp}, which {@link Recipes#windowBinding}
 * hands it, and which is also when the viewer becomes active: the first picture is usually decoded before the window
 * exists, and is drawn then.
 */
final class Viewer {

    private final Model model;
    private final Previewer previewer;
    private final PictureViewer view;
    private volatile GuiApp app;

    Viewer(Gui gui, KronoGui krono, Model model, Previewer previewer, Textures textures, TitleBar bar, Opener opener) {
        this.model = model;
        this.previewer = previewer;
        this.view = new PictureViewer(gui, krono, textures, bar, this::thumbnail, new PictureViewer.Host() {
            @Override
            public void step(int delta) {
                Viewer.this.step(delta);
            }

            @Override
            public void pick(Path path) {
                model.change(d -> path.equals(d.at()) ? d : d.showing(path));
            }

            @Override
            public void close() {
                Viewer.this.close();
            }

            @Override
            public boolean opens() {
                return true;
            }

            @Override
            public void open() {
                opener.file();
            }
        }, "");
        model.onChange(this::show);
        previewer.onRead(p -> view.film());
        show(model.doc());
    }

    /** Main thread. Null on the way out. */
    void bind(GuiApp app) {
        this.app = app;
        view.active(app != null);
    }

    /** Close the window, which is quitting. Nothing before there is a window. */
    void close() {
        GuiApp a = app;
        if (a != null) {
            a.controls().close();
        }
    }

    private void show(Doc doc) {
        view.show(showing(doc));
    }

    /** What the viewer shows for {@code doc}. */
    static Showing showing(Doc doc) {
        Path at = doc.at();
        String count;
        if (!doc.listed()) {
            count = "reading the folder…";
        } else if (doc.pictures().isEmpty()) {
            count = "no images here";
        } else {
            count = "Image " + (doc.index() + 1) + " of " + doc.pictures().size();
        }
        String file = at == null ? "" : name(at);
        String name = at == null ? doc.folder() == null ? "" : name(doc.folder()) : file;
        String title = at == null ? Pix.TITLE : file + " — " + Pix.TITLE;
        Decoded p = doc.preview();
        String info = at == null ? "" : p == null ? "reading…" : p.identity();
        Picture picture = p == null || !p.image() ? null : p.large() != null ? p.large() : p.picture();
        // Keep the picture up while the next is read; clear it for a file with none, or none shown.
        return new Showing(doc.pictures(), doc.index(), name, title, count, info, picture, at != null && p == null);
    }

    private static String name(Path p) {
        return p.getFileName() == null ? p.toString() : p.getFileName().toString();
    }

    private Picture thumbnail(Path path) {
        Decoded p = previewer.kept(path);
        return p == null ? null : p.picture();
    }

    /**
     * Show the image {@code delta} places along from the one shown, stopping at the ends; the extreme values mean the
     * first and the last.
     */
    private void step(int delta) {
        // Inside the commit, not from a read before it: key handlers run on a pool, so two presses read apart
        // from their writes would both step from the same picture, and holding an arrow key would lose half of them.
        model.change(d -> {
            Path target = PictureViewer.target(d.pictures(), d.at(), delta);
            return target == null || target.equals(d.at()) ? d : d.showing(target);
        });
    }
}
