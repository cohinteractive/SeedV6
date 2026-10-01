package com.ohinteractive.seedv6.search.tablebase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Properties;

/** Optional process-owned small-table provider. Initialization and native probes are serialized. */
public final class SyzygyNative {
    private SyzygyNative() {}

    /** Process configuration is fixed before first use; no default loading or downloading. */
    public static RootTablebase configured() { return Configured.ROOT; }

    private static final class Configured {
        static final RootTablebase ROOT = openConfigured();
    }

    private static RootTablebase openConfigured() {
        try {
            Properties settings = new Properties();
            String configuration = System.getProperty("seedv6.syzygy.config", "");
            if(!configuration.isBlank()) {
                try(var reader = Files.newBufferedReader(Path.of(configuration), StandardCharsets.UTF_8)) {
                    settings.load(reader);
                }
            }
            String configured = System.getProperty("seedv6.syzygy.path", settings.getProperty("path", ""));
            if(configured.isBlank()) return RootTablebase.NONE;
            Path path = Path.of(configured).toRealPath();
            verifyTables(path);
            loadLibrary(System.getProperty("seedv6.syzygy.library", settings.getProperty("library", "")));
            // Java UTF-8 bytes, not JNI modified UTF-8, support supplementary path characters.
            byte[] encoded = path.toString().getBytes(StandardCharsets.UTF_8);
            synchronized(SyzygyNative.class) {
                if(initialize(encoded) != 4) throw new IOException("The verified small table set did not initialize.");
            }
            return new SyzygyRoot(SyzygyNative::probe);
        } catch(IOException | NoSuchAlgorithmException | RuntimeException | LinkageError failure) {
            System.err.println("SeedV6 tablebase disabled: " + failure.getMessage());
            return RootTablebase.NONE;
        }
    }

    static void verifyTables(Path path) throws IOException, NoSuchAlgorithmException {
        if(!Files.isDirectory(path)) throw new IOException("Tablebase path is not a directory.");
        Properties hashes = new Properties();
        try(var input = SyzygyNative.class.getResourceAsStream("small-tables.sha256")) {
            if(input == null) throw new IOException("Missing tablebase integrity manifest.");
            hashes.load(input);
        }
        if(hashes.size() != 70) throw new IOException("Invalid tablebase integrity manifest.");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for(String name : hashes.stringPropertyNames()) {
            // The maintained small set is under 5 MiB in total; never read an unbounded input.
            Path file = path.resolve(name);
            long size = Files.size(file);
            if(size < 16 || size > 2_000_000) throw new IOException("Unsupported tablebase file: " + name);
            byte[] bytes;
            try(var input = Files.newInputStream(file)) { bytes = input.readNBytes(2_000_001); }
            if(bytes.length != size) throw new IOException("Tablebase file changed while reading: " + name);
            if(!HexFormat.of().formatHex(digest.digest(bytes)).equals(hashes.getProperty(name)))
                throw new IOException("Tablebase checksum mismatch: " + name);
        }
    }

    private static void loadLibrary(String explicit) throws IOException, NoSuchAlgorithmException {
        if(!explicit.isBlank()) {
            System.load(Path.of(explicit).toRealPath().toString());
            return;
        }
        String name = System.mapLibraryName("seedv6_syzygy");
        try(var input = SyzygyNative.class.getResourceAsStream("/native/" + name)) {
            if(input == null) throw new IOException("This build has no optional Syzygy native library.");
            byte[] bytes = input.readAllBytes();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String hash = HexFormat.of().formatHex(digest.digest(bytes));
            Path directory = Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), "seedv6-syzygy"));
            Path library = directory.resolve(hash + "-" + name);
            // Windows cannot remove a loaded DLL during Java's delete-on-exit hook.
            // Reuse one verified immutable cache entry per bundled binary instead.
            if(!Files.isRegularFile(library)) {
                Path staging = Files.createTempFile(directory, "extract-", ".tmp");
                try {
                    Files.write(staging, bytes);
                    try { Files.move(staging, library, java.nio.file.StandardCopyOption.ATOMIC_MOVE); }
                    catch(IOException failure) { if(!Files.isRegularFile(library)) throw failure; }
                } finally { Files.deleteIfExists(staging); }
            }
            if(Files.size(library) != bytes.length
                    || !HexFormat.of().formatHex(digest.digest(Files.readAllBytes(library))).equals(hash))
                throw new IOException("Native tablebase cache checksum mismatch.");
            System.load(library.toAbsolutePath().toString());
        }
    }

    private static synchronized int probe(long[] board) {
        return root(board[0], board[1], board[2], board[3], (int)board[4]);
    }

    // Maps remain process-owned; no consumer may unload while another one probes.
    private static native int initialize(byte[] utf8Path);
    private static native int root(long b0, long b1, long b2, long b3, int status);
}
