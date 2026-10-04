package com.ohinteractive.seedv6.gui;

import java.awt.Component;
import java.awt.Window;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import javax.swing.*;

/** The only GUI entry point for user-selected filesystem paths. No engine dependencies. */
final class FilePickers {
    enum Kind { FILE, DIRECTORY, FILE_OR_DIRECTORY }

    enum Purpose {
        ADD_TRAINING_DATA("training-data.add", Kind.FILE_OR_DIRECTORY),
        RELOCATE_TRAINING_DATA("training-data.relocate", Kind.FILE_OR_DIRECTORY),
        TRAINING_STORAGE("training.storage", Kind.DIRECTORY),
        ADOPT_CHECKPOINT("training.adopt-checkpoint", Kind.DIRECTORY),
        GENERATOR_STORE("training.generator", Kind.DIRECTORY),
        TEACHER_STORE("training.teacher", Kind.DIRECTORY),
        CORPUS_ARCHIVE("corpus.archive", Kind.FILE),
        CORPUS_ROOT("corpus.root", Kind.DIRECTORY),
        WHITE_STORE("play.white", Kind.DIRECTORY),
        BLACK_STORE("play.black", Kind.DIRECTORY),
        OPPONENT_STORE("play.opponent", Kind.DIRECTORY);

        final String key;
        final Kind kind;
        Purpose(String key, Kind kind) { this.key = key; this.kind = kind; }
        boolean acceptsName(String name) {
            return this != CORPUS_ARCHIVE || name.toLowerCase(Locale.ROOT).endsWith(".zst");
        }
        static Purpose playStore(String side) {
            return switch (side) {
                case "white" -> WHITE_STORE;
                case "black" -> BLACK_STORE;
                case "opponent" -> OPPONENT_STORE;
                default -> throw new IllegalArgumentException("Unknown play picker: " + side);
            };
        }
    }

    record Request(Purpose purpose, String title, Path directory) {}
    interface Backend {
        Optional<Path> show(Window owner, Request request) throws Exception;
    }

    private final PickerLocations locations;
    private final Backend backend;
    FilePickers(PickerLocations locations, Backend backend) {
        this.locations = locations; this.backend = backend;
    }
    private static final class Application {
        static final FilePickers INSTANCE = new FilePickers(
                new PickerLocations(TrainingSettings.preferences().node("pickers")),
                platformBackend(System.getProperty("os.name", "")));
    }

    // Platform bindings are only initialized when that backend is selected.
    static Backend platformBackend(String osName) {
        String os = osName.toLowerCase(Locale.ROOT);
        if (os.startsWith("windows")) return new WindowsFilePicker();
        if (os.startsWith("mac") || os.equals("darwin")) return new MacFilePicker();
        return new SwingFilePicker();
    }

    static Optional<Path> choose(Component parent, Purpose purpose, String title, String configuredPath) {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Pickers must be opened on the Swing EDT");
        Window owner = parent instanceof Window window ? window : SwingUtilities.getWindowAncestor(parent);
        try {
            return Application.INSTANCE.select(owner, purpose, title, configuredPath);
        } catch (Exception | LinkageError failure) {
            JOptionPane.showMessageDialog(parent, "Could not open or complete the file picker: " + failure.getMessage(),
                    title, JOptionPane.ERROR_MESSAGE);
            return Optional.empty(); // Never silently substitute a Swing picker on Windows/macOS.
        }
    }

    Optional<Path> select(Window owner, Purpose purpose, String title, String configuredPath) throws Exception {
        Optional<Path> result = backend.show(owner, new Request(purpose, title, locations.initial(purpose, configuredPath)));
        if (result.isEmpty()) return result;
        Path selected = result.get().toAbsolutePath().normalize();
        boolean directory = Files.isDirectory(selected);
        if (!Files.isReadable(selected) || (!directory && !Files.isRegularFile(selected))
                || (purpose.kind == Kind.DIRECTORY && !directory)
                || (purpose.kind == Kind.FILE && directory)
                || (!directory && !purpose.acceptsName(selected.getFileName().toString())))
            throw new IOException("The selected item is unavailable or is not an allowed " +
                    (purpose.kind == Kind.DIRECTORY ? "folder." : "file or folder."));
        try {
            locations.remember(purpose, directory ? selected : selected.getParent());
        } catch (BackingStoreException | IllegalArgumentException | SecurityException failure) {
            // Selection remains usable even if the OS preference store is unavailable.
            JOptionPane.showMessageDialog(owner, "Selected " + selected +
                    "\nThe picker location could not be saved: " + failure.getMessage(),
                    title, JOptionPane.WARNING_MESSAGE);
        }
        return Optional.of(selected);
    }
}
