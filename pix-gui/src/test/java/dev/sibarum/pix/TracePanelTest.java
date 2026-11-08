package dev.sibarum.pix;

import dev.sibarum.pix.core.Messages.TraceSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TracePanelTest {

    @Test
    void theSliderNamesEveryColourCountFromTheFewestToTheMost() {
        for (int n = TraceSettings.FEWEST; n <= TraceSettings.MOST; n++) {
            assertEquals(n, TracePanel.colours(TracePanel.slider(n)));
        }
        assertEquals(TraceSettings.FEWEST, TracePanel.colours(0));
        assertEquals(TraceSettings.MOST, TracePanel.colours(1));
    }
}
