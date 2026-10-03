package com.ohinteractive.seedv6.training.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.HexFormat;

/** Small forced records with atomic replacement; no non-atomic fallback. */
public final class DataFiles {
    public static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    public static <T> T read(Path file, Class<T> type) throws IOException {
        try {
            T value = JSON.fromJson(Files.readString(file), type);
            if (value == null) throw new IllegalArgumentException("Empty record");
            return value;
        } catch (RuntimeException e) { throw new IOException("Invalid Training Data metadata: " + file, e); }
    }
    public static void write(Path file, Object value) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(file.toAbsolutePath().getParent(), file.getFileName() + ".", ".pending");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer bytes = ByteBuffer.wrap((JSON.toJson(value) + "\n").getBytes(StandardCharsets.UTF_8));
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            try (FileChannel directory = FileChannel.open(file.toAbsolutePath().getParent(), StandardOpenOption.READ)) { directory.force(true); }
            catch (AccessDeniedException | UnsupportedOperationException unsupported) { /* Same directory-force limitation as CheckpointStore on Windows. */ }
        } finally { Files.deleteIfExists(temporary); }
    }
    public static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }
    public static String hash(String value) {
        return HexFormat.of().formatHex(digest().digest(value.getBytes(StandardCharsets.UTF_8)));
    }
    private DataFiles() {}
}
