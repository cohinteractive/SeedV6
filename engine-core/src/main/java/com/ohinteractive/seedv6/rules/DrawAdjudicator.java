package com.ohinteractive.seedv6.rules;

import java.util.Objects;

import com.ohinteractive.seedv6.core.Board;

/**
 * Position-history and rule-state draw adjudication outside static evaluation.
 * Callers must establish that the position has a legal move first; checkmate
 * and stalemate therefore take precedence over these non-terminal rule draws.
 * Repetition here is formal current-position threefold (at least three counted
 * occurrences). WS2 deliberately defines no separate twofold/line-repeat draw
 * policy.
 */
public final class DrawAdjudicator {

    public enum RuleDraw {
        NONE,
        FIFTY_MOVE,
        FORMAL_THREEFOLD,
        INSUFFICIENT_MATERIAL
    }

    public static RuleDraw adjudicateNonTerminal(long[] board, SearchLineHistory history) {
        Objects.requireNonNull(board, "board");
        if(board.length < Board.MAX_BITBOARDS) {
            throw new IllegalArgumentException("Board must contain at least " + Board.MAX_BITBOARDS + " longs.");
        }
        return adjudicateNonTerminal(
            board[0], board[1], board[2], board[3],
            (int) board[Board.STATUS], board[Board.KEY], history
        );
    }

    public static RuleDraw adjudicateNonTerminal(
        long board0, long board1, long board2, long board3, int status, long boardKey,
        SearchLineHistory history
    ) {
        Objects.requireNonNull(history, "history");
        if(isFiftyMoveClaimable(status)) return RuleDraw.FIFTY_MOVE;
        if(history.isFormalThreefold(board0, board1, board2, board3, status, boardKey)) {
            return RuleDraw.FORMAL_THREEFOLD;
        }
        if(InsufficientMaterial.isAutomaticDraw(board0, board1, board2, board3)) {
            return RuleDraw.INSUFFICIENT_MATERIAL;
        }
        return RuleDraw.NONE;
    }

    public static boolean isFiftyMoveClaimable(int status) {
        return Board.halfMoveClock(status) >= FIFTY_MOVE_HALFMOVES;
    }

    public static final int FIFTY_MOVE_HALFMOVES = 100;

    private DrawAdjudicator() {}
}
