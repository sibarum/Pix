package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.Format;
import dev.sibarum.pix.core.Messages.WriteCopy;
import dev.sibarum.pix.core.Messages.Written;
import dev.vexelray.codec.image.Picture;
import dev.vexelray.codec.image.Pictures;
import dev.vexelray.fields.art.Raster;
import dev.vexelray.fields.art.Svg;
import dev.vexelray.fields.sdg.Png;
import dev.vexelray.fields.sdg.Sdg;
import dev.vexelray.fields.sdg.SdgFile;
import dev.vexelray.framework.api.Job;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Write a picture beside itself in another format, on the offload lane (docs/convert.md):
 *
 * <ul>
 *   <li><b>to PNG</b>, a raster at its own size, an SVG as the decoder draws it, a drawing rendered at its own size by
 *       {@code Convert.sdgToRaster};</li>
 *   <li><b>to SDG</b>, an SVG by its paths ({@code Convert.svgToSdg}), never traced. A raster becomes a drawing by the
 *       Trace panel, which has the settings a trace needs, so one asked for here is refused, and why.</li>
 *   <li><b>to a video</b>, a picture that plays, every frame on the background asked for ({@link MotionVideo}).</li>
 * </ul>
 *
 * A name taken is never written over ({@link Beside}). What goes wrong is an answer with no file and why.
 */
@Job
public final class ConvertPicture {

    /** The longer side, in texels, an SVG's drawing is made at: sharp to well past a screen, in a few hundred KB. */
    static final int SVG_TEXELS = 1024;

    public Written run(WriteCopy w) {
        Path file = Beside.free(w.path(), w.to().extension());
        // Written whole beside it and moved into place, so half a file is never there to be opened. A video's keeps its
        // extension, which is how the encoder picks its container.
        Path part = w.to() == Format.WEBM ? file.resolveSibling(stem(file) + ".part.webm")
                : file.resolveSibling(file.getFileName() + ".part");
        try {
            if (w.to() == Format.WEBM) {
                MotionVideo.write(w.path(), part, w.background());
            } else {
                Files.write(part, w.to() == Format.PNG ? png(w.path()) : sdg(w.path()));
            }
            Files.move(part, file, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            return new Written(w.path(), Optional.of(file), "");
        } catch (IOException | RuntimeException | OutOfMemoryError | LinkageError e) {
            try {
                Files.deleteIfExists(part);
            } catch (IOException | RuntimeException ignored) {
                // What could not be written is said; what could not be cleaned up is left.
            }
            return new Written(w.path(), Optional.empty(),
                    "Could not save " + file.getFileName() + ": " + e.getMessage());
        }
    }

    /** A file's name without its extension. */
    private static String stem(Path file) {
        String name = String.valueOf(file.getFileName());
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /** The picture as PNG: its first frame at its own size, or a drawing rendered at its own. */
    static byte[] png(Path path) {
        Picture p = Pictures.of(path).picture();
        if (p == null) {
            throw new IllegalArgumentException("it is not a picture Rasterware can read");
        }
        if (p.drawing() != null) {
            Sdg sdg = p.drawing().source();
            int w = (int) Math.max(1, Math.round(sdg.width()));
            int h = (int) Math.max(1, Math.round(sdg.height()));
            Raster r = dev.vexelray.fields.convert.Convert.sdgToRaster(w, h).apply(sdg);
            return Png.write(r.width(), r.height(), r.rgba());
        }
        return Png.write(p.frameWidth(), p.frameHeight(), ReadPicture.firstFrame(p));
    }

    /** An SVG as a drawing, by its paths. */
    static byte[] sdg(Path path) throws IOException {
        if (!Format.svg(path)) {
            throw new IllegalArgumentException("a picture made of pixels becomes a drawing by Trace (T)");
        }
        Sdg sdg = dev.vexelray.fields.convert.Convert.svgToSdg(SVG_TEXELS).apply(Svg.read(path));
        return SdgFile.xml(sdg).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
