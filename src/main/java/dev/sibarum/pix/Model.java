package dev.sibarum.pix;

import dev.sibarum.suite.pictures.Decoded;
import sibarum.atchung.Committer;
import sibarum.atchung.State;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * The one authoritative state, and the only way to change it. Every edit is a function of the current value,
 * committed through atchung's {@code State}, so concurrent edits are put in an order and none is lost.
 *
 * <p>The two edits that arrive from elsewhere carry what they were made for: a listing its folder and a picture its
 * file, so one that lost a race with a later change is dropped instead of drawn.
 */
final class Model {

    private final State<Doc> state;

    private final Committer<Doc, UnaryOperator<Doc>> edit;

    Model() {
        State.Builder<Doc> builder = State.of(Doc.initial());
        // Declared before build, held as a handle: State refuses a committer it was not built with, which is
        // what stops an unrelated component minting its own way to write this.
        this.edit = builder.mutation("doc.edit", (current, change) -> change.apply(current));
        this.state = builder.build();
    }

    Doc doc() {
        return state.value();
    }

    void change(UnaryOperator<Doc> change) {
        state.commit(edit, change);
    }

    /** Open {@code folder} on {@code at}, or on its first image when {@code at} is null. */
    void open(Path folder, Path at) {
        change(d -> d.opening(folder, at));
    }

    /** A listing arrived. Dropped unless {@code folder} is still the one open. */
    void listed(Path folder, List<Path> pictures) {
        change(d -> Objects.equals(d.folder(), folder) ? d.listing(pictures) : d);
    }

    /** A picture arrived. Dropped unless {@code path} is still the one shown. */
    void previewed(Path path, Decoded preview) {
        change(d -> Objects.equals(d.at(), path) ? d.previewing(preview) : d);
    }

    /**
     * Tell {@code listener} about each new document, <b>in order, one at a time, and never an older one after a
     * newer one</b>.
     *
     * <p>{@code State.onCommit} fires on the committing thread after the compare-and-set, and handlers commit from a
     * pool, so two commits can deliver as version 6 then 5, or at the same instant. A listener that draws the whole
     * document would then finish on the stale one and stay wrong until the next edit. The document is the truth and
     * only the newest matters, so an older one that arrives late is dropped.
     */
    void onChange(Consumer<Doc> listener) {
        Object gate = new Object();
        long[] last = {0L};
        state.onCommit(v -> {
            synchronized (gate) {
                if (v.version() <= last[0]) {
                    return;
                }
                last[0] = v.version();
                listener.accept(v.value());
            }
        });
    }
}
