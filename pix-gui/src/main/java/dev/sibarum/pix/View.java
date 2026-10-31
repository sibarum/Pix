package dev.sibarum.pix;

import dev.sibarum.pix.core.Messages.Caption;
import dev.sibarum.pix.core.Messages.Edge;
import dev.sibarum.pix.core.Messages.Hold;
import dev.sibarum.pix.core.Messages.Pictured;
import dev.sibarum.pix.core.Messages.Release;
import dev.sibarum.pix.core.Messages.Show;
import dev.sibarum.pix.core.Messages.Step;
import dev.vexelray.framework.api.Subscribe;
import dev.vexelray.gui.core.Feed;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.core.NodeEvent;
import dev.vexelray.gui.core.input.Shortcut;
import sibarum.atchung.Sender;
import sibarum.tactroller.api.Key;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * The view: the picture the viewer chose, fitted to the window, and the keys that step through the folder. A
 * {@code @View}, so it runs on the GUI thread, in each frame's widget tick.
 *
 * <h2>It decides nothing</h2>
 *
 * <p>Which picture is wanted, which are read and which kept is the {@code Viewer}'s. This shows what it is told
 * ({@link Show}, {@link Caption}) and turns keys into messages ({@link Step}, {@link Edge}), declared once as chords:
 * nothing of the application runs at a key press.
 *
 * <h2>It holds the pictures</h2>
 *
 * <p>A picture arrives as a feed a job wrote, which is a handle to pixels on their way to the screen and so cannot sit
 * in the viewer's state. The view keeps each one it is told to {@link Hold}, shows one when told to, and closes it when
 * told to {@link Release} it, which is what lets the renderer give back its texture. The viewer never releases the one
 * shown.
 */
@dev.vexelray.framework.api.View
final class View {

    private final Ui ui;

    /** The pictures held, by path. */
    private final Map<Path, Pictured> held = new HashMap<>();

    /** The picture shown; null before the first. */
    private Pictured shown;

    /** The stage's size in layout px, as last laid out. */
    private float stageW;
    private float stageH;

    View(Gui gui, Ui ui, Sender<Step> step, Sender<Edge> edge) {
        this.ui = ui;
        gui.hear(ui.stage(), NodeEvent.RESIZE, layout -> {
            stageW = layout.rect().w();
            stageH = layout.rect().h();
            refit();
        });
        gui.shortcut(Shortcut.of(Key.RIGHT), step, new Step(1));
        gui.shortcut(Shortcut.of(Key.LEFT), step, new Step(-1));
        gui.shortcut(Shortcut.of(Key.PAGE_DOWN), step, new Step(1));
        gui.shortcut(Shortcut.of(Key.PAGE_UP), step, new Step(-1));
        gui.shortcut(Shortcut.of(Key.HOME), edge, new Edge(false));
        gui.shortcut(Shortcut.of(Key.END), edge, new Edge(true));
    }

    @Subscribe
    public void hold(Hold m) {
        Pictured was = held.put(m.picture().path(), m.picture());
        if (was != null && was != shown) {
            close(was);
        }
    }

    @Subscribe
    public void show(Show m) {
        Pictured p = held.get(m.path());
        if (p == null) {
            return;
        }
        shown = p;
        Fit fit = Fit.of(p.width(), p.height(), stageW, stageH);
        ui.picture(p.feed().orElse(null), fit.widthPercent(), fit.heightPercent(), p.identity());
    }

    @Subscribe
    public void release(Release m) {
        Pictured p = held.get(m.path());
        if (p != null && p != shown) {
            held.remove(m.path());
            close(p);
        }
    }

    @Subscribe
    public void caption(Caption m) {
        ui.caption(m.name(), m.place());
    }

    private static void close(Pictured p) {
        p.feed().ifPresent(Feed::close);
    }

    private void refit() {
        if (shown != null && shown.feed().isPresent()) {
            Fit fit = Fit.of(shown.width(), shown.height(), stageW, stageH);
            ui.fit(fit.widthPercent(), fit.heightPercent());
        }
    }
}
