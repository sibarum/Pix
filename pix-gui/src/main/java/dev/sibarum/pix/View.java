package dev.sibarum.pix;

import dev.sibarum.pix.Keys.EscKey;
import dev.sibarum.pix.Keys.FitKey;
import dev.sibarum.pix.Keys.ConvertKey;
import dev.sibarum.pix.Keys.DetailsKey;
import dev.sibarum.pix.Keys.LayersKey;
import dev.sibarum.pix.Keys.TraceKey;
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
import dev.sibarum.pix.core.Messages.Convert;
import dev.sibarum.pix.core.Messages.Format;
import dev.sibarum.pix.core.Messages.SaveRoles;
import dev.sibarum.pix.core.Messages.SaveTrace;
import dev.sibarum.pix.core.Messages.TraceSettings;
import dev.sibarum.pix.core.Messages.Written;
import dev.sibarum.pix.core.Messages.Traced;
import dev.sibarum.pix.core.Messages.Tracing;
import dev.vexelray.framework.api.Draw;
import dev.vexelray.framework.api.Subscribe;
import dev.vexelray.framework.core.CloseRequested;
import dev.vexelray.framework.core.WindowCommand;
import dev.vexelray.framework.core.WindowFullScreen;
import dev.vexelray.gui.core.Drawn;
import dev.vexelray.gui.core.Feed;
import dev.vexelray.gui.core.Frame;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.core.ImageSdg;
import dev.vexelray.gui.core.style.SdgRoles;
import dev.vexelray.gui.core.style.Theme;
import dev.vexelray.fields.sdg.Sdg;
import dev.vexelray.gui.core.Node;
import dev.vexelray.gui.core.NodeEvent;
import dev.vexelray.gui.core.input.CursorShape;
import dev.vexelray.gui.core.input.DragEvent;
import dev.vexelray.gui.core.input.PinchEvent;
import dev.vexelray.gui.core.input.Shortcut;
import dev.vexelray.gui.keymap.Keymap;
import dev.vexelray.gui.core.layout.NodeLayout;
import dev.vexelray.gui.widget.Button;
import dev.vexelray.gui.widget.Segment;
import dev.vexelray.gui.widget.Slider;
import sibarum.atchung.Sender;
import sibarum.tactroller.api.Key;
import sibarum.tactroller.api.Modifier;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
     * and how far along a step glides them, and the shades of what comes and goes; and the trace compared with the
     * picture, drawn as it is.
     */
    record Drawing(Node picture, Node leaving, Lens lens, Toss toss, List<Node> faded, Fade fade, List<Node> cells,
                   Glide glide, List<Shade> shades, Compare compare, Peel peel) {
    }

    /**
     * A drawing's layers, one node each, back first: {@code count} of them drawn at the picture's zoom and offset, each
     * {@code step} px further down and right than the one behind it, at {@code shrink} of the picture's size so the fan
     * fits the stage, faint but for {@code picked} (-1: none picked,
     * all whole). None drawn, the picture is; any, the picture is not, since they are it.
     */
    /** How much of a layer not picked is drawn while one is: enough to see where it lies. */
    static final float FAINT = 0.45f;

    record Peel(List<Node> nodes, int count, float step, float shrink, int picked) {
    }

    /**
     * The trace, drawn at the picture's zoom and offset, and moved {@code shift} px across and {@code drop} px down from the
     * top left of the box right of the split, where it is laid out, to where the picture is ({@link TracePanel}); and the line, {@code lineShift} px left, so its
     * middle is on the split.
     */
    record Compare(Node traced, float shift, float drop, Node line, float lineShift) {
    }

    /** The Trace panel's settings, the trace shown, where the split is (0 to 1 of the stage), and what is out. */
    private boolean tracing;
    private TraceSettings settings = TraceSettings.FIRST;
    private Traced traced;
    private float split = 0.5f;
    private boolean splitting;
    private boolean saving;
    private final Sender<Tracing> tracingOut;
    private final Sender<SaveTrace> saveTrace;

    /** The Convert panel: open or not, the format chosen in it, and the sender its Save asks with. */
    private boolean converting;
    private Format chosen = Format.PNG;
    private final Sender<Convert> convertOut;

    /**
     * The Layers panel: open or not, the drawing it shows, each layer's role as set and as saved, the layer picked (-1
     * none), the peel and how far apart, the light page, a drawing a layer as drawn, and the sender Save roles asks with.
     */
    private boolean layering;
    private Sdg drawingShown;
    private List<String> roles = new ArrayList<>();
    private List<String> rolesSaved = List.of();
    private int picked = -1;
    private boolean peeled = true;
    private float apart = 1f;
    private boolean lightPage;
    private final ImageSdg[] images = new ImageSdg[LayersPanel.MOST_DRAWN];
    private final Sender<SaveRoles> saveRolesOut;

    /** Whether the Details panel is open. */
    private boolean informing;

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

    View(Gui gui, Keymap keys, Ui ui, Sender<Step> step, Sender<Edge> edge, Sender<Go> go, Sender<Span> span,
         Sender<Open> open, Sender<WindowCommand> window, Sender<FitKey> fitKey, Sender<ZoomKey> zoomKey,
         Sender<EscKey> escKey, Sender<TraceKey> traceKey, Sender<Tracing> tracingOut, Sender<SaveTrace> saveTrace,
         Sender<ConvertKey> convertKey, Sender<Convert> convertOut, Sender<LayersKey> layersKey,
         Sender<SaveRoles> saveRolesOut, Sender<DetailsKey> detailsKey) {
        this.saveRolesOut = saveRolesOut;
        this.convertOut = convertOut;
        this.gui = gui;
        this.ui = ui;
        this.tracingOut = tracingOut;
        this.saveTrace = saveTrace;
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
                ui.faded(), Fade.shown(System.nanoTime()), ui.cells(), Glide.NONE, List.of(shades),
                new Compare(ui.tracePanel().traced(), 0f, 0f, ui.tracePanel().line(), 0f),
                new Peel(ui.layersPanel().drawnNodes(), 0, 0f, 1f, -1));
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
            compare();
            if (layering) {
                drawLayers();
                peel();
            }
        });
        gui.hear(ui.stage(), NodeEvent.POINTER, p -> {
            pointerX = p.localX() - p.nodeW() / 2f;
            pointerY = p.localY() - p.nodeH() / 2f;
            gui.cursor(ui.stage(), splitting || nearLine(p.localX()) ? CursorShape.RESIZE_HORIZONTAL
                    : CursorShape.DEFAULT);
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
        // A drag that starts on the trace's line moves the line; any other moves the picture.
        gui.hear(ui.stage(), NodeEvent.DRAG, d -> {
            if (d.phase() == DragEvent.Phase.START) {
                splitting = nearLine(d.x() - d.nodeX());
            } else if (d.phase() == DragEvent.Phase.MOVE && splitting) {
                split(split + d.dx() / Math.max(1f, stageW));
            } else if (d.phase() == DragEvent.Phase.MOVE) {
                look(state.lens().moved(d.dx(), d.dy()));
            } else {
                splitting = false;
            }
        });
        // Every window-wide command is the keymap's, so Key bindings… lists it and the person can change its chords.
        // The id is what their changes are stored under, so it is the part to keep stable.
        keys.action("view.next", "Next picture", Shortcut.of(Key.RIGHT), Shortcut.of(Key.PAGE_DOWN))
                .sends(step, new Step(1));
        keys.action("view.previous", "Previous picture", Shortcut.of(Key.LEFT), Shortcut.of(Key.PAGE_UP))
                .sends(step, new Step(-1));
        keys.action("view.first", "First picture", Shortcut.of(Key.HOME)).sends(edge, new Edge(false));
        keys.action("view.last", "Last picture", Shortcut.of(Key.END)).sends(edge, new Edge(true));
        keys.action("file.open", "Open a picture", Shortcut.of(Key.O, Modifier.CONTROL)).sends(open, new Open(false));
        keys.action("file.open-folder", "Open a folder", Shortcut.of(Key.O, Modifier.CONTROL, Modifier.SHIFT))
                .sends(open, new Open(true));
        gui.hear(ui.nameNode(), NodeEvent.CLICK, c -> open.send(new Open(false)));
        keys.action("window.full-screen", "Full screen", Shortcut.of(Key.F11))
                .sends(window, WindowCommand.toggleFullScreen(CloseRequested.MAIN));
        keys.action("window.leave", "Leave full screen, or close", Shortcut.of(Key.ESCAPE)).sends(escKey, new EscKey());
        // The stage's own: Previous and Next take their double clicks, so pressing one twice quickly only steps twice.
        gui.hear(ui.stage(), NodeEvent.DOUBLE_CLICK,
                c -> window.send(WindowCommand.toggleFullScreen(CloseRequested.MAIN)));
        keys.action("view.fit", "Fit the picture", Shortcut.of(Key.DIGIT_0)).sends(fitKey, new FitKey());
        keys.action("view.trace", "Trace to SDG", Shortcut.of(Key.T)).sends(traceKey, new TraceKey());
        keys.action("view.convert", "Convert to another format", Shortcut.of(Key.C))
                .sends(convertKey, new ConvertKey());
        gui.hear(ui.convertPanel().badge(), NodeEvent.CLICK, c -> convertKey.send(new ConvertKey()));
        keys.action("view.layers", "Layers of a drawing", Shortcut.of(Key.L)).sends(layersKey, new LayersKey());
        keys.action("view.details", "Details of the picture", Shortcut.of(Key.I)).sends(detailsKey, new DetailsKey());
        for (int r = 0; r < LayersPanel.MOST_LISTED; r++) {
            int row = r;
            gui.hear(ui.layersPanel().row(r), NodeEvent.CLICK, c -> pickRow(row));
        }
        keys.action("view.zoom-in", "Larger controls",
                Shortcut.of(Key.EQUAL, Modifier.CONTROL), Shortcut.of(Key.NUMPAD_ADD, Modifier.CONTROL))
                .sends(zoomKey, new ZoomKey(1));
        keys.action("view.zoom-out", "Smaller controls",
                Shortcut.of(Key.MINUS, Modifier.CONTROL), Shortcut.of(Key.NUMPAD_SUBTRACT, Modifier.CONTROL))
                .sends(zoomKey, new ZoomKey(-1));
        keys.action("view.zoom-reset", "Controls as they started",
                Shortcut.of(Key.DIGIT_0, Modifier.CONTROL), Shortcut.of(Key.NUMPAD_0, Modifier.CONTROL))
                .sends(zoomKey, new ZoomKey(0));

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
        // A picture held again in its own place (read again after Rasterware wrote over it) lets the old one go even
        // while it is shown: letGo keeps its feed until no layer draws it, and the viewer shows the new one next.
        Pictured was = held.put(m.picture().path(), m.picture());
        if (was != null && was != m.picture()) {
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
            ui.layer(1 - front, null, null, 0f, 0f);
            onLayer[1 - front] = null;
        }
        Fit fit = Fit.of(p.width(), p.height(), stageW, stageH);
        ui.layer(front, feed, p.drawing().orElse(null), fit.widthPercent(), fit.heightPercent());
        ui.front(front);
        ui.note(feed == null ? p.identity() : null);
        shades[NOTE] = shades[NOTE].toward(feed == null ? 1f : 0f, now);
        onLayer[front] = feed == null ? null : p;
        shown = p;
        state = new Drawing(ui.layer(front), ui.layer(1 - front), Lens.fitted(fitW(fit), fitH(fit), stageW, stageH),
                toss, state.faded(), state.fade(), state.cells(), state.glide(), List.of(shades), state.compare(), state.peel());
        closeLetGo();
        sayZoom();
        if (tracing) {
            retrace();
            compare();
        }
        closeConvert();
        ui.convertPanel().format(badge(p));
        ui.kind(traceable(p), p.drawing().isPresent());
        if (informing) {
            paintDetails();
        }
        if (layering && p.drawing().isPresent()) {
            loadLayers();
        } else {
            closeLayers();
        }
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
        ui.layer(front, null, null, 0f, 0f);
        onLayer[front] = null;
        shades[NOTE] = shades[NOTE].toward(0f, now);
        state = new Drawing(ui.layer(front), ui.layer(1 - front), state.lens(), toss, state.faded(), state.fade(),
                state.cells(), state.glide(), List.of(shades), state.compare(), state.peel());
        closeLetGo();
        closeTrace();
        closeConvert();
        ui.convertPanel().format("");
        closeLayers();
        ui.kind(false, false);
        closeDetails();
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
                state.cells(), glide, state.shades(), state.compare(), state.peel());
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
        if (informing) {
            closeDetails();
            return;
        }
        if (layering) {
            closeLayers();
            return;
        }
        if (converting) {
            closeConvert();
            return;
        }
        if (tracing) {
            closeTrace();
            return;
        }
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
        } else if (p.button().equals(ui.detailsAddress())) {
            detailsKey(new DetailsKey());
        } else if (p.button().equals(ui.detailsPanel().closeAddress())) {
            closeDetails();
        } else if (p.button().equals(ui.layersAddress())) {
            layersKey(new LayersKey());
        } else if (p.button().equals(ui.layersPanel().closeAddress())) {
            closeLayers();
        } else if (p.button().equals(ui.layersPanel().peelAddress()) && layering) {
            peeled = !peeled;
            paintLayers();
            peel();
        } else if (p.button().equals(ui.layersPanel().lightAddress()) && layering) {
            lightPage = !lightPage;
            drawLayers();
            paintLayers();
        } else if (p.button().equals(ui.layersPanel().saveAddress()) && layering && !saving) {
            saving = true;
            ui.layersPanel().status("");
            paintLayers();
            saveRolesOut.send(new SaveRoles(shown.path(), roles));
        } else if (p.button().equals(ui.convertPanel().saveAddress())) {
            convertSave();
        } else if (p.button().equals(ui.traceAddress())) {
            traceKey(new TraceKey());
        } else if (p.button().equals(ui.tracePanel().closeAddress())
                || p.button().equals(ui.tracePanel().cancelAddress())) {
            closeTrace();
        } else if (p.button().equals(ui.tracePanel().saveAddress()) && tracing && !saving) {
            saving = true;
            ui.tracePanel().save(drawingName(), true);
            ui.tracePanel().status("");
            saveTrace.send(new SaveTrace());
        }
    }

    // ------------------------------------------------------------------ tracing (docs/trace.md)

    /** T, or Trace in the bar: the panel opened on the picture shown, if it is one made of pixels, or closed. */
    @Subscribe
    public void traceKey(TraceKey k) {
        if (tracing) {
            closeTrace();
        } else if (traceable(shown)) {
            closeConvert();
            closeLayers();
            closeDetails();
            tracing = true;
            ui.tracing(true);
            ui.tracePanel().split(split * 100f);
            compare();
            retrace();
        }
    }

    /** The colours slider moved: traced again with that many, as it moves. */
    @Subscribe
    public void slid(Slider.Changed c) {
        apartSlid(c);
        if (c.slider().equals(ui.tracePanel().colourAddress())) {
            int n = TracePanel.colours(c.value());
            if (n != settings.colours()) {
                settings(new TraceSettings(n, settings.texture()));
            }
        }
    }

    /** Gradients flattened or kept. */
    @Subscribe
    public void chose(Segment.Changed c) {
        roleChosen(c);
        if (c.segment().equals(ui.tracePanel().gradientsAddress()) && c.value() instanceof Boolean texture
                && texture != settings.texture()) {
            settings(new TraceSettings(settings.colours(), texture));
        }
    }

    /**
     * A trace landed. Shown if it is of the picture shown and the panel is open, whatever settings it was traced with:
     * the latest the viewer has, until a newer one lands, marked as tracing while the settings shown are not its own.
     */
    @Subscribe
    public void traced(Traced m) {
        if (!tracing || shown == null || !m.path().equals(shown.path())) {
            return;
        }
        boolean current = m.settings().equals(settings);
        if (m.drawing().isEmpty()) {
            ui.tracePanel().status(m.why());
            return;
        }
        traced = m;
        Fit fit = Fit.of(shown.width(), shown.height(), stageW, stageH);
        ui.tracePanel().drawing(m.drawing().get(), fit.widthPercent(), fit.heightPercent());
        ui.tracePanel().result(m.palette(), Details.bytes(m.bytes()), String.valueOf(m.layers()));
        ui.tracePanel().tags(shown.identity(), "SDG · " + m.layers() + (m.layers() == 1 ? " layer" : " layers"));
        ui.tracePanel().status(current ? "" : TracePanel.TRACING);
        compare();
    }

    /** The save answered: written, the viewer shows the drawing, which closes the panel; not, the panel says why. */
    @Subscribe
    public void written(Written m) {
        saving = false;
        if (tracing) {
            ui.tracePanel().save(drawingName(), false);
            if (m.file().isEmpty()) {
                ui.tracePanel().status(m.why());
            }
        }
        if (layering) {
            paintLayers();
            if (m.file().isEmpty()) {
                ui.layersPanel().status(m.why());
            }
        }
        if (converting) {
            paintConvert();
            ui.convertPanel().status(m.file().isEmpty() ? m.why() : "");
        }
    }

    // ------------------------------------------------------------------ details (docs/details.md)

    /** I, or Details in the bar: the Details panel opened on the picture shown, or closed. */
    @Subscribe
    public void detailsKey(DetailsKey k) {
        if (informing) {
            closeDetails();
            return;
        }
        if (shown == null || shown.feed().isEmpty()) {
            return;
        }
        closeTrace();
        closeLayers();
        closeConvert();
        informing = true;
        ui.informing(true);
        paintDetails();
    }

    /** What the picture shown is, row by row, and its palette; a file that is not a picture closes the panel. */
    private void paintDetails() {
        if (shown == null || shown.feed().isEmpty()) {
            closeDetails();
            return;
        }
        ui.detailsPanel().show(String.valueOf(shown.path().getFileName()),
                Facts.of(shown, java.time.ZoneId.systemDefault(), home()));
        ui.detailsPanel().palette(shown.palette());
    }

    private static Path home() {
        String h = System.getProperty("user.home");
        return h == null ? null : Path.of(h).toAbsolutePath();
    }

    private void closeDetails() {
        if (informing) {
            informing = false;
            ui.informing(false);
        }
    }

    // ------------------------------------------------------------------ a drawing's layers (docs/layers.md)

    /** L, or Layers in the bar: the Layers panel opened on the drawing shown, or closed. */
    @Subscribe
    public void layersKey(LayersKey k) {
        if (layering) {
            closeLayers();
            return;
        }
        if (shown == null || shown.drawing().isEmpty()) {
            return;
        }
        closeTrace();
        closeConvert();
        closeDetails();
        layering = true;
        picked = -1;
        ui.layering(true);
        loadLayers();
    }

    /**
     * The drawing shown, its layers read again: as it is on disk, its roles as saved, the one picked kept if it is still
     * there. A drawing re-read after Save roles comes here, and so does one stepped to with the panel open.
     */
    private void loadLayers() {
        drawingShown = shown.drawing().get().source();
        roles = new ArrayList<>();
        for (Sdg.Layer l : drawingShown.layers()) {
            roles.add(!l.isGroup() && l.paint() instanceof Sdg.Paint.Solid s ? s.role() : "");
        }
        rolesSaved = List.copyOf(roles);
        if (picked >= drawingShown.layers().size()) {
            picked = -1;
        }
        int n = drawingShown.layers().size();
        ui.layersPanel().size(Math.round(drawingShown.width()) + " × " + Math.round(drawingShown.height())
                + " units · " + n + (n == 1 ? " layer" : " layers") + " · front at the top");
        ui.layersPanel().status(n > LayersPanel.MOST_DRAWN
                ? "Drawn whole: picking and peeling take " + LayersPanel.MOST_DRAWN + " layers at most." : "");
        drawLayers();
        paintLayers();
        peel();
    }

    /** A node a layer, each its own drawing with the roles as set, laid out as the picture is; none past the most. */
    private void drawLayers() {
        int n = drawnCount();
        Fit fit = Fit.of(shown.width(), shown.height(), stageW, stageH);
        for (int i = 0; i < LayersPanel.MOST_DRAWN; i++) {
            images[i] = i < n ? layerImage(i) : null;
            ui.layersPanel().drawn(i, images[i], fit.widthPercent(), fit.heightPercent());
        }
    }

    /** How many layers are drawn one a node: all of them, or none when there are more than the panel draws. */
    private int drawnCount() {
        int n = drawingShown == null ? 0 : drawingShown.layers().size();
        return n > LayersPanel.MOST_DRAWN ? 0 : n;
    }

    /**
     * Layer {@code i} alone, as a drawing: its solid paint following the role set for it, or, on the light page, the
     * colour that role takes in the framework's light theme, since the window's own theme is dark.
     */
    private ImageSdg layerImage(int i) {
        Sdg.Layer l = drawingShown.layers().get(i);
        if (!l.isGroup() && l.paint() instanceof Sdg.Paint.Solid s) {
            String role = roles.get(i);
            Sdg.Paint.Solid paint = lightPage
                    ? new Sdg.Paint.Solid(SdgRoles.rgba(Theme.LIGHT, role, s.rgba()))
                    : s.as(role);
            l = new Sdg.Layer(l.name(), l.stencil(), paint, l.opacity());
        }
        return ImageSdg.of(new Sdg(drawingShown.width(), drawingShown.height(), drawingShown.pages(), List.of(l)));
    }

    /** The rows front first, the picked layer's card, the tools and Save roles, as they now are. */
    private void paintLayers() {
        LayersPanel panel = ui.layersPanel();
        int n = drawingShown.layers().size();
        int listed = Math.min(n, LayersPanel.MOST_LISTED);
        for (int r = 0; r < listed; r++) {
            int i = n - 1 - r;
            panel.row(r, swatch(i), layerName(i), kind(i), i == picked);
        }
        panel.rows(listed, n - listed);
        if (picked >= 0) {
            Sdg.Layer l = drawingShown.layers().get(picked);
            boolean solid = !l.isGroup() && l.paint() instanceof Sdg.Paint.Solid;
            String detail = solid
                    ? String.format("#%06x · %s", ((Sdg.Paint.Solid) l.paint()).rgba() >>> 8, l.stencil().kind())
                            .toUpperCase(java.util.Locale.ROOT)
                    : kind(picked);
            panel.picked(swatch(picked), layerName(picked), detail, solid, roles.get(picked));
        } else {
            panel.picked(null, "No layer picked", "Pick one to see it alone, and to give a solid one a theme colour.",
                    false, "");
        }
        panel.tools(peeled, apart, lightPage);
        panel.save(!roles.equals(rolesSaved), saving);
    }

    private String layerName(int i) {
        String name = drawingShown.layers().get(i).name();
        return name == null || name.isBlank() ? "Layer " + (i + 1) : name;
    }

    /** What a layer is: its stencil's kind (or a group), its paint, and its role if it follows one. */
    private String kind(int i) {
        Sdg.Layer l = drawingShown.layers().get(i);
        if (l.isGroup()) {
            return "group";
        }
        String paint = l.paint() instanceof Sdg.Paint.Solid ? "solid"
                : l.paint() instanceof Sdg.Paint.Sampled ? "sampled" : "unknown";
        String role = roles.get(i);
        return l.stencil().kind().toUpperCase(java.util.Locale.ROOT) + " · " + paint
                + (role.isEmpty() ? "" : " · " + role);
    }

    /** A solid layer's colour as it is drawn now, its role followed in the theme shown; none for any other. */
    private Integer swatch(int i) {
        Sdg.Layer l = drawingShown.layers().get(i);
        if (l.isGroup() || !(l.paint() instanceof Sdg.Paint.Solid s)) {
            return null;
        }
        return SdgRoles.rgba(lightPage ? Theme.LIGHT : gui.theme(), roles.get(i), s.rgba());
    }

    /** Row {@code r} (front first) clicked: that layer picked, or, picked already, none. */
    private void pickRow(int r) {
        if (!layering || r >= drawingShown.layers().size()) {
            return;
        }
        int i = drawingShown.layers().size() - 1 - r;
        picked = picked == i ? -1 : i;
        paintLayers();
        peel();
    }

    /** The layers fanned as far apart as the slider says while the peel is on; together while it is off. */
    private void peel() {
        int count = layering ? drawnCount() : 0;
        boolean fanned = peeled && count > 1 && apart > 0f;
        // Smaller as they spread, so the fan stays on the stage: across, what the shrunk picture leaves; down, half that.
        float shrink = fanned ? 1f - 0.45f * apart : 1f;
        Lens l = state.lens();
        float room = Math.min(stageW - shrink * l.fitW(), 2f * (stageH - shrink * l.fitH())) * 0.9f;
        float step = fanned ? Math.max(0f, room) / (count - 1) : 0f;
        ui.layersPanel().frames(layering && fanned, picked);
        state = new Drawing(state.picture(), state.leaving(), state.lens(), state.toss(), state.faded(), state.fade(),
                state.cells(), state.glide(), state.shades(), state.compare(),
                new Peel(state.peel().nodes(), count, step, shrink, picked < count ? picked : -1));
    }

    private void closeLayers() {
        if (!layering) {
            return;
        }
        layering = false;
        drawingShown = null;
        java.util.Arrays.fill(images, null);
        ui.layering(false);
        peel();
    }

    /** The picked layer's theme colour chosen: drawn so at once, and Save roles offered. */
    private void roleChosen(Segment.Changed c) {
        if (layering && picked >= 0 && c.segment().equals(ui.layersPanel().roleAddress())
                && c.value() instanceof String role && !role.equals(roles.get(picked))) {
            roles.set(picked, role);
            if (picked < drawnCount()) {
                Fit fit = Fit.of(shown.width(), shown.height(), stageW, stageH);
                images[picked] = layerImage(picked);
                ui.layersPanel().drawn(picked, images[picked], fit.widthPercent(), fit.heightPercent());
            }
            paintLayers();
        }
    }

    /** How far apart the peel fans the layers. */
    private void apartSlid(Slider.Changed c) {
        if (layering && c.slider().equals(ui.layersPanel().apartAddress())) {
            apart = (float) c.value();
            peel();
        }
    }

    // ------------------------------------------------------------------ converting (docs/convert.md)

    /** C, or the format badge: the Convert panel opened on the picture shown, or closed. */
    @Subscribe
    public void convertKey(ConvertKey k) {
        if (converting) {
            closeConvert();
            return;
        }
        if (shown == null || shown.feed().isEmpty()) {
            return;
        }
        closeLayers();
        closeTrace();
        closeDetails();
        converting = true;
        chosen = offered(Format.PNG) ? Format.PNG : Format.SDG;
        ui.converting(true);
        ui.convertPanel().status("");
        paintConvert();
    }

    /** A format's tile pressed: that one chosen. Pressing the one chosen leaves it chosen. */
    @Subscribe
    public void toggled(Button.Toggled t) {
        for (Format f : Format.values()) {
            if (t.button().equals(ui.convertPanel().tileAddress(f)) && offered(f)) {
                chosen = f;
                paintConvert();
            }
        }
    }

    /**
     * The panel's Save: the picture written beside itself in the format chosen. A picture made of pixels becomes a
     * drawing by Trace, which has the settings a trace needs, so SDG for one closes this panel and opens that.
     */
    private void convertSave() {
        if (!converting || saving || shown == null) {
            return;
        }
        if (chosen == Format.SDG && !Format.svg(shown.path())) {
            closeConvert();
            traceKey(new TraceKey());
            return;
        }
        saving = true;
        ui.convertPanel().status("");
        paintConvert();
        convertOut.send(new Convert(shown.path(), chosen));
    }

    private void closeConvert() {
        if (converting) {
            converting = false;
            ui.converting(false);
        }
    }

    /** Whether the picture shown can be written as {@code f}: anything Rasterware reads as PNG, an SVG or raster as SDG. */
    private boolean offered(Format f) {
        return shown != null && shown.feed().isPresent() && !current(f);
    }

    /** Whether the picture shown is in {@code f} already. */
    private boolean current(Format f) {
        return f == Format.SDG ? shown.drawing().isPresent()
                : shown.drawing().isEmpty() && Format.of(shown.path()).filter(g -> g == f).isPresent();
    }

    /** The tiles, the line on what the chosen format does to this picture, and Save. */
    private void paintConvert() {
        for (Format f : Format.values()) {
            ui.convertPanel().tile(f, offered(f), f == chosen, current(f));
        }
        boolean svg = Format.svg(shown.path());
        boolean drawing = shown.drawing().isPresent();
        String says;
        String save;
        if (chosen == Format.SDG && !svg) {
            says = "Traced into layers of colour, sharp at any size. Trace has the settings.";
            save = "Trace…";
        } else {
            says = chosen == Format.SDG ? "Its paths and colours, exactly, sharp at any size."
                    : drawing || svg ? "Drawn at its own size, as pixels."
                    : chosen.says();
            save = "Save " + stem(shown.path()) + "." + chosen.extension();
        }
        ui.convertPanel().says(says, save, saving);
    }

    /** What the badge says of {@code p}: SDG for a drawing, its format by its name, nothing for a file that is none. */
    private static String badge(Pictured p) {
        if (p.feed().isEmpty()) {
            return "";
        }
        if (p.drawing().isPresent()) {
            return Format.SDG.label();
        }
        String name = String.valueOf(p.path().getFileName());
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot + 1).toUpperCase(java.util.Locale.ROOT) : "";
    }

    private static String stem(Path path) {
        String name = String.valueOf(path.getFileName());
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /** Whether {@code p} can be traced: a picture shown from its pixels, not a drawing already, nor a file that is none. */
    private static boolean traceable(Pictured p) {
        return p != null && p.feed().isPresent() && p.drawing().isEmpty();
    }

    /** The settings changed: shown, and traced again. */
    private void settings(TraceSettings next) {
        settings = next;
        retrace();
    }

    /**
     * Ask for the picture shown to be traced with the settings shown; a picture that cannot be closes the panel. The
     * trace shown stays until the new one lands, unless it is of another picture.
     */
    private void retrace() {
        if (!traceable(shown)) {
            closeTrace();
            return;
        }
        if (traced != null && !traced.path().equals(shown.path())) {
            traced = null;
            ui.tracePanel().drawing(null, 0f, 0f);
            ui.tracePanel().result(List.of(), "", "");
        }
        ui.tracePanel().settings(settings);
        ui.tracePanel().before(Details.bytes(shown.bytes()));
        ui.tracePanel().save(drawingName(), saving);
        ui.tracePanel().status(TracePanel.TRACING);
        tracingOut.send(new Tracing(Optional.of(shown.path()), settings));
    }

    /** The panel closed, the stage whole, the trace let go, and the viewer told nothing is wanted traced. */
    private void closeTrace() {
        if (!tracing) {
            return;
        }
        tracing = false;
        traced = null;
        splitting = false;
        ui.tracing(false);
        tracingOut.send(new Tracing(Optional.empty(), settings));
    }

    /** What Save writes, as the panel names it: the picture's name with {@code .sdg}. */
    private String drawingName() {
        if (shown == null) {
            return "";
        }
        String name = String.valueOf(shown.path().getFileName());
        int dot = name.lastIndexOf('.');
        return (dot > 0 ? name.substring(0, dot) : name) + ".sdg";
    }

    /** Whether {@code x}, px from the stage's left, is on the trace's line, which is there while a trace is shown. */
    private boolean nearLine(float x) {
        return tracing && traced != null && Math.abs(x - split * stageW) <= TracePanel.GRAB_REM * remPx;
    }

    /** The split moved to {@code value}, kept a little in from either edge so the line can be taken again. */
    private void split(float value) {
        split = Math.clamp(value, 0.02f, 0.98f);
        ui.tracePanel().split(split * 100f);
        compare();
    }

    /** The trace and the line drawn where the split and the stage put them. */
    private void compare() {
        Compare c = state.compare();
        Lens l = state.lens();
        float lineW = TracePanel.GRIP_REM * remPx;
        state = new Drawing(state.picture(), state.leaving(), state.lens(), state.toss(), state.faded(), state.fade(),
                state.cells(), state.glide(), state.shades(),
                new Compare(c.traced(), (stageW - l.fitW()) / 2f - split * stageW, (stageH - l.fitH()) / 2f,
                        c.line(), -lineW / 2f), state.peel());
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
            drawCompare(s.compare(), l.to().zoom(), l.to().x(), l.to().y(), out);
            drawPeel(s.peel(), l.to().zoom(), l.to().x(), l.to().y(), s.picture(), out);
            return fadeDue;
        }
        out.scale(s.picture(), l.zoom(t));
        out.offset(s.picture(), l.x(t) + k.in(t), l.y(t));
        out.opacity(s.picture(), k.inOpacity(t));
        drawCompare(s.compare(), l.zoom(t), l.x(t) + k.in(t), l.y(t), out);
        drawPeel(s.peel(), l.zoom(t), l.x(t) + k.in(t), l.y(t), s.picture(), out);
        out.scale(s.leaving(), k.from().zoom());
        out.offset(s.leaving(), k.out(t), k.from().y());
        out.opacity(s.leaving(), k.outOpacity(t));
        return l.moving(t) || k.moving(t) || g.moving(t) || shading ? frame.presentNanos() : fadeDue;
    }

    /** The trace at the picture's zoom and offset, moved left as {@link Compare} says, and the line on the split. */
    /** The layers at the picture's zoom and offset, fanned by the peel and faint but for the one picked; the picture not. */
    private static void drawPeel(Peel p, float zoom, float x, float y, Node picture, Drawn out) {
        if (p.count() == 0) {
            return;
        }
        out.opacity(picture, 0f);
        float middle = (p.count() - 1) / 2f;
        for (int k = 0; k < p.count(); k++) {
            Node n = p.nodes().get(k);
            float along = (k - middle) * p.step();
            out.scale(n, zoom * p.shrink());
            out.offset(n, x * p.shrink() + along, y * p.shrink() + along * 0.5f);
            out.opacity(n, p.picked() < 0 || p.picked() == k ? 1f : FAINT);
        }
    }

    private static void drawCompare(Compare c, float zoom, float x, float y, Drawn out) {
        out.scale(c.traced(), zoom);
        out.offset(c.traced(), x + c.shift(), y + c.drop());
        out.offset(c.line(), c.lineShift(), 0f);
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
                state.cells(), state.glide(), state.shades(), state.compare(), state.peel());
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
                state.cells(), state.glide(), List.of(shades), state.compare(), state.peel());
    }

    /** Where the picture in front is drawn at {@code now}: its zoom, and its offset from the stage's centre. */
    private Lens.Pose drawnAt(long now) {
        Lens l = state.lens();
        return new Lens.Pose(l.zoom(now), l.x(now) + state.toss().in(now), l.y(now));
    }

    private void look(Lens lens) {
        state = new Drawing(state.picture(), state.leaving(), lens, state.toss(), state.faded(), state.fade(),
                state.cells(), state.glide(), state.shades(), state.compare(), state.peel());
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
        // Trace or Layers, Details and Keep shown, the zoom, and the five gaps between the bar's six parts.
        float room = barW - zoomW - remPx * (3 * Ui.TOOL_REM + 5 * Ui.MARGIN_REM);
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
                    ui.tracePanel().fit(fit.widthPercent(), fit.heightPercent());
                }
            }
        }
    }
}
