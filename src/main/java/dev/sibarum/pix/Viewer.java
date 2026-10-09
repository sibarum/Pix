package dev.sibarum.pix;

import dev.sibarum.pix.ImageView.Backdrop;
import dev.sibarum.pix.files.Picture;
import dev.sibarum.pix.files.Preview;
import dev.vexelray.canvas.Color;
import dev.vexelray.gui.core.Gui;
import dev.vexelray.gui.core.Node;
import dev.vexelray.gui.core.app.GuiApp;
import dev.vexelray.gui.core.input.InputTopics;
import dev.vexelray.gui.core.input.InteractionState;
import dev.vexelray.gui.core.layout.LayoutEnums.AlignItems;
import dev.vexelray.gui.core.layout.LayoutEnums.Direction;
import dev.vexelray.gui.core.layout.LayoutEnums.Justify;
import dev.vexelray.gui.core.layout.Length;
import dev.vexelray.gui.core.layout.NodeLayout;
import dev.vexelray.gui.core.style.Relief;
import dev.vexelray.gui.core.style.Role;
import dev.vexelray.gui.krono.KronoGui;
import dev.vexelray.gui.krono.Scheduled;
import dev.vexelray.gui.widget.TitleBar;
import sibarum.kronometer.Dur;
import sibarum.kronometer.anim.Ease;
import sibarum.tactroller.api.InputEvent;
import sibarum.tactroller.api.Key;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The window: one picture, as large as the window allows, and a step to the next or previous image in its folder.
 *
 * <p><b>It draws the document and keeps no place of its own.</b> Stepping changes {@link Doc#at}, and what is shown is
 * whatever the document's picture is, so a step from a key, the filmstrip or a script is drawn the same way.
 *
 * <p><b>Fast stepping costs nothing it should not.</b> The viewer asks for nothing itself; it changes the document,
 * and {@link Previewer} reads only the newest request and reads ahead of it. Until the new picture lands the old one
 * stays up, under the new name and count, so holding an arrow key moves the counter at once and never blanks.
 *
 * <p><b>Every control is a {@link Tile}</b>: one size, an icon over a word. They sit in one bar under the picture, in
 * three groups: what this is and Close on the left, moving through the folder in the middle (Previous, the
 * {@link Filmstrip}, Next), and how it is shown on the right (Background, Keep shown).
 *
 * <p><b>The picture is never covered, and never moves.</b> The bar has its own room under the picture. When the
 * pointer rests the bar fades out but keeps that room, so the picture does not grow and shrink as the mouse comes and
 * goes; moving the mouse, clicking or pressing Tab brings it back, and Keep shown stops it going. The one thing drawn
 * over the picture is the backdrop menu, which the user opens and which closes on a choice, a second press, a click on
 * the picture, or Escape.
 *
 * <p>Vexplore's viewer, in a window of its own: there it is a second window that drives the explorer's selection; here
 * it is the application's main tree, on the framework's {@code Gui} and title bar. Close closes the window, which is
 * quitting; it needs the {@code GuiApp} for that, which {@link Recipes#windowBinding} hands it.
 */
final class Viewer {

    /** How long the pointer rests before the bar fades, and how long the fade takes. */
    static final int IDLE_MS = 2500;
    private static final int FADE_MS = 240;

    private static final Length SLAB_CORNER = Length.rem(1.625f);
    /** The title's text column is this wide and clips, so a long name never pushes the filmstrip aside. */
    private static final Length TITLE_W = Length.rem(10f);
    private static final Length SWATCH = Length.rem(1.75f);
    /** How much of a panel's fill is drawn. Its tiles are see-through too ({@link Tile#GLASS}), so together they cover
     *  about four fifths: the picture shows behind the controls and the words on them stay legible. */
    static final float PANEL_ALPHA = 0.5f;

    private final Gui gui;
    private final KronoGui krono;
    private final Model model;
    private final Previewer previewer;
    private final ImageView image;
    private final TitleBar bar;
    private final Node name;
    private final Node info;
    private final Node count;
    private final Tile prev;
    private final Tile next;
    private final Tile background;
    private final Tile keep;
    private final Filmstrip strip;
    private final Node controls;
    private final Node title;
    private final Node tools;
    private final Node menu;
    private final Map<Backdrop, Tile> swatches = new EnumMap<>(Backdrop.class);

    /** The window's box, for drawing the checkerboard across it, and the size it was last drawn at. */
    private volatile NodeLayout windowBox = NodeLayout.ABSENT;
    private long checkered = -1;

    private volatile GuiApp app;
    private Picture shown;
    /** The file {@link #shown} is of: where the next picture is coming from, for the side it slides in on. */
    private Path shownPath;

    /** The bar's idle fade. Guarded by this, except the volatiles, which input reads on its own thread. */
    private volatile long stirred;
    private volatile boolean hidden;
    private volatile boolean keepShown;
    private volatile boolean menuOpen;
    private volatile boolean overBar;
    private final AtomicInteger fade = new AtomicInteger();
    private Scheduled idle;

    Viewer(Gui gui, KronoGui krono, Model model, Previewer previewer, Textures textures, TitleBar bar) {
        this.gui = gui;
        this.krono = krono;
        this.model = model;
        this.previewer = previewer;
        this.bar = bar;

        this.name = text("", Type.BODY, Role.INK, Type.UI);
        this.info = text("", Type.SMALL, Look.TEXT, Type.MONO);
        this.count = text("", Type.META, Look.TEXT, Type.UI);
        gui.landmark(Landmarks.NAME, name);
        gui.landmark(Landmarks.INFO, info);
        gui.landmark(Landmarks.COUNT, count);

        Tile close = Tile.of(gui, Icons.CLOSE, "Close").onPress(this::close);
        this.prev = Tile.of(gui, Icons.PREVIOUS, "Previous").onPress(() -> step(-1));
        this.next = Tile.of(gui, Icons.NEXT, "Next").onPress(() -> step(1));
        this.background = Tile.of(gui, Icons.BACKGROUND, "Background").toggle().onPress(this::toggleMenu);
        this.keep = Tile.of(gui, Icons.PIN, "Keep shown").toggle().onPress(this::toggleKeep);
        gui.landmark(Landmarks.CLOSE, close.node());
        gui.landmark(Landmarks.PREVIOUS, prev.node());
        gui.landmark(Landmarks.NEXT, next.node());
        gui.landmark(Landmarks.BACKGROUND, background.node());
        gui.landmark(Landmarks.KEEP, keep.node());

        this.strip = new Filmstrip(gui, krono, textures, this::thumbnail, this::pick);

        Node words = gui.column().width(TITLE_W).height(Length.AUTO).gap(Length.rem(0.375f))
                .padding(Length.ZERO, Length.rem(0.375f)).clip(true).scroll(false, false)
                .children(name, info, count);
        this.title = slab(gui.row()).alignItems(AlignItems.CENTER).children(close.node(), words);
        Node browse = slab(gui.row()).alignItems(AlignItems.CENTER).children(prev.node(), strip.node(), next.node());
        this.tools = slab(gui.row()).alignItems(AlignItems.CENTER).children(background.node(), keep.node());

        Node left = gui.row().width(Length.grow(1f)).height(Length.AUTO).scroll(false, false).children(title);
        Node right = gui.row().width(Length.grow(1f)).height(Length.AUTO).justify(Justify.END).scroll(false, false)
                .children(tools);
        this.controls = gui.row().width(Length.FILL).height(Length.AUTO).alignItems(AlignItems.CENTER)
                .padding(Length.ZERO, Length.rem(1f)).gap(Length.rem(1f)).scroll(false, false)
                .children(left, browse, right);
        gui.onResize(controls, this::fitStrip);
        gui.onState(controls, s -> overBar = s != InteractionState.NORMAL);

        Node choices = gui.row().height(Length.AUTO).gap(Tile.GAP).scroll(false, false);
        for (Backdrop b : Backdrop.values()) {
            Tile t = Tile.of(gui, swatch(b), b.label()).onPress(() -> {
                backdrop(b);
                closeMenu();
            });
            gui.landmark(Landmarks.BACKDROP + b.name().toLowerCase(Locale.ROOT), t.node());
            swatches.put(b, t);
            choices.append(t.node());
        }
        Node caption = text("BACKGROUND", Type.SMALL, Role.DIM, Type.UI);
        this.menu = gui.column().height(Length.AUTO).padding(Length.rem(1f)).scroll(false, false)
                .floatAt(Length.percent(100f), Length.percent(100f)).visible(false)
                .children(slab(gui.column()).alignItems(AlignItems.CENTER).children(caption, choices));

        this.image = new ImageView(gui, krono, textures, Length.rem(1f), Length.rem(1f)).bare();
        gui.onClick(image.node(), this::closeMenu);
        Node stage = gui.column().width(Length.FILL).height(Length.grow(1f)).scroll(false, false)
                .children(image.node(), menu);
        gui.root().direction(Direction.COLUMN).children(bar.node(), stage, controls);
        gui.onResize(gui.root(), box -> {
            windowBox = box;
            paintBackdrop();
        });
        backdrop(Backdrop.DARK);

        gui.shortcut(Key.LEFT, () -> step(-1));
        gui.shortcut(Key.RIGHT, () -> step(1));
        gui.shortcut(Key.PAGE_UP, () -> step(-1));
        gui.shortcut(Key.PAGE_DOWN, () -> step(1));
        gui.shortcut(Key.HOME, () -> step(Integer.MIN_VALUE));
        gui.shortcut(Key.END, () -> step(Integer.MAX_VALUE));
        gui.shortcut(Key.ESCAPE, () -> {
            if (menuOpen) {
                closeMenu();
            } else {
                close();
            }
        });

        // The framework has no per-node pointer-move hook, so the bar's idle fade reads input off the bus.
        gui.bus().subscribe(InputTopics.INPUT, e -> {
            if (e instanceof InputEvent.PointerMoved || e instanceof InputEvent.ButtonPressed
                    || e instanceof InputEvent.Scrolled
                    || e instanceof InputEvent.KeyPressed k && k.key() == Key.TAB) {
                stir();
            }
        });

        model.onChange(this::show);
        previewer.onRead(p -> film(model.doc()));
        show(model.doc());
    }

    /**
     * Main thread. Null on the way out.
     *
     * <p>The picture is drawn again here, because it usually arrives first: the file asked for is decoded while the
     * window is still being made, and a picture shown with no window to upload into is not shown at all. And the bar's
     * idle count starts here, not when the tree is built, since the window takes about as long as {@link #IDLE_MS} to
     * appear and a bar that faded before anyone saw it is a bar nobody finds.
     */
    void bind(GuiApp app) {
        this.app = app;
        if (app != null) {
            synchronized (this) {
                shown = null;
                shownPath = null;
                show(model.doc());
            }
            stir();
        }
    }

    /** Close the window, which is quitting. Nothing before there is a window. */
    void close() {
        GuiApp a = app;
        if (a != null) {
            a.controls().close();
        }
    }

    /**
     * Draw what the document says: the file's name and place among the folder's images at once, and its picture when
     * it has arrived. The picture already up stays until then.
     */
    private synchronized void show(Doc doc) {
        Path at = doc.at();
        List<Path> pictures = doc.pictures();
        int i = doc.index();
        if (!doc.listed()) {
            count.text("reading the folder…");
        } else if (pictures.isEmpty()) {
            count.text("no images here");
        } else {
            count.text("Image " + (i + 1) + " of " + pictures.size());
        }
        prev.enabled(i > 0);
        next.enabled(i >= 0 && i < pictures.size() - 1);
        strip.show(pictures, i);
        String file = at == null ? "" : at.getFileName() == null ? at.toString() : at.getFileName().toString();
        String folder = doc.folder() == null ? "" : doc.folder().getFileName() == null ? doc.folder().toString()
                : doc.folder().getFileName().toString();
        name.text(at == null ? folder : file);
        bar.title(at == null ? Pix.TITLE : file + " — " + Pix.TITLE);

        Preview p = doc.preview();
        if (at == null) {
            info.text("");
            clear();
            return;
        }
        if (p == null) {
            info.text("reading…");
            return;
        }
        info.text(p.identity());
        if (!p.image()) {
            clear();
            return;
        }
        Picture want = p.large() != null ? p.large() : p.picture();
        if (want != shown) {
            if (shown == null) {
                // The first picture, or the first after a file that was not one: the bar is shown with it, so the
                // controls are seen once there is something to use them on.
                stir();
            }
            // Not remembered when there is no window yet: bind draws it then.
            if (image.show(want, direction(pictures, shownPath, at))) {
                shown = want;
                shownPath = at;
            }
        }
    }

    /** Holding this. */
    private void clear() {
        shown = null;
        shownPath = null;
        image.clear();
    }

    /** Bring the filmstrip up to date: a neighbour's thumbnail may have been read since the last step. */
    private synchronized void film(Doc doc) {
        strip.show(doc.pictures(), doc.index());
    }

    private Picture thumbnail(Path path) {
        Preview p = previewer.kept(path);
        return p == null ? null : p.picture();
    }

    /** Show {@code path}, picked from the filmstrip. */
    private void pick(Path path) {
        model.change(d -> path.equals(d.at()) ? d : d.showing(path));
    }

    /**
     * Show the image {@code delta} places along from the one shown, stopping at the ends; the extreme values mean the
     * first and the last.
     */
    private void step(int delta) {
        // Inside the commit, not from a read before it: key handlers run on a pool, so two presses read apart
        // from their writes would both step from the same picture, and holding an arrow key would lose half of them.
        model.change(d -> {
            Path target = target(d.pictures(), d.at(), delta);
            return target == null || target.equals(d.at()) ? d : d.showing(target);
        });
    }

    /**
     * Which side the picture of {@code to} comes in from, after the picture of {@code from}: 1 if it is later in the
     * folder, -1 if earlier, 0 if it is the same file (a sharper copy arriving) or either is not among {@code pictures}.
     */
    static int direction(List<Path> pictures, Path from, Path to) {
        int a = from == null ? -1 : pictures.indexOf(from);
        int b = to == null ? -1 : pictures.indexOf(to);
        return a < 0 || b < 0 ? 0 : Integer.signum(b - a);
    }

    /** Where a step of {@code delta} from {@code at} lands among {@code pictures}, or null when there are none. */
    static Path target(List<Path> pictures, Path at, int delta) {
        if (pictures.isEmpty()) {
            return null;
        }
        int i = at == null ? -1 : pictures.indexOf(at);
        long to;
        if (delta == Integer.MIN_VALUE) {
            to = 0;
        } else if (delta == Integer.MAX_VALUE) {
            to = pictures.size() - 1;
        } else if (i < 0) {
            to = 0;
        } else {
            to = (long) i + delta;
        }
        return pictures.get((int) Math.max(0, Math.min(pictures.size() - 1, to)));
    }

    /**
     * How many thumbnails fit between the bar's two side groups, which the middle group must not crowd: the strip
     * takes what is left once both sides have their width, so the middle stays centred.
     */
    static int thumbnails(float barWidth, float sideWidth, float tile, float gap, float barGap) {
        float middle = barWidth - 2f * sideWidth - 2f * barGap;
        // The middle slab: its padding, Previous and Next, and a gap after each tile but the last.
        float room = middle - 2f * gap - 2f * tile - gap;
        return room <= 0f ? 0 : (int) (room / (tile + gap));
    }

    private void fitStrip(NodeLayout box) {
        float tile = prev.node().layout().rect().w();
        if (!box.present() || tile <= 0f) {
            return;
        }
        float side = Math.max(title.layout().rect().w(), tools.layout().rect().w());
        float gap = tile / 9f;
        strip.fit(thumbnails(box.content().w(), side, tile, gap, 2f * gap));
    }

    // ---------------------------------------------------------------- background

    private void backdrop(Backdrop b) {
        image.backdrop(b);
        paintBackdrop();
        swatches.forEach((k, t) -> t.on(k == b));
    }

    /**
     * The backdrop is the whole window's, not only the picture's: the window's own fill, and for the checkerboard its
     * drawing, redrawn when the window changes size. The picture's frames are bare, so one pattern runs behind it all.
     */
    private synchronized void paintBackdrop() {
        Backdrop b = image.backdrop();
        Node root = gui.root();
        root.background(ImageView.color(gui.theme(), b));
        if (b != Backdrop.CHECKER) {
            root.picture(null);
            checkered = -1;
            return;
        }
        NodeLayout box = windowBox;
        int w = Math.round(box.rect().w());
        int h = Math.round(box.rect().h());
        long size = (long) w << 32 | h;
        if (box.present() && w > 0 && h > 0 && size != checkered) {
            checkered = size;
            root.picture(ImageView.checks(w, h).picture());
        }
    }

    private void toggleMenu() {
        if (menuOpen) {
            closeMenu();
        } else {
            menuOpen = true;
            menu.visible(true);
            background.on(true);
        }
    }

    private void closeMenu() {
        if (!menuOpen) {
            return;
        }
        menuOpen = false;
        menu.visible(false);
        background.on(false);
        stir();
    }

    /** A swatch of what {@code b} shows behind a picture: its colour, or a small checkerboard. */
    private Node swatch(Backdrop b) {
        Node s = gui.box().size(SWATCH, SWATCH).corner(Length.rem(0.5f)).clip(true)
                .background(ImageView.color(gui.theme(), b))
                .border(Length.dp(1.5f), gui.theme().color(Role.INK));
        if (b == Backdrop.CHECKER) {
            gui.onResize(s, box -> {
                int w = Math.round(box.rect().w());
                int h = Math.round(box.rect().h());
                if (w > 0 && h > 0) {
                    s.picture(ImageView.checks(w, h, Math.max(3, w / 4)).picture());
                }
            });
        }
        return s;
    }

    // ---------------------------------------------------------------- the idle fade

    private void toggleKeep() {
        keepShown = !keepShown;
        keep.on(keepShown);
        stir();
    }

    /** The user did something: show the bar, and start counting again. */
    private void stir() {
        stirred = System.nanoTime();
        if (hidden) {
            reveal();
        }
        arm(IDLE_MS);
    }

    private synchronized void arm(long ms) {
        if (idle == null) {
            idle = krono.after(Dur.ms(ms), this::rest);
        }
    }

    /** The timer ran out: fade the bar if nothing has happened since, or wait out what is left. */
    private void rest() {
        synchronized (this) {
            idle = null;
        }
        if (hidden) {
            return;
        }
        long quiet = (System.nanoTime() - stirred) / 1_000_000L;
        if (quiet < IDLE_MS) {
            arm(IDLE_MS - quiet);
            return;
        }
        if (keepShown || menuOpen || overBar) {
            return;
        }
        hidden = true;
        int g = fade.incrementAndGet();
        controls.hitInert(true);
        krono.ramp(Dur.ms(FADE_MS), Ease.LINEAR, p -> {
            if (fade.get() == g) {
                controls.opacity((float) (1.0 - p));
            }
        }, () -> { });
    }

    private void reveal() {
        fade.incrementAndGet();
        hidden = false;
        controls.opacity(1f).hitInert(false);
    }

    // ---------------------------------------------------------------- building

    /** A group of tiles on a panel of its own. */
    private Node slab(Node n) {
        return n.width(Length.AUTO).height(Length.AUTO).padding(Tile.GAP).gap(Tile.GAP).corner(SLAB_CORNER)
                .scroll(false, false).background(Color.withAlpha(gui.theme().color(Look.CHROME), PANEL_ALPHA))
                .border(Length.dp(1), gui.theme().color(Role.LINE))
                .elevation(gui.theme().elevation(Relief.FLOATING));
    }

    private Node text(String s, Length size, Role ink, int face) {
        return gui.text(s).font(face).textSize(size).textColor(gui.theme().color(ink)).wordWrap(false);
    }
}
