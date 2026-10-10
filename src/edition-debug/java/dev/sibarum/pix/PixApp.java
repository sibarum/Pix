package dev.sibarum.pix;

import dev.vexelray.framework.api.VexelApp;
import dev.vexelray.framework.automation.AutomationStarter;

/**
 * The debug edition's application declaration: {@link Pix}'s facts plus {@link AutomationStarter}, the driving socket
 * (off unless {@code --automation} or {@code -Dautomation} asks, and loopback-only when it is).
 *
 * <p>The processor generates {@code PixAppWiring} from this. The release edition ({@code src/edition-release})
 * declares the same application with no starters and no dependency on the automation module. The pom chooses which
 * one is compiled (property {@code edition.src}); keep the two annotations identical apart from {@code starters}.
 */
@VexelApp(name = Pix.APP, title = Pix.TITLE, width = Pix.W, height = Pix.H, icon = "/pix.ico",
        starters = AutomationStarter.class)
final class PixApp {

    private PixApp() {
    }
}
