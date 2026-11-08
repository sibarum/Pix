package dev.sibarum.pix.core;

import java.nio.file.Files;
import java.nio.file.Path;

/** Where a file made from a picture is written: beside it, under its name, never over another file. */
final class Beside {

    private Beside() {
    }

    /** {@code picture}'s name with {@code extension} for its own, or the first {@code (n)} of it not taken. */
    static Path free(Path picture, String extension) {
        String name = String.valueOf(picture.getFileName());
        int dot = name.lastIndexOf('.');
        String stem = dot > 0 ? name.substring(0, dot) : name;
        Path file = picture.resolveSibling(stem + "." + extension);
        for (int n = 2; Files.exists(file); n++) {
            file = picture.resolveSibling(stem + " (" + n + ")." + extension);
        }
        return file;
    }
}
