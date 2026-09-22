package com.ohinteractive.seedv6.training.history;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static java.nio.file.StandardOpenOption.*;

/** Single trainer-owned writer (under the existing store ownership); independent read-only GUI instances.
 * No shared monitor, EDT callback, checkpoint format change, or search-path work.
 */
public final class HistoryRepository {
    public static final String FILE = "history/generations-v1.tsv";
    public record Snapshot(List<GenerationRecord> records, List<String> warnings) {
        public static final Snapshot EMPTY = new Snapshot(List.of(), List.of());
        public Snapshot { records = List.copyOf(records); warnings = List.copyOf(warnings); }
    }
    private final Path file;
    private Snapshot cached;
    private long size = -1;
    private java.nio.file.attribute.FileTime modified;
    private boolean unterminated, unsupported;
    private final Map<String, GenerationRecord> identities = new HashMap<>();
    private final Set<Long> generations = new HashSet<>();
    public HistoryRepository(Path checkpointRoot) { file = checkpointRoot.resolve(FILE); }
    public Path file() { return file; }

    /** Call on an I/O thread; unchanged files return the exact same immutable snapshot. */
    public Snapshot refresh() throws IOException {
        java.nio.file.attribute.BasicFileAttributes attributes;
        try { attributes = Files.readAttributes(file, java.nio.file.attribute.BasicFileAttributes.class); }
        catch (NoSuchFileException missing) {
            if (cached == null || size != 0) { cached = Snapshot.EMPTY; size = 0; modified = null; identities.clear(); generations.clear(); unterminated = false; unsupported = false; }
            return cached;
        }
        if (!attributes.isRegularFile()) throw new IOException("History is not a regular file: " + file);
        if (cached != null && size == attributes.size() && Objects.equals(modified, attributes.lastModifiedTime())) return cached;
        List<GenerationRecord> records = new ArrayList<>(); List<String> warnings = new ArrayList<>();
        identities.clear(); generations.clear(); unterminated = false; unsupported = false;
        int lineNumber = 1, errors = 0; long remaining = attributes.size();
        // Bound individual line memory, and read only the observed prefix while another process appends.
        try (InputStream input = new BufferedInputStream(Files.newInputStream(file))) {
            ByteArrayOutputStream line = new ByteArrayOutputStream(); boolean oversized = false;
            while (remaining-- > 0) {
                int c = input.read(); if (c < 0) break;
                if (c == '\n') {
                    String text = line.toString(StandardCharsets.UTF_8);
                    if (text.endsWith("\r")) text = text.substring(0, text.length() - 1);
                    try {
                        if (oversized) throw new IllegalArgumentException("Oversized record");
                        GenerationRecord record = HistoryCodec.decode(text);
                        GenerationRecord duplicate = identities.get(record.candidate());
                        if (duplicate != null || generations.contains(record.generation()))
                            throw new IllegalArgumentException("Duplicate/conflicting generation identity (first valid record retained)");
                        identities.put(record.candidate(), record); generations.add(record.generation()); records.add(record);
                    } catch (RuntimeException invalid) {
                        errors++; if (warnings.size() < 10) warnings.add("History line " + lineNumber + ": " + invalid.getMessage());
                        if (String.valueOf(invalid.getMessage()).startsWith("Unsupported history schema")
                                || text.startsWith("schema=") && !text.startsWith("schema=1\t") && !text.startsWith("schema=2\t")) unsupported = true;
                    }
                    line.reset(); oversized = false; lineNumber++;
                } else {
                    if (line.size() < 16384) line.write(c); else oversized = true;
                }
            }
            unterminated = line.size() != 0 || oversized;
            if (unterminated) warnings.add("History line " + lineNumber + ": unfinished final record ignored; bytes preserved.");
        }
        if (errors > 10) warnings.add((errors - 10) + " additional invalid history lines.");
        records.sort(Comparator.comparingLong(GenerationRecord::generation));
        size = attributes.size(); modified = attributes.lastModifiedTime();
        return cached = new Snapshot(records, warnings);
    }

    /** Success means a complete newline-terminated record has been forced to storage. Idempotent by Candidate. */
    public boolean append(GenerationRecord record) throws IOException {
        Snapshot before = refresh();
        GenerationRecord prior = identities.get(record.candidate());
        if (prior != null) {
            if (!prior.equals(record)) throw new IOException("Conflicting history for Candidate " + record.candidate());
            return false;
        }
        if (unsupported) throw new IOException("Unsupported history schema; refusing to append: " + file);
        if (generations.contains(record.generation())) throw new IOException("Conflicting history generation " + record.generation());
        if (!before.records().isEmpty() && record.generation() <= before.records().getLast().generation())
            throw new IOException("History is ahead of this training lineage; refusing out-of-order append");
        Files.createDirectories(file.getParent());
        // Seal, but never truncate/rewrite, an interrupted tail before the next independent record.
        byte[] bytes = ((unterminated ? "\n" : "") + HistoryCodec.encode(record) + "\n").getBytes(StandardCharsets.UTF_8);
        try (FileChannel channel = FileChannel.open(file, CREATE, WRITE, APPEND)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        } catch (IOException failure) { cached = null; throw failure; }
        var records = new ArrayList<>(before.records()); records.add(record);
        cached = new Snapshot(records, before.warnings()); identities.put(record.candidate(), record); generations.add(record.generation());
        size = Files.size(file); modified = Files.getLastModifiedTime(file); unterminated = false;
        return true;
    }
}
