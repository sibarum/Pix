package dev.sibarum.pix;

import dev.sibarum.pix.Keys.EscKey;
import dev.sibarum.pix.Keys.FitKey;
import dev.sibarum.pix.Keys.ZoomKey;
import dev.sibarum.pix.core.Messages.Caption;
import dev.sibarum.pix.core.Messages.Clear;
import dev.sibarum.pix.core.Messages.Edge;
import dev.sibarum.pix.core.Messages.Filmstrip;
import dev.sibarum.pix.core.Messages.Go;
import dev.sibarum.pix.core.Messages.Hold;
import dev.sibarum.pix.core.Messages.HoldThumb;
import dev.sibarum.pix.core.Messages.Open;
import dev.sibarum.pix.core.Messages.Pictured;
import dev.sibarum.pix.core.Messages.Release;
import dev.sibarum.pix.core.Messages.ReleaseThumb;
import dev.sibarum.pix.core.Messages.Show;
import dev.sibarum.pix.core.Messages.Span;
import dev.sibarum.pix.core.Messages.Step;
import dev.sibarum.pix.core.Messages.Thumb;
import dev.vexelray.framework.api.Draw;
import dev.vexelray.framework.api.Subscribe;
import dev.vexelray.framework.core.CloseRequested;
import dev.vexelray.framework.core.WindowCommand;
import dev.vexelray.framework.core.WindowFullScreen;
import dev.vexelray.gui.core.Drawn;
import dev.vexelray.gui.core.Feed;
import dev.vexelray.gui.core.Frame;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.core.Node;
import dev.vexelray.gui.core.NodeEvent;
import dev.vexelray.gui.core.input.DragEvent;
import dev.vexelray.gui.core.input.PinchEvent;
import dev.vexelray.gui.core.input.Shortcut;
import dev.vexelray.gui.core.layout.NodeLayout;
import dev.vexelray.gui.widget.Button;
import sibarum.atchung.Sender;
import sibarum.tactroller.api.Key;
import sibarum.tactroller.api.Modifier;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The view: the picture the viewer chose, fitted to the window and looked at closely, and the keys that step through
 * the folder. A {@code @View}, so it runs on the GUI thread, in each frame's widget tick.
 *
 * <h2>It decides nothing about the folder</h2>
 *
 * <p>Which picture is wanted, which are read and which kept is the {@code Viewer}'s. This shows what it is told
 * ({@link Show}, {@link Caption}) and turns keys into messages ({@link Step}, {@link Edge}, {@link Open}), declared
 * once as chords: nothing of the application runs at a key press. Even a dialog is the viewer's to ask for.
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
 *
 * <h2>It fills the filmstrip</h2>
 *
 * <p>How many cells fit is the view's to know ({@link Strip}), so it tells the viewer how far either side that reaches
 * ({@code Span}), and shows what the viewer says is there ({@code Filmstrip}) from the thumbnails it is told to hold,
 * each a feed of its own. A click on one asks for that picture by its path ({@code Go}).
 *
 * <h2>It fades the controls</h2>
 *
 * <p>The bar and the arrows fade once the pointer has rested, and come back when it moves or a step shows another name
 * ({@link Fade}). The root hears the pointer anywhere in the window and that it left; each control, that the pointer is
 * on it, which holds them. Drawn by the asked half beside the lens, so nothing runs while the pointer rests until the
 * moment the fade starts. Keep shown holds them and moves the bar off the picture.
 */
@dev.vexelray.framework.api.View
final class View {

    /**
     * Layout px of wheel per notch: the framework's scroll step, three em at the 16 px em. The wheel reaches a view as
     * a distance, not as notches (framework-notes FN-6).
     */
    static final float NOTCH_PX = 48f;

    /**
     * What the asked half draws: the layer in front and how its picture is looked at, the other layer and how a step
     * threw the one picture in and the other out, the controls and how far they are shown, and the filmstrip's cells
     * and how far along a step glides them, and the shades of what comes and goes.
     */
    record Drawing(Node picture, Node leaving, Lens lens, Toss toss, List<Node> faded, Fade fade, List<Node> cells,
                   Glide glide, List<Shade> shades) {
    }

