package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.util.Arrays;
import static com.ohinteractive.seedv6.core.brnpair2.LowRankRelationNetwork.*;

/** Immutable R056 ONE inference snapshot. No training or production selection. */
public final class IncrementalOne {
    private static final int POOL = RANK * WIDTH;
    private final double[] products = new double[768 * POOL];
    private final double[] queries = new double[768 * RANK];
    private final double[] selfContent = new double[768 * WIDTH];
    private final double[] readout = new double[768 * WIDTH];
    private final double[] unary = new double[768];
    private final double[] norms = new double[33];
    private final double bias;

    public IncrementalOne(LowRankRelationNetwork model) {
        if (model.compose) throw new IllegalArgumentException("ONE required");
        for (double x : model.weights) if (!Double.isFinite(x) || Math.abs(x) > .99)
            throw new IllegalArgumentException("Finite R056 clipped weights required");
        bias = model.weights[768];
        for (int n = 2; n <= 32; n++) norms[n] = 1 / Math.sqrt(RANK * (n - 1));
        for (int node = 0; node < 768; node++) {
            unary[node] = model.weights[node];
            double self = 0;
            for (int r = 0; r < RANK; r++) {
                double key = model.weights[KEY + node * RANK + r];
                double query = model.weights[QUERY + node * RANK + r];
                queries[node * RANK + r] = query;
                self += query * key;
                for (int d = 0; d < WIDTH; d++)
                    products[node * POOL + r * WIDTH + d] = key * model.weights[E + node * WIDTH + d];
            }
            for (int d = 0; d < WIDTH; d++) {
                selfContent[node * WIDTH + d] = self * model.weights[E + node * WIDTH + d];
                readout[node * WIDTH + d] = model.weights[Q + node * WIDTH + d];
            }
        }
    }

    static int node(int piece, int square, int orientation) {
        return ((piece & 7) - 1 + 6 * ((piece >>> 3) ^ orientation)) * 64 + (square ^ (orientation * 56));
    }

    public Accumulator newAccumulator() { return new Accumulator(); }

    public com.ohinteractive.seedv6.search.evaluation.SearchEvaluation searchDefinition() {
        return com.ohinteractive.seedv6.search.evaluation.SearchEvaluation.researchCentipawnsStateful(Stack::new);
    }

    private final class Stack implements com.ohinteractive.seedv6.search.evaluation.SearchEvaluation.ResearchCpLifecycle {
        private final Accumulator[] frames;
        private final Accumulator roundingOracle = newAccumulator();
        Stack(int capacity) {
            frames = new Accumulator[capacity];
            for (int i = 0; i < capacity; i++) frames[i] = newAccumulator();
        }
        @Override public void initialize(long[] board, int ply) { frames[ply].rebuild(board); }
        @Override public void child(long[] parent, long[] child, int ply) { frames[ply + 1].update(parent, child, frames[ply]); }
        @Override public void initializeFrom(long[] board, int ply,
                com.ohinteractive.seedv6.search.evaluation.SearchEvaluation.ResearchCpLifecycle source) {
            if (!(source instanceof IncrementalOne.Stack other)) throw new IllegalArgumentException("ONE stack");
            frames[ply].copyFrom(board, other.frames[ply]);
        }
        @Override public double evaluate(long[] board, int ply) {
            double cp = frames[ply].scoreCp(board), magnitude = Math.abs(cp);
            // R056 weights are clipped to .99, board <=32 pieces, search paths <=256 plies.
            // Conservative guard far exceeds observed <=5e-12 cp reassociation error.
            // Restore the reference operation order near an integer rounding discontinuity.
            if (magnitude < 32511 && Math.abs(magnitude - Math.floor(magnitude) - .5) <= 1e-6) {
                roundingOracle.rebuild(board); return roundingOracle.canonicalScoreCp(board);
            }
            return cp;
        }
    }

    /** Payload sizes exclude JVM headers/alignment; benchmark reports measured allocations too. */
    public long immutablePayloadBytes() {
        return 8L * (products.length + queries.length + selfContent.length + readout.length + unary.length + norms.length + 1);
    }

    public final class Accumulator {
        private final double[] pool = new double[2 * POOL];
        private final double[] unarySum = new double[2];
        private final long[] position = new long[4];
        private int count, materialTenths;
        private boolean initialized;

        private IncrementalOne owner() { return IncrementalOne.this; }

