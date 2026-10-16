package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.training.data.DataFiles;
import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.util.HexFormat;

/** Same forced-file/atomic-publication contract as Training Data and checkpoint stores. */
final class LearningArenaFiles {
    interface Writer { void write(OutputStream out) throws IOException; }
    static void write(Path file, Writer writer) throws IOException {
        Files.createDirectories(file.getParent());
        Path pending = Files.createTempFile(file.getParent(), "arena-", ".pending");
        try {
            try (var channel = FileChannel.open(pending, StandardOpenOption.WRITE)) {
                var out = new BufferedOutputStream(Channels.newOutputStream(channel));
                writer.write(out); out.flush(); channel.force(true);
            }
            Files.move(pending, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            forceDirectory(file.getParent());
        } finally { Files.deleteIfExists(pending); }
    }
    static void forceDirectory(Path directory) throws IOException {
        try (var channel = FileChannel.open(directory, StandardOpenOption.READ)) { channel.force(true); }
        catch (AccessDeniedException | UnsupportedOperationException unsupported) { /* Windows, as in CheckpointStore. */ }
    }
    static String hash(Path file) throws IOException {
        var digest = DataFiles.digest();
        try (var in = new DigestInputStream(new BufferedInputStream(Files.newInputStream(file)), digest)) {
            in.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }
    private LearningArenaFiles() {}
}
