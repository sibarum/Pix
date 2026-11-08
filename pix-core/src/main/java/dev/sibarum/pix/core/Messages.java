package dev.sibarum.pix.core;

import dev.vexelray.gui.core.Feed;
import dev.vexelray.gui.core.ImageSdg;
import sibarum.atchung.Topic;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Every message Rasterware's parts send each other: what each one says, and the topic it travels on.
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

    /** Go to this picture: a thumbnail in the filmstrip, clicked. A path not in the folder is ignored. */
    public static final Topic<Go> GO_TOPIC = Topic.edge(Go.class);

    public record Go(Path path) {
    }

    /**
     * How many pictures the filmstrip shows each side of the one wanted: what fits in the bar, which only the view
     * knows. The viewer reads thumbnails that far and no further; zero, the strip is hidden and none are read.
     */
    public static final Topic<Span> SPAN_TOPIC = Topic.state(Span.class);

    public record Span(int radius) {
    }

    /**
     * Ask for a picture to open, among the pictures in its folder, or, {@code folder}, a folder to open on its first:
     * Ctrl+O or a click on the name, and Ctrl+Shift+O. The viewer asks the OS's own dialog, starting in the folder open.
     */
    public static final Topic<Open> OPEN_TOPIC = Topic.edge(Open.class);

    public record Open(boolean folder) {
    }

    // ------------------------------------------------------------------ the viewer tells the view

    /**
     * Keep this picture, read, until told to let it go: the one wanted, or one read ahead. Its feed is already handed
     * its pixels; with no feed, the file is not a picture Rasterware can show, and its {@code identity} says why.
     */
    public static final Topic<Hold> HOLD_TOPIC = Topic.edge(Hold.class);

    public record Hold(Pictured picture) {
    }

    /**
     * Show the picture held for this path. It is always held first: both travel from the viewer, in order. {@code side}
     * is where it is from the picture shown before, in the folder: 1 later, -1 earlier, so it is thrown in from that
     * side; 0 when there was none, or either is not in the folder (a picture opened), and it simply replaces it.
     */
    public static final Topic<Show> SHOW_TOPIC = Topic.edge(Show.class);

    public record Show(Path path, int side) {
    }

    /**
     * Show nothing: a folder opened that has no pictures, so the picture from the one before is not left on the stage
     * as if it were there.
     */
    public static final Topic<Clear> CLEAR_TOPIC = Topic.edge(Clear.class);

    public record Clear() {
    }

    /** Let go of the picture held for this path, closing its feed. Never the one shown. */
    public static final Topic<Release> RELEASE_TOPIC = Topic.edge(Release.class);

    public record Release(Path path) {
    }

    /** What the window's title and status line say: the picture's name, and where it is in its folder. */
    public static final Topic<Caption> CAPTION_TOPIC = Topic.state(Caption.class);

    public record Caption(String name, String place) {
    }

    /**
     * What the filmstrip shows: the pictures within the span either side of the one wanted, in folder order, and where
     * that one is among them. Empty with nothing wanted in a listed folder, or no span.
     */
    public static final Topic<Filmstrip> FILMSTRIP_TOPIC = Topic.state(Filmstrip.class);

    public record Filmstrip(List<Path> paths, int at) {
        public Filmstrip {
            paths = List.copyOf(paths);
        }
    }

    /** Keep this thumbnail until told to let it go. Held apart from the pictures, and let go apart from them. */
    public static final Topic<HoldThumb> HOLD_THUMB_TOPIC = Topic.edge(HoldThumb.class);

    public record HoldThumb(Thumb thumb) {
    }

    /** Let go of the thumbnail held for this path, closing its feed. Never one the filmstrip shows. */
    public static final Topic<ReleaseThumb> RELEASE_THUMB_TOPIC = Topic.edge(ReleaseThumb.class);

    public record ReleaseThumb(Path path) {
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

    /**
     * Read a picture and put it in a feed: the one wanted, or, when that is shown, one read ahead. One at a time: the
     * viewer asks for the next only once this is answered.
     */
    public static final Topic<Read> READ_TOPIC = Topic.edge(Read.class);

    public record Read(Path path) {
    }

    /** Read only a picture's thumbnail: one in the filmstrip that is not read whole. One at a time, as {@link Read}. */
    public static final Topic<ReadThumb> READ_THUMB_TOPIC = Topic.edge(ReadThumb.class);

    public record ReadThumb(Path path) {
    }

    /** A thumbnail read on its own. */
    public static final Topic<Thumbnailed> THUMBNAILED_TOPIC = Topic.edge(Thumbnailed.class);

    public record Thumbnailed(Thumb thumb) {
    }

    /**
     * A picture read: its feed and size, or none and why. {@code identity} is what it is and its own size ("PNG image ·
     * 1920 × 1080"), the feed's being at most what is drawn; {@code bytes} is the file's size on disk, or -1 unknown.
     * {@code thumb} is its thumbnail, from the same decode. {@code drawing} is the drawing itself when the file is an SDG
     * drawing, which the view draws sharp at any zoom in place of the feed (then only its poster); {@code width} and
     * {@code height} are then what it is fitted from, as an SVG decoded at its longest. {@code modified} is the file's
     * last-modified time, ms since the epoch, or -1 unknown; {@code palette} up to five of its colours, most of the
     * picture first, {@code 0xRRGGBBAA} (docs/details.md).
     */
    public static final Topic<Pictured> PICTURED_TOPIC = Topic.edge(Pictured.class);

    public record Pictured(Path path, Optional<Feed> feed, int width, int height, String identity, long bytes,
                           Thumb thumb, Optional<ImageSdg> drawing, long modified, List<Integer> palette) {

        public Pictured {
            palette = List.copyOf(palette);
        }

        /** A picture that is not a drawing, with no time or palette known: what a test makes. */
        public Pictured(Path path, Optional<Feed> feed, int width, int height, String identity, long bytes, Thumb thumb) {
            this(path, feed, width, height, identity, bytes, thumb, Optional.empty(), -1, List.of());
        }
    }

    // ------------------------------------------------------------------ tracing a picture into a drawing (docs/trace.md)

    /**
     * How a picture is traced: into {@code colours} regions, each a layer, and whether a region whose colour varies keeps
     * it as a texture ({@code texture}) or is flattened to its mean. Not a message: what {@link Tracing}, {@link Trace},
     * {@link Traced} and {@link WriteTrace} carry.
     */
    public record TraceSettings(int colours, boolean texture) {

        /** The fewest and most colours the panel offers. */
        public static final int FEWEST = 2, MOST = 12;

        /** What the panel opens on: the board's six colours, gradients kept. */
        public static final TraceSettings FIRST = new TraceSettings(6, true);

        public TraceSettings {
            colours = Math.clamp(colours, FEWEST, MOST);
        }
    }

    /**
     * The view tells the viewer what the person wants traced now: the picture shown, with the panel's settings, or, with
     * no path, nothing, the panel closed. Only the latest matters.
     */
    public static final Topic<Tracing> TRACING_TOPIC = Topic.state(Tracing.class);

    public record Tracing(Optional<Path> path, TraceSettings settings) {
    }

    /**
     * The viewer asks {@link TracePicture} to trace a picture, for the panel to compare. One at a time: the next only
     * once this is answered, and then only if the person has asked for another since.
     */
    public static final Topic<Trace> TRACE_TOPIC = Topic.edge(Trace.class);

    public record Trace(Path path, TraceSettings settings) {
    }

    /**
     * A picture traced: the drawing, its layer count, the size it would be as a file, and its regions' colours, straight
     * {@code 0xRRGGBBAA}, bottom layer first; or no drawing and {@code why}. Heard by the viewer, which traces again if
     * the person has moved on, and by the view, which shows it if it is still of the picture shown.
     */
    public static final Topic<Traced> TRACED_TOPIC = Topic.edge(Traced.class);

    public record Traced(Path path, TraceSettings settings, Optional<ImageSdg> drawing, int layers, long bytes,
                         List<Integer> palette, String why) {
        public Traced {
            palette = List.copyOf(palette);
        }
    }

    /** The view asks for the trace wanted to be saved as a drawing beside its picture. */
    public static final Topic<SaveTrace> SAVE_TRACE_TOPIC = Topic.edge(SaveTrace.class);

    public record SaveTrace() {
    }

    /** The viewer asks {@link SaveDrawing} to trace a picture at full quality and write it beside the picture. */
    public static final Topic<WriteTrace> WRITE_TRACE_TOPIC = Topic.edge(WriteTrace.class);

    public record WriteTrace(Path path, TraceSettings settings) {
    }

    // ------------------------------------------------------------------ converting a picture (docs/convert.md)

    /**
     * The formats a picture can be written in, as far as the stack can write them: PNG (pure Java, {@code Png.write})
     * and SDG. What each is called, its extension, and what the Convert panel says it does to a picture.
     */
    public enum Format {
        PNG("PNG", "png", "Every pixel kept, saved as PNG."),
        SDG("SDG", "sdg", "Shapes and colours as a drawing, sharp at any size.");

        private final String label;
        private final String extension;
        private final String says;

        Format(String label, String extension, String says) {
            this.label = label;
            this.extension = extension;
            this.says = says;
        }

        public String label() {
            return label;
        }

        public String extension() {
            return extension;
        }

        public String says() {
            return says;
        }

        /** The format {@code path} is in, by its name: what the badge says. Empty for one Rasterware cannot write. */
        public static Optional<Format> of(Path path) {
            String name = String.valueOf(path.getFileName()).toLowerCase(java.util.Locale.ROOT);
            if (name.endsWith(".png")) {
                return Optional.of(PNG);
            }
            if (name.endsWith(".sdg") || name.endsWith(".sdgb")) {
                return Optional.of(SDG);
            }
            return Optional.empty();
        }

        /** Whether {@code path} names an SVG, which becomes a drawing by its paths rather than by a trace. */
        public static boolean svg(Path path) {
            String name = String.valueOf(path.getFileName()).toLowerCase(java.util.Locale.ROOT);
            return name.endsWith(".svg") || name.endsWith(".svgz");
        }
    }

    /** The view asks for a picture to be written beside itself in another format: Save in the Convert panel. */
    public static final Topic<Convert> CONVERT_TOPIC = Topic.edge(Convert.class);

    public record Convert(Path path, Format to) {
    }

    /** The viewer asks {@link ConvertPicture} to write a picture beside itself in another format. */
    public static final Topic<WriteCopy> WRITE_COPY_TOPIC = Topic.edge(WriteCopy.class);

    public record WriteCopy(Path path, Format to) {
    }

    // ------------------------------------------------------------------ a drawing's layers (docs/layers.md)

    /**
     * The view asks for a drawing's roles to be saved into it: a theme role for each of its top-level layers, in the
     * drawing's order, {@code ""} for none. Only a solid layer takes one; any other's is ignored.
     */
    public static final Topic<SaveRoles> SAVE_ROLES_TOPIC = Topic.edge(SaveRoles.class);

    public record SaveRoles(Path path, List<String> roles) {
        public SaveRoles {
            roles = List.copyOf(roles);
        }
    }

    /** The viewer asks {@link RoleDrawing} to write those roles into the drawing, in place. */
    public static final Topic<WriteRoles> WRITE_ROLES_TOPIC = Topic.edge(WriteRoles.class);

    public record WriteRoles(Path path, List<String> roles) {
        public WriteRoles {
            roles = List.copyOf(roles);
        }
    }

    /**
     * A picture written beside another, by a trace's save or a conversion, or over itself, by a save of its roles; or
     * none and why. The viewer then lists the
     * folder again and wants the new file; the view says why when there is none.
     */
    public static final Topic<Written> WRITTEN_TOPIC = Topic.edge(Written.class);

    public record Written(Path picture, Optional<Path> file, String why) {
    }

    /**
     * A picture's thumbnail: a feed of its own at most {@code ReadPicture.THUMB} px on its longer side, and that size;
     * no feed for a file that is not a picture. Not a message: what {@link Pictured}, {@link Thumbnailed} and
     * {@link HoldThumb} carry.
     */
    public record Thumb(Path path, Optional<Feed> feed, int width, int height) {
    }
}
