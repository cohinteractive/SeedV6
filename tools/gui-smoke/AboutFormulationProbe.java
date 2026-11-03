package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import com.formdev.flatlaf.util.ScaledImageIcon;
import com.ohinteractive.seedv6.ApplicationVersion;

/** Automated offscreen render of actual packaged About components, not interactive UI acceptance.
 * Compile against the GUI image's app/*; run with its runtime and a new output directory. */
public final class AboutFormulationProbe {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[0]);
        Files.createDirectory(output);
        // Resolve outside the EDT, just as Play does. Opening About never loads a model.
        var bindings = PlayParticipants.shared(PlayEvaluator.bundled(PlayEvaluator.Mode.CONVENTIONAL_CP64));
        SwingUtilities.invokeAndWait(() -> {
            try {
                SeedTheme.initialize();
                var version = ApplicationVersion.load();
                for (boolean started : new boolean[]{false, true}) {
                    String summary = AboutEnginePanel.play(bindings, started, false, GameController.GameMode.HUMAN_VS_ENGINE,
                            GameController.HumanSide.WHITE, 4, false);
                    String details = AboutEnginePanel.play(bindings, started, false, GameController.GameMode.HUMAN_VS_ENGINE,
                            GameController.HumanSide.WHITE, 4, true);
                    var panel = new AboutEnginePanel(version, summary, details);
                    var message = new JOptionPane(panel, JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION,
                            new ScaledImageIcon(new ImageIcon(ApplicationIcons.windowImages().getLast()), 64, 64));
                    message.setSize(message.getPreferredSize());
                    layout(message);
                    var image = new BufferedImage(message.getWidth(), message.getHeight(), BufferedImage.TYPE_INT_RGB);
                    var graphics = image.createGraphics();
                    message.printAll(graphics); graphics.dispose();
                    ImageIO.write(image, "png", output.resolve(started ? "active.png" : "idle.png").toFile());
                    Files.writeString(output.resolve(started ? "active.txt" : "idle.txt"), version.aboutText() + "\n" + summary + "\n" + details);
                }
                System.out.println(version.aboutText());
                System.out.println(bindings.white().baseline().formulation(4, 64).summary());
            } catch (Exception failure) { throw new RuntimeException(failure); }
        });
    }
    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) if (child instanceof Container nested) layout(nested);
    }
}
