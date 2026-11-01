# Pix

Pictures, one at a time, as large as the window allows, and the rest of the folder a key press away.

A VexelRay desktop application on the 0.2 message model, generated from the `vexel-desktop` template and rebuilt as
a picture viewer. This branch (`v0.2`) is the port: it starts again from the template rather than from Pix 0.1, and
the 0.1 viewer's features come back one slice at a time ([docs/TODO.md](docs/TODO.md) lists what is still to come).
Pix is the first application of vexelray-gui's **feed** (`frame-contract.md` D): a job decodes a picture into a
feed, and the feed, not its pixels, is what crosses to the view.

## Run it

From this folder, the parent of the two modules:

```
mvn compile exec:exec
mvn compile exec:exec "-Dapp.args=C:/Users/me/Pictures/holiday/IMG_0412.jpg"
mvn compile exec:exec "-Dapp.args=C:/Users/me/Pictures/holiday"
mvn compile exec:exec -Dautomation=0                      # a driving socket on a free port (see Taking a screenshot)
mvn test                                                  # the viewer, the jobs, the fit
```

`exec:exec` rather than `exec:java`, so native access can be enabled for Panama FFM.

| Command | Opens |
| --- | --- |
| `pix <file>` | that picture, stepping through the pictures in its folder |
| `pix <folder>` | the folder's first picture |
| `pix` | the first picture in the person's Pictures folder, or their home |

The file asked for is shown before its folder has been read; the bar says *reading the folder…* until it
has.

## A native executable

