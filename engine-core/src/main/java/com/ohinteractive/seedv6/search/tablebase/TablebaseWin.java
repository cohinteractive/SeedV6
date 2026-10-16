package com.ohinteractive.seedv6.search.tablebase;

/** A completed game-outcome decision. DTZ is the table's rounded progress metric, not a mate distance or searched depth. */
public record TablebaseWin(long bestMove, int distanceToZero) {
    public TablebaseWin {
        if(bestMove == 0 || distanceToZero < 1 || distanceToZero > 98)
            throw new IllegalArgumentException("Invalid tablebase winning move.");
    }
}
