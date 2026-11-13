package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.Format;
import dev.sibarum.pix.core.Messages.WriteCopy;
import dev.sibarum.pix.core.Messages.Written;
import dev.vexelray.codec.image.Picture;
import dev.vexelray.codec.video.Probed;
import dev.vexelray.codec.video.VideoFile;
import dev.vexelray.codec.video.Videos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A picture that plays made a video: its frames, their times, and what shows through where it was transparent. */
class MotionVideoTest {

    @TempDir
    Path dir;

    /** 15 x 9, odd both ways: blue on the left, transparent on the right for 100 ms; then all green for 300 ms. */
    private Path spin() throws IOException {
        int w = 15;
        int h = 9;
        byte[] first = new byte[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                first[y * w + x] = (byte) (x < 7 ? 1 : 0);
            }
        }
        byte[] second = new byte[w * h];
        java.util.Arrays.fill(second, (byte) 2);
        return Files.write(dir.resolve("spin.gif"), Gif.of(w, h, new int[]{0x000000, 0x0000FF, 0x00FF00, 0xFFFFFF}, 0,
                List.of(first, second), new int[]{10, 30}));
    }

    @Test
    void aGifBecomesAVideoBesideItOnTheBackgroundChosen() throws IOException {
        Path gif = spin();
        Written w = new ConvertPicture().run(new WriteCopy(gif, Format.WEBM, 0xFF0000));
        assertEquals(dir.resolve("spin.webm"), w.file().orElseThrow(), w.why());
        try (var files = Files.list(dir)) {
            assertEquals(2, files.count(), "nothing but the picture and its video");
        }

        Probed probed = Videos.probe(w.file().get(), 0);
        VideoFile v = probed.video().orElseThrow(() -> new AssertionError(probed.identity()));
        assertEquals(15, v.width());
        assertEquals(9, v.height());
        assertFalse(v.sound());
        assertEquals(400, v.durationMicros() / 1000.0, 40, "every frame for as long as it is shown");

        Picture p = Videos.poster(w.file().get(), 0, true).picture();
        byte[] f = ReadPicture.firstFrame(p);
        assertNear(new int[]{0, 0, 255}, f, p.frameWidth(), 2, 4);
        assertNear(new int[]{255, 0, 0}, f, p.frameWidth(), 12, 4);
    }

    @Test
    void oneFrameMakesAVideoAsLongAsItIsShownAndNoPartIsLeft() throws IOException {
        Path gif = Files.write(dir.resolve("still.gif"), Gif.of(4, 4, new int[]{0, 0xFFFFFF}, -1,
                List.of(new byte[16]), new int[]{0}));
        Written w = new ConvertPicture().run(new WriteCopy(gif, Format.WEBM, 0));
        assertTrue(w.file().isPresent(), w.why());
        try (var files = Files.list(dir)) {
            assertTrue(files.noneMatch(x -> x.getFileName().toString().contains(".part")));
        }
    }

    @Test
    void transparentPixelsAreLaidOverTheBackgroundAsABrowserDrawsThem() {
        byte[] px = {(byte) 255, 0, 0, (byte) 255, 0, 0, (byte) 255, 0, 0, (byte) 255, 0, (byte) 128};
        MotionVideo.over(px, 0xFFFFFF);
        assertArrayEquals(new byte[]{(byte) 255, 0, 0, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255,
                (byte) 127, (byte) 255, (byte) 127, (byte) 255}, px);
        assertTrue(ReadPicture.transparent(new byte[]{1, 2, 3, (byte) 255, 1, 2, 3, (byte) 254}));
        assertFalse(ReadPicture.transparent(new byte[]{1, 2, 3, (byte) 255}));
    }

    private static void assertNear(int[] rgb, byte[] frame, int width, int x, int y) {
        int i = (y * width + x) * 4;
        for (int c = 0; c < 3; c++) {
            int got = frame[i + c] & 0xFF;
            assertEquals(rgb[c], got, 40, "channel " + c + " at " + x + "," + y);
        }
    }

    /**
     * The smallest GIF writer that will do for a test, since the stack writes none: each frame's indices sent as 8-bit
     * LZW codes with a clear code often enough that the code never grows, so no compression is needed to make one.
     */
    static final class Gif {

        private Gif() {
        }

        /**
         * A {@code w x h} GIF of {@code frames}, each a palette index per pixel, shown {@code delays} hundredths of a
         * second each; {@code transparent} the index that is transparent, or -1. Loops forever.
         */
        static byte[] of(int w, int h, int[] palette, int transparent, List<byte[]> frames, int[] delays) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.writeBytes("GIF89a".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            short16(out, w);
            short16(out, h);
            int bits = 1;
            while ((1 << bits) < palette.length) {
                bits++;
            }
            out.write(0x80 | 0x70 | (bits - 1));
            out.write(0);
            out.write(0);
            for (int i = 0; i < (1 << bits); i++) {
                int c = i < palette.length ? palette[i] : 0;
                out.write((c >> 16) & 0xFF);
                out.write((c >> 8) & 0xFF);
                out.write(c & 0xFF);
            }
            if (frames.size() > 1) {
                out.writeBytes(new byte[]{0x21, (byte) 0xFF, 0x0B});
                out.writeBytes("NETSCAPE2.0".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                out.writeBytes(new byte[]{3, 1, 0, 0, 0});
            }
            for (int f = 0; f < frames.size(); f++) {
                out.writeBytes(new byte[]{0x21, (byte) 0xF9, 4});
                out.write((1 << 2) | (transparent >= 0 ? 1 : 0));
                short16(out, delays[f]);
                out.write(Math.max(0, transparent));
                out.write(0);
                out.write(0x2C);
                short16(out, 0);
                short16(out, 0);
                short16(out, w);
                short16(out, h);
                out.write(0);
                out.write(7);
                ByteArrayOutputStream codes = new ByteArrayOutputStream();
                byte[] px = frames.get(f);
                for (int i = 0; i < px.length; i++) {
                    if (i % 100 == 0) {
                        codes.write(128);
                    }
                    codes.write(px[i]);
                }
                codes.write(129);
                byte[] data = codes.toByteArray();
                for (int at = 0; at < data.length; at += 255) {
                    int n = Math.min(255, data.length - at);
                    out.write(n);
                    out.write(data, at, n);
                }
                out.write(0);
            }
            out.write(0x3B);
            return out.toByteArray();
        }

        private static void short16(ByteArrayOutputStream out, int v) {
            out.write(v & 0xFF);
            out.write((v >> 8) & 0xFF);
        }
    }
}
