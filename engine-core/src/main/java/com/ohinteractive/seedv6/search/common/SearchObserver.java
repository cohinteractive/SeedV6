package com.ohinteractive.seedv6.search.common;

/**
 * Synchronous search callbacks invoked on the search thread. Observer exceptions
 * propagate to a direct caller. The managed lifecycle generation-gates callbacks
 * and contains observer failures so they cannot corrupt worker ownership. An
 * observer must not re-enter the same search instance.
 * Root move indices are one-based, and {@code best} means the completed root move
 * strictly improved the prior root best. The finished result is the same immutable
 * object subsequently returned by the search.
 */
public interface SearchObserver {

    public SearchObserver NONE = new SearchObserver() {};
    default void onSearchStarted(int depth, int rootEval, int rootMoveCount) {

    }

    default void onRootMoveStarted(int index, int total, long move) {

    }

    default void onRootMoveFinished(int index, int total, long move, int score, boolean best, long nodes, long elapsedNanos) {

    }

    default void onSearchFinished(SearchResult result, long elapsedNanos) {

    }

    /** Called exactly once for each fully completed iterative-search depth. */
    default void onIterationCompleted(IterationSnapshot snapshot) {

    }

}
