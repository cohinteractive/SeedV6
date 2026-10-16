package com.ohinteractive.seedv6.core.nnue;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.iterative.IterativeDeepeningSearch;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.tools.eval.EvaluationCorpus;
import com.ohinteractive.seedv6.tools.search.SearchBenchmark;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.*;

class NnueExactInferenceTest {
    @Test
    void scalarAndOptimizedPrivateSearchesAgreeIncludingQsearchNodesAndPv() {
        NnueNetwork network = NnueNetwork.initialized(73);
        NnueScoreMapping mapping = new NnueScoreMapping(1_000_000);
        for (var position : SearchBenchmark.corpus()) {
            long[] board = Board.fromFen(position.fen());
            SearchRequest request = new SearchRequest(board, GameHistory.initial(board), 3,
                    SearchObserver.NONE, SearchControl.unlimited(), true);
            try (var scalar = new IterativeDeepeningSearch(new AlphaBetaPvsSearch(
                    SearchEvaluation.incrementalScalarOracle(network, mapping), 1 << 18));
                 var optimized = new IterativeDeepeningSearch(new AlphaBetaPvsSearch(
                         SearchEvaluation.incremental(network, mapping), 1 << 18))) {
                assertEquals(scalar.search(request), optimized.search(request), position.name());
            }
        }
    }

    @Test
    void everyLegalCorpusActivationMatchesTheUnmodifiedScalarOracleBitForBit() {
        NnueNetwork network = NnueNetwork.initialized(73);
        NnueEvaluator evaluator = new NnueEvaluator(network);
        Oracle oracle = new Oracle(network);
        float[] input = new float[128];
        NnueScoreMapping mapping = new NnueScoreMapping(1_000_000);
        int samples = 0;
        for (var entry : EvaluationCorpus.entries()) {
            long[] board = entry.board();
            long[] before = board.clone();
            evaluator.evaluate(board);
            int player = Board.player((int) board[Board.STATUS]);
            for (int i = 0; i < 128; i++) input[i] = evaluator.accumulator(i < 64 ? player : player ^ 1, i & 63);
            float expected = oracle.forward(input);
            bits(expected, evaluator.raw());
            for (int i = 0; i < 32; i++) bits(oracle.hidden[i], evaluator.hidden(i));
            assertEquals(Double.doubleToLongBits(StrictMath.tanh(expected)),
                    Double.doubleToLongBits(evaluator.boundedValue()));
            assertEquals(mapping.map(StrictMath.tanh(expected)), mapping.map(evaluator.boundedValue()));
            assertArrayEquals(before, board);
            samples++;
        }
        assertEquals(3182, samples);
        System.out.println("exactInference corpus=OPT5-all samples=" + samples
                + " rawHiddenBoundedMappedMismatches=0");
    }

    @Test
    void cancellationRoundingSubnormalsAndExtremeFiniteParametersRetainScalarSemantics() {
        SplittableRandom random = new SplittableRandom(9017);
        float[] edge = {0.0f, -0.0f, Float.MIN_VALUE, -Float.MIN_VALUE,
                Float.MIN_NORMAL, -Float.MIN_NORMAL, 0.5f, Math.nextDown(1.0f), 1.0f,
                Float.MAX_VALUE, -Float.MAX_VALUE};
        for (int regime = 0; regime < 4; regime++) {
            float[] hb = new float[32], hw = new float[4096], ow = new float[32];
            for (int i = 0; i < hw.length; i++) hw[i] = regime == 3 ? edge[i % edge.length]
                    : (float) random.nextDouble(-1, 1) * new float[] {0.015625f, 1, 10000}[regime];
            for (int i = 0; i < 32; i++) {
                hb[i] = regime == 3 ? edge[i % edge.length] : (float) random.nextDouble(-1, 1);
                ow[i] = regime == 3 ? edge[(i + 3) % edge.length] : (float) random.nextDouble(-1, 1);
            }
            NnueNetwork network = NnueNetwork.of(1, new float[64], new float[NnueNetwork.FEATURE_WEIGHT_COUNT],
                    hb, hw, -0.0f, ow);
            Oracle oracle = new Oracle(network);
            // Mutation of construction arguments must not influence the inference path.
            Arrays.fill(hw, Float.NaN);
            float[] input = new float[128], actual = new float[32];
            for (int sample = 0; sample < 1024; sample++) {
                for (int i = 0; i < 128; i++) input[i] = sample < edge.length
                        ? NnueNetwork.clip01(edge[sample]) : (float) random.nextDouble();
                float[] before = input.clone();
                float expected = oracle.forward(input);
                bits(expected, network.forward(input, actual));
                for (int i = 0; i < 32; i++) bits(oracle.hidden[i], actual[i]);
                assertArrayEquals(before, input);
            }
        }
    }

    private static void bits(float expected, float actual) {
        // Inherited overflow may produce NaN; payload propagation is not a numeric contract.
        assertEquals(Float.floatToIntBits(expected), Float.floatToIntBits(actual));
    }

    private static final class Oracle {
        final float[] hb = new float[32], hw = new float[4096], ow = new float[32], hidden = new float[32];
        final float bias;
        Oracle(NnueNetwork network) {
            bias = network.outputBias();
            for (int i = 0; i < 32; i++) {
                hb[i] = network.hiddenBias(i);
                ow[i] = network.outputWeight(i);
                for (int j = 0; j < 128; j++) hw[i * 128 + j] = network.hiddenWeight(i, j);
            }
        }
        float forward(float[] input) { return NnueMath.forward(hb, hw, bias, ow, input, null, hidden); }
    }
}
