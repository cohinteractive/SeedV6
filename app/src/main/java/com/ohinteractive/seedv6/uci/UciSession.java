package com.ohinteractive.seedv6.uci;

import java.util.Arrays;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.LegalMoveResolver;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.flat.FlatNegamax;

final class UciSession {

    UciSession() {
        reset();
    }

    void reset() {
        final long[] initial = Board.startingPosition();
        board = initial;
        history = GameHistory.initial(initial);
        search = new FlatNegamax();
    }

    void setPosition(String[] tokens) {
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
        board = candidateBoard;
        history = candidateSnapshot;
    }

    SearchResult search(int depth) {
        return search.search(new SearchRequest(board, history, depth));
    }

    long[] boardSnapshot() {
        return board.clone();
    }

    GameHistory historySnapshot() {
        return history.snapshot();
    }

    private final LegalMoveResolver resolver = new LegalMoveResolver();
    private long[] board;
    private GameHistory history;
    private FlatNegamax search;
}
