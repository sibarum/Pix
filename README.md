# Pix

An image viewer: one picture, as large as the window allows, and the rest of its folder a key press away. Built on
[vexelray-gui](../vexelray-gui) through [vexelray-framework](../vexelray-framework), and the third member of the suite
with [Vex](../text-editor-vexel-demo) and [Vexplore](../vexplore).

**Install it** on Windows, with the other VexelRay applications, from [sibarum.github.io](https://sibarum.github.io/).

Pix began as Vexplore's viewer, and the two still share it. In Vexplore the viewer is a second window that drives
the explorer's selection; here it is the whole application, so `pix <file>` opens a picture without an explorer
behind it. Vexplore hands pictures to Pix when Pix is installed.

## Run it

```
mvn compile exec:exec
mvn compile exec:exec "-Dapp.args=C:/Users/me/Pictures/holiday/IMG_0412.jpg"
mvn compile exec:exec "-Dapp.args=C:/Users/me/Pictures/holiday"
mvn test
```

Needs the stack installed to the local Maven repository first, in the order in
[vexelray-gui/CLAUDE.md](../vexelray-gui/CLAUDE.md), then [`vexelray-codec`](../vexelray-codec), and imagelib's
native library (`imagelib-wrapper`) behind it.

| Command | Opens |
| --- | --- |
| `pix <file>` | that picture, stepping through the images in its folder |
| `pix <folder>` | the folder's first image |
| `pix` | the first image in the user's Pictures folder, or their home |

The file asked for is shown before its folder has been read; the count says *reading the folder…* until it has.

## What it does

- **Every format imagelib decodes**: PNG, JPEG, GIF, WebP, BMP, TIFF, SVG, ICO and the rest, animated where the file
  is. A file that is not a picture, or will not decode, says why in place of the picture.
- **An animated GIF, WebP or APNG plays like a movie**, of any length and size: a transport over the picture has play
  and pause, a frame back and on, a seek bar to drag, the clock and frame, and loop. **Space** plays and pauses,
  **,** and **.** step a frame (with **Shift**, a second), **L** turns looping off and on. It plays as soon as it is
  shown, round and round, and pauses where it is while Pix is not the window in front, playing on when it is again
  (a pause of your own stays). Its frames are decoded as it plays, a few ahead, each scaled to at most 2560 px with
  its mip chain, and streamed to the GPU; up to 512 MB of the frames already shown are kept, so seeking back within
  them is instant.
- **Videos play the same way**: an MP4, MKV, WebM, MOV (and AVI, M4V, TS and the others FFmpeg reads) is listed among
  the pictures, with what it is under the name (*H.264 video · 1920 × 1080 · 0:42*), and played from its first frame
  with the same transport, keys and looping as an animated GIF, held while Pix is not in front, and zoomed, moved and
  turned like any picture. Fitted, a video fills the window (or the screen), as players show it, however small the
  clip; a picture is never enlarged past its own pixels. A frame is a number at the rate the stream declares, so
  **,** and **.** step exactly one. Frames are decoded a few ahead at no more than 2560 px, given their mips and
  streamed to the GPU; up to 256 MB of those already shown are kept, so stepping back is instant, and a seek further
  off is FFmpeg's exact seek. **Its sound plays with it**, out of the system's default output, and the pictures keep
  time by what is being heard, so they never drift from it; the sound stops with a pause, a step, the end, and while
  Pix is not in front,
  and goes wherever a seek or the loop sends the picture. A video with no sound, or a machine with no sound device,
  plays silently as before. `-Dvexelray.sound=off` (or `nowhere`, which drains it to no device in real time) keeps it
  quiet. **A video with sound has Mute and a volume slider** after Loop in the transport, and **M** mutes and unmutes;
  dragging the slider unmutes it. The level and the mute carry from one video to the next and are kept between runs
  (`sound.volume` and `sound.muted` in `~/.pix/settings.properties`); muted, the pictures keep time as before. In the
  filmstrip a video is a still from a
  tenth of the way in (no further than 5 s). A video that does not say how long it is or how fast it plays is shown by
  that still alone. Decoded through [`ffmpeg-wrapper`](../ffmpeg-wrapper)'s FFmpeg, by `vexelray-codec-video`'s `Videos.poster` and `Footage`.
- **Full screen**: a double click on the picture, or **F11**, gives Pix the whole screen, the title bar gone and the
  controls floating and fading over the picture as before; a double click, **F11** or **Esc** comes back to the window
  as it was, maximized if it was. A window closed full screen opens as the window it was. Borderless, not a change of
  display mode, so switching away is instant.
- **←/→, PgUp/PgDn, Home/End** step through the folder, in name order with numbers compared as numbers (`IMG_9`
  before `IMG_10`). Holding an arrow key decodes only where it stops; the next images are read ahead, and recent ones
  kept, so going back is instant. A picture is thrown in across the window from the side it is on, and the old one
  flung out the other way.
- **Look closely**: the wheel zooms about the pointer and a drag moves the picture. Smaller than the window, it stays
  inside it; larger, it always covers at least a quarter of it, and pulled further it gives against the hand and
  springs back with the momentum it had. **R** and **Shift+R** turn it a quarter (a fitted picture is fitted again the
  new way round); **Ctrl+drag** turns it to any angle with the pointer, and **Shift+wheel** a few degrees at a time.
  **H** mirrors it left and right and **V** top and bottom, as it is seen, whichever way it is turned. **Turn** (the
  tile beside Background) has the quarter turns and the mirrors as a menu, and *As the file is*: upright, not
  mirrored, fitted. **0** fits it again (a double click is full screen). The next picture starts fitted, upright and
  not mirrored. Nothing is written to the file.
- **The touchpad, read finger by finger** (a precision touchpad, on Windows), not through the wheel the OS makes of
  it, which Pix ignores while fingers are down:
  - **Two fingers are always a pan**: they carry the picture with them, 16 px for each millimetre, against the same
    give at the edges as a drag, and a pan lets go of it at the speed they were going. The picture follows them
    a few hundredths of a second behind, so a pad's jitter never reaches it.
  - **A zoom joins** once the fingers' spread has changed by half as much as they have travelled, and catches up as
    it joins, so the picture is zoomed as far as the fingers are, about the pointer. Zoomed past the limits it gives
    a little and comes back.
  - **A turn is locked** until the fingers have twisted 30°: short of that the picture leans with them, a few degrees
    and less the further they go, and springs back upright when they lift. Past it the lock gives, the picture
    swings round to where the fingers are and turns freely with them. A picture already turned off a quarter (by
    Ctrl+drag, Shift+wheel or the fingers) turns with them from the start.
  - **Quarter turns and the fitted size are magnets** while the fingers are down. Within 7° of a quarter turn the
    picture is pulled onto it and held, giving only a little, until the fingers are 10° away and it lets go with a
    jump; within 10% of its fitted size, it is pulled to that size and towards the centre, until 14% away. What it
    shows is what lifting does: held on a quarter it settles there, still about the point the fingers were at; held
    at its fitted size it is fitted and centred; anywhere else it stays as it was left.
  - **Three fingers swiping** left go to the next picture and right to the previous, as a page turns. The step happens
    once the fingers have gone 12 mm, while they are still moving, or on a quick flick.
  - **A two-finger double tap** goes from fitted to the picture's own pixels about the pointer (twice its size if it
    already fits at that), and from anything else back to fitted.

  Windows uses three-finger swipes to switch applications unless *Settings → Bluetooth & devices → Touchpad* says
  otherwise. Pix sees the fingers either way, but Windows acts on them too.