    private final Gui gui;
    private final Ui ui;
    private final Sender<Step> step;
    private final Sender<Span> span;
    private final Sender<Open> open;
    private final Sender<WindowCommand> window;

    /** Whether the main window is full screen, as the framework last said. */
    private boolean full;

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

    /** The thumbnails held, by path; what the filmstrip shows; how many cells it has, and the radius last told. */
    private final Map<Path, Thumb> thumbs = new HashMap<>();
    private Filmstrip strip = new Filmstrip(List.of(), 0);
    private int cells;
    private int radius;

    /** The bar's content width and a rem, in px, as last laid out: what the strip is sized from. */
    private float barW;
    private float remPx;
    /** The bar's zoom's width, px, as last laid out, which the strip leaves it. */
    private float zoomW;

    /** The picture shown; null before the first. */
    private Pictured shown;

    /** Which of the two layers is in front, and the picture each draws, null for none. */
    private int front;
    private final Pictured[] onLayer = new Pictured[2];

    /** Pictures let go while a layer still drew them, closed once none does ({@link #letGo}). */
    private final List<Pictured> closing = new ArrayList<>();

    /** The stage's size in layout px, as last laid out. */
    private float stageW;
    private float stageH;

    /** Where the pointer last was over the stage, from its centre, layout px: what the wheel zooms about. */
    private float pointerX;
    private float pointerY;

    /** Whether a pinch is in flight on the stage, which the wheel then leaves alone. */
    private boolean pinching;

    /**
     * How far each thing Rasterware shows and hides itself is shown ({@link Shade}): each cell, then each cell's thumbnail,
     * and the note. The thumbnails drawn at the last painting, so one read since fades up. The words are left to the
     * framework, whose own motion already carries a change of them.
     */
    private static final int THUMBS = Strip.MOST;
    private static final int NOTE = 2 * Strip.MOST;
    private final Shade[] shades = new Shade[NOTE + 1];
    private Set<Path> thumbsDrawn = Set.of();

    /** Whether the pointer is on each of {@link Ui#controls}, and whether Keep shown is on. */
    private final boolean[] over = new boolean[3];
    private boolean kept;

