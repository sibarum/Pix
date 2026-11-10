# Framework notes

Findings about VexelRay, vexelray-gui, Kronometer, tactroller and atchung that came out of building
**Rasterware** — things the framework does not have, does not document, or does in a way that cost time to
discover.

**Why this file exists.** An application built on a framework is the only place its gaps are visible, and they
are visible exactly once: at the moment they are worked around. A workaround with no note beside it becomes a
piece of application code nobody can tell from a design decision, and the framework never hears about it. So
the rule is to write the note *when the workaround is written*, not in a retrospective, and to write down what
was measured rather than what was assumed.

**Before writing a workaround, ask whether it is a component.** If the answer is "every project on this
framework will write these same four calls" — that is a finding, and the fix belongs upstream. Say so here
with that framing, so the [retrospective](retrospective.md) at the end of the milestone has a candidate rather than a
complaint.

## How to write one

    ## FN-1 · One line saying what is missing 🔬
    
    What was wanted, what the framework offers instead, and what was done about it.
    Then: what it costs, and what the framework could do about it.

The markers are a filter, not decoration:

| | |
| --- | --- |
| 🔬 | a framework gap — something upstream could fix |
| 💡 | an idea, not yet a finding |
| 📋 | carried over: needs re-verifying against a newer build before it is repeated |

## Findings

## FN-1 · A decoded picture had no way to the screen 🔬

*Resolved upstream, 2026-10-31.* Rasterware's whole job is a picture decoded off the GUI thread and drawn on it, and 0.2
had no form for that. A job's reply could not carry the codec's `Picture` (a `byte[]`, refused by the processor as
an array anyone holding can change), and a view could not show one: `Node.image` takes a device texture only
`GuiApp` can make, the codec's `Textures` hands one back through a `Consumer` (the callback form 0.2 removes), and
`Pixels`, the one device-free handle, is kept until the device goes, which is right for an icon atlas and a leak for
a folder of photographs. The answer was already planned (vexelray-gui's `frame-contract.md` D, the feed) and not
built, so it was built: `Feed` in vexelray-gui-core, `Node.image(Feed)`, `Drawn.show`, and the processor accepting a
feed as a handle (frame-contract.md step 4 has the shape and why it is device-free). `ReadPicture` now answers with
the feed it wrote; nothing in Rasterware works around anything. **What would have saved the time:** a port's first question
is how its pixels, sound and files reach the screen in 0.2, and nothing asked it until the viewer was being written.

## FN-2 · An exception that ends the application under automation is not in its log 🔬

The first live run stopped during `settle`, and `ottermate` said only that the application closed the connection.
`pix.log`, at trace level, ended with the automation sockets switching off and no error. Run again without
automation, the console said why at once: an `IllegalArgumentException` from `StreamTexture` on the main thread
("a stream needs a slot to show and one to write: 1"), a bug in the feed's first draft. So under automation an
uncaught exception on the main thread reaches neither the log file nor the driver. It cost one run here; for an agent
driving an application it is the difference between a finding and a mystery. The framework could log it before the
shutdown it starts, and `ottermate` could print the application's last error with "closed the connection".

## FN-3 · The template is an editor, so a port starts by deleting it 🔬

Generated, the project is a tabbed Markdown editor: of its 50 files, 30 were the editor's (`Editor`, `Doc`,
`TextFile`, `Markdown`, eight jobs, `Workspace`, `Buffer`, `Navigator`, their tests), deleted before Rasterware's first
line. What was kept (`Recipes`, `PixApp`, `Pix`, `Look`, the poms, `native-build.cmd`, the icon) is the part every
application needs, and some of it still described the editor: `Pix.java`'s usage, `PixApp`'s and `Recipes`' javadoc
(which also called the `View` a `@Component` with a lane of its own, a 0.1 description), and `Look.EMPHASIS`, a
Markdown colour. The generator has no slot for the project's version either, so it started at `0.1.0-SNAPSHOT`.
A `--shape blank` (one window, one view, one component, one job, no editor), and a `--version`, would make
"regenerate and rebuild" the short path it should be for every port still to come.

