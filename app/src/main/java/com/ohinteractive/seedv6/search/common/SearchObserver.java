package com.ohinteractive.seedv6.search.common;

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
