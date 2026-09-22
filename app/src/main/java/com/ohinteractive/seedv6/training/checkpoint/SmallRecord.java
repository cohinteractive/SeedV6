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
    static <T> T read(Path path, String kind, Reader<T> reader, int maximum) throws IOException {
        long size = Files.size(path);
        if (size < 48 || size > maximum) throw new IOException("Invalid record size: " + path);
        return decode(Files.readAllBytes(path), kind, reader, maximum);
    }

    static <T> T decode(byte[] bytes, String kind, Reader<T> reader) throws IOException {
        return decode(bytes, kind, reader, MAX_BYTES);
    }
    private static <T> T decode(byte[] bytes, String kind, Reader<T> reader, int maximum) throws IOException {
        if (bytes.length < 48 || bytes.length > maximum) throw new IOException("Invalid record size.");
        byte[] body = Arrays.copyOf(bytes, bytes.length - 32);
        if (!MessageDigest.isEqual(digest().digest(body), Arrays.copyOfRange(bytes, body.length, bytes.length))) {
            throw new IOException("Record SHA-256 mismatch.");
        }
        try (var in = new DataInputStream(new ByteArrayInputStream(body))) {
            if (in.readInt() != MAGIC || in.readInt() != VERSION || !in.readUTF().equals(kind)) {
                throw new IOException("Unknown record format/version/kind.");
            }
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
            for (int n; (n = input.read(buffer)) != -1;) digest.update(buffer, 0, n);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
    static String requireHash(String hash) {
        if (hash == null || !hash.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid SHA-256.");
        return hash;
    }
    private SmallRecord() {}
}
