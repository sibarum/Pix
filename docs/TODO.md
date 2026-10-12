# TODO

## To ship

- ~~A mark~~ — done: `src/main/rc/pix.svg` from the suite icon canvas, rendered to `pix.ico` and linked into the
  executable through `pix.rc`. The window, file and light variants are beside it; the file icon is for the
  associations below.
- **The debug native build** (`-Pnative`). The release edition builds, starts and shows a picture with the
  reachability metadata copied from Vexplore's; the debug one, with automation, has not been built.
- ~~`installer.json` and the suites~~ — done: `installer.json` (id `pix`, on PATH, the icon, associations for every
  type imagelib decodes), and Pix is a member of both suites, Vexplore's `suite/suite.json` and
  `vexelray-framework/installer-suite.json` (with `docs/apps/pix.md`). They skip it until it is published.
- **The first release**: the GitHub repo `sibarum/Pix` (public, since the installers read its raw `manifest.json`),
  then `installer-core`'s CLI to generate `install.ps1`, `uninstall.ps1`, `update.ps1` and `manifest.json` and to
  publish `pix.exe` as release `v0.1.0`, as Vexplore's were. Its build needs `vex-suite-common` and
  `vexelray-codec` published too.

## The suite

- ~~Vexplore hands pictures to Pix~~ — done: Enter, double-click and *View in Pix* start `pix <file>` when Pix's install
  record is there, and Vexplore keeps its own viewer when it is not.
- **One Pix per picture.** Each hand-off starts a new Pix; a second could go to the one already open.
- **The order.** Pix steps by name; Vexplore steps in whatever order its list is sorted. A hand-off that should keep
  the explorer's order needs a way to say it on the command line.

## The viewer

- ~~Zoom and pan~~ — done, with turning, in `vex-suite-common`'s `PictureViewer` (its `Lens`), so Vexplore has them too.
  ~~Bilinear at rest~~ — done: once still, the part on screen is made again from the file's own pixels (`Sharpen`).
- ~~A setting for how pictures are scaled~~ — done: the Settings menu, moving and at rest, seven filters each.
  Moving, the kernel filters shrinking a picture spread a fixed number of bilinear taps, which approximates; mipmaps
  would make that exact. Moving, filtering is in the stored sRGB values; at rest, in linear light, so a filter can look
  a shade different once still.
- The backdrop is forgotten on close.
- Watch the folder: a file added, removed or renamed while Pix is open is not seen until it is opened again.
- Delete, copy, rotate: nothing here changes a file yet.
- Drop a file on the window to open it. (~~An Open… for a file~~ — done: Ctrl+O, Ctrl+Shift+O and a
  click on the name.)
- One dialog that picks a file or a folder, for the click on the name: it picks only a file.

## Beyond pictures

"All the formats" means decoders the stack does not have: video, audio and PDF. The document's `Decoded` is the seam a
second kind of content would come in through, and `PictureViewer.Showing` the one it would be drawn through.
