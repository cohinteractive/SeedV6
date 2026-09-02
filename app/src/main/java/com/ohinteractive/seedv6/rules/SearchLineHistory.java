package com.ohinteractive.seedv6.rules;

import java.util.Arrays;
import java.util.Objects;

import com.ohinteractive.seedv6.core.Board;

/**
 * Search-owned primitive stack containing a game-history snapshot followed by
 * real legal positions on the current searched line. Null moves are not real
 * positions and must not be pushed here. Rule adjudication requires the board
 * to match the top real position, so a future null-move search cannot silently
 * count its artificial transition; repetition treatment inside null branches
 * remains an explicit policy decision for the selective-search workstream.
 */
public final class SearchLineHistory {

    public SearchLineHistory(GameHistory gameHistory) {
        Objects.requireNonNull(gameHistory, "gameHistory");
        rootSize = gameHistory.size();
        final long desired = (long) rootSize + DEFAULT_LINE_CAPACITY;
        final int capacity = desired > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) desired;
        keys = new long[Math.max(rootSize, capacity)];
        gameHistory.copyInto(keys);
        size = rootSize;
    }

    public int size() {
        return size;
    }

    public int rootSize() {
        return rootSize;
    }

    public long currentKey() {
        return keys[size - 1];
    }

    public void pushRealPosition(long[] position) {
        Objects.requireNonNull(position, "position");
        if(position.length < Board.MAX_BITBOARDS) {
            throw new IllegalArgumentException("Board must contain at least " + Board.MAX_BITBOARDS + " longs.");
        }
        pushRealPosition(
            position[0], position[1], position[2], position[3],
            (int) position[Board.STATUS], position[Board.KEY]
        );
    }

    public void pushRealPosition(
        long board0, long board1, long board2, long board3, int status, long boardKey
    ) {
        ensureCapacity(size + 1);
        keys[size ++] = PositionIdentity.repetitionKey(
            board0, board1, board2, board3, status, boardKey, identityMoves, generatorScratch
        );
    }

    public void popRealPosition() {
        if(size == rootSize) {
            throw new IllegalStateException("Cannot pop the game-history root from a search line.");
        }
        keys[-- size] = 0L;
    }

    /**
     * Restores the immutable game-history prefix after normal or exceptional
     * traversal unwind.
     */
    public void restoreRoot() {
        Arrays.fill(keys, rootSize, size, 0L);
        size = rootSize;
    }

    public int currentOccurrences(long[] currentBoard) {
        Objects.requireNonNull(currentBoard, "currentBoard");
        if(currentBoard.length < Board.MAX_BITBOARDS) {
            throw new IllegalArgumentException("Board must contain at least " + Board.MAX_BITBOARDS + " longs.");
        }
        return currentOccurrences(
            currentBoard[0], currentBoard[1], currentBoard[2], currentBoard[3],
            (int) currentBoard[Board.STATUS], currentBoard[Board.KEY]
        );
    }

    public int currentOccurrences(
        long board0, long board1, long board2, long board3, int status, long boardKey
    ) {
        final long current = PositionIdentity.repetitionKey(
            board0, board1, board2, board3, status, boardKey, identityMoves, generatorScratch
        );
        if(current != currentKey()) {
            throw new IllegalArgumentException("Current board does not match the top search-line position.");
        }
        final int reversiblePlies = Board.halfMoveClock(status);
        final int first = reversiblePlies == Board.MAX_HALF_MOVE_CLOCK
            ? 0
            : Math.max(0, size - 1 - reversiblePlies);
        int count = 0;
        for(int i = first; i < size; i ++) {
            if(keys[i] == current) count ++;
        }
        return count;
    }

    public boolean isFormalThreefold(long[] currentBoard) {
        return currentOccurrences(currentBoard) >= 3;
    }

    public boolean isFormalThreefold(
        long board0, long board1, long board2, long board3, int status, long boardKey
    ) {
        return currentOccurrences(board0, board1, board2, board3, status, boardKey) >= 3;
    }

    private static final int DEFAULT_LINE_CAPACITY = 64;
    private static final int MAX_MOVES = 256;

    private final int rootSize;
    private final long[] identityMoves = new long[MAX_MOVES];
    private final long[] generatorScratch = new long[Board.MAX_BITBOARDS];
    private long[] keys;
    private int size;

    private void ensureCapacity(int required) {
        if(required <= keys.length) return;
        if(required < 0) throw new IllegalStateException("Search-line history capacity overflow.");
        final int grown = keys.length <= (Integer.MAX_VALUE >>> 1)
            ? keys.length << 1
            : Integer.MAX_VALUE;
        if(grown < required) throw new IllegalStateException("Search-line history capacity overflow.");
        keys = Arrays.copyOf(keys, grown);
    }
}
