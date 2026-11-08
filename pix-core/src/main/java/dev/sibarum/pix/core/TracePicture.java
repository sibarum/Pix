package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.Trace;
import dev.sibarum.pix.core.Messages.Traced;
import dev.vexelray.fields.art.Raster;
import dev.vexelray.fields.sdg.Sdg;
import dev.vexelray.fields.sdg.SdgFile;
import dev.vexelray.framework.api.Job;
import dev.vexelray.gui.core.ImageSdg;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Trace a picture into a drawing for the Trace panel to compare, on the offload lane: read at {@link Traces#SIDE}, cut
 * coarsely so a change of setting answers in under a second. What it would be as a file is measured by writing it as
 * the XML Save writes. A file that is not a raster, or a trace that fails, is an answer with no drawing and why, never a throw.
 */
@Job
public final class TracePicture {

    public Traced run(Trace t) {
        Raster raster = Traces.raster(t.path());
        if (raster == null) {
            return none(t, "Only a picture made of pixels can be traced");
        }
        try {
            Sdg sdg = Traces.trace(raster, t.settings(), false);
            return new Traced(t.path(), t.settings(), Optional.of(ImageSdg.of(sdg)), sdg.layers().size(),
                    SdgFile.xml(sdg).getBytes(StandardCharsets.UTF_8).length, Traces.palette(sdg), "");
        } catch (RuntimeException | OutOfMemoryError e) {
            return none(t, "Could not trace it: " + e.getMessage());
        }
    }

    private static Traced none(Trace t, String why) {
        return new Traced(t.path(), t.settings(), Optional.empty(), 0, 0, List.of(), why);
    }
}
