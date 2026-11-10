# TODO

Work on **Rasterware** that is known about and not done. Distinct from
[framework-notes.md](framework-notes.md), which is about the framework rather than about this application —
if the fix belongs upstream, it goes there instead.

Keep an entry short enough that it does not need editing, and delete it when it is done rather than ticking it.

The v0.2 port starts from the template, and Pix 0.1's features come back one slice at a time. 0.1's README
(`develop`) says what each did; this list is what is still to bring back, roughly in order.

## Next

- [ ] **Look closely, the rest.** The wheel zoom, the drag and 0 are in (`Lens`). Still to come: R / Shift+R and
      Ctrl+drag to turn, and Shift+wheel for a fine turn (FN-6), never a touch gesture, which need a `Drawn.turn`; H
      and V to mirror, a non-uniform scale the canvas cannot draw yet; and 0.1's feel: a drag past the limit against a
      rubber band that springs back, and a fling that glides. Each is a term in `Lens`'s closed form or a spring solved
      for its moment. `Drawn.turn` also brings back the throw's lean, 7° a whole width out (`Toss`).
- [ ] **The bar, the rest**: the tools and their ⋯ fold come with the tools ([layout.md](layout.md), *Where it stands*);
      Keep shown remembered between runs comes with Rasterware's settings.
- [ ] **Sharp at rest, and the scaling setting**: the eight filters moving and at rest, kept in settings.
- [ ] **The backdrop** behind a transparent picture (Auto, Dark, Light, Checker, ...).

## Trace, the rest (docs/trace.md)

- [ ] **Trace at the picture's own size.** Both the preview and Save trace at 512 px, so a picture larger than that
      loses detail in its drawing (an 883 px screenshot's thin curves step). Save could trace at the picture's size up to
      1024 px, about 15 to 20 s at full quality: then it wants a progress and a Cancel of its own.
- [ ] **The trace shown over the picture drawn by hand** (FN-25): the compare repeats the picture's transform in
      `View.draw`; a `Drawn.follow` would replace it.

## New SDG, the rest (docs/sdg-editor.md)

- [ ] **Mask** (slice 2): a round brush painting alpha, size, strength and falloff; a background image to trace over.
- [ ] **Stencil** (slice 3): the brush painting inside and outside, the field re-distanced after each stroke
      (`MaskSdf`), then snapping to the background image's edges.
- [ ] **Heightmap** (slice 4): the 3D viewport after the designer's `Viewport`, the plane at zero and its crossing
      outlined, raise, lower, smooth and flatten, and the height span saved as the stencil's `range`. First find out
      whether `SdfComposer` marches a field sampled from a texture.
- [ ] **Switching mode** in the editor, warning before a switch that loses something; and undo.
- [ ] **The New SDG window, the rest**: Esc and Enter for Cancel and Create; the save dialog starting in the folder open.

## Waiting on the stack

- [ ] **Pictures that play, the rest.** Animated pictures and videos play, looping, a video with its sound; Space
      pauses, and the seek track along the bar's top is clicked to seek (the view's own, as the lens is). Still to come
      from 0.1: `,` and `.` stepping a frame, and on a video the arrows seeking; the seek preview under the pointer
      (`Motion.glimpses`); the volume and mute, kept between runs; held while the window is not focused; a file that asks
      to play only so many times (`Plays.loops`) stopping after them, its end heard as a `Timer` message. And the native
      build staging FFmpeg beside the executable for video (Later, *Packaging*).
- [ ] **Recording from the camera** (`vexelray-codec-record`, still in `pix-gui`'s pom from 0.1's).
- [ ] **Corners** in the Trace panel, fit sharp or as traced: the tracer has no corner fitting (FN-22). A prototype,
  `CornerTrace`, works for shapes and logos but not yet for small text, and nothing in the app uses it (FN-22 has the
  numbers).
- [ ] **JPEG, WebP, GIF and SVG** in the Convert panel, with the board's knobs: the stack has no encoder for them (FN-26).
- [ ] **Layers, the rest**: the 3D peel (FN-28), the distance-field view (FN-29), a layer's opacity, and roles inside
      a group (only top-level solid layers take one).

## Later

- [ ] **A drawing holds its poster too.** `ReadPicture` answers a `.sdg` with the drawing and, as its feed, the CPU poster
  (up to 1024 px, with its mips), which is only drawn in the filmstrip's thumbnail. The feed could be the thumbnail's
  alone, once `View` stops reading "no feed" as "not a picture".
- [ ] Packaging, as 0.1 had it: `installer.json` (carried over, still at 0.1.2) with its `appId` checked against the
      build's, the file associations, the native builds staging FFmpeg and the recording encoder, the Linux build.
- [ ] **A held picture can go stale.** The viewer holds pictures by path, so a file changed on disk since it was read
      shows as it was until it is let go. Rasterware's own writes over a file (Save roles) read it again (`readAgain`);
      a change made by another program does not. 0.1 kept by the file's `Stamp`; the read could answer with one, and a step
      compare it. Open can reopen the file shown now, and shows it as it was.
- [ ] Vexplore's hand-off (`pix <file>`), and one Rasterware per picture rather than one per hand-off.
