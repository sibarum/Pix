package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.CreateSdg;
import dev.sibarum.pix.core.Messages.EditMode;
import dev.sibarum.pix.core.Messages.SaveSdg;
import dev.sibarum.pix.core.Messages.SdgWritten;
import dev.sibarum.pix.core.Messages.Sizing;
import dev.sibarum.pix.core.Messages.Sketched;
import dev.sibarum.pix.core.Messages.Unit;
import dev.sibarum.pix.core.Messages.WriteSdg;
import dev.vexelray.framework.core.PickFile;
import dev.vexelray.framework.core.Picked;
import org.junit.jupiter.api.Test;
import sibarum.atchung.Sender;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The part that holds a field drawn by hand: making one, and saving it through the OS's dialog. */
class SketcherTest {

    private final List<Sketched> sketched = new ArrayList<>();
    private final List<PickFile> picks = new ArrayList<>();
    private final List<WriteSdg> writes = new ArrayList<>();
    private final List<SdgWritten> written = new ArrayList<>();
    private final Sketcher sketcher = new Sketcher(Sender.collecting(Messages.SKETCHED_TOPIC, sketched),
            Sender.collecting(PickFile.TOPIC, picks), Sender.collecting(Messages.WRITE_SDG_TOPIC, writes),
            Sender.collecting(Messages.SDG_WRITTEN_TOPIC, written));

    private static final Path FILE = Path.of("/drawings/plan").toAbsolutePath();

    @Test
    void aFieldMadeIsShownAtItsSize() {
        sketcher.create(new CreateSdg(new Sizing(2, 1, Unit.IN, 100), true, EditMode.MASK));
        Sketched s = sketched.getFirst();
        assertEquals(200, s.width());
        assertEquals(100, s.height());
        assertEquals(EditMode.MASK, s.mode());
    }

    @Test
    void aSizeTooLargeMakesNothing() {
        sketcher.create(new CreateSdg(new Sizing(9000, 10, Unit.PX, 300), true, EditMode.STENCIL));
        assertTrue(sketched.isEmpty());
        sketcher.save(new SaveSdg());
        assertTrue(picks.isEmpty(), "nothing to save");
    }

    @Test
    void savingAsksWhereThenWritesThere() {
        sketcher.create(new CreateSdg(new Sizing(16, 8, Unit.PX, 300), true, EditMode.STENCIL));
        sketcher.save(new SaveSdg());
        sketcher.save(new SaveSdg());
        assertEquals(1, picks.size(), "one dialog at a time");
        assertEquals(PickFile.Kind.SAVE, picks.getFirst().kind());
        sketcher.picked(new Picked(Sketcher.PICK, FILE.toString()));
        WriteSdg w = writes.getFirst();
        assertEquals(FILE.resolveSibling("plan.sdg"), w.file(), "the extension added");
        assertEquals(16.0, w.drawing().source().width());
        sketcher.written(new SdgWritten(Optional.of(w.file()), ""));
        sketcher.save(new SaveSdg());
        assertEquals(2, picks.size(), "the save answered, another may go");
    }

    @Test
    void aDialogCancelledSaysNothingAndLetsAnotherSave() {
        sketcher.create(new CreateSdg(new Sizing(16, 8, Unit.PX, 300), true, EditMode.STENCIL));
        sketcher.save(new SaveSdg());
        sketcher.picked(Picked.cancelled(Sketcher.PICK));
        assertTrue(writes.isEmpty());
        assertEquals(List.of(new SdgWritten(Optional.empty(), "")), written);
        sketcher.save(new SaveSdg());
        assertEquals(2, picks.size());
    }

    @Test
    void anotherPartsDialogIsNotThisOnes() {
        sketcher.create(new CreateSdg(new Sizing(16, 8, Unit.PX, 300), true, EditMode.STENCIL));
        sketcher.save(new SaveSdg());
        sketcher.picked(new Picked(1, FILE.toString()));
        assertTrue(writes.isEmpty());
    }
}
