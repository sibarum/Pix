# Framework notes

Findings about VexelRay, vexelray-gui, vexelray-codec, Kronometer, tactroller and atchung that came out of building
**Pix**: things the framework does not have, does not document, or does in a way that cost time to discover. Write a
note when the workaround is written, not afterwards, and write down what was measured rather than what was assumed.
[Vexplore's notes](../../vexplore/docs/framework-notes.md) have the format and the markers; Pix uses the same ones.

## The v0.2 port inventory

Written 2026-10-29, before any port, so that what Pix needs is on the table while the API is still moving. VexelRay
0.2 is the message model (`Documents/Spike/ROADMAP.md`): every part talks by messages, a widget says what happened on a
topic, slow work is a `@Job`, and application code never runs on the main thread. Checked against the spike's `v0.2`
branches as they stood: `vexelray-gui` at `904dd98` (phase 3, steps 1 to 18), `vexelray-framework` at `8b17e06`
(phase 4 begun, the template rebuilt) and `atchung` at `aa05b5d`. Phases 4 (the tick, `Outbox`, the processor's
checks) and 5 (the shell) are mostly not built, so what they replace is the roadmap's plan, not code.

**How it was checked.** Copies of `vexelray-codec` (`4940a1e`), `vex-suite-common` (`a08cdac`) and Pix (`0e4ee0c`)
were compiled together, offline, against the spike's installed `0.2.0-SNAPSHOT` jars and atchung `1.4-SNAPSHOT`.
Nothing was installed and no checkout was touched. Each repo's first errors were patched in the copy to reach the
next one, so the compile errors below are the complete list, not the first screenful. Compiling only finds what is
gone. What still compiles but phase 4's checks will refuse (`synchronized`, `volatile`, `java.util.concurrent`,
fields that hold functions) was found by grep.

**Pix is not on the roadmap's phase 8 list, and neither is anything it stands on.** Pix is the smallest part of its
own port. The work is in the two libraries under it, ported in this order:

1. **vexelray-codec.** It has no `v0.2` branch and the roadmap does not mention it. It almost compiles as it is (five
   call sites), but its players are paced by `KronoGui`, which 0.2 no longer gives an application (below).
2. **vex-suite-common.** Its viewer (`PictureViewer`, `Tile`, `Filmstrip`, `Choice`, `UpdateNotice`) is most of
   the port. **Vexplore uses the same viewer**, so this work is Vexplore's too, and the two ports should share it.
   Vexplore's inventory lists `PictureViewer.Host` as "not Vexplore's to port"; it is this.
3. **Pix.** Fifteen files, and a third of it is already the right shape: `Doc` is a value with pure edits.

Line numbers are each repo's `src/main/java` at the commits above.

### What the compiler says

| Repo | Errors | Where |
| --- | --- | --- |
| vexelray-codec | 5 | `ImageView:191, 196, 206, 613`, `VideoView:94`. With those patched, every module compiles, including `-video`, `-record` and `-audio` |
| vex-suite-common | 64 | `PictureViewer` (43), `Tile` (6), `Choice` (4), `UpdateNotice` (5), `Filmstrip` (2), `TerminalScreen` (3), `Glance` (1). The `pty`, `update` and `git` modules compile unchanged |
| Pix | 8 sites | `Recipes:81` (no `KronoGui` is provided; the processor stops here first), then `Opener:47`, `Previewer:83`, and `Recipes:86, 88, 157, 159, 161` |

### Pix: removed outright

