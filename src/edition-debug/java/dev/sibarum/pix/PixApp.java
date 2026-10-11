package dev.sibarum.pix;

import dev.vexelray.codec.image.gui.ImageStarter;
import dev.vexelray.framework.api.VexelApp;
import dev.vexelray.framework.automation.AutomationStarter;

/**
 * The debug edition's application declaration: {@link Pix}'s facts, {@link ImageStarter} (the textures, bound to the
 * window), and {@link AutomationStarter}, the driving socket (off unless {@code --automation} or {@code -Dautomation}
 * asks, and loopback-only when it is).
 *
 * <p>The processor generates {@code PixAppWiring} from this. The release edition ({@code src/edition-release})
 * declares the same application without {@code AutomationStarter} and with no dependency on the automation module.
 * The pom chooses which one is compiled (property {@code edition.src}); keep the two annotations identical apart from
 * {@code AutomationStarter}.
 */
@VexelApp(name = Pix.APP, title = Pix.TITLE, width = Pix.W, height = Pix.H, icon = "/pix.ico",
        starters = {ImageStarter.class, AutomationStarter.class})
final class PixApp {

    private PixApp() {
    }
}
