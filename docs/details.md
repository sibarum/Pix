# Details: what the picture shown is

The canvas's Viewer board, its *Image details* panel: a toggle in the bar's tools (and **I**) opens a panel at the
stage's right with the picture's name and, row by row, **Format**, **Dimensions**, **Aspect**, **File size**,
**Modified** and **Folder**, then its **Palette**, five swatches. It follows the picture shown as the person steps.
The proposal, before `Messages.java` changes.

## Where each comes from

| Row | From |
| --- | --- |
| Format | `Pictured.identity` (*PNG image*, *SDG drawing · 5 layers*), as the bar says it |
| Dimensions | the identity's own size (`883 × 590`), the picture's, not the size it is read at |
| Aspect | those reduced (`3:2`), or to one decimal when they do not reduce small (`1.50:1`) |
| File size | `Pictured.bytes` |
| Modified | **new**: `Pictured.modified`, the file's time, read by `ReadPicture` beside its size |
| Folder | the path's parent, from the home folder (`~/Pictures/Screenshots`) |
| Palette | **new**: `Pictured.palette`, five colours, most of the picture first, from the thumbnail `ReadPicture` decodes anyway |

## Messages

No new topic. `Pictured` gains two fields, filled by the read it already makes:

- `long modified`: the file's last-modified time, ms since the epoch, -1 when it cannot be asked.
- `List<Integer> palette`: up to five colours, `0xRRGGBBAA`, from the thumbnail (at most 192 px), its pixels binned
  four bits a channel and the five fullest bins' means taken, near-equal ones merged. A drawing's is its poster's.

The viewer passes `Pictured` through as it does now; the view keeps it for the picture shown.

## The window

The panel is a third right-hand panel beside Trace and Layers, one of them at a time with Convert. Its toggle is the
suite's `Icon.INFO`, in the bar's tools before Keep shown, shown whenever a picture is.

## As built (2026-11-08)

Built as proposed. Departures: the work of saying each row is `Facts` (pure, with its test), and the panel only shows
it; a value too long for its row (a folder) is cut in the middle, as the bar cuts a name; Details is in the tools for
any picture, beside Trace (a raster) or Layers (a drawing), so the strip leaves room for three tools. The palette is
from the thumbnail `ReadPicture` decodes anyway (`Palette`: 4-bit bins, the fullest five, near ones merged).

Seen with `ottermate`: I on `dusk.svg` gives *SVG image*, *480 × 320 px*, *3:2*, *491 bytes*, its time, its folder from
`~`, and its five colours; stepping to `shot.png` the panel follows (*PNG image*, *883 × 590 px*, *1.50:1*, *7.0 KB*);
T opens Trace in its place, I takes it back, Esc closes it.
