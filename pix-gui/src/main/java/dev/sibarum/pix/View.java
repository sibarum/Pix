package dev.sibarum.pix;

import dev.sibarum.pix.Keys.FitKey;
import dev.sibarum.pix.Keys.ZoomKey;
import dev.sibarum.pix.core.Messages.Caption;
import dev.sibarum.pix.core.Messages.Edge;
import dev.sibarum.pix.core.Messages.Hold;
import dev.sibarum.pix.core.Messages.Pictured;
import dev.sibarum.pix.core.Messages.Release;
import dev.sibarum.pix.core.Messages.Show;
import dev.sibarum.pix.core.Messages.Step;
import dev.vexelray.framework.api.Draw;
import dev.vexelray.framework.api.Subscribe;
import dev.vexelray.gui.core.Drawn;
import dev.vexelray.gui.core.Feed;
import dev.vexelray.gui.core.Frame;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.core.Node;
import dev.vexelray.gui.core.NodeEvent;
import dev.vexelray.gui.core.input.DragEvent;
import dev.vexelray.gui.core.input.Shortcut;
import dev.vexelray.gui.core.layout.NodeLayout;
import dev.vexelray.gui.widget.Button;
import sibarum.atchung.Sender;
import sibarum.tactroller.api.Key;
import sibarum.tactroller.api.Modifier;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * The view: the picture the viewer chose, fitted to the window and looked at closely, and the keys that step through
 * the folder. A {@code @View}, so it runs on the GUI thread, in each frame's widget tick.
 *
 * <h2>It decides nothing about the folder</h2>
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
 *
 * <h2>It is how the picture is looked at</h2>
 *
 * <p>The wheel zooms about the pointer, a drag moves the picture, 0 fits it again: a {@link Lens}, which is the view's
 * own, since nothing else needs to know. Told and asked halves (vexelray-gui's frame-contract.md): each gesture
 * replaces the lens in {@link #state}, and {@link #draw} reads it at every frame's moment and sets the picture's scale
 * and offset, due again while a gesture eases. Nothing is laid out again for a zoom: the picture keeps its fitted box,
 * drawn larger and elsewhere.
 *
 * <h2>It keeps the bar's words fitting</h2>
 *
 * <p>The name is cut in the middle by the layout itself ({@code Ellipsis}). The details go whole when they do not fit
 * ({@link Details}), which needs their widths: each is heard as it is laid out, and kept while it is hidden, so a bar
 * that widens again knows what fits without showing anything to find out. New words are all shown at once and given
 * way a frame later, once they have been measured.
 */
@dev.vexelray.framework.api.View
final class View {

    /**
     * Layout px of wheel per notch: the framework's scroll step, three em at the 16 px em. The wheel reaches a view as
     * a distance, not as notches (framework-notes FN-6).
     */
    static final float NOTCH_PX = 48f;

    /** What the asked half draws: the picture's node, and how it is looked at. */
    record Drawing(Node picture, Lens lens) {
    }

    private final Gui gui;
    private final Ui ui;
    private final Sender<Step> step;

    /** What each detail says, its width as last laid out, and which are drawn; the dot's width; the room. */
    private final String[] said = {"", "", ""};
    private final float[] widths = new float[Details.COUNT];
    private boolean[] drawn = new boolean[Details.COUNT];
    private float dotW;
    private float room;

    /** The view's drawn state, replaced at each gesture, read by {@link #draw} on every frame. */
    Drawing state;

    /** The pictures held, by path. */
    private final Map<Path, Pictured> held = new HashMap<>();

    /** The picture shown; null before the first. */
    private Pictured shown;

    /** The stage's size in layout px, as last laid out. */
    private float stageW;
    private float stageH;

    /** Where the pointer last was over the stage, from its centre, layout px: what the wheel zooms about. */
    private float pointerX;
    private float pointerY;

    View(Gui gui, Ui ui, Sender<Step> step, Sender<Edge> edge, Sender<FitKey> fitKey, Sender<ZoomKey> zoomKey) {
        this.gui = gui;
        this.ui = ui;
        this.step = step;
        this.state = new Drawing(ui.picture(), Lens.fitted(0f, 0f, 0f, 0f));
        gui.hear(ui.stage(), NodeEvent.RESIZE, layout -> {
            stageW = layout.rect().w();
            stageH = layout.rect().h();
            refit();
        });
        gui.hear(ui.stage(), NodeEvent.POINTER, p -> {
            pointerX = p.localX() - p.nodeW() / 2f;
            pointerY = p.localY() - p.nodeH() / 2f;
        });
        gui.hear(ui.stage(), NodeEvent.WHEEL, w ->
                look(state.lens().wheel(-w.dy() / NOTCH_PX, pointerX, pointerY, System.nanoTime())));
        gui.hear(ui.stage(), NodeEvent.DRAG, d -> {
            if (d.phase() == DragEvent.Phase.MOVE) {
                look(state.lens().moved(d.dx(), d.dy()));
            }
        });
        gui.shortcut(Shortcut.of(Key.RIGHT), step, new Step(1));
        gui.shortcut(Shortcut.of(Key.LEFT), step, new Step(-1));
        gui.shortcut(Shortcut.of(Key.PAGE_DOWN), step, new Step(1));
        gui.shortcut(Shortcut.of(Key.PAGE_UP), step, new Step(-1));
        gui.shortcut(Shortcut.of(Key.HOME), edge, new Edge(false));
        gui.shortcut(Shortcut.of(Key.END), edge, new Edge(true));
        gui.shortcut(Shortcut.of(Key.DIGIT_0), fitKey, new FitKey());
        gui.shortcut(Shortcut.of(Key.EQUAL, Modifier.CONTROL), zoomKey, new ZoomKey(1));
        gui.shortcut(Shortcut.of(Key.MINUS, Modifier.CONTROL), zoomKey, new ZoomKey(-1));
        gui.shortcut(Shortcut.of(Key.DIGIT_0, Modifier.CONTROL), zoomKey, new ZoomKey(0));
        gui.shortcut(Shortcut.of(Key.NUMPAD_ADD, Modifier.CONTROL), zoomKey, new ZoomKey(1));
        gui.shortcut(Shortcut.of(Key.NUMPAD_SUBTRACT, Modifier.CONTROL), zoomKey, new ZoomKey(-1));
        gui.shortcut(Shortcut.of(Key.NUMPAD_0, Modifier.CONTROL), zoomKey, new ZoomKey(0));

        gui.hear(ui.details(), NodeEvent.RESIZE, layout -> {
            room = layout.content().w();
            giveWay();
        });
        for (int i = 0; i < Details.COUNT; i++) {
            int at = i;
            gui.hear(ui.item(i), NodeEvent.RESIZE, layout -> {
                if (measured(layout)) {
                    widths[at] = layout.rect().w();
                    giveWay();
                }
            });
        }
        gui.hear(ui.dot(Details.BYTES), NodeEvent.RESIZE, layout -> {
            if (measured(layout)) {
                dotW = layout.rect().w();
                giveWay();
            }
        });
    }

    @Subscribe
    public void hold(Hold m) {
        Pictured was = held.put(m.picture().path(), m.picture());
        if (was != null && was != shown) {
            close(was);
        }
    }

    /** A picture shown is fitted, in the middle, whatever the one before was zoomed to. */
    @Subscribe
    public void show(Show m) {
        Pictured p = held.get(m.path());
        if (p == null) {
            return;
        }
        shown = p;
        Fit fit = Fit.of(p.width(), p.height(), stageW, stageH);
        ui.picture(p.feed().orElse(null), fit.widthPercent(), fit.heightPercent(), p.identity());
        look(Lens.fitted(fitW(fit), fitH(fit), stageW, stageH));
        say(Details.KIND, p.identity());
        say(Details.BYTES, Details.bytes(p.bytes()));
        showSaid();
    }

    @Subscribe
    public void release(Release m) {
        Pictured p = held.get(m.path());
        if (p != null && p != shown) {
            held.remove(m.path());
            close(p);
        }
    }

    /**
     * The name wanted and its place. The details are the picture shown's, so a name wanted before its picture is read
     * has none until it is.
     */
    @Subscribe
    public void caption(Caption m) {
        ui.name(m.name());
        if (shown == null || !m.name().equals(String.valueOf(shown.path().getFileName()))) {
            say(Details.KIND, "");
            say(Details.BYTES, "");
        }
        say(Details.PLACE, m.place());
        showSaid();
    }

    @Subscribe
    public void fitKey(FitKey k) {
        look(state.lens().fit(System.nanoTime()));
    }

    @Subscribe
    public void zoomKey(ZoomKey k) {
        if (k.direction() > 0) {
            gui.zoomIn();
        } else if (k.direction() < 0) {
            gui.zoomOut();
        } else {
            gui.resetZoom();
        }
    }

    /** Previous and Next, as ← and →. */
    @Subscribe
    public void pressed(Button.Pressed p) {
        if (p.button().equals(ui.previousAddress())) {
            step.send(new Step(-1));
        } else if (p.button().equals(ui.nextAddress())) {
            step.send(new Step(1));
        }
    }

    /**
     * The asked half: the picture drawn at the lens's zoom, about its box's centre, and moved by its offset, at the
     * frame's moment. Due again while a gesture eases; with motion reduced, drawn where it is going.
     */
    @Draw
    public static long draw(Drawing s, Frame frame, Drawn out) {
        Lens l = s.lens();
        if (frame.reducedMotion()) {
            out.scale(s.picture(), l.to().zoom());
            out.offset(s.picture(), l.to().x(), l.to().y());
            return Frame.NEVER;
        }
        long t = frame.monoNanos();
        out.scale(s.picture(), l.zoom(t));
        out.offset(s.picture(), l.x(t), l.y(t));
        return l.moving(t) ? frame.presentNanos() : Frame.NEVER;
    }

    private void say(int detail, String text) {
        said[detail] = text;
        ui.detail(detail, text);
    }

    /** Every detail that says something drawn, to be measured; {@link #giveWay} takes away what does not fit. */
    private void showSaid() {
        for (int i = 0; i < Details.COUNT; i++) {
            drawn[i] = !said[i].isEmpty();
        }
        ui.shown(drawn);
    }

    private void giveWay() {
        boolean[] says = new boolean[Details.COUNT];
        for (int i = 0; i < says.length; i++) {
            says[i] = !said[i].isEmpty();
        }
        boolean[] next = Details.shown(widths, says, dotW, room);
        if (!java.util.Arrays.equals(next, drawn)) {
            drawn = next;
            ui.shown(drawn);
        }
    }

    /** Whether {@code layout} is of a node drawn: a hidden one is laid out at nothing, which is not its width. */
    private static boolean measured(NodeLayout layout) {
        return layout.present() && layout.rect().w() > 0f;
    }

    private void look(Lens lens) {
        state = new Drawing(state.picture(), lens);
    }

    private static void close(Pictured p) {
        p.feed().ifPresent(Feed::close);
    }

    private float fitW(Fit fit) {
        return fit.widthPercent() / 100f * stageW;
    }

    private float fitH(Fit fit) {
        return fit.heightPercent() / 100f * stageH;
    }

    private void refit() {
        if (shown != null && shown.feed().isPresent()) {
            Fit fit = Fit.of(shown.width(), shown.height(), stageW, stageH);
            ui.fit(fit.widthPercent(), fit.heightPercent());
            look(state.lens().measured(fitW(fit), fitH(fit), stageW, stageH));
        }
    }
}
