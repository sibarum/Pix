package dev.sibarum.pix;

import sibarum.atchung.Topic;

/**
 * What a key chord asks of the {@link View} itself, rather than of the viewer: the message the chord was declared to
 * send. A chord is a declaration ({@code gui.shortcut(chord, sender, message)}, threading.md T7.5): the dispatcher
 * sends this when it is pressed and runs nothing of Pix's, and the view hears it in the frame like any other message.
 * How the picture is looked at, and how big the controls are, are the view's alone, so these never reach {@code -core}.
 */
final class Keys {

    private Keys() {
    }

    /** 0: the picture fitted again, in the middle. */
    record FitKey() {
        static final Topic<FitKey> TOPIC = Topic.edge(FitKey.class);
    }

    /**
     * Ctrl+= ({@code 1}), Ctrl+- ({@code -1}) and Ctrl+0 ({@code 0}): the controls a step larger, smaller, or as they
     * started. The window's zoom, which sizes the controls and not the words (layout.md, <i>Sizes</i>).
     */
    record ZoomKey(int direction) {
        static final Topic<ZoomKey> TOPIC = Topic.edge(ZoomKey.class);
    }
}
