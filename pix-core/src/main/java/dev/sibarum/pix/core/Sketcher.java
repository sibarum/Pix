package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.Backdrop;
import dev.sibarum.pix.core.Messages.CreateSdg;
import dev.sibarum.pix.core.Messages.Dab;
import dev.sibarum.pix.core.Messages.EditMode;
import dev.sibarum.pix.core.Messages.PickBackdrop;
import dev.sibarum.pix.core.Messages.ReadBackdrop;
import dev.sibarum.pix.core.Messages.Repaint;
import dev.sibarum.pix.core.Messages.SaveSdg;
import dev.sibarum.pix.core.Messages.SdgWritten;
import dev.sibarum.pix.core.Messages.Sizing;
import dev.sibarum.pix.core.Messages.Sketched;
import dev.sibarum.pix.core.Messages.WriteSdg;
import dev.vexelray.codec.image.Pictures;
import dev.vexelray.fields.sdg.SdgFile;
import dev.vexelray.framework.api.Component;
import dev.vexelray.framework.api.Subscribe;
import dev.vexelray.framework.core.PickFile;
import dev.vexelray.framework.core.Picked;
import dev.vexelray.framework.core.Timer;
import dev.vexelray.gui.core.Feed;
import dev.vexelray.gui.core.ImageSdg;
import sibarum.atchung.Sender;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * The field being drawn by hand, and the one part that holds it (docs/sdg-editor.md).
 *
 * <p><b>Why a component, and why on a lane of its own.</b> A field is millions of floats, changed in place stroke by
 * stroke, and the strokes must land in the order they were drawn. That rules out a job (jobs are in no order at all) and
 * the viewer's ledger (a state replaced whole at every step). So it lives here, in a field nothing else can reach, as
 * phasearray's {@code Sound} keeps its speaker: reached only by messages, in order, on the {@code sketch} lane, so a large
 * field's work never holds up the viewer. What crosses is a preview in a feed, or the drawing as a handle.
 *
 * <p><b>The preview is one feed for the life of a field</b>, of three slots, written again after every dab: only the
 * pixels the dab touched are made again ({@link Sketch#paint}), and the whole preview is copied into a free slot. When
 * no slot is free (the window has not shown the last two yet), it asks the framework's {@link Timer} for a
 * {@link Repaint} a moment later rather than waiting, so the last dab of a stroke is always shown.
 *
 * <p>Saving, and the picture to trace over, ask the OS's dialogs first (the framework's {@link PickFile}, under ids
 * the viewer's own never use); {@link WriteDrawing} writes and {@link ReadBackdropPicture} reads.
 */
@Component(lane = "sketch")
public final class Sketcher {

    /** The longest side of the preview the editor shows. */
    static final int PREVIEW = 1024;

    /** The slots of the preview's feed: one shown, one landing, one being written. */
    static final int SLOTS = 3;

    /** How soon to try again when every slot is in use: about a frame. */
    static final Duration RETRY = Duration.ofMillis(8);

    /** This part's questions to the dialogs: the framework answers with the same id. */
    static final long PICK = 0x5d6_0001L, PICK_BACKDROP = 0x5d6_0002L;

    /** What the save dialog offers. */
    static final List<PickFile.Filter> DRAWINGS = List.of(new PickFile.Filter("SDG drawing", List.of(SdgFile.XML)));

    /** What the open dialog for a picture to trace over offers. */
    static final List<PickFile.Filter> PICTURES = List.of(new PickFile.Filter("Pictures", Pictures.extensions()));

    private final Sender<Sketched> sketched;
    private final Sender<PickFile> pick;
    private final Sender<WriteSdg> write;
    private final Sender<SdgWritten> written;
    private final Sender<ReadBackdrop> readBackdrop;
    private final Timer timer;

    /** The field, row by row ({@link Sketch}); null before one is made. */
    private float[] field;
    private int width;
    private int height;
    private Sizing sizing = Sizing.FIRST;
    private EditMode mode = EditMode.STENCIL;
    /** Whether a save is under way, from the dialog asked to the file written. */
    private boolean saving;

    /** The preview, its feed, the stamp the next slot is handed over with, and whether it has changed since. */
    private Sketch.Preview preview;
    private Feed feed;
    private long stamp;
    private boolean dirty;
    /** A repaint asked of the timer and not heard yet: at most one is out. */
    private Timer.Scheduled retry;

    /** Where the last dab was, which the next draws from; the stroke so far, and the texels it has reached. */
    private float lastX;
    private float lastY;
    private Sketch.Stroke stroke;
    private Sketch.Rect reached = Sketch.Rect.NONE;

    /** Whether a picture is under the field, which the preview is then a shade over; and whether one is being read. */
    private boolean over;
    private boolean pickingBackdrop;

    public Sketcher(Sender<Sketched> sketched, Sender<PickFile> pick, Sender<WriteSdg> write,
                    Sender<SdgWritten> written, Sender<ReadBackdrop> readBackdrop, Timer timer) {
        this.sketched = sketched;
        this.pick = pick;
        this.write = write;
        this.written = written;
        this.readBackdrop = readBackdrop;
        this.timer = timer;
    }

    /**
     * A new field, in place of the one being drawn, with a feed of its own: the editor closes the one before. A size
     * the New window would not offer is ignored. A picture traced over stays under the new field.
     */
    @Subscribe
    public void create(CreateSdg c) {
        if (!c.sizing().why().isEmpty()) {
            return;
        }
        sizing = c.sizing();
        mode = c.mode();
        width = sizing.texelsWide();
        height = sizing.texelsHigh();
        field = Sketch.filled(width, height, c.inside());
        stroke = Sketch.Stroke.of(width, height);
        reached = Sketch.Rect.NONE;
        preview = Sketch.preview(field, width, height, PREVIEW, shaded());
        feed = Feed.of(preview.width(), preview.height(), SLOTS, false);
        stamp = 0;
        dirty = true;
        present();
        sketched.send(new Sketched(feed, width, height, sizing, mode));
    }

    /** The brush, where the pointer is: a stroke started, or drawn on from the last dab. Nothing in a mode that does not paint. */
    @Subscribe(capacity = 1024)
    public void dab(Dab d) {
        if (field == null || !mode.paints()) {
            return;
        }
        if (d.first()) {
            stroke.begin(width, reached);
            reached = Sketch.Rect.NONE;
        }
        Sketch.Rect touched = d.first()
                ? Sketch.dab(field, stroke, width, height, d.brush(), d.x(), d.y())
                : Sketch.line(field, stroke, width, height, d.brush(), lastX, lastY, d.x(), d.y());
        lastX = d.x();
        lastY = d.y();
        reached = reached.union(touched);
        if (!touched.empty()) {
            Sketch.paint(field, width, height, preview, touched, shaded());
            dirty = true;
            present();
        }
    }

    /** The timer's answer: the preview shown again, if it is still waiting for a slot. */
    @Subscribe
    public void repaint(Repaint r) {
        retry = null;
        present();
    }

    /** Save: where, first. Nothing to save, or a save already under way, does nothing. */
    @Subscribe
    public void save(SaveSdg s) {
        if (field == null || saving) {
            return;
        }
        saving = true;
        pick.send(PickFile.save(PICK, "", "Untitled." + SdgFile.XML, DRAWINGS));
    }

    /** A picture to trace over: which, first. */
    @Subscribe
    public void pickBackdrop(PickBackdrop p) {
        if (pickingBackdrop) {
            return;
        }
        pickingBackdrop = true;
        pick.send(PickFile.open(PICK_BACKDROP, "", PICTURES));
    }

    /** Where to save, or which picture to trace over, or that the person cancelled. */
    @Subscribe
    public void picked(Picked m) {
        if (m.id() == PICK_BACKDROP && pickingBackdrop) {
            pickingBackdrop = false;
            Optional<Path> file = m.isCancelled() ? Optional.empty() : Begin.path(m.path());
            file.ifPresent(f -> readBackdrop.send(new ReadBackdrop(f)));
            return;
        }
        if (m.id() != PICK || !saving) {
            return;
        }
        Optional<Path> file = m.isCancelled() ? Optional.empty() : Begin.path(m.path());
        if (file.isEmpty() || field == null) {
            saving = false;
            written.send(new SdgWritten(Optional.empty(), ""));
            return;
        }
        Path named = named(file.get());
        write.send(new WriteSdg(named, ImageSdg.of(Sketch.drawing(field, width, height, sizing, mode))));
    }

    /** A picture to trace over, read: the field shown as a shade over it from now. One that would not read changes nothing. */
    @Subscribe
    public void backdrop(Backdrop b) {
        if (b.feed().isEmpty() || over) {
            return;
        }
        over = true;
        if (field != null) {
            preview = Sketch.preview(field, width, height, PREVIEW, shaded());
            dirty = true;
            present();
        }
    }

    /** A save finished, one way or the other: another may start. */
    @Subscribe
    public void written(SdgWritten m) {
        saving = false;
    }

    /** {@code file} with {@code .sdg} for its extension, if the person typed none. */
    static Path named(Path file) {
        String name = String.valueOf(file.getFileName());
        return name.toLowerCase(java.util.Locale.ROOT).endsWith("." + SdgFile.XML) ? file
                : file.resolveSibling(name + "." + SdgFile.XML);
    }

    /** Whether the preview is a shade over a picture: there is one, and this mode traces. */
    private boolean shaded() {
        return over && mode.traces();
    }

    /**
     * The preview handed to the feed, if it changed and a slot is free; if none is, a repaint asked of the timer. A feed
     * closed (the editor let it go for a newer field) takes nothing and is not tried again.
     */
    private void present() {
        if (!dirty || feed == null || feed.closed()) {
            return;
        }
        int slot = feed.take();
        if (slot < 0) {
            if (retry == null && !feed.closed()) {
                retry = timer.after(RETRY, Messages.REPAINT_TOPIC, new Repaint());
            }
            return;
        }
        byte[] level = feed.level(slot, 0);
        System.arraycopy(preview.rgba(), 0, level, 0, level.length);
        feed.handOver(slot, stamp++);
        dirty = false;
    }
}
