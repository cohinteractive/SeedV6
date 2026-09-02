package com.ohinteractive.seedv6.core;

import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;

/** Clear, allocating reference SEE built only from authoritative legal moves. */
final class SeeLegalOracle {

    private static final int MAX_MOVES = 256;

    static int evaluate(long[] board, long move) {
        int status = Math.toIntExact(board[Board.STATUS]);
        int mover = status & Board.PLAYER_BIT;
        int from = (int) move & Board.SQUARE_BITS;
        int target = (int) (move >>> Board.TARGET_SQUARE_SHIFT) & Board.SQUARE_BITS;
        int movingType = Board.getSquare(board[0], board[1], board[2], board[3], from)
                & Piece.TYPE;
        int capturedType = Board.getSquare(board[0], board[1], board[2], board[3], target)
                & Piece.TYPE;
        if (capturedType == Value.NONE && movingType == Piece.PAWN
                && target == Board.enPassantSquare(status)) {
            capturedType = Piece.PAWN;
        }
        int promoteType = (int) (move >>> Board.PROMOTE_PIECE_SHIFT)
                & Board.PIECE_BITS & Piece.TYPE;

        long[] child = new long[Board.MAX_BITBOARDS];
        Board.makeMoveInto(board[0], board[1], board[2], board[3], status,
                board[Board.KEY], move, child);
        int initialGain = value(capturedType) + promotionDelta(promoteType);
        return initialGain - bestContinuation(child, target, 1 ^ mover);
    }

    private static int bestContinuation(long[] board, int target, int player) {
        int targetType = Board.getSquare(board[0], board[1], board[2], board[3], target)
                & Piece.TYPE;
        if (targetType == Piece.KING) {
            return 0;
        }

        long[] moves = new long[MAX_MOVES];
        long[] scratch = new long[Board.MAX_BITBOARDS];
        int count = Gen.genAll(board[0], board[1], board[2], board[3],
                Math.toIntExact(board[Board.STATUS]), board[Board.KEY], true, moves, scratch);
        int best = 0;
        for (int i = 0; i < count; i++) {
            long move = moves[i];
            int moveTarget = (int) (move >>> Board.TARGET_SQUARE_SHIFT) & Board.SQUARE_BITS;
            int encodedCapture = (int) (move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS;
            if (moveTarget != target || encodedCapture == Value.NONE) {
                continue;
            }

            int promoteType = (int) (move >>> Board.PROMOTE_PIECE_SHIFT)
                    & Board.PIECE_BITS & Piece.TYPE;
            long[] child = new long[Board.MAX_BITBOARDS];
            Board.makeMoveInto(board[0], board[1], board[2], board[3],
                    Math.toIntExact(board[Board.STATUS]), board[Board.KEY], move, child);
            int gain = Eval.exchangeValue(targetType) + promotionDelta(promoteType)
                    - bestContinuation(child, target, 1 ^ player);
            best = Math.max(best, gain);
        }
        return best;
    }

    private static int promotionDelta(int promoteType) {
        return promoteType == Value.NONE ? 0
                : Eval.exchangeValue(promoteType) - Eval.exchangeValue(Piece.PAWN);
    }

    private static int value(int pieceType) {
        return pieceType == Value.NONE ? 0 : Eval.exchangeValue(pieceType);
    }

    private SeeLegalOracle() {}
}
