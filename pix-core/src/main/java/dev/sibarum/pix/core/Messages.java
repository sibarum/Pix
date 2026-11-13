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

    /**
     * What the window's title and status line say: the picture's name, its title (its folder's name, a slash and its
     * name, as {@code Trophies/404.jpg}; the name alone with no folder above it), and where it is in its folder.
     */
    public static final Topic<Caption> CAPTION_TOPIC = Topic.state(Caption.class);

    public record Caption(String name, String title, String place) {
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
     * picture first, {@code 0xRRGGBBAA} (docs/details.md). {@code plays} is there when the file plays (an animated GIF,
     * WebP or APNG): its feed then has room for the frames after the first, which it shows until it is played.
     */
    public static final Topic<Pictured> PICTURED_TOPIC = Topic.edge(Pictured.class);

    public record Pictured(Path path, Optional<Feed> feed, int width, int height, String identity, long bytes,
                           Thumb thumb, Optional<ImageSdg> drawing, long modified, List<Integer> palette,
                           Optional<Plays> plays) {

        public Pictured {
            palette = List.copyOf(palette);
        }

        /** A picture that is not a drawing, with no time or palette known: what a test makes. */
        public Pictured(Path path, Optional<Feed> feed, int width, int height, String identity, long bytes, Thumb thumb) {
            this(path, feed, width, height, identity, bytes, thumb, Optional.empty(), -1, List.of(), Optional.empty());
        }
    }

    /**
     * That a picture plays, and what its file says about the playing: the codec's {@code Motion} as values, which the
     * view plays it by. {@code size} and {@code modified} are the file's as it was read, so the frames played are of the
     * file the first was. {@code video} is a video, played with its sound and fitted to fill the stage; otherwise an
     * animated GIF, WebP or APNG, never enlarged. {@code transparent} is whether its first frame has a pixel that is not
     * opaque, so a video made of it asks what goes behind (docs/convert.md); never for a video. Not a message: what
     * {@link Pictured} carries.
     */
    public record Plays(long size, long modified, int frames, long durationMillis, int loops, boolean video,
                        boolean transparent) {
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
     * The formats a picture can be written in, as far as the stack can write them: PNG (pure Java, {@code Png.write}),
     * SDG, and a picture that plays as a video (AV1 in WebM, the recorder's encoder). What each is called, its
     * extension, and what the Convert panel says it does to a picture.
     */
    public enum Format {
        PNG("PNG", "png", "Every pixel kept, saved as PNG."),
        SDG("SDG", "sdg", "Shapes and colours as a drawing, sharp at any size."),
        WEBM("Video", "webm", "Every frame, for as long as it is shown, as an AV1 video. Plays once, silent.");

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
            if (name.endsWith(".webm")) {
                return Optional.of(WEBM);
            }
            return Optional.empty();
        }

        /** Whether {@code path} names an SVG, which becomes a drawing by its paths rather than by a trace. */
        public static boolean svg(Path path) {
            String name = String.valueOf(path.getFileName()).toLowerCase(java.util.Locale.ROOT);
            return name.endsWith(".svg") || name.endsWith(".svgz");
        }
    }

    /** What goes behind a transparent picture made a video unless the person picks otherwise: black, {@code 0xRRGGBB}. */
    public static final int BLACK = 0x000000;

    /**
     * The view asks for a picture to be written beside itself in another format: Save in the Convert panel.
     * {@code background} ({@code 0xRRGGBB}) is what shows through a transparent pixel in a format that has none (a
     * video); the formats that keep transparency ignore it.
     */
    public static final Topic<Convert> CONVERT_TOPIC = Topic.edge(Convert.class);

    public record Convert(Path path, Format to, int background) {

        /** On black, which only a video uses. */
        public Convert(Path path, Format to) {
            this(path, to, BLACK);
        }
    }

    /** The viewer asks {@link ConvertPicture} to write a picture beside itself in another format, as {@link Convert}. */
    public static final Topic<WriteCopy> WRITE_COPY_TOPIC = Topic.edge(WriteCopy.class);

    public record WriteCopy(Path path, Format to, int background) {

        /** On black, which only a video uses. */
        public WriteCopy(Path path, Format to) {
            this(path, to, BLACK);
        }
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

    // ------------------------------------------------------------------ a drawing made by hand (docs/sdg-editor.md)

    /** What a size is said in: pixels, or a length at a DPI. {@code perInch} is how many make an inch; 0 for pixels. */
    public enum Unit {
        PX("Pixels", 0), MM("mm", 25.4), IN("in", 1), PT("pt", 72);

        private final String label;
        private final double perInch;

        Unit(String label, double perInch) {
            this.label = label;
            this.perInch = perInch;
        }

        public String label() {
            return label;
        }

        public double perInch() {
            return perInch;
        }
    }

    /**
     * How big a new drawing is: {@code width × height} in {@code unit}, and, for a length, {@code dpi} texels an inch.
     * In pixels the canvas's units are pixels and the field has a texel a unit; in a length they are that length, and the
     * field has {@code dpi / perInch} texels a unit (the stencil's {@code scale}, sdg.md §1). Not a message: what
     * {@link CreateSdg} and {@link Sketched} carry.
     */
    public record Sizing(double width, double height, Unit unit, double dpi) {

        /** The longest side a field may have, and the most texels it may hold: 64 MB of floats. */
        public static final int MOST_SIDE = 8192, MOST_TEXELS = 4096 * 4096;

        /** What the New SDG window opens on. */
        public static final Sizing FIRST = new Sizing(1024, 1024, Unit.PX, 300);

        /** Texels a unit. */
        public double scale() {
            return unit == Unit.PX ? 1 : dpi / unit.perInch();
        }

        public int texelsWide() {
            return (int) Math.round(width * scale());
        }

        public int texelsHigh() {
            return (int) Math.round(height * scale());
        }

        /** Why a field of this size cannot be made, or empty when it can. */
        public String why() {
            long w = texelsWide();
            long h = texelsHigh();
            if (w < 1 || h < 1) {
                return "Too small: under one texel";
            }
            if (w > MOST_SIDE || h > MOST_SIDE) {
                return "Too large: at most " + MOST_SIDE + " texels a side";
            }
            if (w * h > MOST_TEXELS) {
                return "Too large: at most " + (MOST_TEXELS / 1_000_000) + " million texels";
            }
            return "";
        }

        /**
         * The same size said in {@code to}: the same length when both are lengths, the same texels when either is
         * pixels (through the DPI). Pixels are whole; a length is rounded to hundredths.
         */
        public Sizing in(Unit to) {
            double inchesW = unit == Unit.PX ? width / dpi : width / unit.perInch();
            double inchesH = unit == Unit.PX ? height / dpi : height / unit.perInch();
            double k = to == Unit.PX ? dpi : to.perInch();
            double step = to == Unit.PX ? 1 : 100;
            return new Sizing(Math.round(inchesW * k * step) / step, Math.round(inchesH * k * step) / step, to, dpi);
        }
    }

    /** How a field is edited, and so what it is saved as (docs/sdg-editor.md). */
    public enum EditMode {
        HEIGHTMAP("Heightmap", "Terrain in 3D; where it crosses the plane is the edge, a steeper slope a sharper one."),
        STENCIL("Stencil", "Traced over a picture, kept a true distance, so its edges are exact."),
        MASK("Mask", "Transparency, painted soft or hard, saved as an alpha mask.");

        private final String label;
        private final String says;

        EditMode(String label, String says) {
            this.label = label;
            this.says = says;
        }

        public String label() {
            return label;
        }

        public String says() {
            return says;
        }

        /**
         * Whether a brush paints this mode's field in the flat view. A heightmap's brushes raise and lower terrain in 3D,
         * which slice 4 brings.
         */
        public boolean paints() {
            return this != HEIGHTMAP;
        }

        /** Whether a picture may be put under this mode's field to trace over. */
        public boolean traces() {
            return this != HEIGHTMAP;
        }

        /**
         * Whether the field is kept a true distance: re-distanced from its edge when a stroke lifts, and drawn in the
         * editor with its edge sharp, as a reader draws it. A mask is alpha and is left as painted.
         */
        public boolean distances() {
            return this == STENCIL;
        }

        /** Whether a brush snaps to the edges of the picture traced over: a stencil is an outline, a mask is not. */
        public boolean snaps() {
            return this == STENCIL;
        }
    }

    /**
     * How a brush paints: {@code radius} in texels; {@code strength}, how far one stroke moves a texel toward the brush's
     * value, 0 to 1; {@code hardness}, the part of the radius painted at full strength before it falls off to nothing at
     * the rim, 0 (all falloff) to 1 (a hard edge); whether it paints the inside ({@code erase} false) or the outside; and
     * {@code snap}, 0 (off) to 1 (tightest), how closely it keeps to the colour under its middle in the picture traced
     * over, so that it stops at that picture's edges (only where the mode {@link EditMode#snaps}). Not a message: what
     * {@link Dab} carries.
     */
    public record Brush(float radius, float strength, float hardness, boolean erase, float snap) {

        /** The smallest and largest radius the editor offers, in texels. */
        public static final float LEAST = 1, MOST = 512;

        /** What the editor opens on. */
        public static final Brush FIRST = new Brush(24, 0.5f, 0.5f, false, 0.5f);

        public Brush {
            radius = Math.clamp(radius, LEAST, MOST);
            strength = Math.clamp(strength, 0f, 1f);
            hardness = Math.clamp(hardness, 0f, 1f);
            snap = Math.clamp(snap, 0f, 1f);
        }

        /** A brush that does not snap. */
        public Brush(float radius, float strength, float hardness, boolean erase) {
            this(radius, strength, hardness, erase, 0f);
        }

        /**
         * This brush at full strength: how a stencil is painted ({@link EditMode#distances}). A stencil's texel is only
         * inside or outside once the stroke settles, so a stroke that moved it part of the way would show grey while
         * drawn and then fall back to where it was; at full strength the edge settles where the brush's falloff crosses
         * a half, and the hardness says where that is.
         */
        public Brush whole() {
            return new Brush(radius, 1f, hardness, erase, snap);
        }
    }

    /**
     * The editor tells the {@link Sketcher} the brush was lifted: the stroke is over. A stencil is then re-distanced
     * about what the stroke touched ({@link EditMode#distances}).
     */
    public static final Topic<Lift> LIFT_TOPIC = Topic.edge(Lift.class);

    public record Lift() {
    }

    /**
     * The editor tells the {@link Sketcher} where the brush is: {@code x}, {@code y} in texels from the field's top left,
     * which may be off it. {@code first} starts a stroke there; otherwise the brush is drawn from where the last dab was,
     * so a fast drag paints a line and not a row of dots. In order, one per pointer move, so the mailbox is deep.
     */
    public static final Topic<Dab> DAB_TOPIC = Topic.edge(Dab.class);

    public record Dab(Brush brush, float x, float y, boolean first) {
    }

    /**
     * The {@link Sketcher} asks itself, through the framework's timer, to show the field again: every slot of its
     * preview's feed was in use when it last changed (the feed's backpressure), so it tries again a moment later.
     */
    public static final Topic<Repaint> REPAINT_TOPIC = Topic.edge(Repaint.class);

    public record Repaint() {
    }

    /** The editor asks for a picture to put under the field: the {@link Sketcher} asks the OS's open dialog. */
    public static final Topic<PickBackdrop> PICK_BACKDROP_TOPIC = Topic.edge(PickBackdrop.class);

    public record PickBackdrop() {
    }

    /** The {@link Sketcher} asks {@link ReadBackdropPicture} to read the picture chosen into a feed. */
    public static final Topic<ReadBackdrop> READ_BACKDROP_TOPIC = Topic.edge(ReadBackdrop.class);

    public record ReadBackdrop(Path path) {
    }

    /**
     * A picture read to trace over: its feed and its size, or none and why. Heard by the editor, which shows it under the
     * field and closes the one before, and by the {@link Sketcher}, which shows the field over it as a shade rather than
     * in grey.
     */
    public static final Topic<Backdrop> BACKDROP_TOPIC = Topic.edge(Backdrop.class);

    public record Backdrop(Path path, Optional<Feed> feed, int width, int height, String why) {
    }

    /**
     * The New SDG window asks for a field to be made: this size, filled with the far inside or the far outside, to be
     * edited in this mode. It replaces the one being edited.
     */
    public static final Topic<CreateSdg> CREATE_SDG_TOPIC = Topic.edge(CreateSdg.class);

    public record CreateSdg(Sizing sizing, boolean inside, EditMode mode) {
    }

    /**
     * The {@link Sketcher} tells the editor a field was made: a preview of it in a feed of its own at most
     * {@code Sketcher.PREVIEW} px on its longer side, which the {@code Sketcher} writes again as the field is painted and
     * the editor closes when the next field's arrives; and the field's size in texels, its sizing and its mode.
     */
    public static final Topic<Sketched> SKETCHED_TOPIC = Topic.edge(Sketched.class);

    public record Sketched(Feed feed, int width, int height, Sizing sizing, EditMode mode) {
    }

    /** The editor asks for the field to be saved: the {@link Sketcher} asks where, with the OS's save dialog. */
    public static final Topic<SaveSdg> SAVE_SDG_TOPIC = Topic.edge(SaveSdg.class);

    public record SaveSdg() {
    }

    /** The {@link Sketcher} asks {@link WriteDrawing} to write the field's drawing at {@code file}, or beside it. */
    public static final Topic<WriteSdg> WRITE_SDG_TOPIC = Topic.edge(WriteSdg.class);

    public record WriteSdg(Path file, ImageSdg drawing) {
    }

    /**
     * A drawing made by hand written, or none and why; none and no why when the person cancelled the dialog. Heard by
     * the {@link Sketcher}, which may save again, by the editor, which says so, and by the viewer, which shows it.
     */
    public static final Topic<SdgWritten> SDG_WRITTEN_TOPIC = Topic.edge(SdgWritten.class);

    public record SdgWritten(Optional<Path> file, String why) {
    }

    /**
     * A picture's thumbnail: a feed of its own at most {@code ReadPicture.THUMB} px on its longer side, and that size;
     * no feed for a file that is not a picture. Not a message: what {@link Pictured}, {@link Thumbnailed} and
     * {@link HoldThumb} carry.
     */
    public record Thumb(Path path, Optional<Feed> feed, int width, int height) {
    }
}