- **Sharp at rest**: while the picture moves it is drawn from a copy at most 2560 px across; a moment after it stops,
  what is on screen is made again from the file's own pixels at exactly the size shown, in linear light, on whole
  screen pixels.
- **Settings** (the gear) chooses how the picture is scaled **while moving** (on the GPU, every frame) and **at
  rest** (made again, as above), from the same eight: Nearest, Sharp bilinear (square pixels with clean edges),
  Bilinear, Gaussian (the bell read at points), Normal (the bell's area over each pixel, with the B-spline's spread),
  B-spline, Mitchell and Lanczos, each with its icon from the icon set. Bilinear and Lanczos at first; the choice is
  kept in Pix's settings (`scaling.moving`, `scaling.resting`).
- **The filmstrip** shows the neighbours as they are read; a click shows one.
- **Background**: dark, light, checker, magenta or green behind the picture, across the whole window.
- **The picture has the whole window**, and the controls float over it and fade when the pointer rests; moving the
  mouse, a click or Tab brings them back. **Keep shown** (the pin) pins them: they stop fading and take room of their
  own, so none of them covers the picture. In a narrow window Previous and Next move to its sides, and what does not
  fit is hidden.
- **Ctrl+O**, or a click on the name (the words beside Close, which say *Open a picture…* while nothing is open)
  opens a picture, among the images in its folder;
  **Ctrl+Shift+O** opens a folder on its first image. The OS's own dialogs, starting in the folder open.
