package com.ohinteractive.seedv6.core.nnue.conventional;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Value;
import java.util.Objects;

/** Worker/ply-owned exact integer sums. Parents survive siblings, undo and retries.
 * Board placement deltas cover all move types; null moves change head order only. */
public final class ConventionalAccumulator {
    private final ConventionalNetwork network;
    private final int[][] values = new int[2][ConventionalNetwork.HIDDEN];
    private final long[] placement = new long[4];
    private boolean prepared;

    ConventionalAccumulator(ConventionalNetwork network) { this.network = Objects.requireNonNull(network); }

    public void rebuild(long[] board) {
        ConventionalFeatures.requireBoard(board);
        refresh(board, Value.WHITE); refresh(board, Value.BLACK); remember(board);
    }

    private void refresh(long[] board, int perspective) {
        int[] dst = values[perspective];
        int king = ConventionalFeatures.king(board, perspective);
        network.reset(dst);
        long occupied = board[0] | board[1] | board[2];
        while (occupied != 0) {
            int square = Long.numberOfTrailingZeros(occupied); occupied &= occupied - 1;
            int piece = Board.getSquare(board[0], board[1], board[2], board[3], square);
            network.add(ConventionalFeatures.index(perspective, king, piece, square), 1, dst);
        }
    }

    public void update(long[] parentBoard, long[] childBoard, ConventionalAccumulator parent) {
        if (parent == this) throw new IllegalArgumentException("Parent must be distinct");
        requireSource(parentBoard, parent);
        ConventionalFeatures.requireBoard(childBoard);
        long changed = 0;
        for (int i = 0; i < 4; i++) changed |= parentBoard[i] ^ childBoard[i];
        for (int side = 0; side < 2; side++) {
            int king = ConventionalFeatures.king(childBoard, side);
            if (ConventionalFeatures.context(side, king) != ConventionalFeatures.context(side, ConventionalFeatures.king(parentBoard, side))) {
                refresh(childBoard, side);
                continue;
            }
            System.arraycopy(parent.values[side], 0, values[side], 0, ConventionalNetwork.HIDDEN);
            long squares = changed;
            while (squares != 0) {
                int sq = Long.numberOfTrailingZeros(squares); squares &= squares - 1;
                int before = Board.getSquare(parentBoard[0], parentBoard[1], parentBoard[2], parentBoard[3], sq);
                int after = Board.getSquare(childBoard[0], childBoard[1], childBoard[2], childBoard[3], sq);
                if (before != Value.NONE) network.add(ConventionalFeatures.index(side, king, before, sq), -1, values[side]);
                if (after != Value.NONE) network.add(ConventionalFeatures.index(side, king, after, sq), 1, values[side]);
            }
        }
        remember(childBoard);
    }

    public void copyFrom(long[] board, ConventionalAccumulator source) {
        requireSource(board, source);
        for (int side = 0; side < 2; side++) System.arraycopy(source.values[side], 0, values[side], 0, ConventionalNetwork.HIDDEN);
        remember(board);
    }

    public int evaluate(long[] board) {
        requirePlacement(board);
        int stm = Board.player((int) board[Board.STATUS]);
        return network.score(values[stm], values[stm ^ 1]);
    }

    public int raw(int perspective, int unit) {
        if (!prepared) throw new IllegalStateException("Accumulator is not prepared");
        return values[Objects.checkIndex(perspective, 2)][Objects.checkIndex(unit, ConventionalNetwork.HIDDEN)];
    }

    private void requireSource(long[] board, ConventionalAccumulator source) {
        Objects.requireNonNull(source);
        if (source.network != network) throw new IllegalArgumentException("Network mismatch");
        source.requirePlacement(board);
    }
    private void requirePlacement(long[] board) {
        if (!prepared) throw new IllegalStateException("Accumulator is not prepared");
        if (board == null || board.length < Board.MAX_BITBOARDS) throw new IllegalArgumentException("Invalid board length");
        for (int i = 0; i < 4; i++) if (placement[i] != board[i]) throw new IllegalArgumentException("Stale accumulator placement");
    }
    private void remember(long[] board) { System.arraycopy(board, 0, placement, 0, 4); prepared = true; }
}
