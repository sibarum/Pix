package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.Backdrop;
import dev.sibarum.pix.core.Messages.ReadBackdrop;
import dev.vexelray.codec.image.Decoded;
import dev.vexelray.codec.image.Picture;
import dev.vexelray.codec.image.Pictures;
import dev.vexelray.framework.api.Job;

import java.util.Optional;

/**
 * Read a picture to trace over into a feed, on the offload lane (docs/sdg-editor.md): decoded at most
 * {@link ReadPicture#SIDE} on its longer side, as a picture to look at is, its first frame if it plays. Answered on its
 * own topic rather than as a {@code Pictured}, which the viewer would take for a picture of its folder. A file that is
 * not a picture is an answer with no feed and why.
 */
@Job
public final class ReadBackdropPicture {

    public Backdrop run(ReadBackdrop r) {
        Decoded d = Pictures.of(r.path(), ReadPicture.SIDE, ReadPicture.THUMB);
        Picture p = d.picture();
        if (p == null) {
            return new Backdrop(r.path(), Optional.empty(), 0, 0, d.identity());
        }
        return new Backdrop(r.path(), Optional.of(ReadPicture.feed(p)), p.frameWidth(), p.frameHeight(), "");
    }
}
