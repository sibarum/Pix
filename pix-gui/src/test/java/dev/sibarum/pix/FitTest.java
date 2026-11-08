package dev.sibarum.pix;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A picture fitted to the stage: whole, at its own aspect, and never past its own pixels. */
class FitTest {

    @Test
    void aWidePictureFillsTheStagesWidth() {
        Fit f = Fit.of(2000, 1000, 1000, 800);
        assertEquals(100f, f.widthPercent(), 1e-4);
        assertEquals(500f / 800f * 100f, f.heightPercent(), 1e-4);
    }

    @Test
    void aTallPictureFillsTheStagesHeight() {
        Fit f = Fit.of(1000, 4000, 1000, 800);
        assertEquals(100f, f.heightPercent(), 1e-4);
        assertEquals(200f / 1000f * 100f, f.widthPercent(), 1e-4);
    }

    @Test
    void aSmallPictureIsNotEnlarged() {
        Fit f = Fit.of(100, 50, 1000, 500);
        assertEquals(10f, f.widthPercent(), 1e-4);
        assertEquals(10f, f.heightPercent(), 1e-4);
    }

    @Test
    void noStageYetFillsWhateverItIsGiven() {
        assertEquals(new Fit(100f, 100f), Fit.of(100, 50, 0, 0));
    }
}
