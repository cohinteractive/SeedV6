package com.ohinteractive.seedv6.rules;

import java.util.Arrays;
import java.util.Objects;

import com.ohinteractive.seedv6.core.Board;

/**
 * Immutable ordered repetition identities from the supplied initial position
 * through the current game/search root. The initial position and current root
 * each occupy an ordinary sequence entry; earlier occurrences of the same
 * identity remain distinct entries.
 */
public final class GameHistory {

    public static GameHistory initial(long[] initialBoard) {
        final long repetitionKey = PositionIdentity.repetitionKey(initialBoard);
        return new GameHistory(
            new long[] {repetitionKey},
            initialBoard[Board.KEY],
            (int) initialBoard[Board.STATUS]
        );
    }

    public static Builder builder(long[] initialBoard) {
        return new Builder(initialBoard);
    }

    public int size() {
        return keys.length;
    }

    public long keyAt(int index) {
        return keys[index];
    }

    public long currentKey() {
        return keys[keys.length - 1];
    }

    public int occurrences(long key) {
        int count = 0;
        for(long candidate : keys) {
            if(candidate == key) count ++;
        }
        return count;
    }

    /**
     * Counts the current position inside the reversible window represented by
     * the board's halfmove clock. A count of one is the current root itself.
     * Pawn moves and captures make earlier identities unreachable. Castling-
     * right changes need not reset the clock: their distinct keys remain in
     * the scanned superset and cannot create a false match. At the saturated
     * packed value, all supplied history is scanned because the true reversible
     * run may be longer than the stored value.
     */
    public int currentOccurrences(long[] currentBoard) {
        requireCurrent(currentBoard);
        final int reversiblePlies = Board.halfMoveClock((int) currentBoard[Board.STATUS]);
        final int first = reversiblePlies == Board.MAX_HALF_MOVE_CLOCK
            ? 0
            : Math.max(0, keys.length - 1 - reversiblePlies);
        final long current = currentKey();
        int count = 0;
        for(int i = first; i < keys.length; i ++) {
            if(keys[i] == current) count ++;
        }
        return count;
    }

    public boolean isFormalThreefold(long[] currentBoard) {
        return currentOccurrences(currentBoard) >= 3;
    }

    public boolean matchesCurrent(long[] currentBoard) {
        return currentBoard != null
            && currentBoard.length >= Board.MAX_BITBOARDS
            && currentBoard[Board.KEY] == currentBoardKey
            && (int) currentBoard[Board.STATUS] == currentStatus
            && PositionIdentity.repetitionKey(currentBoard) == currentKey();
    }

    public void requireCurrent(long[] currentBoard) {
        if(!matchesCurrent(currentBoard)) {
            throw new IllegalArgumentException("Current board does not match the final game-history position.");
        }
    }

    /**
     * Returns an independently owned immutable snapshot.
     */
    public GameHistory snapshot() {
        return new GameHistory(keys.clone(), currentBoardKey, currentStatus);
    }

    void copyInto(long[] destination) {
        System.arraycopy(keys, 0, destination, 0, keys.length);
    }

    private final long[] keys;
    private final long currentBoardKey;
    private final int currentStatus;

    private GameHistory(long[] keys, long currentBoardKey, int currentStatus) {
        if(keys.length == 0) throw new IllegalArgumentException("Game history must contain an initial position.");
        this.keys = keys;
        this.currentBoardKey = currentBoardKey;
        this.currentStatus = currentStatus;
    }

    public static final class Builder {

        private Builder(long[] initialBoard) {
            keys[0] = PositionIdentity.repetitionKey(initialBoard);
            currentBoardKey = initialBoard[Board.KEY];
            currentStatus = (int) initialBoard[Board.STATUS];
            size = 1;
        }

        /**
         * Appends a position after its move has been resolved and applied by
         * the authoritative legal-move path.
         */
        public Builder appendPosition(long[] position) {
            Objects.requireNonNull(position, "position");
            ensureCapacity(size + 1);
            keys[size ++] = PositionIdentity.repetitionKey(position);
            currentBoardKey = position[Board.KEY];
            currentStatus = (int) position[Board.STATUS];
            return this;
        }

        public int size() {
            return size;
        }

        public GameHistory snapshot() {
            return new GameHistory(
                Arrays.copyOf(keys, size), currentBoardKey, currentStatus
            );
        }

        private long[] keys = new long[16];
        private int size;
        private long currentBoardKey;
        private int currentStatus;

        private void ensureCapacity(int required) {
            if(required <= keys.length) return;
            if(required < 0) throw new IllegalStateException("Game history capacity overflow.");
            final int grown = keys.length <= (Integer.MAX_VALUE >>> 1)
                ? keys.length << 1
                : Integer.MAX_VALUE;
            if(grown < required) throw new IllegalStateException("Game history capacity overflow.");
            keys = Arrays.copyOf(keys, grown);
        }
    }
}
