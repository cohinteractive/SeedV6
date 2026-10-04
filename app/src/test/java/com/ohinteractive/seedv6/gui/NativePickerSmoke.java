package com.ohinteractive.seedv6.gui;

import java.awt.BorderLayout;
import java.nio.file.*;
import java.util.prefs.Preferences;
import javax.swing.*;

/**
 * Opt-in interactive QA with isolated preferences; never shipped in application distributions.
 * Arguments: fixture-directory isolated-preferences-node [cleanup].
 */
public final class NativePickerSmoke {
    public static void main(String[] args) throws Exception {
        Path fixtures = Path.of(args[0]).toAbsolutePath();
        Preferences preferences = Preferences.userRoot().node(args[1]);
        if (args.length > 2 && args[2].equals("cleanup")) {
            preferences.removeNode(); preferences.flush(); return;
        }
        SwingUtilities.invokeLater(() -> {
            var picker = new FilePickers(new PickerLocations(preferences),
                    FilePickers.platformBackend(System.getProperty("os.name")));
            JFrame frame = new JFrame("SeedV6 native picker smoke");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            JTextArea results = new JTextArea(7, 70); results.setEditable(false);
            JPanel buttons = new JPanel();
            for (var purpose : new FilePickers.Purpose[] {FilePickers.Purpose.ADD_TRAINING_DATA,
                    FilePickers.Purpose.GENERATOR_STORE, FilePickers.Purpose.CORPUS_ARCHIVE}) {
                String title = switch (purpose) {
                    case ADD_TRAINING_DATA -> "Add Training Data source";
                    case GENERATOR_STORE -> "Select NNUE generator store";
                    default -> "Select Lichess evaluated positions (.jsonl.zst)";
                };
                JButton button = new JButton(title); buttons.add(button);
                button.addActionListener(event -> {
                    try {
                        var selected = picker.select(frame, purpose, title, fixtures.toString());
                        results.append(purpose + ": " + selected.map(Path::toString).orElse("CANCEL") + "\n");
                        results.append("History: " + preferences.get(purpose.key, "(unset)") + "\n");
                    } catch (Exception error) { results.append("ERROR: " + error + "\n"); }
                });
            }
            JLabel heartbeat = new JLabel();
            Timer timer = new Timer(250, event -> heartbeat.setText("EDT active: " + System.nanoTime()));
            frame.add(buttons, BorderLayout.NORTH); frame.add(new JScrollPane(results)); frame.add(heartbeat, BorderLayout.SOUTH);
            frame.pack(); frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setLocationRelativeTo(null); frame.setVisible(true); timer.start();
        });
    }
}
