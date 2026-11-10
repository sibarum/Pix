# New SDG: a distance field made by hand

Rasterware has only ever looked at pictures and converted them. This is the proposal for its first editor: a **New**
menu, whose first item makes an SDG from nothing, and an editor with three modes for the one field it holds. It is
written before any message, as CLAUDE.md asks, and it touches `Recipes.java`, `Messages.java` and possibly a lane, so
nothing here is built until it is agreed.

## What the person sees

- **A New menu** in the title bar, after the lockup (`TitleBar.menu("new", "New", …)`, as MainFrame and the text editor
  declare theirs). Its first item is **SDG…** (Ctrl+N, in the keymap so *Key bindings…* lists it). Choosing it sends a
  message; nothing runs at the click.
- **The New SDG window**, a second window declared as the calculator declares its settings window
  (`windows.named(...)`):
  - **Size**, in **pixels** (width × height; canvas units are pixels and the field has one texel a unit) or in
    **units** (width × height in mm, in or pt, and a **DPI**, which becomes the stencil's `scale`, texels a unit). The
    texel size is said under it either way (*2480 × 3508 texels*).
  - **Starts** *inside* or *outside*: the field is filled with the far inside or the far outside, so the first stroke
    either cuts away or adds.
  - **Mode**: Heightmap, Stencil or Mask (below). Create and Cancel.
- **The editor**, a third window (an editor beside a viewer, not a mode of the stage). Its mode is a segment in its
  bar; a brush's size, strength and falloff under it; Save writes `.sdg`, never over another file (`Beside`).

### Heightmap

A 3D viewport over the field as terrain: height is the signed value. A **semi-transparent plane** at zero cuts the
space in half, and wherever the terrain crosses it the crossing is outlined: that outline is the drawing's edge.
Brushes raise, lower, smooth and flatten, as a terrain editor's do. Orbit, pan and zoom move the camera only.

What this means for the drawing, and why the mode is worth having: an SDG reader turns distance into coverage over
about one pixel of distance (`sdg.md` §2), so **a slope's steepness where it crosses the plane is that edge's
softness**. A cliff is a sharp edge, a gentle hill a soft one. The field is not normalized, on purpose.

### Stencil

A flat view of the field with a **background image** under it to trace over. Brushes paint inside or outside and
**snap to the image's edges** (the stroke's boundary pulled to the strongest nearby gradient). After each stroke the
field is **normalized to true distance** (re-distanced from its zero crossing, as `MaskSdf` does from a mask), so what
is saved is a correct SDF stencil whose edges a reader draws exactly.

### Mask

The same flat view and optional background image, but what is painted is **alpha**: coverage from 0 to 1, soft where
the brush is soft, never normalized. It is saved as a group's `<mask kind="alpha">`, for transparency rather than an
outline.

## The document

One single-channel field, `width × height` texels, held as floats while it is edited and quantized only on save
(SDG pages are 8-bit PNG, `sdg.md` §4.1, so a heightmap keeps 256 levels across its `range`; the editor says so
when the range is set). Saved as:

| Mode | Written as |
| --- | --- |
| Heightmap | one layer, `stencil kind="sdf"`, `range` the height span chosen, paint the default ink |
| Stencil | the same, re-distanced, `range` 8 texels as the tracer writes |
| Mask | a group with `<mask kind="alpha">` holding one layer that is the alpha |

**Switching mode** on a document already drawn: Mask → Stencil thresholds at 0.5 and re-distances; Stencil →
Heightmap is lossless; Heightmap → Stencil re-distances (the slopes are lost). Each lossy switch says so first.

## Where message passing gets awkward, said before it is worked around