        private void verify(long[] board) {
            if (!initialized || board[0] != position[0] || board[1] != position[1]
                    || board[2] != position[2] || board[3] != position[3])
                throw new IllegalStateException("ONE accumulator/board mismatch");
        }

        private void add(int piece, int square, int sign) {
            count += sign;
            int material = (int)Math.round(10 * BrnPair2Features.pieceMaterial(piece));
            materialTenths += sign * ((piece & 8) == 0 ? material : -material);
            for (int orientation = 0; orientation < 2; orientation++) {
                int node = node(piece, square, orientation), offset = node * POOL, base = orientation * POOL;
                unarySum[orientation] += sign * unary[node];
                for (int k = 0; k < POOL; k++) pool[base + k] += sign * products[offset + k];
            }
        }

        public void rebuild(long[] board) {
            initialized = false;
            Arrays.fill(pool, 0); Arrays.fill(unarySum, 0); count = materialTenths = 0;
            for (long bits = board[0] | board[1] | board[2]; bits != 0; bits &= bits - 1) {
                int s = Long.numberOfTrailingZeros(bits);
                add(Board.getSquare(board[0], board[1], board[2], board[3], s), s, 1);
            }
            if (count < 2 || count > 32) throw new IllegalArgumentException("Piece count");
            System.arraycopy(board, 0, position, 0, 4); initialized = true;
        }

        public void copyFrom(long[] board, Accumulator source) {
            if (source == this || source.owner() != owner()) throw new IllegalArgumentException("ONE state ownership");
            source.verify(board);
            System.arraycopy(source.pool, 0, pool, 0, pool.length);
            System.arraycopy(source.unarySum, 0, unarySum, 0, 2);
            System.arraycopy(source.position, 0, position, 0, 4);
            count = source.count; materialTenths = source.materialTenths; initialized = true;
        }

        /** Changed-square comparison handles captures, promotions, castling, en passant and null. */
        public void update(long[] parent, long[] child, Accumulator source) {
            copyFrom(parent, source);
            long changes = (parent[0] ^ child[0]) | (parent[1] ^ child[1])
                    | (parent[2] ^ child[2]) | (parent[3] ^ child[3]);
            for (; changes != 0; changes &= changes - 1) {
                int s = Long.numberOfTrailingZeros(changes);
                int before = Board.getSquare(parent[0], parent[1], parent[2], parent[3], s);
                int after = Board.getSquare(child[0], child[1], child[2], child[3], s);
                if (before != 0) add(before, s, -1);
                if (after != 0) add(after, s, 1);
            }
            if (count < 2 || count > 32) { initialized = false; throw new IllegalArgumentException("Piece count"); }
            System.arraycopy(child, 0, position, 0, 4);
        }

        public double scoreCp(long[] board) {
            verify(board);
            int stm = Board.player((int)board[4]);
            double value = (stm == 0 ? materialTenths : -materialTenths) / 40.0 + bias + unarySum[stm];
            return readout(board, stm, value);
        }

        /** After rebuild only: exactly R056's material/unary and pool summation order. */
        double canonicalScoreCp(long[] board) {
            verify(board); int stm = Board.player((int)board[4]);
            double value = BrnPair2Features.material(board) / 4 + bias;
            for (long bits = board[0] | board[1] | board[2]; bits != 0; bits &= bits - 1) {
                int s = Long.numberOfTrailingZeros(bits);
                value += unary[node(Board.getSquare(board[0], board[1], board[2], board[3], s), s, stm)];
            }
            return readout(board, stm, value);
        }

        private double readout(long[] board, int stm, double value) {
            int base = stm * POOL;
            double norm = norms[count];
            for (long bits = board[0] | board[1] | board[2]; bits != 0; bits &= bits - 1) {
                int s = Long.numberOfTrailingZeros(bits);
                int node = node(Board.getSquare(board[0], board[1], board[2], board[3], s), s, stm);
                int q = node * RANK, output = node * WIDTH;
                for (int d = 0; d < WIDTH; d++) {
                    double a = 0;
                    for (int r = 0; r < RANK; r++) a += queries[q + r] * pool[base + r * WIDTH + d];
                    a = norm * (a - selfContent[output + d]);
                    value += readout[output + d] * (a / (1 + Math.abs(a))) / WIDTH;
                }
            }
            if (!Double.isFinite(value)) throw new ArithmeticException("Nonfinite ONE score");
            return 400 * value;
        }

        public long payloadBytes() { return 8L * (pool.length + unarySum.length + position.length) + 9; }
    }
}
