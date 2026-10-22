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

/** Immutable versioned Bullet export: 10 mirrored buckets, 256x2 SCReLU, one head.
 * No material term, tanh, legacy NNUE mapping or implicit production selection. */
public final class ConventionalNetwork {
    public static final int HIDDEN = 256, QA = 255, QB = 64, SCALE = 400;
    public static final int PARAMETERS = ConventionalFeatures.COUNT * HIDDEN + HIDDEN * 3 + 1;
    public static final int PAYLOAD_BYTES = PARAMETERS * 2;
    public static final int HEADER_BYTES = 8 + 8 * 4 + 32;
    private static final byte[] MAGIC = {'S', '6', 'C', 'N', 'U', '0', '0', '1'};
    private final short[] weights, bias, output;
    private final short outputBias;
    private final String payloadSha256;

    private ConventionalNetwork(byte[] payload, String hash) {
        var data = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        weights = new short[ConventionalFeatures.COUNT * HIDDEN];
        bias = new short[HIDDEN]; output = new short[2 * HIDDEN];
        data.get(weights); data.get(bias); data.get(output); outputBias = data.get();
        payloadSha256 = hash;
    }

    public static ConventionalNetwork load(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) { return read(input); }
    }

    public static ConventionalNetwork read(InputStream input) throws IOException {
        byte[] header = input.readNBytes(HEADER_BYTES);
        if (header.length != HEADER_BYTES) throw new IOException("Truncated conventional NNUE header");
        var h = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        byte[] magic = new byte[8]; h.get(magic);
        int[] expected = {1, ConventionalFeatures.COUNT, HIDDEN, 10, QA, QB, SCALE, PAYLOAD_BYTES};
        if (!Arrays.equals(magic, MAGIC)) throw new IOException("Unsupported conventional NNUE identity");
        for (int value : expected) if (h.getInt() != value) throw new IOException("Unsupported conventional NNUE schema");
        byte[] hash = new byte[32]; h.get(hash);
        byte[] payload = input.readNBytes(PAYLOAD_BYTES);
        if (payload.length != PAYLOAD_BYTES || input.read() != -1) throw new IOException("Invalid conventional NNUE length");
        byte[] actual;
        try { actual = MessageDigest.getInstance("SHA-256").digest(payload); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        if (!MessageDigest.isEqual(hash, actual)) throw new IOException("Conventional NNUE checksum mismatch");
        return new ConventionalNetwork(payload, HexFormat.of().formatHex(actual));
    }

    public String payloadSha256() { return payloadSha256; }
    public ConventionalAccumulator newAccumulator() { return new ConventionalAccumulator(this); }
    void reset(int[] destination) { for (int i = 0; i < HIDDEN; i++) destination[i] = bias[i]; }
    void add(int index, int sign, int[] destination) {
        int offset = index * HIDDEN;
        for (int i = 0; i < HIDDEN; i++) destination[i] += sign * weights[offset + i];
    }

    int score(int[] us, int[] them) {
        long sum = 0;
        for (int i = 0; i < HIDDEN; i++) {
            int a = Math.clamp(us[i], 0, QA), b = Math.clamp(them[i], 0, QA);
            sum += (long) a * a * output[i] + (long) b * b * output[HIDDEN + i];
        }
        return scale(sum);
    }

    private int scale(long sum) {
        // Match Bullet's example integer divisions (truncate toward zero), with long
        // intermediates even for hostile i16 weights. Keep scores outside mate domain.
        long cp = (sum / QA + outputBias) * SCALE / (QA * QB);
        return (int) Math.clamp(cp, -32511L, 32511L);
    }

    /** Independent unit-major full-rebuild oracle; no accumulator or delta kernel.
     * Up to 64 signed-short additions fit int; the complete clipped head fits long. */
    public int fullRebuild(long[] board) {
        ConventionalFeatures.requireBoard(board);
        int stm = Board.player((int) board[Board.STATUS]);
        long sum = 0;
        for (int side = 0; side < 2; side++) {
            int perspective = side == 0 ? stm : stm ^ 1;
            int king = ConventionalFeatures.king(board, perspective);
            for (int i = 0; i < HIDDEN; i++) {
                int raw = bias[i];
                for (int square = 0; square < 64; square++) {
                    int piece = Board.getSquare(board[0], board[1], board[2], board[3], square);
                    if (piece != Value.NONE) raw += weights[ConventionalFeatures.index(perspective, king, piece, square) * HIDDEN + i];
                }
                long a = Math.clamp(raw, 0, QA);
                sum += a * a * output[side * HIDDEN + i];
            }
        }
        return scale(sum);
    }
}
