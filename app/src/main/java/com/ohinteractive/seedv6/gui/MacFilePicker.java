package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.nio.file.Path;
import java.util.Optional;
import javax.swing.JOptionPane;

/** JDK 21's FileDialog delegates to AppKit NSOpenPanel, including folder mode. */
final class MacFilePicker implements FilePickers.Backend {
    private static final String DIRECTORY_PROPERTY = "apple.awt.fileDialogForDirectories";
    private static boolean showing;

    @Override public Optional<Path> show(Window owner, FilePickers.Request request) {
        // AWT exposes files OR directories on macOS, not both in one panel.
        // Keep both source types available through an explicit choice before the native panel.
        boolean directory = request.kind() == FilePickers.Kind.DIRECTORY;
        if (request.kind() == FilePickers.Kind.FILE_OR_DIRECTORY) {
            int choice = JOptionPane.showOptionDialog(owner, "Select a source file or a source folder?",
                    request.title(), JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, new String[] {"File...", "Folder...", "Cancel"}, "File...");
            if (choice < 0 || choice == 2) return Optional.empty();
            directory = choice == 1;
        }
        // FileDialog uses a process-wide property. Guard against nested pickers in its modal EDT loop.
        if (showing) throw new IllegalStateException("Another native picker is already open");
        String previous = System.getProperty(DIRECTORY_PROPERTY);
        FileDialog dialog = null;
        showing = true;
        try {
            dialog = owner instanceof Dialog d ? new FileDialog(d, request.title(), FileDialog.LOAD)
                    : new FileDialog(owner instanceof Frame f ? f : null, request.title(), FileDialog.LOAD);
            System.setProperty(DIRECTORY_PROPERTY, Boolean.toString(directory));
            dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setMultipleMode(false);
            if (request.directory() != null) dialog.setDirectory(request.directory().toString());
            if (!directory) dialog.setFilenameFilter((dir, name) -> request.purpose().acceptsName(name));
            dialog.setVisible(true);
            return dialog.getFile() == null ? Optional.empty()
                    : Optional.of(Path.of(dialog.getDirectory(), dialog.getFile()));
        } finally {
            if (dialog != null) dialog.dispose();
            if (previous == null) System.clearProperty(DIRECTORY_PROPERTY);
            else System.setProperty(DIRECTORY_PROPERTY, previous);
            showing = false;
        }
    }
}
