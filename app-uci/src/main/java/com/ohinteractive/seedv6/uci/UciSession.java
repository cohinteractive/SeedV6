package com.ohinteractive.seedv6.uci;

import java.util.Arrays;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.LegalMoveResolver;
import com.ohinteractive.seedv6.rules.GameHistory;

final class UciSession {

    UciSession() {
        reset();
    }

    void reset() {
        final long[] initial = Board.startingPosition();
        board = initial;
        history = GameHistory.initial(initial);
    }

    void setPosition(String[] tokens) {
        install(parsePosition(tokens));
    }

    PositionState parsePosition(String[] tokens) {
        if(tokens.length < 2 || !tokens[0].equals("position")) {
            throw new IllegalArgumentException("Malformed position command.");
        }

        final long[] initial;
        final int movesMarker;
        if(tokens[1].equals("startpos")) {
            initial = Board.startingPosition();
            movesMarker = 2;
        } else if(tokens[1].equals("fen")) {
            if(tokens.length < 8) throw new IllegalArgumentException("Position FEN requires six fields.");
            initial = Board.fromFen(String.join(" ", Arrays.copyOfRange(tokens, 2, 8)));
            movesMarker = 8;
        } else {
            throw new IllegalArgumentException("Unsupported position source.");
        }

        validate(initial);
        int moveIndex = movesMarker;
        if(moveIndex < tokens.length) {
            if(!tokens[moveIndex].equals("moves")) {
                throw new IllegalArgumentException("Unexpected position tokens.");
            }
            moveIndex ++;
        }

        long[] candidateBoard = initial;
        final GameHistory.Builder candidateHistory = GameHistory.builder(initial);
        for(; moveIndex < tokens.length; moveIndex ++) {
            final long move = resolver.resolve(candidateBoard, UciMoveParser.parse(tokens[moveIndex]));
            final long[] child = new long[Board.MAX_BITBOARDS];
            Board.makeMoveInto(
                candidateBoard[0], candidateBoard[1], candidateBoard[2], candidateBoard[3],
                (int) candidateBoard[Board.STATUS], candidateBoard[Board.KEY], move, child
            );
            candidateHistory.appendPosition(child);
            candidateBoard = child;
        }

        final GameHistory candidateSnapshot = candidateHistory.snapshot();
        candidateSnapshot.requireCurrent(candidateBoard);
        return new PositionState(candidateBoard, candidateSnapshot);
    }

    void install(PositionState state) {
        board = state.board;
        history = state.history;
    }

    PositionState snapshot() {
        return new PositionState(board.clone(), history.snapshot());
    }

    long[] boardSnapshot() {
        return board.clone();
    }

    GameHistory historySnapshot() {
        return history.snapshot();
    }

    private final LegalMoveResolver resolver = new LegalMoveResolver();

    /** Reject structurally unsafe FENs before they reach the optimized generator. */
    private static void validate(long[] board) {
        long kings = board[0] & ~board[1] & ~board[2];
        if (Long.bitCount(kings & ~board[3]) != 1 || Long.bitCount(kings & board[3]) != 1)
            throw new IllegalArgumentException("FEN requires exactly one king per side");
        long pawns = ~board[0] & board[1] & board[2];
        if ((pawns & 0xff000000000000ffL) != 0)
            throw new IllegalArgumentException("FEN has an unpromoted pawn on a back rank");
        int status = (int)board[Board.STATUS], side = Board.player(status);
        if (Board.isPlayerInCheck(board[0], board[1], board[2], board[3], side ^ 1))
            throw new IllegalArgumentException("FEN leaves the side that just moved in check");
        for (int color = 0; color < 2; color++) {
            int back = color * 56, owner = color * 8;
            if ((Board.kingSide(status, color) || Board.queenSide(status, color))
                    && square(board, back + 4) != (owner | 1))
                throw new IllegalArgumentException("FEN castling right has no king on its home square");
            if (Board.kingSide(status, color) && square(board, back + 7) != (owner | 3)
                    || Board.queenSide(status, color) && square(board, back) != (owner | 3))
                throw new IllegalArgumentException("FEN castling right has no matching rook");
        }
        int ep = Board.enPassantSquare(status);
        if (ep >= 0 && (square(board, ep) != 0
                || square(board, ep + (side == 0 ? -8 : 8)) != (((side ^ 1) * 8) | 6)
                || square(board, ep + (side == 0 ? 8 : -8)) != 0))
            throw new IllegalArgumentException("FEN en passant target has no matching double pawn move");
    }

    private static int square(long[] board, int square) {
        int piece = Board.getSquare(board[0], board[1], board[2], board[3], square);
        return (piece & 7) == 0 ? 0 : piece;
    }
    private long[] board;
    private GameHistory history;

    static final class PositionState {
        final long[] board;
        final GameHistory history;

        PositionState(long[] board, GameHistory history) {
            this.board = board;
            this.history = history;
        }
    }
}
