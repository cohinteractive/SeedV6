package com.ohinteractive.seedv6.core.nnue.conventional;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Value;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

/** Immutable versioned Bullet export: Research-only 256/512x2 SCReLU; production loader stays unchanged.
 * No material term, tanh, legacy NNUE mapping or implicit production selection. */
public final class StrengthNetwork {
    public static final int QA = 255, QB = 64, SCALE = 400;
    private final int hidden, version;
    /** Legacy S6CNU001 geometry; S6CNU002 has eight output heads in the same header envelope. */
    public static final int HEADER_BYTES = 8 + 8 * 4 + 32;
    private static final byte[] MAGIC = {'S', '6', 'C', 'N', 'U', '0', '0', '1'};
    private final short[] weights, bias, outputBias;
    private final int[] output;
    private final Head vectorHead;
    private final int outputBuckets;
    private final String payloadSha256;

    private StrengthNetwork(byte[] payload, String hash, int outputBuckets, int hidden, int version) {
        this.outputBuckets = outputBuckets; this.hidden = hidden; this.version = version;
        var data = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        weights = new short[ConventionalFeatures.COUNT * hidden];
        bias = new short[hidden]; output = new int[2 * hidden * outputBuckets];
        outputBias = new short[outputBuckets];
        data.get(weights); data.get(bias);
        for (int i = 0; i < output.length; i++) output[i] = data.get();
        data.get(outputBias);
        payloadSha256 = hash;
        // Lazy linkage keeps the ordinary headless runtime independent of the optional module.
        vectorHead = !Boolean.getBoolean("seed.nnue.scalar")
                && ModuleLayer.boot().findModule("jdk.incubator.vector").isPresent()
                && StrengthVectorHead.supported(hidden) ? new StrengthVectorHead() : null;
    }

