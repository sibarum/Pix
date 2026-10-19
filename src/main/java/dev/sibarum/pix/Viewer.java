package dev.sibarum.pix;

import dev.sibarum.suite.view.PictureViewer;
import dev.sibarum.suite.view.PictureViewer.Showing;
import dev.vexelray.canvas.ImageFilter;
import dev.vexelray.codec.image.Decoded;
import dev.vexelray.codec.image.Picture;
import dev.vexelray.codec.image.gui.Textures;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.core.app.Settings;
import dev.vexelray.framework.shell.Window;
import dev.vexelray.gui.keymap.Keymap;
import dev.vexelray.gui.krono.KronoGui;
import dev.vexelray.gui.widget.TitleBar;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * The window: the suite's {@link PictureViewer} as the application's whole tree, on the framework's {@code Gui} and
 * title bar, showing what the {@link Doc} says.
 *
 * <p><b>It draws the document and keeps no place of its own.</b> A step changes {@link Doc#at}, and what is shown is
 * whatever the document's picture is, so a step from a key, the filmstrip or a script is drawn the same way. Until the
 * new picture lands the old one stays up, under the new name and count.
 *
 * <p>Close closes the main window, which is quitting, through the framework's handle for it, so a close gate would be
 * asked first. The viewer becomes active when {@link Recipes#windowBinding} says there is a device to upload its
 * pictures to: the first picture is usually decoded before the window exists, and is drawn then.
 */
final class Viewer {

    /** Where the Settings menu's choices are kept: how pictures are scaled moving, and at rest. */
    static final String MOVING = "scaling.moving";
    static final String RESTING = "scaling.resting";
    /** Where the transport's sound is kept: how loud a video is heard, and whether it is muted. */
    static final String VOLUME = "sound.volume";
    static final String MUTED = "sound.muted";

    private final Model model;
    private final Previewer previewer;
    private final PictureViewer view;
    private final Window main;
    /** What puts the window full screen, and back: nothing until there is a window ({@link #screen}). */
    private volatile Consumer<Boolean> screen = on -> { };

    Viewer(Gui gui, KronoGui krono, Model model, Previewer previewer, Textures textures, TitleBar bar, Opener opener,
           Window main, Settings settings, Keymap keys) {
        this.model = model;
        this.previewer = previewer;
        this.main = main;
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

            @Override
            public boolean fullscreens() {
                return true;
            }

            @Override
            public void fullscreen(boolean on) {
                screen.accept(on);
            }

            @Override
            public void scaled(ImageFilter moving, ImageFilter resting) {
                settings.putString(MOVING, moving.name()).putString(RESTING, resting.name()).save();
            }

            @Override
            public void sounded(float volume, boolean muted) {
                settings.putFloat(VOLUME, volume).putBoolean(MUTED, muted).save();
            }
        }, "", keys);
        view.scaling(filter(settings, MOVING, ImageFilter.BILINEAR), filter(settings, RESTING, ImageFilter.LANCZOS));
        view.sound(settings.getFloat(VOLUME, 1f), settings.getBoolean(MUTED, false));
        model.onChange(this::show);
        previewer.onRead(p -> view.film());
        show(model.doc());
    }

    /** Main thread: whether there is a device to draw pictures with. False on the way out. */
    void active(boolean on) {
        view.active(on);
    }

    /**
     * What puts the window full screen and back, once there is a window: a double click on the picture, F11, and
     * Escape to come back. Called on input threads.
     */
    void screen(Consumer<Boolean> fullscreen) {
        screen = fullscreen == null ? on -> { } : fullscreen;
    }

    /** Close the window, which is quitting. From any thread; before there is a window, quitting as soon as there is. */
    void close() {
        main.close();
    }

    private void show(Doc doc) {
        view.show(showing(doc));
    }

    /** What the viewer shows for {@code doc}. */
    static Showing showing(Doc doc) {
        Path at = doc.at();
        String count;
        if (doc.folder() == null) {
            count = "";
        } else if (!doc.listed()) {
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
        Picture picture = p == null || !p.image() ? null : p.picture(1) != null ? p.picture(1) : p.picture();
        // Keep the picture up while the next is read; clear it for a file with none, or none shown.
        return new Showing(doc.pictures(), doc.index(), name, title, count, info, picture, at != null && p == null);
    }

    /** The filter kept under {@code key}, or {@code def} if none is, or it names no filter (an older or edited file). */
    private static ImageFilter filter(Settings settings, String key, ImageFilter def) {
        try {
            return ImageFilter.valueOf(settings.getString(key, def.name()));
        } catch (IllegalArgumentException e) {
            return def;
        }
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
