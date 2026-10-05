package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.telemetry.OptimizationSnapshot;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.edt;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static org.junit.jupiter.api.Assertions.*;

class TrainingProgressViewTest {
    @Test void actualRecipeExposureAndOptimizerMetricsRenderAndUnknownsStayUnknown() throws Exception {
        edt(() -> {
            SeedTheme.initialize(); var view = new TrainingProgressView("testProgress");
            view.showProgress(new OptimizationSnapshot("Relational experiment - BRN-3", "Round 4 - training from Gen 3", 8, 128, .003,
                    65536, 1048576, 512, 1500, 2012, .034512, 20_000_000_000L, "This training segment", 3211264L, 4096));
            assertEquals("LR: 0.003", named(view, "testProgressLearningRate", JLabel.class).getText());
            assertTrue(named(view, "testProgressStep", JLabel.class).getText().contains("2,012"));
            assertTrue(named(view, "testProgressDetail", JTextArea.class).getText().contains("204.8 visits/s"));
            assertTrue(named(view, "testProgressSamples", JProgressBar.class).getString().contains("1,048,576"));
            if (!GraphicsEnvironment.isHeadless()) {
                var frame = new JFrame("Shared optimizer progress"); frame.setContentPane(view); frame.setSize(410, 330); frame.setVisible(true);
                try {
                    frame.validate(); var image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
                    var g = image.createGraphics(); frame.paint(g); g.dispose();
                    try { Path path = Path.of("build/learning-arena/training-progress.png"); Files.createDirectories(path.getParent()); ImageIO.write(image, "png", path.toFile()); }
                    catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
                } finally { frame.dispose(); }
            }
            var snapshot = TrainingDashboardTest.snapshot(com.ohinteractive.seedv6.training.service.TrainerSnapshot.State.TRAINING, false, false, false);
            view.showTraining(TrainingDashboardTest.view(snapshot));
            assertTrue(named(view, "testProgressLearningRate", JLabel.class).getText().contains("unknown"));
            assertTrue(named(view, "testProgressExposure", JLabel.class).getText().contains("unknown"));
            view.showProgress(null); assertEquals(0, named(view, "testProgressSamples", JProgressBar.class).getValue());
        });
    }
}
