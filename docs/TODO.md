# TODO

## To ship

- ~~A mark~~ — done: `src/main/rc/pix.svg` from the suite icon canvas, rendered to `pix.ico` and linked into the
  executable through `pix.rc`. The window, file and light variants are beside it; the file icon is for the
  associations below.
- **A native build**, both editions, and the reachability metadata checked against it (copied from Vexplore's, with
  the main class renamed; never run).
- **`installer.json`, `manifest.json`, `install.ps1`**, like Vexplore's, with file associations for the image types,
  and an entry in Vexplore's `suite/suite.json`.

## The suite

- ~~Vexplore hands pictures to Pix~~ — done: Enter, double-click and *View in Pix* start `pix <file>` when Pix's install
  record is there, and Vexplore keeps its own viewer when it is not.
- **One Pix per picture.** Each hand-off starts a new Pix; a second could go to the one already open.
- **The order.** Pix steps by name; Vexplore steps in whatever order its list is sorted. A hand-off that should keep
  the explorer's order needs a way to say it on the command line.

## The viewer

- Zoom and pan, in `vex-suite-common`'s `PictureViewer` so Vexplore gets them too; the backdrop is forgotten on close.
- Watch the folder: a file added, removed or renamed while Pix is open is not seen until it is opened again.
- Delete, copy, rotate: nothing here changes a file yet.
- Drop a file on the window to open it; an Open… for a file.

## Beyond pictures

"All the formats" means decoders the stack does not have: video, audio and PDF. The document's `Decoded` is the seam a
second kind of content would come in through, and `PictureViewer.Showing` the one it would be drawn through.
