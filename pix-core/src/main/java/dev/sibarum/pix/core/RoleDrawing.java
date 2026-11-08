package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.WriteRoles;
import dev.sibarum.pix.core.Messages.Written;
import dev.vexelray.fields.sdg.Sdg;
import dev.vexelray.fields.sdg.SdgFile;
import dev.vexelray.framework.api.Job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Write theme roles into a drawing, in place, on the offload lane (docs/layers.md): each top-level solid layer takes
 * the role asked for it, or none; every other layer, and everything else in the file, is kept as it was read. Written
 * compiled if the file is {@code .sdgb}, as XML otherwise ({@code SdgFile.write}, which writes beside and moves into
 * place). What goes wrong is an answer with no file and why.
 */
@Job
public final class RoleDrawing {

    public Written run(WriteRoles w) {
        try {
            Sdg sdg = roled(SdgFile.read(w.path()), w.roles());
            SdgFile.write(sdg, w.path());
            return new Written(w.path(), Optional.of(w.path()), "");
        } catch (IOException | RuntimeException e) {
            return new Written(w.path(), Optional.empty(),
                    "Could not save " + w.path().getFileName() + ": " + e.getMessage());
        }
    }

    /** {@code sdg} with each solid top-level layer's role as {@code roles} says; a layer past the list keeps its own. */
    static Sdg roled(Sdg sdg, List<String> roles) {
        List<Sdg.Layer> layers = new ArrayList<>(sdg.layers().size());
        for (int i = 0; i < sdg.layers().size(); i++) {
            Sdg.Layer l = sdg.layers().get(i);
            if (i < roles.size() && !l.isGroup() && l.paint() instanceof Sdg.Paint.Solid s) {
                l = new Sdg.Layer(l.name(), l.stencil(), s.as(roles.get(i)), l.opacity());
            }
            layers.add(l);
        }
        return new Sdg(sdg.width(), sdg.height(), sdg.pages(), layers);
    }
}
