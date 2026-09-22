package com.ohinteractive.seedv6.core.brn1;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.*;
import static com.ohinteractive.seedv6.core.brn1.Brn1Model.*;
import static org.junit.jupiter.api.Assertions.*;

class Brn1CoreTest {
    @Test void sharedFeatureIdsMapBijectivelyToExactly839233Parameters() {
        assertEquals(26224, FEATURE_COUNT); assertEquals(32, HIDDEN_WIDTH);
        assertEquals(839168, HIDDEN_BIAS_OFFSET); assertEquals(839233, PARAMETER_COUNT);
        int expected = 0;
        for (int f = 1; f < BrnFeatureSchema.PARAMETER_COUNT; f++)
            for (int c = 0; c < HIDDEN_WIDTH; c++) assertEquals(expected++, embeddingIndex(f, c));
        assertEquals(HIDDEN_BIAS_OFFSET, expected);
        assertThrows(IllegalArgumentException.class, () -> embeddingIndex(0, 0));
        assertThrows(IllegalArgumentException.class, () -> embeddingIndex(FEATURE_COUNT + 1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> embeddingIndex(1, 32));
    }

    @Test void initializationIsReproducibleBoundedAndBreaksChannelSymmetry() {
        var a = new Brn1Model(); var b = new Brn1Model();
        assertArrayEquals(a.copyWeights(), b.copyWeights());
        for (int i = 0; i < HIDDEN_BIAS_OFFSET; i++) assertTrue(Math.abs(a.weight(i)) <= .01);
        double bound = StrictMath.sqrt(6.0 / 33);
        for (int c = 0; c < HIDDEN_WIDTH; c++) {
            assertEquals(0, a.weight(HIDDEN_BIAS_OFFSET + c));
            assertTrue(Math.abs(a.weight(OUTPUT_WEIGHT_OFFSET + c)) <= bound);
            if (c != 0) assertNotEquals(a.weight(0), a.weight(c));
        }
        assertEquals(0, a.weight(OUTPUT_BIAS));
        double[] copy = a.copyWeights(); copy[0] = 100; assertNotEquals(100, a.weight(0));
    }

    @Test void forwardMatchesIndependentOccurrenceOracleAndReusableScratchIsDeterministic() {
        var model = new Brn1Model(); var scratch = new Brn1Workspace();
        for (long[] board : new long[][] {Board.startingPosition(), xorBoard(0, false), xorBoard(1, true), new long[5]}) {
            var features = new BrnFeatures(); features.extract(board);
            double z = model.weight(OUTPUT_BIAS);
            for (int c = 0; c < HIDDEN_WIDTH; c++) {
                double h = model.weight(HIDDEN_BIAS_OFFSET + c);
                for (int n = 1; n < features.size(); n++) h += model.weight(embeddingIndex(features.indexAt(n), c));
                z += model.weight(OUTPUT_WEIGHT_OFFSET + c) * Math.max(0, h);
            }
            double expected = StrictMath.tanh(z);
            for (int repeat = 0; repeat < 5; repeat++) assertEquals(expected, model.evaluate(board, scratch));
            assertEquals(expected, new Brn1Trainer(model, new BrnAdamConfig(.001)).predict(board));
        }
    }

    @Test void reluAndRawSideToMoveGateBoardPatternsInsteadOfAddingAnOutputOffset() {
        var a = xorBoard(0, false); var b = xorBoard(1, false);
        var blackA = xorBoard(0, true); var blackB = xorBoard(1, true);
        long statusDelta = a[Board.STATUS] ^ blackA[Board.STATUS];
        assertEquals(1, Long.bitCount(statusDelta));
        int status = BrnFeatureSchema.statusIndex(Long.numberOfTrailingZeros(statusDelta));
        // A knight on b1 supplies +1; the raw side bit supplies -1.5 to the SAME channel.
        int knight = BrnFeatureSchema.squareCode(a[0], a[1], a[2], a[3], 1);
        double[] w = new double[PARAMETER_COUNT];
        w[embeddingIndex(BrnFeatureSchema.nodeIndex(knight, 1), 0)] = 1;
        w[embeddingIndex(status, 0)] = -1.5;
        w[HIDDEN_BIAS_OFFSET] = -.25; w[OUTPUT_WEIGHT_OFFSET] = 1;
        w[HIDDEN_BIAS_OFFSET + 1] = -2; w[OUTPUT_WEIGHT_OFFSET + 1] = 100;
        var m = new Brn1Model(w); var s = new Brn1Workspace();
        // Avoid assuming the board convention's side-bit polarity in the oracle.
        long[] offA = (a[Board.STATUS] & statusDelta) == 0 ? a : blackA;
        long[] onA = offA == a ? blackA : a;
        long[] offB = (b[Board.STATUS] & statusDelta) == 0 ? b : blackB;
        long[] onB = offB == b ? blackB : b;
        assertEquals(StrictMath.tanh(.75), m.evaluate(offA, s));
        assertEquals(0, m.evaluate(onA, s)); assertEquals(0, m.evaluate(offB, s)); assertEquals(0, m.evaluate(onB, s));
        assertNotEquals(m.evaluate(onA, s) - m.evaluate(offA, s), m.evaluate(onB, s) - m.evaluate(offB, s));
    }

    @Test void finiteDifferencesCheckOutputHiddenAndRepeatedEmbeddingGradients() {
        long[] board = Board.startingPosition();
        double[] w = new Brn1Model().copyWeights();
        for (int c = 0; c < HIDDEN_WIDTH; c++) w[HIDDEN_BIAS_OFFSET + c] = 1;
        var features = new BrnFeatures(); features.extract(board);
        int[] counts = counts(features); int repeated = 0;
        for (int f = BrnFeatureSchema.RELATION_OFFSET; f < counts.length; f++) if (counts[f] > 1) { repeated = f; break; }
        assertTrue(repeated > 0);
        var trainer = new Brn1Trainer(new Brn1Model(w), new BrnAdamConfig(.001));
        double target = -.3; trainer.train(board, target);
        for (int i : new int[] {OUTPUT_BIAS, OUTPUT_WEIGHT_OFFSET + 3, HIDDEN_BIAS_OFFSET + 3,
                embeddingIndex(features.indexAt(1), 3), embeddingIndex(repeated, 3)}) {
            double old = w[i], eps = 1e-6;
            w[i] = old + eps; double plus = loss(w, board, target);
            w[i] = old - eps; double minus = loss(w, board, target); w[i] = old;
            double numeric = (plus - minus) / (2 * eps);
            double analytic = trainer.optimizer().firstMoment(i) / (1 - .9);
            assertEquals(numeric, analytic, 2e-8, "parameter " + i);
        }
    }

    @Test void sparseAdamAggregatesCountsFreezesAbsentRowsAndUpdatesAllDenseMoments() {
        long[] board = Board.startingPosition(); var features = new BrnFeatures(); features.extract(board);
        int[] counts = counts(features);
        double[] w = new double[PARAMETER_COUNT], first = new double[PARAMETER_COUNT], second = new double[PARAMETER_COUNT];
        Arrays.fill(first, .03); Arrays.fill(second, .02);
        // Channel 0 is active; channel 1 and all others have zero derivative but nonzero momentum.
        w[HIDDEN_BIAS_OFFSET] = .5; w[OUTPUT_WEIGHT_OFFSET] = .2;
        var config = new BrnAdamConfig(.001);
        var trainer = new Brn1Trainer(w.clone(), config, new Brn1AdamState(7, first.clone(), second.clone()));
        double prediction = trainer.predict(board), d = (prediction - 1) * (1 - prediction * prediction);
        double[] expected = w.clone(), expectedFirst = first.clone(), expectedSecond = second.clone();
        double c1 = 1 - StrictMath.pow(.9, 8), c2 = 1 - StrictMath.pow(.999, 8);
        int repeatedRows = 0;
        for (int f = 1; f < counts.length; f++) if (counts[f] > 1) repeatedRows++;
        assertTrue(repeatedRows > 0);
        for (int i = 0; i < PARAMETER_COUNT; i++) {
            boolean embedding = i < HIDDEN_BIAS_OFFSET;
            if (embedding && counts[i / HIDDEN_WIDTH + 1] == 0) continue;
            double gradient;
            if (embedding) gradient = i % HIDDEN_WIDTH == 0 ? d * .2 * counts[i / HIDDEN_WIDTH + 1] : 0;
            else if (i == OUTPUT_BIAS) gradient = d;
            else if (i == OUTPUT_WEIGHT_OFFSET) gradient = d * .5;
            else gradient = i == HIDDEN_BIAS_OFFSET ? d * .2 : 0;
            double m = .9 * first[i] + (1 - .9) * gradient;
            double v = .999 * second[i] + (1 - .999) * gradient * gradient;
            expected[i] -= .001 * (m / c1) / (StrictMath.sqrt(v / c2) + 1e-8);
            expectedFirst[i] = m; expectedSecond[i] = v;
        }
        assertEquals(.5 * (prediction - 1) * (prediction - 1), trainer.train(board, 1));
        assertArrayEquals(expected, trainer.weights); assertArrayEquals(expectedFirst, trainer.optimizer().first);
        assertArrayEquals(expectedSecond, trainer.optimizer().second); assertEquals(8, trainer.optimizer().step());
        // A formerly active row freezes with its nonzero moments on a feature-free input.
        int row = embeddingIndex(features.indexAt(1), 0);
        double old = trainer.weights[row], m = trainer.optimizer().first[row], v = trainer.optimizer().second[row];
        trainer.train(new long[5], -1);
        assertEquals(old, trainer.weights[row]); assertEquals(m, trainer.optimizer().first[row]); assertEquals(v, trainer.optimizer().second[row]);
    }

    @Test void invalidInputAndOverflowNeverPartiallyPublishAnOptimizerStep() throws Exception {
        var trainer = new Brn1Trainer(.001); byte[] before = Brn1Codec.encodeTraining(trainer);
        for (double bad : new double[] {Double.NaN, 1.1, -1.1})
            assertThrows(IllegalArgumentException.class, () -> trainer.train(Board.startingPosition(), bad));
        assertThrows(IllegalArgumentException.class, () -> trainer.train(new long[4], 1));
        assertArrayEquals(before, Brn1Codec.encodeTraining(trainer));
        double[] w = new double[PARAMETER_COUNT]; w[HIDDEN_BIAS_OFFSET] = 1e200;
        var overflow = new Brn1Trainer(new Brn1Model(w), new BrnAdamConfig(.001));
        byte[] prior = Brn1Codec.encodeTraining(overflow);
        assertThrows(ArithmeticException.class, () -> overflow.train(new long[5], 1));
        assertArrayEquals(prior, Brn1Codec.encodeTraining(overflow));
    }

    @Test void learnsBoardStatusXorWhichAdditiveBrn0CannotClassify() {
        long[][] boards = {xorBoard(0, false), xorBoard(0, true), xorBoard(1, false), xorBoard(1, true)};
        double[] targets = {1, -1, -1, 1};
        // All primitive board features depend only on placement; raw status only on side.
        // Thus BRN-0 logits necessarily obey z00+z11=z01+z10 for ANY weights.
        int[][] all = new int[4][];
        for (int i = 0; i < 4; i++) { var f = new BrnFeatures(); f.extract(boards[i]); all[i] = counts(f); }
        for (int f = 0; f < BrnFeatureSchema.PARAMETER_COUNT; f++) assertEquals(all[0][f] + all[3][f], all[1][f] + all[2][f]);
        // XOR requires positive logits on the left, negative on the right: impossible for BRN-0.
        // At least one wrong-sign prediction gives mean half-squared loss >= 1/(2*4).
        var trainer = new Brn1Trainer(.01); var baseline = new BrnTrainer(.01);
        double initial = meanLoss(trainer, boards, targets);
        var random = new java.util.Random(71);
        int[] order = {0, 1, 2, 3};
        for (int epoch = 0; epoch < 2000; epoch++) {
            for (int i = 3; i > 0; i--) { int j = random.nextInt(i + 1), t = order[i]; order[i] = order[j]; order[j] = t; }
            for (int i : order) { trainer.train(boards[i], targets[i]); baseline.train(boards[i], targets[i]); }
        }
        double after = meanLoss(trainer, boards, targets), baseLoss = 0;
        for (int i = 0; i < 4; i++) {
            double difference = baseline.predict(boards[i]) - targets[i]; baseLoss += .5 * difference * difference / 4;
            assertTrue(trainer.predict(boards[i]) * targets[i] > .9);
        }
        assertTrue(after < .005 && after < initial / 50); assertTrue(baseLoss >= .125);
        System.out.printf("BRN1_XOR initial=%.9f final=%.9f BRN0=%.9f additiveLowerBound=0.125 (expressivity only)%n", initial, after, baseLoss);
    }

    static long[] xorBoard(int placement, boolean black) {
        return Board.fromFen("4k3/8/8/8/8/8/8/" + (placement == 0 ? "1N2K3" : "2N1K3") + (black ? " b" : " w") + " - - 0 1");
    }
    static int[] counts(BrnFeatures features) {
        int[] result = new int[BrnFeatureSchema.PARAMETER_COUNT];
        for (int i = 0; i < features.size(); i++) result[features.indexAt(i)]++;
        return result;
    }
    private static double loss(double[] w, long[] board, double target) {
        double d = new Brn1Model(w).evaluate(board, new Brn1Workspace()) - target; return .5 * d * d;
    }
    private static double meanLoss(Brn1Trainer trainer, long[][] boards, double[] targets) {
        double result = 0;
        for (int i = 0; i < boards.length; i++) { double d = trainer.predict(boards[i]) - targets[i]; result += .5 * d * d; }
        return result / boards.length;
    }
}
