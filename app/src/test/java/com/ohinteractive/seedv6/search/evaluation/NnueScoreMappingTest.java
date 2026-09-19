package com.ohinteractive.seedv6.search.evaluation;

import java.util.HashSet;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import com.ohinteractive.seedv6.training.selfplay.SelfPlayConfig;
import com.ohinteractive.seedv6.training.validation.ValidationConfig;
import static org.junit.jupiter.api.Assertions.*;

class NnueScoreMappingTest {
    private static final int LIMIT = TranspositionScores.MAX_NORMAL_SCORE;

    @Test void denseDomainHasNoClippingReversalAsymmetryOrUnreachableInteger() {
        var mapping = NnueScoreMapping.V1;
        assertEquals(LIMIT, mapping.scale());
        int previous = -LIMIT;
        for (int i = -200_000; i <= 200_000; i++) {
            double value = i / 200_000.0;
            int score = mapping.map(value);
            assertTrue(score >= previous, "No alpha/beta ordering reversal");
            assertEquals(score, mapping.map(value));
            assertEquals(-score, mapping.map(-value));
            assertFalse(TranspositionScores.isMateScore(score));
            double magnitude = Math.abs(value) * mapping.scale();
            assertTrue(magnitude <= LIMIT, "No clamp is activated anywhere in the valid domain");
            assertTrue(Math.abs(Math.abs(score) - magnitude) <= (magnitude < .5 ? 1 : .500000001));
            previous = score;
        }
        var reached = new HashSet<Integer>();
        for (int i = -LIMIT; i <= LIMIT; i++) {
            assertEquals(i, mapping.map(i / (double) LIMIT));
            reached.add(mapping.map(i / (double) LIMIT));
        }
        assertEquals(2 * LIMIT + 1, reached.size());
    }

    @Test void endpointsSignedZeroTinyValuesHalfTiesAndMateStorageRemainSeparate() {
        var mapping = NnueScoreMapping.V1;
        assertEquals(0, mapping.map(0.0)); assertEquals(0, mapping.map(-0.0));
        for (double value : new double[] {Double.MIN_VALUE, Float.MIN_VALUE, 1e-12, .5 / LIMIT, 1.5 / LIMIT,
                Math.nextDown(1.0), Math.nextDown(1.0f), 1}) {
            int score = mapping.map(value);
            assertTrue(score >= 1 && score <= LIMIT);
            assertEquals(-score, mapping.map(-value));
            for (int ply : new int[] {0, 1, 128, 256}) for (int ordinary : new int[] {score, -score}) {
                assertEquals(ordinary, TranspositionScores.toTableScore(ordinary, ply));
                assertEquals(ordinary, TranspositionScores.fromTableScore(ordinary, ply));
            }
        }
        assertEquals(LIMIT, mapping.map(1)); assertEquals(-LIMIT, mapping.map(-1));
        assertEquals(2, mapping.map(1.5 / LIMIT));
        assertEquals(LIMIT + 1, TranspositionScores.MATE_THRESHOLD);
        for (int ply : new int[] {0, 1, 128, 256}) for (int sign : new int[] {-1, 1}) {
            int mate = sign * (TranspositionScores.MATE_SCORE - ply);
            assertTrue(TranspositionScores.isMateScore(mate));
            assertEquals(sign * TranspositionScores.MATE_SCORE, TranspositionScores.toTableScore(mate, ply));
            assertEquals(mate, TranspositionScores.fromTableScore(sign * TranspositionScores.MATE_SCORE, ply));
        }
        for (double invalid : new double[] {Math.nextUp(1.0), Math.nextDown(-1.0), Double.NaN, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> mapping.map(invalid));
        }
    }

    @Test void customHistoricalMappingsKeepTheirExactSemantics() {
        var legacy = new NnueScoreMapping(1_000_000);
        assertEquals(LIMIT, legacy.map(.04)); assertEquals(-LIMIT, legacy.map(-.04));
        assertEquals(1, legacy.map(Double.MIN_VALUE));
        assertEquals(4000, new NnueScoreMapping(4096).map(4000.0 / 4096));
        assertSame(NnueScoreMapping.V1, SelfPlayConfig.defaults(3, 7).scoreMapping());
        assertSame(NnueScoreMapping.V1, ValidationConfig.defaults(3, 7).scoreMapping());
        assertSame(legacy, SelfPlayConfig.defaults(3, 7, legacy).scoreMapping());
        assertSame(legacy, ValidationConfig.defaults(3, 7, legacy).scoreMapping());
        var network = NnueNetwork.initialized(73);
        var evaluator = new NnueEvaluator(network); var board = Board.startingPosition(); evaluator.evaluate(board);
        var state = SearchEvaluation.incremental(network).newState(1); state.initialize(board, 0);
        assertEquals(NnueScoreMapping.V1.map(evaluator.boundedValue()), state.evaluate(board, 0));
    }

    @Test void fixedNetworkLegacyAndV1HaveLegalResultsPrivateTablesAndUnchangedTerminalMeaning() {
        var network = NnueNetwork.initialized(73);
        var oldDefinition = SearchEvaluation.incremental(network, new NnueScoreMapping(1_000_000));
        var newDefinition = SearchEvaluation.incremental(network);
        var legacy = new AlphaBetaPvsSearch(oldDefinition);
        var canonical = new AlphaBetaPvsSearch(newDefinition);
        var board = Board.startingPosition();
        var coldOld = search(legacy, board); var coldNew = search(canonical, board);
        assertTrue(coldOld.hasMove()); assertTrue(coldNew.hasMove());
        for (var result : new SearchResult[] {coldOld, coldNew}) {
            var game = new com.ohinteractive.seedv6.training.selfplay.HeadlessGame(board, 1024);
            for (long move : result.principalVariation()) game.play(move);
            assertTrue(game.playedPlies() > 0);
        }
        search(legacy, board);
        assertEquals(coldNew, search(new AlphaBetaPvsSearch(newDefinition), board));
        legacy.newGame(); assertEquals(coldOld, search(legacy, board));
        assertThrows(IllegalArgumentException.class, () -> newDefinition.newState(2).initializeFrom(board, 0, oldDefinition.newState(2)));
        String[] terminal = {"7k/6Q1/5K2/8/8/8/8/8 b - - 100 1", "7k/5Q2/6K1/8/8/8/8/8 b - - 0 1",
                "4k3/8/8/8/8/8/8/R3K3 w - - 100 1", "4k3/8/8/8/8/8/8/4K3 w - - 0 1"};
        for (int i = 0; i < terminal.length; i++) {
            int expected = i == 0 ? -TranspositionScores.MATE_SCORE : 0;
            assertEquals(expected, search(legacy, Board.fromFen(terminal[i])).score());
            assertEquals(expected, search(canonical, Board.fromFen(terminal[i])).score());
        }
    }
    private static SearchResult search(AlphaBetaPvsSearch engine, long[] board) {
        var result = engine.search(new SearchRequest(board, 2)); assertTrue(result.completed()); return result;
    }
}
