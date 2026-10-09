package dev.sibarum.pix;

import dev.sibarum.suite.pictures.Decoded;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything Pix knows, as one value: the folder, the images in it, which one is shown, and the picture of it once it
 * has been read.
 *
 * @param folder   the folder being stepped through; null before anything is opened
 * @param pictures its images in order; just the file asked for until the listing arrives, so it shows at once
 * @param listed   whether {@code pictures} is the folder's listing yet
 * @param at       the image shown; null when the folder has none
 * @param preview  the picture of {@code at}, or null until it has been read. Never of another file
 */
record Doc(Path folder, List<Path> pictures, boolean listed, Path at, Decoded preview) {

    static Doc initial() {
        return new Doc(null, List.of(), false, null, null);
    }

    /** Open {@code folder} on {@code at}, or on its first image once listed when {@code at} is null. */
    Doc opening(Path folder, Path at) {
        return new Doc(folder, at == null ? List.of() : List.of(at), false, at, null);
    }

    /**
     * The folder's images arrived. The image shown stays shown, and stays in the list even when the folder's listing
     * would leave it out (a file opened by name whose extension Pix does not list), first, so it can be stepped from.
     */
    Doc listing(List<Path> found) {
        List<Path> all = found;
        if (at != null && !found.contains(at)) {
            all = new ArrayList<>(found.size() + 1);
            all.add(at);
            all.addAll(found);
            all = List.copyOf(all);
        }
        Path shown = at != null ? at : all.isEmpty() ? null : all.getFirst();
        return new Doc(folder, all, true, shown, shown == at ? preview : null);
    }

    /** Show {@code path}. Its picture is not known yet. */
    Doc showing(Path path) {
        return new Doc(folder, pictures, listed, path, null);
    }

    Doc previewing(Decoded p) {
        return new Doc(folder, pictures, listed, at, p);
    }

    /** Where {@link #at} is among {@link #pictures}, or -1. */
    int index() {
        return at == null ? -1 : pictures.indexOf(at);
    }
}
