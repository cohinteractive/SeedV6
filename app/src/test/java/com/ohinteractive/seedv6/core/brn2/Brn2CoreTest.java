package com.ohinteractive.seedv6.core.brn2;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.*;
import static com.ohinteractive.seedv6.core.brn2.Brn2Model.*;
import static org.junit.jupiter.api.Assertions.*;

class Brn2CoreTest {
    @Test void parameterLayoutIsBijectiveAndHasExactly1645665TrainableBinary64Values() {
        assertEquals(32, HIDDEN_WIDTH); assertEquals(2, ENDPOINT_COUNT);
        assertEquals(960, NODE_ROWS); assertEquals(25200, RELATION_ROWS); assertEquals(64, STATUS_ROWS);
        int index = 0;
        for (int f = 1; f < BrnFeatureSchema.RELATION_OFFSET; f++) for (int h = 0; h < 32; h++)
            assertEquals(index++, nodeIndex(f, h));
        for (int f = BrnFeatureSchema.RELATION_OFFSET; f < BrnFeatureSchema.STATUS_OFFSET; f++) for (int h = 0; h < 32; h++)
            assertEquals(index++, relationAIndex(f, h));
        for (int f = BrnFeatureSchema.RELATION_OFFSET; f < BrnFeatureSchema.STATUS_OFFSET; f++) for (int h = 0; h < 32; h++)
            assertEquals(index++, relationBIndex(f, h));
        for (int bit = 0; bit < 64; bit++) for (int h = 0; h < 32; h++) assertEquals(index++, statusIndex(bit, h));
        assertEquals(index, LOCAL_BIAS_OFFSET); index += 32;
        assertEquals(index, BOARD_BIAS_OFFSET); index += 32;
        assertEquals(index, OUTPUT_WEIGHT_OFFSET); index += 32;
        assertEquals(index++, OUTPUT_BIAS); assertEquals(1645665, index);
        assertEquals(index, PARAMETER_COUNT); assertEquals(index, new Brn2Model().copyWeights().length);
        assertThrows(IndexOutOfBoundsException.class, () -> nodeIndex(0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> relationAIndex(1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> relationBIndex(BrnFeatureSchema.STATUS_OFFSET, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> statusIndex(64, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> nodeIndex(1, 32));
    }

    @Test void deterministicInitializationUsesTheSpecifiedRangesAndHasUsableUnsaturatedActivations() {
        var a = new Brn2Model(); var b = new Brn2Model();
        assertArrayEquals(a.copyWeights(), b.copyWeights());
        var random = new Random(0x533642524e320001L);
        for (int i = 0; i < LOCAL_BIAS_OFFSET; i++)
            assertEquals((2 * random.nextDouble() - 1) * (i < 960 * 32 ? .01 : .005), a.weight(i));
        for (int h = 0; h < 32; h++) {
            assertEquals(0, a.weight(LOCAL_BIAS_OFFSET + h)); assertEquals(0, a.weight(BOARD_BIAS_OFFSET + h));
            assertEquals((2 * random.nextDouble() - 1) * StrictMath.sqrt(6.0 / 33), a.weight(OUTPUT_WEIGHT_OFFSET + h));
        }
        assertEquals(0, a.weight(OUTPUT_BIAS)); assertNotEquals(a.weight(0), a.weight(1));
        var s = new Brn2Workspace();
        for (long[] board : corpus()) {
            double prediction = a.evaluate(board, s);
            assertTrue(Double.isFinite(prediction) && Math.abs(prediction) < .98, "pathological bootstrap: " + prediction);
            if (s.features.nodeCount() > 0) {
                int positive = 0, negative = 0;
                for (int i = 0; i < s.features.nodeCount() * 32; i++) {
                    if (s.localPre[i] > 0) positive++; else if (s.localPre[i] < 0) negative++;
                }
                assertTrue(positive > 0 && negative > 0);
            }
        }
        double[] copy = a.copyWeights(); copy[0] = 123; assertNotEquals(123, a.weight(0));
    }

    @Test void canonicalPhysicalPairIsRoutedOnceWithAsymmetricEndpointsRegardlessOfEnumeration() {
        long[] board = raw(0, 15, 7, 3, 8), reversed = raw(0, 3, 8, 15, 7);
        int relation = BrnFeatureSchema.relationIndex(15, 7, 3, 8);
        assertEquals(relation, BrnFeatureSchema.relationIndex(3, 8, 15, 7)); // dx=-7, dy=1
        double[] w = new double[PARAMETER_COUNT];
        w[relationAIndex(relation, 0)] = 2; w[relationBIndex(relation, 0)] = 5;
        w[OUTPUT_WEIGHT_OFFSET] = .1;
        var s = new Brn2Workspace(); double value = s.evaluate(board, w);
        assertEquals(1, s.features.relationCount()); assertEquals(2, s.localPre[0]); assertEquals(5, s.localPre[32]);
        assertEquals(7, s.boardPre[0]); assertEquals(StrictMath.tanh(.7), value, 1e-15);
        assertEquals(value, s.evaluate(reversed, w));
        assertEquals(value, oracle(w, board, true), 1e-15);
    }

    @Test void incidentRelationsCombineBeforeLocalReluAndDifferentNodesStaySeparate() {
        long[] board = raw(0, 1, 0, 2, 1, 3, 8);
        double[] w = new double[PARAMETER_COUNT];
        int r01 = BrnFeatureSchema.relationIndex(1, 0, 2, 1), r08 = BrnFeatureSchema.relationIndex(1, 0, 3, 8);
        w[relationAIndex(r01, 0)] = 2; w[relationAIndex(r08, 0)] = -3;
        w[relationBIndex(r01, 0)] = 4; w[relationBIndex(r08, 0)] = -2;
        w[OUTPUT_WEIGHT_OFFSET] = .1;
        var s = new Brn2Workspace();
        assertEquals(StrictMath.tanh(.4), s.evaluate(board, w));
        assertEquals(-1, s.localPre[0]); assertEquals(4, s.localPre[32]); assertEquals(-2, s.localPre[64]);
        assertEquals(4, s.boardPre[0]); // neither ReLU-per-edge (6) nor global-first ReLU (1)
    }

    @Test void canonicalRuleStatusConditionsEachLocalNodeBeforeTheFirstReluAndNeverReadsZobrist() {
        double[] w = new double[PARAMETER_COUNT];
        w[nodeIndex(BrnFeatureSchema.nodeIndex(1, 0), 0)] = 1;
        w[nodeIndex(BrnFeatureSchema.nodeIndex(2, 1), 0)] = -2;
        for (int bit = 0; bit < 64; bit++) w[statusIndex(bit, 0)] = -.75;
        w[OUTPUT_WEIGHT_OFFSET] = 1;
        var s = new Brn2Workspace();
        assertEquals(StrictMath.tanh(1), s.evaluate(raw(0, 1, 0, 2, 1), w));
        for (int bit = 1; bit < Board.FULL_MOVE_NUMBER_SHIFT; bit++) {
            var board = raw(1L << bit, 1, 0, 2, 1);
            assertEquals(StrictMath.tanh(.25), s.evaluate(board, w));
            assertEquals(.25, s.localPre[0]); assertEquals(-2.75, s.localPre[32]);
            board[Board.KEY] = ~board[Board.KEY]; assertEquals(StrictMath.tanh(.25), s.evaluate(board, w));
        }
        assertEquals(0, s.evaluate(raw((1L << Board.FULL_MOVE_NUMBER_SHIFT) - 2, 1, 0, 2, 1), w));
    }

    @Test void boardBiasAndSecondReluActAfterPoolingIncludingAnEmptyBoard() {
        double[] w = new double[PARAMETER_COUNT];
        w[LOCAL_BIAS_OFFSET] = 1; w[LOCAL_BIAS_OFFSET + 1] = 2;
        w[BOARD_BIAS_OFFSET] = -3; w[BOARD_BIAS_OFFSET + 1] = -1;
        w[OUTPUT_WEIGHT_OFFSET] = 100; w[OUTPUT_WEIGHT_OFFSET + 1] = .2;
        w[OUTPUT_BIAS] = -.1;
        var s = new Brn2Workspace();
        assertEquals(StrictMath.tanh(.5), s.evaluate(raw(0, 1, 0, 2, 63), w), 1e-15);
        assertEquals(-1, s.boardPre[0]); assertEquals(3, s.boardPre[1]);
        assertEquals(StrictMath.tanh(-.1), s.evaluate(new long[5], w));
        w[BOARD_BIAS_OFFSET + 1] = 2;
        assertEquals(StrictMath.tanh(.3), s.evaluate(new long[5], w), 1e-15);
    }

    @Test void forwardMatchesIndependentSquareOracleAndReusedWorkspaceIsDeterministic() {
        double[] w = new Brn2Model().copyWeights(); var model = new Brn2Model(w); var s = new Brn2Workspace();
        var trainer = new Brn2Trainer(model, new BrnAdamConfig(.001));
        for (long[] board : corpus()) {
            double expected = oracle(w, board, false);
            assertEquals(expected, oracle(w, board, true), 2e-14);
            for (int repeat = 0; repeat < 3; repeat++) assertEquals(expected, model.evaluate(board, s), 2e-14);
            assertEquals(model.evaluate(board, s), trainer.predict(board));
        }
        // Worst-case packed input: every square occupied, every status bit set, all 12 chess piece codes supported.
        long[] full = new long[6];
        for (int sq = 0; sq < 64; sq++) put(full, sq % 6 + 1 + ((sq / 6) % 2) * 8, sq);
        full[Board.STATUS] = -1;
        assertEquals(oracle(w, full, false), model.evaluate(full, s), 2e-14);
        assertEquals(2016, s.features.relationCount()); assertEquals(64, s.features.nodeCount());
        trainer.train(full, .2); trainer.train(new long[5], -.2); trainer.train(full, .2);
        assertEquals(3, trainer.optimizer().step());
    }

    @Test void finiteDifferencesCoverEveryParameterFamilyAndBothReluMasks() {
        long[] board = raw(1L << Board.HALF_MOVE_CLOCK_SHIFT, 1, 0, 1, 1, 1, 2);
        double[] w = gradientWeights();
        var trainer = new Brn2Trainer(new Brn2Model(w), new BrnAdamConfig(.001));
        trainer.train(board, -.3);
        int relation = BrnFeatureSchema.relationIndex(1, 0, 1, 1);
        int[] selected = {nodeIndex(BrnFeatureSchema.nodeIndex(1, 0), 0), nodeIndex(BrnFeatureSchema.nodeIndex(1, 1), 0),
                relationAIndex(relation, 0), relationBIndex(relation, 0), statusIndex(Board.HALF_MOVE_CLOCK_SHIFT, 0), LOCAL_BIAS_OFFSET,
                BOARD_BIAS_OFFSET, OUTPUT_WEIGHT_OFFSET, OUTPUT_BIAS, LOCAL_BIAS_OFFSET + 1,
                BOARD_BIAS_OFFSET + 1, OUTPUT_WEIGHT_OFFSET + 1, relationAIndex(relation, 1)};
        for (int i : selected) {
            double old = w[i], eps = 1e-6;
            w[i] = old + eps; double plus = loss(w, board, -.3);
            w[i] = old - eps; double minus = loss(w, board, -.3); w[i] = old;
            assertEquals((plus - minus) / (2 * eps), trainer.optimizer().firstMoment(i) / (1 - .9), 2e-9, "parameter " + i);
        }
    }

    @Test void sparseAdamAggregatesDifferentEndpointDerivativesBeforeOneStepAndFreezesAbsentRows() {
        long[] board = raw(1L << Board.HALF_MOVE_CLOCK_SHIFT, 1, 0, 1, 1, 1, 2);
        double[] w = gradientWeights(), first = new double[PARAMETER_COUNT], second = new double[PARAMETER_COUNT];
        Arrays.fill(first, .03); Arrays.fill(second, .02);
        var trainer = new Brn2Trainer(w.clone(), new BrnAdamConfig(.001), new Brn2AdamState(7, first.clone(), second.clone()));
        double p = trainer.predict(board), d = (p - 1) * (1 - p * p), g = d * .2;
        double[] gradient = new double[PARAMETER_COUNT]; boolean[] active = new boolean[EMBEDDING_ROWS];
        // Pre-activations at squares 0,1,2 are -1,.5,1. Only the last two pass local ReLU.
        for (int sq = 0; sq < 3; sq++) {
            int index = nodeIndex(BrnFeatureSchema.nodeIndex(1, sq), 0);
            active[index / 32] = true; gradient[index] = sq == 0 ? 0 : g;
        }
        for (int a = 0; a < 3; a++) for (int b = a + 1; b < 3; b++) {
            int r = BrnFeatureSchema.relationIndex(1, a, 1, b);
            int ia = relationAIndex(r, 0), ib = relationBIndex(r, 0);
            active[ia / 32] = true; active[ib / 32] = true;
            gradient[ia] += a == 0 ? 0 : g; gradient[ib] += g;
        }
        active[statusIndex(Board.HALF_MOVE_CLOCK_SHIFT, 0) / 32] = true; gradient[statusIndex(Board.HALF_MOVE_CLOCK_SHIFT, 0)] = g + g;
        gradient[LOCAL_BIAS_OFFSET] = g + g; gradient[BOARD_BIAS_OFFSET] = g;
        gradient[OUTPUT_WEIGHT_OFFSET] = d * 1.8; gradient[OUTPUT_BIAS] = d;
        int repeated = BrnFeatureSchema.relationIndex(1, 0, 1, 1);
        assertEquals(g, gradient[relationAIndex(repeated, 0)]); assertEquals(2 * g, gradient[relationBIndex(repeated, 0)]);
        double[] expected = w.clone();
        double c1 = 1 - StrictMath.pow(.9, 8), c2 = 1 - StrictMath.pow(.999, 8);
        for (int i = 0; i < PARAMETER_COUNT; i++) {
            if (i < LOCAL_BIAS_OFFSET && !active[i / 32]) continue;
            first[i] = .9 * first[i] + (1 - .9) * gradient[i];
            second[i] = .999 * second[i] + (1 - .999) * gradient[i] * gradient[i];
            expected[i] -= .001 * (first[i] / c1) / (StrictMath.sqrt(second[i] / c2) + 1e-8);
        }
        trainer.train(board, 1);
        assertArrayEquals(expected, trainer.weights, 1e-16); assertArrayEquals(first, trainer.optimizer().first, 1e-16);
        assertArrayEquals(second, trainer.optimizer().second, 1e-16); assertEquals(8, trainer.optimizer().step());
        // Every previously touched sparse row, including its nonzero momentum, freezes across an empty input.
        double[] prior = trainer.weights.clone(), m = trainer.optimizer().first.clone(), v = trainer.optimizer().second.clone();
        trainer.train(new long[5], -1);
        assertArrayEquals(Arrays.copyOf(prior, LOCAL_BIAS_OFFSET), Arrays.copyOf(trainer.weights, LOCAL_BIAS_OFFSET));
        assertArrayEquals(Arrays.copyOf(m, LOCAL_BIAS_OFFSET), Arrays.copyOf(trainer.optimizer().first, LOCAL_BIAS_OFFSET));
        assertArrayEquals(Arrays.copyOf(v, LOCAL_BIAS_OFFSET), Arrays.copyOf(trainer.optimizer().second, LOCAL_BIAS_OFFSET));
        trainer.train(board, 1); assertEquals(10, trainer.optimizer().step()); // slot reuse after the gap
    }

    @Test void invalidInputsAndNonfiniteCandidatesNeverPartiallyPublish() throws Exception {
        var t = new Brn2Trainer(.001); byte[] before = Brn2Codec.encodeTraining(t);
        for (double bad : new double[] {Double.NaN, Double.POSITIVE_INFINITY, -1.1, 1.1})
            assertThrows(IllegalArgumentException.class, () -> t.train(Board.startingPosition(), bad));
        assertThrows(IllegalArgumentException.class, () -> t.train(new long[4], 1));
        assertArrayEquals(before, Brn2Codec.encodeTraining(t));
        double[] w = new double[PARAMETER_COUNT]; w[BOARD_BIAS_OFFSET] = 1e200;
        var overflow = new Brn2Trainer(new Brn2Model(w), new BrnAdamConfig(.001));
        byte[] prior = Brn2Codec.encodeTraining(overflow);
        assertThrows(ArithmeticException.class, () -> overflow.train(new long[5], 1));
        assertArrayEquals(prior, Brn2Codec.encodeTraining(overflow));
    }

    @Test void learnsSyntheticConvergingRelationsAcrossTranslatedLocalMotifs() {
        // Two code-1 anchors. Code 2 is one file right of one anchor; code 3 is one rank
        // above one anchor. Target +1 iff these two exact primitive relations converge
        // on the SAME anchor; otherwise -1. No chess semantics enter this synthetic task.
        long[][] boards = new long[48][]; double[] targets = new double[48];
        int n = 0;
        for (int rank = 0; rank < 3; rank++) for (int file = 0; file < 4; file++)
            for (int a = 0; a < 2; a++) for (int b = 0; b < 2; b++) {
                boards[n] = motif(rank * 8 + file, a, b); targets[n++] = a == b ? 1 : -1;
            }
        // A constructive local solution confirms the target requires both incident messages
        // in this channel: either relation alone is below zero, together they activate it.
        double[] witness = new double[PARAMETER_COUNT]; witness[LOCAL_BIAS_OFFSET] = -1.5;
        witness[relationAIndex(BrnFeatureSchema.relationIndex(1, 0, 2, 1), 0)] = 1;
        witness[relationAIndex(BrnFeatureSchema.relationIndex(1, 0, 3, 8), 0)] = 1;
        witness[OUTPUT_WEIGHT_OFFSET] = 8; witness[OUTPUT_BIAS] = -2;
        for (int i = 0; i < n; i++) assertTrue(new Brn2Workspace().evaluate(boards[i], witness) * targets[i] > .95);
        var trainer = new Brn2Trainer(.003); double initial = meanLoss(trainer, boards, targets);
        int[] order = new int[n]; for (int i = 0; i < n; i++) order[i] = i;
        var random = new Random(72);
        for (int epoch = 0; epoch < 600; epoch++) {
            for (int i = n - 1; i > 0; i--) { int j = random.nextInt(i + 1), swap = order[i]; order[i] = order[j]; order[j] = swap; }
            for (int i : order) trainer.train(boards[i], targets[i]);
        }
        double after = meanLoss(trainer, boards, targets);
        for (int i = 0; i < n; i++) assertTrue(trainer.predict(boards[i]) * targets[i] > .95);
        assertTrue(after < .002 && after < initial / 100);
        System.out.printf("BRN2_LOCAL_MOTIF examples=%d initial=%.9f final=%.9f (architecture learning only)%n", n, initial, after);
    }

    private static long[] motif(int origin, int a, int b) { return raw(0, 1, origin, 1, origin + 3, 2, origin + a * 3 + 1, 3, origin + b * 3 + 8); }
    private static double[] gradientWeights() {
        double[] w = new double[PARAMETER_COUNT];
        w[nodeIndex(BrnFeatureSchema.nodeIndex(1, 0), 0)] = -1;
        w[nodeIndex(BrnFeatureSchema.nodeIndex(1, 1), 0)] = .5;
        w[nodeIndex(BrnFeatureSchema.nodeIndex(1, 2), 0)] = 1;
        w[BOARD_BIAS_OFFSET] = .3; w[OUTPUT_WEIGHT_OFFSET] = .2;
        w[LOCAL_BIAS_OFFSET + 1] = 1; w[BOARD_BIAS_OFFSET + 1] = -10; w[OUTPUT_WEIGHT_OFFSET + 1] = .7;
        return w;
    }
    static long[] raw(long status, int... codeSquares) {
        long[] board = new long[6]; board[Board.STATUS] = status;
        for (int i = 0; i < codeSquares.length; i += 2) put(board, codeSquares[i], codeSquares[i + 1]);
        return board;
    }
    private static void put(long[] board, int code, int square) {
        for (int plane = 0; plane < 4; plane++) if ((code & (1 << plane)) != 0) board[plane] |= 1L << square;
    }
    private static long[][] corpus() {
        return new long[][] {Board.startingPosition(), Board.fromFen("r3k2r/pppq1ppp/2npbn2/3Np3/2B1P3/2N2Q1P/PPP2PP1/R3K2R b KQkq - 4 12"),
                Board.fromFen("7k/8/8/8/8/8/PPP5/7K w - - 0 1"), new long[5], raw(Long.MIN_VALUE, 15, 63)};
    }
    /** Deliberately square-centric O(64^2 H) oracle; never uses the production pair extractor. */
    private static double oracle(double[] w, long[] board, boolean reverse) {
        int flip = Board.player((int) board[Board.STATUS]) * 56;
        int role = Board.player((int) board[Board.STATUS]) * 8;
        long status = Brn2Features.status(board[Board.STATUS], role / 8);
        double[] pooled = Arrays.copyOfRange(w, BOARD_BIAS_OFFSET, BOARD_BIAS_OFFSET + 32);
        for (int q = 0; q < 64; q++) {
            int sq = reverse ? 63 - q : q, code = Board.getSquare(board[0], board[1], board[2], board[3], sq ^ flip);
            if (code == 0) continue;
            code ^= role;
            for (int h = 0; h < 32; h++) {
                double pre = w[nodeIndex(BrnFeatureSchema.nodeIndex(code, sq), h)] + w[LOCAL_BIAS_OFFSET + h];
                for (int bit = 0; bit < 64; bit++) if ((status & (1L << bit)) != 0) pre += w[statusIndex(bit, h)];
                for (int r = 0; r < 64; r++) {
                    int other = reverse ? 63 - r : r;
                    int otherCode = Board.getSquare(board[0], board[1], board[2], board[3], other ^ flip);
                    if (other == sq || otherCode == 0) continue;
                    otherCode ^= role;
                    int relation = BrnFeatureSchema.relationIndex(code, sq, otherCode, other);
                    pre += w[sq < other ? relationAIndex(relation, h) : relationBIndex(relation, h)];
                }
                pooled[h] += Math.max(0, pre);
            }
        }
        double z = w[OUTPUT_BIAS];
        for (int h = 0; h < 32; h++) z += w[OUTPUT_WEIGHT_OFFSET + h] * Math.max(0, pooled[h]);
        return StrictMath.tanh(z);
    }
    private static double loss(double[] w, long[] board, double target) { double d = new Brn2Workspace().evaluate(board, w) - target; return .5 * d * d; }
    private static double meanLoss(Brn2Trainer trainer, long[][] boards, double[] targets) {
        double sum = 0; for (int i = 0; i < boards.length; i++) { double d = trainer.predict(boards[i]) - targets[i]; sum += .5 * d * d; }
        return sum / boards.length;
    }
}
