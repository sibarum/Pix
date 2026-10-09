package dev.sibarum.pix.files;

import java.nio.file.Path;

/**
 * One file as Pix shows it, decided once and read-only: what the file is, and its picture at two sizes.
 *
 * <p>Nothing in a shown file is ever executed: this is text and decoded pixels, produced by {@link Pictures}.
 *
 * @param path     the file
 * @param identity what the file is ("PNG image · 1920 × 1080"), or why there is no picture of it ("not an image",
 *                 "unreadable: AccessDeniedException")
 * @param picture  the decoded image at the thumbnail's size, {@link Picture#FRAME_SIDE}; null when there is none
 * @param large    the same image at the viewer's size, {@link Picture#VIEW_SIDE}, from the same decode; null when only
 *                 the thumbnail was asked for, and when there is no picture
 */
public record Preview(Path path, String identity, Picture picture, Picture large) {

    /** A file with no picture to show, and the reason. */
    public static Preview none(Path path, String why) {
        return new Preview(path, why, null, null);
    }

    /** Whether there is a picture to show. */
    public boolean image() {
        return picture != null;
    }

    /** What this holds in decoded pixels: the weight a cache of previews is bounded by. */
    public long bytes() {
        return (picture == null ? 0 : picture.bytes()) + (large == null ? 0 : large.bytes());
    }
}