    View(Gui gui, Ui ui, Sender<Step> step, Sender<Edge> edge, Sender<Go> go, Sender<Span> span, Sender<Open> open,
         Sender<WindowCommand> window, Sender<FitKey> fitKey, Sender<ZoomKey> zoomKey, Sender<EscKey> escKey) {
        this.gui = gui;
        this.ui = ui;
        this.window = window;
        this.step = step;
        this.span = span;
        this.open = open;
        gui.hear(ui.bar(), NodeEvent.RESIZE, layout -> {
            barW = layout.content().w();
            sizeStrip();
        });
        gui.hear(ui.zoomNode(), NodeEvent.RESIZE, layout -> {
            zoomW = layout.rect().w();
            sizeStrip();
        });
        gui.hear(ui.keepNode(), NodeEvent.RESIZE, layout -> {
            if (measured(layout)) {
                remPx = layout.rect().w() / Ui.TOOL_REM;
                sizeStrip();
            }
        });
        for (int i = 0; i < Strip.MOST; i++) {
            int at = i;
            gui.hear(ui.cell(i), NodeEvent.CLICK, c -> {
                int p = Strip.path(at, cells, strip.at(), strip.paths().size());
                if (p >= 0) {
                    go.send(new Go(strip.paths().get(p)));
                }
            });
        }
        for (int i = 0; i < Strip.MOST; i++) {
            shades[i] = Shade.at(ui.cell(i), 0f);
            shades[THUMBS + i] = Shade.at(ui.thumb(i), 1f);
        }
        shades[NOTE] = Shade.at(ui.noteNode(), 0f);
        this.state = new Drawing(ui.layer(0), ui.layer(1), Lens.fitted(0f, 0f, 0f, 0f), Toss.NONE,
                ui.faded(), Fade.shown(System.nanoTime()), ui.cells(), Glide.NONE, List.of(shades));
        // The window's root hears the pointer anywhere in it, and that it left; each control, that it rests there.
        gui.hear(gui.root(), NodeEvent.POINTER, p -> {
            if (p.over()) {
                wake();
            } else {
                fade(kept ? state.fade().held(System.nanoTime()) : state.fade().gone(System.nanoTime()));
            }
        });
        Node[] controls = ui.controls();
        for (int i = 0; i < controls.length; i++) {
            int at = i;
            gui.hear(controls[i], NodeEvent.POINTER, p -> {
                over[at] = p.over();
                wake();
            });
        }
        gui.hear(ui.stage(), NodeEvent.RESIZE, layout -> {
            stageW = layout.rect().w();
            stageH = layout.rect().h();
            refit();
        });
        gui.hear(ui.stage(), NodeEvent.POINTER, p -> {
            pointerX = p.localX() - p.nodeW() / 2f;
            pointerY = p.localY() - p.nodeH() / 2f;
        });
        gui.hear(ui.stage(), NodeEvent.WHEEL, w -> {
            // Windows may hand a pinch to the window as Ctrl+wheel as well: while one is in flight, it zooms once.
            if (!pinching) {
                look(state.lens().wheel(-w.dy() / NOTCH_PX, pointerX, pointerY, System.nanoTime()));
            }
        });
        gui.hear(ui.stage(), NodeEvent.PINCH, p -> {
            pinching = p.phase() != PinchEvent.Phase.END;
            look(state.lens().pinched(p.scale(), pointerX, pointerY, System.nanoTime()));
        });
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
        gui.shortcut(Shortcut.of(Key.O, Modifier.CONTROL), open, new Open(false));
        gui.shortcut(Shortcut.of(Key.O, Modifier.CONTROL, Modifier.SHIFT), open, new Open(true));
        gui.hear(ui.nameNode(), NodeEvent.CLICK, c -> open.send(new Open(false)));
        gui.shortcut(Shortcut.of(Key.F11), window, WindowCommand.toggleFullScreen(CloseRequested.MAIN));
        gui.shortcut(Shortcut.of(Key.ESCAPE), escKey, new EscKey());
        // The stage's own: Previous and Next take their double clicks, so pressing one twice quickly only steps twice.
        gui.hear(ui.stage(), NodeEvent.DOUBLE_CLICK,
                c -> window.send(WindowCommand.toggleFullScreen(CloseRequested.MAIN)));
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
            letGo(was);
        }
    }

    /**
     * A picture shown is fitted, in the middle, whatever the one before was zoomed to. From a side, and with a picture
     * before it, it is thrown in on the other layer while that one is flung out from wherever it is drawn now, mid-throw
     * as well. Otherwise, a picture after another fades up over it while that one fades where it is, and over nothing
     * from nothing: nothing is swapped in one frame.
     */
    @Subscribe
    public void show(Show m) {
        Pictured p = held.get(m.path());
        if (p == null) {
            return;
        }
        long now = System.nanoTime();
        Feed feed = p.feed().orElse(null);
        Pictured before = onLayer[front];
        Toss toss = Toss.NONE;
        Toss was = state.toss();
        Lens.Pose from = drawnAt(now);
        if (m.side() != 0 && feed != null && before != null && before != p) {
            toss = Toss.thrown(m.side(), now, stageW, from, was.inOpacity(now));
            if (p == onLayer[1 - front] && was.moving(now)) {
                // The picture going out, wanted back: from where it has got to, as far as it is still shown.
                toss = new Toss(m.side(), now, stageW, was.out(now), was.outOpacity(now), from, was.inOpacity(now));
            }
            front = 1 - front;
        } else if (before != p) {
            toss = Toss.crossfade(now, from, was.inOpacity(now));
            front = 1 - front;
        } else {
            ui.layer(1 - front, null, 0f, 0f);
            onLayer[1 - front] = null;
        }
        Fit fit = Fit.of(p.width(), p.height(), stageW, stageH);
        ui.layer(front, feed, fit.widthPercent(), fit.heightPercent());
        ui.front(front);
        ui.note(feed == null ? p.identity() : null);
        shades[NOTE] = shades[NOTE].toward(feed == null ? 1f : 0f, now);
        onLayer[front] = feed == null ? null : p;
        shown = p;
        state = new Drawing(ui.layer(front), ui.layer(1 - front), Lens.fitted(fitW(fit), fitH(fit), stageW, stageH),
                toss, state.faded(), state.fade(), state.cells(), state.glide(), List.of(shades));
        closeLetGo();
        sayZoom();
        say(Details.KIND, p.identity());
        say(Details.BYTES, Details.bytes(p.bytes()));
        showSaid();
    }

    /**
     * Nothing shown: a folder opened with no pictures. The picture from before fades where it is, and stays held until
     * it is released.
     */
    @Subscribe
    public void clear(Clear m) {
        long now = System.nanoTime();
        shown = null;
        Toss toss = Toss.crossfade(now, drawnAt(now), state.toss().inOpacity(now));
        front = 1 - front;
        ui.layer(front, null, 0f, 0f);
        onLayer[front] = null;
        shades[NOTE] = shades[NOTE].toward(0f, now);
        state = new Drawing(ui.layer(front), ui.layer(1 - front), state.lens(), toss, state.faded(), state.fade(),
                state.cells(), state.glide(), List.of(shades));
        closeLetGo();
        say(Details.KIND, "");
        say(Details.BYTES, "");
        showSaid();
    }

    @Subscribe
    public void release(Release m) {
        Pictured p = held.get(m.path());
        if (p != null && p != shown) {
            held.remove(m.path());
            letGo(p);
        }
    }

    /**
     * The name wanted and its place. The details are the picture shown's, so a name wanted before its picture is read
     * has none until it is.
     */
    @Subscribe
    public void caption(Caption m) {
        ui.name(m.name());
        ui.splash(m.name().isEmpty());
        if (shown == null || !m.name().equals(String.valueOf(shown.path().getFileName()))) {
            say(Details.KIND, "");
            say(Details.BYTES, "");
        }
        say(Details.PLACE, m.place());
        showSaid();
        wake();
    }

    /**
     * What the filmstrip shows. A step moves the pictures along, so the cells take their new thumbnails and glide them
     * from where they were drawn ({@link Glide}); the cells themselves stay where layout puts them.
     */
    @Subscribe
    public void filmstrip(Filmstrip m) {
        int shift = Strip.shift(strip.paths(), strip.at(), m.paths(), m.at());
        strip = m;
        paintStrip(shift != 0);
        float slot = (Strip.CELL_W + Strip.GAP) * remPx;
        Glide glide = state.glide().shifted(shift, slot, slot * (cells / 2), System.nanoTime());
        state = new Drawing(state.picture(), state.leaving(), state.lens(), state.toss(), state.faded(), state.fade(),
                state.cells(), glide, state.shades());
    }

    /** A thumbnail to keep; one held already for that picture is put back, and closed once nothing draws it. */
    @Subscribe
    public void holdThumb(HoldThumb m) {
        Thumb was = thumbs.put(m.thumb().path(), m.thumb());
        paintStrip();
        if (was != null && was != m.thumb()) {
            close(was);
        }
    }

    @Subscribe
    public void releaseThumb(ReleaseThumb m) {
        Thumb t = thumbs.remove(m.path());
        if (t != null) {
            paintStrip();
            close(t);
        }
    }

    /** The main window full screen, or back: the title bar goes with it, and comes back. */
    @Subscribe
    public void fullScreen(WindowFullScreen m) {
        if (m.main()) {
            full = m.on();
            ui.fullScreen(full);
        }
    }

    /** Esc leaves full screen; in the window, it closes Rasterware, by the route the close button takes. */
    @Subscribe
    public void escKey(EscKey k) {
        window.send(full ? WindowCommand.windowed(CloseRequested.MAIN) : WindowCommand.close(CloseRequested.MAIN));
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

    /** Previous and Next, as ← and →; the bar's − and +; Keep shown, on or off; the splash's button, as Ctrl+O. */
    @Subscribe
    public void pressed(Button.Pressed p) {
        if (p.button().equals(ui.splashOpenAddress())) {
            open.send(new Open(false));
        } else if (p.button().equals(ui.previousAddress())) {
            step.send(new Step(-1));
        } else if (p.button().equals(ui.nextAddress())) {
            step.send(new Step(1));
        } else if (p.button().equals(ui.zoomOutAddress())) {
            look(state.lens().stepped(-1, own(), System.nanoTime()));
        } else if (p.button().equals(ui.zoomInAddress())) {
            look(state.lens().stepped(1, own(), System.nanoTime()));
        } else if (p.button().equals(ui.keepAddress())) {
            kept = !kept;
            ui.keep(kept);
            wake();
        }
    }

    /**
     * The asked half: the picture drawn at the lens's zoom, about its box's centre, and moved by its offset, and the
     * controls as far as they are shown, at the frame's moment. Due again while a gesture eases or the controls fade,
     * and when the pointer has rested long enough for them to start; with motion reduced, drawn where each is going.
     */
    @Draw
    public static long draw(Drawing s, Frame frame, Drawn out) {
        Lens l = s.lens();
        Fade f = s.fade();
        long t = frame.monoNanos();
        boolean reduced = frame.reducedMotion();
        float shown = reduced ? f.opacityReduced(t) : f.opacity(t);
        for (Node n : s.faded()) {
            out.opacity(n, shown);
        }
        long wait = f.next(t, reduced);
        long fadeDue = wait == Fade.STILL ? Frame.NEVER : frame.presentNanos() + wait;
        Toss k = s.toss();
        Glide g = s.glide();
        float along = reduced ? 0f : g.offset(t);
        for (Node c : s.cells()) {
            out.offset(c, along, 0f);
        }
        boolean shading = false;
        for (Shade d : s.shades()) {
            out.opacity(d.node(), reduced ? d.to() : d.opacity(t));
            shading |= !reduced && d.moving(t);
        }
        if (reduced) {
            // Nothing thrown, glided or shaded: the new picture, the thumbnails and the words are simply there.
            out.scale(s.picture(), l.to().zoom());
            out.offset(s.picture(), l.to().x(), l.to().y());
            out.opacity(s.picture(), 1f);
            out.opacity(s.leaving(), 0f);
            return fadeDue;
        }
        out.scale(s.picture(), l.zoom(t));
        out.offset(s.picture(), l.x(t) + k.in(t), l.y(t));
        out.opacity(s.picture(), k.inOpacity(t));
        out.scale(s.leaving(), k.from().zoom());
        out.offset(s.leaving(), k.out(t), k.from().y());
        out.opacity(s.leaving(), k.outOpacity(t));
        return l.moving(t) || k.moving(t) || g.moving(t) || shading ? frame.presentNanos() : fadeDue;
    }

    /**
     * The controls back: kept while Keep shown is on or the pointer is on one, else fading again once it has rested.
     * A step wakes them too, so the new name is seen for a rest.
     */
    private void wake() {
        long now = System.nanoTime();
        boolean onOne = over[0] || over[1] || over[2];
        fade(kept || onOne ? state.fade().held(now) : state.fade().woken(now));
    }

    private void fade(Fade fade) {
        state = new Drawing(state.picture(), state.leaving(), state.lens(), state.toss(), state.faded(), fade,
                state.cells(), state.glide(), state.shades());
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
        shade();
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

    /** The shades as they now are, for the asked half. */
    private void shade() {
        state = new Drawing(state.picture(), state.leaving(), state.lens(), state.toss(), state.faded(), state.fade(),
                state.cells(), state.glide(), List.of(shades));
    }

    /** Where the picture in front is drawn at {@code now}: its zoom, and its offset from the stage's centre. */
    private Lens.Pose drawnAt(long now) {
        Lens l = state.lens();
        return new Lens.Pose(l.zoom(now), l.x(now) + state.toss().in(now), l.y(now));
    }

    private void look(Lens lens) {
        state = new Drawing(state.picture(), state.leaving(), lens, state.toss(), state.faded(), state.fade(),
                state.cells(), state.glide(), state.shades());
        sayZoom();
    }

    /** The bar's zoom, saying where the lens is going: fitted, or a percent of the picture's own pixels. */
    private void sayZoom() {
        Lens l = state.lens();
        ui.zoomLevel(l.fitted() ? Ui.FIT : l.percent(own()) + "%");
    }

    /** The zoom at which the picture shown is at its own pixels, one to a layout px; none while nothing is shown. */
    private float own() {
        float fitW = state.lens().fitW();
        return shown == null || !(fitW > 0f) ? 0f : shown.width() / fitW;
    }

    /**
     * Close {@code p}'s feed, or, while a layer still draws it (the picture flung out by a step), once neither does: a
     * feed closed under a node that draws it is a texture given back mid-frame.
     */
    private void letGo(Pictured p) {
        if (p == onLayer[0] || p == onLayer[1]) {
            closing.add(p);
        } else {
            close(p);
        }
    }

    /** Close what was let go while a layer drew it, now that none does. */
    private void closeLetGo() {
        closing.removeIf(p -> {
            boolean drawn = p == onLayer[0] || p == onLayer[1];
            if (!drawn) {
                close(p);
            }
            return !drawn;
        });
    }

    private static void close(Pictured p) {
        p.feed().ifPresent(Feed::close);
    }

    private static void close(Thumb t) {
        t.feed().ifPresent(Feed::close);
    }

    /**
     * As many cells as half the bar holds ({@link Strip}), once the bar and a rem have both been measured; and the
     * viewer told how far either side that reaches, so it reads those thumbnails and no others.
     */
    private void sizeStrip() {
        // Keep shown, the zoom, and the three gaps between the bar's four parts.
        float room = barW - zoomW - remPx * (Ui.TOOL_REM + 3 * Ui.MARGIN_REM);
        int n = Strip.cells(room, remPx);
        ui.strip(n, barW > 0f ? Strip.width(n, remPx) / barW * 100f : 0f);
        cells = n;
        paintStrip();
        if (n / 2 != radius) {
            radius = n / 2;
            span.send(new Span(radius));
        }
    }

    /**
     * Each cell from the filmstrip and the thumbnails held: the picture wanted in the middle, a gap past an end. A cell
     * that comes or goes, and a thumbnail read since the last painting, fade; but not at a step ({@code glided}), whose
     * glide already carries each picture from the cell it was in.
     */
    private void paintStrip(boolean glided) {
        long now = System.nanoTime();
        float cellW = Strip.CELL_W * remPx;
        float cellH = Strip.CELL_H * remPx;
        Set<Path> drawnNow = new HashSet<>();
        for (int i = 0; i < cells; i++) {
            int p = Strip.path(i, cells, strip.at(), strip.paths().size());
            float there = p < 0 ? 0f : 1f;
            shades[i] = glided ? Shade.at(ui.cell(i), there) : shades[i].toward(there, now);
            if (p < 0) {
                ui.cell(i, null, null, "", false);
                continue;
            }
            Path path = strip.paths().get(p);
            Thumb t = thumbs.get(path);
            Feed feed = t == null ? null : t.feed().orElse(null);
            Fit fit = feed == null ? null : Fit.of(t.width(), t.height(), cellW, cellH);
            ui.cell(i, feed, fit, String.valueOf(path.getFileName()), p == strip.at());
            if (feed != null) {
                drawnNow.add(path);
                if (!thumbsDrawn.contains(path)) {
                    shades[THUMBS + i] = shades[THUMBS + i].from(0f, 1f, now);
                } else if (glided) {
                    shades[THUMBS + i] = Shade.at(ui.thumb(i), 1f);
                }
            }
        }
        thumbsDrawn = drawnNow;
        shade();
    }

    private void paintStrip() {
        paintStrip(false);
    }

    private float fitW(Fit fit) {
        return fit.widthPercent() / 100f * stageW;
    }

    private float fitH(Fit fit) {
        return fit.heightPercent() / 100f * stageH;
    }

    private void refit() {
        for (int i = 0; i < onLayer.length; i++) {
            Pictured p = onLayer[i];
            if (p != null) {
                Fit fit = Fit.of(p.width(), p.height(), stageW, stageH);
                ui.fit(i, fit.widthPercent(), fit.heightPercent());
                if (i == front) {
                    look(state.lens().measured(fitW(fit), fitH(fit), stageW, stageH));
                }
            }
        }
    }
}
