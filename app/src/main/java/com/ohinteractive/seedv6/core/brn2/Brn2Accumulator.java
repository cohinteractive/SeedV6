package com.ohinteractive.seedv6.core.brn2;

import java.util.Arrays;
import java.util.Objects;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.BrnFeatureSchema;
import static com.ohinteractive.seedv6.core.brn2.Brn2Model.*;

/** Worker/ply-owned incident-relation sums, indexed by actual square, bound to one immutable model.
 * Board differences cover all move types without duplicating chess rules. Parents are never changed;
 * unmake simply returns to the parent's slot. Status is recomputed in canonical bit order at inference.
 * Binary64 reassociation differs from the reference by roundoff; rebuild every 32 transitions bounds
 * drift independently of game length. No Zobrist input, shared scratch or successful-path allocation.
 */
public final class Brn2Accumulator {
    private final Brn2Model model;
    private final double[] relations = new double[64 * HIDDEN_WIDTH];
    private final long[] position = new long[5];
    private final int[] codes = new int[64];
    private final double[] context = new double[HIDDEN_WIDTH], pooled = new double[HIDDEN_WIDTH];
    private boolean prepared;
    private int distance;
    private double raw = Double.NaN;

    /** Value-head preactivation from the last successful evaluation, before tanh. */
    public double raw() {
        if (Double.isNaN(raw)) throw new IllegalStateException("No successful BRN-2 evaluation.");
        return raw;
    }

    public Brn2Accumulator(Brn2Model model) { this.model = Objects.requireNonNull(model); }

    public void rebuild(long[] board) {
        requireBoard(board);
        decode(board);
        double[] weights = model.weights();
        long occupied = occupied(board);
        for (long remaining = occupied; remaining != 0; remaining &= remaining - 1) {
            int square = Long.numberOfTrailingZeros(remaining);
            rebuildSquare(board, occupied, square, weights);
        }
        remember(board); distance = 0;
    }

    public void update(long[] parentBoard, long[] childBoard, Brn2Accumulator parent) {
        if (parent == this) throw new IllegalArgumentException("BRN-2 parent and child must be distinct.");
        Objects.requireNonNull(parent).requireCompatible(parentBoard, model);
        requireBoard(childBoard);
        long changed = (parentBoard[0] ^ childBoard[0]) | (parentBoard[1] ^ childBoard[1])
                | (parentBoard[2] ^ childBoard[2]) | (parentBoard[3] ^ childBoard[3]);
        if (parent.distance >= 31 || Long.bitCount(changed) > 4) { rebuild(childBoard); return; }
        decode(childBoard);
        double[] weights = model.weights();
        long oldOccupied = occupied(parentBoard), newOccupied = occupied(childBoard);
        for (long remaining = newOccupied; remaining != 0; remaining &= remaining - 1) {
            int square = Long.numberOfTrailingZeros(remaining), offset = square * HIDDEN_WIDTH;
            if ((changed & (1L << square)) != 0) {
                rebuildSquare(childBoard, newOccupied, square, weights);
            } else {
                System.arraycopy(parent.relations, offset, relations, offset, HIDDEN_WIDTH);
                for (long edits = changed; edits != 0; edits &= edits - 1) {
                    int other = Long.numberOfTrailingZeros(edits);
                    if ((oldOccupied & (1L << other)) != 0)
                        subtract(weights, endpointRow(parent.codes, square, other), offset);
                    if ((newOccupied & (1L << other)) != 0)
                        add(weights, endpointRow(codes, square, other), offset);
                }
            }
        }
        remember(childBoard); distance = parent.distance + (changed == 0 ? 0 : 1);
    }

