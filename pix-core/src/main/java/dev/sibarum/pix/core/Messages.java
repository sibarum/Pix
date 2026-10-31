package dev.sibarum.pix.core;

import dev.vexelray.gui.core.Feed;
import sibarum.atchung.Topic;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Every message Pix's parts send each other: what each one says, and the topic it travels on.
 *
 * <p>Three kinds of part, none holding another. The {@link Viewer} decides which picture is wanted and when it is read;
 * the view, in {@code -gui}, shows it and says what the person pressed; the jobs ({@link Begin}, {@link ListPictures},
 * {@link ReadPicture}) touch the disk. Each record is what crossed, and crosses as itself: the processor refuses a
 * payload that is not deeply immutable, with one exception that matters here. A {@link Feed} is a handle to a picture
 * already on its way to the screen, and crosses as one, so a decoded picture's pixels never go through the bus.
 *
 * <p>Grouped by direction, each record beside its topic, which is named by its payload.
 */
public final class Messages {

    private Messages() {
    }

    // ------------------------------------------------------------------ the view tells the viewer

    /** Go {@code delta} pictures on through the folder (back, when negative): an arrow key, or Page Up or Down. */
    public static final Topic<Step> STEP_TOPIC = Topic.edge(Step.class);

    public record Step(int delta) {
    }

    /** Go to the folder's first picture, or its last: Home or End. */
    public static final Topic<Edge> EDGE_TOPIC = Topic.edge(Edge.class);

    public record Edge(boolean last) {
    }

    // ------------------------------------------------------------------ the viewer tells the view

    /**
     * Show this picture: its feed, already handed its pixels, and its size in them. With no feed, the file is not a
     * picture Pix can show, and {@code identity} says why ("not an image").
     */
    public static final Topic<Show> SHOW_TOPIC = Topic.edge(Show.class);

    public record Show(Path path, Optional<Feed> feed, int width, int height, String identity) {
    }

    /** What the window's title and status line say: the picture's name, and where it is in its folder. */
    public static final Topic<Caption> CAPTION_TOPIC = Topic.state(Caption.class);

    public record Caption(String name, String place) {
    }

    // ------------------------------------------------------------------ the viewer asks the jobs, and they answer

    /** Where to start: what the command line named, if anything. */
    public static final Topic<Recall> RECALL_TOPIC = Topic.edge(Recall.class);

    public record Recall() {
    }

    /**
     * The folder to step through, and the file in it to show first, if the command line named one. A folder named, or
     * nothing named, shows the folder's first picture.
     */
    public static final Topic<Recalled> RECALLED_TOPIC = Topic.edge(Recalled.class);

    public record Recalled(Path folder, Optional<Path> file) {
    }

    /** List the pictures in a folder. */
    public static final Topic<ListFolder> LIST_FOLDER_TOPIC = Topic.edge(ListFolder.class);

    public record ListFolder(Path folder) {
    }

    /** A folder's pictures, in the order they are stepped through; empty when it cannot be read. */
    public static final Topic<Listed> LISTED_TOPIC = Topic.edge(Listed.class);

    public record Listed(Path folder, List<Path> pictures) {
        public Listed {
            pictures = List.copyOf(pictures);
        }
    }

    /** Read a picture and put it in a feed. One at a time: the viewer asks for the next only once this is answered. */
    public static final Topic<Read> READ_TOPIC = Topic.edge(Read.class);

    public record Read(Path path) {
    }

    /** A picture read: its feed and size, or none and why. */
    public static final Topic<Pictured> PICTURED_TOPIC = Topic.edge(Pictured.class);

    public record Pictured(Path path, Optional<Feed> feed, int width, int height, String identity) {
    }
}
