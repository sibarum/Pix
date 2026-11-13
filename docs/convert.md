# Convert: the format badge is the button

The canvas's board *1 · Convert*. The picture's format, at the start of the bar's details, is a button; it opens a
small panel of the formats it can become, and Save writes the picture beside itself in the one chosen, then shows it.
This is the proposal, made before `Messages.java` changes, as CLAUDE.md asks.

## What the stack can write (2026-11-08)

| Target | From a raster (PNG, JPEG, WebP, GIF, …) | From an SVG | From an SDG drawing |
| --- | --- | --- | --- |
| **PNG** | re-encoded at its own size (`Png.write`, pure Java) | rasterized at its own size (`Convert.svgToRaster`) | rendered at its own size (`Convert.sdgToRaster`) |
| **SDG** | traced: opens the Trace panel, which already is this conversion's fine-tuning | converted as paths, not traced (`Convert.svgToSdg`) | it is one |
| JPEG, WebP, GIF, SVG | no encoder in the stack: imagelib only decodes, the recorder only writes AV1 (FN-26) | | |

So the panel offers **PNG** and **SDG** (marked *new*), the picture's own format marked as what it is. The four the
stack cannot write are left out rather than shown dead; FN-26 and the TODO say what would bring them. The board's
per-format knobs (*Quality*, *Chroma*, …) belong to those encoders; PNG has none worth a control, and SDG's are the
Trace panel's.

## What the person sees

- **The badge**: `PNG ⇄` at the left of the bar's details, a button. A click (or **C**) opens the Convert panel over
  the bottom of the stage, above the badge; Esc or a second click closes it.
- **The panel**: a tile per format, the current one marked; under them one line on what the chosen one does ("Same
  pixels, re-saved: lossless" / "Traced into layers, sharp at any size" / "Its paths, exactly") and **Save
  `<name>.<ext>`**. SDG from a raster closes this panel and opens Trace instead.
- **Saved**, the new file is listed with its folder and shown, as Trace's Save does; a name taken gets ` (2)`. Not
  saved, the panel says why.

## Messages

Trace's save and Convert's are one shape: a file written beside a picture, then shown. So `TraceWritten` becomes
`Written`, heard from both jobs, and the viewer's one-save-at-a-time is shared.

| Topic | Kind | From → to | Record |
| --- | --- | --- | --- |
| `CONVERT_TOPIC` | edge | view → viewer | `Convert(Path path, Format to)` |
| `WRITE_COPY_TOPIC` | edge | viewer → `ConvertPicture` job | `WriteCopy(Path path, Format to)` |
| `WRITTEN_TOPIC` (was `TRACE_WRITTEN_TOPIC`) | edge | `ConvertPicture`, `SaveDrawing` → viewer, view | `Written(Path picture, Optional<Path> file, String why)` |

`Format` is an enum in `-core` (`PNG`, `SDG`) with its extension and the line the panel says; it decides from a path
whether it can be the source (`Format.of(path)`). The viewer's `Tracer.saving` moves up to `State.saving`, which a
trace save and a convert both hold.

## Recipes, lanes, `Doc`

No change to `Recipes.java` or a lane. One job, `ConvertPicture`, on `Core`'s list.

## As built (2026-11-08)

Built as proposed, with these departures:

- **The badge is a word in a plate**, not a widget button, at the bar's left before the name: `SVG`, `PNG`, `SDG` for a
  drawing, any other format by its extension (`JPG`, `WEBP`). Hidden for a file that is not a picture.
- **The tiles are toggle buttons** (heard as `Button.Toggled`); the picture's own format is shown disabled as
  *`SDG · now`*. A PNG's own tile is never offered: re-saving it as itself is nothing.
- **What the panel says** is per source: *Drawn at its own size, as pixels* (an SVG or a drawing to PNG), *Every pixel
  kept, saved as PNG* (a raster), *Its paths and colours, exactly* (an SVG to SDG), and for a raster to SDG *Traced
  into layers of colour… Trace has the settings*, with **Trace…** for Save.
- **Opening one panel closes the other**: C closes Trace, T closes Convert. Esc closes whichever is open first.
- **`ConvertPicture` writes to `<name>.part` and moves it into place**, since `Files.write` alone would leave half a
  file listable; `Beside.free` is shared with Trace's save.
- An SVG becomes a drawing at 1024 texels across: a 491-byte SVG makes a 57 KB `.sdg` (its stencils are fields).

Seen with `ottermate`: `dusk.svg` → SDG made `dusk.sdg`, 5 layers, shown; that drawing → PNG made `dusk.png`, 480 × 320,
shown; `shot.png`'s SDG tile opened Trace with 6 layers. FN-27: after Save closed the panel, Previous drew a focus ring.

## Video: a picture that plays, as AV1 in WebM (2026-11-12)

One destination for anything that moves, and the one the stack can write: AV1 in WebM, through the recorder's encoder
(`Recording`, ffmpeg-wrapper-record). WebP was the other candidate and has no encoder (FN-26). Sound is ignored both
ways: a picture has none, and a video made a video is written without its own.

- **The tile**: *Video*, offered for an animated GIF, WebP or APNG, and chosen first when the panel opens on one. A
  video's own `.webm` is its current format. Video sources are slice 2 (below).
- **What is written**: every frame at the picture's own size, each for as long as Rasterware shows it (`Movie.timed`,
  so a 0 ms GIF delay is 100 ms), the last held to its end. It plays once: a video cannot ask to loop.
- **Transparency**: a video has none, so a picture whose first frame has a pixel not wholly opaque
  (`Plays.transparent`, found by `ReadPicture` from the frame it already decodes) shows a *Background* row: a swatch
  and its hex, black by default, whose button opens the stack's `ColorPicker` (alpha hidden) inside the panel. Each
  frame is laid over that colour as a browser draws it. The colour is kept between pictures for the run.
- **Messages**: `Format.WEBM`; `Convert` and `WriteCopy` carry `background` (`0xRRGGBB`, ignored by PNG and SDG);
  `Plays` carries `transparent`. `ConvertPicture` hands a video to `MotionVideo`, which reads frames one at a time
  (imagelib's `Animation`), so only one canvas is held. The part file is `<name>.part.webm`, since the encoder picks its
  container by the name, and it is deleted when the write fails.

Seen with `ottermate` on a 160 × 96, 12-frame GIF with a transparent background, every default untouched: the panel
opened on *Video* with *Background #000000*, Save wrote `ball.webm` and showed it, playing. Not yet seen: the picker
opened and a colour chosen, because the stack installed at the time was out of step with itself (a click on the
Background button stopped the app with `NoClassDefFoundError: DragEvent$DragEnded`, from a `vexelray-gui` built ahead
of the `tactroller` installed). Save's label says `ball.webm` when that name is taken and `ball (2).webm` is what is
written, as it does for PNG and SDG.

**Slice 2, video sources**: frames read one after another with no clock, which codec-video does not offer (`Clip`'s
decoder is package-private; `Reel` drops late frames; `Footage` is for playing), and a progress and Cancel, which a
`@Job` cannot give: it answers once and cannot be cancelled. Animated pictures take well under a second, so slice 1
needs neither.
