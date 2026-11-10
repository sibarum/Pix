package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.Brush;
import dev.sibarum.pix.core.Messages.CreateSdg;
import dev.sibarum.pix.core.Messages.Dab;
import dev.sibarum.pix.core.Messages.EditMode;
import dev.sibarum.pix.core.Messages.Lift;
import dev.sibarum.pix.core.Messages.Sizing;
import dev.sibarum.pix.core.Messages.Sketched;
import dev.sibarum.pix.core.Messages.Unit;
import dev.vexelray.framework.core.PickFile;
import dev.vexelray.framework.core.Timer;
import dev.vexelray.gui.core.Feed;
import org.junit.jupiter.api.Test;
import sibarum.atchung.Atchung;
import sibarum.atchung.Sender;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A stencil kept a true distance, and a brush that snaps to the picture under it (docs/sdg-editor.md, slice 3). */
class StencilTest {

    private static final int N = 64;

    /** A disc of {@code brush} at the middle of an empty N × N field, settled. */
    private static float[] disc(Brush brush) {
        float[] field = Sketch.filled(N, N, false);
        Sketch.Rect touched = Sketch.dab(field, Sketch.Stroke.of(N, N), N, N, brush, 32f, 32f);
        Sketch.redistance(field, N, N, touched);
        return field;
    }

    /** The distance in texels a field's value says, inside positive. */
    private static float distance(float e) {
        return (e - 0.5f) * (float) Sketch.RANGE;
    }

    @Test
    void aHardDiscBecomesItsTrueDistance() {
        float[] field = disc(new Brush(20, 1f, 1f, false));
        int checked = 0;
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                double truth = 20 - Math.hypot(x + 0.5 - 32, y + 0.5 - 32);
                if (Math.abs(truth) < 3) {
                    assertEquals(truth, distance(field[y * N + x]), 0.75, "at " + x + "," + y);
                    checked++;
                }
            }
        }
        assertTrue(checked > 100, "the edge was checked all round: " + checked);
        assertEquals(1f, field[32 * N + 32], "far inside");
        assertEquals(0f, field[0], "far outside");
    }

    @Test
    void aSoftRampBecomesADistanceAboutWhereItCrossedAHalf() {
        // A soft dab at full strength crosses ½ where its falloff does: half the radius out.
        float[] field = disc(new Brush(20, 1f, 0f, false));
        for (int k = -2; k <= 2; k++) {
            int x = 32 + 10 + k;
            double truth = 10 - (x + 0.5 - 32);
            assertEquals(truth, distance(field[32 * N + x]), 0.6, "at " + x);
        }
    }

    @Test
    void reDistancingADistanceChangesNothingMuch() {
        float[] once = disc(new Brush(17, 1f, 0.4f, false));
        float[] twice = once.clone();
        Sketch.redistance(twice, N, N, new Sketch.Rect(0, 0, N, N));
        for (int i = 0; i < once.length; i++) {
            assertEquals(once[i], twice[i], 0.03f, "texel " + i);
        }
    }

    @Test
    void aStencilsEdgeIsDrawnSharpAtThePreviewsSize() {
        float[] field = new float[]{0.5f - 2f / 8f, 0.5f - 0.0625f, 0.5f + 0.0625f, 0.5f + 2f / 8f};
        Sketch.Preview p = Sketch.preview(field, 4, 1, 4, new Sketch.Look(false, true));
        assertEquals(0, p.rgba()[0] & 0xff, "two texels out: none");
        assertEquals(255, p.rgba()[12] & 0xff, "two texels in: whole");
        assertEquals(0, p.rgba()[4] & 0xff, "half a texel out: none, at a texel a pixel");
        assertEquals(255, p.rgba()[8] & 0xff, "half a texel in: whole");
    }

    @Test
    void aPictureLetterboxedInTheFieldIsReadWhereItIs() {
        byte[] rgba = new byte[2 * 1 * 4];
        rgba[0] = (byte) 255;           // left pixel red
        rgba[6] = (byte) 255;           // right pixel blue
        Sketch.Under under = new Sketch.Under(rgba, 2, 1, 4, 4);
        assertEquals(-1, under.at(1f, 0.5f), "above the picture");
        assertEquals(0xff0000, under.at(1f, 2f));
        assertEquals(0x0000ff, under.at(3f, 2f));
        assertEquals(-1, under.at(1f, 3.5f), "below it");
    }

    @Test
    void aSnappingBrushStopsAtThePicturesEdge() {
        int w = 64;
        int h = 32;
        byte[] rgba = new byte[w * h * 4];
        for (int i = 0; i < w * h; i++) {
            int v = i % w < 32 ? 30 : 230;   // dark left, light right
            rgba[i * 4] = (byte) v;
            rgba[i * 4 + 1] = (byte) v;
            rgba[i * 4 + 2] = (byte) v;
            rgba[i * 4 + 3] = (byte) 255;
        }
        Sketch.Under under = new Sketch.Under(rgba, w, h, w, h);
        float[] field = Sketch.filled(w, h, false);
        Sketch.dab(field, Sketch.Stroke.of(w, h), w, h, new Brush(28, 1f, 1f, false, 1f), 20f, 16f, under);
        assertEquals(1f, field[16 * w + 31], "up to the edge");
        assertEquals(0f, field[16 * w + 32], "and not past it");
        float[] loose = Sketch.filled(w, h, false);
        Sketch.dab(loose, Sketch.Stroke.of(w, h), w, h, new Brush(28, 1f, 1f, false, 0f), 20f, 16f, under);
        assertEquals(1f, loose[16 * w + 40], "with no snap, past it");
    }

    @Test
    void alikeIsWholeForTheSameColourAndNothingForAFarOne() {
        float tight = Sketch.tolerance(1f);
        assertEquals(1f, Sketch.alike(0x808080, 0x808080, tight));
        assertEquals(0f, Sketch.alike(0xffffff, 0x000000, tight));
        assertEquals(1f, Sketch.alike(0xffffff, 0x000000, 2f), "a tolerance past the cube lets all through");
    }

    @Test
    void liftingAStencilSettlesItAndLiftingAMaskDoesNot() {
        assertEquals(2, lifted(EditMode.STENCIL), "the stroke, then its re-distanced edge");
        assertEquals(1, lifted(EditMode.MASK), "the stroke alone");
    }

    /** The stamp of the newest preview after a soft dab and a lift in {@code mode}. */
    private static long lifted(EditMode mode) {
        List<Sketched> sketched = new ArrayList<>();
        Sketcher s = new Sketcher(Sender.collecting(Messages.SKETCHED_TOPIC, sketched),
                Sender.collecting(PickFile.TOPIC, new ArrayList<>()),
                Sender.collecting(Messages.WRITE_SDG_TOPIC, new ArrayList<>()),
                Sender.collecting(Messages.SDG_WRITTEN_TOPIC, new ArrayList<>()),
                Sender.collecting(Messages.READ_BACKDROP_TOPIC, new ArrayList<>()), Timer.silent(Atchung.create()));
        s.create(new CreateSdg(new Sizing(N, N, Unit.PX, 300), false, mode));
        Feed feed = sketched.getFirst().feed();
        s.dab(new Dab(new Brush(12, 1f, 0f, false), 32f, 32f, true));
        s.lift(new Lift());
        return feed.stamp(feed.newest());
    }
}
