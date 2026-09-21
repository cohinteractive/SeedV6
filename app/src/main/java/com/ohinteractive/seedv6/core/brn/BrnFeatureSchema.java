package com.ohinteractive.seedv6.core.brn;

import com.ohinteractive.seedv6.core.Board;

/** BRN-0 parameter vocabulary; changing any index meaning requires a new schema version. */
public final class BrnFeatureSchema {
    public static final int VERSION = 1;
    public static final int CODE_COUNT = 15;
    public static final int SQUARE_COUNT = 64;
    public static final int DISPLACEMENT_COUNT = 112;
    public static final int BIAS = 0;
    public static final int NODE_OFFSET = 1;
    public static final int NODE_COUNT = CODE_COUNT * SQUARE_COUNT;
    public static final int RELATION_OFFSET = NODE_OFFSET + NODE_COUNT;
    public static final int RELATION_COUNT = CODE_COUNT * CODE_COUNT * DISPLACEMENT_COUNT;
    public static final int STATUS_OFFSET = RELATION_OFFSET + RELATION_COUNT;
    public static final int PARAMETER_COUNT = STATUS_OFFSET + Long.SIZE;
    public static final int MAX_ACTIVE_FEATURES = 1 + 64 + 64 * 63 / 2 + 64;

    private BrnFeatureSchema() {}

    public static int squareCode(long p0, long p1, long p2, long p3, int square) {
        requireSquare(square);
        return Board.getSquare(p0, p1, p2, p3, square);
    }

    public static int nodeIndex(int code, int square) {
        requireCode(code);
        requireSquare(square);
        return NODE_OFFSET + (code - 1) * 64 + square;
    }

    /** Endpoint codes follow the orientation: dy > 0, or dy == 0 and dx > 0. */
    public static int relationIndex(int codeA, int squareA, int codeB, int squareB) {
        requireCode(codeA);
        requireCode(codeB);
        requireSquare(squareA);
        requireSquare(squareB);
        if (squareA == squareB) throw new IllegalArgumentException("Relation endpoints must differ.");
        // With a1=0 and rank-major squares, ascending square order is exactly canonical.
        return squareA < squareB ? orderedRelationIndex(codeA, squareA, codeB, squareB)
                : orderedRelationIndex(codeB, squareB, codeA, squareA);
    }

    static int orderedRelationIndex(int codeA, int squareA, int codeB, int squareB) {
        int dx = (squareB & 7) - (squareA & 7);
        int dy = (squareB >>> 3) - (squareA >>> 3);
        int displacement = dy == 0 ? dx - 1 : 7 + (dy - 1) * 15 + dx + 7;
        return RELATION_OFFSET + ((codeA - 1) * 15 + codeB - 1) * DISPLACEMENT_COUNT + displacement;
    }

    public static int statusIndex(int bit) {
        if (bit < 0 || bit >= Long.SIZE) throw new IllegalArgumentException("Status bit must be in [0,63].");
        return STATUS_OFFSET + bit;
    }

    private static void requireCode(int code) {
        if (code < 1 || code > 15) throw new IllegalArgumentException("Node code must be in [1,15].");
    }

    private static void requireSquare(int square) {
        if (square < 0 || square >= 64) throw new IllegalArgumentException("Square must be in [0,63].");
    }
}