## FN-4 · Fitting a picture is a size heard and a percentage set 💡

The layout has no aspect-ratio length, so a picture fitted to its stage needs the stage's size (`NodeEvent.RESIZE`,
heard by the view) and its own, and sets its node's width and height as percentages of the stage on every resize
(`Fit`). The codec's `ImageView` said the same in 0.1. Every application that shows a picture writes this; a length
that keeps an aspect inside what it is given would remove it.

## FN-5 · Every run in the spike opens with a wall of "the stack is not the one it was built against" 🔬

With the spike's `v0.2` checkouts installed beside 0.1's in one `~/.m2`, `framework.stack` warns on every start, in one
line some 4,000 characters long, listing every pair of modules whose build ids differ. Each pair is true and none
was the cause of anything here, so the warning is read once and then skipped, which is when it will be missed. It
could name only the repository furthest upstream that differs, which is what its own advice ("reinstall from the
first repository behind these") needs.

## FN-6 · The wheel reaches a view as a scroll distance, with no notches, no pointer and no modifiers 🔬

Zooming about the pointer needs three things from a wheel turn: how many notches, where the pointer was, and whether
Shift was held (0.1 turned the picture with Shift and the wheel). `NodeEvent.WHEEL`'s `WheelEvent(dx, dy)` carries
only the distance a scroller would move, three em per notch at the turning node's em. So Rasterware divides by 48 px
(`View.NOTCH_PX`, three em at 16 px), which zooms more or less per notch as the window's UI zoom changes the em; it
hears `NodeEvent.POINTER` on the stage to know where the pointer last was; and it cannot tell Shift from none, so the
fine turn waits. A wheel event that also carried the notches (fractional, for a touchpad), the pointer in client
space and the node's box as `DragEvent` does, and the modifiers, would serve every view that is not a scroller: a
picture, a map, a 3D camera, a timeline.

## FN-7 · Text cannot follow the system's text size, and the zoom sizes text and controls together 🔬

Rasterware wants its controls sizeable (Ctrl+= / Ctrl+-) without its words getting too small to read. The stack's one size
knob is the window's zoom, which scales the root em, and `Metrics.ROOT_EM_PX` is a constant 16, so nothing reads the
system's text size (Windows' *Accessibility → Text size*). Rasterware sizes its controls in rem and its text in `dp`, which
honours display density and not zoom ([layout.md](layout.md)); that keeps text readable, but at one size for everyone.
A root em read from the system, and a text size that follows it and not the zoom, would serve every application whose
controls and words want different knobs.

## FN-8 · A one-line label could only be cut at its edge, through a glyph 🔬

The bar's name has to fit whatever room is left, and a file's name says most at its start and its extension. A label
kept to a line (`wordWrap(false)`) was cut where its box ended, through whatever glyph was there, and an application
cannot cut it better itself: the glyph advances are the layout's, and a view reads them only a frame late
(`Node.layout()`), from a resize that does not fire when only the text changes. **Added upstream** (vexelray-gui):
`Node.ellipsis(Ellipsis.END | MIDDLE)`. The layout cuts what is drawn, between characters, at the width it settles
on; the node's text, which is the accessible name and what a script awaits, stays whole. A cut label is drawn plain,
since its spans index the whole text.

## FN-9 · The window grew and shrank with the zoom, with no way to keep its size 🔬

Rasterware's Ctrl+- is meant to make the controls smaller so the picture has more room. `GuiWindow` scaled the window's
client area by every zoom step (`ZoomSizing`), so Ctrl+- three times took a 1180×720 window to 604×369: the controls
were smaller and the picture had less room than before. Right for a window sized to its content; wrong for one whose
content is the room left over. **Added upstream**: `Gui.zoomSizesWindow(false)`, which Rasterware's `Ui` sets.

