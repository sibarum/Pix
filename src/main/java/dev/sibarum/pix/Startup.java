package dev.sibarum.pix;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * What Pix opens on: the command line's first path, or the user's Pictures folder.
 *
 * <ul>
 *   <li>{@code pix <file>} shows that file, and steps through the images in its folder;</li>
 *   <li>{@code pix <folder>} shows the folder's first image;</li>
 *   <li>{@code pix} shows the first image in the user's Pictures folder, or their home.</li>
 * </ul>
 *
 * <p>A path that does not exist is ignored rather than refused, since the window is still useful where it would have
 * opened anyway.
 */
final class Startup {

    /** Where to open: a folder, and the file in it to show first, or null for its first image. */
    record Place(Path folder, Path file) {
    }

    private Startup() {
    }

    static Place place(List<String> given) {
        Path asked = given.isEmpty() ? null : Path.of(given.getFirst()).toAbsolutePath().normalize();
        if (asked != null && Files.isRegularFile(asked) && asked.getParent() != null) {
            return new Place(asked.getParent(), asked);
        }
        if (asked != null && Files.isDirectory(asked)) {
            return new Place(asked, null);
        }
        Path home = Path.of(System.getProperty("user.home"));
        Path pictures = home.resolve("Pictures");
        return new Place(Files.isDirectory(pictures) ? pictures : home, null);
    }
}
