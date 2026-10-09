package dev.sibarum.pix;

import dev.sibarum.suite.pictures.Decoded;
import dev.sibarum.suite.view.PictureViewer.Showing;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the viewer is told for each state of the document. */
class ShowingTest {

    private static final Path DIR = Path.of("photos");
    private static final Path A = DIR.resolve("a.png");
    private static final Path B = DIR.resolve("b.png");

    @Test
    void aFileIsNamedAtOnceAndItsFolderCountedOnceRead() {
        Showing s = Viewer.showing(Doc.initial().opening(DIR, B));
        assertEquals("b.png", s.name());
        assertEquals("b.png — Pix", s.title());
        assertEquals("reading the folder…", s.count());
        assertEquals("reading…", s.info());
        assertTrue(s.keep(), "the picture up stays while the next is read");

        s = Viewer.showing(Doc.initial().opening(DIR, B).listing(List.of(A, B)));
        assertEquals("Image 2 of 2", s.count());
    }

    @Test
    void aFileWithNoPictureSaysWhyAndClears() {
        Doc d = Doc.initial().opening(DIR, A).listing(List.of(A)).previewing(Decoded.none(A, "not an image"));
        Showing s = Viewer.showing(d);
        assertEquals("not an image", s.info());
        assertNull(s.picture());
        assertFalse(s.keep());
    }

    @Test
    void anEmptyFolderIsNamedAndSaysSo() {
        Showing s = Viewer.showing(Doc.initial().opening(DIR, null).listing(List.of()));
        assertEquals("photos", s.name());
        assertEquals("no images here", s.count());
        assertEquals("Pix", s.title());
        assertFalse(s.keep());
    }
}
