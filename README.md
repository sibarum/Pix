# Rasterware

Pictures, one at a time, as large as the window allows, and the rest of the folder a key press away. Animations and
videos play in place, and a picture can be traced into a drawing, converted, or taken apart into its layers.

A VexelRay desktop application, generated from `vexelray-framework`'s `vexel-desktop` template and rebuilt on the 0.2
message model. It starts again from the template rather than from Pix 0.1, whose features come back one slice at a
time: [docs/TODO.md](docs/TODO.md) lists what is still to come. Rasterware is the first application of vexelray-gui's
**feed** (`frame-contract.md` D): a job decodes a picture into a feed, and the feed, not its pixels, is what crosses to
the view.

- [Run it](#run-it)
- [Using it](#using-it): looking, the bar, pictures that play, the tools
- [How it is put together](#how-it-is-put-together): the parts, the files, threading
- [Building it](#building-it): native executables, the marks
- [Driving it from outside](#driving-it-from-outside): automation, screenshots, logging
- [The stack](#the-stack) and [the documents](#the-documents)

## Run it

From this folder, the parent of the two modules. The stack is installed locally, not downloaded ([The stack](#the-stack)).

```
mvn compile exec:exec                                                # the Pictures folder
mvn compile exec:exec "-Dapp.args=C:/Users/me/Pictures/IMG_0412.jpg" # that picture, among its folder's
mvn compile exec:exec "-Dapp.args=C:/Users/me/Pictures/holiday"      # that folder's first picture
mvn compile exec:exec -Dautomation=0                                 # with its automation sockets open
mvn test                                                             # unit tests, and the window tree built headless
```

`exec:exec` rather than `exec:java`, so native access can be enabled for Panama FFM.

| Command line | Opens |
| --- | --- |
| `pix <file>` | that picture, stepping through the pictures in its folder |
| `pix <folder>` | the folder's first picture |
| `pix` | the first picture in the person's Pictures folder, or their home |

The file asked for is shown before its folder has been read; the bar says *reading the folder…* until it has. With
nothing named, most first runs show the splash: Windows keeps pictures in folders under Pictures (Screenshots, Camera
Roll) rather than in it. The splash is the mark and the wordmark, and an *Open a picture…* button that does what Ctrl+O
does.

## Using it

### Looking

| Input | Does |
| --- | --- |
| → / ←, Page Down / Page Up | the next picture in the folder / the one before, stopping at the ends |
| Home / End | the folder's first picture / its last |
| ‹ and › at the window's sides | as ← and → |
| The wheel, or two fingers on the touchpad | zoom in (up, or spread) or out, about the pointer |
| Drag | move the picture; it stays where at least a quarter of the window still shows it |
| 0 | fit the picture again, in the middle. Every picture arrives fitted |
| Ctrl+O, or a click on the name | open a picture, through the OS's own dialog, starting in the folder open |
| Ctrl+Shift+O | open a folder, on its first picture. A folder with none says so |
| F11, or a double click on the picture | full screen, borderless; again to come back, maximized if it was |
| Esc | close whichever panel is open; else leave full screen; else close Rasterware |
| Ctrl+= / Ctrl+- / Ctrl+0 | the controls larger / smaller / as they were. Words keep their size; the picture gives up the room |

Every window-wide command is in the keymap, so the framework's *Key bindings…* lists it and the person can change its
chords.

The folder is in name order, ignoring case, numbers compared as numbers (`IMG_9` before `IMG_10`). Every format
imagelib decodes is shown, at its own aspect and never larger than its own pixels; a file that is not a picture, or will
not decode, says why in its place. A step throws the new picture in from its side and flings the one before out the
other way; anything else that appears or goes fades over 150 ms rather than changing in a frame. With motion reduced,
it is simply there.

While you look, Rasterware reads ahead: the next picture, then the previous, then the one after next. Every picture
read is kept, up to 256 MB of pixels, the least recently shown let go first, so going back is instant too. Holding an
arrow key decodes the picture it stops on and at most one on the way.

### The bar

The bar floats over the bottom of the picture, and it and the arrows fade once the pointer has rested two seconds,
coming back when it moves. Left to right:

- **The format badge** (`PNG`, `SVG`, `SDG`), a button that opens Convert.
- **The name**, cut in the middle when too long so its start and its extension both show, and under it what the picture
  is, its file's size, and its place in the folder (*12 of 340*). A narrow window drops the file's size first, then
  what the picture is; the place stays.
- **The filmstrip**: the neighbours as thumbnails, the picture in the middle and marked, three to fifteen cells in at
  most half the bar. A click shows that picture; a step glides the thumbnails into their new cells.
- **The zoom**, − and + stepping between stops of the picture's own pixels.
- **The tools**: Trace for a raster or Layers for a drawing, and Details.
- **The pin**, *Keep shown*: the bar and the arrows never fade, and the bar takes room of its own below the picture.

### Pictures that play

An animated file (GIF, WebP, APNG) plays, looping, at its own pace, and so does a video (anything FFmpeg decodes),
with its sound, filling the window. **Space** pauses either, and the thin track along the top of the bar is clicked to
seek.

### The tools

Each opens a panel, at the stage's right or, for Convert, just above the badge; one at a time. Each was proposed in its own document before its messages were
written, and that document says what was built and why.

| Tool | Key | Does | |
| --- | --- | --- | --- |
| **Trace** | T | traces a raster into an SDG drawing. The stage splits: the picture left of a line, the trace right of it, drawn natively so it stays sharp at any zoom, the line dragged to compare. Colours and gradients are set in the panel; Save writes `<name>.sdg` beside the picture | [trace.md](docs/trace.md) |
| **Convert** | C, or the format badge | writes the picture beside itself as PNG, or as SDG (an SVG by its paths; a raster by Trace). JPEG, WebP, GIF and SVG wait on encoders the stack lacks | [convert.md](docs/convert.md) |
| **Layers** | L, for a drawing | lists a drawing's layers; one is picked out, the stack peeled apart, a solid layer given a theme role, shown on a light page, and Save roles writes the drawing in place | [layers.md](docs/layers.md) |
| **Details** | I | the picture's format, dimensions, aspect, file size, modified time and folder, and its palette as five swatches | [details.md](docs/details.md) |

### Making a drawing

**New** in the title bar, or **Ctrl+N**, opens the New SDG window: the drawing's size in pixels, or in mm, in or pt at a
DPI, with the texels that makes; whether its field starts inside or outside; and its mode, Heightmap, Stencil or Mask.
Create opens the editor, a window of its own showing the field, and its Save asks where with the OS's dialog and shows
the drawing once written.

In Mask and Stencil a drag on the field paints it with a round brush: Paint or Erase, its size, its strength (how far
one stroke goes, however its dabs overlap) and its hardness (how much of it is solid before it falls off). **Trace
over…** puts a picture under the field, which is then a shade over the outside. Stencil's re-distancing and snapping,
and Heightmap's 3D view, come a slice at a time ([sdg-editor.md](docs/sdg-editor.md)).

Nothing is ever written over another file: a name taken becomes `<name> (2).sdg`, and every file is written beside its
place and moved in, so a half-written one is never there to be opened. What goes wrong is said in the panel.

[docs/layout.md](docs/layout.md) is the window's design: what was wrong with 0.1's bar, the sizes, and the input.

## How it is put together

Two modules under this folder, split where the dependency edge falls, and four kinds of part that never hold each
other:

- **components**: logic, reached only by messages. The viewer runs on the framework's logic lane.
- **views**: presentation, run on the GUI thread in each frame (`@View`).
- **jobs**: slow work on the offload lane, a request in and a reply out. What can go wrong is a field of the reply.
- **recipes**: a part built once and handed to whoever takes it.

The parts talk by messages, and every message is in one file. Read these in this order.

### `pix-core`, the controller

It builds against no widget, so its tests need no window.

| File | What belongs in it |
| --- | --- |
| `Messages.java` | **every message the parts send each other**, grouped by who sends it to whom, each record beside its topic. A record crosses as itself, so it holds only immutable values, and one handle: a `Feed`. |
| `Viewer.java` | **the controller**: which picture is wanted, what is read and when, which pictures are kept, and what each tool is in the middle of, as declared steps on one immutable state held by a ledger. One read is out at a time; idle, it reads ahead to the neighbours, then to the thumbnails the filmstrip lacks. Its state names pictures by path; the view holds them. It asks for the OS's open dialogs (the framework's `PickFile`). |
| `Core.java` | the list of this module's component and jobs. The processor compiling `-gui` sees only `-gui`'s sources, so a component or job missing from this list is never built, with no error. |
| `Begin.java`, `ListPictures.java` | jobs: where to start, and a folder's pictures in order. |
| `ReadPicture.java`, `ReadThumbnail.java` | jobs: a picture decoded into a feed, with its thumbnail and palette from the same decode (a player's feed for one that plays); a thumbnail alone. |
| `TracePicture.java`, `SaveDrawing.java`, `Traces.java` | Trace: a quick trace for the panel to compare, the full-quality one Save writes, and what they share. |
| `ConvertPicture.java` | Convert: the picture written beside itself as PNG or SDG. |
| `RoleDrawing.java` | Layers: theme roles written into a drawing, in place. |
| `Sketcher.java`, `Sketch.java`, `WriteDrawing.java`, `ReadBackdropPicture.java` | a drawing made by hand: the component that owns the field, on the `sketch` lane, which paints it dab by dab into a live feed; the brush, the preview and the drawing it is saved as; the job that writes it; and the job that reads a picture to trace over. |
| `Beside.java`, `Palette.java` | where a file made from a picture goes, never over another; a picture's five colours. |
| `CornerTrace.java`, `CornerFit.java` | a prototype trace with sharp corners (FN-22). Nothing in the app uses it yet. |

### `pix-gui`, the view and the framework integration

| File | What belongs in it |
| --- | --- |
| `Recipes.java` | **what this module builds**: one `@Provides` method per part. `PixAppWiring`, which builds them and the components and jobs in order, is generated while it compiles; never write it. |
| `View.java` | **the view**, a `@View`: the picture the viewer chose, fitted to the stage, and the keys declared as the messages they send. It holds the pictures the viewer keeps, as feeds, shows the one it is told to, and closes one when the viewer lets it go. It decides nothing about the folder. How the picture is looked at is its own: each gesture replaces its `Lens`, and its `@Draw` half draws that on every frame. |
| `Ui.java` | the window: the stage, Previous and Next at its sides, the bar over its bottom or below it. Controls in rem and words in dp, so the zoom sizes the one and never the other. Holds no state. |
| `TracePanel.java`, `ConvertPanel.java`, `LayersPanel.java`, `DetailsPanel.java` | the tools' panels, and what Trace and Layers draw on the stage. Like `Ui`, they only show. |
| `NewSdgWindow.java`, `NewSdgView.java` | the New SDG window and its view, which also declares the title bar's New menu and Ctrl+N. |
| `SketchWindow.java`, `SketchView.java` | the SDG editor's window and its view: the brush, the field fitted to the stage over the picture traced, a drag told as dabs, and Save. |
| `Splash.java`, `Wordmark.java` | what shows while no picture is wanted; the RASTERWARE wordmark, in the title bar and on the splash. |
| `Lens.java` | how the picture is looked at, as a value: the zoom and where it was moved, eased in closed form so a frame is drawn from the value and its moment alone; and the bar's zoom stops. |
| `Fade.java`, `Toss.java`, `Glide.java`, `Shade.java` | the other motions, each a value in closed form: the controls fading at rest; a step's throw; the filmstrip's glide; one node shown or hidden over 150 ms. |
| `Fit.java`, `Strip.java`, `Details.java`, `Facts.java` | arithmetic with no `Gui` in it: where a picture sits; how many thumbnails fit and which is which; what the bar's details say and which a narrow bar keeps; what the Details panel says. |
| `Keys.java` | the chords that ask something of the view itself: 0, Space, Esc, the tools' keys, and the controls' size. |
| `Pix.java`, `PixApp.java` | the entry point and constants; the application, which names its starters, `AutomationStarter` among them. |
| `Look.java`, `Landmarks.java` | the vocabularies: colour, and the names an automation script may depend on. |

The application edge (input, the clipboard, window memory, the clock, the frame loop with its wakes and pacing, the
dialogs, the command line and the shutdown order) is `vexelray-framework`'s.

### Threading, in one paragraph

The GUI loop runs on the main thread, which owns Vulkan, the window and present, and nothing of this application's
runs there but the view, which is presentation and nothing else. The viewer runs on the framework's logic lane, one
message at a time, and its state changes only by its declared steps, which the processor reads: a step that touches
the disk, waits or reaches outside itself does not compile. Every read and write of the disk is a job on the offload
lane, so a slow disk is a slow reply and never a frozen window. **A picture reaches the screen as a feed**: `ReadPicture`
decodes it, builds its mip chain and writes it into a one-slot `Feed`, and answers with the feed; the view names it on
a node, and the renderer uploads it the first time it is drawn. A picture that plays has a feed of three slots: the
codec's `Player` writes its frames there on a thread of its own and keeps the feed's time, and the view's `@Draw` shows
the frame due. The pixels are written once and never cross the bus. **Jobs are in no order at all**, which is why the
viewer keeps one read out at a time and drops an answer it no longer wants.

## Building it

### Native executables

Two builds on Windows, made with GraalVM native-image (a GraalVM JDK as `JAVA_HOME`, and Visual Studio with the
"Desktop development with C++" workload):

```
native-build.cmd -Pnative-release package -DskipTests   # pix-gui/target/pix.exe        what ships: no console
native-build.cmd -Pnative package -DskipTests           # pix-gui/target/pix-debug.exe  a console: prints the automation port line
```

On Linux there is one, with plain `mvn`:

```
mvn -Pnative-linux package -DskipTests                  # pix-gui/target-linux/pix
```

`native-build.cmd` is `mvn` inside a Visual Studio developer environment: native-image needs the MSVC `link.exe` and
the icon step needs `rc.exe`, and neither is on `PATH` in a plain shell. Under Git Bash it is worse, since coreutils'
own `link.exe` comes first. The script finds `vcvars64.bat` with `vswhere`, loads it, and runs `mvn` with its arguments
(`./native-build.cmd ...` from Git Bash). From anywhere else the icon step stops and says to use it.

This program's own native-image metadata is `pix-gui/src/main/resources/META-INF/native-image/.../reachability-metadata.json`,
and it is short because almost nothing is this program's: the libraries register their own, and the processor
registers the icon. If a native run fails on something the JVM run did not (a `Missing...RegistrationError`), run it
on a JVM under `-agentlib:native-image-agent=config-output-dir=...`, do the thing that failed, and add only the entries
that name this program.

Both builds carry the automation sockets, off until asked for, so ottermate can drive either. Packaging (the installer,
file associations, FFmpeg staged beside the executable for video) is still to come back from 0.1 ([TODO](docs/TODO.md)).

### The marks

Every mark is made from its drawing at build time by `vexelray-gui-icons-maven-plugin`, and no icon is committed. The
drawings are in `pix-gui/src/main/rc`:

| Drawing | Becomes |
| --- | --- |
| `pix.svg` | the mark: `/pix.ico`, which the window and its taskbar button wear (`@VexelApp(icon = Pix.ICON)`) and `pix.rc` links into the executable from `target/generated-rc`; `/pix.sdgb`, which the splash draws; and the art pair About condenses as it opens |
| `wordmark.svg`, `wordmark-split.svg` | the RASTERWARE wordmark and its taped version for the splash, as drawings whose face is in a theme role |
| `pix-background.svg` (with `-16` and `-24`) | the icon of a run in the background, redrawn at small sizes |
| `file-*.svg` | the file types' icons, for the installer |

Change the drawing, and everything made from it changes together. The other drawings there (`-light`, `pix-window`,
`pix-file`) are the suite canvas's variants, kept beside them and not yet built.

The wordmark is Press Start 2P's capitals, which sit on an eight-pixel grid, so the word is a raster 79 by 7, traced
pixel by pixel into its SVG and drawn as SDG: square and sharp at any scale. In the title bar it is lit in the theme's
ink, and in the accent while the pointer is on the lockup; a click there opens the framework's app menu (About, the
camera, and the debug run's ports and restart).

An icon for a control comes from the suite's set, `dev.vexelray.gui.icons.Icon`, first. One it lacks goes in
`pix-gui/src/main/icons/small` or `large`, and the `app-icons` profile bakes it into `PixIcon` (Layers' icon is the
one so far).

Run from `mvn exec:exec`, the process is `java.exe`, so Windows groups the taskbar button with other Java programs and
pinning it pins Java. The native executable has neither problem.

## Driving it from outside

### Automation

Rasterware can be driven and read by a script, a macro or an agent through the framework's two loopback sockets:
**remote control** (the keys, clicks and reads a person has) and **diagnostics** (the flight recorder, the log, the
Inspector). Both are in every build, the release one too, and both start off.

- **Switched on by hand**: the plug in the title bar opens the switches, one per socket, each with a port (blank for a
  free one).
- **Switched on at launch**: `--automation=0` (both, on a free port), `--automation.control=<port>`,
  `--automation.diagnostics=<port>`, or `VEXELRAY_AUTOMATION=0` in the environment.
- **Not there at all**: `--automation.available=none` (or `control`, or `diagnostics`), or
  `VEXELRAY_AUTOMATION_AVAILABLE`. A socket that is not available cannot be switched on by anything, and with neither
  available the plug is gone.

The plug's lamps say what each socket is doing, remote control's (red) left of the plug and diagnostics' (blue) right:

| Lamp | Means |
| --- | --- |
| none | off, and nothing has connected to it this session |
| ring | open, and nothing has connected yet |
| filled ring | open, and something is connected now |
| ring with a grey dot | open; something was connected and has gone |
| grey dot | off, after something had connected to it this session |

The plug's tooltip and the switches' window say the same in words, with the port and how many are connected.

### Screenshots and scripts

Screenshots are `ottermate`'s, and only its (`vexelray-gui/docs/guides/ottermate.md`): it photographs the running
window on the application's own device, so the picture is right about content as well as chrome. There is no
`--capture` flag in this project on purpose. Everything before `--launch` is ottermate's; everything after it is the
application's command line (on Windows, `mvn.cmd`):

```
ottermate shot out.png --launch mvn.cmd compile exec:exec -Dautomation=0
ottermate --zoom 1.5 --dpi 2 --size 1600x900 shot big.png --launch ...   # pixels
ottermate --size 24emx16em shot smallest.png --launch ...                # the minimum, in em
ottermate --script check.txt --launch ...                                # one command a line; stops at the first failure
```

The names in `Landmarks.java` are what a script refers to: `await bar.name IMG_0412.jpg` waits for a picture to be
wanted, `await bar.place 3 of` for its folder to be listed, `click bar.strip.0` presses the filmstrip's left cell, and
`click bar.trace` opens Trace. `ottermate help` lists every verb.

### Logging

The framework configures logging before anything else runs, and the defaults follow what kind of run it is:

| Run | Console | File |
| --- | --- | --- |
| a test | warnings | none |
| from your checkout (`mvn exec:exec`) | info | `pix-gui/target/logs/pix.log`, debug |
| driven by `ottermate` or `-Dautomation` | debug | `…/pix.log`, **trace**, with a probe trace beside it |
| a native image | warnings | `~/.pix/logs/pix.log`, info |

When a run goes wrong, read the log first. Write to it with a logger named for its area, never `System.out`, which is
for lines another program parses:

```java
private static final Log LOG = Log.of("pix.wordmark");   // sibarum.probe.Log
LOG.warn("could not read {}", path, exception);           // the exception goes last; its stack reaches the file
```

`--log=debug` (or `off`) changes the level for one launch, and `-Dlog.level.pix.wordmark=trace` raises one logger.
Levels, settings and what belongs at which level are in `atchung/docs/logging.md`.

## The stack

Everything below is installed locally rather than downloaded, so `mvn install` in each of those projects has to have
happened at least once. If the build cannot resolve them, that is why. Their source and javadoc are the documentation.

| What | Why it is here |
| --- | --- |
| `vexelray-framework-api`, `vexelray-gui-core` | all `-core` builds against: the framework's annotations, and the values and the bus that are safe from any thread |
| `vexelray-framework-core`, `-shell`, `-automation`, `-processor` | the application edge, the driving socket, and the processor that checks both modules and writes the wiring |
| `vexelray-gui-widget` | the widgets: the title bar, buttons, sliders, the panels' parts |
| `vexelray-gui-krono` | the clock: every transition and cue |
| `vexelray-gui-automation` | driving the running app the way a person does |
| `vexelray-gui-icons-maven-plugin` | the marks made from their drawings, and `PixIcon` |
| `vexelray-codec-image`, `-video` | decoding pictures (imagelib) and playing animations and videos (FFmpeg), with sound |
| `vexelray-codec-record` | recording from the camera, carried over from 0.1 and not yet used |
| `vexelray-fields-convert` | SDG drawings: tracing, converting, rendering, reading and writing them |
| `tactroller-atchung`, `tactroller-clipboard` | keyboard and pointer input onto an atchung bus; the OS clipboard |
| `vexelray-os-*`, `tactroller-*` | picked by an OS-activated profile; exactly one activates |

## The documents

Three are kept up as the work goes, and [CLAUDE.md](CLAUDE.md) says when each is written:

- [docs/framework-notes.md](docs/framework-notes.md): every place this application works around the framework, written
  at the moment of the workaround, so the gap is fixed upstream instead of copied. The most important of the three.
- [docs/retrospective.md](docs/retrospective.md): at the end of each milestone, the pass over the notes that looks for
  what they have in common. The one the framework reads first.
- [docs/TODO.md](docs/TODO.md): what is known and not done.

The rest are designs, each proposed before its messages were written and ending with what was built:
[layout.md](docs/layout.md) (the window and the hands), [trace.md](docs/trace.md), [convert.md](docs/convert.md),
[layers.md](docs/layers.md), [details.md](docs/details.md) and [sdg-editor.md](docs/sdg-editor.md).
