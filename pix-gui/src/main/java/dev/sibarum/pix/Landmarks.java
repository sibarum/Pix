package dev.sibarum.pix;

/**
 * Every name an automation script may write down, in one place.
 *
 * <p><b>A ref is minted per run; a landmark still means something tomorrow.</b> That is the whole distinction
 * (vexelray-gui/docs/automation.md 5), and it is why these are constants rather than string literals scattered
 * through the view: a landmark is a published contract, so renaming one breaks a script somebody else wrote, and
 * the compiler should be the thing that notices.
 *
 * <p>State is carried in the accessible names where a driver waits on it: {@code await status.name IMG_0412.jpg} waits
 * for a picture to be wanted, and {@code await status.place 3 of} for the folder to be listed.
 */
final class Landmarks {

    /** The room the picture is fitted into. */
    static final String STAGE = "stage";

    /** The picture. */
    static final String PICTURE = "picture";

    /** What is said in the picture's place when a file is not a picture Pix can show. */
    static final String NOTE = "note";

    /** The status line, and its slots: the picture's name, and where it is in its folder. */
    static final String STATUS = "status";
    static final String STATUS_NAME = "status.name";
    static final String STATUS_PLACE = "status.place";

    private Landmarks() {
    }
}