    /** Includes every raw status bit, including side, counters and en-passant; key is ignored. */
    public double evaluate(long[] board) {
        raw = Double.NaN;
        requireCompatible(board, model);
        double[] weights = model.weights();
        Arrays.fill(context, 0);
        for (long status = board[Board.STATUS]; status != 0; status &= status - 1) {
            int row = (STATUS_ROW + Long.numberOfTrailingZeros(status)) * HIDDEN_WIDTH;
            for (int h = 0; h < HIDDEN_WIDTH; h++) context[h] += weights[row + h];
        }
        boolean bounded = model.boundedIntermediates();
        if (!bounded) for (double value : context) finite(value);
        System.arraycopy(weights, BOARD_BIAS_OFFSET, pooled, 0, HIDDEN_WIDTH);
        for (long remaining = occupied(board); remaining != 0; remaining &= remaining - 1) {
            int square = Long.numberOfTrailingZeros(remaining), offset = square * HIDDEN_WIDTH;
            int row = ((codes[square] - 1) * 64 + square) * HIDDEN_WIDTH;
            for (int h = 0; h < HIDDEN_WIDTH; h++) {
                double pre = weights[row + h] + context[h] + weights[LOCAL_BIAS_OFFSET + h] + relations[offset + h];
                if (!bounded) finite(pre);
                pooled[h] += Math.max(0, pre);
            }
        }
        double z = weights[OUTPUT_BIAS];
        for (int h = 0; h < HIDDEN_WIDTH; h++) {
            if (!bounded) finite(pooled[h]); z += weights[OUTPUT_WEIGHT_OFFSET + h] * Math.max(0, pooled[h]);
        }
        finite(z); raw = z; return StrictMath.tanh(z);
    }

    private void rebuildSquare(long[] board, long occupied, int square, double[] weights) {
        int offset = square * HIDDEN_WIDTH;
        Arrays.fill(relations, offset, offset + HIDDEN_WIDTH, 0);
        // For one endpoint, original pair order is exactly ascending other-square order.
        for (long others = occupied & ~(1L << square); others != 0; others &= others - 1)
            add(weights, endpointRow(codes, square, Long.numberOfTrailingZeros(others)), offset);
    }

    private static int endpointRow(int[] codes, int square, int other) {
        int relation = BrnFeatureSchema.relationIndex(codes[square], square, codes[other], other)
                - BrnFeatureSchema.RELATION_OFFSET;
        return ((square < other ? RELATION_A_ROW : RELATION_B_ROW) + relation) * HIDDEN_WIDTH;
    }
    private void add(double[] weights, int row, int offset) {
        for (int h = 0; h < HIDDEN_WIDTH; h++) relations[offset + h] += weights[row + h];
    }
    private void subtract(double[] weights, int row, int offset) {
        for (int h = 0; h < HIDDEN_WIDTH; h++) relations[offset + h] -= weights[row + h];
    }
    private static int code(long[] board, int square) { return Board.getSquare(board[0], board[1], board[2], board[3], square); }
    private void decode(long[] board) {
        for (long remaining = occupied(board); remaining != 0; remaining &= remaining - 1) {
            int square = Long.numberOfTrailingZeros(remaining); codes[square] = code(board, square);
        }
    }
    private static long occupied(long[] board) { return board[0] | board[1] | board[2] | board[3]; }
    private static void requireBoard(long[] board) {
        if (Objects.requireNonNull(board).length < 5) throw new IllegalArgumentException("Five position longs required.");
    }
    private void remember(long[] board) { System.arraycopy(board, 0, position, 0, 5); prepared = true; }
    private void requireCompatible(long[] board, Brn2Model expected) {
        requireBoard(board);
        if (!prepared) throw new IllegalStateException("Unprepared BRN-2 accumulator.");
        if (model != expected) throw new IllegalArgumentException("BRN-2 model mismatch.");
        for (int i = 0; i < 5; i++) if (position[i] != board[i]) throw new IllegalArgumentException("BRN-2 position mismatch.");
    }
    private static void finite(double value) {
        if (!Double.isFinite(value)) throw new ArithmeticException("Nonfinite BRN-2 preactivation.");
    }
}
