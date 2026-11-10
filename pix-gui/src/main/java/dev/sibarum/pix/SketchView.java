package dev.sibarum.pix;

import dev.sibarum.pix.core.Messages.SaveSdg;
import dev.sibarum.pix.core.Messages.SdgWritten;
import dev.sibarum.pix.core.Messages.Sizing;
import dev.sibarum.pix.core.Messages.Sketched;
import dev.sibarum.pix.core.Messages.Unit;
import dev.vexelray.framework.api.Subscribe;
import dev.vexelray.framework.api.View;
import dev.vexelray.gui.core.Feed;
import dev.vexelray.gui.core.NodeEvent;
import dev.vexelray.gui.widget.Button;
import sibarum.atchung.Sender;

/**
 * The SDG editor's view (docs/sdg-editor.md): the field the {@code Sketcher} made, shown fitted to the stage, what it
 * is, and Save, told to the {@code Sketcher} as {@link SaveSdg}. It holds the preview's feed while it shows it, and
 * closes it when the next arrives.
 *
 * <p>It decides nothing: whether a save can start is the {@code Sketcher}'s, and where the file goes is the person's,
 * in the OS's dialog. What it keeps is only what it shows, and a save under way so Save cannot be pressed twice.
 */
@View(window = SketchWindow.KEY)
final class SketchView {

    private final SketchWindow page;
    private final Sender<SaveSdg> save;

    /** The field shown, its preview's feed, and the stage's content size, in layout px. */
    private Sketched shown;
    private Feed feed;
    private float stageW;
    private float stageH;
    private boolean saving;

    SketchView(SketchWindow page, Sender<SaveSdg> save) {
        this.page = page;
        this.save = save;
        page.gui().hear(page.stage(), NodeEvent.RESIZE, layout -> {
            stageW = layout.content().w();
            stageH = layout.content().h();
            fit();
        });
        page.status("", false, false, "Save");
    }

    /** A field made, or changed: shown in place of the one before, whose feed is let go. */
    @Subscribe
    public void sketched(Sketched s) {
        Feed before = feed;
        shown = s;
        feed = s.feed();
        fit();
        if (before != null) {
            before.close();
        }
        page.title(s.mode().label() + Ui.DOT + size(s.sizing()));
        paint("");
    }

    /** A save finished: where it went, or why it did not; nothing said when the person cancelled the dialog. */
    @Subscribe
    public void written(SdgWritten w) {
        if (!saving) {
            return;
        }
        saving = false;
        if (w.file().isPresent()) {
            paint("Saved as " + w.file().get().getFileName());
        } else {
            paint(w.why());
        }
    }

    @Subscribe
    public void pressed(Button.Pressed p) {
        if (p.button().equals(page.saveAddress()) && shown != null && !saving) {
            saving = true;
            paint("");
            save.send(new SaveSdg());
        }
    }

    /** The field fitted to the stage: as large as it allows, at the field's aspect. */
    private void fit() {
        if (shown == null) {
            page.field(null, null);
            return;
        }
        page.field(feed, Fit.of(shown.width(), shown.height(), stageW, stageH, true));
    }

    private void paint(String said) {
        boolean danger = said.startsWith("Could not");
        page.status(saving ? "Saving…" : said, danger, shown != null && !saving, saving ? "Saving…" : "Save");
    }

    /**
     * A size as the person gave it, and the texels it makes when that is not the same thing: {@code 1024 × 768 px},
     * {@code 210 × 297 mm at 300 DPI · 2480 × 3508 texels}.
     */
    static String size(Sizing s) {
        String given = number(s.width()) + " × " + number(s.height());
        if (s.unit() == Unit.PX) {
            return given + " px";
        }
        return given + " " + s.unit().label() + " at " + number(s.dpi()) + " DPI" + Ui.DOT + s.texelsWide() + " × "
                + s.texelsHigh() + " texels";
    }

    /** A number as short as it can be said: no decimals for a whole one. */
    static String number(double v) {
        return v == Math.rint(v) ? Long.toString((long) v) : Double.toString(v);
    }
}
