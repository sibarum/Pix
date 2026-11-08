# Trace: a picture traced into an SDG drawing

The canvas's board *2 · Trace* (raster → SDG, split compare), built on vexelray-fields' `Convert.rasterToSdg`
(`Trace.trace`). This is the proposal for its messages, made before they are written, as CLAUDE.md asks.

## What the person sees

- **T** (keymap `view.trace`, "Trace to SDG") or the bar's Trace tool, on a picture shown that is not already a
  drawing, opens the Trace panel at the stage's right. Esc or the panel's Cancel closes it.
- **The stage splits.** Left of a line, the picture as it is; right of it, the trace, drawn natively, so it stays sharp
  at any zoom. The line is dragged to compare. The zoom, the wheel and the drag move both halves together: they are one
  picture drawn twice, the trace clipped to the right of the line.
- **The panel**: Colours (2 to 12, a slider) with the trace's palette as swatches under it; Gradients (*Flattened*, one
  colour a layer, or *Kept as texture*); then Before (the file's size), After (the `.sdg`'s size), Layers. Each change
  traces again; the trace shown stays until the new one lands, marked *tracing…*.
- **Save** writes `<name>.sdg` beside the picture at full quality and shows it. A name already taken is never
  overwritten: `<name> (2).sdg`.

Left out, with a note each: the board's **Corners** (*fit sharp* or *round, as the pixels are*): `Trace.Options` has no
corner fitting (FN-22). The board's fixed zoom stops (100 % … 6400 %) are the bar's zoom, which already has stops.

## Cost, measured

`Trace.trace` on a 1920 × 1280 screenshot scaled to 512 px, 6 colours (scratchpad bench, JIT warm):
oversample 8 (the default) 5.1 s, 4 → 1.3 s, 2 → 0.44 s; 256 px at 8, 1.2–2.0 s; 768 px at 8, 10–20 s.
So the preview traces at **512 px, oversample 2** (under half a second), and Save at **512 px, oversample 8**: the
same regions with their edges cut finer. Both are a `@Job`, on the offload lane.

## Messages (`Messages.java`)

The viewer decides, as it does for reads: one trace out at a time, and the latest settings win.

| Topic | Kind | From → to | Record |
| --- | --- | --- | --- |
| `TRACING_TOPIC` | state | view → viewer | `Tracing(Optional<Path> path, TraceSettings settings)`: what the person wants traced now; empty path = panel closed |
| `TRACE_TOPIC` | edge | viewer → `TracePicture` job | `Trace(long id, Path path, TraceSettings settings, boolean full)` |
| `TRACED_TOPIC` | edge | job → viewer | `Traced(long id, Path path, TraceSettings settings, Optional<ImageSdg> drawing, int layers, long bytes, List<Integer> palette, String why)` |
| `TRACE_SHOWN_TOPIC` | state | viewer → view | `TraceShown(Optional<Traced> traced, boolean tracing)`: the trace to compare, and whether a newer one is out |
| `SAVE_TRACE_TOPIC` | edge | view → viewer | `SaveTrace()` |
| `WRITE_TRACE_TOPIC` | edge | viewer → `WriteTrace` job | `WriteTrace(Path picture, TraceSettings settings)`: traces at full quality and writes beside it |
| `TRACE_WRITTEN_TOPIC` | edge | job → viewer | `TraceWritten(Optional<Path> file, String why)`: the viewer then lists the folder again and wants the new file, as Open does |

`TraceSettings(int colours, boolean texture)` is a value, not a message. `Traced.drawing` crosses as the `ImageSdg`
handle (FN-21's ruling). The viewer's `State` gains `tracing` (wanted), `traceOut` (the id asked and not answered) and
`saving`.

## Recipes, lanes, `Doc`

No change to `Recipes.java` or a lane: the panel is part of `Ui`, the two jobs go on `Core`'s list.
`pix-core` adds `vexelray-fields-convert` for `Convert.rasterToSdg`.

## As built (2026-11-08)

Built as proposed, with these departures:

- **No `TRACE_SHOWN_TOPIC`.** The view hears `Traced` and `TraceWritten` from the jobs itself, as more than one inbox
  may hear a topic: it shows a trace only if it is of the picture shown with the panel open, and says *Tracing…* while
  the settings shown are not the ones the trace it shows was made with. The viewer keeps only paths and settings, as
  it does for pictures. So the view → viewer → job → viewer + view loop has one message fewer than the table above.
- **`Traced` carries the path and settings, not an id**: the viewer matches an answer to the trace it has out by them.
  The viewer's `Tracer` also remembers the trace last asked for, so the same one is not traced twice, and forgets it
  when the panel closes, since the view then lets its drawing go.
- **After** is the size of the XML Save writes (pages inline), not the compiled form: 23 to 34 KB for a 7 KB
  screenshot, said as it is.
- **The jobs are `TracePicture` and `SaveDrawing`**, the second named so as not to share `WriteTrace`'s name.
- **The trace is laid out at the top left of its box** and moved by the picture's fit, not centred and measured
  (FN-23). **The Gradients segment keeps its own width** (FN-24).
- **The line is taken by a drag that starts within a rem of it**, read by the stage, which shows a sideways cursor
  there; it is drawn and never hit, so it cannot take a wheel or a drag from the picture.
- **The bar is in the window's flow while the panel is open**, as Keep shown puts it, so it never covers Save.

Seen with `ottermate` on a 883 × 590 screenshot: T opens the panel and the trace lands in about 1.5 s with the JIT
cold; the trace lies exactly over the picture at Fit and at 156 %; the line drags; 2 colours and Flatten trace again;
Save writes `shot.sdg` and shows it, *2 of 2*; Esc closes the panel and leaves the stage whole.