## FN-10 · An asked half could move a node but not fade it, and a faded node was still under the pointer 🔬

The bar fades when the pointer rests, which is a value of time: drawn by the view's asked half (`@Draw`) at each
frame's moment, as the picture's zoom is. `Drawn` could set an offset, a stretch and a scale, not an opacity, and
`Node.opacity` is the told half's, set at a message, not at a frame. And a node faded to nothing still took the click
on what looks like the picture under it, unless it said `hitInert`, which an asked half cannot. **Added upstream**:
`Drawn.opacity(node, opacity)`, multiplied with the node's own, and a node drawn at nothing by it is passed over by the
hit test, subtree and all, so the pointer reaches what is behind it, and an ancestor still hears it move.

## FN-11 · A row cannot say "this child gives way first", and a view cannot ask what a rem is 🔬

The filmstrip and the name share the bar, and the strip is to give way first (layout.md). Flex here has grow, but no
shrink below a basis, no minimum or maximum width, so two children that both want room split it by their grow factors,
whatever the window. So Rasterware's view sizes the strip itself: it hears the bar's width, works out the cells that fit in
half, and sets the strip's width as a percentage of the bar. For that it needs a rem in px, and `Gui` keeps the zoom and
the density in `Seams`, where an application should not reach; so the view measures the Keep shown button, which is
two rem wide, and divides. It works, a frame late after a zoom. **Not fixed**: a `minWidth` /
`maxWidth` (or a shrink factor) would let the layout do it, and a public `Gui.remPx()` would retire the measuring.

## FN-12 · A pinch reached an application only as the whole window's raw gestures 🔬

The wheel reaches the node under the pointer (`NodeEvent.WHEEL`); a pinch reached nothing but the tree's gesture topic
(`Gui.gestures()`), tactroller's `GestureEvent` for the whole window, pans and taps among it, in millimetres and with
the pointer in window coordinates. A view would have subscribed to that topic through `Gui.mailbox`, which says it is
for the framework's widgets, filtered it for pinches, and hit-tested the pointer itself to know whether the pinch was
over its stage. **Added upstream** (vexelray-gui): `NodeEvent.PINCH` with `PinchEvent(phase, scale)`, given whole to
the nearest node under the pointer that asked, chosen when the pinch starts and kept until it ends. The twist and the
drift stay on the gesture topic.

## FN-13 · Every picture was drawn a gamma curve too dark, and a large one's fine detail darker still 🔬

*Fixed upstream, 2026-10-31.* "The gamma looks wrong in some pictures" was two faults, measured with grey bars written at
known sRGB values and read back from `ottermate shot`:

- **Every picture, every format** (vexelray-gui). A picture feed is uploaded `R8G8B8A8_SRGB`, so sampling it gives linear
  light, and the canvas must encode it again for the `B8G8R8A8_UNORM` swapchain. `TreeRenderer` asked for that encode only
  when the node's image *was* a linear `SampledImage`; since frame contract step 4 a node holds the `Feed`, which
  `DeviceImages` turns into its texture only later, so the test never matched and the encode was skipped. Bars at
  16, 48, 80, 112, 144, 176, 208, 240 showed as 1, 8, 20, 41, 71, 111, 160, 222. Dark pictures suffered most, which is
  why it read as "some". The renderer now also takes a feed that is a `picture()` as linear; the same bars now show
  exactly as written, from 8-bit, 16-bit and greyscale PNGs, a JPEG and an Adobe CMYK JPEG alike.
- **Pictures over 2560 px, and every filmstrip thumbnail** (vexelray-codec). `Picture.fit` shrank them by averaging
  the stored sRGB bytes, where `Resample` and the GPU's mips average light. One-pixel black and white lines in a
  5120 px file showed as 128 grey (bytes averaged) rather than 188 (light averaged); a 2560 px file, shrunk only by the
  mips, already showed 188. `fit` now averages in linear light, with `Resample`'s tables. Fine bright detail on dark
  ground (stars, lit windows, text) is where a photograph shows it.

