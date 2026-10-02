package com.ohinteractive.seedv6.gui;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.training.service.ValidationMethod;
import static org.junit.jupiter.api.Assertions.*;

class TrainingSearchThreadsTest {
    @TempDir Path root;

    @Test void threadControlDescribesPerSearchCapacityAndKeepsItsBounds() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var settings = TrainingSettings.defaults(root, NetworkArchitecture.NNUE).withValidationMethod(ValidationMethod.GAME_PAIRS);
            var panel = new TrainingPanel(settings);
            var threads = (JSpinner) named(panel, "trainingThreads");
            var model = (SpinnerNumberModel) threads.getModel();
            assertEquals(1, model.getMinimum());
            assertEquals(16, model.getMaximum());
            assertEquals(1, model.getValue());
            assertEquals("Maximum search workers per search, including the main worker. Games run sequentially.",
                    threads.getToolTipText());
        });
    }

    private static Component named(Component component, String name) {
        if (name.equals(component.getName())) return component;
        if (component instanceof Container container)
            for (Component child : container.getComponents()) {
                Component found = named(child, name);
                if (found != null) return found;
            }
        return null;
    }
}
