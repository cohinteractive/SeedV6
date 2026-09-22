package com.ohinteractive.seedv6.core.brn2;

import java.util.Objects;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.BrnFeatureSchema;

/** BRN-2 schema 2: player-relative identities and geometry; BRN-0/1 retain schema 1. */
public final class Brn2Features {
    public static final int VERSION = 2;
    public static final String LEGACY_MESSAGE = "BRN-2 schema 1 uses incompatible absolute-color weights. "
            + "Choose a separate empty store to start a fresh canonical BRN-2 lineage (schema 2); "
            + "keep the old store unchanged. Weight migration is not supported.";
    private final int[] squares = new int[64], codes = new int[64];
    private final int[] indices = new int[BrnFeatureSchema.MAX_ACTIVE_FEATURES];
    private int size, nodeCount, relationCount, statusCount;

    /** Same rank reflection as chess-equivalent color reversal; files stay fixed. */
    static int square(int physical, int perspective) { return physical ^ (perspective * 56); }
    static long orient(long bits, int perspective) { return perspective == 0 ? bits : Long.reverseBytes(bits); }

    /** Low three bits retain piece type; bit 3 is THEM (0 is US), never absolute color. */
    static int code(long[] board, int canonicalSquare, int perspective) {
        return Board.getSquare(board[0], board[1], board[2], board[3], square(canonicalSquare, perspective))
                ^ (perspective << Board.PLAYER_SHIFT);
    }
    static long occupied(long[] board) { return board[0] | board[1] | board[2] | board[3]; }

    /** Only castling (us K/Q, them K/Q), oriented EP and halfmove clock are learned.
     * STM is redundant. Fullmove numbering advances after physical Black, and reserved
     * bits have no rule meaning; neither is part of the effective representation.
     */
    static long status(long raw, int perspective) {
        int rights = (int) (raw >>> Board.CASTLING_SHIFT) & Board.CASTLING_BITS;
        if (perspective != 0) rights = ((rights & 3) << 2) | (rights >>> 2);
        int ep = (int) (raw >>> Board.ESQUARE_SHIFT) & Board.SQUARE_BITS;
        if (ep != 0) ep = square(ep, perspective);
        return ((long) rights << Board.CASTLING_SHIFT) | ((long) ep << Board.ESQUARE_SHIFT)
                | (raw & ((long) Board.HALF_MOVE_CLOCK_BITS << Board.HALF_MOVE_CLOCK_SHIFT));
    }

    /** Feature-layer transform only: does not construct, copy or mutate a Board. */
    public void extract(long[] board) {
        if (Objects.requireNonNull(board).length <= Board.STATUS)
            throw new IllegalArgumentException("Five position longs required.");
        int perspective = Board.player((int) board[Board.STATUS]);
        size = 0; nodeCount = 0;
        indices[size++] = BrnFeatureSchema.BIAS;
        for (long remaining = orient(occupied(board), perspective); remaining != 0; remaining &= remaining - 1) {
            int square = Long.numberOfTrailingZeros(remaining), code = code(board, square, perspective);
            squares[nodeCount] = square; codes[nodeCount++] = code;
            indices[size++] = BrnFeatureSchema.nodeIndex(code, square);
        }
        for (int a = 0; a < nodeCount; a++) for (int b = a + 1; b < nodeCount; b++)
            indices[size++] = BrnFeatureSchema.relationIndex(codes[a], squares[a], codes[b], squares[b]);
        relationCount = nodeCount * (nodeCount - 1) / 2;
        long status = status(board[Board.STATUS], perspective);
        statusCount = Long.bitCount(status);
        for (; status != 0; status &= status - 1)
            indices[size++] = BrnFeatureSchema.statusIndex(Long.numberOfTrailingZeros(status));
    }

    public int size() { return size; }
    public int nodeCount() { return nodeCount; }
    public int relationCount() { return relationCount; }
    public int statusCount() { return statusCount; }
    public int indexAt(int occurrence) { return indices[Objects.checkIndex(occurrence, size)]; }
}
