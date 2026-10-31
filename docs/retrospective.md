# Retrospective

What each milestone of **Pix** showed when it was read back: the shapes in
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
