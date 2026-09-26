package com.ohinteractive.seedv6.gui;

import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApplicationMenuTest {
    @Test void menusContainOnlyRequiredItemsAndDelegateToSuppliedActions() throws Exception {
        var shutdowns = new AtomicInteger(); var dialogs = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            var menu = ApplicationMenu.create(shutdowns::incrementAndGet, dialogs::incrementAndGet);
            assertEquals(2, menu.getMenuCount());
            assertEquals("File", menu.getMenu(0).getText());
            assertEquals("Help", menu.getMenu(1).getText());
            assertEquals(1, menu.getMenu(0).getItemCount());
            assertEquals(1, menu.getMenu(1).getItemCount());
            assertEquals("Exit", menu.getMenu(0).getItem(0).getText());
            assertEquals("About", menu.getMenu(1).getItem(0).getText());
            menu.getMenu(1).getItem(0).doClick();
            assertEquals(1, dialogs.get()); assertEquals(0, shutdowns.get());
            menu.getMenu(0).getItem(0).doClick();
            assertEquals(1, shutdowns.get()); assertEquals(1, dialogs.get());
        });
    }
}