**The field is a large mutable buffer edited in order**, and nothing in Rasterware so far is. Strokes must apply in the
order drawn, which rules out a job per stroke (jobs are in no order at all). A field cannot be a `Viewer.State`
field either: the state is an immutable value replaced each step, and copying 9 M floats a dab is not an option. The
shape that fits the rules is a **component that owns the field** and hears strokes through an inbox in order, applies
each, and publishes the result to the view as a `Feed` (its pixels never cross the bus, as a picture's do not).
Whether a component may hold mutable arrays, and on which lane it should run so a large brush never stalls the
viewer, is a question for the framework, and the first slice will find out. If it may not, that is an FN entry,
not a shared object.

**Heightmap's 3D view needs a heightfield in a marcher.** The designer's `Viewport` (ported from the calculator's
`SdfViewport`) marches a composed SDF scene with a perspective camera and sketches a grid over it; that is the
precedent. Whether `SdfComposer` can march a field sampled from a texture, and the plane's crossing outline, is not
yet known. If not, it is the first stack gap this work finds, and goes in `framework-notes.md` before any
workaround (the fallback, a mesh drawn by the canvas, waits on FN-28's perspective).

## Messages (sketch, to be settled in the first slice)

| Topic | From → to | Record |
| --- | --- | --- |
| `NEW_SDG_TOPIC` | menu → viewer | `NewSdg()`: show the New SDG window |
| `CREATE_SDG_TOPIC` | New window → field owner | `CreateSdg(Sizing size, boolean inside, EditMode mode)` |
| `STROKE_TOPIC` | editor view → field owner | `Stroke(Brush brush, List<Dab> dabs)`, in order |
| `FIELD_SHOWN_TOPIC` | field owner → editor view | `FieldShown(Feed feed, EditMode mode, …)` |
| `SAVE_SDG_TOPIC` / `SDG_SAVED_TOPIC` | editor → job → editor | written beside, as `SaveDrawing` does |

`Sizing` (pixels, or units with DPI) and `Brush` are values. Undo is a field of the owner's (strokes kept since the
last checkpoint), not a message of its own yet.

## Recipes, lanes

`Recipes.java` gains the New SDG window and the editor window, each `windows.named(...)` with its view, and the title
bar's New menu. Possibly one lane for the field owner (above). Each is proposed again, concretely, when its slice
starts.

## Slices, in order

Each from model to screen, with its test and its `ottermate` check.

1. **New → SDG… → the New SDG window → an empty editor**: the menu, the options, the window that opens on Create
   showing the flat field, and Save writing an empty `.sdg` the viewer opens. Settles the field owner's shape.
2. **Mask**: a round brush painting alpha, a background image, Save as an alpha mask.
3. **Stencil**: the same brush painting inside/outside, re-distancing after each stroke, then snapping to edges.
4. **Heightmap**: the 3D viewport, the plane and its outline, raise/lower/smooth/flatten.

## As built: slice 1 (2026-11-10)

New → SDG… → the New SDG window → an empty editor → Save. Built as proposed, with these departures:

- **No `NEW_SDG_TOPIC`.** The New menu's item and Ctrl+N (keymap `file.new-sdg`, so *Key bindings…* lists it) send
  the framework's `WindowCommand.show("new-sdg")` themselves, as MainFrame's Open and the Inspector's graph window are
  opened. Opening a window is presentation, so the viewer is not asked.
- **The field owner is `Sketcher`**, a `@Component(lane = "sketch")` with `@Subscribe` mailboxes, shaped like
  phasearray's `Sound`: its field is a `float[]` in a plain field, which the processor accepts (a component owns its
  state; T2.7 forbids only synchronisation in it). So no FN was needed, and the shape is settled for the brushes.
- **One array for every mode.** The field is stored as a stencil texel is encoded, 1 the far inside, 0 the far outside,
  ½ the edge, and a mask's alpha is the same number. Only `Sketch.drawing`, what is saved, differs by mode.
- **Messages as built**: `CreateSdg(Sizing, inside, EditMode)` new window → `Sketcher`; `Sketched(Feed, width,
  height, Sizing, EditMode)` → editor; `SaveSdg()` editor → `Sketcher`, which asks `PickFile.save` under its own id;
  `WriteSdg(Path, ImageSdg)` → the `WriteDrawing` job; `SdgWritten(Optional<Path>, why)` → `Sketcher`, editor and
  viewer, which shows the new file among its folder's pictures. `Sizing`, `Unit` and `EditMode` are values.
