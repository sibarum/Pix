package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.WriteRoles;
import dev.sibarum.pix.core.Messages.Written;
import dev.vexelray.fields.sdg.Sdg;
import dev.vexelray.fields.sdg.SdgFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Roles written into a drawing in place: solids take theirs, a texture is left, the rest of the file is kept. */
class RoleDrawingTest {

    @TempDir
    Path dir;

    private static final String SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" width="40" height="20" viewBox="0 0 40 20">
              <rect width="40" height="20" fill="#24102a"/>
              <circle cx="28" cy="10" r="6" fill="#ffe8c4"/>
            </svg>
            """;

    @Test
    void eachSolidLayerTakesTheRoleAskedAndKeepsItsColour() throws IOException {
        Path svg = Files.writeString(dir.resolve("dusk.svg"), SVG);
        for (String name : List.of("dusk.sdg", "dusk.sdgb")) {
            Path drawing = dir.resolve(name);
            Files.deleteIfExists(drawing);
            Path made = new ConvertPicture().run(new Messages.WriteCopy(svg, Messages.Format.SDG)).file().orElseThrow();
            if (!made.equals(drawing)) {
                SdgFile.write(SdgFile.read(made), drawing);
                Files.delete(made);
            }
            Sdg before = SdgFile.read(drawing);
            Written w = new RoleDrawing().run(new WriteRoles(drawing, List.of("", "accent")));
            assertEquals(drawing, w.file().orElseThrow(), w.why());
            Sdg after = SdgFile.read(drawing);
            assertEquals(before.layers().size(), after.layers().size());
            Sdg.Paint.Solid sun = (Sdg.Paint.Solid) after.layers().get(1).paint();
            assertEquals("accent", sun.role(), name);
            assertEquals(((Sdg.Paint.Solid) before.layers().get(1).paint()).rgba(), sun.rgba());
            assertEquals("", ((Sdg.Paint.Solid) after.layers().get(0).paint()).role());
        }
    }
}
