package com.ohinteractive.seedv6.training.selfplay;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import com.ohinteractive.seedv6.core.Board;

/** Temporary fixed-record storage for a training worker. No position-sized index or memory mapping. */
public final class SampleStore extends AbstractList<TrajectorySampler.Sample> implements RandomAccess {
    private static final int RECORD = 64, BUFFER = RECORD * 1024;
    private static final InheritableThreadLocal<Workspace> CURRENT = new InheritableThreadLocal<>();

    /** The service owns this scope through checkpoint persistence and validation, including failure. */
    public static final class Workspace implements AutoCloseable {
        private final List<Path> files = new ArrayList<>();
        private final List<Closeable> writers = new ArrayList<>();
        private boolean closed;
        public Workspace() {
            if (CURRENT.get() != null) throw new IllegalStateException("Nested training workspace");
            CURRENT.set(this);
        }
        synchronized Path temporary(String suffix) throws IOException {
            Path file = Files.createTempFile("seedv6-training-", suffix); files.add(file); return file;
        }
        synchronized void writer(Closeable writer) { writers.add(writer); }
        /** Only after all references to the completed generation have been released. */
        public void clear() throws IOException {
            IOException failure = null;
            for (Closeable writer : writers) try { writer.close(); } catch (IOException e) { failure = collect(failure, e); }
            writers.clear();
            for (Iterator<Path> it = files.iterator(); it.hasNext();) {
                try { Files.deleteIfExists(it.next()); it.remove(); } catch (IOException e) { failure = collect(failure, e); }
            }
            if (failure != null) throw failure;
        }
        public void close() throws IOException {
            if (closed) return;
            closed = true; CURRENT.remove(); clear();
        }
        private static IOException collect(IOException failure, IOException next) {
            if (failure == null) return next; failure.addSuppressed(next); return failure;
        }
    }

    private final Path file;
    private final long offset;
    private final int recordBytes;
    private DataOutputStream writer;
    private int count;
    private final byte[] cache = new byte[BUFFER];
    private long cacheOffset = -1;
    private int cacheLength;

    private SampleStore(Workspace workspace) throws IOException {
        file = workspace.temporary(".positions"); offset = 0; recordBytes = RECORD;
        writer = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file), BUFFER));
        workspace.writer(writer);
    }
    /** A view over an immutable existing generation record, with no open handle to leak. */
    public SampleStore(Path file, long offset, int count) {
        this.file = file; this.offset = offset; this.count = count; recordBytes = 56;
        if (offset < 0 || count < 0) throw new IllegalArgumentException("Invalid sample range");
    }
    public static List<TrajectorySampler.Sample> mutable() {
        try { return CURRENT.get() == null ? new ArrayList<>() : new SampleStore(CURRENT.get()); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
    public static List<TrajectorySampler.Sample> copyOf(List<TrajectorySampler.Sample> samples) {
        if (samples instanceof SampleStore store) { store.seal(); return store; }
        if (CURRENT.get() == null) return List.copyOf(samples);
        var result = mutable(); result.addAll(samples); return copyOf(result);
    }
    public static void releaseGeneration() throws IOException {
        if (CURRENT.get() != null) CURRENT.get().clear();
    }
    public static boolean streaming() { return CURRENT.get() != null; }
    /** Bounded auxiliary indexes share the generation's deterministic cleanup scope. */
    public static Path temporary(String suffix) throws IOException {
        if (CURRENT.get() == null) throw new IllegalStateException("No training workspace");
        return CURRENT.get().temporary(suffix);
    }
    public static List<TrajectorySampler.Sample> withTargets(List<TrajectorySampler.Sample> samples,
            java.util.function.ToDoubleFunction<TrajectorySampler.Sample> target, SelfPlayControl control) {
        var result = mutable();
        for (var sample : samples) {
            if (control.cancelled()) break;
            result.add(new TrajectorySampler.Sample(sample.board(), sample.target(), target.applyAsDouble(sample)));
        }
        return copyOf(result);
    }
    private void seal() {
        if (writer == null) return;
        try { writer.close(); writer = null; }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
    @Override public boolean add(TrajectorySampler.Sample sample) {
        if (writer == null) throw new UnsupportedOperationException("Sealed training positions");
        if (count == Integer.MAX_VALUE) throw new IllegalStateException("Too many training positions");
        try {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Training spool interrupted");
            for (long value : sample.board()) writer.writeLong(value);
            writer.writeDouble(sample.target()); writer.writeDouble(sample.trainingTarget()); count++; return true;
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
    @Override public int size() { return count; }
    Reader reader() throws IOException { seal(); return new Reader(); }
    final class Reader implements AutoCloseable {
        private final FileChannel input = FileChannel.open(file);
        private final ByteBuffer bytes = ByteBuffer.allocate(recordBytes);
        Reader() throws IOException {}
        TrajectorySampler.Sample get(int index) throws IOException {
            Objects.checkIndex(index, count); bytes.clear();
            long position = offset + (long) index * recordBytes;
            while (bytes.hasRemaining()) if (input.read(bytes, position + bytes.position()) < 0)
                throw new EOFException("Truncated training positions");
            bytes.flip();
            long[] board = new long[Board.MAX_BITBOARDS];
            for (int i = 0; i < board.length; i++) board[i] = bytes.getLong();
            double terminal = bytes.getDouble();
            return new TrajectorySampler.Sample(board, terminal, recordBytes == RECORD ? bytes.getDouble() : terminal);
        }
        public void close() throws IOException { input.close(); }
    }
    @Override public synchronized TrajectorySampler.Sample get(int index) {
        Objects.checkIndex(index, count);
        try {
            if (writer != null) writer.flush();
            long position = offset + (long) index * recordBytes;
            if (position < cacheOffset || position + recordBytes > cacheOffset + cacheLength) {
                cacheOffset = offset + (long) (index / 1024) * 1024 * recordBytes;
                int length = (int) Math.min(cache.length / recordBytes * recordBytes,
                        offset + (long) count * recordBytes - cacheOffset);
                ByteBuffer bytes = ByteBuffer.wrap(cache, 0, length);
                try (var input = FileChannel.open(file)) {
                    while (bytes.hasRemaining()) if (input.read(bytes, cacheOffset + bytes.position()) < 0)
                        throw new EOFException("Truncated training positions");
                }
                cacheLength = length;
            }
            ByteBuffer bytes = ByteBuffer.wrap(cache, (int) (position - cacheOffset), recordBytes);
            long[] board = new long[Board.MAX_BITBOARDS];
            for (int i = 0; i < board.length; i++) board[i] = bytes.getLong();
            double terminal = bytes.getDouble();
            return new TrajectorySampler.Sample(board, terminal, recordBytes == RECORD ? bytes.getDouble() : terminal);
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
}
