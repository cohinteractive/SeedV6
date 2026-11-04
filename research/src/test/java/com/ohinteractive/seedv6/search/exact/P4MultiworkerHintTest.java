package com.ohinteractive.seedv6.search.exact;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.ohinteractive.seedv6.core.Board;

class P4MultiworkerHintTest {
    @Test void provenanceFollowsActualReplacementAndSelectiveDelivery() {
        long[] moves = ExhaustiveOracle.legalMoves(Board.startingPosition());
        for (var mode : P4MultiworkerHintStudy.Delivery.values()) {
            var table = new P4MultiworkerHintStudy.HintTable(2, 1, mode);
            table.advanceGeneration(); table.bind(0);
            assertTrue(table.saveHint(1, 4, moves[0]));
            table.bind(1);
            assertEquals(mode == P4MultiworkerHintStudy.Delivery.ALL ? moves[0] : 0, table.probeHint(1));
            assertFalse(table.saveHint(1, 2, moves[1]));
            assertEquals(0, table.producers.get(1L).worker());
            assertTrue(table.saveHint(1, 4, moves[1]));
            assertEquals(moves[1], table.probeHint(1));
            assertEquals(1, table.otherWorker); assertEquals(1, table.sameWorker);
            assertEquals(1, table.cross[0][1]); assertEquals(1, table.cross[1][1]);
            assertEquals(0, table.probeHint(2));
        }
    }
    @Test void unboundOrUnexpectedScoreProducerFailsInsteadOfInventingProvenance() {
        var table = new P4MultiworkerHintStudy.HintTable(4, 1, P4MultiworkerHintStudy.Delivery.ALL);
        assertThrows(AssertionError.class, () -> table.probeHint(1));
        assertThrows(AssertionError.class, () -> table.save(1, 1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> table.bind(4));
    }
}
