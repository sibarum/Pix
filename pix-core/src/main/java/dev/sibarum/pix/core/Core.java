package dev.sibarum.pix.core;

import dev.vexelray.framework.api.Configuration;

/**
 * What this module is made of: the {@link Viewer} and the jobs that touch the disk.
 *
 * <p>The annotation processor sees only the sources of the module it is compiling, so when {@code -gui} compiles,
 * everything here is a class file it would never construct. This list is how it hears of them: {@code -gui}'s
 * {@code PixApp} names this class among its {@code starters}, and the generated wiring builds what is listed
 * here as if it were its own, and checks every message between them. A new component or job in this module goes on
 * this list, or nothing builds it.
 */
@Configuration(components = Viewer.class, jobs = {Begin.class, ListPictures.class, ReadPicture.class, ReadThumbnail.class,
        TracePicture.class, SaveDrawing.class, ConvertPicture.class,
        RoleDrawing.class})
public final class Core {
}
