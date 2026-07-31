package com.ohinteractive.seedv6.tools.perft;

import java.util.Objects;

public record PerftCase(PerftPosition position, int depth, long expectedNodes) {

    public PerftCase {
        position = Objects.requireNonNull(position, "position");
        if(depth <= 0) {
            throw new IllegalArgumentException("depth must be positive: " + depth);
        }
        if(expectedNodes < 0L) {
            throw new IllegalArgumentException("expectedNodes must be non-negative: " + expectedNodes);
        }

        long knownNodes = position.expectation(depth).expectedNodes();
        if(expectedNodes != knownNodes) {
            throw new IllegalArgumentException(
                "Expected nodes " + expectedNodes + " do not match position '" + position.id() +
                "' at depth " + depth + ": " + knownNodes
            );
        }
    }
}
