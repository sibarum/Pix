# Layout and input

What Pix v0.2's window looks like and what the hands do in it, agreed before the slices that build it (the filmstrip,
the bar, full screen, the touchpad). 0.1's README says what 0.1 did; this says where v0.2 departs from it and why.

## What was wrong with 0.1's bar

0.1's chrome was the suite's `PictureViewer`, and four things in it made Pix cramped:

1. **One knob sized everything.** Ctrl+= / Ctrl+- was the window's zoom, which scales the root em, and every tile, gap
   and letter was in rem. So the only way to make the controls smaller made the words smaller with them.
2. **The words had a fixed column.** The name, the details and the count sat in a column 10 rem wide that clipped, so a
   long name or a picture's details were cut off however wide the window was.
3. **Fixed widths where room was free.** The seek bar was 16 rem and the volume 6 rem; neither grew into the window.
4. **Big tiles.** Each control was a 4.5 rem tile in a slab with its own padding, so the bar was about 6 rem tall.

## The window

- **The picture has the whole window** under the title bar. The controls float over it and fade when the pointer rests,
  and Keep shown pins them and gives them room of their own, as in 0.1.
- **One slim bar along the bottom**, as tall as two lines of text, not as a tile. Left to right:
  - **The words**: the name on the first line, cut in the middle so its start and its extension both show; under it
    the details (size, file size) and the place (*12 of 340*).
  - **The middle**: the filmstrip, or for a picture that plays its transport, whose timeline grows into whatever the
    bar has spare. This is the part that gives first.
  - **The tools**: Turn, Background, Settings, Keep shown, as icons sized for the bar.
- **Previous and Next** stay at the window's sides, halfway up, smaller than 0.1's tiles.
- **A narrow window gives way in order**: the filmstrip shrinks, then the details drop one at a time, then the tools
  fold into a ⋯ menu. The name and the place go last. Nothing is cut at a fixed edge.

## Sizes

- **Ctrl+= / Ctrl+- / Ctrl+0 size the controls only.** Tiles, icons, gaps and the bar's height are in rem, which the
  window's zoom scales.
- **Text is never zoomed.** Its size is in `dp` (display density, not zoom), so it stays readable however small the
  controls are made. The stack has no reading of the system's text size yet (FN-7); until it has, the text is at one
  size for every Pix.
- A control is never smaller than its words need: the bar's height is the larger of its rem height and two lines.

## Input

- **The wheel zooms** about the pointer: up (on a touchpad, two fingers swiped down) zooms in, down zooms out.
- **Pinch zooms** about the fingers' centre, continuously, as far as the fingers are: spreading zooms in, pinching
  out. Its twist is ignored.
- **Two fingers swiping** are the wheel the OS makes of them, so they zoom as the wheel does. Nothing else is read from
  the touchpad: no two-finger pan, no turn lock, no magnets, no three-finger swipes, no two-finger double tap.
- **A drag moves** the picture; **0** fits it; **R / Shift+R** turn it a quarter, **Ctrl+drag** to any angle,
  **H / V** mirror it. Turning is never a touch gesture.
- Windows may hand a pinch to the window as Ctrl+wheel as well; while a pinch is in flight the wheel is ignored, so it
  is not zoomed twice.

## Still open

- Whether a sideways two-finger swipe (the wheel's `dx`) does anything.
- Whether a click on the name still opens a picture (0.1's Open).
- Whether Keep shown is remembered between runs. It is not yet: Pix has no settings of its own (TODO).

## Where it stands

Built: the bar's words (the name cut in the middle, the details going whole), Previous and Next at the stage's sides,
and the sizes (controls in rem, words in dp, Ctrl+= / Ctrl+- / Ctrl+0, the window keeping its size). The bar floats
over the picture, and it and the arrows fade once the pointer has rested two seconds (`Fade`, drawn through
`Drawn.opacity`, FN-10); they are shown at the start, while the pointer is on one of them, and for a rest after a
step, so a new name is seen. Keep shown is the pin at the bar's right. The filmstrip is the bar's middle: at most half
of it, three to fifteen cells, none below three, so it gives way before the name (`Strip`). Pinch zooms about the pointer
through `NodeEvent.PINCH` (FN-12), at once rather than eased, and the wheel is ignored while a pinch is in flight. Not yet:

- **The transport and the tools** come with what they are for: the transport in the filmstrip's place for a
  picture that plays, Turn, Background, Settings. The ⋯ fold comes with the tools.
