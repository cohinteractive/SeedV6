package com.ohinteractive.seedv6.gui;

import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.ptr.IntByReference;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises real COM binding/configuration/cleanup, without opening an interactive dialog. */
@EnabledOnOs(OS.WINDOWS)
class WindowsFilePickerTest {
    @TempDir Path temp;
    @Test void configuresFileFolderAndMixedDialogsOnAnSta() throws Exception {
        Path directory = Files.createDirectory(temp.resolve("Picker space-資料"));
        var task = new FutureTask<Void>(() -> {
            WindowsFilePicker.check(Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_APARTMENTTHREADED).intValue(), "COM init");
            try {
                for (var request : new FilePickers.Request[] {
                        new FilePickers.Request(FilePickers.Purpose.CORPUS_ARCHIVE, "Archive", directory),
                        new FilePickers.Request(FilePickers.Purpose.CORPUS_ROOT, "Folder", directory),
                        new FilePickers.Request(FilePickers.Purpose.ADD_TRAINING_DATA, "Mixed", directory),
                        new FilePickers.Request(FilePickers.Purpose.ADD_TRAINING_DATA, "Add file", directory, FilePickers.Kind.FILE),
                        new FilePickers.Request(FilePickers.Purpose.ADD_TRAINING_DATA, "Add folder", directory, FilePickers.Kind.DIRECTORY)}) {
                    try (var session = WindowsFilePicker.NativeDialog.open(request, new AtomicBoolean(), new AtomicReference<>())) {
                        assertEquals(directory, WindowsFilePicker.pathFromDialog(session.dialog, 13));
                        var options = new IntByReference();
                        session.dialog.require(10, "Read options", options);
                        assertEquals(request.kind() == FilePickers.Kind.DIRECTORY, (options.getValue() & 0x20) != 0);
                        assertEquals(request.kind() == FilePickers.Kind.FILE_OR_DIRECTORY, session.events.mixed);
                        assertEquals(0, options.getValue() & 0x200);
                        assertNotEquals(0, options.getValue() & 0x40);
                        // Verify COM can query the second event interface and balance its references.
                        try (var controls = new WindowsFilePicker.Com(session.events.events.pointer).query(WindowsFilePicker.CONTROLS)) {
                            assertNotNull(controls.getPointer());
                        }
                    }
                }
            } finally { Ole32.INSTANCE.CoUninitialize(); }
            return null;
        });
        Thread.ofPlatform().daemon().start(task);
        task.get(30, TimeUnit.SECONDS);
    }
}
