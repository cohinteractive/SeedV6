package com.ohinteractive.seedv6.search.common;

/**
 * Synchronous search callbacks invoked on the search thread. Observer exceptions
 * propagate to the caller. An observer must not re-enter the same search instance.
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

}
