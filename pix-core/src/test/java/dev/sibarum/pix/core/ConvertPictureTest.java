package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.Format;
import dev.sibarum.pix.core.Messages.WriteCopy;
import dev.sibarum.pix.core.Messages.Written;
import dev.vexelray.codec.image.Picture;
import dev.vexelray.codec.image.Pictures;
import dev.vexelray.fields.sdg.Png;
import dev.vexelray.fields.sdg.SdgFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The conversion job on real files: a PNG re-saved, an SVG made a drawing and back, a raster refused as a drawing. */
class ConvertPictureTest {

    @TempDir
    Path dir;

    private static final String SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" width="40" height="20" viewBox="0 0 40 20">
              <rect width="40" height="20" fill="#24102a"/>
              <circle cx="28" cy="10" r="6" fill="#ffe8c4"/>
            </svg>
            """;

    private Path png() throws IOException {
        byte[] rgba = new byte[8 * 4 * 4];
        for (int i = 0; i < rgba.length; i += 4) {
            rgba[i] = (byte) i;
            rgba[i + 3] = (byte) 0xff;
        }
        return Files.write(dir.resolve("strip.png"), Png.write(8, 4, rgba));
    }

    @Test
    void aPngResavedKeepsEveryPixelBesideTheOriginal() throws IOException {
        Path p = png();
        Written w = new ConvertPicture().run(new WriteCopy(p, Format.PNG));
        assertEquals(dir.resolve("strip (2).png"), w.file().orElseThrow(), w.why());
        Picture a = Pictures.of(p).picture();
        Picture b = Pictures.of(w.file().get()).picture();
        assertArrayEquals(a.sheet(), b.sheet());
    }

    @Test
    void anSvgBecomesADrawingByItsPathsAndTheDrawingBecomesAPng() throws IOException {
        Path svg = Files.writeString(dir.resolve("dusk.svg"), SVG);
        Written drawing = new ConvertPicture().run(new WriteCopy(svg, Format.SDG));
        assertEquals(dir.resolve("dusk.sdg"), drawing.file().orElseThrow(), drawing.why());
        assertEquals(2, SdgFile.read(drawing.file().get()).layers().size());
        Written png = new ConvertPicture().run(new WriteCopy(drawing.file().get(), Format.PNG));
        Picture p = Pictures.of(png.file().orElseThrow()).picture();
        assertEquals(40, p.frameWidth());
        assertEquals(20, p.frameHeight());
    }

    @Test
    void aRasterIsNotMadeADrawingHereAndNothingIsLeftBehind() throws IOException {
        Path p = png();
        Written w = new ConvertPicture().run(new WriteCopy(p, Format.SDG));
        assertTrue(w.file().isEmpty());
        assertTrue(w.why().contains("Trace"), w.why());
        try (var files = Files.list(dir)) {
            assertEquals(1, files.count());
        }
        assertFalse(Files.exists(dir.resolve("strip.sdg")));
    }
}
