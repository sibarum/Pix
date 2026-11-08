package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.TraceSettings;
import dev.vexelray.codec.image.Picture;
import dev.vexelray.codec.image.Pictures;
import dev.vexelray.fields.art.Raster;
import dev.vexelray.fields.convert.Convert;
import dev.vexelray.fields.sdg.Sdg;
import dev.vexelray.fields.sdg.Trace;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * What both trace jobs share: the picture read small enough to trace, and traced by vexelray-fields'
 * {@code Convert.rasterToSdg} (docs/trace.md, which has the measurements behind these numbers).
 */
final class Traces {

    /** The longest side a picture is traced at: under half a second for a preview, about five at full quality. */
    static final int SIDE = 512;

    /** Fine pixels a texel where regions are cut: coarse for the panel's preview, the tracer's own default for a save. */
    static final int PREVIEW_OVERSAMPLE = 2;
    static final int FULL_OVERSAMPLE = Trace.Options.DEFAULT.oversample();

    /** Below this RMS variation a region is one colour; above it, a texture. "Flattened" makes every region one colour. */
    static final double KEEP_TEXTURE = Trace.Options.DEFAULT.flatness();
    static final double FLATTEN = 1e9;

    private Traces() {
    }

    /** The tracer's options for {@code settings}, at a preview's quality or a save's. */
    static Trace.Options options(TraceSettings settings, boolean full) {
        Trace.Options d = Trace.Options.DEFAULT;
        return new Trace.Options(settings.colours(), d.texels(), d.spread(), d.under(),
                full ? FULL_OVERSAMPLE : PREVIEW_OVERSAMPLE, settings.texture() ? KEEP_TEXTURE : FLATTEN);
    }

    /** The picture at {@code path}, at most {@link #SIDE}: its first frame if it plays. Null when it is not a raster. */
    static Raster raster(Path path) {
        Picture p = Pictures.of(path, SIDE).picture();
        if (p == null || p.drawing() != null) {
            return null;
        }
        return new Raster(p.frameWidth(), p.frameHeight(), ReadPicture.firstFrame(p));
    }

    /** {@code raster} traced. */
    static Sdg trace(Raster raster, TraceSettings settings, boolean full) {
        return Convert.rasterToSdg(options(settings, full)).apply(raster);
    }

    /**
     * Each layer's colour, bottom first, straight {@code 0xRRGGBBAA}: a solid's own, or the mean of what a texture paints,
     * where it paints anything.
     */
    static List<Integer> palette(Sdg sdg) {
        List<Integer> out = new ArrayList<>();
        for (Sdg.Layer l : sdg.layers()) {
            if (l.paint() instanceof Sdg.Paint.Solid s) {
                out.add(s.rgba());
            } else if (l.paint() instanceof Sdg.Paint.Sampled s) {
                out.add(mean(sdg.pages().get(s.page()), s.x(), s.y(), s.width(), s.height()));
            }
        }
        return out;
    }

    private static int mean(Sdg.Page page, int x0, int y0, int w, int h) {
        long r = 0, g = 0, b = 0, n = 0;
        byte[] px = page.rgba();
        for (int y = y0; y < y0 + h && y < page.height(); y++) {
            for (int x = x0; x < x0 + w && x < page.width(); x++) {
                int i = (y * page.width() + x) * 4;
                if ((px[i + 3] & 0xff) > 0) {
                    r += px[i] & 0xff;
                    g += px[i + 1] & 0xff;
                    b += px[i + 2] & 0xff;
                    n++;
                }
            }
        }
        if (n == 0) {
            return 0;
        }
        return (int) (r / n) << 24 | (int) (g / n) << 16 | (int) (b / n) << 8 | 0xff;
    }
}
