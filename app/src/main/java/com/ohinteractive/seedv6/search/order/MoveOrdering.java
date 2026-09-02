package com.ohinteractive.seedv6.search.order;

import java.util.Arrays;
import java.util.Objects;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;

/**
 * Worker-owned bounded history and killer state for staged move ordering.
 *
 * <p>Only legal quiet moves should be recorded after a future beta cutoff.
 * This class nevertheless rechecks tactical eligibility so a capture,
 * en-passant move, or promotion can never enter either heuristic. There is no
 * global mutable ordering state.</p>
 */
public final class MoveOrdering {

    public static final int KILLER_SLOTS = 2;
    public static final int HISTORY_LIMIT = 16_384;
    public static final int MAX_BONUS_DEPTH = 64;

    private static final int PIECE_TYPE_COUNT = Piece.PAWN + 1;
    private static final int SQUARE_COUNT = 64;
    private static final int SIDE_STRIDE = PIECE_TYPE_COUNT * SQUARE_COUNT * SQUARE_COUNT;
    private static final int PIECE_STRIDE = SQUARE_COUNT * SQUARE_COUNT;

    private final int maxPly;
    private final int[] history = new int[2 * SIDE_STRIDE];
    private final long[] killers;
    private final StagedMovePicker picker;

    public MoveOrdering(int maxPly) {
        if(maxPly < 1) {
            throw new IllegalArgumentException("Maximum ply must be at least 1: " + maxPly);
        }
        this.maxPly = maxPly;
        killers = new long[maxPly * KILLER_SLOTS];
        picker = new StagedMovePicker(this, maxPly);
    }

    public int maxPly() {
        return maxPly;
    }

    /** Reusable picker with independent primitive state for every configured ply. */
    public StagedMovePicker picker() {
        return picker;
    }

    /**
     * Record a legal quiet beta-cutoff move for both history and killers.
     *
     * <p>The positive history bonus is {@code min(depth, 64)^2}. Addition is
     * performed against the remaining room below {@link #HISTORY_LIMIT}, so no
     * intermediate or stored value can overflow. Tactical moves and zero are
     * ignored and return {@code false}.</p>
     */
    public boolean recordQuietCutoff(long[] board, int ply, long move, int depth) {
        requireBoard(board);
        requirePly(ply);
        if(depth < 1) {
            throw new IllegalArgumentException("Cutoff depth must be at least 1: " + depth);
        }
        if(move == StagedMovePicker.NO_MOVE || isTactical(board, move)) return false;

        final int historyIndex = historyIndex(move);
        final int cappedDepth = Math.min(depth, MAX_BONUS_DEPTH);
        final int bonus = cappedDepth * cappedDepth;
        final int current = history[historyIndex];
        history[historyIndex] = current + Math.min(bonus, HISTORY_LIMIT - current);
        recordKiller(ply, move);
        return true;
    }

    /** Current history score for the complete side/piece/from/to index. */
    public int historyScore(long move) {
        return history[historyIndex(move)];
    }

    /** Killer slot zero is most recent; slot one is the distinct older move. */
    public long killer(int ply, int slot) {
        requirePly(ply);
        if(slot < 0 || slot >= KILLER_SLOTS) {
            throw new IndexOutOfBoundsException("Killer slot out of range: " + slot);
        }
        return killers[ply * KILLER_SLOTS + slot];
    }

    /** Deterministically clear history, killers, and any prepared picker frames. */
    public void reset() {
        Arrays.fill(history, 0);
        Arrays.fill(killers, StagedMovePicker.NO_MOVE);
        picker.reset();
    }

    /**
     * True for the exact tactical contract supplied by production
     * {@code Gen.genTactical}: captures, en passant, and every promotion.
     */
    public static boolean isTactical(long[] board, long move) {
        requireBoard(board);
        if(move == StagedMovePicker.NO_MOVE) return false;

        final int targetPiece = (int) (move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS;
        final int promotePiece = (int) (move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS;
        if(targetPiece != Value.NONE || promotePiece != Value.NONE) return true;

        final int startPiece = (int) (move >>> Board.START_PIECE_SHIFT) & Board.PIECE_BITS;
        if((startPiece & Piece.TYPE) != Piece.PAWN) return false;
        final int from = Move.fromSquare(move);
        final int target = Move.toSquare(move);
        return (from & Value.FILE) != (target & Value.FILE)
            && target == Board.enPassantSquare(Math.toIntExact(board[Board.STATUS]));
    }

    private void recordKiller(int ply, long move) {
        final int base = ply * KILLER_SLOTS;
        final long first = killers[base];
        if(move == first) return;
        if(move == killers[base + 1]) {
            killers[base] = move;
            killers[base + 1] = first;
            return;
        }
        killers[base + 1] = first;
        killers[base] = move;
    }

    private static int historyIndex(long move) {
        final int startPiece = (int) (move >>> Board.START_PIECE_SHIFT) & Board.PIECE_BITS;
        final int player = startPiece >>> Board.PLAYER_SHIFT;
        final int pieceType = startPiece & Piece.TYPE;
        if(player < Value.WHITE || player > Value.BLACK
            || pieceType < Piece.KING || pieceType > Piece.PAWN) {
            throw new IllegalArgumentException("Move has no valid encoded moving piece: " + move);
        }
        return player * SIDE_STRIDE + pieceType * PIECE_STRIDE
            + Move.fromSquare(move) * SQUARE_COUNT + Move.toSquare(move);
    }

    private void requirePly(int ply) {
        if(ply < 0 || ply >= maxPly) {
            throw new IndexOutOfBoundsException(
                "Ply out of range: " + ply + " (configured maximum " + maxPly + ")"
            );
        }
    }

    static void requireBoard(long[] board) {
        Objects.requireNonNull(board, "board");
        if(board.length < Board.MAX_BITBOARDS) {
            throw new IllegalArgumentException(
                "Board must contain at least " + Board.MAX_BITBOARDS + " longs."
            );
        }
    }
}
