package com.ohinteractive.seedv6.core.nnue;

import com.ohinteractive.seedv6.core.Board;

import java.util.Objects;

/**
 * Full-recomputation and prepared-accumulator inference. One caller/worker owns each
 * mutable evaluator; it is not thread-safe. The immutable network may be shared.
 * Successful evaluation allocates nothing and never writes to the supplied board.
 * Results and activated layer values remain available until the next evaluation attempt.
 */
public final class NnueEvaluator {

    private final NnueNetwork network;
    private final boolean scalarOracle;
    private final NnueAccumulator rebuiltAccumulator;
    private final float[] input = new float[NnueNetwork.CONCATENATED_SIZE];
    private final float[] hidden = new float[NnueNetwork.HIDDEN_SIZE];
    private boolean evaluated;
    private float raw;
    private double boundedValue;
    private int player;

    public NnueEvaluator(NnueNetwork network) {
        this(network, false);
    }

    /** Accepted ascending scalar kernel, retained for independent runtime comparisons. */
    public static NnueEvaluator scalarOracle(NnueNetwork network) {
        return new NnueEvaluator(network, true);
    }

    private NnueEvaluator(NnueNetwork network, boolean scalarOracle) {
        this.network = Objects.requireNonNull(network, "network");
        this.scalarOracle = scalarOracle;
        rebuiltAccumulator = new NnueAccumulator(network);
    }

    /**
     * Rebuilds both perspectives, including a feature for every piece (both kings included).
     * Requires at least Board.MAX_BITBOARDS longs and exactly one king per colour;
     * rejects undefined piece encodings but does not validate chess legality.
     * Returns the raw side-to-move scalar, with no centipawn or mate-score interpretation.
     */
    public float evaluate(long[] board) {
        evaluated = false;
        rebuiltAccumulator.rebuild(board);
        return evaluate(board, rebuiltAccumulator);
    }

    /**
     * Infers from prepared raw state with no transformer rebuild or allocation. Requires
     * the same network instance and matching placement. Uses THIS board's side to move;
     * status/key may differ from the board used to prepare the accumulator. State remains
     * untouched, and result/activation inspection remains valid if the caller reuses it.
     */
    public float evaluate(long[] board, NnueAccumulator accumulator) {
        evaluated = false;
        Objects.requireNonNull(accumulator, "accumulator").requireCompatible(board, network);
        player = Board.player((int) board[Board.STATUS]);
        accumulator.writeInput(player, input);
        raw = scalarOracle ? network.forwardScalar(input, hidden) : network.forward(input, hidden);
        boundedValue = StrictMath.tanh(raw);
        evaluated = true;
        return raw;
    }

    public float raw() {
        requireResult();
        return raw;
    }

    /** tanh(raw), in [-1, +1] for finite inference; no fixed search-score conversion. */
    public double boundedValue() {
        requireResult();
        return boundedValue;
    }

    /** Scalar inspection of a clipped perspective accumulator, without exposing scratch arrays. */
    public float accumulator(int perspective, int unit) {
        requireResult();
        NnueFeatureSchema.requireColour(perspective);
        Objects.checkIndex(unit, NnueNetwork.ACCUMULATOR_SIZE);
        return input[(perspective == player ? 0 : NnueNetwork.ACCUMULATOR_SIZE) + unit];
    }

    public float hidden(int unit) {
        requireResult();
        return hidden[unit];
    }

    private void requireResult() {
        if (!evaluated) {
            throw new IllegalStateException("No successful NNUE evaluation is available.");
        }
    }
}
