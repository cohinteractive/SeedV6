package com.ohinteractive.seedv6.search.tt;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TTableTest {
    @Test void emptyZeroKeyAndUntouchedMissScratch() {
        var table = new TTable(1);
        var entry = new TTable.TEntry(); entry.data = 123; entry.hashMove = 456;
        assertFalse(table.probe(0, entry));
        assertEquals(123, entry.data); assertEquals(456, entry.hashMove);
        table.save(0, 0, TTable.TYPE_EXACT, 0, 17);
        assertTrue(table.probe(0, entry)); assertEquals(17, entry.hashMove);
        long data = entry.data;
        assertFalse(table.probe(1L << 40, entry)); // same low index, different full key
        assertEquals(data, entry.data); assertEquals(17, entry.hashMove);
    }

    @Test void packedSearchFieldsAndFullMoveRoundTrip() {
        for(int depth : new int[] {0, 1, 127, 255})
            for(int type : new int[] {TTable.TYPE_EXACT, TTable.TYPE_LOWER, TTable.TYPE_UPPER})
                for(int score : new int[] {Integer.MIN_VALUE, -32768, -32511, 0, 32511, 32768, Integer.MAX_VALUE}) {
                    var table = new TTable(1); table.advanceGeneration();
                    long move = 0xfedcba9876543210L;
                    table.save(91, depth, type, score, move);
                    var entry = new TTable.TEntry(); assertTrue(table.probe(91, entry));
                    assertEquals(depth, entry.data & 255);
                    assertEquals(type, (entry.data >>> 8) & 3);
                    assertEquals(1, (entry.data >>> 10) & 255);
                    assertEquals(score, (int) (entry.data >> 32)); assertEquals(move, entry.hashMove);
                    assertNotEquals(0, entry.data & (1L << 18));
                }
    }

    @Test void clearInvalidatesDataIncludingZeroKey() {
        var table = new TTable(1); var entry = new TTable.TEntry();
        table.save(0, 3, TTable.TYPE_EXACT, 8, 99); table.clear();
        assertFalse(table.probe(0, entry));
        table.save(0, 0, TTable.TYPE_UPPER, -7, 1); assertTrue(table.probe(0, entry));
        assertEquals(-7, (int) (entry.data >> 32));
    }

    @Test void lowEightBitGenerationWrapClearsAncientEvidence() {
        var table = new TTable(1); var entry = new TTable.TEntry();
        table.save(7, 4, TTable.TYPE_EXACT, 9, 3);
        for(int i = 0; i < 255; i++) table.advanceGeneration();
        assertTrue(table.probe(7, entry)); assertEquals(0, (entry.data >>> 10) & 255);
        table.save(8, 1, TTable.TYPE_EXACT, 4, 2);
        assertTrue(table.probe(8, entry)); assertEquals(255, (entry.data >>> 10) & 255);
        table.advanceGeneration(); assertFalse(table.probe(7, entry)); assertFalse(table.probe(8, entry));
        table.save(7, 1, TTable.TYPE_EXACT, 5, 2);
        assertTrue(table.probe(7, entry)); assertEquals(0, (entry.data >>> 10) & 255);
    }

    @Test void separateEvalModeRetainsRawScoreAndCallerManagedValidity() {
        var search = new TTable(1); var eval = new TTable(1); var entry = new TTable.TEntry();
        search.save(7, 2, TTable.TYPE_EXACT, 11, 77);
        eval.save(7, 0, TTable.TYPE_EVAL, -1234, 0);
        assertTrue(eval.probeEval(7, entry)); assertEquals(-1234, entry.data);
        entry.hashMove = 19; assertFalse(eval.probeEval(8, entry));
        assertEquals(-1234, entry.data); assertEquals(19, entry.hashMove);
        eval.clear(); assertTrue(eval.probeEval(7, entry)); assertEquals(0, entry.data);
        assertTrue(search.probe(7, entry)); assertEquals(11, (int) (entry.data >> 32));
    }

    @Test void mechanicalProbeDoesNotApplySearchDepthGenerationBoundOrMatePolicy() {
        var table = new TTable(1); var entry = new TTable.TEntry();
        table.save(9, 200, TTable.TYPE_LOWER, 32766, Long.MAX_VALUE);
        table.advanceGeneration();
        assertTrue(table.probe(9, entry)); assertEquals(200, entry.data & 255);
        assertEquals(0, (entry.data >>> 10) & 255); assertEquals(32766, (int) (entry.data >> 32));
        assertEquals(Long.MAX_VALUE, entry.hashMove);
    }

    @Test void acceptedReplacementBehaviorIsPreservedEvenAcrossRequestsForSameKey() {
        var table = new TTable(1); var entry = new TTable.TEntry();
        table.save(1, 4, TTable.TYPE_EXACT, 10, 20); table.advanceGeneration();
        table.save(1, 4, TTable.TYPE_EXACT, 99, 22);
        assertTrue(table.probe(1, entry)); assertEquals(10, (int) (entry.data >> 32));
        // Old same-key evidence may survive mechanically; Search must reject its score.
        assertEquals(0, (entry.data >>> 10) & 255);
        table.save(1 + (1L << 40), 1, TTable.TYPE_UPPER, 30, 40);
        assertFalse(table.probe(1, entry)); assertTrue(table.probe(1 + (1L << 40), entry));
    }
}
