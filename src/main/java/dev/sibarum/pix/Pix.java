package dev.sibarum.pix;

import dev.vexelray.framework.shell.VexelApplication;

/**
 * An image viewer: one picture, as large as the window allows, and the rest of its folder a key press away.
 *
 * <p>The entry point, and the constants the rest of this package reads. What this application builds is in
 * {@link Recipes}, one method per part; {@code PixAppWiring}, which builds those parts in order, is generated from them
 * and from the annotation on {@link PixApp} while the project compiles. Everything drawn is in {@link Viewer};
 * everything the application <em>knows</em> is in {@link Model}. This class holds no state of its own.
 *
 * <pre>
 * pix                  the first image in the user's Pictures folder
 * pix &lt;file&gt;           that image, stepping through the images in its folder
 * pix &lt;folder&gt;         that folder's first image
 * pix --key=value      override a setting for this launch
 * </pre>
 *
 * <p>A misspelled flag is refused by name with the alternatives listed, rather than a stack trace before any window.
 * Needs {@code --enable-native-access=ALL-UNNAMED}.
 *
 * <p>The {@code @VexelApp} declaration is {@link PixApp}.
 */
public final class Pix {

    /** The application's own name, which is what its settings directory is called. Stable across releases. */
    static final String APP = "pix";

    /** The window's title. */
    static final String TITLE = "Pix";

    /** Window size on a first run, in the engine's logical coordinates. */
    static final int W = 1100;
    static final int H = 760;

    /** The smallest this UI is still coherent at, in root ems — a floor, not the design size. */
    static final float MIN_W_EM = 30;
    static final float MIN_H_EM = 16;

    public static void main(String[] args) {
        String[] cleaned = java.util.Arrays.stream(args).filter(s -> !s.isBlank()).toArray(String[]::new);
        VexelApplication.run(new PixAppWiring(), cleaned);
    }

    private Pix() {
    }
}
