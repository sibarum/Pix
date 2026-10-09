# TODO

## To ship

- **A mark.** `vexelray-icons` has none for Pix. Draw one on the 96 px grid (`svg/pix.svg`, its own hue), render
  `src/main/rc/pix.ico`, and put back the `rc.exe` step and the `.res` linker option Vexplore's pom has.
- **A native build**, both editions, and the reachability metadata checked against it (copied from Vexplore's, with
  the main class renamed; never run).
- **`installer.json`, `manifest.json`, `install.ps1`**, like Vexplore's, with file associations for the image types,
  and an entry in Vexplore's `suite/suite.json`.

## The suite

- **Vexplore hands pictures to Pix**, the way it hands text to Vex: *View* and *Pop out* start `pix <file>` when Pix
  is installed (found through its install record, framework `Apps`), and keep Vexplore's own viewer when it is not.
- **The order.** Pix steps by name; Vexplore steps in whatever order its list is sorted. A hand-off that should keep
  the explorer's order needs a way to say it on the command line.

## The copies

The viewer's parts are in both Vexplore and Pix: `ImageView`, `Filmstrip`, `Tile`, `Icons`, `Textures`, `Lru`,
`files/Picture`, `files/Stamp`, and most of `Previewer` and `Pictures`. Move them to one place both depend on:
`vexelray-gui-widget` where they are general (`Lru`, `Textures`, `ImageView`, `Filmstrip`), or a small shared module
for what is the suite's own (`Tile`, `Icons`, the palette).

## The viewer

- Zoom and pan; the viewer forgets its backdrop and size on close (both carried over from Vexplore).
- Watch the folder: a file added, removed or renamed while Pix is open is not seen until it is opened again.
- Delete, copy, rotate: nothing here changes a file yet.
- Drop a file on the window to open it; an Open… for a file.

## Beyond pictures

"All the formats" means decoders the stack does not have: video, audio and PDF. `Preview` is the seam a second kind
of content would come in through.
