package dev.sibarum.pix;

import sibarum.atchung.Topic;

/**
 * What a key chord asks of the {@link View} itself, rather than of the viewer: the message the chord was declared to
 * send. A chord is a declaration ({@code gui.shortcut(chord, sender, message)}, threading.md T7.5): the dispatcher
 * sends this when it is pressed and runs nothing of Pix's, and the view hears it in the frame like any other message.
 * How the picture is looked at is the view's alone, so these never reach {@code -core}.
 */
final class Keys {

    private Keys() {
    }

    /** 0: the picture fitted again, in the middle. */
    record FitKey() {
        static final Topic<FitKey> TOPIC = Topic.edge(FitKey.class);
    }
}
