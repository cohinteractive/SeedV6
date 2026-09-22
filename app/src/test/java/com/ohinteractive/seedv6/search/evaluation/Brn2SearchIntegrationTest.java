package com.ohinteractive.seedv6.search.evaluation;

import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import static org.junit.jupiter.api.Assertions.*;

class Brn2SearchIntegrationTest {
    @Test void boundedMappingIsMonotoneOddDeterministicAndKeepsMateBandReserved() {
        int previous = Integer.MIN_VALUE;
        for (int i = -10000; i <= 10000; i++) {
            double value = i / 10000.0;
            int score = BrnScoreMapping.map(value);
            assertTrue(score >= previous); previous = score;
            assertEquals(-score, BrnScoreMapping.map(-value));
            assertEquals(score, BrnScoreMapping.map(value));
            assertEquals(NnueScoreMapping.V1.map(value), score, "Established NNUE mapping is unchanged");
            assertTrue(Math.abs(score) <= TranspositionScores.MAX_NORMAL_SCORE);
        }
        assertEquals(0, BrnScoreMapping.map(-0.0)); assertEquals(0, BrnScoreMapping.map(0));
        assertEquals(1, BrnScoreMapping.map(Double.MIN_VALUE));
        assertEquals(-1, BrnScoreMapping.map(-Double.MIN_VALUE));
        assertEquals(32511, BrnScoreMapping.map(1)); assertEquals(-32511, BrnScoreMapping.map(-1));
        for (double bad : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.MAX_VALUE, -Double.MAX_VALUE, Math.nextUp(1.0), Math.nextDown(-1.0)})
            assertThrows(IllegalArgumentException.class, () -> BrnScoreMapping.map(bad));
    }

    @Test void evaluatorUsesRawSideToMoveValueWithPrivateScratchAndPinnedModel() {
        double[] weights = new double[Brn2Model.PARAMETER_COUNT];
        weights[Brn2Model.OUTPUT_BIAS] = 0.25;
        Brn2Model model = new Brn2Model(weights);
        SearchEvaluation definition = SearchEvaluation.brn2(model);
        var a = definition.newState(4); var b = definition.newState(4);
        var white = Board.startingPosition();
        var black = Board.fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 0 1");
        // Production must consume prepared incremental state, not silently re-encode every evaluation.
        assertThrows(IllegalStateException.class, () -> a.evaluate(white, 0));
        a.initialize(white, 0); b.initialize(black, 0);
        int expected = BrnScoreMapping.map(StrictMath.tanh(0.25));
        // The model itself is side-to-move; no extra colour flip belongs at this boundary.
        assertEquals(expected, a.evaluate(white, 0)); assertEquals(expected, b.evaluate(black, 0));
        a.child(white, black, 0); b.initializeFrom(black, 1, a);
        assertEquals(expected, b.evaluate(black, 1));
        weights[Brn2Model.OUTPUT_BIAS] = -10; assertEquals(expected, a.evaluate(white, 0));
        assertFalse(definition.usesAspiration());
        assertThrows(IllegalArgumentException.class, () -> a.initializeFrom(white, 0, SearchEvaluation.handcrafted().newState(4)));
        assertThrows(IllegalArgumentException.class, () -> a.initializeFrom(white, 0, SearchEvaluation.brn2(model).newState(4)));
    }
}
