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
