package dev.sibarum.pix.files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Which files a folder is stepped through, and in what order. */
class FolderTest {

    @Test
    void numbersAreComparedAsNumbers() {
        assertTrue(Folder.natural("IMG_9.jpg", "IMG_10.jpg") < 0);
        assertTrue(Folder.natural("img_010.jpg", "IMG_9.jpg") > 0, "leading zeros do not make a number larger");
        assertTrue(Folder.natural("a.png", "B.png") < 0, "case is ignored");
        assertTrue(Folder.natural("IMG_9.jpg", "img_9.jpg") != 0, "names differing only in case still have an order");
    }

    @Test
    void onlyImagesInNaturalOrder(@TempDir Path dir) throws IOException {
        for (String n : List.of("IMG_10.jpg", "notes.txt", "IMG_2.PNG", "IMG_1.webp", "song.mp3")) {
            Files.writeString(dir.resolve(n), "x");
        }
        Files.createDirectory(dir.resolve("album.png"));
        assertEquals(List.of("IMG_1.webp", "IMG_2.PNG", "IMG_10.jpg"),
                Folder.images(dir).stream().map(p -> p.getFileName().toString()).toList(),
                "a folder named like an image is still a folder");
    }

    @Test
    void videosAreListedAmongTheImages(@TempDir Path dir) throws IOException {
        for (String n : List.of("b.mkv", "a.jpg", "c.WEBM", "d.mov", "e.mp4", "subs.srt")) {
            Files.writeString(dir.resolve(n), "x");
        }
        Files.createDirectory(dir.resolve("clips.mp4"));
        assertEquals(List.of("a.jpg", "b.mkv", "c.WEBM", "d.mov", "e.mp4"),
                Folder.images(dir).stream().map(p -> p.getFileName().toString()).toList(),
                "by name, whatever the case of the extension; a folder named like a video is still a folder");
    }

    @Test
    void aFolderThatCannotBeReadHasNoImages(@TempDir Path dir) {
        assertEquals(List.of(), Folder.images(dir.resolve("gone")));
    }
}