- **Saved as**: a heightmap or stencil, one `sdf` layer painted black in the role `foreground`, `range` 8 texels; a mask,
  a group whose ink layer is masked by the `alpha` of a layer painted from the page. One page: red the field, green and
  blue the far inside (the mask's stencils read green), alpha the field for a mask. A heightmap's range is the
  tracer's 8 texels until slice 4 gives it a height span.
- **Sizes**: pixels or mm, in, pt at a DPI. Changing the unit says the same size in it (pixels whole, lengths to
  hundredths). At most 8192 texels a side and 16.7 million texels (a 64 MB field); past that the window says why and
  Create is greyed.
- **Save asks where** with the OS's save dialog, starting where the OS starts, named *Untitled.sdg*; a name with no
  extension gets `.sdg`. As everything Rasterware writes, a name taken is never written over, even one the dialog
  confirmed: `Untitled (2).sdg`.
- **The mode is shown, not switched**, in the editor's bar (*Mask · 640 × 360 px*). Switching, with its warnings, comes
  with the slices that give the modes something to differ in.

Seen with `ottermate`: the New menu opens with *SDG… Ctrl+N*; its item and Ctrl+N each open the New SDG window; mm shows
the DPI and keeps 1024 × 1024 texels, and Pixels brings back 1024; 640 and 360 typed make *640 × 360 texels*; Outside and
Mask chosen, Create opens the editor titled *Mask · 640 × 360 px* with the field fitted black at 16:9; Esc in the
dialog leaves Rasterware running. **Not seen live: Save**, since ottermate cannot press a native dialog and one opened
by a background run would come up on the desk. `SketcherTest`, `SketchTest` and `ViewerTest` cover asking, cancelling,
writing beside a taken name, what each mode renders as, and the viewer showing the file.

## As built: slice 2, Mask (2026-11-10)

A round brush painting the field, and a picture under it to trace over. As proposed, with these departures:

- **The brush paints Mask and Stencil**, not Mask alone: both are the same flat field, and a stencil's brush is the
  same brush until slice 3 re-distances after each stroke and snaps to edges. Heightmap shows no brush; its brushes are
  slice 4's, in 3D (`EditMode.paints`, `traces`).
- **Messages as built**: `Dab(Brush, x, y, first)` editor → `Sketcher`, one per pointer move, in texels (a
  `@Subscribe(capacity = 1024)` mailbox, since an edge mailbox that overflows is a fault); `Repaint()`, the `Sketcher`
  to itself through the framework's `Timer`; `PickBackdrop()` editor → `Sketcher`, which asks `PickFile.open`;
  `ReadBackdrop(Path)` → the `ReadBackdropPicture` job; `Backdrop(Path, Optional<Feed>, width, height, why)` →
  `Sketcher` and editor. `Brush(radius, strength, hardness, erase)` is a value. The proposal's `Stroke(Brush,
  List<Dab>)` became one dab a message, which needs no batching in the view.
- **Strength caps a stroke.** The first build moved a texel by the strength at every dab, so overlapping dabs piled
  up and a 50 % stroke came out white (seen live). Now a stroke keeps, per texel, its value when the stroke reached it
  and the most of the brush that has reached it since (`Sketch.Stroke`), so a 50 % stroke makes 50 % alpha however
  its dabs overlap, and a second stroke goes half the rest of the way. That costs two more arrays the field's size,
  so the largest field (16.7 million texels) holds about 200 MB while edited.
- **The preview is one feed of three slots for the life of a field**, written again after each dab: only the pixels
  the dab touched are made again, and the whole preview is copied into a free slot. With none free, the `Sketcher`
  asks the `Timer` for a `Repaint` 8 ms later rather than waiting, so a stroke's last dab is always shown.
- **The picture traced over** is fitted inside the field's box at its own aspect, under it; the field is then a shade
  over the outside (near black, 75 % at the far outside) and clear over the inside, rather than grey. It is read by
  its own job on its own topic: `ReadPicture`'s `Pictured` would be taken by the viewer for a picture of its folder.
- **The brush's size** is a slider even in ratio from 2 to 1024 texels across, said as the diameter; strength and
  hardness are percentages. Hardness is the part of the radius at full strength, and a smooth step falls off past it.

Seen with `ottermate`: on a 1024 px Mask field started outside, a stroke drawn with four moves is one unbroken soft line
(a press with no move paints a dab); a 50 % stroke is mid grey; at 576 across, 100 % and hard, a stroke is white with a
sharp edge; Erase cuts through both. **Not seen live: Trace over…**, which opens the OS's dialog, for the reason Save is
not. `SketchTest` and `SketcherTest` cover the falloff, a line with no gaps, the stroke's cap, the partial repaint
matching a whole one, the shade over a picture, the feed written per dab, the slots running out, and the dialog asked.

## As built: slice 3, Stencil (2026-11-10)

A stencil kept a true distance, and a brush that snaps to the picture under it. As proposed, with these choices:

- **Re-distanced when the brush lifts.** A new `Lift()` (editor → `Sketcher`, on the drag's release) settles the stroke:
  the field over what it touched, and `Sketch.REACH` (6) texels round that, is made `½ + d / 8` again, `d` the signed
  distance in texels to the nearest point where the field crosses ½, clamped to the range. Where it crosses is read
  between each texel and its right and lower neighbours by where the line between their values meets ½, so an edge
  keeps its place within a texel; a summed-area table of texels with a crossing skips the search wherever none is near.
  A stroke whose lift was never heard is settled before the next stroke and before a save. Mask is left as painted.
- **What a soft stroke leaves is where it crossed ½**: with a soft brush the edge is where the brush's reach times its
  strength makes ½, so at 100 % half way out along its falloff, and at 50 % only where it was whole. A stroke at under
  50 % from the far outside moves no edge.
- **The preview draws a stencil as a reader would**: its distance in preview pixels plus a half, clamped (sdg.md §2). So
  during a soft stroke the edge shows as soft as it would be drawn if saved then, and it sharpens when the brush lifts.
- **Snap is a colour-aware brush**, not an edge detector. With a picture under a stencil and Snap above 0, each dab
  reads the colour under its middle, and each texel's share of the dab is also how alike its colour is to that one
  (`Sketch.alike`: whole to half the tolerance, a smooth step to nothing at it; Snap 100 % is a tolerance of 0.05 of
  the RGB cube's diagonal, near 0 % it is 0.6). So a stroke spreads over the region it starts in and stops at that region's
  edge, and the re-distancing that follows puts the stencil's edge on the picture's. A stroke that crosses into another
  region paints that one as its middle goes over it. Snap opens at 50 %, and shows only in Stencil.
- **The picture's pixels reach the `Sketcher` through its feed** (FN-37): read once from the still feed the job
  wrote, copied, and sampled where the editor shows it, fitted inside the field's box (`Sketch.Under`).
- **The brush row is narrower** so all four sliders and Trace over… fit the editor's remembered 1040 px width.

Seen with `ottermate`: on a 1024 px Stencil field started outside, a soft brush 1024 across at 100 % paints a broad soft
band while held, and on release the band becomes a sharp edge on the line where it crossed half. **Not seen live:
snapping**, which needs a picture under the field, and so the OS's dialog. `StencilTest` covers a hard disc
re-distanced to within ¾ of a texel of its true distance all round, a soft ramp settling where it crossed ½,
re-distancing a distance changing nothing much, the preview's sharp edge, a letterboxed picture read where it is, a
snapping brush stopping at a picture's edge (and not without Snap), and a lift settling a stencil and not a mask.
