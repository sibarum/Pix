# TODO

## To ship

- ~~A mark~~ — done: `src/main/rc/pix.svg` from the suite icon canvas, rendered to `pix.ico` and linked into the
  executable through `pix.rc`. The window, file and light variants are beside it; the file icon is for the
  associations below.
- **The debug native build** (`-Pnative`). The release edition builds, starts and shows a picture with the
  reachability metadata copied from Vexplore's; the debug one, with automation, has not been built.
- ~~`installer.json` and the suite~~ — done: `installer.json` (id `pix`, on PATH, the icon, associations for every
  type imagelib decodes), and Pix is a member of the suite in `vexelray-dist/installer-suite.json` (with
  `vexelray-framework/docs/apps/pix.md`).
- **FFmpeg beside the native build**, for video posters. Checked, not done; what it needs:
  1. *Reachability metadata in ffmpeg-wrapper* (`META-INF/native-image/.../reachability-metadata.json`), declaring its
     FFM downcall shapes the way imagelib-wrapper's does. It has none, so a native image cannot call FFmpeg at all.
  2. *Not the libraries as resources.* ffmpeg-wrapper finds them in `-Dffmpeg.native.dir` or extracts them from its
     jar's `natives/<platform>/`. Registering those resources would put 125 MB into `pix.exe`; leaving them out and
     pointing the property at a folder beside the executable is the shape that fits. Better in ffmpeg-wrapper's
     `NativeLib` (look beside the running executable before extracting) than a `System.setProperty` in Pix.
  3. *A zip of the folder* (`ffwrap.dll`, the five FFmpeg DLLs, `libs.txt`, `FFMPEG-LICENSE.txt`,
     `FFMPEG-SOURCE.txt`), as a second entry in `installer.json`'s `binaries`: `kind` `archive` (inferred from
     `.zip`) with `dir` set to an `ffmpeg` folder of the install. vexelray-installer already unpacks, records and
     removes archives; it is how a jlink runtime ships. Nothing builds that zip yet. Keeping the DLLs as separate,
     replaceable files is also what FFmpeg's LGPL asks of a shared build, and the licence and source pointer travel
     in the zip.
  4. *The size*: about 55 MB more to download, beside a 43 MB `pix.exe`. A custom FFmpeg configured with
     `--disable-everything` and only the decoders and demuxers wanted is a fraction of that, and changes nothing above
     ffmpeg-wrapper's `build.sh`.
  5. *File associations* for `.mp4`, `.mkv`, `.webm`, `.mov`: deliberately not added. Pix shows a poster, not the
     film, so it should not become what double-clicking a video opens.
- ~~The first release~~ — done: `pix.exe` 0.1.0 is published from
  [`vexelray-dist`](https://github.com/sibarum/vexelray-dist) (`apps/pix/`, release `pix-v0.1.0`). Its
  [DEPLOYING.md](https://github.com/sibarum/vexelray-dist/blob/main/DEPLOYING.md) says how the next one goes out;
  the install page is <https://sibarum.github.io/vexelray-framework/install/>.

## The suite

- ~~Vexplore hands pictures to Pix~~ — done: Enter, double-click and *View in Pix* start `pix <file>` when Pix's install
  record is there, and Vexplore keeps its own viewer when it is not.
- **One Pix per picture.** Each hand-off starts a new Pix; a second could go to the one already open.
- **The order.** Pix steps by name; Vexplore steps in whatever order its list is sorted. A hand-off that should keep
  the explorer's order needs a way to say it on the command line.

## The viewer

- ~~Zoom and pan~~ — done, with turning, in `vex-suite-common`'s `PictureViewer` (its `Lens`), so Vexplore has them too.
  ~~Bilinear at rest~~ — done: once still, the part on screen is made again from the file's own pixels (`Sharpen`).
- ~~A setting for how pictures are scaled~~ — done: the Settings menu, moving and at rest, eight filters each.
  ~~Mipmaps and linear light while moving~~ — done: a picture's texture carries its mip chain, made in linear light,
  and is sRGB, so the GPU blends in light; the kernels' taps read the level of their own spacing. What still differs
  at rest: it is made from the file's own pixels, where moving reads the 2560 px copy.
- ~~Animations as movies~~ — done: an animated GIF, WebP or APNG is listed as its first frame and played from the file
  (`Movie`, `Playback` in `vexelray-codec-image`), streamed through `GuiApp.stream`, with a transport in the suite's
  `PictureViewer`, held while the window is not in front. What is left: a held picture's play button still shows
  Pause, since pressing it would pause; seeking back past the kept frames (512 MB) walks forward from the start, with
  nothing on screen saying so; and the filmstrip's thumbnails do not fill in on the first picture until a step (seen
  with stills too, so older than this).
- Show the file's pixels untouched at exactly 100%, whatever the filter: at that scale they are the honest answer.
- The backdrop is forgotten on close.
- Watch the folder: a file added, removed or renamed while Pix is open is not seen until it is opened again.
- Delete, copy, rotate: nothing here changes a file yet.
- Drop a file on the window to open it. (~~An Open… for a file~~ — done: Ctrl+O, Ctrl+Shift+O and a
  click on the name.)
- One dialog that picks a file or a folder, for the click on the name: it picks only a file.

## Beyond pictures

- ~~Video posters~~ — done: a video is listed among the pictures and shown as one still frame a tenth of the way in,
  with its codec, size and length (`Videos.poster` in `vexelray-codec-video`). One frame, by choice: a contact strip
  or cycling frames were weighed and left out. Playback is a player's, on its own code path. What is left: the count
  still says *Image 2 of 5* for a video; and the native build, see *To ship*.

"All the formats" means decoders the stack does not have: audio and PDF. The document's `Decoded` is the seam a
second kind of content would come in through, and `PictureViewer.Showing` the one it would be drawn through.
