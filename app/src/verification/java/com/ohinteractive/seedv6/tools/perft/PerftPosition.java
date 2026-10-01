package com.ohinteractive.seedv6.tools.perft;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record PerftPosition(
    String id,
    String name,
    String fen,
    List<PerftExpectation> expectations,
    int defaultDepth
) {

    public PerftPosition {
        id = requireText(id, "id");
        name = requireText(name, "name");
        fen = requireText(fen, "fen");
        expectations = List.copyOf(Objects.requireNonNull(expectations, "expectations"));
        if(defaultDepth <= 0) {
            throw new IllegalArgumentException("defaultDepth must be positive: " + defaultDepth);
        }

        Set<Integer> depths = new HashSet<>();
        boolean hasDefaultExpectation = false;
        for(PerftExpectation expectation : expectations) {
            Objects.requireNonNull(expectation, "expectations must not contain null");
            if(!depths.add(expectation.depth())) {
                throw new IllegalArgumentException(
                    "Duplicate expectation depth " + expectation.depth() + " for position '" + id + "'"
                );
            }
            if(expectation.depth() == defaultDepth) hasDefaultExpectation = true;
        }
        if(!hasDefaultExpectation) {
            throw new IllegalArgumentException(
                "No expectation for default depth " + defaultDepth + " in position '" + id + "'"
            );
        }
    }

    public PerftExpectation expectation(int depth) {
        if(depth <= 0) {
            throw new IllegalArgumentException("depth must be positive: " + depth);
        }
        for(PerftExpectation expectation : expectations) {
            if(expectation.depth() == depth) return expectation;
        }
        throw new IllegalArgumentException(
            "No expectation for depth " + depth + " in position '" + id + "'"
        );
    }

    public PerftExpectation defaultExpectation() {
        return expectation(defaultDepth);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if(value.isBlank()) throw new IllegalArgumentException(fieldName + " must not be blank");
        return value;
    }
}
