package com.ohinteractive.seedv6.search.exact;

import java.util.Objects;

import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;

/**
 * Worker-owned evaluation, always relative to the board's side to move.
 * Scores must be within +/-ExactSearch.MAX_STATIC_SCORE. Implementations must
 * not mutate boards; initialize/child maintain optional per-ply evaluator state.
 */
@FunctionalInterface
public interface ExactEvaluator {
    int evaluate(long[] board, int ply);

    default void initialize(long[] board) {}

    default void child(long[] parent, long[] child, int parentPly) {}

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
