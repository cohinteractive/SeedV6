package com.ohinteractive.seedv6.rules;

import java.util.Objects;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.core.util.Zobrist;

/**
 * Produces the identity used by the repetition rules.
 *
 * <p>{@link Board#KEY} includes every stored en-passant file. Repetition
 * identity includes that file only when the side to move has a legal
 * en-passant capture, because only then does it change the set of legal
 * moves. Piece placement, side to move, and castling rights are inherited
 * unchanged from the authoritative board key.</p>
 */
public final class PositionIdentity {

    public static long repetitionKey(long[] board) {
        requireBoard(board);
        final int status = (int) board[Board.STATUS];
        if(Board.enPassantSquare(status) == Value.INVALID) return board[Board.KEY];
        return repetitionKey(
            board[0], board[1], board[2], board[3], status, board[Board.KEY],
            new long[MAX_MOVES], new long[Board.MAX_BITBOARDS]
        );
    }

    /**
     * Allocation-free variant for a search-owned move and generator buffer.
     */
    public static long repetitionKey(
        long board0, long board1, long board2, long board3, int status, long boardKey,
        long[] moves, long[] generatorScratch
    ) {
        final int eSquare = Board.enPassantSquare(status);
        if(eSquare == Value.INVALID) return boardKey;
        Objects.requireNonNull(moves, "moves");
        Objects.requireNonNull(generatorScratch, "generatorScratch");
        if(moves.length < MAX_MOVES) {
            throw new IllegalArgumentException("Move buffer must contain at least " + MAX_MOVES + " longs.");
        }
        if(generatorScratch.length < Board.MAX_BITBOARDS) {
            throw new IllegalArgumentException(
                "Generator scratch must contain at least " + Board.MAX_BITBOARDS + " longs."
            );
        }

        final int moveCount = Gen.genTactical(
            board0, board1, board2, board3, status, boardKey, true, moves, generatorScratch
        );
        for(int i = 0; i < moveCount; i ++) {
            final long move = moves[i];
            final int targetSquare = (int) (move >>> Board.TARGET_SQUARE_SHIFT) & Board.SQUARE_BITS;
            final int startPiece = (int) (move >>> Board.START_PIECE_SHIFT) & Board.PIECE_BITS;
            final int targetPiece = (int) (move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS;
            if(targetSquare == eSquare
                && (startPiece & Piece.TYPE) == Piece.PAWN
                && targetPiece == Value.NONE) {
                return boardKey;
            }
        }
        return boardKey ^ Zobrist.ENPASSANT_FILE[eSquare & Value.FILE];
    }

    private static final int MAX_MOVES = 256;

    private PositionIdentity() {}

    private static void requireBoard(long[] board) {
        Objects.requireNonNull(board, "board");
        if(board.length < Board.MAX_BITBOARDS) {
            throw new IllegalArgumentException("Board must contain at least " + Board.MAX_BITBOARDS + " longs.");
        }
    }
}
