package dev.sibarum.pix;

/**
 * Every name an automation script may write down, in one place.
 *
 * <p><b>A ref is minted per run; a landmark still means something tomorrow.</b> That is the whole distinction
 * (vexelray-gui/docs/automation.md 5), and it is why these are constants rather than string literals scattered
 * through the view: a landmark is a published contract, so renaming one breaks a script somebody else wrote, and
 * the compiler should be the thing that notices.
 *
 * <p>State is carried in the accessible names where a driver waits on it: {@code await bar.name IMG_0412.jpg} waits
 * for a picture to be wanted, and {@code await bar.place 3 of} for the folder to be listed. A name cut to fit is cut
 * only as drawn, so its accessible name is whole.
 */
final class Landmarks {

    /** The room the picture is fitted into. */
    static final String STAGE = "stage";

    /** The picture. */
    static final String PICTURE = "picture";

    /** What is said in the picture's place when a file is not a picture Pix can show. */
    static final String NOTE = "note";

    /** The buttons at the stage's sides. */
    static final String PREVIOUS = "previous";
    static final String NEXT = "next";

    /**
     * The bar along the bottom, and its words: the picture's name; what the picture shown is and its size; the file's
     * size; and where it is in its folder.
     */
    static final String BAR = "bar";
    static final String BAR_NAME = "bar.name";
    static final String BAR_KIND = "bar.kind";
    static final String BAR_BYTES = "bar.bytes";
    static final String BAR_PLACE = "bar.place";

    /** The filmstrip, and its cells left to right: {@code bar.strip.0} and on. */
    static final String STRIP = "bar.strip";

    static String cell(int i) {
        return STRIP + "." + i;
    }

    /** Keep shown, in the bar's tools: the controls pinned, and given room of their own. */
    static final String KEEP = "bar.keep";

    private Landmarks() {
    }
}