**Not fixed, and not a gamma fault in Rasterware:** imagelib (imagelib-wrapper, the `image` crate) ignores a PNG's gAMA and
iCCP and a JPEG's ICC profile, and hands back the stored values as sRGB. A PNG written linear with gAMA 1.0 or a
linear ICC profile shows dark (1 … 222 for the bars above); a photograph tagged Adobe RGB or Display P3 shows a
little dull rather than with a wrong curve. Colour management belongs there, in the decoder, so every caller of the
codec has it: reading gAMA/cHRM/sRGB/iCCP from the `png` crate and the ICC from the JPEG, and converting to sRGB.

## FN-14 · A part could open, hide and close a window, but not put it full screen 🔬

*Fixed upstream, 2026-10-31* (vexelray-framework). `NativeWindow.fullscreen(boolean)` is there, borderless and keeping
the placement to go back to, and 0.1 reached it through `app.window()`. In v0.2 a part reaches a window only by sending
a `WindowCommand`, whose commands were show, hide, toggle and close, so there was no way to it at all. Added
`WindowCommand.fullScreen`, `windowed` and `toggleFullScreen`, carried out on the main thread between frames, and
`WindowFullScreen(window, on)`, a state topic said after each, so Esc can know whether to leave full screen or close.
It reports what the commands did; a window the OS takes out of full screen by itself is not reported.

## FN-15 · Two quick presses of a button were also a double click on what it sits in 🔬

*Fixed upstream, 2026-10-31* (vexelray-gui-widget). Clicks and double clicks bubble separately, on purpose (a row that
opens on a double click inside a list that selects on a click). But a `Button` or `IconButton` takes only clicks, so
pressing Next twice quickly stepped twice and then handed the double click to the stage, which puts Rasterware full screen.
Both buttons now take their own double clicks and do nothing with them; held by `ButtonTest`, which fails without it.

## FN-16 · An icon button pressed looked the same as one hovered 🔬

*Fixed upstream, 2026-11-01* (vexelray-gui-widget). `IconButton` filled hovered and pressed alike, so a click on Previous,
Next or Keep shown showed nothing of its own before what it did. Pressed is now the hover fill a press step deeper
(`Role.at(PRESSED)`), which the framework's colour fade carries in; held by `ButtonTest`, which fails without it.

## FN-17 · The title bar's buttons lit up in one frame 🔬

*Fixed upstream, 2026-11-01* (vexelray-gui, docs/plans/motion-by-default.md). The framework fades every colour a node
is given, but the title bar drew its plate and glyph into the button's picture (`TitleBar.Caption`), and a picture's
colours are not faded. The plate is now a node with a role background, and the glyph is drawn once per state and shown
by an opacity, which the framework now fades. The same plan made words, images, opacities and `visible()` fade, and
anything clickable shade itself, by default; Rasterware's picture layers, thumbnails and cell names say `paintCuts`, since
the view times those itself (`Toss`, `Glide`, `Shade`).

## FN-18 · A stage resized is drawn a frame out of step with its picture 🧭

*Not fixed.* The picture's layers are sized as a share of the stage, and the view's lens zooms them in its asked half.
When the stage changes size (a window resized, Keep shown, full screen), layout and the lens take the change a frame
apart, so for that frame the picture is drawn at the wrong size: on Keep shown, 1023 px wide, then 1083, then 963. So
Keep shown cannot be eased into yet; an ease made it pop larger before shrinking. What would let it: a node that fits
an image's aspect inside its box itself, or a size change the told half makes and the asked half sees in the same frame.

## FN-19 · A node taken out cannot be put back, so a thing that comes and goes cannot fade 🔬

