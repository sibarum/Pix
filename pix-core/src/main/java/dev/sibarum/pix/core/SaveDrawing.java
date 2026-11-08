package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.TraceWritten;
import dev.sibarum.pix.core.Messages.WriteTrace;
import dev.vexelray.fields.art.Raster;
import dev.vexelray.fields.sdg.Sdg;
import dev.vexelray.fields.sdg.SdgFile;
import dev.vexelray.framework.api.Job;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Trace a picture at full quality and write it beside the picture as {@code <name>.sdg}, on the offload lane. A name
 * already taken is never overwritten: {@code <name> (2).sdg}, and on. {@code SdgFile.write} writes it beside and moves
 * it into place, so a drawing half written is never there to be opened. What goes wrong is an answer with no file and
 * why.
 */
@Job
public final class SaveDrawing {

    public TraceWritten run(WriteTrace w) {
        Raster raster = Traces.raster(w.path());
        if (raster == null) {
            return new TraceWritten(w.path(), Optional.empty(), "Only a picture made of pixels can be traced");
        }
        Path file = null;
        try {
            Sdg sdg = Traces.trace(raster, w.settings(), true);
            file = free(w.path());
            SdgFile.write(sdg, file);
            return new TraceWritten(w.path(), Optional.of(file), "");
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            String name = file == null ? "the drawing" : String.valueOf(file.getFileName());
            return new TraceWritten(w.path(), Optional.empty(), "Could not save " + name + ": " + e.getMessage());
        }
    }

    /** {@code picture}'s name with {@code .sdg} for its extension, or the first {@code (n)} of it not taken. */
    static Path free(Path picture) {
        String name = String.valueOf(picture.getFileName());
        int dot = name.lastIndexOf('.');
        String stem = dot > 0 ? name.substring(0, dot) : name;
        Path file = picture.resolveSibling(stem + "." + SdgFile.XML);
        for (int n = 2; Files.exists(file); n++) {
            file = picture.resolveSibling(stem + " (" + n + ")." + SdgFile.XML);
        }
        return file;
    }
}
