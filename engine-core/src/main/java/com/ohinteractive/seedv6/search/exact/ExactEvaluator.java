package com.ohinteractive.seedv6.search.exact;

import java.util.Objects;

import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;

/**
 * Worker-owned evaluation, always relative to the board's side to move.
 * Scores must be within +/-ExactSearch.MAX_STATIC_SCORE. Implementations must
 * not mutate boards; initialize/child maintain optional per-ply evaluator state.
 * With TT enabled, the definition must remain fixed within a request and static
 * values must depend only on the board (ply is an accumulator index).
 */
@FunctionalInterface
public interface ExactEvaluator {
    int evaluate(long[] board, int ply);

    default void initialize(long[] board) {}

    default void child(long[] parent, long[] child, int parentPly) {}

    /** Actual destination domain for admission telemetry: -1 main, 0 qroot, >0 q continuation.
     * Default preserves existing evaluator state and independent reference implementations. */
    default void child(long[] parent, long[] child, int parentPly, int qply) {
        child(parent, child, parentPly);
    }

    /** Admission of an additional same-position search, without an evaluator transition. */
    default void reenter(int ply) {}

    /** Reuses HCE/NNUE/BRN inference and transitions, without their search policies. */
    static ExactEvaluator from(SearchEvaluation definition) {
        SearchEvaluation.State state = Objects.requireNonNull(definition, "definition")
                .newState(ExactSearch.MAX_DEPTH + 1);
        return new ExactEvaluator() {
            @Override public void initialize(long[] board) { state.initialize(board, 0); }
            @Override public void child(long[] parent, long[] child, int parentPly) {
                state.child(parent, child, parentPly);
            }
            @Override public int evaluate(long[] board, int ply) { return state.evaluate(board, ply); }
        };
    }
}
