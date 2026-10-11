package dev.sibarum.pix;

import dev.vexelray.codec.image.gui.ImageStarter;
import dev.vexelray.framework.api.VexelApp;

/**
 * The release edition's application declaration: {@link Pix}'s facts and {@link ImageStarter}. In particular no
 * {@code AutomationStarter}, and the pom drops {@code vexelray-framework-automation} from the classpath under
 * {@code -Pnative-release}, so a shipped binary cannot open a driving socket. The debug edition is
 * {@code src/edition-debug}; keep the two annotations identical apart from {@code AutomationStarter}.
 */
@VexelApp(name = Pix.APP, title = Pix.TITLE, width = Pix.W, height = Pix.H, icon = "/pix.ico",
        starters = ImageStarter.class)
final class PixApp {

    private PixApp() {
    }
}
