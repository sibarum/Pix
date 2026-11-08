package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.Recall;
import dev.sibarum.pix.core.Messages.Recalled;
import dev.vexelray.framework.api.Job;
import dev.vexelray.framework.core.Launch;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Where Rasterware starts, on the offload lane: the file or folder the command line named, or the person's Pictures folder,
 * or their home. A job because telling a file from a folder is asking the disk, which a step may not.
 *
 * <pre>
 * pix &lt;file&gt;      that picture, stepping through the pictures in its folder
 * pix &lt;folder&gt;    the folder's first picture
 * pix             the first picture in the person's Pictures folder, or their home
 * </pre>
 */
@Job
public final class Begin {

    private final Launch launch;

    public Begin(Launch launch) {
        this.launch = launch;
    }

    public Recalled run(Recall r) {
        Optional<Path> asked = launch.rest().isEmpty() ? Optional.empty() : path(launch.rest().getFirst());
        return where(asked, Path.of(System.getProperty("user.home")));
    }

    static Recalled where(Optional<Path> asked, Path home) {
        if (asked.isPresent()) {
            Path p = asked.get();
            if (Files.isDirectory(p)) {
                return new Recalled(p, Optional.empty());
            }
            Path parent = p.getParent();
            return new Recalled(parent != null ? parent : p, Optional.of(p));
        }
        Path pictures = home.resolve("Pictures");
        return new Recalled(Files.isDirectory(pictures) ? pictures : home, Optional.empty());
    }

    /** A path from the command line, absolute and normalised; none for one that is not a path. */
    static Optional<Path> path(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Path.of(text).toAbsolutePath().normalize());
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
    }
}
