# TODO

Work on **Pix** that is known about and not done. Distinct from
[framework-notes.md](framework-notes.md), which is about the framework rather than about this application —
if the fix belongs upstream, it goes there instead.

Keep an entry short enough that it does not need editing, and delete it when it is done rather than ticking it.

The v0.2 port starts from the template, and Pix 0.1's features come back one slice at a time. 0.1's README
(`develop`) says what each did; this list is what is still to bring back, roughly in order.

## Next

- [ ] **Look closely, the rest.** The wheel zoom, the drag and 0 are in (`Lens`). Still to come: R / Shift+R and
      Ctrl+drag to turn, and Shift+wheel for a fine turn (FN-6), never a touch gesture, which need a `Drawn.turn`; H
      and V to mirror, a non-uniform scale the canvas cannot draw yet; and 0.1's feel: a drag past the limit against a
      rubber band that springs back, and a fling that glides. Each is a term in `Lens`'s closed form or a spring solved for its moment.
- [ ] **The bar, the rest**: the tools and their ⋯ fold come with the tools ([layout.md](layout.md), *Where it stands*);
      Keep shown remembered between runs comes with Pix's settings.
- [ ] **A picture thrown in across the window** from the side it came from, the old one flung out.
- [ ] **Full screen**: a double click, F11, Esc. Phase 5 moved window state to messages.
- [ ] **Open**: Ctrl+O, Ctrl+Shift+O and a click on the name, through `PickFile` / `Picked`.
- [ ] **Sharp at rest, and the scaling setting**: the eight filters moving and at rest, kept in settings.
- [ ] **The backdrop** behind a transparent picture (Auto, Dark, Light, Checker, ...).
- [ ] **Pinch to zoom**, about the fingers' centre, the wheel ignored while a pinch is in flight. Nothing else from the
      touchpad: 0.1's two-finger pan, turn lock, magnets, three-finger swipes and double tap are not coming back.

## Waiting on the stack

- [ ] **Animated GIF, WebP and APNG**, played: a feed of three slots, paced by an asked half
      (`frame-contract.md` step 5, the codec on it). Shown by their first frame until then.
- [ ] **Video and its sound**, the transport, the seek preview, the poster in the filmstrip: the same step.
- [ ] **Recording from the camera** (`vexelray-codec-record`, still in `pix-gui`'s pom from 0.1's).

## Later

- [ ] Packaging, as 0.1 had it: `installer.json` (carried over, still at 0.1.2) with its `appId` checked against the
      build's, the file associations, the native builds staging FFmpeg and the recording encoder, the Linux build.
- [ ] **A held picture can go stale.** The viewer holds pictures by path, so a file changed on disk since it was read
      shows as it was until it is let go. 0.1 kept by the file's `Stamp`; the read could answer with one, and a step
      compare it. Matters once Open can reopen the file shown.
- [ ] Vexplore's hand-off (`pix <file>`), and one Pix per picture rather than one per hand-off.
- [ ] Replace the palette anchors in `Look.java` with Pix's own, measured in Oklab.
