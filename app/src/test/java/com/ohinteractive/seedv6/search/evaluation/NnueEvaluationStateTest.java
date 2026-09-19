package com.ohinteractive.seedv6.search.evaluation;

import java.lang.management.ManagementFactory;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class NnueEvaluationStateTest {
    private static final NnueNetwork NETWORK = NnueNetwork.initialized(20260918);
    private static final NnueScoreMapping MAPPING = new NnueScoreMapping(1_000_000);

    @Test
    void mappingIsMonotonicSignPreservingSymmetricAndCannotReachMateBand() {
        for (double scale : new double[] {0.001, 1, 1000, 1_000_000, Double.MAX_VALUE}) {
            NnueScoreMapping mapper = new NnueScoreMapping(scale);
            int previous = -TranspositionScores.MAX_NORMAL_SCORE;
            for (int i = -10_000; i <= 10_000; i++) {
                double value = i / 10_000.0;
                int score = mapper.map(value);
                assertTrue(score >= previous);
                assertEquals(Integer.signum(i), Integer.signum(score));
                assertEquals(-score, mapper.map(-value));
                assertFalse(TranspositionScores.isMateScore(score));
                previous = score;
            }
        }
        assertEquals(2, new NnueScoreMapping(10).map(0.15));
        assertEquals(-2, new NnueScoreMapping(10).map(-0.15));
        assertEquals(1, MAPPING.map(Double.MIN_VALUE));
        for (double invalid : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new NnueScoreMapping(invalid));
        }
        for (double invalid : new double[] {1.1, -1.1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> MAPPING.map(invalid));
        }
    }

    @Test
    void separateWorkersAndQsearchStacksPreserveParentsSiblingsAndAbsolutePly256() {
        SearchEvaluation definition = SearchEvaluation.incremental(NETWORK, MAPPING);
        SearchEvaluation.State main = definition.newState(257);
        SearchEvaluation.State qsearch = definition.newState(257);
        SearchEvaluation.State otherWorker = definition.newState(257);
        SearchEvaluation.State oracle = SearchEvaluation.fullRecompute(NETWORK, MAPPING).newState(257);
        long[] board = Board.startingPosition();
        long[] moves = new long[256];
        int count = Gen.genAll(board[0], board[1], board[2], board[3], (int) board[4], board[5],
                true, moves, new long[6]);
        main.initialize(board, 0);
        int root = main.evaluate(board, 0);
        assertEquals(oracle.evaluate(board, 0), root);
        for (int i = count - 1; i >= 0; i--) {
            long[] child = new long[6];
            Board.makeMoveInto(board[0], board[1], board[2], board[3], (int) board[4], board[5], moves[i], child);
            main.child(board, child, 0);
            qsearch.initializeFrom(child, 1, main);
            otherWorker.initialize(child, 256);
            assertEquals(oracle.evaluate(child, 1), main.evaluate(child, 1));
            assertEquals(main.evaluate(child, 1), qsearch.evaluate(child, 1));
            assertEquals(main.evaluate(child, 1), otherWorker.evaluate(child, 256));
            assertEquals(root, main.evaluate(board, 0));
            main.initialize(board, 255);
            main.child(board, child, 255);
            qsearch.initializeFrom(child, 256, main);
            assertEquals(oracle.evaluate(child, 256), qsearch.evaluate(child, 256));
        }
        assertThrows(IllegalArgumentException.class, () -> main.initializeFrom(board, 0, main));
        SearchEvaluation.State different = SearchEvaluation.incremental(NnueNetwork.initialized(9), MAPPING).newState(257);
        different.initialize(board, 0);
        assertThrows(IllegalArgumentException.class, () -> main.initializeFrom(board, 0, different));
        main.initialize(board, 0);
        assertEquals(root, main.evaluate(board, 0));
    }

    @Test
    void incrementalTransitionInferenceAndQsearchHandoffAllocateNoHeapAfterWarmup() {
        var bean = ManagementFactory.getThreadMXBean();
        assumeTrue(bean instanceof com.sun.management.ThreadMXBean);
        var allocations = (com.sun.management.ThreadMXBean) bean;
        assumeTrue(allocations.isThreadAllocatedMemorySupported());
        allocations.setThreadAllocatedMemoryEnabled(true);
        SearchEvaluation definition = SearchEvaluation.incremental(NETWORK, MAPPING);
        SearchEvaluation.State main = definition.newState(257);
        SearchEvaluation.State qsearch = definition.newState(257);
        long[] board = Board.startingPosition();
        long[] moves = new long[256];
        Gen.genAll(board[0], board[1], board[2], board[3], (int) board[4], board[5], true, moves, new long[6]);
        long[] child = new long[6];
        Board.makeMoveInto(board[0], board[1], board[2], board[3], (int) board[4], board[5], moves[0], child);
        main.initialize(board, 0);
        int checksum = exercise(main, qsearch, board, child, 20_000);
        long thread = Thread.currentThread().threadId();
        long before = allocations.getThreadAllocatedBytes(thread);
        checksum += exercise(main, qsearch, board, child, 10_000);
        long allocated = allocations.getThreadAllocatedBytes(thread) - before;
        assertNotEquals(0, checksum);
        assertEquals(0L, allocated, "Prepared transitions, inference and handoff must not allocate.");
    }

    private static int exercise(SearchEvaluation.State main, SearchEvaluation.State qsearch,
                                long[] board, long[] child, int iterations) {
        int sum = 0;
        for (int i = 0; i < iterations; i++) {
            main.child(board, child, 0);
            sum += main.evaluate(child, 1);
            qsearch.initializeFrom(child, 1, main);
            sum += qsearch.evaluate(child, 1);
        }
        return sum;
    }
}
