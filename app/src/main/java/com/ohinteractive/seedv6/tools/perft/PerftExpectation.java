package com.ohinteractive.seedv6.tools.perft;

public record PerftExpectation(int depth, long expectedNodes) {

    public PerftExpectation {
        if(depth <= 0) {
            throw new IllegalArgumentException("depth must be positive: " + depth);
        }
        if(expectedNodes < 0L) {
            throw new IllegalArgumentException("expectedNodes must be non-negative: " + expectedNodes);
        }
    }
}
