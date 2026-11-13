package dev.sibarum.pix;

import sibarum.atchung.Topic;

/**
 * What a key chord asks of the {@link View} itself, rather than of the viewer: the message the chord was declared to
 * send. A chord is a declaration (a keymap action's {@code sends(sender, message)}, threading.md T7.5): the
 * dispatcher sends this when it is pressed and runs nothing of Rasterware's, and the view hears it in the frame like
 * any other message.
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
     * View → Zoom In ({@code 1}), Zoom Out ({@code -1}) and Actual Size ({@code 0}): the picture a stop larger or
     * smaller, as the bar's + and − make it, or at its own pixels. How it is looked at is the view's, so this never
     * reaches {@code -core}. Fill Window is {@link FitKey}.
     */
    record LensKey(int direction) {
        static final Topic<LensKey> TOPIC = Topic.edge(LensKey.class);
    }

    /**
     * Space: a picture that plays paused, or played on. How it is watched is the view's, as how it is looked at is, so
     * this never reaches {@code -core}.
     */
    record PlayKey() {
        static final Topic<PlayKey> TOPIC = Topic.edge(PlayKey.class);
    }

    /** Esc: out of full screen, or, in the window, closing Rasterware. Which, only the view knows. */
    record EscKey() {
        static final Topic<EscKey> TOPIC = Topic.edge(EscKey.class);
    }

    /** T: the Trace panel opened on the picture shown, or closed. Whether it can be, only the view knows. */
    record TraceKey() {
        static final Topic<TraceKey> TOPIC = Topic.edge(TraceKey.class);
    }

    /** C, or a click on the format badge: the Convert panel opened on the picture shown, or closed. */
    record ConvertKey() {
        static final Topic<ConvertKey> TOPIC = Topic.edge(ConvertKey.class);
    }

    /** L, or Layers in the bar: the Layers panel opened on the drawing shown, or closed. */
    record LayersKey() {
        static final Topic<LayersKey> TOPIC = Topic.edge(LayersKey.class);
    }

    /** I, or Details in the bar: the Details panel opened on the picture shown, or closed. */
    record DetailsKey() {
        static final Topic<DetailsKey> TOPIC = Topic.edge(DetailsKey.class);
    }

    /**
     * Ctrl+= ({@code 1}), Ctrl+- ({@code -1}) and Ctrl+0 ({@code 0}): the controls a step larger, smaller, or as they
     * started. The window's zoom, which sizes the controls and not the words (layout.md, <i>Sizes</i>).
     */
    record ZoomKey(int direction) {
        static final Topic<ZoomKey> TOPIC = Topic.edge(ZoomKey.class);
    }
}
