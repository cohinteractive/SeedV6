package com.ohinteractive.seedv6.search.common;

import java.util.Objects;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;

public final class SearchRequest {

    private final long[] board;
    private final GameHistory gameHistory;
    private final int depth;
    private final SearchObserver observer;

    public SearchRequest(long[] board, int depth) {
        this(board, GameHistory.initial(board), depth, SearchObserver.NONE);
    }

    public SearchRequest(long[] board, int depth, SearchObserver observer) {
        this(board, GameHistory.initial(board), depth, observer);
    }

    public SearchRequest(long[] board, GameHistory gameHistory, int depth) {
        this(board, gameHistory, depth, SearchObserver.NONE);
    }

    public SearchRequest(long[] board, GameHistory gameHistory, int depth, SearchObserver observer) {
        Objects.requireNonNull(board, "board");
        if(board.length < Board.MAX_BITBOARDS) {
            throw new IllegalArgumentException("Board must contain at least " + Board.MAX_BITBOARDS + " longs.");
        }
        if(depth < 1) {
            throw new IllegalArgumentException("Search depth must be at least 1: " + depth);
        }
        this.board = new long[Board.MAX_BITBOARDS];
        System.arraycopy(board, 0, this.board, 0, Board.MAX_BITBOARDS);
        Objects.requireNonNull(gameHistory, "gameHistory").requireCurrent(this.board);
        this.gameHistory = gameHistory.snapshot();
        this.depth = depth;
        this.observer = Objects.requireNonNull(observer, "observer");
    }

    public int depth() {
        return depth;
    }

    public SearchObserver observer() {
        return observer;
    }

    /**
     * Immutable, independently owned history whose final entry is this
     * request's root board.
     */
    public GameHistory gameHistory() {
        return gameHistory;
    }

    public void copyBoardInto(long[] destination) {
        Objects.requireNonNull(destination, "destination");
        if(destination.length < Board.MAX_BITBOARDS) {
            throw new IllegalArgumentException("Destination must contain at least " + Board.MAX_BITBOARDS + " longs.");
        }
        System.arraycopy(board, 0, destination, 0, Board.MAX_BITBOARDS);
    }

}
