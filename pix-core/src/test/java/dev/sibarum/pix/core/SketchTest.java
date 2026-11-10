package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.EditMode;
import dev.sibarum.pix.core.Messages.SdgWritten;
import dev.sibarum.pix.core.Messages.Sizing;
import dev.sibarum.pix.core.Messages.Unit;
import dev.sibarum.pix.core.Messages.WriteSdg;
import dev.vexelray.fields.art.Raster;
import dev.vexelray.fields.sdg.Sdg;
import dev.vexelray.fields.sdg.SdgFile;
import dev.vexelray.fields.sdg.SdgRender;
import dev.vexelray.gui.core.ImageSdg;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A drawing made by hand (docs/sdg-editor.md): its size, its field, and what it is saved as. */
class SketchTest {

    private static final Sketch.Look GREY = new Sketch.Look(false, false), OVER = new Sketch.Look(true, false);

    @TempDir
    Path dir;

    @Test
    void pixelsAreTexels() {
        Sizing s = new Sizing(640, 480, Unit.PX, 300);
        assertEquals(1.0, s.scale());
        assertEquals(640, s.texelsWide());
        assertEquals(480, s.texelsHigh());
        assertEquals("", s.why());
    }

    @Test
    void aLengthIsTexelsByItsDpi() {
        Sizing a4 = new Sizing(210, 297, Unit.MM, 300);
        assertEquals(2480, a4.texelsWide());
        assertEquals(3508, a4.texelsHigh());
        assertEquals(300 / 25.4, a4.scale(), 1e-9);
        assertEquals(144, new Sizing(2, 1, Unit.IN, 72).texelsWide());
    }

    @Test
    void aNewUnitSaysTheSameSize() {
        Sizing px = new Sizing(600, 300, Unit.PX, 300);
        assertEquals(new Sizing(2, 1, Unit.IN, 300), px.in(Unit.IN));
        assertEquals(new Sizing(50.8, 25.4, Unit.MM, 300), px.in(Unit.MM));
        assertEquals(new Sizing(144, 72, Unit.PT, 300), px.in(Unit.IN).in(Unit.PT));
        assertEquals(px, px.in(Unit.IN).in(Unit.PX));
        Sizing square = new Sizing(1024, 1024, Unit.PX, 300);
        assertEquals(86.7, square.in(Unit.MM).width(), "a length to hundredths");
        assertEquals(square, square.in(Unit.MM).in(Unit.PX), "back to whole pixels");
    }

    @Test
    void aFieldTooLargeOrTooSmallSaysWhy() {
        assertFalse(new Sizing(9000, 10, Unit.PX, 300).why().isEmpty());
        assertFalse(new Sizing(5000, 5000, Unit.PX, 300).why().isEmpty());
        assertFalse(new Sizing(0.1, 10, Unit.PX, 300).why().isEmpty());
        assertEquals("", new Sizing(4096, 4096, Unit.PX, 300).why());
    }

