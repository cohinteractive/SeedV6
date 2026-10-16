package com.ohinteractive.seedv6.tools.perft;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record PerftSuite(String id, String name, List<PerftCase> cases) {

    public PerftSuite {
        id = requireText(id, "id");
        name = requireText(name, "name");
        cases = List.copyOf(Objects.requireNonNull(cases, "cases"));
        if(cases.isEmpty()) throw new IllegalArgumentException("cases must not be empty");

        Set<String> selections = new HashSet<>();
        for(PerftCase perftCase : cases) {
            Objects.requireNonNull(perftCase, "cases must not contain null");
            String selection = perftCase.position().id() + "\u0000" + perftCase.depth();
            if(!selections.add(selection)) {
                throw new IllegalArgumentException(
                    "Duplicate perft case for position '" + perftCase.position().id() +
                    "' at depth " + perftCase.depth()
                );
            }
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if(value.isBlank()) throw new IllegalArgumentException(fieldName + " must not be blank");
        return value;
    }
}
