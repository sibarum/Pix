# Framework notes

Findings about VexelRay, vexelray-gui, Kronometer, tactroller and atchung that came out of building
**Pix** — things the framework does not have, does not document, or does in a way that cost time to
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

*Resolved upstream, 2026-10-31.* Pix's whole job is a picture decoded off the GUI thread and drawn on it, and 0.2
had no form for that. A job's reply could not carry the codec's `Picture` (a `byte[]`, refused by the processor as
an array anyone holding can change), and a view could not show one: `Node.image` takes a device texture only
`GuiApp` can make, the codec's `Textures` hands one back through a `Consumer` (the callback form 0.2 removes), and
`Pixels`, the one device-free handle, is kept until the device goes, which is right for an icon atlas and a leak for
a folder of photographs. The answer was already planned (vexelray-gui's `frame-contract.md` D, the feed) and not
built, so it was built: `Feed` in vexelray-gui-core, `Node.image(Feed)`, `Drawn.show`, and the processor accepting a
feed as a handle (frame-contract.md step 4 has the shape and why it is device-free). `ReadPicture` now answers with
the feed it wrote; nothing in Pix works around anything. **What would have saved the time:** a port's first question
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
`TextFile`, `Markdown`, eight jobs, `Workspace`, `Buffer`, `Navigator`, their tests), deleted before Pix's first
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
only the distance a scroller would move, three em per notch at the turning node's em. So Pix divides by 48 px
(`View.NOTCH_PX`, three em at 16 px), which zooms more or less per notch as the window's UI zoom changes the em; it
hears `NodeEvent.POINTER` on the stage to know where the pointer last was; and it cannot tell Shift from none, so the
fine turn waits. A wheel event that also carried the notches (fractional, for a touchpad), the pointer in client
space and the node's box as `DragEvent` does, and the modifiers, would serve every view that is not a scroller: a
picture, a map, a 3D camera, a timeline.

## FN-7 · Text cannot follow the system's text size, and the zoom sizes text and controls together 🔬

Pix wants its controls sizeable (Ctrl+= / Ctrl+-) without its words getting too small to read. The stack's one size
knob is the window's zoom, which scales the root em, and `Metrics.ROOT_EM_PX` is a constant 16, so nothing reads the
system's text size (Windows' *Accessibility → Text size*). Pix sizes its controls in rem and its text in `dp`, which
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

Pix's Ctrl+- is meant to make the controls smaller so the picture has more room. `GuiWindow` scaled the window's
client area by every zoom step (`ZoomSizing`), so Ctrl+- three times took a 1180×720 window to 604×369: the controls
were smaller and the picture had less room than before. Right for a window sized to its content; wrong for one whose
content is the room left over. **Added upstream**: `Gui.zoomSizesWindow(false)`, which Pix's `Ui` sets.

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
whatever the window. So Pix's view sizes the strip itself: it hears the bar's width, works out the cells that fit in
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
