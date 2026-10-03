package com.ohinteractive.seedv6.training.data;

import com.github.luben.zstd.ZstdInputStream;
import com.ohinteractive.seedv6.corpus.lichess.LichessDecoder;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Lazy source navigation. Index contains only visited seek points, never positions or features. */
public final class SourceReaders {
    public record Point(long record, long offset, long within) {}
    public record Index(String identity, List<Point> points, long count, String checksum) {
        public Index {
            if (identity == null || points == null || points.isEmpty() || count < -1
                    || !checksum.equals(DataFiles.hash(identity + points + count))) throw new IllegalArgumentException("Invalid source seek index");
            long previous = -1;
            for (Point point : points) {
                if (point.record() <= previous || point.offset() < 0) throw new IllegalArgumentException("Invalid seek order");
                previous = point.record();
            }
            points = List.copyOf(points);
        }
        Index(String identity, List<Point> points, long count) { this(identity, points, count, DataFiles.hash(identity + points + count)); }
    }
    public static PositionReader open(DataSource source, Path indexes, long start) throws IOException {
        source.verify();
        if (start < 0) throw new IllegalArgumentException("Negative source position");
        return source.format() == DataSource.Format.SEED_LEGACY
                ? new com.ohinteractive.seedv6.corpus.LegacyPositionReader(source, indexes, start)
                : new Lines(source, indexes, start);
    }
    public static final class Navigation {
        private final DataSource source;
        private final Path file;
        private final TreeMap<Long, Point> points = new TreeMap<>();
        private long count = -1;
        public Navigation(DataSource source, Path directory, Point first) throws IOException {
            this.source = source; file = directory.resolve(source.identity() + ".json");
            points.put(0L, first);
            if (Files.exists(file)) {
                Index index = DataFiles.read(file, Index.class);
                if (!source.identity().equals(index.identity())) throw new IOException("Seek index source identity mismatch");
                index.points().forEach(p -> points.put(p.record(), p)); count = index.count();
            }
        }
        public Point floor(long record) throws IOException {
            Point point = points.floorEntry(record).getValue();
            if (record - point.record() > 4096) throw new IOException("Training Data seek metadata does not cover position " + record
                    + ". Restore this lineage's seek metadata; an unbounded prefix scan was not started.");
            return point;
        }
        public void add(Point point) throws IOException {
            var previous = points.putIfAbsent(point.record(), point);
            if (previous != null && !previous.equals(point)) throw new IOException("Training Data seek point changed");
        }
        public void eof(long record) throws IOException {
            if (count >= 0 && count != record) throw new IOException("Training Data position count changed");
            count = record;
        }
        public void save() throws IOException {
            source.verify();
            DataFiles.write(file, new Index(source.identity(), new ArrayList<>(points.values()), count));
        }
    }
    public static void requirePzstd(Path path) throws IOException {
        try (var file = new RandomAccessFile(path.toFile(), "r")) { frameLength(file, 0); }
    }
    private static long frameLength(RandomAccessFile file, long offset) throws IOException {
        file.seek(offset);
        byte[] header = new byte[12]; file.readFully(header);
        var bytes = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        if (bytes.getInt() != 0x184d2a50 || bytes.getInt() != 4)
            throw new IOException("This Zstandard source has no PZstandard frame-length headers. Efficient durable resume is unavailable; use a framed source. No conversion was started.");
        long size = Integer.toUnsignedLong(bytes.getInt());
        if (size < 6 || offset + 12 + size > file.length()) throw new IOException("Truncated Training Data frame");
        if (Integer.reverseBytes(file.readInt()) != 0xfd2fb528) throw new IOException("Missing Zstandard frame");
        return size;
    }
    private static final class Frames extends InputStream {
        private final RandomAccessFile file;
        private ZstdInputStream decoder;
        private long frame, nextFrame, decoded;
        Frames(Path path, long frame) throws IOException { file = new RandomAccessFile(path.toFile(), "r"); this.frame = frame; openFrame(); }
        private void openFrame() throws IOException {
            if (frame == file.length()) { decoder = null; return; }
            long length = frameLength(file, frame); nextFrame = frame + 12 + length; decoded = 0;
            file.seek(frame + 12);
            decoder = new ZstdInputStream(new InputStream() {
                private long remaining = length;
                public int read() throws IOException { if (remaining == 0) return -1; remaining--; return file.read(); }
                public int read(byte[] b, int off, int len) throws IOException {
                    if (remaining == 0) return -1;
                    int n = file.read(b, off, (int) Math.min(len, remaining));
                    if (n < 0) throw new EOFException("Truncated source frame"); remaining -= n; return n;
                }
            });
        }
        public int read() throws IOException { byte[] one = new byte[1]; return read(one, 0, 1) < 0 ? -1 : one[0] & 255; }
        public int read(byte[] bytes, int offset, int length) throws IOException {
            while (decoder != null) {
                int n = decoder.read(bytes, offset, length);
                if (n >= 0) { decoded += n; return n; }
                decoder.close(); frame = nextFrame; openFrame();
            }
            return -1;
        }
        public void close() throws IOException { try { if (decoder != null) decoder.close(); } finally { file.close(); } }
    }
    private static final class Lines implements PositionReader {
        private final DataSource source;
        private final Navigation navigation;
        private final InputStream input;
        private final Frames frames;
        private final byte[] buffer = new byte[65536];
        private int cursor, end;
        private long position, decoded, seekRecords, plainRead, lastIndexedFrame = -1;
        private boolean closed;
        Lines(DataSource source, Path indexes, long start) throws IOException {
            this.source = source;
            navigation = new Navigation(source, indexes, new Point(0, 0, 0));
            Point point = navigation.floor(start); position = point.record();
            if (source.format() == DataSource.Format.LICHESS_PZSTD) {
                frames = new Frames(source.path(), point.offset()); input = frames;
                long remaining = point.within();
                try {
                    while (remaining > 0) {
                        int n = input.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                        if (n < 0) throw new EOFException("Missing indexed position"); remaining -= n;
                    }
                } catch (Throwable failure) { input.close(); throw failure; }
            } else {
                frames = null;
                FileChannel channel = FileChannel.open(source.path()); channel.position(point.offset()); plainRead = point.offset();
                input = java.nio.channels.Channels.newInputStream(channel);
            }
            try {
                // Skip bytes/lines only within the visited frame or sparse plain-text interval. Never parse FEN/JSON here.
                while (position < start) { if (line(false) == null) throw new EOFException("Training Data exhausted before position " + start); position++; seekRecords++; }
            } catch (Throwable failure) { input.close(); throw failure; }
        }
        private boolean fill() throws IOException {
            if (cursor < end) return true;
            do { end = input.read(buffer); } while (end == 0);
            cursor = 0; if (end < 0) return false;
            plainRead += end; return true;
        }
        private Point point() { return new Point(position, frames == null ? plainRead - (end - cursor) : frames.frame,
                frames == null ? 0 : frames.decoded - (end - cursor)); }
        private String line(boolean retain) throws IOException {
            if (!fill()) return null;
            var start = point();
            if (position % 4096 == 0 || frames != null && start.offset() != lastIndexedFrame) {
                navigation.add(start); lastIndexedFrame = start.offset();
            }
            ByteArrayOutputStream bytes = retain ? new ByteArrayOutputStream(1024) : null;
            int size = 0;
            do {
                byte value = buffer[cursor++];
                if (value == '\n') break;
                if (++size > 1048576) throw new IOException("Training Data line exceeds 1 MiB at position " + position);
                if (retain) bytes.write(value);
            } while (fill());
            return retain ? bytes.toString(StandardCharsets.UTF_8) : "";
        }
        public Entry next() throws IOException {
            String line = line(true);
            if (line == null) { navigation.eof(position); return null; }
            long ordinal = position++; decoded++;
            try { return new Entry(ordinal, LichessDecoder.decode(line, 1, ordinal + 1), ""); }
            catch (IllegalArgumentException invalid) { return new Entry(ordinal, null, invalid.getMessage()); }
        }
        public long nextPosition() { return position; }
        public long decodedRecords() { return decoded; }
        public long seekRecords() { return seekRecords; }
        public void close() throws IOException {
            if (closed) return; closed = true;
            try { navigation.save(); } finally { input.close(); }
        }
    }
    private SourceReaders() {}
}
