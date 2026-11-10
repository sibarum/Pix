package dev.sibarum.pix;

/**
 * Where a picture sits on the stage, fitted: whole, at its own aspect, and never larger than its own pixels. Said in
 * percent of the stage, the one unit the layout takes that needs neither the density nor a pixel length.
 *
 * @param widthPercent  the picture's width over the stage's, in percent
 * @param heightPercent the same, down
 */
record Fit(float widthPercent, float heightPercent) {

    /** A picture of {@code width x height} pixels on a stage of {@code stageW x stageH} layout px. */
    static Fit of(int width, int height, float stageW, float stageH) {
        return of(width, height, stageW, stageH, false);
    }

    /**
     * The same, or with {@code fills} as large as the stage allows, past its own pixels: a video, which is watched at
     * the size of the window whatever size it was made at.
     */
    static Fit of(int width, int height, float stageW, float stageH, boolean fills) {
        if (width <= 0 || height <= 0 || !(stageW > 0f) || !(stageH > 0f)) {
            return new Fit(100f, 100f);
        }
        float scale = Math.min(stageW / width, stageH / height);
        if (!fills) {
            scale = Math.min(1f, scale);
        }
        return new Fit(width * scale / stageW * 100f, height * scale / stageH * 100f);
    }
}
