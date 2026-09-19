package com.ohinteractive.seedv6.core.nnue;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;

import java.util.Objects;

/**
 * Worker-owned V1 transformer sums BEFORE clip01: one owned float[64] per perspective.
 * Allocate once per future search ply and reuse. No operation exposes the mutable arrays,
 * allocates on success, or writes to a supplied board. Not thread-safe.
 *
 * Each state is bound to one immutable network by identity. Four placement longs detect
 * stale/mismatched boards without retaining a mutable board reference; status/key are not
 * features. Rejected arguments leave the destination's previous state intact.
 */
public final class NnueAccumulator {

    private final NnueNetwork network;
    private final float[] white = new float[NnueNetwork.ACCUMULATOR_SIZE];
    private final float[] black = new float[NnueNetwork.ACCUMULATOR_SIZE];
    private long placement0, placement1, placement2, placementColour;
    private boolean prepared;

    public NnueAccumulator(NnueNetwork network) {
        this.network = Objects.requireNonNull(network, "network");
    }

    /**
     * Full-recomputation oracle: bias plus all occupied-square rows in ascending square
     * order, independently of any previous accumulator. Requires at least
     * Board.MAX_BITBOARDS longs, valid piece encodings and exactly one king per colour.
     * Does not validate chess legality.
     */
    public void rebuild(long[] board) {
        requireBoard(board);
        rebuildPerspective(board, Value.WHITE, kingSquare(board, Value.WHITE), white);
        rebuildPerspective(board, Value.BLACK, kingSquare(board, Value.BLACK), black);
        rememberPlacement(board);
    }

    /**
     * Derives this destination from a prepared parent and authoritative board placements.
     * Parent and destination MUST be distinct (aliasing is rejected); their arrays cannot
     * otherwise alias. The parent must belong to this same network and parent placement.
     * Siblings/re-searches may repeatedly reuse an unchanged parent.
     *
     * Only a moved perspective king triggers that perspective's full rebuild. Otherwise
     * copy 64 raw floats and subtract/add rows on changed squares. A placement-preserving
     * transition copies all 128 floats exactly, including null moves. No move encoding or
     * chess special-move rules are needed, and no heap allocation occurs on success.
     */
    public void update(long[] parentBoard, long[] childBoard, NnueAccumulator parent) {
        Objects.requireNonNull(parent, "parent");
        if (parent == this) {
            throw new IllegalArgumentException("Parent and child accumulators must be distinct.");
        }
        parent.requireCompatible(parentBoard, network);
        requireBoard(childBoard);
        // Board[0..2] encode type, Board[3] encodes colour. Exclude STATUS=4 and KEY=5.
        long changed = (parentBoard[0] ^ childBoard[0]) | (parentBoard[1] ^ childBoard[1])
                | (parentBoard[2] ^ childBoard[2]) | (parentBoard[3] ^ childBoard[3]);
        updatePerspective(parentBoard, childBoard, changed, Value.WHITE, parent.white, white);
        updatePerspective(parentBoard, childBoard, changed, Value.BLACK, parent.black, black);
        rememberPlacement(childBoard);
    }

    /** Scalar inspection of raw, unclipped sums. Unprepared states have no usable values. */
    public float raw(int perspective, int unit) {
        requirePrepared();
        NnueFeatureSchema.requireColour(perspective);
        return (perspective == Value.WHITE ? white : black)[unit];
    }

    private void updatePerspective(long[] parentBoard, long[] childBoard, long changed,
                                   int perspective, float[] source, float[] destination) {
        int king = kingSquare(childBoard, perspective);
        if (king != kingSquare(parentBoard, perspective)) {
            rebuildPerspective(childBoard, perspective, king, destination);
            return;
        }
        System.arraycopy(source, 0, destination, 0, NnueNetwork.ACCUMULATOR_SIZE);
        while (changed != 0) {
            int square = Long.numberOfTrailingZeros(changed);
            changed &= changed - 1;
            int oldPiece = Board.getSquare(parentBoard[0], parentBoard[1], parentBoard[2], parentBoard[3], square);
            int newPiece = Board.getSquare(childBoard[0], childBoard[1], childBoard[2], childBoard[3], square);
            if (oldPiece != Value.NONE) {
                network.subtractFeature(feature(perspective, king, oldPiece, square), destination);
            }
            if (newPiece != Value.NONE) {
                network.addFeature(feature(perspective, king, newPiece, square), destination);
            }
        }
    }

    private void rebuildPerspective(long[] board, int perspective, int king, float[] destination) {
        network.resetAccumulator(destination);
        long occupied = board[0] | board[1] | board[2];
        while (occupied != 0) {
            int square = Long.numberOfTrailingZeros(occupied);
            occupied &= occupied - 1;
            int piece = Board.getSquare(board[0], board[1], board[2], board[3], square);
            network.addFeature(feature(perspective, king, piece, square), destination);
        }
    }

    private static int feature(int perspective, int king, int piece, int square) {
        return NnueFeatureSchema.featureIndex(perspective, king,
                piece >>> Board.PLAYER_SHIFT, piece & Piece.TYPE, square);
    }

    private static int kingSquare(long[] board, int perspective) {
        long kings = board[0] & ~board[1] & ~board[2];
        return Long.numberOfTrailingZeros(kings & (perspective == Value.WHITE ? ~board[3] : board[3]));
    }

    private static void requireBoard(long[] board) {
        requireBoardLength(board);
        long occupied = board[0] | board[1] | board[2];
        if ((board[3] & ~occupied) != 0 || (board[0] & board[1] & board[2]) != 0) {
            throw new IllegalArgumentException("Invalid SeedV6 piece encoding.");
        }
        long kings = board[0] & ~board[1] & ~board[2];
        if (Long.bitCount(kings & ~board[3]) != 1) {
            throw new IllegalArgumentException("NNUE requires exactly one White king.");
        }
        if (Long.bitCount(kings & board[3]) != 1) {
            throw new IllegalArgumentException("NNUE requires exactly one Black king.");
        }
    }

    private static void requireBoardLength(long[] board) {
        Objects.requireNonNull(board, "board");
        if (board.length < Board.MAX_BITBOARDS) {
            throw new IllegalArgumentException("Board must contain at least " + Board.MAX_BITBOARDS + " longs.");
        }
    }

    private void rememberPlacement(long[] board) {
        placement0 = board[0];
        placement1 = board[1];
        placement2 = board[2];
        placementColour = board[3];
        prepared = true;
    }

    private void requirePrepared() {
        if (!prepared) {
            throw new IllegalStateException("No prepared NNUE accumulator is available.");
        }
    }

    void requireCompatible(long[] board, NnueNetwork expectedNetwork) {
        requirePrepared();
        requireBoardLength(board);
        if (network != expectedNetwork) {
            throw new IllegalArgumentException("Accumulator belongs to a different NNUE network.");
        }
        if (placement0 != board[0] || placement1 != board[1]
                || placement2 != board[2] || placementColour != board[3]) {
            throw new IllegalArgumentException("Accumulator does not match the supplied board placement.");
        }
    }

    // Called only after requireCompatible. Activation never changes raw state.
    void writeInput(int player, float[] input) {
        float[] first = player == Value.WHITE ? white : black;
        float[] second = player == Value.WHITE ? black : white;
        for (int unit = 0; unit < NnueNetwork.ACCUMULATOR_SIZE; unit++) {
            input[unit] = NnueNetwork.clip01(first[unit]);
            input[NnueNetwork.ACCUMULATOR_SIZE + unit] = NnueNetwork.clip01(second[unit]);
        }
    }
}
