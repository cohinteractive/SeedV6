package com.ohinteractive.seedv6.search.common;

public record SearchResult(
    long bestMove,
    boolean hasMove,
    int score,
    int depth,
    long nodes,
    int legalRootMoves,
    boolean completed
) {

    public SearchResult {
        if(depth < 1) {
            throw new IllegalArgumentException("Search result depth must be at least 1: " + depth);
        }
        if(legalRootMoves < 0) {
            throw new IllegalArgumentException("Legal root move count must not be negative: " + legalRootMoves);
        }
        if(!hasMove && bestMove != 0L) {
            throw new IllegalArgumentException("A result without a move must use bestMove 0.");
        }
    }

}
