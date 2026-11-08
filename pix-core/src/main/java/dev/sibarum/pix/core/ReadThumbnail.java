package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.ReadThumb;
import dev.sibarum.pix.core.Messages.Thumbnailed;
import dev.vexelray.codec.image.Pictures;
import dev.vexelray.framework.api.Job;

/**
 * Read only a picture's thumbnail, for a filmstrip cell whose picture is not read whole: decoded at
 * {@link ReadPicture#THUMB}, which the codec scales to as it decodes, so a thumbnail costs a decode and not the pixels of
 * a picture shown. A file that is not a picture is a thumbnail with no feed.
 */
@Job
public final class ReadThumbnail {

    public Thumbnailed run(ReadThumb r) {
        return new Thumbnailed(ReadPicture.thumb(r.path(), Pictures.of(r.path(), ReadPicture.THUMB).picture()));
    }
}
