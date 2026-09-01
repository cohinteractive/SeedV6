package com.ohinteractive.seedv6.core.move;

import java.util.Objects;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;

/**
 * Resolves coordinate intent only by selecting an exact move generated as legal
 * for a snapshot of the supplied position. Instances are reusable and single-thread confined.
 */
public final class LegalMoveResolver {

    private static final int MAX_MOVES = 256;
    private static final int[] LSB = Board.LSB;
    private static final long DB = Board.DB;

    private final long[] boardSnapshot = new long[Board.MAX_BITBOARDS];
    private final long[] moves = new long[MAX_MOVES];
    private final long[] genScratch = new long[Board.MAX_BITBOARDS];

    private int matchCount;
    private long matchedMove;

    public long resolve(long[] board, MoveIntent intent) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(intent, "intent");
        if(board.length < Board.MAX_BITBOARDS) {
            throw new IllegalArgumentException("Board must contain at least " + Board.MAX_BITBOARDS + " longs.");
        }
        System.arraycopy(board, 0, boardSnapshot, 0, Board.MAX_BITBOARDS);

        matchCount = 0;
        matchedMove = 0L;

        final long board0 = boardSnapshot[0];
        final long board1 = boardSnapshot[1];
        final long board2 = boardSnapshot[2];
        final long board3 = boardSnapshot[3];
        final int status = (int) boardSnapshot[Board.STATUS];
        final long key = boardSnapshot[Board.KEY];
        final long checkers = computeCheckers(board0, board1, board2, board3, status);
        if(checkers != 0L) {
            scan(
                Gen.genEvasion(board0, board1, board2, board3, status, key, true, checkers, moves, genScratch),
                intent
            );
        } else {
            scan(Gen.genTactical(board0, board1, board2, board3, status, key, true, moves, genScratch), intent);
            scan(Gen.genQuiet(board0, board1, board2, board3, status, key, true, moves, genScratch), intent);
        }

        if(matchCount == 0) {
            throw new IllegalArgumentException("No legal move matches intent: " + intent);
        }
        if(matchCount != 1) {
            throw new IllegalStateException("Multiple generated legal moves match intent: " + intent);
        }
        return matchedMove;
    }

    private void scan(int moveCount, MoveIntent intent) {
        for(int i = 0; i < moveCount; i ++) {
            final long move = moves[i];
            if(Move.fromSquare(move) == intent.fromSquare()
                && Move.toSquare(move) == intent.toSquare()
                && Move.promotion(move) == intent.promotion()) {
                matchCount ++;
                matchedMove = move;
            }
        }
    }

    private static long computeCheckers(long board0, long board1, long board2, long board3, int status) {
        final int player = status & Board.PLAYER_BIT;
        final long allOccupancy = board0 | board1 | board2;
        final long colorMask = ~(-(long) player ^ board3);
        final long king = board0 & ~board1 & ~board2 & colorMask;
        final int kingSquare = LSB[(int) (((king & -king) * DB) >>> 58)];
        return Board.getCheckers(board0, board1, board2, board3, colorMask, player, kingSquare, allOccupancy);
    }

}
