package com.ohinteractive.seedv6.rules;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PositionIdentityTest {

    @Test
    void uncapturableEnPassantTargetIsExcludedFromRepetitionIdentity() {
        final long[] withoutEp = Board.fromFen("4k3/8/8/8/4P3/8/8/4K3 b - - 0 1");
        final long[] withEp = Board.fromFen("4k3/8/8/8/4P3/8/8/4K3 b - e3 0 1");

        assertNotEquals(withoutEp[Board.KEY], withEp[Board.KEY]);
        assertEquals(
            PositionIdentity.repetitionKey(withoutEp),
            PositionIdentity.repetitionKey(withEp)
        );
    }

    @Test
    void legalEnPassantCaptureRemainsPartOfRepetitionIdentity() {
        final long[] withoutEp = Board.fromFen("4k3/8/8/8/3pP3/8/8/4K3 b - - 0 1");
        final long[] withEp = Board.fromFen("4k3/8/8/8/3pP3/8/8/4K3 b - e3 0 1");

        assertNotEquals(
            PositionIdentity.repetitionKey(withoutEp),
            PositionIdentity.repetitionKey(withEp)
        );
    }

    @Test
    void illegalPinnedEnPassantCaptureIsExcludedFromRepetitionIdentity() {
        final long[] withoutEp = Board.fromFen("4r1k1/8/8/3pP3/8/8/8/4K3 w - - 0 1");
        final long[] withEp = Board.fromFen("4r1k1/8/8/3pP3/8/8/8/4K3 w - d6 0 1");

        assertEquals(
            PositionIdentity.repetitionKey(withoutEp),
            PositionIdentity.repetitionKey(withEp)
        );
    }

    @Test
    void castlingRightsRemainPartOfRepetitionIdentity() {
        final long[] withRights = Board.fromFen(
            "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"
        );
        final long[] withoutRights = Board.fromFen(
            "r3k2r/8/8/8/8/8/8/R3K2R w - - 0 1"
        );

        assertNotEquals(withRights[Board.KEY], withoutRights[Board.KEY]);
        assertNotEquals(
            PositionIdentity.repetitionKey(withRights),
            PositionIdentity.repetitionKey(withoutRights)
        );
    }

    @Test
    void sideToMoveRemainsPartOfRepetitionIdentity() {
        final long[] white = Board.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1");
        final long[] black = Board.fromFen("4k3/8/8/8/8/8/8/4K3 b - - 0 1");

        assertNotEquals(
            PositionIdentity.repetitionKey(white),
            PositionIdentity.repetitionKey(black)
        );
    }

    @Test
    void piecePlacementRemainsPartOfRepetitionIdentity() {
        final long[] e1 = Board.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1");
        final long[] d1 = Board.fromFen("4k3/8/8/8/8/8/8/3K4 w - - 0 1");

        assertNotEquals(
            PositionIdentity.repetitionKey(e1),
            PositionIdentity.repetitionKey(d1)
        );
    }
}
