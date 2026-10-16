package com.ohinteractive.seedv6.core.brn;

import com.ohinteractive.seedv6.core.Board;
import java.util.Objects;

/** Reusable single-owner workspace. Stores occurrences, including repeated relation indices. */
public final class BrnFeatures {
    private final int[] squares = new int[64];
    private final int[] codes = new int[64];
    private final int[] indices = new int[BrnFeatureSchema.MAX_ACTIVE_FEATURES];
    private int size, nodeCount, relationCount, statusCount;

    /** Reads only P0-P3 and the complete status long; an optional Board.KEY is ignored. */
    public void extract(long[] board) {
        Objects.requireNonNull(board, "board");
        if (board.length <= Board.STATUS) throw new IllegalArgumentException("Five position longs required.");
        long p0 = board[0], p1 = board[1], p2 = board[2], p3 = board[3];
        long occupied = p0 | p1 | p2 | p3;
        size = 0;
        nodeCount = 0;
        indices[size++] = BrnFeatureSchema.BIAS;
        while (occupied != 0) {
            int square = Long.numberOfTrailingZeros(occupied);
            int code = Board.getSquare(p0, p1, p2, p3, square);
            squares[nodeCount] = square;
            codes[nodeCount++] = code;
            indices[size++] = BrnFeatureSchema.NODE_OFFSET + (code - 1) * 64 + square;
            occupied &= occupied - 1;
        }
        for (int a = 0; a < nodeCount; a++) {
            for (int b = a + 1; b < nodeCount; b++) {
                indices[size++] = BrnFeatureSchema.orderedRelationIndex(codes[a], squares[a], codes[b], squares[b]);
            }
        }
        relationCount = nodeCount * (nodeCount - 1) / 2;
        long status = board[Board.STATUS];
        statusCount = Long.bitCount(status);
        while (status != 0) {
            indices[size++] = BrnFeatureSchema.STATUS_OFFSET + Long.numberOfTrailingZeros(status);
            status &= status - 1;
        }
    }

    public int size() { return size; }
    public int nodeCount() { return nodeCount; }
    public int relationCount() { return relationCount; }
    public int statusCount() { return statusCount; }
    public int indexAt(int occurrence) { return indices[Objects.checkIndex(occurrence, size)]; }

    /** Same deterministic ascending-node/pair/status accumulation for inference and training. */
    double value(double[] weights) {
        if (size == 0) throw new IllegalStateException("Extract a position first.");
        double z = 0;
        for (int i = 0; i < size; i++) z += weights[indices[i]];
        if (!Double.isFinite(z)) throw new ArithmeticException("Nonfinite BRN preactivation.");
        return StrictMath.tanh(z);
    }
}
