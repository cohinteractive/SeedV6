package com.ohinteractive.seedv6.gui;

import java.nio.file.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/** Picker history is distinct from editable application configuration, in the same Preferences tree. */
final class PickerLocations {
    private final Preferences preferences;
    private final Path home;
    PickerLocations(Preferences preferences) {
        this(preferences, Path.of(System.getProperty("user.home", ".")));
    }
    PickerLocations(Preferences preferences, Path home) {
        this.preferences = preferences; this.home = home.toAbsolutePath().normalize();
    }

    Path initial(FilePickers.Purpose purpose, String configuredPath) {
        Path remembered = path(preferences.get(purpose.key, ""));
        if (usable(remembered)) return remembered;
        Path configured = path(configuredPath);
        // Existing stored file paths (including the legacy corpus archive setting) seed their parent.
        if (configured != null && Files.isRegularFile(configured)) configured = configured.getParent();
        if (usable(configured)) return configured;
        Path documents = home.resolve("Documents");
        if (usable(documents)) return documents;
        return usable(home) ? home : null; // Let the native shell choose its default if home is unavailable.
    }

    void remember(FilePickers.Purpose purpose, Path directory) throws BackingStoreException {
        if (!usable(directory)) return;
        preferences.put(purpose.key, directory.toAbsolutePath().normalize().toString());
        preferences.flush(); // Persist on approval, without relying on graceful JVM shutdown.
    }

    private static Path path(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Path.of(value).toAbsolutePath().normalize(); }
        catch (InvalidPathException | SecurityException invalid) { return null; }
    }

    private static boolean usable(Path path) {
        if (path == null) return false;
        try { return Files.isDirectory(path) && Files.isReadable(path); }
        catch (SecurityException unavailable) { return false; }
    }
}
