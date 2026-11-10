package dev.sibarum.pix;

import dev.sibarum.pix.core.Messages.Backdrop;
import dev.sibarum.pix.core.Messages.Brush;
import dev.sibarum.pix.core.Messages.Dab;
import dev.sibarum.pix.core.Messages.PickBackdrop;
import dev.sibarum.pix.core.Messages.SaveSdg;
import dev.sibarum.pix.core.Messages.SdgWritten;
import dev.sibarum.pix.core.Messages.Sizing;
import dev.sibarum.pix.core.Messages.Sketched;
import dev.sibarum.pix.core.Messages.Unit;
import dev.vexelray.framework.api.Subscribe;
import dev.vexelray.framework.api.View;
import dev.vexelray.gui.core.Feed;
import dev.vexelray.gui.core.NodeEvent;
import dev.vexelray.gui.core.input.DragEvent;
import dev.vexelray.gui.widget.Button;
import dev.vexelray.gui.widget.Segment;
import dev.vexelray.gui.widget.Slider;
import sibarum.atchung.Sender;

/**
 * The SDG editor's view (docs/sdg-editor.md): the field the {@code Sketcher} made, shown fitted to the stage over the
 * picture traced, if any; the brush the person sets; and what they do, told to the {@code Sketcher}: a drag on the field
 * as {@link Dab}s, Save as {@link SaveSdg}, Trace over… as {@link PickBackdrop}.
 *
 * <p>It decides nothing about the field: where a dab lands is the pointer's place on the field's box, said in texels, and
 * what a dab does is the {@code Sketcher}'s. It holds the feeds it shows, the field's and the picture's, and closes each
 * when the next arrives; and the brush, which is what the person set and nothing else.
 */
@View(window = SketchWindow.KEY)
final class SketchView {

    private final SketchWindow page;
    private final Sender<SaveSdg> save;
    private final Sender<Dab> dabs;
    private final Sender<PickBackdrop> pickBackdrop;

    /** The field shown and its preview's feed; the picture under it and its size; the stage's content, layout px. */
    private Sketched shown;
    private Feed feed;
    private Feed backdrop;
    private int backdropW;
    private int backdropH;
    private float stageW;
    private float stageH;
    private boolean saving;
    private Brush brush = Brush.FIRST;

    SketchView(SketchWindow page, Sender<SaveSdg> save, Sender<Dab> dabs, Sender<PickBackdrop> pickBackdrop) {
        this.page = page;
        this.save = save;
        this.dabs = dabs;
        this.pickBackdrop = pickBackdrop;
        page.gui().hear(page.stage(), NodeEvent.RESIZE, layout -> {
            stageW = layout.content().w();
            stageH = layout.content().h();
            fit();
        });
        page.gui().hear(page.fieldNode(), NodeEvent.DRAG, this::drag);
        page.status("", false, false, "Save");
        showBrush();
    }

    /** A field made: shown in place of the one before, whose feed is let go. */
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
        page.tools(s.mode().paints(), s.mode().traces());
        paint("");
    }

    /** A picture to trace over, read: shown under the field, the one before let go; or why it could not be. */
    @Subscribe
    public void backdrop(Backdrop b) {
        if (b.feed().isEmpty()) {
            paint("Could not trace over " + b.path().getFileName() + ": " + b.why());
            return;
        }
        Feed before = backdrop;
        backdrop = b.feed().get();
        backdropW = b.width();
        backdropH = b.height();
        fit();
        if (before != null) {
            before.close();
        }
        paint("Tracing over " + b.path().getFileName());
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
        } else if (p.button().equals(page.traceAddress())) {
            pickBackdrop.send(new PickBackdrop());
        }
    }

    /** The brush's size, strength or hardness moved. */
    @Subscribe
    public void slid(Slider.Changed c) {
        float v = (float) c.value();
        if (c.slider().equals(page.sizeAddress())) {
            brush = new Brush(radius(v), brush.strength(), brush.hardness(), brush.erase());
        } else if (c.slider().equals(page.strengthAddress())) {
            brush = new Brush(brush.radius(), v, brush.hardness(), brush.erase());
        } else if (c.slider().equals(page.hardnessAddress())) {
            brush = new Brush(brush.radius(), brush.strength(), v, brush.erase());
        } else {
            return;
        }
        showBrush();
    }

    /** Paint or erase. */
    @Subscribe
    public void chose(Segment.Changed c) {
        if (c.segment().equals(page.eraseAddress()) && c.value() instanceof Boolean erase) {
            brush = new Brush(brush.radius(), brush.strength(), brush.hardness(), erase);
            showBrush();
        }
    }

    /** A press on the field starts a stroke there; a move draws it on. Said in texels, off the field or not. */
    private void drag(DragEvent d) {
        if (shown == null || !shown.mode().paints() || d.phase() == DragEvent.Phase.END || !(d.nodeW() > 0f)
                || !(d.nodeH() > 0f)) {
            return;
        }
        float x = (d.x() - d.nodeX()) / d.nodeW() * shown.width();
        float y = (d.y() - d.nodeY()) / d.nodeH() * shown.height();
        dabs.send(new Dab(brush, x, y, d.phase() == DragEvent.Phase.START));
    }

    /** The field fitted to the stage, as large as it allows at the field's aspect, and the picture fitted in it. */
    private void fit() {
        if (shown == null) {
            page.field(null, null);
            return;
        }
        Fit box = Fit.of(shown.width(), shown.height(), stageW, stageH, true);
        page.field(feed, box);
        if (backdrop != null && shown.mode().traces()) {
            float boxW = stageW * box.widthPercent() / 100f;
            float boxH = stageH * box.heightPercent() / 100f;
            page.backdrop(backdrop, Fit.of(backdropW, backdropH, boxW, boxH, true));
        } else {
            page.backdrop(null, null);
        }
    }

    private void showBrush() {
        page.brush(brush, Integer.toString(Math.round(brush.radius() * 2)),
                Math.round(brush.strength() * 100) + "%", Math.round(brush.hardness() * 100) + "%");
    }

    private void paint(String said) {
        boolean danger = said.startsWith("Could not");
        page.status(saving ? "Saving…" : said, danger, shown != null && !saving, saving ? "Saving…" : "Save");
    }

    /** The radius, in texels, the size slider's {@code v} names: from the least to the most, evenly in ratio. */
    static float radius(float v) {
        return (float) (Brush.LEAST * Math.pow(Brush.MOST / Brush.LEAST, Math.clamp(v, 0f, 1f)));
    }

    /** Where the size slider is for {@code radius}: {@link #radius} turned round. */
    static float sizeSlider(float radius) {
        return (float) (Math.log(radius / Brush.LEAST) / Math.log(Brush.MOST / Brush.LEAST));
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