- **Esc** closes the background menu, leaves full screen, or closes Pix. **Ctrl+= / Ctrl+- / Ctrl+0** size the controls.
- **Every key and gesture can be changed**: the keyboard in the title bar opens the key bindings, where each command
  lists its keys and touchpad gestures together. Click one and press a key or make a gesture on the touchpad to change
  it, and × removes it. What changes is kept in `~/.pix/keys.properties` (only what differs from the defaults, by
  command: `view.next = Right ThreeFingerSwipeLeft`).

## How it is put together

```
files/     Folder: the images (and videos) in a folder, in order. Blocking: run it on Gui.offload().
Doc        the one state: the folder, its images, which one is shown and its picture.
Model      the only way to change it, and an onChange that never delivers an older document after a newer one.
Previewer  reads the picture of whatever is shown: one worker, latest request wins, read-ahead, an LRU by bytes.
Opener     opens a place: the command line's, or one picked in the open or folder dialog.
Viewer     the suite's PictureViewer as the whole tree: draws the document, and a step changes it.
Recipes    what is built; the wiring is generated from it.
```

Everything else is shared. [`vexelray-codec`](../vexelray-codec)'s image module has the pictures: `Pictures` and
`Picture` (decoding and packing), `Textures`, `ImageView` and `Lru`, and `ImageStarter`, which `PixApp` names so the
textures are built and bound to the window. [`vex-suite-common`](../vex-suite-common), shared with Vexplore, has the
sizes they are decoded at, `PictureViewer` (the tree and its bar), `Filmstrip`, `Tile`, `Icons` and the palette. Both
are sibling checkouts, installed to the local Maven repository (`mvn install`) before this one builds.

## Automation

Pix can be driven and read from outside, by a script, a macro or an agent, through the framework's two loopback
sockets: **remote control** (the keys, clicks and reads a person has) and **diagnostics** (the flight recorder, the
log, the Inspector). Both are in every build and both start off.

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

The plug's tooltip and the switches' window say the same in words, with the port and how many are connected. In full
screen the title bar, and so the lamps, are hidden with the rest of it.

## Looking at it without a person

Every build, the release one too, has the framework's automation sockets, off until asked for (see
[Automation](#automation)). Every control has a landmark (`name`, `count`, `info`, `open`,
`previous`, `next`, `turn` and its menu's `turn.left`, `turn.right`, `turn.mirror-across`, `turn.mirror-down` and
`turn.upright`, `background`, `keep`, `backdrop.checker`, and for a picture that plays `play`, `frame-back`,
`frame-on`, `seek`, `loop`, `mute`, `volume` and `position`, which reads like `0:01.8 / 0:04.8  48/120`), …:

```
java -jar ../vexelray-gui/vexelray-gui-automation-cli/target/vexelray-gui-automation-cli-0.1.0-SNAPSHOT.jar \
     --script scene.txt --launch mvn.cmd -q exec:exec -Dautomation=0 "-Dapp.args=C:/some/folder"
```

`settle` waits for every animation, and that includes the bar's fade, so a script that wants the bar in its picture
clicks `keep` first. A picture that plays keeps the clock busy for as long as it plays, so `settle` times out on one:
`await position " 30/120"` waits for a frame instead, and `click play` pauses it for a picture that holds still. Pix
holds a picture while its window is not in front, and a window opened over it (a second Pix, say) leaves the next
picture on its first frame until Pix is in front again.

## Native builds

As Vexplore's: GraalVM 25 as `JAVA_HOME`, from a Visual Studio developer prompt.

```
mvn -Pnative-release package -DskipTests   # target/pix.exe        GUI subsystem
mvn -Pnative package -DskipTests           # target/pix-debug.exe  console: prints the automation port line
```

Both carry the automation sockets ([Automation](#automation)).

Both link `src/main/rc/pix.rc`, the executable's icon: `pix.svg` from the suite icon canvas, rendered to `pix.ico` by
`vex-suite-common`'s `tools/Ico.java`. The window, file and light variants are beside it.

**FFmpeg ships beside the executable, not in it.** It is six shared libraries (about 125 MB, `avcodec` most of it),
not a static library to link in as imagelib is. Both native builds take them out of ffmpeg-wrapper's jar into
`target/ffmpeg/`, where ffmpeg-wrapper looks beside a running executable, so `target/pix.exe` shows video posters as
it stands. They are also zipped, with FFmpeg's licence and source pointer, into
`target/pix-ffmpeg-windows-x86_64.zip` (about 55 MB, the same bytes for the same libraries), which `installer.json`
lists as a second binary: an archive unpacked into the install's `ffmpeg` folder, and downloaded again only when it
changes. Without that folder Pix still runs; a video says *the video decoder is unavailable* where its poster would
be. The JVM run (`mvn exec:exec`) extracts the libraries from the jar to a temp folder instead, the first time a
video is read.
