package com.ohinteractive.seedv6.core.move;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Piece;

/** Immutable presentation history from committed legal moves, never inferred from missing material. */
public final class CapturedPieces {
    private final int[] counts;
    private final boolean fromStartingPosition;

    private CapturedPieces(int[] counts, boolean fromStartingPosition) {
        this.counts = counts;
        this.fromStartingPosition = fromStartingPosition;
    }

    public static CapturedPieces empty(boolean fromStartingPosition) {
        return new CapturedPieces(new int[15], fromStartingPosition);
    }

    /** True only when the observed game's initial position was the standard start. */
    public boolean fromStartingPosition() { return fromStartingPosition; }

    /** Pieces of this colour/type captured by the opponent. */
    public int count(int piece) { return counts[piece]; }

    /** Call only after this exact generated move was committed; also handles legacy metadata-free moves. */
    public CapturedPieces afterMove(long move) {
        int captured = (int) (move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS;
        int mover = (int) (move >>> Board.START_PIECE_SHIFT) & Board.PIECE_BITS;
        // Seed's legal en-passant move encodes an empty destination, not a captured target piece.
        if (captured == 0 && (mover & Piece.TYPE) == Piece.PAWN
                && (Move.fromSquare(move) & 7) != (Move.toSquare(move) & 7)) {
            captured = Piece.PAWN | ((mover ^ 8) & 8);
        }
        int type = captured & Piece.TYPE;
        if (type < Piece.QUEEN || type > Piece.PAWN) return this;
        int[] next = counts.clone();
        next[captured]++;
        return new CapturedPieces(next, fromStartingPosition);
    }
}
