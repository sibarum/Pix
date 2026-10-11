package dev.sibarum.pix;

import dev.vexelray.codec.image.Decoded;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What opening and listing do to the document, and that a late arrival is dropped. */
class DocTest {

    private static final Path DIR = Path.of("photos");
    private static final Path A = DIR.resolve("a.png");
    private static final Path B = DIR.resolve("b.png");
    private static final Path C = DIR.resolve("c.png");

    @Test
    void aFileAskedForIsShownBeforeItsFolderIsRead() {
        Doc d = Doc.initial().opening(DIR, B);
        assertEquals(B, d.at());
        assertEquals(List.of(B), d.pictures());
        assertFalse(d.listed());
    }

    @Test
    void theListingKeepsTheFileShownAndItsPicture() {
        Decoded p = Decoded.none(B, "reading");
        Doc d = Doc.initial().opening(DIR, B).previewing(p).listing(List.of(A, B, C));
        assertEquals(B, d.at());
        assertEquals(1, d.index());
        assertSame(p, d.preview(), "the picture already read is still the picture of the file shown");
        assertTrue(d.listed());
    }

    @Test
    void aFolderOpensOnItsFirstImage() {
        Doc d = Doc.initial().opening(DIR, null);
        assertNull(d.at());
        d = d.listing(List.of(A, B, C));
        assertEquals(A, d.at());
        assertNull(d.preview());
    }

    @Test
    void aFileTheListingLeavesOutIsKeptFirst() {
        Path odd = DIR.resolve("scan.raw");
        Doc d = Doc.initial().opening(DIR, odd).listing(List.of(A, B));
        assertEquals(List.of(odd, A, B), d.pictures());
        assertEquals(0, d.index());
    }

    @Test
    void anEmptyFolderShowsNothing() {
        Doc d = Doc.initial().opening(DIR, null).listing(List.of());
        assertNull(d.at());
        assertEquals(-1, d.index());
    }

    @Test
    void aListingOrPictureForSomewhereElseIsDropped() {
        Model m = new Model();
        m.open(DIR, A);
        m.listed(Path.of("elsewhere"), List.of(C));
        assertFalse(m.doc().listed(), "a listing of a folder no longer open");
        m.previewed(B, Decoded.none(B, "late"));
        assertNull(m.doc().preview(), "a picture of a file no longer shown");
        m.previewed(A, Decoded.none(A, "on time"));
        assertEquals("on time", m.doc().preview().identity());
    }
}
