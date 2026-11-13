package dev.sibarum.pix.core;

import dev.vexelray.codec.image.Movie;
import sibarum.ffmpeg.record.Recording;
import sibarum.imagelib.Animation;
import sibarum.imagelib.Imagelib;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A picture that plays (an animated GIF, WebP or APNG) written as a video: every frame at the picture's own size, each
 * for as long as Rasterware shows it, encoded to AV1 in the container {@code to}'s name asks for (docs/convert.md).
 *
 * <p>Frames come from imagelib one at a time ({@link Animation}), each composited over the one before, so however long
 * the picture only one canvas is held. A video has no transparency, so each frame is laid over {@code background}
 * first. The file plays once, silent: a video cannot ask to loop, and a picture has no sound.
 *
 * <p>Blocking: it decodes and encodes every frame, so it runs in a job. Throws on what goes wrong; the job says why.
 */
final class MotionVideo {

    /** AV1's quantiser for a picture made a video: finer than the recorder's text-sharp 30, since the frames are few. */
    static final int QUALITY = 24;

    private MotionVideo() {
    }

    /** Write {@code picture}'s frames to {@code to} on {@code background} ({@code 0xRRGGBB}). */
    static void write(Path picture, Path to, int background) throws IOException {
        byte[] bytes = Files.readAllBytes(picture);
        try (Animation a = Imagelib.animate(bytes)) {
            if (a.count() == 0) {
                throw new IllegalArgumentException("it has no frames");
            }
            long ms = 0;
            for (int i = 0; i < a.count(); i++) {
                ms += Movie.timed(a.delayMillis(i));
            }
            double rate = Math.min(1000, Math.max(1, a.count() * 1000.0 / Math.max(1, ms)));
            Recording.Settings settings = new Recording.Settings(rate, QUALITY, Recording.Encoder.SOFTWARE);
            try (Recording rec = Recording.create(to, a.width(), a.height(), settings)) {
                long at = 0;
                while (a.next()) {
                    byte[] rgba = a.rgba();
                    over(rgba, background);
                    rec.frame(rgba, a.width(), a.height(), at * 1000);
                    at += Movie.timed(a.delayMillis(a.index()));
                }
                if (rec.frames() == 0) {
                    throw new IllegalArgumentException("none of its frames could be decoded");
                }
                // The last frame held for as long as it is shown, rather than the file ending as it appears.
                rec.repeat(at * 1000);
            }
        }
    }

    /** {@code rgba} (straight RGBA8, sRGB) laid over the opaque {@code background}, in place, as a browser draws it. */
    static void over(byte[] rgba, int background) {
        int br = (background >> 16) & 0xFF;
        int bg = (background >> 8) & 0xFF;
        int bb = background & 0xFF;
        for (int i = 0; i < rgba.length; i += 4) {
            int a = rgba[i + 3] & 0xFF;
            if (a == 255) {
                continue;
            }
            int k = 255 - a;
            rgba[i] = (byte) (((rgba[i] & 0xFF) * a + br * k + 127) / 255);
            rgba[i + 1] = (byte) (((rgba[i + 1] & 0xFF) * a + bg * k + 127) / 255);
            rgba[i + 2] = (byte) (((rgba[i + 2] & 0xFF) * a + bb * k + 127) / 255);
            rgba[i + 3] = (byte) 0xFF;
        }
    }
}