The splash and the controls it stands in for (Previous, Next, the zoom) were written to be removed and inserted
again, so the framework's presence motion would fade them in and out. `Node.remove()` posts a `Remove`, and the
reconciler releases the node and its subtree on it, so the later `insert` of the same handle finds nothing and does
nothing, with no error. The splash went away with the first picture and nothing came back with the last; the first
check drove only the one direction. They are shown and hidden now (`Node.visible`, which keeps identity), and so come
and go in a frame. The framework's own context menu met the same wall in the same week and fades its opacity by hand
before hiding. What would stop it: a `remove` that refuses a handle used again (a startup error rather than a silent
no-op), and a way to take a node out of view with the presence motion and keep it, `visible(false)` eased as an
add and a removal are.

## FN-20 · The title bar hands the wordmark a paint, and a raster cannot hold one 🔬

`TitleBar.Wordmark.ink` takes a `Paint` (`Role.INK` at rest, `Role.ACCENT` in the pointer's state under it) since
2026-11-05, so a theme swapped later reaches the bar's own text wordmark at draw time. Rasterware's wordmark is a
raster lit in a colour, and a `Feed` has no tint a paint could feed: `Wordmark.bar` resolves the paint against
`gui.theme()` when the bar hands it over and keeps one raster per colour. So after a theme swap the mark stays in the
old theme's ink until the pointer next crosses the lockup.

What the framework could do: a paint-valued image tint on `Node` (`imageTint(Paint)`, multiplied in the shader), so a
mark drawn white once is lit in whatever the theme says, every frame. Then `bar` would hold one white raster and pass
the paint straight through, as the framework's own text wordmark does.

**Fixed upstream 2026-11-08** (SDG plan, slice 5), not as asked but to the same end: the wordmark is a drawing now,
`/wordmark.sdgb`, its face in the SDG role `tint`, and a drawing with no tint of its own takes its node's text colour,
a paint resolved in each frame's theme. So `Wordmark.bar` sets `textColor(ink)` as the text wordmark does, holds one
drawing, and follows a theme swap and the pointer.

## FN-21 · An SDG drawing could not cross the bus, because it holds arrays 🔬

Rasterware opened `.sdg` drawings from the first day codec-image read them, but drew the poster: `ReadPicture`
answered with a `Feed` of the CPU-rendered raster, which is soft at any zoom past its size. Showing the drawing itself
meant answering with codec-image's `Picture.drawing()`, an `ImageSdg`, and the processor refused the reply (T2.4):
`SdgDrawing.pageWidths` is an `int[]`, and the look's values and the pages' `Pixels` are arrays too. A `Feed` crosses
only because `Shareable` names it as a handle.

Fixed upstream on 2026-11-08 rather than worked round: `Shareable` names `ImageSdg` beside `Feed`, as the same kind of
handle, built once by a job and only read by the GUI thread (`anSdgDrawingMayCrossInAPayloadThoughItHoldsArrays`). The
cost is the same trust a feed already has, that its maker never changes it after. What would remove the trust: the
copier T2.4 rules for, or a drawing whose pages are feeds.

## FN-22 · The tracer cannot fit sharp corners 🔬

The canvas's Trace board offers **Corners**: *round, as the pixels are*, or *fit sharp corners*. vexelray-fields'
`Trace.Options` has colours, texels, spread, under, oversample and flatness, and nothing that finds a corner: an edge is
cut on the fine grid and comes out as round as the pixels made it. So the panel has no Corners control (docs/trace.md).
What the stack could do: a corner fit in `Trace` (a region's boundary fitted to lines and arcs where it turns sharply, as
an MSDF keeps its corners), as an option, with its own case in the conversions bench.

**Prototyped (2026-11-10)** in `-core` from parts the stack already has: `CornerFit` (geometry) and `CornerTrace` (each
traced layer's own SDF contoured, fitted, then stored as an MSDF), measured by `CornerTraceBench`
(`mvn -pl pix-core test -Dtest=CornerTraceBench -Dbench=<dir> -Dbench.pictures=<a;b>`). Nothing in the app uses it.

- **No edge detector is needed, and no transform.** The tracer's regions are the edges, and each layer's stencil already
  places its edge between the pixels: contour it at 127.5 (marching squares), a sample at the texel's *centre*
  (measured: half a texel off otherwise). Fourier smoothing would round the very corners this is for.
- **The pipeline:** contour → corners (a turn over ±3 units that stays put at ±6, so a small circle is not one) → the
  two sides extended to meet → each run between corners smoothed (¼ ½ ¼, four passes: the cut grid leaves a ~0.5 px
  stair) and fitted with cubics by least squares (Schneider), 0.2 px tolerance → a `Shape` → `shapeToMsdf` →
  `fieldToSdg(MSDF, …)`, the layers' fields packed on one page.
- **Results**, 6 colours, Save's quality: drawn shapes 16.5 KB traced → 14.1 KB fitted at 0.5 texel a unit, every
  corner of a star and triangle sharp at 4×; the VexelRay logo 252 → 184 KB, its wordmark crisper than traced; fitting
  costs 0.1–1.4 s on top of the trace. **A 512 px screenshot's text does not survive 0.5 texel a unit** (1 px strokes);
  at 1 texel a unit it reads as the trace does but is 877 KB against 369 KB and takes 43 s.
- **What the stack lacks for it, each a gap of its own:**
  1. *`SvgToSdg` writes plain SDF stencils*, though the format has `msdf`: a path drawing's corners stay sharp only down
     to its texels, so a 240-unit drawing took 1024 texels (68 KB) to look sharp. Building MSDF layers by hand meant
     re-packing pages, which `Assembly` (package-private) already does.
  2. *`shapeToMsdf` is brute force over every edge for every texel*: 43 s for the screenshot's 43 k pieces. A spatial
     index (or msdfgen's per-edge bounds) would make it linear-ish.
  3. *`maskToShape` gives few straight lines* (20 for a disc and a triangle), and `sdgToMask`'s per-layer masks did not
     read as one layer each, so neither could be the contour step.
  4. *`svgToSdg` threw `NegativeArraySizeException` in `Cover`* on an SVG with a far-off control point (a fit bug of
     mine, since fixed): a bad path should be a refused path, not an overflow.
- **Still wrong in the prototype:** layers are fitted apart, so at an acute corner the layer below can show a fringe
  past the one above; acute ends of thin strokes still spike a little; rings under 24 units are left exactly as cut, and
  mid-size glyphs are softened by the smoothing. The density should follow the picture's smallest features, not be fixed.
  In `Trace` itself, with the regions' shared boundaries in hand, both edges of a boundary could be one fitted curve,
  which would end the fringes.

## FN-23 · A node's box is reported where it is drawn, so a view that measures what it moves chases itself 🔬

The trace is compared with its picture by drawing it, at the picture's zoom and offset, in a box right of the line
(`TracePanel`). The first way: centre it in that box as the picture is centred in the stage, measure where layout put it
(`NodeEvent.RESIZE`), and move it by the difference. Two things the framework does made that fail:

1. **A child wider than a centring parent is laid out from its start**, not centred about the parent's middle as CSS
   does, so where it lands was not where the arithmetic said, and the trace was drawn 149 px off.
2. **The box a `RESIZE` (and `tree`) reports includes the node's `Drawn.offset`.** Measuring the trace after moving it
   gave the moved box, the next move was computed from that, and it swung between two places every frame; `settle`
   timed out ("frame still owed").

Done instead: the trace is laid out at its box's top left, and moved by what the picture's fit says, measuring nothing.
What the framework could do: report the laid-out box apart from where it is drawn (`NodeLayout.rect()` the layout's, a
`drawn()` beside it), and centre an overflowing child as CSS's `justify-content: center` does.

## FN-24 · A segment stretched to its row leaves its options at the start and the rest dead 🔬

`Segment`'s node in a column that stretches its children is as wide as the column, its options packed at its start and
the rest an empty track that looks pressable and is not: `click trace.gradients` landed in the middle and changed
nothing. It is wrapped in a row so it keeps its own width. What vexelray-gui could do: a segment given a width shares
it among its options, as a tab strip does, or keeps its own width whatever its parent asks.

## FN-25 · Drawing one node as another is drawn is the view's own arithmetic 💡

The trace must move and zoom with the picture exactly, wherever the lens and a step's throw put it. `View.draw` repeats
the picture's scale and offset on the trace's node every frame, plus a fixed shift. Any compare, a before/after, an
onion skin (the Frames board) will want the same. What the framework could do: a `Drawn.follow(node, leader)` that draws
a node with another's transform, so a second view of a picture is one line and cannot fall a frame behind.

## FN-26 · The stack decodes every format a picture comes in and writes two 🔬

The Convert board offers PNG, JPEG, WebP, GIF, SVG and SDG, each with its knobs (quality, chroma, colours, dither).
imagelib decodes all of them and encodes none; the recorder encodes AV1 video only; vexelray-fields writes PNG
(`Png.write`, pure Java, no quality to choose) and SDG. A GUI may not touch AWT (`NoAwtGuardTest`), and a job could
but would drag `java.desktop` into the native image, so ImageIO is no way round it. The panel offers PNG and SDG and
leaves the four out (docs/convert.md), with their knobs.
What the stack could do: an `encode(Frames, Format, Options)` beside imagelib's `decode`, over the same Rust crates
(`image` writes JPEG, GIF and lossless WebP; `libwebp` lossy), returning bytes; and an SDG → SVG conversion in
`Convert` for the vector one, a layer per path where its stencil came from one.

## FN-27 · A focused button hidden hands the focus ring to the window's first control 🔬

Convert's Save is pressed with the pointer, the panel closes (its column `visible(false)`), and the next frame
draws the focus ring on Previous, at the stage's left, which nothing touched. Focus seems to go to the first focusable
node when the focused one is hidden, and to show its ring as if the keyboard had moved it. Not worked round. What
the framework could do: a focus that leaves a node hidden by a pointer press goes nowhere visible (or to the node that
opened the panel, the badge here), and a ring only follows the keyboard.

## FN-28 · No turn and no perspective, so a peel can only fan flat 🔬

The Layers board peels a drawing's layers apart in 3D: the stack tilted back and turned, each layer lifted along the
depth. `Drawn` has a scale, an offset and an opacity, and nothing that turns a node or sets it in perspective (the TODO
already waits on a `Drawn.turn` for R / Shift+R). So the peel fans the layers flat, down and right, smaller as they
spread, each framed as a sheet (`View.peel`, `LayersPanel.frames`). It reads, but it is not the board.
What the framework could do: `Drawn.turn(node, radians)` first, then a node transform with a perspective (a 4×4, or a
tilt about x and a depth), drawn by the canvas as a quad with its corners where the transform puts them.

## FN-29 · A layer's distance field cannot be shown as itself 💡

The board's *Show distance fields* draws each layer's field as bands about its edge. A node draws an SDG as the
drawing it is (`Node.sdg`); nothing draws the field under a stencil (its distance as colour, or isolines). Left out.
What the stack could do: a look for an SDG layer that paints its distance, `Look.distanceBands(spacing)`, as the
effects on SDG already take a look per layer.

## FN-30 · `-gui` compiled alone against an older `-core` crashes the processor 🔬

`mvn -pl pix-gui compile`, after `-core` gained a message but before it was installed, failed with
`NullPointerException: ... "element" is null` from the compiler, naming no file, no class and no message. Built from
the parent with `-am` it compiled. What the framework's processor could do: when a class `Core` lists is not on the
class path as compiled, or lacks what a `@Hears` names, say which and that the module before it needs building.

## FN-31 · `exec:exec` without `compile` runs `-gui` against a stale `-core`, and the window quits in silence 🔬

`mvn -q exec:exec` from the parent, with no `compile` goal, resolved `pix-core` from the local repository (an install
from `df22435`, days old) rather than from the reactor, against the `pix-gui` built from today's sources. The
application logged the stack-mismatch warning (FN-5's: `pix-core is df22435…, but pix-gui was built against
88e695b…`), reached `phase RUN`, and closed 150 ms later with nothing logged as why; `ottermate` only said that the
application closed the connection part-way through the reply to its first `await`. It happened twice before the cause
was looked for, and once looked like a playback fault. With `compile exec:exec` it runs. What was done: every launch
here goes through `compile exec:exec`, as CLAUDE.md's lines already show. What the framework could do: when the
mismatch is between modules of the application itself rather than of the stack, refuse to start and say which module
is stale and that `compile` (or an install) fixes it, rather than warn and then exit without a reason.

## FN-32 · A feed's node drawn at nothing may never hear the hand-over that would show it 🔬

An empty feed draws a white placeholder (`AtlasTexture.placeholder`, 1x1 white), so a picture that plays has its feed's
node drawn at nothing by its asked half until a frame is due. But a feed learns whom to wake when its renderer first
draws it (`Feed.wake`), and a node drawn at nothing may never be drawn, so the first hand-over wakes nobody and the
asked half, answering `Frame.NEVER` (no time, no later slot), is never asked again. The codec's `ImageView` and
`VideoView` look again every 16 ms while a first frame is coming instead. What it costs: a frame a widget asks for
with nothing to show, for the moment a file is opening. What the framework could do: register the wake when a node
first names a feed (`Node.image(Feed)`), or draw an empty feed as nothing rather than as the placeholder, and either
removes the look.

## FN-33 · `Node.image(Feed)` takes no region, so a picture that plays cannot be mirrored 🔬

`ImageView`'s pose mirrors a picture by reading its texture the other way (`ImageRegion` reversed). A feed is shown
with `Node.image(Feed)`, which clears the region, so a picture that plays is shown unmirrored when its pose is
mirrored, where 0.1's stream was mirrored like a still. Rasterware has no mirror yet (TODO, *Look closely*), so it does
not show here; vex-suite-common's viewer does. What the framework could do: `Node.image(Feed, ImageRegion)`, as for a
texture.

## FN-34 · The widget library's seam was there, and nothing said a widget outside vexelray-gui could use it 🔬

Step 5 of vexelray-gui's frame contract needed vexelray-codec's `ImageView` and `VideoView` to answer each frame with an
asked half. `Asked` said it was "registered by the host, never by an application", `Seams.ask` said "the host's", and
frame-contract.md said widgets are parts without saying how one registers its half. So the question put to the author
was whether to build "a door for widgets", which would have restated T7.6 and its checks. The author remembered that
this had come up before with an easy answer: `Seams` is, by its own class Javadoc, the widget library's seam as well
as the host's, and vex-suite-common's `TerminalScreen` (a widget library in another repo) already registers through it.
`ask` was on it all along. What was done: `Asked`, `Seams.ask` and threading.md T7.6 were corrected, and vexelray-gui
gained `docs/reference/seams.md`: which seam is whose, what an application uses instead, how the boundary is held, and
how a widget writes and tests its asked half. Writing it found one more thing: `Seams`'s Javadoc said the processor
refuses a `@View` that imports `spi`, and it does not yet (owed with phase 4's `@View` checks), so `Seams.of` is a
public static an application could call. What it cost: a round of questions to the author, and a proposed change to
the threading model that was not needed. What the framework could do: keep the map current as seams are added (a
test that every public method of `Seams` appears in seams.md's table would hold it), and build the `spi` refusal.
