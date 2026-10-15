package dev.sibarum.pix;

import dev.sibarum.suite.pictures.Sizes;
import dev.vexelray.codec.image.Decoded;
import dev.vexelray.codec.image.Picture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A video is shown by its poster: one still frame, through the bundled FFmpeg. {@code rgb.mp4} is vexelray-codec-video's
 * fixture: three seconds of H.264 at 64x48, a second each of red, green and blue.
 */
class PosterTest {

    @Test
    void aVideoIsAStillFromItsFirstSecondAndSaysWhatItIs(@TempDir Path dir) throws IOException {
        Path clip = fixture(dir, "rgb.mp4");
        Decoded d = Previewer.decode(clip, true);
        assertTrue(d.image(), d.identity());
        assertEquals("H.264 video · 64 × 48 · 0:03", d.identity());
        Picture p = d.picture();
        assertEquals(64, p.frameWidth());
        assertEquals(48, p.frameHeight());
        assertFalse(p.moves(), "a poster, not playback");
        assertTrue(red(p), "a tenth of the way into three seconds is still the red second");
        assertTrue(Previewer.large(d), "a small video's one frame is already the viewer's size");
    }

    @Test
    void aFileNamedLikeAVideoThatIsNotOneSaysSo(@TempDir Path dir) throws IOException {
        Path fake = Files.writeString(dir.resolve("fake.mp4"), "not a video at all");
        Decoded d = Previewer.decode(fake, true);
        assertFalse(d.image());
        assertEquals("not a video", d.identity());
    }

    @Test
    void aThumbnailSmallerThanTheFileIsNotTheViewersSize() {
        Picture thumb = new Picture("k", new byte[4], 1, 1, 1, 1, new int[1], 4000, 3000, false);
        assertFalse(Previewer.large(new Decoded("k", "", List.of(thumb))));
        Picture whole = new Picture("k", new byte[4], 1, 1, 1, 1, new int[1], 1, 1, false);
        assertTrue(Previewer.large(new Decoded("k", "", List.of(whole))));
        Picture view = new Picture("k", new byte[Sizes.VIEW * 4], Sizes.VIEW, 1, 1, 1, new int[1], 4000, 3000, false);
        assertTrue(Previewer.large(new Decoded("k", "", List.of(view))),
                "a frame at the viewer's size is enough even of a larger file");
    }

    private static boolean red(Picture p) {
        byte[] s = p.sheet();
        int i = ((p.frameHeight() / 2) * p.frameWidth() + p.frameWidth() / 2) * 4;
        return (s[i] & 0xFF) > 200 && (s[i + 1] & 0xFF) < 60 && (s[i + 2] & 0xFF) < 60;
    }

    private static Path fixture(Path dir, String name) throws IOException {
        Path out = dir.resolve(name);
        try (InputStream in = PosterTest.class.getResourceAsStream("/" + name)) {
            assertNotNull(in, name);
            Files.copy(in, out);
        }
        return out;
    }
}