| What | Call sites | What replaces it |
| --- | --- | --- |
| `Gui.offload()` | `Opener:47` (the listing), `Previewer:83` (the reader's `Executor`) | A `@Job` per kind of disk work, as the template's `ListFolder` does: a request record and a reply record. `Model.listed` and `Model.previewed` are already the reply halves, and already carry what they were for, so a stale reply is dropped by the same check |
| `keymap.Action.on(Runnable)` | `Recipes:86, 88` (open a file, a folder), `157, 159, 161` (the zoom keys) | `Action.sends(sender, message)`. Opening becomes `OpenFile` / `OpenFolder` messages to whatever owns the dialogs. The zoom is the GUI's own (`Gui.zoomIn` is still public), so its message form is a question for the keymap module, not Pix |
| `KronoGui` as an application part | `Recipes:81`, passed through `Viewer:47` to `PictureViewer` | Nothing provides it any more (gui step 18, part 1: "`KronoGui` leaves a view's reach"). Pix only takes it to hand on, so this goes when the viewer stops wanting it. See the codec's section |

### Pix: still compiles, goes in phase 4 or 5

| What | Call sites | What the roadmap plans |
| --- | --- | --- |
| `Dialogs.openFile`, `openFolder` with callbacks | `Opener:52, 57` | `PickFile` / `Picked` (phase 5). Until then they compile, and the answer arrives on the handler lane, which is gone, so it should be checked that it still arrives at all |
| `Window.close()` from the viewer's Close | `Viewer:125` | Phase 5's close gate: `CloseRequested` / `CloseAnswer` |
| Full screen through `GuiApp.post` | `Recipes:131` (`viewer.screen`), `Viewer:45` (a `volatile` function) | Window state confined to the main thread and changed by message (phase 5). `@MainThread` on `windowBinding` (`Recipes:127`) stays: the processor still has it |
| atchung `State` with a named `Committer`, and `onCommit` listeners | `Model:27–31`, `Model.onChange:66` with its `synchronized` gate at `70` | `Model` becomes a component holding `Doc` on a ledger, the way the template's `Editor` is: each `change(UnaryOperator<Doc>)` call site becomes a message and a step. The gate exists because handlers committed from a pool and could deliver version 6 before 5; on one logic lane that cannot happen, so the gate and the version check go |
| `Viewer.step` committing inside the change | `Viewer:180` | A `Step(delta)` message and a step that reads `Doc.at` itself. The comment's reason (key handlers on a pool, two presses reading the same picture) goes with the pool |
| `Settings.putString(...).save()` from the viewer's host | `Viewer:95, 100` | `Settings.save` is still `synchronized` in vexelray-gui. Writing a file is disk work, so in 0.2 it is a job's, but this is the settings module's question, not Pix's |

### Pix: the reader, which phase 4 would refuse

`Previewer` is the one part of Pix with real concurrency. Every rule below exists because one offload worker
writes what the model's listeners read:

- **`synchronized`:** `Previewer:117, 163, 190, 208`, guarding `last`, `lastPictures`, `wanted`, `asking`,
  `running` and the read-ahead queue.
- **`volatile`:** `Previewer:76` (the read-ahead listener), `Viewer:45`.
- **`java.util.concurrent`:** `Previewer:21, 22` (`Executor`, `RejectedExecutionException`). Its test, `PreviewerTest`,
  builds its own `Executors.newSingleThreadExecutor()`.

The design survives the port; the threading does not. **Latest wins, the file shown always gets an answer, idle
reads ahead**: these are decisions about state, so they become fields of the model's state and steps on it. The worker
becomes a `ReadPicture(path, big)` job, and `Read(path, decoded)` is its reply. **One thing has no precedent:** the
template's jobs run in no order and in parallel, and the reader is deliberately *one* worker with *one* slot, so that
holding an arrow key through a folder decodes only the picture it stops on. In 0.2 that has to be the ledger's
choice: it sends one request, keeps `reading` in its state, and sends the next only when the reply lands. That is
expressible, but no application has done it yet, so it should be written as an FN when it is built.

**The decoded-picture cache needs a home.** `Previewer.kept` (`Previewer:102`) is read by the viewer's filmstrip for
thumbnails (`Viewer:168`), and the job writes it. In 0.2 a part may not hold another across lanes, so either the
reply carries the picture and the view keeps its own thumbnails, or the cache becomes the job's alone and the
filmstrip asks for them by message. The first is simpler. It costs a second copy of each thumbnail's handle, not
its pixels.

### vex-suite-common: the shared viewer

**Ruled 2026-10-29: `dev.sibarum.suite.view` is a widget library.** `Tile` and `Choice` are widgets in all but
name, and `PictureViewer` is a composite widget with its own landmarks. So it may use `Seams` the way
vexelray-gui's widgets do (step 10), and it says what happened as messages, as `Button.PRESSED` does, rather than
calling the code it was given. Where the tables below give two replacements, the `Seams` one is the one to take.
`PictureViewer.Host` is exactly the callback form 0.2 removes:

| `Host` method (`PictureViewer:128–175`) | Becomes |
| --- | --- |
| `step(delta)`, `pick(path)` | `Step` / `Pick` messages to the owner's model |
| `close()`, `open()`, `fullscreen(on)` | `Close`, `Open`, `Fullscreen` messages. The owner (Pix, or Vexplore's second window) routes them to phase 5's close gate, the dialogs and the window |
| `scaled(moving, resting)`, `sounded(volume, muted)` | `Scaled` / `Sounded` messages, which the owner persists through a job |
| `closes()`, `opens()`, `fullscreens()` | Not callbacks, just flags. They become constructor values |

The compile errors and their neighbours, by what replaces them:

| What | Call sites | What replaces it |
| --- | --- | --- |
| `Gui.onClick`, `onDoubleClick`, `onState`, `onResize`, `onResizeUi`, `onDragUi`, `claim(…, Runnable)` | `PictureViewer:548, 551, 561, 625, 627, 628, 637, 643, 1488, 1510, 1724–1727`, `Tile:75–81`, `Choice:53–56`, `Filmstrip:89, 90`, `UpdateNotice:452–455`, `TerminalScreen:464, 472` | `Seams.of(gui).…` if a widget library; `gui.hear(node, NodeEvent.…, stage)` and `gui.claim(node, chord, scope, sender, message)` if not |
| `Tile.onPress(Runnable)` | `PictureViewer:475–481, 514–518, 530, 569, 576` (14; these compile, `Tile` being the suite's own) | `Tile` publishes `PRESSED` / `TOGGLED` carrying its address, as `Button` does. Every tile then needs a name unique in its tree |
| `keymap.Action.on(Runnable)` | `PictureViewer:655–701` (23 actions) | `Action.sends(sender, message)`, each a playback or lens message the viewer hears in its own mailbox |
| `Slider.onChange`, `onCommit` (now package-private) | `PictureViewer:521, 532, 533` (the seek bar, the volume) | The slider's message form |
| `Gui.offload()` | `PictureViewer:623` (`Sharpen`), `Glance:133`, `UpdateNotice:175` | A job each. `Sharpen` takes an `Executor` (`Sharpen:9–11`) and is a resample, so it is the job's body as it stands |
| `InputTopics.INPUT` | `PictureViewer:706`, `TerminalScreen:91` | `gui.input()`: input topics are named per tree (gui `099ebb9`) |
| `Gui.modifiers()` | `PictureViewer:1101` | `Seams.of(gui).modifiers()` (gui step 16), or a modifier state topic if this is not a widget library |

**What phase 4 would refuse:** `PictureViewer` has 16 `volatile` fields (`291–414`), 24 `synchronized` sites and a
`fitLock`. `Filmstrip` has 7 and `Glance` 10. All of them exist because input threads, handlers and the frame
loop met in one object. On a GUI thread that runs the view and nothing else, most go without replacement. Each
`volatile` that input reads "on its own thread" (`PictureViewer:391`) should be checked against where input
lands in 0.2 before it is deleted.

**Not Pix's to port:** `tty/` (`Terminal`, `TerminalScreen`'s reader), `pty-*`'s reader threads, `Pump`, `git/`'s
process threads and `update/`'s `ExecutorService`. They are I/O behind a job, the roadmap's "infrastructure, not an
offence", as Vexplore's `remote/` is. `UpdateNotice`'s update thread (`UpdateNotice:317`) is the same, but its
buttons are view code and port with `Tile`.

### vexelray-codec: playback has no message form yet

The five compile errors are mechanical: `onResize` to `Seams` (`ImageView:191, 196`, `VideoView:94`), `gui.input()`
(`ImageView:206`), and `offload` (`ImageView:613`, which starts a `Playback` on it). With those patched, it compiles.

**The real gap is the clock.** Every moving picture is paced by `KronoGui`: `krono.spork` runs a loop for an
animated image (`ImageView:620`), a video's frames (`Playback:466`, `VideoView:216`) and the viewer's inertia
(`PictureViewer:1020`); `krono.ramp` fades (`ImageView:463`, `PictureViewer:1281, 1570`); `krono.after` settles
and idles (`PictureViewer:1033, 1541`). Gui step 18 took `KronoGui` from applications: a widget runs a `Timing`
on the tree's `Clock`, and a view's own motion is a `Tween`. The ramps and fades map onto that directly. **A video
does not**: it is code that runs on every frame while playing, decides which decoded frame is due, and hands it to
`Textures`. Nothing in 0.2 says how a widget does that. **Pix and the codec's own player are the witness for it**,
as Vexplore is for a declared drop zone.

**Ruled 2026-10-29: the codec's views are a widget library** (`ImageView`, `VideoView`, `Playback`), like
vexelray-gui-widget, and the application says `Play`, `Pause` and `Seek` and hears `Position` and `Ended`. **How a
video is paced is proposed, not ruled:** vexelray-gui's `docs/plans/frame-contract.md` (on the `v0.2` checkout).
Neither reading offered here survived: keeping the loop behind `Seams` keeps the unchecked polling code, and a frame
on the bus each frame lets the logic tick hold a video back. Instead, drawing, presentation and pacing become one
contract: a part's state changes only by declared steps when it is *told* something (`Play`, `Seek`), and a declared,
checked, allocation-free draw is *asked* at emit what to show at the predicted present time (the due slot). The
decoder writes stamped frames into a feed the draw picks from. The plan's *What moves onto it* maps each `KronoGui`
site above.

**Infrastructure, not an offence:** the decoder threads (`Reel:49`, `Movie:96`, `Footage:127`, `VideoGlimpses:75`,
`Recorder:91`) and their locks, `Sound` and `Track`, `Lru`, and `Textures`' posts to the main thread
(`Textures:102–269`), which upload to the GPU where the device lives. `ImageStarter:37` keeps its `@MainThread`.
The codec's demo player (`demos/video-player`) uses `gui.shortcut(…, Runnable)` and `offload`, and ports with the
codec.

### What can be done before the merge

None of the message APIs exist on `develop`, so nothing above can be written against them yet. What survives the
port, written now:

- **Pix:** the reader's decisions as pure functions on a state record (what to ask for next, given what is shown,
  asked for and read), tested without an `Executor`; `PreviewerTest`'s cases are already the right ones. And the
  thumbnail a read lands carried in the reply, so the filmstrip stops reaching into the cache.
- **vex-suite-common:** `PictureViewer.Host` written as a list of message records beside the interface, so both
  owners can be ported against the same list.
- **Rulings, before any code:** that `suite.view` and the codec's views are widget libraries is ruled (2026-10-29);
  how a video is paced waits on `frame-contract.md` being accepted.

Do not move Pix's poms to `0.2.0-SNAPSHOT` before the merge: the spike installs `vexelray-framework` and
`vexelray-gui` at that version into the shared `~/.m2`, and codec and suite-common have no `0.2` build to resolve.
