package dev.sibarum.pix.core;

import dev.sibarum.pix.core.Messages.Create;
import dev.sibarum.pix.core.Messages.Created;
import dev.vexelray.framework.api.Job;
import sibarum.probe.Log;

import java.io.IOException;
import java.nio.file.Files;

/** Make an empty file, which the path bar named into being. */
@Job
public final class CreateFile {

    private static final Log LOG = Log.of("pix.files");

    /** A new file gets this platform's line endings: asked here, so the editor never reads the platform. */
    private static final boolean CRLF = System.lineSeparator().equals("\r\n");

    public Created run(Create c) {
        try {
            Files.createFile(c.path());
            return new Created(c.path(), CRLF, "");
        } catch (IOException e) {
            LOG.warn("could not create {}", c.path(), e);
            return new Created(c.path(), CRLF, String.valueOf(e.getMessage()));
        }
    }
}
