package com.ohinteractive.seedv6.gui;

import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.gui.FilePickers.Purpose.*;

class FilePickersTest {
    @TempDir Path temp;
    Preferences preferences;
    PickerLocations locations;
    @BeforeEach void setup() {
        preferences = Preferences.userRoot().node("seedv6-picker-test/" + UUID.randomUUID());
        locations = new PickerLocations(preferences, temp);
    }
    @AfterEach void cleanup() throws Exception { preferences.removeNode(); preferences.flush(); }

    @Test void everyPurposeIsIndependentAndSurvivesReload() throws Exception {
        assertEquals(FilePickers.Purpose.values().length, Arrays.stream(FilePickers.Purpose.values()).map(p -> p.key).distinct().count());
        for (var purpose : FilePickers.Purpose.values()) {
            Path folder = Files.createDirectory(temp.resolve(purpose.name()));
            locations.remember(purpose, folder);
        }
        var reloaded = new PickerLocations(Preferences.userRoot().node(preferences.absolutePath()), temp);
        for (var purpose : FilePickers.Purpose.values())
            assertEquals(temp.resolve(purpose.name()), reloaded.initial(purpose, temp.toString()));
    }

    @Test void approvalRemembersFilesParentAndDirectoryItself() throws Exception {
        Path directory = Files.createDirectory(temp.resolve("working"));
        Path file = Files.createFile(directory.resolve("archive.ZST"));
        var files = new FilePickers(locations, (owner, request) -> Optional.of(file));
        assertEquals(file, files.select(null, CORPUS_ARCHIVE, "Archive", "").orElseThrow());
        assertEquals(directory, locations.initial(CORPUS_ARCHIVE, ""));
        var folders = new FilePickers(locations, (owner, request) -> Optional.of(directory));
        folders.select(null, GENERATOR_STORE, "Generator", "");
        assertEquals(directory, locations.initial(GENERATOR_STORE, ""));
        assertNull(preferences.get(TEACHER_STORE.key, null));
    }

    @Test void cancellationDoesNotReplaceHistoryOrPersistDefault() throws Exception {
        Path remembered = Files.createDirectory(temp.resolve("remembered"));
        Path elsewhere = Files.createDirectory(temp.resolve("elsewhere"));
        locations.remember(ADD_TRAINING_DATA, remembered);
        var cancelled = new FilePickers(locations, (owner, request) -> {
            assertEquals(remembered, request.directory());
            return Optional.empty();
        });
        assertTrue(cancelled.select(null, ADD_TRAINING_DATA, "Add", elsewhere.toString()).isEmpty());
        assertEquals(remembered.toString(), preferences.get(ADD_TRAINING_DATA.key, null));
        new FilePickers(locations, (owner, request) -> Optional.empty())
                .select(null, RELOCATE_TRAINING_DATA, "Relocate", elsewhere.toString());
        assertNull(preferences.get(RELOCATE_TRAINING_DATA.key, null));
    }

    @Test void missingMalformedAndNonDirectoryHistoryFallsBackToConfiguredLocation() throws Exception {
        Path configured = Files.createDirectory(temp.resolve("configured"));
        Path file = Files.createFile(configured.resolve("source.zst"));
        for (String invalid : List.of(temp.resolve("deleted").toString(), "?:invalid-path", file.toString())) {
            preferences.put(CORPUS_ARCHIVE.key, invalid);
            assertEquals(configured, locations.initial(CORPUS_ARCHIVE, file.toString()));
        }
        preferences.put(CORPUS_ROOT.key, temp.resolve("disconnected").toString());
        assertEquals(configured, locations.initial(CORPUS_ROOT, configured.toString()));
    }

    @Test void validHistoryWinsThenConfiguredThenDocumentsThenHome() throws Exception {
        Path configured = Files.createDirectory(temp.resolve("configured"));
        Path remembered = Files.createDirectory(temp.resolve("remembered"));
        locations.remember(TRAINING_STORAGE, remembered);
        assertEquals(remembered, locations.initial(TRAINING_STORAGE, configured.toString()));
        Files.delete(remembered);
        assertEquals(configured, locations.initial(TRAINING_STORAGE, configured.toString()));
        Path documents = Files.createDirectory(temp.resolve("Documents"));
        assertEquals(documents, locations.initial(TRAINING_STORAGE, "invalid\0default"));
        Files.delete(documents);
        assertEquals(temp, locations.initial(TRAINING_STORAGE, ""));
    }

    @Test void invalidSelectionAndWrongExtensionDoNotUpdateHistory() throws Exception {
        Path rejected = Files.createFile(temp.resolve("source.gz"));
        var picker = new FilePickers(locations, (owner, request) -> Optional.of(rejected));
        assertThrows(java.io.IOException.class, () -> picker.select(null, CORPUS_ARCHIVE, "Archive", ""));
        assertThrows(java.io.IOException.class, () -> picker.select(null, TEACHER_STORE, "Teacher", ""));
        assertNull(preferences.get(CORPUS_ARCHIVE.key, null));
        assertNull(preferences.get(TEACHER_STORE.key, null));
    }

    @Test void mixedSourceAcceptsBothTypesWithoutAFileExtensionRestriction() throws Exception {
        Path file = Files.createFile(temp.resolve("source.jsonl"));
        new FilePickers(locations, (owner, request) -> Optional.of(file)).select(null, ADD_TRAINING_DATA, "Add", "");
        Path folder = Files.createDirectory(temp.resolve("shards"));
        new FilePickers(locations, (owner, request) -> Optional.of(folder)).select(null, ADD_TRAINING_DATA, "Add", "");
        assertEquals(folder, locations.initial(ADD_TRAINING_DATA, ""));
    }

    @Test void nonWindowsDispatchWorksWithoutAnyJnaOrWindowsClasses() throws Exception {
        try (var loader = new URLClassLoader(new java.net.URL[] {
                FilePickers.class.getProtectionDomain().getCodeSource().getLocation()
        }, ClassLoader.getPlatformClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("com.sun.jna") || name.endsWith("WindowsFilePicker"))
                    throw new ClassNotFoundException("Windows/JNA deliberately unavailable");
                return super.loadClass(name, resolve);
            }
        }) {
            var type = loader.loadClass(FilePickers.class.getName());
            var dispatch = type.getDeclaredMethod("platformBackend", String.class);
            dispatch.setAccessible(true);
            assertEquals("MacFilePicker", dispatch.invoke(null, "Mac OS X").getClass().getSimpleName());
            assertEquals("MacFilePicker", dispatch.invoke(null, "Darwin").getClass().getSimpleName());
            assertEquals("SwingFilePicker", dispatch.invoke(null, "Linux").getClass().getSimpleName());
        }
    }
}
