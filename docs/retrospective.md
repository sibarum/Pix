# Retrospective

What each milestone of **Rasterware** showed when it was read back: the shapes in
[framework-notes.md](framework-notes.md) since the last one, what would stop each, what went wrong in how the work
was done, and what to keep. The notes are written one at a time, as each workaround is written. This file is the pass
over them, and it is where the framework takes its proposals from.

**When.** At the end of every milestone, before the next one starts, and always before a rebuild or an archive. A
milestone with no new findings gets one line saying so. The framework's `docs/developers/retrospective.md` has the
reasons and the full method.

**Never delete a section.** Newest milestone first. A rebuild carries this file over.

## How to write one

    ## Milestone name · 2026-10-22
    
    The count: N findings (FN-a to FN-b), how many of them architectural.
    
    ### The shapes
    | | The promise | What happened | How it looked |
    
    ### What would stop each
    1. One structural change per shape, numbered so a plan can cite it.
    
    ### My own mistakes
    What was trusted, what it turned out to be, and the rule that covers it.
    
    ### What to keep exactly as it is
    What carried the milestone, and why.

## Pictures that play: animations and video, on the frame contract · 2026-11-10

The count: 3 findings (FN-31 to FN-33), none architectural. The architecture was the work itself: vexelray-gui's
frame-contract.md step 5, done in the stack in this milestone (the author's ask), with Rasterware as its witness.

### The shapes
| | The promise | What happened | How it looked |
| --- | --- | --- | --- |
| FN-31 | `mvn exec:exec` runs what was built | `-gui` ran against a days-old `-core` from the local repository, and quit 150 ms in with no reason | ottermate: "the application closed the connection"; looked like a playback fault |
| FN-32 | a feed's hand-over wakes the window | not if its node was only ever drawn at nothing | a picture's first frame never shown, so the widgets look again every 16 ms until it is |
| FN-33 | a feed is shown like a texture | `Node.image(Feed)` has no region | a mirrored pose does not mirror a picture that plays |

### What would stop each
1. FN-31: the stack check refuses to start when the mismatch is between the application's own modules, and says which
   is stale.
2. FN-32: a feed learns whom to wake when a node first names it, not when it is first drawn.
3. FN-33: `Node.image(Feed, ImageRegion)`.

### My own mistakes
- **I trusted "it compiles" from a build that was up to date.** The video-player demo and vex-suite-common both
  "compiled" for me before I changed anything, and both were broken on 0.2 (`@MainThread` refused; `Gui.claim` and
  the rest gone). A compile that did nothing is not a compile. Rule: to know whether something builds, build it from
  clean.
- **I launched without `compile` twice before asking why it failed** (FN-31), and the first time wrote the failure off.
  Rule: a run that ends at its first `await` is a finding, not noise; read `pix.log` first, as CLAUDE.md says.
- **I put the seek track where the bar floats over it.** The bar is the root's last child and floats over the stage, so
  a track laid out after the stage was under it, and the click meant for it hit the bar. Then I drew it in a colour the
  bar is nearly made of. Both were seen only in the shots. Rule: look at the screenshot before saying a control is there,
  and click it through ottermate.
- **`sed` over a file I had not read back** replaced two lines it should not have (an asked half's local, and a guard in
  `clear`), caught only by compiling. Rule: after a pattern edit, grep for every line it touched.

### What to keep exactly as it is
- **The producer keeps the time, on the feed.** `Feed.time` made the asked half a search over three stamps with no clock
  of its own, a pause a time that stops, and a video that follows its sound a producer that says the time again. No
  part's state holds a mutable clock, so nothing had to be argued past the processor.
- **Stamps that only increase, passes on whole multiples.** Seeking back is a floor (`Feed.stale`), and a seek bar's
  place is the feed's time modulo a pass (`Player.positionOf`), so the clock topic the plan had for it was never needed.
- **The job's feed already showing the first frame.** No node swaps when a picture starts playing, and a still and a
  picture that plays are shown the same way until the player's first hand-over.
- **The demo as the meter.** Counting the distinct frames an asked half chose gave exact frame rates (24, 30 and 60 fps
  clips held within a frame a second over twelve runs), which a screenshot cannot.

### Checked live
An animated GIF played red, green and blue at 0.50 to 0.54 s apart, looping. A 30 fps AV1 video filled the window and
played; Space held its frame across two shots and played on; a click in the middle of the track while paused showed
frame 149 (4.98 s of 9.97) within one shot and held it; the GIF after it played at its own size with the playhead a
fifth along. Not heard: a video's sound through the speakers (every run here used `vexelray.sound=nowhere`); its
following the sound is held by `SoundPlayerTest`.

## Details · 2026-11-08

No new findings. Two fields on `Pictured` and a panel: the read already had the file and its thumbnail, so the
modified time and the palette cost a stat and a pass over 192 px. Keep: a panel's words worked out in a pure class
(`Facts`) that a test reads without a window. My mistake: a heredoc lost the backslashes of `\d` and `'\'` in
`Facts`; the compiler caught it. Rule: Java with backslashes goes in through Write, not a shell heredoc.

## Layers · 2026-11-08

The count: 3 findings (FN-28 to FN-30), one architectural (FN-28: the canvas has no turn or perspective).

### The shapes
| | The promise | What happened | How it looked |
| --- | --- | --- | --- |
| FN-28 | the asked half draws any motion a view describes | scale, offset and opacity only: no turn, no depth | a 3D peel drawn as a flat fan of framed sheets |
| FN-29 | SDG is the stack's own format, with tools to see it | nothing draws a layer's field as itself | the board's distance-field view left out |
| FN-30 | a compile error names what is wrong | a stale `-core` jar crashed the processor with an NPE naming nothing | `-pl pix-gui compile` failing where `-am` passed |

### What would stop each
1. FN-28: `Drawn.turn`, then a perspective transform drawn as a quad.
2. FN-29: a `Look` for an SDG layer that paints its distance as bands.
3. FN-30: the processor checks the classes `Core` lists and names the module to build first.

### My own mistakes
- **I fanned full-size layers and called the peel done in my head before I saw it.** It was a mush; framing the
  layers as sheets and shrinking them as they spread is what made it read. Rule: a picture-shaped feature is designed
  from the screenshot, not from the arithmetic.
- **I wrote in the design that the view lets go of a re-read picture's old feed, and did not build it** until reading
  the as-built notes back. Every proposal line is a checklist item: read the proposal against the diff before
  writing "as built".
- **A scripted step assumed the folder's order** (Right from `dusk.sdg` to `shot.png`; it was `dusk.svg`). Rule: a
  script goes to a picture by its cell or by Home and End, not by counting.

### What to keep exactly as it is
- **A file written over itself is read again by the viewer, as state** (`readAgain`): forgotten as held and shown,
  advanced, and shown when read. No new message, and the stale-picture TODO is half closed by it.
- **A node a layer, made once and shown or hidden**, so picking, peeling, roles and the light page are each a paint of
  what is there.

## Convert · 2026-11-08

The count: 2 findings (FN-26, FN-27), one architectural (FN-26: the stack reads every format and writes two).

### The shapes
| | The promise | What happened | How it looked |
| --- | --- | --- | --- |
| FN-26 | a picture viewer on the stack can save what it shows | imagelib decodes all, encodes none; only PNG and SDG can be written | a Convert panel of two formats where the board has six |
| FN-27 | focus follows the person | hiding the focused Save put the ring on Previous | a ring on a control nobody touched |

### What would stop each
1. FN-26: `encode` beside imagelib's `decode`, and an SDG → SVG conversion in `Convert`.
2. FN-27: focus from a hidden node returns to its opener, and a ring is shown only for keyboard focus.

### My own mistakes
- **A perl substitution with `|` as its delimiter and `\|\|` in the pattern** matched the empty string at the start of
  `Ui.java` and wrote a line before `package`. Caught by reading the grep. Rule: a pattern with `||` in it gets another
  delimiter, or goes through Edit.
- **I gave the error line a place in the panel while it was empty**, which drew a gap above Save. Rule: a line that
  is usually empty is hidden while it is.

### What to keep exactly as it is
- **One write at a time, shared.** Trace's save and a conversion are the same `Written` and one `State.saving`, so a
  second never races the first, and the folder is listed and the new file shown by the route Open takes.
- **The panel offers only what the stack can do**, and hands a raster's SDG to Trace rather than tracing with defaults.

## Trace · 2026-11-08

The count: 4 findings (FN-22 to FN-25), one architectural (FN-23, where a view reads a box it also moves).

### The shapes
| | The promise | What happened | How it looked |
| --- | --- | --- | --- |
| FN-22 | the stack's tracer is the gold standard SDG conversion | it has no corner fitting, so the board's Corners has nothing to ask for | a control left off the panel |
| FN-23 | a view can measure what layout did | the reported box includes the drawn offset, and an overflowing child is not centred | a trace drawn 149 px off, then swinging every frame |
| FN-24 | a widget fits wherever it is put | a stretched segment is a dead track with its options at one end | a scripted click that changed nothing |
| FN-25 | the asked half draws what the view says | a second view of the picture is the picture's transform repeated by hand | a fixed shift added in `draw` |

### What would stop each
1. FN-22: a corner option in `Trace`, benchmarked as every conversion is.
2. FN-23: `NodeLayout` reports the laid-out box apart from the drawn one; an overflowing child centres as CSS does.
3. FN-24: a segment shares the width it is given among its options.
4. FN-25: `Drawn.follow(node, leader)`.

### My own mistakes
- **I computed where layout would put a node instead of asking it, then asked it in a way that included my own
  move.** The first compare assumed centring; the second measured the moved box. Rule: a node the asked half moves is
  laid out where nothing about its placement is a guess (a corner of its parent), and is never measured to place it.
- **I wrote a scripted check whose assertion was a guess** (`await trace.after 22` after Flatten; it was 23 KB). The
  screenshot showed it worked, the run said it failed. Rule: await what a step changes in kind (a word, a count the
  step sets), not a number the step only estimates.
- **I nearly proposed a viewer → view relay for every trace**, when the view can hear the job's answer itself. Rule:
  before adding a forwarding message, ask whether the part at the end can subscribe to the topic directly.

### What to keep exactly as it is
- **One trace out at a time, the latest settings next, as state** (`Viewer.Tracer`): a slider dragged across ten stops
  traces twice, and `ViewerTest` holds it with no thread in sight. The reads' pattern carried over unchanged.
- **Preview coarse, Save fine, measured first.** The oversample numbers came from a bench run before any code, so the
  panel answers in under a second instead of five.
- **Save writes beside, never over, and the viewer opens the result by the route Open takes**, so the new drawing is
  listed, shown and stepped to with no new path through the viewer.

## Still pictures on a feed · 2026-10-31

The count: 5 findings (FN-1 to FN-5), one architectural (FN-1, resolved upstream by building the feed).

### The shapes
| | The promise | What happened | How it looked |
| --- | --- | --- | --- |
| FN-1 | 0.2 carries an application's work by messages | the one thing a picture viewer does, pixels made off the GUI thread and drawn on it, had a plan and no code | a viewer that could not be started |
| FN-2 | automation makes an application's failures visible | an uncaught exception ended the run with nothing in the log or the driver | a connection closed during `settle` |
| FN-3 | a port is "regenerate and rebuild" | the template is an editor, so the port began by deleting most of it | 30 files gone before the first line of Pix |

### What would stop each
1. FN-1: a port checklist whose first question is how the application's pixels, sound and files reach the screen in
   0.2, answered before any code.
2. FN-2: the shell logs an uncaught main-thread exception before the shutdown it starts, and `ottermate` prints it.
3. FN-3: a blank shape for the generator, and a version slot.

### My own mistakes
- **I proposed a fix that contradicted the plan.** Finding FN-1, I proposed a picture value that crosses the bus,
  when `frame-contract.md` already said pixels never do and had the feed for it. The author remembered it. Rule:
  before proposing a framework change, search the stack's `docs/plans` for the problem's own words.
- **I gave the engine a ring it refuses.** The feed's first draft made a one-slot `StreamTexture` for a still, which
  the constructor refuses; the tests ran without a device, so only the live run found it. Rule: what reaches the
  device is driven before it is called done, as CLAUDE.md says.

### What to keep exactly as it is
- **One read out at a time, as state.** The viewer's `reading` field is the whole of 0.1's single-slot reader, its
  locks and its executor; `ViewerTest` holds the arrow-key case with no thread in sight.
- **The job answers with the feed.** Decode, mip chain and the copy into the slot happen on the offload lane, and the
  view only names the feed, so nothing about a picture is the GUI thread's until it is drawn.

## Read ahead · 2026-10-31

The count: no new findings. The slice needed nothing from the stack: the feed carried it, and the ordering the bus
already promises (one sender's messages reach a subscriber in the order sent) let the viewer tell the view to hold
a picture and then show it without a handshake.

### What changed shape
- **The view holds pictures; the viewer decides about them by path.** A component's state may not hold a feed, which
  is right: the viewer's decisions (what to read, show, keep, let go) are about paths and sizes, and stay testable
  with no window. `Hold`, `Show` and `Release` are the whole of the view's side.
- **0.1's previewer is gone into three fields.** Its slot, its queue of reads ahead, its LRU and its locks are now
  `reading`, `held` and the order in `AHEAD`, decided in the steps that already existed.

### My own mistakes
- **A test helper threw away the message under test.** `startedOn` cleared everything sent, which now included the
  read ahead the listing started, so five tests failed for the helper's sake. Rule: a helper that forgets what was
  sent forgets only what no test asks about.

### What to keep exactly as it is
- **A step never waits behind more than one read.** Reading ahead uses the one read slot only when nothing is
  wanted, so the arrow-key case is unchanged and `ViewerTest` still holds it.
- **The budget spares the neighbourhood.** Never letting go of the picture shown, the one wanted or its neighbours
  means reading ahead can never chase its own tail; the neighbourhood at its largest is well under the budget.

## Looking closely: zoom and move · 2026-10-31

The count: 1 finding (FN-6, the wheel as a scroll distance), and one member added upstream: `Drawn.scale`, which
frame-contract.md B had already named.

### The shapes
| | The promise | What happened | How it looked |
| --- | --- | --- | --- |
| FN-6 | a view hears what the person did to a node | the wheel arrives as the distance a scroller would move | a zoom per notch that changes with the UI zoom, and no Shift for a fine turn |

### What would stop it
1. FN-6: a wheel event that carries its notches, the pointer and the modifiers, as `DragEvent` carries its own.

### What changed shape
- **0.1's `Lens` stepped a spring on every frame under a lock; this one is a value read at a moment.** A gesture
  records where it started, where it is going and when; the asked half computes the pose from those and the frame's
  time, so it allocates nothing, keeps nothing, and is right on a late frame. Zoom and centre share one easing factor,
  which is what keeps the point under the pointer still: a test reads every 10 ms of an ease and checks it.
- **Nothing is laid out for a zoom.** The picture keeps its fitted box; the asked half draws it larger and elsewhere
  (`Drawn.scale`, `Drawn.offset`). The engine's canvas already scaled and turned exactly; only the contract lacked
  the words.

### My own mistakes
- **I drove the wheel backwards.** The first live script scrolled three notches down and zoomed out, when I meant in.
  The geometry still checked out (the corner landed within a pixel of where the maths put it), so the run was not
  wasted, but the slice's main case went unseen until a second run. Rule: before scripting an input, read which way
  its sign goes in the reference, not the sign that feels natural.

### What to keep exactly as it is
- **The pose is the view's, not the viewer's.** Zoom and pan matter only to drawing, so they never touch the bus
  beyond the chord that asks for a fit; `-core` stays a folder viewer that knows nothing of looking closely.

## The bar · 2026-10-31

The count: 2 findings (FN-8, no ellipsis; FN-9, the window following the zoom), both fixed upstream with a member each
(`Node.ellipsis`, `Gui.zoomSizesWindow`), and FN-7 written the turn before. The layout was planned first
([layout.md](layout.md)); this is its bar's words and sides.

### The shapes
| | The promise | What happened | How it looked |
| --- | --- | --- | --- |
| FN-8 | a label kept to one line is cut off at its edge | cut through a glyph, wherever the box ended | a long name that loses its extension, with half a letter at the end |
| FN-9 | the zoom keeps a window's composition | the window shrank with the controls | Ctrl+- left less room for the picture, not more |

### What would stop it
1. FN-7: a root em read from the system's text size, so words in dp still follow a person who needs them larger.

### What changed shape
- **0.1 clipped its words in a column 10 rem wide; v0.2 gives them the bar.** The name is cut by the layout, in the
  middle, only when the window really is too narrow, and the details go whole, one at a time, measured as they are
  laid out and remembered while hidden, so widening the window again shows them without a frame of guessing.
- **The zoom sizes the controls only.** Words in dp, controls in rem, the framework's own rule turned round on
  purpose; and the window keeps its size, so the picture gets what the controls give up.

### My own mistakes
- **The template's minimum size came through the port unread**: 36 × 20 em, with a reason about a navigator and a
  document side by side. It cut the bar at 420 px in the first live run. Now 24 × 16 em, with Pix's own reason.
- **One narrow shot looked wrong and was not**: the place drawn 30 px late, past the edge. Read again after a pointer
  move, the tree had it where it belongs; the first `tree` and `shot` had caught it sliding into place. Rule: when a
  shot disagrees with the arithmetic, read the tree twice before believing either.

### What to keep exactly as it is
- **The rule for what goes is a function of widths** (`Details.shown`), tested alone, and the view only measures and
  applies it.

## The floating bar · 2026-10-31

The count: 1 finding (FN-10, no opacity for an asked half, and a faded node still hit), fixed upstream with a member
(`Drawn.opacity`) and a rule in the hit test. One more the build caught for me, which is not a finding: a `@Draw`
state holding an array was refused at compile time with the reason, and a `List` was the fix.

### The shapes
| | The promise | What happened | How it looked |
| --- | --- | --- | --- |
| FN-10 | the asked half draws whatever changes with time | it could move, stretch and scale, not fade; and faded, a control still took the click | the fade would have needed a timer in the told half, and a click on the picture could have pressed an invisible Next |

### What changed shape
- **The fade is a value of time, not a timer.** `Fade` says, from when the pointer last moved, how opaque the controls
  are at any moment, and when the next change is due; the asked half draws it and asks to be woken then. Nothing runs
  while the pointer rests, until the moment the fade starts.
- **The bar is the root's last child whichever way it is shown.** Floating it puts it over the stage; unfloating it
  for Keep shown puts it below. Nothing moves between parents, and the stage refits from the resize it already hears.

### What to keep exactly as it is
- **The pointer is heard on the root, and the controls each say the pointer is on them.** The root hears it anywhere
  in the window and that it left; the order the two arrive in within a frame does not matter, because each recomputes
  the fade from both.

### Checked live
At 1000×640, the bar and the arrows shown after a move, gone after a rest with the picture whole; a move onto where
the faded bar was brought it back, and resting on it kept it; Keep shown moved the bar off the picture (the stage
608 → 555 px tall, the picture refitted) and kept it through a rest; off again, it floated and faded; a key step showed
the new name. Not checked live: the pointer leaving the window, which ottermate cannot do, and reduced motion.

## The filmstrip · 2026-10-31

The count: 1 finding (FN-11, no way for a row to say which child gives way first, and no rem in px for a view), not
fixed upstream; Pix sizes the strip itself.

### The shapes
| | The promise | What happened | How it looked |
| --- | --- | --- | --- |
| FN-11 | the layout shares a row's room | grow factors only: no minimum, maximum or shrink | the view measures the bar and a button two rem wide, and sets the strip's width as a percentage |

### What changed shape
- **A thumbnail costs no second decode when its picture is read.** The codec makes every size asked for from one
  decode, so `ReadPicture` asks for the picture's and the thumbnail's together, and only a cell whose picture is not
  read ahead gets a read of its own (`ReadThumbnail`), after the picture wanted and its neighbours, one at a time like
  every read. A step is never queued behind more than one thumbnail.
- **The view tells the viewer how far the strip reaches** (`Span`), so the thumbnails read are the ones there is room
  to show, and the viewer lets go of those more than sixteen past either end.
- **The viewer never touches a feed.** A picture read whole after its thumbnail was read alone sends a second
  thumbnail; the view puts it in the first one's place and closes the old once nothing draws it, as it does pictures.

### My own mistakes
- **Alt+F4 through ottermate does not close a background run.** It answered `ok`, the window stayed, and the person
  found Pix sitting in the notification area and ended it there. Now every live check is `ottermate --launch`, which
  takes the application down when the script ends, and the process list is checked after.
- **Two of my scripts expected the wrong picture from a click**: the middle cell is the picture shown, so clicking it
  does nothing, and cell 1 of nine is three before it. The strip was right both times.

### Checked live
At 1000×640, nine cells, the picture in the middle and marked, `broken.png` an empty cell and the folder's ends empty;
a click on the cell after it showed 5 of 7, and one three before showed 2 of 7. At 640 px five cells, at 440 px three,
and with the controls two steps smaller five again, smaller. Not checked live: a folder long enough for thumbnails to
be let go, which the viewer's tests cover.

## Pinch to zoom · 2026-10-31

The count: 1 finding (FN-12, the pinch only on the window's raw gesture topic), fixed upstream with `NodeEvent.PINCH`.

### What changed shape
- **0.1 read the touchpad's whole repertoire; v0.2 reads the spread and nothing else.** No pan, no turn, no swipes,
  no taps: the person asked for pinch alone. Two fingers swiping still zoom, as the wheel the OS makes of them.
- **A pinch is not eased.** The wheel's zoom eases because a notch is a jump; a pinch is the hand on the picture, and
  an ease would trail it. `Lens.pinched` lands at once, from where the picture is drawn, so a pinch begun while the
  wheel's zoom eases takes over without a jump.

### Checked live
Through ottermate's pretend touchpad, raw contacts and not a ready-made gesture: two fingers spread 2.5× over the
picture zoomed in about the pointer; pinched 0.4× it came back to fitted, and 0.5× more took it to half. Not checked:
a real Windows touchpad, which may also send the pinch as Ctrl+wheel. The view ignores the wheel while a pinch is in
flight for that, but a wheel arriving just after the pinch ends would still zoom; the pretend touchpad sends no wheel,
so whether that happens needs a real pad.

## Open · 2026-10-31

The count: no findings. The framework's `PickFile` / `Picked` did what its docs say: the viewer, on the logic lane,
publishes a `PickFile`, the processor wires it to the framework's dialogs, and the answer comes back by id.

### What changed shape
- **The viewer asks for the dialog, not the view.** 0.1's `Opener` held `Dialogs` and called it from the view; now
  Ctrl+O, Ctrl+Shift+O and a click on the name are one `Open` message, and the viewer sends the `PickFile`, keeps its
  id while it is up (a second Ctrl+O is ignored) and takes the path picked as it takes the command line's: a picture
  read while its folder is listed, a folder on its first picture. Telling the two apart needs no disk, since the
  viewer knows which dialog it asked for.
- **A folder with no pictures shows nothing** (`Clear`), rather than the picture from the folder before under
  *no pictures here*.
- **The name says *Open a picture…*** while nothing is wanted, dim, as 0.1's did.

### Checked live
Started on an empty folder: the name reads *Open a picture…* and is a button in the tree, over *no pictures here*, and
the title is *Pix*. Not checked live: the dialogs themselves. ottermate cannot see or press a native picker, and one
opened from a background run would come up on the desk of the person working there, so Ctrl+O was not pressed. The
viewer's tests cover asking, cancelling, a picture picked, a folder picked and an empty one.

## Full screen · 2026-10-31

The count: 2 findings, both fixed upstream. FN-14, no full-screen command for a part to send, and FN-15, a button's
double click reaching what it sits in.

### What changed shape
- **Full screen is a message.** 0.1 called `app.window().fullscreen(on)` from a recipe; now F11 is a chord that sends
  `WindowCommand.toggleFullScreen(main)`, the stage's double click sends the same, and the view hears
  `WindowFullScreen` to hide the title bar and to know what Esc means.
- **Esc decides in the view.** A chord sends one fixed message, and Esc means two things (leave full screen, or close
  Pix), so it sends `EscKey` and the view picks the command.

### My own mistakes
- **I first fixed the double click in the dispatcher**, making it stop at the nearest node that takes clicks. The
  dispatcher's own test says clicks and double clicks walk apart on purpose, and a list selecting on a click with
  rows opening on a double click needs that. The fix went into the buttons instead.

### Checked live
On a maximized window: F11 went full screen at 1920×1200 with no title bar and the controls faded; Esc came back to the
maximized window with its title bar; two clicks on the stage went full screen again; Next pressed twice quickly
stepped to 3 of 7 and stayed in the window; Esc in the window closed Pix. Not checked: a window closed while full
screen opening as the window it was, which is `NativeWindow`'s to keep and was not changed here.

## Thrown-in pictures · 2026-11-01

The count: no findings. The frame contract's asked half drew it with what was there: `Drawn.offset`, `scale` and
`opacity` on two layers, in closed form.

### What changed shape
- **The side is the viewer's to say.** 0.1's viewer compared the folder positions itself; now `Show` carries
  `side`, worked out where the folder is known, and 0 (simply replace) for a first picture, one opened from elsewhere,
  or one before its folder is listed.
- **Two layers, the newest in front.** The picture is two floated nodes over each other on the stage, swapped at each
  throw and put back under Previous and Next; the `picture` landmark follows the one in front. A feed the viewer lets
  go while the layer flung out still draws it is closed once neither layer does.
- **No lean yet.** 0.1 leaned each picture up to 7° as it flew; a node cannot be turned by the asked half until
  `Drawn.turn`, which R / Shift+R need too.

### My own mistakes
- **A step and a step back, quickly, blanked the stage.** I first did as 0.1 did: whatever is wanted comes in from a
  whole width off and from nothing. The picture going out, wanted back, was in the middle and nearly whole, and
  vanished to be thrown in again. It now comes back from where it had got to, as far as it was shown.

### Checked live
From `file-types-dark.png` a step on, shot as it landed, showed the old picture leaving left and the new one coming in
from the right; a step back and on again, quickly, kept the harbour picture in sight near the middle, settling there.
Steps to and from `broken.png` (not a picture) simply replace, by design. Not checked live: reduced motion, and a
resize mid-throw.

## The filmstrip glides · 2026-11-01

A step glides every thumbnail into its new cell over the throw's 420 ms, out-cubic, as 0.1's filmstrip did: the cells
stay where layout puts them and what is drawn in them moves, so the strip reads as film pulled through.

### What changed shape
- **The shift is worked out from the strips, not the folder.** 0.1's strip knew the folder and the index; v0.2's is told
  only the run it shows (`Filmstrip`), so `Strip.shift` finds a picture on both runs and how far it moved. A jump past
  the strip's reach (Home, End, a click far off) has no picture on both and does not glide: the cells are new pictures,
  not the old ones moved. 0.1 glided those from the strip's edge.
- **Whole cells move, by `Drawn.offset`**, the accent edge with the picture it marks, as 0.1's slots did; hit-testing
  follows the offset, so a click mid-glide lands on what is seen. No framework change was needed.

### Checked live
From `file-types-dark.png` a step on, shot as it went: the newly wanted thumbnail, edged, a cell right of the middle and
easing in, the rest moving with it, and at rest in the middle. One frame mid-glide showed the rightmost thumbnail
stretched tall; not tracked down. Not checked live: reduced motion, and a step taken mid-glide.

## A UI pass: hover, press, and nothing in a frame · 2026-11-01

Three rules from the user: whatever can be clicked shows a pointer and a subtle hover; a click shows at once; nothing
changes in one frame.

### What was already there
The framework fades every colour a node is given (120 ms) and slides what layout moves (`WindowMotion`), so the
widgets' hovers were gradual already. What it does not ease is what an application sets itself: an opacity, a node
shown or hidden, an image or a text swapped.

### What changed
- **The name and the cells** take the pointer's state as the widgets do: the name turns the accent under it, deeper
  pressed; a cell takes a dim edge, ink pressed. The edge is always there, so marking the wanted cell (the accent)
  moves nothing. The wanted cell is not pressed: no pointer and no hover, since a click there went nowhere.
- **Previous, Next and Keep shown** were drawn the same pressed as hovered; fixed upstream (FN-16).
- **`Shade`** eases what Pix shows and hides, through `Drawn.opacity` in the asked half: a cell past the folder's
  end, a thumbnail read (one that only moved with a step's glide does not fade again) and the note. A fade of the
  words when they changed was tried and taken out: the framework's own motion already carried a change of them, and
  that was the better one.
- **A picture with no throw crossfades** (`Toss.crossfade`): a first picture, one opened from elsewhere, a file that
  is not a picture, and a folder with none (`Clear`) fade over the one before, which fades where it was drawn.

### Not done
- **Keep shown and full screen** still change the stage's size in a frame. An ease from the drawn size was tried and
  taken out: layout and the lens take a new stage a frame apart, so the picture popped 6% larger before shrinking
  (FN-18). The title bar's own buttons still light up in a frame (FN-17).
- The details giving way in a narrow bar, and a cell shown or hidden as the window is resized, are still at once.

### Checked live
A hovered cell showed its edge and a pressed one a white edge; the name turned the accent under the pointer; Next
pressed showed a deeper fill than hovered; a step to `broken.png` ended on its note with the picture gone. The
crossfade's middle frames were not caught (the shots lag a frame); `TossTest` holds them.
