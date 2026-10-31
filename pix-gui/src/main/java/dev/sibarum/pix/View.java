package dev.sibarum.pix;

import dev.sibarum.pix.core.Messages.Caption;
import dev.sibarum.pix.core.Messages.Edge;
import dev.sibarum.pix.core.Messages.Show;
import dev.sibarum.pix.core.Messages.Step;
import dev.vexelray.framework.api.Subscribe;
import dev.vexelray.gui.core.Feed;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.core.NodeEvent;
import dev.vexelray.gui.core.input.Shortcut;
import sibarum.atchung.Sender;
import sibarum.tactroller.api.Key;

/**
 * The view: the picture the viewer chose, fitted to the window, and the keys that step through the folder. A
 * {@code @View}, so it runs on the GUI thread, in each frame's widget tick.
 *
 * <h2>It decides nothing</h2>
 *
 * <p>Which picture is wanted, and when it is read, is the {@code Viewer}'s. This shows what it is told ({@link Show},
 * {@link Caption}) and turns keys into messages ({@link Step}, {@link Edge}), declared once as chords: nothing of the
 * application runs at a key press.
 *
 * <h2>It owns the feed it shows</h2>
 *
 * <p>A picture arrives as a feed a job wrote, and the node shows it until the next one arrives. Then the old feed is
 * closed, which is what lets the renderer give back its texture: the one piece of cleaning up the view does, because it
 * is the one part that knows when a picture stops being shown.
 */
@dev.vexelray.framework.api.View
final class View {

    private final Ui ui;

    /** The feed shown, and its size in pixels; null before the first picture and while a file cannot be shown. */
    private Feed shown;
    private int width;
    private int height;

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
    public void show(Show m) {
        Feed was = shown;
        shown = m.feed().orElse(null);
        width = m.width();
        height = m.height();
        Fit fit = Fit.of(width, height, stageW, stageH);
        ui.picture(shown, fit.widthPercent(), fit.heightPercent(), m.identity());
        if (was != null && was != shown) {
            was.close();
        }
    }

    @Subscribe
    public void caption(Caption m) {
        ui.caption(m.name(), m.place());
    }

    private void refit() {
        if (shown != null) {
            Fit fit = Fit.of(width, height, stageW, stageH);
            ui.fit(fit.widthPercent(), fit.heightPercent());
        }
    }
}
