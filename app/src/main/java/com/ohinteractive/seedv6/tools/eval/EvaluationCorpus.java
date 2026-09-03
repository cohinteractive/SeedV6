package com.ohinteractive.seedv6.tools.eval;

import com.ohinteractive.seedv6.core.Board;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Immutable OPT5-M1 sample of actual evaluation entries from the accepted
 * Threads=1 cold depth-6 production search at baseline
 * {@value #BASELINE_COMMIT}.
 *
 * <p>Every 256th call in each root/call-site stream is a weighted sample.
 * Eight leading support samples per non-empty stream retain small roots and
 * rare strata, but are excluded from the production-weighted benchmark.</p>
 */
public final class EvaluationCorpus {

    public static final String BASELINE_COMMIT =
            "9981efef51dd5d7be3ea82d97a49add2653d4caf";
    public static final String RAW_SHA256 =
            "dad39174f075e77941e6e4e5d579e0c46a294b6652e22d15d614b9bd0a07f697";
    public static final int FORMAT_VERSION = 1;
    public static final int SAMPLE_STRIDE = 256;
    public static final int SUPPORT_SAMPLES_PER_STREAM = 8;

    private static final String RESOURCE =
            "/com/ohinteractive/seedv6/tools/eval/opt5-m1-eval-corpus-v1.b64";
    private static final CorpusData DATA = load();

    public static Metadata metadata() {
        return DATA.metadata;
    }

    public static List<Entry> entries() {
        return DATA.entries;
    }

    public static List<Entry> weightedEntries() {
        return DATA.weightedEntries;
    }

    public static String rawSha256() {
        return DATA.rawSha256;
    }

    private static CorpusData load() {
        try (InputStream resource = EvaluationCorpus.class.getResourceAsStream(RESOURCE)) {
            if (resource == null) {
                throw new IllegalStateException("Missing evaluation corpus resource: " + RESOURCE);
            }
            String encoded = new String(resource.readAllBytes(), StandardCharsets.US_ASCII);
            StringBuilder compact = new StringBuilder(encoded.length());
            for (String line : encoded.split("\\R")) {
                String value = line.trim();
                if (!value.isEmpty() && !value.startsWith("#")) {
                    compact.append(value);
                }
            }
            byte[] compressed = Base64.getDecoder().decode(compact.toString());
            byte[] raw;
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
                raw = gzip.readAllBytes();
            }
            String actualSha256 = sha256(raw);
            if (!RAW_SHA256.equals(actualSha256)) {
                throw new IllegalStateException("Evaluation corpus checksum mismatch: " + actualSha256);
            }
            return parse(raw, actualSha256);
        } catch (IOException | IllegalArgumentException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static CorpusData parse(byte[] raw, String rawSha256) throws IOException {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(raw))) {
            require(input.readInt() == 0x45564331, "Invalid evaluation corpus magic");
            require(input.readInt() == FORMAT_VERSION, "Unexpected evaluation corpus version");
            require(BASELINE_COMMIT.equals(input.readUTF()), "Unexpected evaluation corpus baseline");
            require(input.readInt() == SAMPLE_STRIDE, "Unexpected evaluation sample stride");
            require(input.readInt() == SUPPORT_SAMPLES_PER_STREAM,
                    "Unexpected evaluation support-sample count");

            int rootCount = input.readInt();
            require(rootCount == 12, "Unexpected evaluation root count");
            List<String> rootNames = new ArrayList<>(rootCount);
            for (int root = 0; root < rootCount; root++) {
                rootNames.add(input.readUTF());
            }
            long[][] callCounts = new long[rootCount][Source.values().length];
            for (int root = 0; root < rootCount; root++) {
                callCounts[root][Source.MAIN_SEARCH.ordinal()] = input.readLong();
                callCounts[root][Source.QSEARCH.ordinal()] = input.readLong();
            }
            long[] phaseHistogram = readLongs(input, 25, "phase histogram");
            long[] whitePawnHistogram = readLongs(input, 9, "white-pawn histogram");
            long[] blackPawnHistogram = readLongs(input, 9, "black-pawn histogram");
            long endgameCalls = input.readLong();
            long promotedMaterialCalls = input.readLong();

            int entryCount = input.readInt();
            require(entryCount > 0 && entryCount <= 10_000, "Invalid evaluation entry count");
            List<Entry> entries = new ArrayList<>(entryCount);
            List<Entry> weightedEntries = new ArrayList<>(entryCount);
            for (int index = 0; index < entryCount; index++) {
                int root = input.readUnsignedByte();
                int source = input.readUnsignedByte();
                int kind = input.readUnsignedByte();
                require(root < rootCount, "Invalid evaluation root index");
                require(source < Source.values().length, "Invalid evaluation source");
                require(kind < SampleKind.values().length, "Invalid evaluation sample kind");
                Entry entry = new Entry(root, Source.values()[source], SampleKind.values()[kind],
                        input.readLong(), input.readLong(), input.readLong(), input.readLong(),
                        input.readLong(), input.readLong(), input.readLong());
                entries.add(entry);
                if (entry.kind == SampleKind.WEIGHTED) {
                    weightedEntries.add(entry);
                }
            }
            require(input.read() == -1, "Trailing evaluation corpus data");

            long totalCalls = sum(callCounts);
            require(sum(phaseHistogram) == totalCalls, "Phase histogram total mismatch");
            require(sum(whitePawnHistogram) == totalCalls, "White-pawn histogram total mismatch");
            require(sum(blackPawnHistogram) == totalCalls, "Black-pawn histogram total mismatch");
            Metadata metadata = new Metadata(rootNames, callCounts, phaseHistogram,
                    whitePawnHistogram, blackPawnHistogram, endgameCalls,
                    promotedMaterialCalls, entries.size(), weightedEntries.size());
            return new CorpusData(metadata, List.copyOf(entries),
                    List.copyOf(weightedEntries), rawSha256);
        }
    }

    private static long[] readLongs(DataInputStream input, int expectedLength,
                                    String name) throws IOException {
        int length = input.readInt();
        require(length == expectedLength, "Unexpected " + name + " length");
        long[] values = new long[length];
        for (int index = 0; index < length; index++) {
            values[index] = input.readLong();
        }
        return values;
    }

    private static long sum(long[][] values) {
        long sum = 0L;
        for (long[] row : values) {
            sum += sum(row);
        }
        return sum;
    }

    private static long sum(long[] values) {
        long sum = 0L;
        for (long value : values) {
            sum += value;
        }
        return sum;
    }

    private static String sha256(byte[] raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void require(boolean condition, String message) throws IOException {
        if (!condition) {
            throw new IOException(message);
        }
    }

    public enum Source { MAIN_SEARCH, QSEARCH }

    public enum SampleKind { SUPPORT, WEIGHTED }

    public record Entry(int rootIndex, Source source, SampleKind kind, long ordinal,
                        long board0, long board1, long board2, long board3,
                        long status, long key) {

        public long[] board() {
            return new long[] {board0, board1, board2, board3, status, key};
        }

        public int phase() {
            long queens = ~board0 & board1 & ~board2;
            long rooks = board0 & board1 & ~board2;
            long bishops = ~board0 & ~board1 & board2;
            long knights = board0 & ~board1 & board2;
            int remaining = 4 * Long.bitCount(queens) + 2 * Long.bitCount(rooks)
                    + Long.bitCount(bishops) + Long.bitCount(knights);
            return Math.max(0, Math.min(24, 24 - remaining));
        }

        public int whitePawnCount() {
            return Long.bitCount(pawns() & ~board3);
        }

        public int blackPawnCount() {
            return Long.bitCount(pawns() & board3);
        }

        public int sliderCount() {
            long queens = ~board0 & board1 & ~board2;
            long rooks = board0 & board1 & ~board2;
            long bishops = ~board0 & ~board1 & board2;
            return Long.bitCount(queens | rooks | bishops);
        }

        public boolean promotedMaterial() {
            long queens = ~board0 & board1 & ~board2;
            long rooks = board0 & board1 & ~board2;
            long bishops = ~board0 & ~board1 & board2;
            long knights = board0 & ~board1 & board2;
            return Long.bitCount(queens & ~board3) > 1
                    || Long.bitCount(queens & board3) > 1
                    || Long.bitCount(rooks & ~board3) > 2
                    || Long.bitCount(rooks & board3) > 2
                    || Long.bitCount(bishops & ~board3) > 2
                    || Long.bitCount(bishops & board3) > 2
                    || Long.bitCount(knights & ~board3) > 2
                    || Long.bitCount(knights & board3) > 2;
        }

        public boolean endgame() {
            return phase() >= 17;
        }

        private long pawns() {
            return ~board0 & board1 & board2;
        }
    }

    public static final class Metadata {
        private final List<String> rootNames;
        private final long[][] callCounts;
        private final long[] phaseHistogram;
        private final long[] whitePawnHistogram;
        private final long[] blackPawnHistogram;
        private final long endgameCalls;
        private final long promotedMaterialCalls;
        private final int sampleCount;
        private final int weightedSampleCount;

        private Metadata(List<String> rootNames, long[][] callCounts, long[] phaseHistogram,
                         long[] whitePawnHistogram, long[] blackPawnHistogram,
                         long endgameCalls, long promotedMaterialCalls,
                         int sampleCount, int weightedSampleCount) {
            this.rootNames = List.copyOf(rootNames);
            this.callCounts = copy(callCounts);
            this.phaseHistogram = phaseHistogram.clone();
            this.whitePawnHistogram = whitePawnHistogram.clone();
            this.blackPawnHistogram = blackPawnHistogram.clone();
            this.endgameCalls = endgameCalls;
            this.promotedMaterialCalls = promotedMaterialCalls;
            this.sampleCount = sampleCount;
            this.weightedSampleCount = weightedSampleCount;
        }

        public List<String> rootNames() {
            return rootNames;
        }

        public long[][] callCounts() {
            return copy(callCounts);
        }

        public long[] phaseHistogram() {
            return phaseHistogram.clone();
        }

        public long[] whitePawnHistogram() {
            return whitePawnHistogram.clone();
        }

        public long[] blackPawnHistogram() {
            return blackPawnHistogram.clone();
        }

        public long endgameCalls() {
            return endgameCalls;
        }

        public long promotedMaterialCalls() {
            return promotedMaterialCalls;
        }

        public int sampleCount() {
            return sampleCount;
        }

        public int weightedSampleCount() {
            return weightedSampleCount;
        }

        public long totalCalls() {
            return sum(callCounts);
        }

        public long calls(Source source) {
            long total = 0L;
            for (long[] root : callCounts) {
                total += root[source.ordinal()];
            }
            return total;
        }

        private static long[][] copy(long[][] source) {
            return Arrays.stream(source).map(long[]::clone).toArray(long[][]::new);
        }
    }

    private record CorpusData(Metadata metadata, List<Entry> entries,
                              List<Entry> weightedEntries, String rawSha256) { }

    private EvaluationCorpus() { }
}