Two builds, made with GraalVM native-image (a GraalVM JDK as `JAVA_HOME`, and Visual Studio with the "Desktop
development with C++" workload):

```
native-build.cmd -Pnative-release package -DskipTests   # pix-gui/target/pix.exe        what ships: no console
native-build.cmd -Pnative package -DskipTests           # pix-gui/target/pix-debug.exe  a console: prints the automation port line
```

Both carry the automation sockets, off until asked for ([Automation](#automation)), so ottermate can drive either.

On Linux there is one build, with no console subsystem or icon step to choose, and plain `mvn`:

```
mvn -Pnative-linux package -DskipTests                  # pix-gui/target-linux/pix
```

`native-build.cmd` is `mvn` inside a Visual Studio developer environment: native-image needs the MSVC `link.exe` and
the icon step needs `rc.exe`, and neither is on `PATH` in a plain shell. Under Git Bash it is worse, since coreutils'
own `link.exe` comes first. The script finds `vcvars64.bat` with `vswhere`, loads it, and runs `mvn` with the
arguments it was given (`./native-build.cmd ...` from Git Bash). Plain `mvn` works too from a developer prompt; from
anywhere else the icon step stops and says to use the script.

This program's own native-image metadata is `pix-gui/src/main/resources/META-INF/native-image/.../reachability-metadata.json`,
and it is short because almost nothing is this program's: the libraries register their own, and the processor
registers the icon. If a native run fails on something the JVM run did not (a `Missing...RegistrationError`), run it
on a JVM under `-agentlib:native-image-agent=config-output-dir=...`, do the thing that failed, and add
only the entries that name this program.

## The icon

`pix-gui/src/main/rc/pix.ico` is the application's mark, and it is one file read three ways: the window and its
taskbar button wear it (`@VexelApp(icon = Pix.ICON)`; the pom puts it on the class path), and so does the
title bar the framework draws; the `.exe` links it (`src/main/rc/pix.rc`), and so Explorer, a shortcut and a
pinned button show it too. Its source is `pix.svg` beside it, the suite icon canvas's Pix mark.

A second window — a viewer, a settings window — wears the window mark, on the taskbar and in its title bar, with
nothing to write: declare it in a recipe with `windows.named(...)`, and the framework builds its tree and claims it
wearing `shell.windowMark()`. That mark is this same one unless
a `pix-window.ico` sits beside it, which is then found by name and registered for the native image with
the mark. The VexelRay suite's applications ship one: a band of the application's colour over its glyph, so a
second window reads as its own and is still told apart from the main one.

To make it yours, draw a 64x64 SVG and render it at every size Windows asks for, 16 to 256 px, into that `.ico` —
`vex-suite-common`'s `tools/Ico.java` does exactly this, from each size's own rasterisation rather than shrinking the
largest. Replace the file, and the window and the executable change together. An `.ico` or `.png` that is missing is
a compile error; one that does not decode costs the window its mark, logged, and nothing else.

Run from `mvn exec:exec`, the process is `java.exe`, so the window and its taskbar button wear the mark but Windows
groups the button with other Java programs, and pinning it pins Java. The native executable has neither problem.

## Using it

| Input | Does |
| --- | --- |
| → / ← and Page Down / Page Up | the next picture in the folder / the one before, stopping at the ends. The picture is thrown in from the side it is on and the one before flung out the other way; a step back mid-throw brings the one going out back from where it had got to. With motion reduced it is simply there |
| Home / End | the folder's first picture / its last |
| The wheel | zoom in (up) or out, about the pointer |
| Two fingers spread or pinched on the touchpad | zoom in (spread) or out, about the pointer, as far as the fingers go. Nothing else on the touchpad does anything but the wheel the OS makes of two fingers swiping |
| Drag | move the picture; it stays where at least a quarter of the window still shows it |
| 0 | fit the picture again, in the middle (every picture arrives fitted) |
| ‹ and › at the window's sides | the picture before / the next one, as ← and → |
| Ctrl+= / Ctrl+- / Ctrl+0 | the controls larger / smaller / as they were. The words stay the size they are, and the window too: what the controls give up is the picture's |
| the pin at the bar's right | Keep shown: the bar and the arrows stay, and the bar takes room of its own below the picture |
| a thumbnail in the bar | that picture |
| Ctrl+O, or a click on the name | open a picture, among the pictures in its folder, through the OS's own dialog starting in the folder open. While nothing is shown the name says *Open a picture…* |
| Ctrl+Shift+O | open a folder, on its first picture. A folder with none shows nothing and says so |
| F11, or a double click on the picture | full screen: the picture has the whole display, the title bar goes, and the controls float and fade as before. F11 or a double click again comes back to the window as it was, maximized if it was. Borderless, not a change of display mode, so switching away is instant |
| Esc | out of full screen; in the window, closes Pix |

The folder is in name order, ignoring case, with numbers compared as numbers (`IMG_9` before `IMG_10`). Every format
imagelib decodes is shown, fitted to the window at its own aspect and never larger than its own pixels; an animated
file shows its first frame for now. A file that is not a picture, or will not decode, says why in its place. Holding
an arrow key decodes the picture it stops on and at most one on the way.

While you look, Pix reads ahead: the next picture, then the previous, then the one after next, so a step finds its
picture waiting. Every picture read is kept, up to 256 MB of pixels, the least recently shown let go first, so going
back is instant too.

The bar along the bottom says the picture's name, cut in the middle when it is too long so its start and its
extension both show, and under it what the picture is and its own size, the file's size, and where it is in the folder
(*12 of 340*). In a narrow window the file's size goes first, then what the picture is; the place stays.

Beside the words, the filmstrip: the picture's neighbours as thumbnails, itself in the middle and marked, filled in as
they are read. It takes at most half the bar, as many cells as fit there, from three to fifteen, so a narrow window
shows fewer and the name keeps its half; smaller controls fit more. A click on one shows that picture.

The picture has the whole window under the title bar: the bar floats over its bottom, and the bar and the arrows fade
when the pointer has rested two seconds, coming back as soon as it moves, or a step shows another name. They stay
while the pointer is on them. With Keep shown on, they never fade, and the bar moves off the picture into room of its
own.

## How it is put together

Two modules under this folder, split where the dependency edge falls, and four kinds of part that never hold each
other: **components** (logic, reached only by messages: the viewer runs on the framework's logic lane), **views**
(presentation, run on the GUI thread in each frame, `@View`), **jobs** (slow work on the offload lane, a request in
and a reply out), and **recipes** (a part built once and handed to whoever takes it). The parts talk by messages, and
every message is in one file. Read these in this order.

**`pix-core`, the controller.** It builds against no widget, so its tests need no window.

| File | What belongs in it |
| --- | --- |
| `Messages.java` | **every message the parts send each other**, grouped by who sends it to whom, each record beside its topic. A record crosses as itself, so it holds only immutable values, and one handle: a `Feed`. |
| `Viewer.java` | **the controller**: which picture is wanted, what is read and when, and which pictures are kept, as declared steps on one immutable state, held by a ledger. One read is out at a time, so a held arrow key decodes only where it stops; idle, that read goes ahead to the neighbours, then to the thumbnails the filmstrip lacks. Its state names pictures by path; the view holds them. It asks for the OS's open dialogs (the framework's `PickFile`) and takes what is picked as it takes the command line's place. |
| `Begin.java`, `ListPictures.java`, `ReadPicture.java`, `ReadThumbnail.java` | the jobs: where to start, a folder's pictures in order, a picture decoded into a feed with its thumbnail from the same decode, and a thumbnail alone. What can go wrong is part of the reply. |
| `Core.java` | the list of this module's component and jobs. The processor compiling `-gui` sees only `-gui`'s sources, so this list, named among `PixApp`'s starters, is how the wiring hears of them. A new component or job goes on it. |

**`pix-gui`, the view and the framework integration.**

| File | What belongs in it |
| --- | --- |
| `Recipes.java` | **what this module builds.** One `@Provides` method per part; `PixAppWiring`, which builds them and the components and jobs in order, is generated while it compiles. |
| `View.java` | **the view**, a `@View`: the picture the viewer chose, fitted to the stage, and the keys declared as the messages they send. It holds the pictures the viewer keeps, as feeds, shows the one it is told to, and closes one when the viewer lets it go. It decides nothing about the folder. How the picture is looked at is its own: each gesture replaces its `Lens`, and its asked half (`@Draw`) draws that on every frame as a scale and an offset, and the controls' `Fade` as an opacity. |
| `Lens.java` | how the picture is looked at, as a value: the zoom and where it was moved, held where it can be seen, eased in closed form so a frame is drawn from the value and its moment alone. |
| `Keys.java` | the chords that ask something of the view itself: 0, to fit, and Ctrl+= / Ctrl+- / Ctrl+0, the controls' size. |
| `Fade.java` | how far the controls are shown, as a value: fading once the pointer has rested, in closed form, drawn by the view's asked half as the lens is. |
| `Toss.java` | a step's throw, as a value: the new picture in across the stage from its side, the one before out the other way, in closed form, drawn by the view's asked half on two layers, the newest in front. |
| `Fit.java` | where a picture sits on the stage: whole, at its own aspect, never past its own pixels. |
| `Ui.java` | the window: the stage, Previous and Next at its sides, and the bar floating over its bottom, or below it with Keep shown. Controls in rem and words in dp, so the zoom sizes the one and never the other. Holds no state; it shows what the view hands it. |
| `Details.java` | the details under the name, which of them a narrow bar keeps (they go whole, the file's size first), and a file's size in words. |
| `Strip.java` | how many thumbnails the filmstrip shows and how wide it is, from the bar's width and a rem, and which picture each cell is. |

`Pix.java` is the entry point and the constants, and nothing else. The application edge — input, the
clipboard, window memory, the clock, the frame loop with its wakes and its pacing, the dialogs, the command line and
the shutdown order — is `vexelray-framework`'s. `Look.java` and `Landmarks.java` are the vocabularies: colour, and
the names an automation script is allowed to depend on.

### Threading, in one paragraph

The GUI loop runs on the main thread, which owns Vulkan, the window and present, and nothing of this application's
runs there but the view, which is presentation and nothing else. The viewer runs on the framework's logic lane, one
message at a time, and its state changes only by its declared steps, which the processor reads: a step that touches
the disk, waits or reaches outside itself does not compile. Every read of the disk is a job on the offload lane, so a
slow disk is a slow reply and never a frozen window. **A picture reaches the screen as a feed**: `ReadPicture`
decodes it, builds its mip chain and writes it into a one-slot `Feed` on the offload lane, and answers with the feed;
the view names it on a node, and the renderer uploads it the first time it is drawn. The pixels are written once and
never cross the bus. **Jobs are in no order at all**, which is why the viewer keeps one read out at a time and drops
an answer it no longer wants.

## The stack

Everything below is installed locally rather than downloaded, so `mvn install` in each of those projects has to have
happened at least once. If the build cannot resolve them, that is why.

| what | why it is here |
| --- | --- |
| `vexelray-framework-api`, `vexelray-gui-core` | all `-core` builds against: the framework's annotations, and the values (a span, a colour, the settings store) and the bus that are safe from any thread |
| `vexelray-framework-shell`, `-automation`, `-processor` | the application edge, the driving socket, and the processor that checks both modules and writes the wiring |
| `vexelray-gui-widget` | the widgets: the `TitleBar`, and the `IconButton`s at the stage's sides |
| `vexelray-gui-krono` | the clock: every transition and cue |
| `vexelray-gui-automation` | driving the running app the way a person does — a debugging instrument |
| `tactroller-atchung` | keyboard and pointer input onto an atchung bus (brings `atchung-core`) |
| `tactroller-clipboard` | the OS clipboard, with no input-subsystem coupling |
| `vexelray-os-*`, `tactroller-*` | picked by an OS-activated profile; exactly one activates |

## Automation

Pix can be driven and read from outside, by a script, a macro or an agent, through the framework's two loopback
sockets: **remote control** (the keys, clicks and reads a person has) and **diagnostics** (the flight recorder, the
log, the Inspector). Both are in every build, the release one too, and both start off.

- **Switched on by hand**: the plug in the title bar opens the switches, one per socket, each with a port (blank for
  a free one).
- **Switched on at launch**, for an agent that means to: `--automation=0` (both, on a free port),
  `--automation.control=<port>`, `--automation.diagnostics=<port>`, or `VEXELRAY_AUTOMATION=0` in the environment.
- **Not there at all**: `--automation.available=none` (or `control`, or `diagnostics`), or
  `VEXELRAY_AUTOMATION_AVAILABLE` in the environment. A socket that is not available cannot be switched on by
  anything, and with neither available the plug is gone.

Whatever turned it on, **the plug's lamps say what each socket is doing**, remote control's (red) left of the plug and
diagnostics' (blue) right:

| Lamp | Means |
| --- | --- |
| none | off, and nothing has connected to it this session |
| ring | open, and nothing has connected yet |
| filled ring | open, and something is connected now |
| ring with a grey dot | open; something was connected and has gone |
| grey dot | off, after something had connected to it this session |

The plug's tooltip and the switches' window say the same in words, with the port and how many are connected.

## Taking a screenshot

Screenshots are `ottermate`'s, and only its: it photographs the running window on the application's own device, so
the picture is right about content as well as chrome. There is no `--capture` flag in this project on purpose.

```
ottermate shot out.png --launch mvn compile exec:exec -Dautomation=0
```

Say what the picture is *of* with options, and `ottermate` sets it first and stops if it cannot. Everything before
`--launch` is ottermate's; everything after it is the application's command line (on Windows, `mvn.cmd`):

```
ottermate --zoom 1.5 --dpi 2 --size 1600x900 shot big.png --launch ...   # pixels
ottermate --size 24emx16em shot smallest.png --launch ...                # the minimum, in em
```

The landmarks in `Landmarks.java` are the names a script uses: `await bar.name IMG_0412.jpg` waits for a picture
to be wanted, and `await bar.place 3 of` for its folder to be listed; `click bar.strip.0` presses the filmstrip's left cell. `ottermate help` lists every verb.

## Logging

The framework configures logging for you, before anything else runs, and the defaults follow what kind of run it is:

| Run | Console | File |
| --- | --- | --- |
| a test | warnings | none |
| from your checkout (`mvn exec:exec`) | info | `pix-gui/target/logs/pix.log`, debug |
| driven by `ottermate` or `-Dautomation` | debug | `…/pix.log`, **trace**, with a probe trace beside it |
| a native image | warnings | `~/.pix/logs/pix.log`, info |

Write to it with a logger, not `System.out` — the jobs do, as `pix.files`:

```java
private static final Log LOG = Log.of("pix.files");   // sibarum.probe.Log
LOG.warn("could not save {}", path, exception);               // the exception goes last; its stack reaches the file
```

`--log=debug` (or `off`) changes the level for one launch, and `-Dlog.level.pix.files=trace` raises one
logger. Levels, settings and the rules for what belongs at which level are in `atchung/docs/logging.md`.

## The three documents

`docs/framework-notes.md` and `docs/TODO.md` start nearly empty and are meant to be filled in as you go. See the note
at the top of the first one — it is the more important of the two. `docs/retrospective.md` is filled in at the end of
each milestone: the pass over the notes that looks for what they have in common, and the one the framework reads
first.
