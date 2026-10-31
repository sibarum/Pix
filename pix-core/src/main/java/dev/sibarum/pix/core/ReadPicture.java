package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.Pictured;
import dev.sibarum.pix.core.Messages.Read;
import dev.vexelray.codec.image.Decoded;
import dev.vexelray.codec.image.Picture;
import dev.vexelray.codec.image.Pictures;
import dev.vexelray.framework.api.Job;
import dev.vexelray.gui.core.Feed;
import dev.vexelray.vulkan.present.MipChain;

import java.util.Optional;

/**
 * Read a picture and put it in a feed, on the offload lane: the decode, the mip chain and the copy into the feed's slot
 * all happen here, and the answer is the feed, a handle. The pixels stay where they were written, and the renderer
 * of whichever window shows the feed uploads them (vexelray-gui's frame-contract.md D).
 *
 * <p>Decoded at most {@link #SIDE} px on its longer side, which is what is drawn while the picture moves. A file that is
 * not a picture, or will not decode, is an answer with no feed and the reason, never a throw.
 */
@Job
public final class ReadPicture {

    /** The longest side a picture is shown from. */
    static final int SIDE = 2560;

    public Pictured run(Read r) {
        Decoded d = Pictures.of(r.path(), SIDE);
        Picture p = d.picture();
        if (p == null) {
            return new Pictured(r.path(), Optional.empty(), 0, 0, d.identity());
        }
        int w = p.frameWidth();
        int h = p.frameHeight();
        byte[][] chain = MipChain.of(firstFrame(p), w, h);
        return new Pictured(r.path(), Optional.of(Feed.still(w, h, true, chain)), w, h, d.identity());
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
