package com.ohinteractive.seedv6.training.checkpoint;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

/** Strict, bounded binary records: magic, version, kind, payload length, payload, SHA-256 of preceding bytes. */
final class SmallRecord {
    static final int VERSION = 1;
    static final int MAX_BYTES = 65536;
    // Largest GUI dataset plus bounded game/validation metadata. V1 frames remain byte-identical.
    static final long MAX_DATA_BYTES = 56L * Integer.MAX_VALUE + 128L * 1024 * 1024;
    private static final int MAGIC = 0x53364350;
    @FunctionalInterface interface Writer { void write(DataOutputStream out) throws IOException; }
    @FunctionalInterface interface Reader<T> { T read(DataInputStream in) throws IOException; }

    static byte[] encode(String kind, Writer writer) throws IOException {
        return encode(kind, writer, MAX_BYTES);
    }
    static byte[] encode(String kind, Writer writer, int maximum) throws IOException {
        var payload = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(payload)) { writer.write(out); }
        var framed = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(framed)) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeUTF(kind);
            out.writeInt(payload.size());
            payload.writeTo(out);
            out.flush();
            out.write(digest().digest(framed.toByteArray()));
        }
        if (framed.size() > maximum) throw new IOException("Record exceeds size limit.");
        return framed.toByteArray();
    }

    static <T> T read(Path path, String kind, Reader<T> reader) throws IOException {
        return read(path, kind, reader, MAX_BYTES);
    }
    static <T> T read(Path path, String kind, Reader<T> reader, long maximum) throws IOException {
        long size = Files.size(path);
        if (size < 48 || size > maximum) throw new IOException("Invalid record size: " + path);
        MessageDigest hash = digest();
        try (var source = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            byte[] buffer = new byte[65536];
            for (long remaining = size - 32; remaining > 0;) {
                checkInterrupted();
                int n = source.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (n < 0) throw new EOFException("Truncated record");
                hash.update(buffer, 0, n); remaining -= n;
            }
            if (!MessageDigest.isEqual(hash.digest(), source.readNBytes(32))) throw new IOException("Record SHA-256 mismatch.");
        }
        try (var in = new FileInput(path, size - 32)) {
            if (in.readInt() != MAGIC) throw new IOException("Unknown record format.");
            int version = in.readInt();
            if ((version != VERSION && version != 2) || !in.readUTF().equals(kind))
                throw new IOException("Unknown record format/version/kind.");
            long payload = version == VERSION ? in.readInt() : in.readLong();
            if (payload != remaining(in)) throw new IOException("Invalid record payload length.");
            T result = reader.read(in);
            if (in.read() != -1) throw new IOException("Trailing record payload.");
            return result;
        } catch (IllegalArgumentException | IllegalStateException invalid) {
            throw new IOException("Invalid record payload.", invalid);
        }
    }

    private static final class LimitedInput extends FilterInputStream {
        long remaining;
        LimitedInput(InputStream input, long count) { super(input); remaining = count; }
        public int available() { return (int) Math.min(Integer.MAX_VALUE, remaining); }
        public int read() throws IOException {
            if (remaining == 0) return -1;
            int b = in.read(); if (b < 0) throw new EOFException("Truncated record"); remaining--; return b;
        }
        public int read(byte[] bytes, int off, int length) throws IOException {
            if (length == 0) return 0;
            if (remaining == 0) return -1;
            int n = in.read(bytes, off, (int) Math.min(length, remaining));
            if (n < 0) throw new EOFException("Truncated record"); remaining -= n; return n;
        }
        public long skip(long n) throws IOException { long skipped = in.skip(Math.min(n, remaining)); remaining -= skipped; return skipped; }
    }
    private static final class FileInput extends DataInputStream {
        final Path path;
        final LimitedInput limited;
        final long length;
        FileInput(Path path, long length) throws IOException { this(path, length, new LimitedInput(new BufferedInputStream(Files.newInputStream(path)), length)); }
        private FileInput(Path path, long length, LimitedInput input) { super(input); this.path = path; this.length = length; limited = input; }
    }
    static java.util.List<com.ohinteractive.seedv6.training.selfplay.TrajectorySampler.Sample> samples(DataInputStream input, int count) throws IOException {
        if (count < 0 || (long) count * 56 > remaining(input)) throw new IOException("Invalid sample count");
        if (input instanceof FileInput file) {
            var result = new com.ohinteractive.seedv6.training.selfplay.SampleStore(file.path, file.length - file.limited.remaining, count);
            for (int i = 0; i < count; i++) {
                if ((i & 1023) == 0) checkInterrupted();
                input.skipNBytes(48);
                double target = input.readDouble();
                if (target != -1 && target != 0 && target != 1) throw new IOException("Invalid terminal sample target");
            }
            return result;
        }
        var result = com.ohinteractive.seedv6.training.selfplay.SampleStore.mutable();
        for (int i = 0; i < count; i++) {
            long[] board = new long[6]; for (int j = 0; j < board.length; j++) board[j] = input.readLong();
            result.add(new com.ohinteractive.seedv6.training.selfplay.TrajectorySampler.Sample(board, input.readDouble()));
        }
        return com.ohinteractive.seedv6.training.selfplay.SampleStore.copyOf(result);
    }

    private static final class CountingOutput extends FilterOutputStream {
        long count;
        CountingOutput(OutputStream out) { super(out); }
        public void write(int b) throws IOException { if ((count & 65535) == 0) checkInterrupted(); out.write(b); count++; }
        public void write(byte[] bytes, int off, int length) throws IOException { if ((count & 65535) == 0) checkInterrupted(); out.write(bytes, off, length); count += length; }
    }
    static void checkInterrupted() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Training record operation interrupted");
    }
    static long remaining(DataInputStream input) throws IOException {
        return input instanceof FileInput file ? file.limited.remaining : input.available();
    }
    /** V1 bytes for existing sizes; V2 extends only the framing length to 64 bits. */
    private static void stream(OutputStream destination, String kind, Writer writer, long maximum) throws IOException {
        var counter = new CountingOutput(OutputStream.nullOutputStream());
        writer.write(new DataOutputStream(counter));
        var header = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(header)) {
            boolean wide = counter.count > Integer.MAX_VALUE;
            out.writeInt(MAGIC); out.writeInt(wide ? 2 : VERSION); out.writeUTF(kind);
            if (counter.count + header.size() + (wide ? 8L : 4L) + 32 > maximum) throw new IOException("Record exceeds size limit.");
            if (wide) out.writeLong(counter.count); else out.writeInt((int) counter.count);
        }
        var digest = digest();
        var hashing = new java.security.DigestOutputStream(destination, digest);
        header.writeTo(hashing);
        var payload = new CountingOutput(hashing);
        var out = new DataOutputStream(payload); writer.write(out); out.flush();
        if (payload.count != counter.count) throw new IOException("Record changed during serialization");
        destination.write(digest.digest());
    }
    static void write(Path path, String kind, Writer writer, long maximum) throws IOException {
        try (var output = new BufferedOutputStream(Files.newOutputStream(path, java.nio.file.StandardOpenOption.CREATE_NEW))) {
            stream(output, kind, writer, maximum);
        }
        try (var file = java.nio.channels.FileChannel.open(path, java.nio.file.StandardOpenOption.WRITE)) { file.force(true); }
    }
    static String hash(String kind, Writer writer, long maximum) throws IOException {
        var digest = digest();
        stream(new java.security.DigestOutputStream(OutputStream.nullOutputStream(), digest), kind, writer, maximum);
        return HexFormat.of().formatHex(digest.digest());
    }

    static <T> T read(Path path, java.util.Map<String, Reader<T>> readers) throws IOException {
        long size = Files.size(path);
        if (size < 48 || size > MAX_BYTES) throw new IOException("Invalid record size: " + path);
        return decode(Files.readAllBytes(path), readers, MAX_BYTES);
    }

    static <T> T decode(byte[] bytes, String kind, Reader<T> reader) throws IOException {
        return decode(bytes, java.util.Map.of(kind, reader), MAX_BYTES);
    }
    private static <T> T decode(byte[] bytes, java.util.Map<String, Reader<T>> readers, int maximum) throws IOException {
        if (bytes.length < 48 || bytes.length > maximum) throw new IOException("Invalid record size.");
        byte[] body = Arrays.copyOf(bytes, bytes.length - 32);
        if (!MessageDigest.isEqual(digest().digest(body), Arrays.copyOfRange(bytes, body.length, bytes.length))) {
            throw new IOException("Record SHA-256 mismatch.");
        }
        try (var in = new DataInputStream(new ByteArrayInputStream(body))) {
            if (in.readInt() != MAGIC || in.readInt() != VERSION) {
                throw new IOException("Unknown record format/version/kind.");
            }
            Reader<T> reader = readers.get(in.readUTF());
            if (reader == null) throw new IOException("Unknown record kind.");
            if (in.readInt() != in.available()) throw new IOException("Invalid record payload length.");
            T result = reader.read(in);
            if (in.read() != -1) throw new IOException("Trailing record payload.");
            return result;
        } catch (IllegalArgumentException | IllegalStateException invalid) {
            throw new IOException("Invalid record payload.", invalid);
        }
    }

    static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    static String hash(byte[] bytes) { return HexFormat.of().formatHex(digest().digest(bytes)); }
    static String hash(Path path) throws IOException {
        MessageDigest digest = digest();
        try (InputStream input = new BufferedInputStream(Files.newInputStream(path))) {
            byte[] buffer = new byte[65536];
            for (int n; (n = input.read(buffer)) != -1;) { checkInterrupted(); digest.update(buffer, 0, n); }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
    static String requireHash(String hash) {
        if (hash == null || !hash.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid SHA-256.");
        return hash;
    }
    private SmallRecord() {}
}
