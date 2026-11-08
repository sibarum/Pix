package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.Trace;
import dev.sibarum.pix.core.Messages.TraceSettings;
import dev.sibarum.pix.core.Messages.TraceWritten;
import dev.sibarum.pix.core.Messages.Traced;
import dev.sibarum.pix.core.Messages.WriteTrace;
import dev.vexelray.fields.sdg.Png;
import dev.vexelray.fields.sdg.SdgFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The two trace jobs on a real file: a small picture of two colours, traced and written beside itself. */
class TraceJobsTest {

    @TempDir
    Path dir;

    /** 64 × 48: dark on the left, a light disc on the right. */
    private Path picture() throws IOException {
        int w = 64, h = 48;
        byte[] rgba = new byte[w * h * 4];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean disc = (x - 44) * (x - 44) + (y - 24) * (y - 24) < 12 * 12;
                int i = (y * w + x) * 4;
                rgba[i] = (byte) (disc ? 0xff : 0x24);
                rgba[i + 1] = (byte) (disc ? 0xe8 : 0x10);
                rgba[i + 2] = (byte) (disc ? 0xc4 : 0x2a);
                rgba[i + 3] = (byte) 0xff;
            }
        }
        Path p = dir.resolve("dusk.png");
        Files.write(p, Png.write(w, h, rgba));
        return p;
    }

    @Test
    void aPictureIsTracedIntoADrawingWithItsColours() throws IOException {
        Traced t = new TracePicture().run(new Trace(picture(), new TraceSettings(2, false)));
        assertTrue(t.drawing().isPresent(), t.why());
        assertEquals(2, t.layers());
        assertEquals(2, t.palette().size());
        assertTrue(t.bytes() > 0);
    }

    @Test
    void aFileThatIsNotAPictureSaysWhy() throws IOException {
        Path text = Files.writeString(dir.resolve("notes.png"), "not a picture");
        Traced t = new TracePicture().run(new Trace(text, TraceSettings.FIRST));
        assertTrue(t.drawing().isEmpty());
        assertFalse(t.why().isEmpty());
    }

    @Test
    void aSaveIsWrittenBesideThePictureAndNeverOverAnother() throws IOException {
        Path p = picture();
        TraceWritten first = new SaveDrawing().run(new WriteTrace(p, new TraceSettings(2, true)));
        assertEquals(dir.resolve("dusk.sdg"), first.file().orElseThrow(), first.why());
        assertEquals(2, SdgFile.read(first.file().get()).layers().size());
        TraceWritten second = new SaveDrawing().run(new WriteTrace(p, new TraceSettings(2, true)));
        assertEquals(dir.resolve("dusk (2).sdg"), second.file().orElseThrow());
    }
}