    @Test
    void aPreviewIsTheFieldInGreyNoLargerThanAsked() {
        float[] field = Sketch.filled(400, 200, false);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                field[y * 400 + x] = 1f;   // the four by four the first preview pixel stands for
            }
        }
        Sketch.Preview p = Sketch.preview(field, 400, 200, 100, GREY);
        assertEquals(100, p.width());
        assertEquals(50, p.height());
        assertEquals((byte) 255, p.rgba()[0], "the inside light");
        assertEquals(0, p.rgba()[4 * 60], "the outside dark");
        assertEquals((byte) 255, p.rgba()[3], "opaque");
    }

    @Test
    void overAPictureTheOutsideIsShadedAndTheInsideClear() {
        float[] field = Sketch.filled(4, 1, false);
        field[3] = 1f;
        Sketch.Preview p = Sketch.preview(field, 4, 1, 4, OVER);
        assertEquals(Sketch.SHADE, p.rgba()[3] & 0xff, "the outside shaded");
        assertEquals(0, p.rgba()[15], "the inside clear");
    }

    @Test
    void aHardDabPaintsItsDiscWholeAndNothingPast() {
        float[] field = Sketch.filled(21, 21, false);
        Sketch.Rect touched = Sketch.dab(field, Sketch.Stroke.of(21, 21), 21, 21, new Messages.Brush(5, 1f, 1f, false), 10.5f, 10.5f);
        assertEquals(1f, field[10 * 21 + 10], "the middle");
        assertEquals(1f, field[10 * 21 + 14], "four texels out");
        assertEquals(0f, field[10 * 21 + 16], "past the rim");
        assertEquals(new Sketch.Rect(5, 5, 16, 16), touched);
    }

    @Test
    void aSoftDabFallsOffAndItsStrengthIsAShare() {
        float[] field = Sketch.filled(21, 21, false);
        Sketch.dab(field, Sketch.Stroke.of(21, 21), 21, 21, new Messages.Brush(8, 0.5f, 0f, false), 10.5f, 10.5f);
        float middle = field[10 * 21 + 10];
        float out = field[10 * 21 + 15];
        assertTrue(middle > 0.45f && middle <= 0.5f, "half way at the middle: " + middle);
        assertTrue(out > 0f && out < middle, "less toward the rim: " + out);
    }

    @Test
    void aStrokesDabsDoNotPileUpButASecondStrokeAddsOn() {
        float[] field = Sketch.filled(64, 8, false);
        Sketch.Stroke stroke = Sketch.Stroke.of(64, 8);
        Messages.Brush half = new Messages.Brush(3, 0.5f, 1f, false);
        Sketch.Rect first = Sketch.dab(field, stroke, 64, 8, half, 10.5f, 4.5f);
        first = first.union(Sketch.line(field, stroke, 64, 8, half, 10.5f, 4.5f, 12.5f, 4.5f));
        first = first.union(Sketch.line(field, stroke, 64, 8, half, 12.5f, 4.5f, 10.5f, 4.5f));
        assertEquals(0.5f, field[4 * 64 + 11], 1e-6, "one stroke at 50 % makes 50 %, back and forth");
        stroke.begin(64, first);
        Sketch.dab(field, stroke, 64, 8, half, 10.5f, 4.5f);
        assertEquals(0.75f, field[4 * 64 + 10], 1e-6, "a second goes half the rest of the way");
    }

    @Test
    void eraseMovesTowardTheOutside() {
        float[] field = Sketch.filled(9, 9, true);
        Sketch.dab(field, Sketch.Stroke.of(9, 9), 9, 9, new Messages.Brush(3, 1f, 1f, true), 4.5f, 4.5f);
        assertEquals(0f, field[4 * 9 + 4]);
        assertEquals(1f, field[0]);
    }

    @Test
    void aLineLeavesNoGapsAndADabOffTheFieldTouchesNothing() {
        float[] field = Sketch.filled(64, 8, false);
        Messages.Brush b = new Messages.Brush(2, 1f, 1f, false);
        Sketch.dab(field, Sketch.Stroke.of(64, 8), 64, 8, b, 2.5f, 4.5f);
        Sketch.line(field, Sketch.Stroke.of(64, 8), 64, 8, b, 2.5f, 4.5f, 60.5f, 4.5f);
        for (int x = 2; x <= 60; x++) {
            assertEquals(1f, field[4 * 64 + x], "texel " + x);
        }
        assertTrue(Sketch.dab(field, Sketch.Stroke.of(64, 8), 64, 8, b, -40f, -40f).empty());
    }

    @Test
    void repaintingWhatADabTouchedMatchesAWholePreview() {
        float[] field = Sketch.filled(300, 200, false);
        Sketch.Preview part = Sketch.preview(field, 300, 200, 128, GREY);
        Sketch.Rect touched = Sketch.dab(field, Sketch.Stroke.of(300, 200), 300, 200, new Messages.Brush(30, 0.7f, 0.3f, false), 120f, 90f);
        Sketch.paint(field, 300, 200, part, touched, GREY);
        assertArrayEquals(Sketch.preview(field, 300, 200, 128, GREY).rgba(), part.rgba());
    }

    @Test
    void aStencilStartedInsideCoversItsCanvas() {
        Sizing s = new Sizing(32, 16, Unit.PX, 300);
        Sdg sdg = Sketch.drawing(Sketch.filled(32, 16, true), 32, 16, s, EditMode.STENCIL);
        assertEquals(32.0, sdg.width());
        assertEquals(1, sdg.layers().size());
        assertEquals(255, alphaAt(SdgRender.render(sdg, 32, 16), 16, 8));
    }

    @Test
    void aStencilStartedOutsideCoversNothing() {
        Sizing s = new Sizing(32, 16, Unit.PX, 300);
        Sdg sdg = Sketch.drawing(Sketch.filled(32, 16, false), 32, 16, s, EditMode.HEIGHTMAP);
        assertEquals(0, alphaAt(SdgRender.render(sdg, 32, 16), 16, 8));
    }

    @Test
    void aMaskIsAnAlphaMaskOverInk() {
        Sizing s = new Sizing(32, 16, Unit.PX, 300);
        float[] field = Sketch.filled(32, 16, false);
        for (int y = 0; y < 16; y++) {
            for (int x = 16; x < 32; x++) {
                field[y * 32 + x] = 1f;
            }
        }
        Sdg sdg = Sketch.drawing(field, 32, 16, s, EditMode.MASK);
        assertTrue(sdg.layers().getFirst().isGroup());
        assertEquals(Sdg.Mask.ALPHA, sdg.layers().getFirst().group().mask().kind());
        Raster r = SdgRender.render(sdg, 32, 16);
        assertEquals(0, alphaAt(r, 4, 8), "masked away where the field is out");
        assertEquals(255, alphaAt(r, 28, 8), "ink where it is in");
    }

    @Test
    void aLengthIsSavedInItsUnitsWithItsScale() throws Exception {
        Sizing s = new Sizing(1, 0.5, Unit.IN, 64);
        Sdg sdg = Sketch.drawing(Sketch.filled(64, 32, true), 64, 32, s, EditMode.STENCIL);
        Path file = dir.resolve("inch.sdg");
        SdgFile.write(sdg, file);
        Sdg read = SdgFile.read(file);
        assertEquals(1.0, read.width());
        assertEquals(0.5, read.height());
        assertEquals(64.0, read.layers().getFirst().stencil().scale());
    }

    @Test
    void writingNeverGoesOverAFile() throws Exception {
        Path taken = dir.resolve("Untitled.sdg");
        Files.writeString(taken, "someone's");
        Sizing s = new Sizing(8, 8, Unit.PX, 300);
        Sdg sdg = Sketch.drawing(Sketch.filled(8, 8, true), 8, 8, s, EditMode.STENCIL);
        SdgWritten w = new WriteDrawing().run(new WriteSdg(taken, ImageSdg.of(sdg)));
        assertEquals(dir.resolve("Untitled (2).sdg"), w.file().orElseThrow());
        assertEquals("someone's", Files.readString(taken));
        assertEquals(1, SdgFile.read(w.file().get()).layers().size());
    }

    @Test
    void aNameTypedWithoutItsExtensionGetsOne() {
        assertEquals(dir.resolve("plan.sdg"), Sketcher.named(dir.resolve("plan")));
        assertEquals(dir.resolve("plan.SDG"), Sketcher.named(dir.resolve("plan.SDG")));
    }

    private static int alphaAt(Raster r, int x, int y) {
        return r.rgba()[(y * r.width() + x) * 4 + 3] & 0xff;
    }
}
