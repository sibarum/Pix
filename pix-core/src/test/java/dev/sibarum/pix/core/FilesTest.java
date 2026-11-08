package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.Recalled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The jobs that ask the disk where to start and what is in a folder. */
class FilesTest {

    @TempDir
    Path dir;

    @Test
    void numbersInNamesAreComparedAsNumbers() {
        List<String> names = new java.util.ArrayList<>(List.of("IMG_10.jpg", "img_9.jpg", "IMG_009b.jpg", "a.png"));
        names.sort(ListPictures::natural);
        assertEquals(List.of("a.png", "img_9.jpg", "IMG_009b.jpg", "IMG_10.jpg"), names);
    }

    @Test
    void aFolderListsItsPicturesInOrderAndNothingElse() throws IOException {
        for (String n : List.of("b10.png", "b9.jpg", "notes.txt", "a.gif")) {
            Files.writeString(dir.resolve(n), "x");
        }
        Files.createDirectory(dir.resolve("c.png"));
        List<String> listed = ListPictures.pictures(dir).stream().map(p -> p.getFileName().toString()).toList();
        assertEquals(List.of("a.gif", "b9.jpg", "b10.png"), listed);
    }

    @Test
    void aFolderThatIsNotThereListsNothing() {
        assertEquals(List.of(), ListPictures.pictures(dir.resolve("gone")));
    }

    @Test
    void aFileStartsInItsFolderAndAFolderInItself() throws IOException {
        Path file = Files.writeString(dir.resolve("p.png"), "x");
        assertEquals(new Recalled(dir, Optional.of(file)), Begin.where(Optional.of(file), dir));
        assertEquals(new Recalled(dir, Optional.empty()), Begin.where(Optional.of(dir), dir));
    }

    @Test
    void nothingNamedStartsInPicturesOrHome() throws IOException {
        assertEquals(new Recalled(dir, Optional.empty()), Begin.where(Optional.empty(), dir));
        Path pictures = Files.createDirectory(dir.resolve("Pictures"));
        assertEquals(new Recalled(pictures, Optional.empty()), Begin.where(Optional.empty(), dir));
    }

    @Test
    void aBlankArgumentIsNoPath() {
        assertTrue(Begin.path("  ").isEmpty());
        assertEquals(Optional.of(dir.toAbsolutePath().normalize()), Begin.path(dir.toString()));
    }
}