    public static StrengthNetwork load(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) { return read(input); }
    }

    public static StrengthNetwork read(InputStream input) throws IOException {
        byte[] header = input.readNBytes(HEADER_BYTES);
        if (header.length != HEADER_BYTES) throw new IOException("Truncated conventional NNUE header");
        var h = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        byte[] magic = new byte[8]; h.get(magic);
        int version = magic[7] - '0';
        if (version < 1 || version > 3) throw new IOException("Unsupported research geometry");
        int hidden = version == 3 ? 512 : 256;
        byte[] expectedMagic = MAGIC.clone(); expectedMagic[7] = (byte) ('0' + version);
        if (!Arrays.equals(magic, expectedMagic)) throw new IOException("Unsupported conventional NNUE identity");
        int heads = version == 1 ? 1 : 8;
        int payloadBytes = (ConventionalFeatures.COUNT * hidden + hidden + heads * (2 * hidden + 1)) * 2;
        int[] expected = {version, ConventionalFeatures.COUNT, hidden, 10, QA, QB, SCALE, payloadBytes};
        for (int value : expected) if (h.getInt() != value) throw new IOException("Unsupported conventional NNUE schema");
        byte[] hash = new byte[32]; h.get(hash);
        byte[] payload = input.readNBytes(payloadBytes);
        if (payload.length != payloadBytes || input.read() != -1) throw new IOException("Invalid conventional NNUE length");
        byte[] actual;
        try { actual = MessageDigest.getInstance("SHA-256").digest(payload); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        if (!MessageDigest.isEqual(hash, actual)) throw new IOException("Conventional NNUE checksum mismatch");
        return new StrengthNetwork(payload, HexFormat.of().formatHex(actual), heads, hidden, version);
    }

    public String payloadSha256() { return payloadSha256; }
    public int outputBuckets() { return outputBuckets; }
    public String schema() { return "S6CNU00" + version; }
    public int hidden() { return hidden; }
    public String headBackend() { return vectorHead == null ? "scalar-long" : vectorHead.name(); }
    interface Head {
        long dot(int[] us, int[] them, int[] weights, int offset);
        String name();
    }
    int bucket(long[] board) {
        if (outputBuckets == 1) return 0;
        int pieces = Long.bitCount(board[0] | board[1] | board[2]);
        if (pieces < 2 || pieces > 32) throw new IllegalArgumentException("Material head requires 2..32 pieces");
        return (pieces - 2) / 4;
    }
    public com.ohinteractive.seedv6.search.evaluation.SearchEvaluation evaluation() {
        return com.ohinteractive.seedv6.search.evaluation.SearchEvaluation.researchCentipawnsStateful(
                capacity -> new Lifecycle(this, capacity));
    }
    private static final class Lifecycle implements com.ohinteractive.seedv6.search.evaluation.SearchEvaluation.ResearchCpLifecycle {
        private final StrengthAccumulator[] stack;
        private final StrengthNetwork model;
        Lifecycle(StrengthNetwork model, int capacity) {
            this.model=model;stack=new StrengthAccumulator[capacity];
            for(int i=0;i<capacity;i++)stack[i]=model.newAccumulator();
        }
        public void initialize(long[] board,int ply){stack[ply].rebuild(board);}
        public void child(long[] parent,long[] child,int ply){stack[ply+1].update(parent,child,stack[ply]);}
        public void initializeFrom(long[] board,int ply,com.ohinteractive.seedv6.search.evaluation.SearchEvaluation.ResearchCpLifecycle source){
            if(source==this || !(source instanceof Lifecycle other) || other.model!=model)
                throw new IllegalArgumentException("Research model/owner mismatch");
            stack[ply].copyFrom(board,other.stack[ply]);
        }
        public double evaluate(long[] board,int ply){return stack[ply].evaluate(board);}
    }
    public StrengthAccumulator newAccumulator() { return new StrengthAccumulator(this); }
    void reset(int[] destination) { for (int i = 0; i < hidden; i++) destination[i] = bias[i]; }
    void add(int index, int sign, int[] destination) {
        int offset = index * hidden;
        for (int i = 0; i < hidden; i++) destination[i] += sign * weights[offset + i];
    }

    int score(int[] us, int[] them) {
        if (outputBuckets != 1) throw new IllegalStateException("Material bucket required");
        return score(us, them, 0);
    }
    int score(int[] us, int[] them, int bucket) {
        int offset = java.util.Objects.checkIndex(bucket, outputBuckets) * 2 * hidden;
        if (vectorHead != null) return scale(vectorHead.dot(us, them, output, offset), bucket);
        long sum = 0;
        for (int i = 0; i < hidden; i++) {
            int a = Math.clamp(us[i], 0, QA), b = Math.clamp(them[i], 0, QA);
            sum += (long) a * a * output[offset + i] + (long) b * b * output[offset + hidden + i];
        }
        return scale(sum, bucket);
    }

    private int scale(long sum, int bucket) {
        // Match Bullet's example integer divisions (truncate toward zero), with long
        // intermediates even for hostile i16 weights. Keep scores outside mate domain.
        long cp = (sum / QA + outputBias[bucket]) * SCALE / (QA * QB);
        return (int) Math.clamp(cp, -32511L, 32511L);
    }

    /** Independent unit-major full-rebuild oracle; no accumulator or delta kernel.
     * Up to 64 signed-short additions fit int; the complete clipped head fits long. */
    public int fullRebuild(long[] board) {
        ConventionalFeatures.requireBoard(board);
        int stm = Board.player((int) board[Board.STATUS]);
        int bucket = bucket(board), offset = bucket * 2 * hidden;
        long sum = 0;
        for (int side = 0; side < 2; side++) {
            int perspective = side == 0 ? stm : stm ^ 1;
            int king = ConventionalFeatures.king(board, perspective);
            for (int i = 0; i < hidden; i++) {
                int raw = bias[i];
                for (int square = 0; square < 64; square++) {
                    int piece = Board.getSquare(board[0], board[1], board[2], board[3], square);
                    if (piece != Value.NONE) raw += weights[ConventionalFeatures.index(perspective, king, piece, square) * hidden + i];
                }
                long a = Math.clamp(raw, 0, QA);
                sum += a * a * output[offset + side * hidden + i];
            }
        }
        return scale(sum, bucket);
    }
}
