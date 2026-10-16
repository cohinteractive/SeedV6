package com.ohinteractive.seedv6.gui;

import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.ref.Reference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.*;
import javax.swing.*;

/** Windows Common Item Dialog. All COM pointers are created, used and released on one STA. */
final class WindowsFilePicker implements FilePickers.Backend {
    @Override public Optional<Path> show(Window owner, FilePickers.Request request) throws Exception {
        // A real Swing modal owner keeps the EDT pumping while the shell runs on its own STA.
        JDialog modal = new JDialog(owner, request.title(), Dialog.ModalityType.APPLICATION_MODAL);
        modal.setUndecorated(true);
        modal.setSize(1, 1);
        modal.setLocationRelativeTo(owner);
        if (modal.getGraphicsConfiguration().getDevice().isWindowTranslucencySupported(
                GraphicsDevice.WindowTranslucency.TRANSLUCENT)) modal.setOpacity(0);
        modal.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        var cancelled = new AtomicBoolean();
        var nativeWindow = new AtomicReference<WinDef.HWND>();
        var result = new AtomicReference<Optional<Path>>(Optional.empty());
        var failure = new AtomicReference<Throwable>();
        modal.addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent event) {
                WinDef.HWND handle = new WinDef.HWND(Native.getComponentPointer(modal));
                Thread.ofPlatform().daemon().name("seed-native-file-picker").start(() -> {
                    try {
                        check(Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_APARTMENTTHREADED
                                | Ole32.COINIT_DISABLE_OLE1DDE).intValue(), "Initialize picker COM apartment");
                        try (NativeDialog dialog = NativeDialog.open(request, cancelled, nativeWindow)) {
                            result.set(dialog.show(handle));
                        } finally { Ole32.INSTANCE.CoUninitialize(); }
                    } catch (Throwable error) { failure.set(error); }
                    finally {
                        nativeWindow.set(null);
                        SwingUtilities.invokeLater(modal::dispose);
                    }
                });
            }
            @Override public void windowClosed(WindowEvent event) {
                cancelled.set(true);
                WinDef.HWND handle = nativeWindow.get();
                if (handle != null) User32.INSTANCE.PostMessage(handle, WinUser.WM_CLOSE,
                        new WinDef.WPARAM(0), new WinDef.LPARAM(0));
            }
        });
        try { modal.setVisible(true); }
        finally { modal.dispose(); }
        if (failure.get() != null) throw new IOException("Windows native dialog failed: " + failure.get().getMessage(), failure.get());
        return result.get();
    }

    static final int CANCELLED = 0x800704c7;
    static final Guid.GUID OPEN_CLASS = guid("DC1C5A9C-E88A-4DDE-A5A1-60F82A20AEF7");
    static final Guid.GUID OPEN_IID = guid("D57C7288-D4AD-4768-BE02-9D969532D960");
    static final Guid.GUID SHELL_ITEM = guid("43826D1E-E718-42EE-BC55-A1E261C37BFE");
    static final Guid.GUID CUSTOMIZE = guid("E6FDD21A-163F-4975-9C8C-A69F1BA37034");
    static final Guid.GUID EVENTS = guid("973510DB-7D7F-452B-8975-74A85828D354");
    static final Guid.GUID CONTROLS = guid("36116642-D713-4B97-9B83-7484A9D00433");
    static final Guid.GUID UNKNOWN = guid("00000000-0000-0000-C000-000000000046");
    static final Guid.GUID OLE_WINDOW = guid("00000114-0000-0000-C000-000000000046");
    private static final int SELECT_FOLDER = 1001;

    private static Guid.GUID guid(String value) { return new Guid.GUID(value); }
    static void check(int hr, String operation) {
        if (hr < 0) throw new IllegalStateException(operation + " (HRESULT 0x" + Integer.toHexString(hr) + ")");
    }

    interface Shell extends StdCallLibrary {
        Shell INSTANCE = Native.load("shell32", Shell.class);
        int SHCreateItemFromParsingName(WString name, Pointer context, Guid.GUID iid, PointerByReference result);
    }
    @Structure.FieldOrder({"name", "pattern"})
    public static class Filter extends Structure {
        public WString name = new WString("Lichess JSONL Zstandard archive (*.jsonl.zst)");
        public WString pattern = new WString("*.zst");
    }

    /** Slots follow the Windows SDK shobjidl_core.h interfaces, including inherited IUnknown. */
    static final class Com extends Unknown implements AutoCloseable {
        Com(Pointer pointer) { super(pointer); }
        int call(int slot, Object... arguments) {
            Object[] nativeArguments = new Object[arguments.length + 1];
            nativeArguments[0] = getPointer();
            System.arraycopy(arguments, 0, nativeArguments, 1, arguments.length);
            return _invokeNativeInt(slot, nativeArguments);
        }
        void require(int slot, String operation, Object... arguments) { check(call(slot, arguments), operation); }
        Com query(Guid.GUID iid) {
            var pointer = new PointerByReference();
            iid.write();
            check(QueryInterface(new Guid.REFIID(iid.getPointer()), pointer).intValue(), "Query native dialog interface");
            return new Com(pointer.getValue());
        }
        @Override public void close() { Release(); }
    }

    static final class NativeDialog implements AutoCloseable {
        final Com dialog;
        final Events events;
        private final Filter filter = new Filter(); // Keep native strings alive through Show.
        private final IntByReference cookie = new IntByReference();
        private boolean advised;

        private NativeDialog(Com dialog, AtomicBoolean cancelled, AtomicReference<WinDef.HWND> window) {
            this.dialog = dialog;
            events = new Events(dialog, cancelled, window);
        }
        static NativeDialog open(FilePickers.Request request, AtomicBoolean cancelled,
                                 AtomicReference<WinDef.HWND> window) {
            var pointer = new PointerByReference();
            check(Ole32.INSTANCE.CoCreateInstance(OPEN_CLASS, null, WTypes.CLSCTX_INPROC_SERVER,
                    OPEN_IID, pointer).intValue(), "Create Windows Common Item Dialog");
            var session = new NativeDialog(new Com(pointer.getValue()), cancelled, window);
            try { session.configure(request); return session; }
            catch (RuntimeException | Error error) { session.close(); throw error; }
        }
        private void configure(FilePickers.Request request) {
            var options = new IntByReference();
            dialog.require(10, "Read picker options", options);
            // FORCEFILESYSTEM | PATHMUSTEXIST | FILEMUSTEXIST | NOCHANGEDIR | DONTADDTORECENT.
            int flags = options.getValue() | 0x40 | 0x800 | 0x1000 | 0x8 | 0x02000000;
            if (request.kind() == FilePickers.Kind.DIRECTORY) flags |= 0x20; // FOS_PICKFOLDERS
            flags &= ~0x200; // FOS_ALLOWMULTISELECT: every Seed picker is single-selection.
            dialog.require(9, "Set picker options", flags);
            dialog.require(17, "Set picker title", new WString(request.title()));
            dialog.require(24, "Set picker identity", guid(UUID.nameUUIDFromBytes(
                    ("SeedV6.picker." + request.purpose().key).getBytes(StandardCharsets.UTF_8)).toString()));
            if (request.purpose() == FilePickers.Purpose.CORPUS_ARCHIVE) {
                filter.write();
                dialog.require(4, "Set archive filter", 1, filter.getPointer());
            }
            if (request.directory() != null) {
                var folder = new PointerByReference();
                int hr = Shell.INSTANCE.SHCreateItemFromParsingName(new WString(request.directory().toString()),
                        null, SHELL_ITEM, folder);
                // A removable/network location can disappear between validation and shell resolution.
                if (hr >= 0) try (var item = new Com(folder.getValue())) {
                    dialog.call(12, item.getPointer()); // SetFolder overrides shell's own last-location cache.
                }
            }
            events.mixed = request.kind() == FilePickers.Kind.FILE_OR_DIRECTORY;
            if (events.mixed) {
                try (var customize = dialog.query(CUSTOMIZE)) {
                    customize.require(5, "Add folder selection button", SELECT_FOLDER, new WString("Select this folder"));
                }
            }
            dialog.require(7, "Attach picker events", events.events.pointer, cookie);
            advised = true;
        }
        Optional<Path> show(WinDef.HWND owner) {
            if (events.cancelled.get()) return Optional.empty();
            int hr = dialog.call(3, owner);
            if (events.failure != null) throw new IllegalStateException("Native picker event failed", events.failure);
            if (hr == CANCELLED) return Optional.empty();
            check(hr, "Show picker");
            return Optional.of(events.folder == null ? pathFromDialog(dialog, 20) : events.folder);
        }
        @Override public void close() {
            try { if (advised) dialog.require(8, "Detach picker events", cookie.getValue()); }
            finally {
                dialog.close();
                Reference.reachabilityFence(events);
                Reference.reachabilityFence(filter);
            }
        }
    }

    static Path pathFromDialog(Com dialog, int slot) {
        var pointer = new PointerByReference();
        dialog.require(slot, "Read selected shell item", pointer);
        try (var item = new Com(pointer.getValue())) {
            var text = new PointerByReference();
            item.require(5, "Read filesystem path", 0x80058000, text); // SIGDN_FILESYSPATH
            try { return Path.of(text.getValue().getWideString(0)); }
            finally { Ole32.INSTANCE.CoTaskMemFree(text.getValue()); }
        }
    }

    // Callback signatures use stdcall on x86 and the unified Windows convention on x64/ARM64.
    public interface Query extends StdCallLibrary.StdCallCallback { int invoke(Pointer self, Pointer iid, Pointer out); }
    public interface Ref extends StdCallLibrary.StdCallCallback { int invoke(Pointer self); }
    public interface Event extends StdCallLibrary.StdCallCallback { int invoke(Pointer self, Pointer dialog); }
    public interface Changing extends StdCallLibrary.StdCallCallback { int invoke(Pointer self, Pointer dialog, Pointer item); }
    public interface Response extends StdCallLibrary.StdCallCallback { int invoke(Pointer self, Pointer dialog, Pointer item, Pointer response); }
    public interface Control extends StdCallLibrary.StdCallCallback { int invoke(Pointer self, Pointer customize, int id); }
    public interface ItemControl extends StdCallLibrary.StdCallCallback { int invoke(Pointer self, Pointer customize, int id, int value); }

    static final class InterfaceMemory {
        final Callback[] callbacks;
        final Memory table, pointer;
        InterfaceMemory(Callback... callbacks) {
            this.callbacks = callbacks;
            table = new Memory((long) Native.POINTER_SIZE * callbacks.length);
            for (int i = 0; i < callbacks.length; i++)
                table.setPointer((long) i * Native.POINTER_SIZE, CallbackReference.getFunctionPointer(callbacks[i]));
            pointer = new Memory(Native.POINTER_SIZE);
            pointer.setPointer(0, table);
        }
    }

    /** One COM identity with IFileDialogEvents and IFileDialogControlEvents vtables. */
    static final class Events {
        final Com dialog;
        final AtomicBoolean cancelled;
        final AtomicReference<WinDef.HWND> window;
        final AtomicInteger references = new AtomicInteger(1);
        final InterfaceMemory events, controls;
        Path folder;
        boolean mixed;
        Throwable failure;
        Events(Com dialog, AtomicBoolean cancelled, AtomicReference<WinDef.HWND> window) {
            this.dialog = dialog; this.cancelled = cancelled; this.window = window;
            Query query = (self, iid, out) -> {
                Guid.GUID requested = new Guid.GUID(iid);
                Pointer selected = requested.equals(UNKNOWN) || requested.equals(EVENTS) ? eventPointer()
                        : requested.equals(CONTROLS) ? controlPointer() : null;
                out.setPointer(0, selected);
                if (selected == null) return 0x80004002; // E_NOINTERFACE
                references.incrementAndGet(); return 0;
            };
            Ref add = self -> references.incrementAndGet(), release = self -> references.decrementAndGet();
            Event noop = (self, pfd) -> 0;
            Changing changing = (self, pfd, item) -> 0;
            Response response = (self, pfd, item, answer) -> { answer.setInt(0, 0); return 0; };
            Event changed = (self, pfd) -> safe(() -> {
                try (var oleWindow = dialog.query(OLE_WINDOW)) {
                    var hwnd = new PointerByReference();
                    oleWindow.require(3, "Read picker window", hwnd);
                    window.set(new WinDef.HWND(hwnd.getValue()));
                }
                if (mixed) {
                    boolean filesystem;
                    try { pathFromDialog(dialog, 13); filesystem = true; }
                    catch (IllegalStateException virtualFolder) { filesystem = false; }
                    try (var customize = dialog.query(CUSTOMIZE)) {
                        // Virtual locations such as This PC can be browsed but cannot become a Path.
                        customize.require(14, "Update folder selection", SELECT_FOLDER, filesystem ? 3 : 2);
                    }
                }
                if (cancelled.get()) dialog.require(23, "Cancel disposed picker", CANCELLED);
            });
            events = new InterfaceMemory(query, add, release, noop, changing, changed, noop, response, noop, response);
            Control button = (self, pfdc, id) -> safe(() -> {
                if (id == SELECT_FOLDER) {
                    // Navigation remains ordinary Explorer navigation. This accepts the displayed folder.
                    folder = pathFromDialog(dialog, 13);
                    dialog.require(23, "Accept folder", 0);
                }
            });
            Control activating = (self, pfdc, id) -> 0;
            ItemControl item = (self, pfdc, id, value) -> 0;
            controls = new InterfaceMemory(query, add, release, item, button, item, activating);
        }
        Pointer eventPointer() { return events.pointer; }
        Pointer controlPointer() { return controls.pointer; }
        int safe(Runnable callback) {
            try { callback.run(); return 0; }
            catch (Throwable error) {
                failure = error;
                dialog.call(23, CANCELLED);
                return 0x80004005; // No Java exception may unwind across a native callback.
            }
        }
    }
}
