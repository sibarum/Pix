# Layers: peel a drawing apart

The canvas's board *3 · Layers*. A drawing shown (`.sdg`, `.sdgb`) opens a panel listing its layers; the stage draws
each layer as its own node, so one can be picked out, the stack fanned apart, and a solid layer given a theme role,
previewed on a dark page and a light one, and saved into the drawing. The proposal, before `Messages.java` changes.

## What the person sees

- **L**, or a Layers tool in the bar shown only for a drawing, opens the Layers panel at the stage's right (it and
  Trace and Convert are one at a time). Esc or its × closes it.
- **The list**, front at the top: a swatch, the name, its kind (*SDF*, *MSDF*, *group*), its paint (*solid*,
  *sampled*) and its role if it has one. A click picks a layer: the stage draws it whole and the others faint.
- **Picked, a solid layer** shows *Follows theme colour*: *none*, *foreground*, *accent*, *muted* (the board's four,
  SDG's role names, `SdgRoles`). The stage shows the change at once.
- **Peel**, a toggle and a slider: the layers fanned apart down and to the right, back to front, each a step further,
  so what lies under what can be seen. Flat: the board's 3D tilt needs a turn and a perspective the canvas has not got
  (FN-28; `Drawn.turn` is already in the TODO).
- **Light page**, a toggle: the drawing's roles drawn as Rasterware's own light look draws them (`Look.LIGHT`,
  through `SdgRoles.rgba`), on that look's page, whichever look the window is in. Before Rasterware had a light look
  (2026-11-13) this drew them in the framework's `Theme.LIGHT`. In the light look it shows what the window already does.
- **Save roles**, once a role has changed: writes the drawing in place (compiled if it is `.sdgb`), and Rasterware reads
  it again and shows it.
- Left out: the board's distance-field view (no way to draw a layer's field as bands, FN-29) and its opacity slider
  (shown fixed at 100 % on the board, not a setting).

A drawing of more than 32 layers is listed whole and drawn whole: picking and peeling need a node a layer.

## Messages

| Topic | Kind | From → to | Record |
| --- | --- | --- | --- |
| `SAVE_ROLES_TOPIC` | edge | view → viewer | `SaveRoles(Path path, List<String> roles)`: a role for each top-level layer, `""` for none |
| `WRITE_ROLES_TOPIC` | edge | viewer → `RoleDrawing` job | `WriteRoles(Path path, List<String> roles)` |
| `WRITTEN_TOPIC` | edge | job → viewer, view | `Written(path, Optional.of(path), "")`, as Trace and Convert answer |

The viewer holds the write as it holds the others (`State.saving`). A file written **over the picture it came from** is
new to the viewer: it forgets it held it and shows it again once read, rather than listing the folder. The view lets
go of a held picture replaced while shown, once no layer draws it. That also answers the TODO's *held picture can go
stale* for Rasterware's own writes.

## Recipes, lanes, `Doc`

No change. One job, `RoleDrawing`, on `Core`'s list. The panel is a class beside `TracePanel`, built in `Ui`.

## As built (2026-11-08)

Built as proposed, with these departures:

- **The peel fans sheets, not shapes.** Fanned at full size the layers drew over each other as a mush. So while they
  are apart each is drawn smaller (down to 55 % at the slider's end, so the fan stays on the stage) and framed: a thin
  edge for each layer, the accent for the one picked. Together, unframed, they are the drawing. On by default, as the
  board opens.
- **A layer not picked is drawn at 45 %** while one is picked; picked again, none is.
- **Layers shows only for a drawing, Trace only for a raster**: the bar has one of the two tools at a time.
- **The card says the colour and the stencil** (`#24102A · MSDF`); the row says kind, paint and role.
- **The Layers icon is Rasterware's own**, `src/main/icons/small/layers.svg`, baked into `PixIcon`: the suite has none.
- **`Segment.Changed` and `Slider.Changed` are heard once each** by the view and handed to Trace's or Layers' handler
  by address.

Seen with `ottermate` on `dusk.sdg` (5 layers, from the SVG): L opens the panel with the stack fanned; picking *fill 2*
frames the sun in the accent; *accent* draws it in the theme's accent at once and offers Save roles; the light page
redraws the roles as `Theme.LIGHT` has them on its page; the peel off lays them together; Save roles writes
`role="accent"` into the file, Rasterware reads it again, and the panel comes back with the layer still picked.
