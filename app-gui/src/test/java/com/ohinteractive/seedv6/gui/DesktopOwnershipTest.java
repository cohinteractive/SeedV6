package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.service.TrainerService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Executes with app-gui's own runtime classpath, without the legacy application. */
class DesktopOwnershipTest {
    @Test void desktopResolvesSharedServicesIdentityAndPackagedArtworkWithoutLegacyApp() throws Exception {
        var loader = SwingLauncher.class.getClassLoader();
        assertThrows(ClassNotFoundException.class, () -> loader.loadClass("com.ohinteractive.seedv6.Main"));
        assertThrows(ClassNotFoundException.class, () -> loader.loadClass("com.ohinteractive.seedv6.uci.UciEngine"));
        assertNotEquals(SwingLauncher.class.getProtectionDomain().getCodeSource(), Board.class.getProtectionDomain().getCodeSource());
        assertNotEquals(SwingLauncher.class.getProtectionDomain().getCodeSource(), TrainerService.class.getProtectionDomain().getCodeSource());
        assertEquals(Board.class.getProtectionDomain().getCodeSource(), ApplicationVersion.class.getProtectionDomain().getCodeSource());
        assertFalse(ApplicationVersion.load().version().isBlank());
        assertEquals(10, ApplicationIcons.windowImages().size());
        assertNotNull(SwingLauncher.class.getResource("FlatDarkLaf.properties"));
        for (String set : new String[]{"original", "legacy"})
            for (String side : new String[]{"w", "b"})
                for (String piece : new String[]{"k", "q", "r", "b", "n", "p"})
                    assertNotNull(SwingLauncher.class.getResource("pieces/" + set + "/" + side + piece + ".png"));
    }
}
