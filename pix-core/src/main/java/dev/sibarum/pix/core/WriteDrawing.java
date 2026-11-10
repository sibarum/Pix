package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.SdgWritten;
import dev.sibarum.pix.core.Messages.WriteSdg;
import dev.vexelray.fields.sdg.SdgFile;
import dev.vexelray.framework.api.Job;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Write a drawing made by hand where the person asked, on the offload lane (docs/sdg-editor.md). A name already taken is
 * never overwritten, even one the save dialog asked about: {@code <name> (2).sdg}, as everything Rasterware writes.
 * {@code SdgFile.write} writes beside and moves into place, so a drawing half written is never there to be opened.
 */
@Job
public final class WriteDrawing {

    public SdgWritten run(WriteSdg w) {
        Path file = null;
        try {
            file = Beside.free(w.file(), SdgFile.XML);
            SdgFile.write(w.drawing().source(), file);
            return new SdgWritten(Optional.of(file), "");
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            String name = file == null ? "the drawing" : String.valueOf(file.getFileName());
            return new SdgWritten(Optional.empty(), "Could not save " + name + ": " + e.getMessage());
        }
    }
}
