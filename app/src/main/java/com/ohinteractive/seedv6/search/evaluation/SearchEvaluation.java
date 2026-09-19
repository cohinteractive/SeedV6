package com.ohinteractive.seedv6.search.evaluation;

import java.util.Objects;

import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.nnue.NnueAccumulator;
import com.ohinteractive.seedv6.core.nnue.NnueEvaluator;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.search.alphabeta.SelectiveSearchPolicy;

/**
 * Immutable search-evaluator definition. Normal application construction uses handcrafted().
 * NNUE requires an explicit immutable network; V1 mapping is the default, with research overrides.
 * No runtime replacement exists.
 * Definitions may be shared, but each board stack must own a distinct State.
 */
public final class SearchEvaluation {
    private static final SearchEvaluation HANDCRAFTED = new SearchEvaluation(null, null, false);
    private final NnueNetwork network;
    private final NnueScoreMapping mapping;
    private final boolean incremental;
    private final boolean scalarOracle;
    private final boolean isolation;

    private SearchEvaluation(NnueNetwork network, NnueScoreMapping mapping, boolean incremental) {
        this(network, mapping, incremental, false);
    }

    private SearchEvaluation(NnueNetwork network, NnueScoreMapping mapping, boolean incremental, boolean scalarOracle) {
        this(network, mapping, incremental, scalarOracle, false);
    }

    private SearchEvaluation(NnueNetwork network, NnueScoreMapping mapping, boolean incremental,
                             boolean scalarOracle, boolean isolation) {
        this.network = network;
        this.mapping = mapping;
        this.incremental = incremental;
        this.scalarOracle = scalarOracle;
        this.isolation = isolation;
    }

    public static SearchEvaluation handcrafted() { return HANDCRAFTED; }

    /** Research-only policy equality: handcrafted evaluation, NNUE-safe selectivity and full windows. */
    public static SearchEvaluation handcraftedIsolation() {
        return new SearchEvaluation(null, null, false, false, true);
    }

    public static SearchEvaluation incremental(NnueNetwork network) {
        return incremental(network, NnueScoreMapping.V1);
    }

    public static SearchEvaluation incremental(NnueNetwork network, NnueScoreMapping mapping) {
        return new SearchEvaluation(Objects.requireNonNull(network, "network"),
                Objects.requireNonNull(mapping, "mapping"), true);
    }

    /** Original scalar float inference with the same incremental lifecycle and private TT construction. */
    public static SearchEvaluation incrementalScalarOracle(NnueNetwork network, NnueScoreMapping mapping) {
        return new SearchEvaluation(Objects.requireNonNull(network, "network"),
                Objects.requireNonNull(mapping, "mapping"), true, true);
    }

    /** Independent integration oracle: full transformer rebuild at every evaluation. */
    public static SearchEvaluation fullRecompute(NnueNetwork network, NnueScoreMapping mapping) {
        return new SearchEvaluation(Objects.requireNonNull(network, "network"),
                Objects.requireNonNull(mapping, "mapping"), false);
    }

    public SelectiveSearchPolicy selectiveSearchPolicy() {
        return network == null && !isolation ? SelectiveSearchPolicy.production()
                : SelectiveSearchPolicy.only(SelectiveSearchPolicy.Heuristic.MATE_DISTANCE);
    }

    public boolean usesAspiration() { return network == null && !isolation; }

    public State newState(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("State capacity must be positive.");
        if (network == null) return new HandcraftedState();
        return incremental ? new IncrementalState(this, capacity) : new RecomputedState(this);
    }

    /**
     * Worker-confined stack companion. All successful operations allocate nothing.
     * initializeFrom copies a main-search leaf into separate qsearch storage at the same
     * absolute ply. Parent states are never mutated by child preparation or inference.
     */
    public abstract static sealed class State {
        public abstract void initialize(long[] board, int ply);
        public abstract void child(long[] parent, long[] child, int parentPly);
        public abstract void initializeFrom(long[] board, int ply, State source);
        public abstract int evaluate(long[] board, int ply);
    }

    private static final class HandcraftedState extends State {
        @Override public void initialize(long[] board, int ply) {}
        @Override public void child(long[] parent, long[] child, int parentPly) {}
        @Override public void initializeFrom(long[] board, int ply, State source) {
            if (!(source instanceof HandcraftedState)) throw new IllegalArgumentException("Evaluator mismatch.");
        }
        @Override public int evaluate(long[] board, int ply) { return Eval.evaluate(board); }
    }

    private static final class RecomputedState extends State {
        private final SearchEvaluation definition;
        private final NnueEvaluator inference;
        private RecomputedState(SearchEvaluation definition) {
            this.definition = definition;
            inference = NnueEvaluator.scalarOracle(definition.network);
        }
        @Override public void initialize(long[] board, int ply) {}
        @Override public void child(long[] parent, long[] child, int parentPly) {}
        @Override public void initializeFrom(long[] board, int ply, State source) {
            if (!(source instanceof RecomputedState other) || definition != other.definition) {
                throw new IllegalArgumentException("Evaluator mismatch.");
            }
        }
        @Override public int evaluate(long[] board, int ply) {
            inference.evaluate(board);
            return definition.mapping.map(inference.boundedValue());
        }
    }

    /**
     * Each 257-slot stack holds 257 accumulator states plus NnueEvaluator's one reusable
     * rebuild scratch accumulator. Main + qsearch therefore own 516 states per worker:
     * 264,192 bytes of raw float payload, plus 1,280 bytes of input/hidden float scratch.
     * Array/object headers, placement metadata and references are JVM-dependent overhead.
     * RootParallelSearch retains its existing extra coordinator/single-thread context.
     */
    private static final class IncrementalState extends State {
        private final SearchEvaluation definition;
        private final NnueAccumulator[] accumulators;
        private final NnueEvaluator inference;
        private IncrementalState(SearchEvaluation definition, int capacity) {
            this.definition = definition;
            inference = definition.scalarOracle ? NnueEvaluator.scalarOracle(definition.network)
                    : new NnueEvaluator(definition.network);
            accumulators = new NnueAccumulator[capacity];
            for (int ply = 0; ply < capacity; ply++) {
                accumulators[ply] = new NnueAccumulator(definition.network);
            }
        }
        @Override public void initialize(long[] board, int ply) { accumulators[ply].rebuild(board); }
        @Override public void child(long[] parent, long[] child, int parentPly) {
            accumulators[parentPly + 1].update(parent, child, accumulators[parentPly]);
        }
        @Override public void initializeFrom(long[] board, int ply, State source) {
            if (!(source instanceof IncrementalState other) || definition != other.definition) {
                throw new IllegalArgumentException("Evaluator mismatch.");
            }
            // A placement-preserving Workstream B transition copies all raw bits exactly.
            accumulators[ply].update(board, board, other.accumulators[ply]);
        }
        @Override public int evaluate(long[] board, int ply) {
            inference.evaluate(board, accumulators[ply]);
            return definition.mapping.map(inference.boundedValue());
        }
    }
}
