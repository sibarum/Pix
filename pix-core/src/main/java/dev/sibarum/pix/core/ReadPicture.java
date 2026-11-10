package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.Pictured;
import dev.sibarum.pix.core.Messages.Plays;
import dev.sibarum.pix.core.Messages.Read;
import dev.sibarum.pix.core.Messages.Thumb;
import dev.vexelray.codec.image.Decoded;
import dev.vexelray.codec.image.Motion;
import dev.vexelray.codec.image.Picture;
import dev.vexelray.codec.image.Pictures;
import dev.vexelray.codec.image.gui.Player;
import dev.vexelray.codec.video.Videos;
import dev.vexelray.framework.api.Job;
import dev.vexelray.gui.core.Feed;
import dev.vexelray.vulkan.present.MipChain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Read a picture and put it in a feed, on the offload lane: the decode, the mip chain and the copy into the feed's slot
 * all happen here, and the answer is the feed, a handle. The pixels stay where they were written, and the renderer
 * of whichever window shows the feed uploads them (vexelray-gui's frame-contract.md D).
 *
 * <p>Decoded at most {@link #SIDE} px on its longer side, which is what is drawn while the picture moves, and at most
 * {@link #THUMB} for the filmstrip, both from the one decode, which is the expensive part. A file that is not a picture,
 * or will not decode, is an answer with no feed and the reason, never a throw.
 */
@Job
public final class ReadPicture {

    /** The longest side a picture is shown from. */
    static final int SIDE = 2560;

    /** The longest side of a thumbnail: a filmstrip cell a few em tall, at twice the density, with room to spare. */
    static final int THUMB = 192;

    public Pictured run(Read r) {
        if (Videos.named(r.path())) {
            return video(r.path());
        }
        Decoded d = Pictures.of(r.path(), SIDE, THUMB);
        Picture p = d.picture();
        if (p == null) {
            return new Pictured(r.path(), Optional.empty(), 0, 0, d.identity(), bytes(r.path()), none(r.path()));
        }
        if (p.drawing() != null) {
            // A drawing is sized as an SVG decoded at SIDE would be: its own aspect, its longer side SIDE. Its feed is the
            // poster, and what is drawn is the drawing.
            double dw = p.drawing().drawing().width();
            double dh = p.drawing().drawing().height();
            double k = SIDE / Math.max(dw, dh);
            return new Pictured(r.path(), Optional.of(feed(p)), (int) Math.round(dw * k), (int) Math.round(dh * k),
                    d.identity(), bytes(r.path()), thumb(r.path(), d.picture(1)), Optional.of(p.drawing()),
                    modified(r.path()), Palette.of(d.picture(1)), Optional.empty());
        }
        Motion m = p.motion();
        if (m != null && m.source() == Motion.ANIMATION && m.frames() > 1) {
            // An animation: its first frame in a feed with room for the rest, which the view's player makes there.
            return new Pictured(r.path(), Optional.of(Player.feed(p.frameWidth(), p.frameHeight(), firstFrame(p))),
                    p.frameWidth(), p.frameHeight(), d.identity(), bytes(r.path()), thumb(r.path(), d.picture(1)),
                    Optional.empty(), modified(r.path()), Palette.of(d.picture(1)),
                    Optional.of(new Plays(m.stamp().size(), m.stamp().modified(), m.frames(), m.durationMillis(),
                            m.loops(), false)));
        }
        return new Pictured(r.path(), Optional.of(feed(p)), p.frameWidth(), p.frameHeight(), d.identity(),
                bytes(r.path()), thumb(r.path(), d.picture(1)), Optional.empty(), modified(r.path()),
                Palette.of(d.picture(1)), Optional.empty());
    }

    /**
     * A video: its first frame at most {@link #SIDE}, in a feed with room for the rest, which the view's player makes
     * there with the video's sound; and its poster, a tenth of the way in, as its thumbnail. Two opens of the file, since
     * the first frame that plays and the frame that stands for the video are not the same. A video that does not say how
     * long it is or how fast it plays is shown by its first frame, still.
     */
    static Pictured video(Path path) {
        Decoded d = Videos.poster(path, SIDE, true);
        Picture p = d.picture();
        if (p == null) {
            return new Pictured(path, Optional.empty(), 0, 0, d.identity(), bytes(path), none(path));
        }
        Picture small = Videos.poster(path, THUMB).picture();
        Motion m = p.motion();
        if (m == null) {
            return new Pictured(path, Optional.of(feed(p)), p.frameWidth(), p.frameHeight(), d.identity(), bytes(path),
                    thumb(path, small), Optional.empty(), modified(path), Palette.of(small), Optional.empty());
        }
        return new Pictured(path, Optional.of(Player.feed(p.frameWidth(), p.frameHeight(), firstFrame(p))),
                p.frameWidth(), p.frameHeight(), d.identity(), bytes(path), thumb(path, small), Optional.empty(),
                modified(path), Palette.of(small), Optional.of(new Plays(m.stamp().size(), m.stamp().modified(),
                m.frames(), m.durationMillis(), 0, true)));
    }

    /** The thumbnail of {@code path} from {@code p}, decoded at {@link #THUMB}. */
    static Thumb thumb(Path path, Picture p) {
        return p == null ? none(path) : new Thumb(path, Optional.of(feed(p)), p.frameWidth(), p.frameHeight());
    }

    static Thumb none(Path path) {
        return new Thumb(path, Optional.empty(), 0, 0);
    }

    /** {@code p}'s first frame in a still feed, with its mip chain. */
    static Feed feed(Picture p) {
        int w = p.frameWidth();
        int h = p.frameHeight();
        return Feed.still(w, h, true, MipChain.of(firstFrame(p), w, h));
    }

    /** The file's last-modified time, ms since the epoch, or -1 when it cannot be asked. */
    static long modified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException | SecurityException e) {
            return -1;
        }
    }

    /** The file's size on disk, or -1 when it cannot be asked: a picture is still shown without it. */
    static long bytes(Path path) {
        try {
            return Files.size(path);
        } catch (IOException | SecurityException e) {
            return -1;
        }
    }

    /** The sheet's first cell: the whole sheet for a still, the first frame of an animation packed into one. */
    static byte[] firstFrame(Picture p) {
        int w = p.frameWidth();
        int h = p.frameHeight();
        int sheetW = p.sheetWidth();
        if (sheetW == w && p.sheetHeight() == h) {
            return p.sheet();
        }
        byte[] out = new byte[w * h * 4];
        for (int y = 0; y < h; y++) {
            System.arraycopy(p.sheet(), y * sheetW * 4, out, y * w * 4, w * 4);
        }
        return out;
    }
}
