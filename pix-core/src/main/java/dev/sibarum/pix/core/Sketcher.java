package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.CreateSdg;
import dev.sibarum.pix.core.Messages.EditMode;
import dev.sibarum.pix.core.Messages.SaveSdg;
import dev.sibarum.pix.core.Messages.SdgWritten;
import dev.sibarum.pix.core.Messages.Sizing;
import dev.sibarum.pix.core.Messages.Sketched;
import dev.sibarum.pix.core.Messages.WriteSdg;
import dev.vexelray.fields.sdg.SdgFile;
import dev.vexelray.framework.api.Component;
import dev.vexelray.framework.api.Subscribe;
import dev.vexelray.framework.core.PickFile;
import dev.vexelray.framework.core.Picked;
import dev.vexelray.gui.core.Feed;
import dev.vexelray.gui.core.ImageSdg;
import dev.vexelray.vulkan.present.MipChain;
import sibarum.atchung.Sender;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * The field being drawn by hand, and the one part that holds it (docs/sdg-editor.md).
 *
 * <p><b>Why a component, and why on a lane of its own.</b> A field is millions of floats, changed in place stroke by
 * stroke, and the strokes must land in the order they were drawn. That rules out a job (jobs are in no order at all) and
 * the viewer's ledger (a state replaced whole at every step). So it lives here, in a field nothing else can reach, as
 * phasearray's {@code Sound} keeps its speaker: reached only by messages, in order, on the {@code sketch} lane, so a large
 * field's work never holds up the viewer. What crosses is a preview in a feed of its own, or the drawing as a handle.
 *
 * <p>Saving asks the OS's save dialog first (the framework's {@link PickFile}, under {@link #PICK}, which the viewer's
 * own dialogs never use), and {@link WriteDrawing} writes it.
 */
@Component(lane = "sketch")
public final class Sketcher {

    /** The longest side of the preview the editor shows. */
    static final int PREVIEW = 1024;

    /** This part's question to the save dialog: the framework answers with the same id. */
    static final long PICK = 0x5d6_0001L;

    /** What the save dialog offers. */
    static final List<PickFile.Filter> DRAWINGS = List.of(new PickFile.Filter("SDG drawing", List.of(SdgFile.XML)));

    private final Sender<Sketched> sketched;
    private final Sender<PickFile> pick;
    private final Sender<WriteSdg> write;
    private final Sender<SdgWritten> written;

    /** The field, row by row ({@link Sketch}); null before one is made. */
    private float[] field;
    private int width;
    private int height;
    private Sizing sizing = Sizing.FIRST;
    private EditMode mode = EditMode.STENCIL;
    /** Whether a save is under way, from the dialog asked to the file written. */
    private boolean saving;

    public Sketcher(Sender<Sketched> sketched, Sender<PickFile> pick, Sender<WriteSdg> write,
                    Sender<SdgWritten> written) {
        this.sketched = sketched;
        this.pick = pick;
        this.write = write;
        this.written = written;
    }

    /** A new field, in place of the one being drawn. A size the New window would not offer is ignored. */
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
        show();
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

    /** Where to save, or that the person cancelled. */
    @Subscribe
    public void picked(Picked m) {
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

    private void show() {
        Sketch.Preview p = Sketch.preview(field, width, height, PREVIEW);
        Feed feed = Feed.still(p.width(), p.height(), true, MipChain.of(p.rgba(), p.width(), p.height()));
        sketched.send(new Sketched(feed, width, height, sizing, mode));
    }
}
