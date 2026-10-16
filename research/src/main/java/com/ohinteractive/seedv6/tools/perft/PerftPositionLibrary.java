package com.ohinteractive.seedv6.tools.perft;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public interface PerftPositionLibrary {

    String id();

    String name();

    List<PerftPosition> positions();

    default PerftSuite all() {
        List<PerftPosition> libraryPositions = validatedPositions();
        return createSuite(id() + ":all", name(), libraryPositions);
    }

    default PerftSuite select(int positionNumber) {
        List<PerftPosition> libraryPositions = validatedPositions();
        PerftPosition position = positionAt(libraryPositions, positionNumber);
        return singlePositionSuite(position);
    }

    default PerftSuite select(String positionId) {
        if(positionId == null || positionId.isBlank()) {
            throw new IllegalArgumentException("positionId must not be blank");
        }
        List<PerftPosition> libraryPositions = validatedPositions();
        for(PerftPosition position : libraryPositions) {
            if(position.id().equals(positionId)) return singlePositionSuite(position);
        }
        throw new IllegalArgumentException(
            "Unknown position ID '" + positionId + "' in library '" + id() + "'"
        );
    }

    default PerftSuite range(int firstPosition, int lastPosition) {
        List<PerftPosition> libraryPositions = validatedPositions();
        if(firstPosition < 1 || lastPosition > libraryPositions.size() || firstPosition > lastPosition) {
            throw new IllegalArgumentException(
                "Invalid one-based position range " + firstPosition + " to " + lastPosition +
                " for library '" + id() + "' with " + libraryPositions.size() + " positions"
            );
        }
        PerftPosition first = libraryPositions.get(firstPosition - 1);
        PerftPosition last = libraryPositions.get(lastPosition - 1);
        return createSuite(
            id() + ":" + first.id() + ".." + last.id(),
            name() + " (positions " + firstPosition + "-" + lastPosition + ")",
            libraryPositions.subList(firstPosition - 1, lastPosition)
        );
    }

    private PerftSuite singlePositionSuite(PerftPosition position) {
        return createSuite(
            id() + ":" + position.id(),
            position.name(),
            List.of(position)
        );
    }

    private PerftSuite createSuite(
        String suiteId,
        String suiteName,
        List<PerftPosition> selectedPositions
    ) {
        List<PerftCase> cases = new ArrayList<>(selectedPositions.size());
        for(PerftPosition position : selectedPositions) {
            PerftExpectation expectation = position.defaultExpectation();
            cases.add(new PerftCase(position, expectation.depth(), expectation.expectedNodes()));
        }
        return new PerftSuite(suiteId, suiteName, cases);
    }

    private PerftPosition positionAt(List<PerftPosition> libraryPositions, int positionNumber) {
        if(positionNumber < 1 || positionNumber > libraryPositions.size()) {
            throw new IllegalArgumentException(
                "Invalid one-based position number " + positionNumber + " for library '" + id() +
                "' with " + libraryPositions.size() + " positions"
            );
        }
        return libraryPositions.get(positionNumber - 1);
    }

    private List<PerftPosition> validatedPositions() {
        List<PerftPosition> libraryPositions = Objects.requireNonNull(positions(), "positions()");
        if(libraryPositions.isEmpty()) {
            throw new IllegalStateException("Library '" + id() + "' contains no positions");
        }

        Set<String> positionIds = new HashSet<>();
        for(PerftPosition position : libraryPositions) {
            Objects.requireNonNull(position, "positions() must not contain null");
            if(!positionIds.add(position.id())) {
                throw new IllegalStateException(
                    "Duplicate position ID '" + position.id() + "' in library '" + id() + "'"
                );
            }
        }
        return libraryPositions;
    }
}
