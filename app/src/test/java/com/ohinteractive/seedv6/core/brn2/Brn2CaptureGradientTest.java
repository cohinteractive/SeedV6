package com.ohinteractive.seedv6.core.brn2;

import java.util.*;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.*;
import com.ohinteractive.seedv6.training.selfplay.HeadlessGame;
import com.ohinteractive.seedv6.core.move.Move;
import static com.ohinteractive.seedv6.core.brn2.Brn2Model.*;
import static org.junit.jupiter.api.Assertions.*;

class Brn2CaptureGradientTest {
    final long[] parent = Board.fromFen("7k/6rp/5KQ1/8/8/8/8/8 w - - 0 1");
    long[] child() {
        var game = new HeadlessGame(parent, 2);
        game.play(Arrays.stream(game.legalMoves()).filter(m -> Move.coordinate(m).equals("g6h7")).findFirst().orElseThrow());
        assertTrue(game.active()); return game.boardSnapshot();
    }
    @Test void finiteDifferencesVerifyBothEndpointsSharedRowsAndLambdaInBothHuberBranches() {
        long[] child = child();
        var original = new Brn2Trainer(.001);
        double[] weights = original.weights.clone();
        // Move away from ReLU kinks for a stable finite difference oracle.
        for (int h = 0; h < HIDDEN_WIDTH; h++) { weights[LOCAL_BIAS_OFFSET + h] = .04; weights[BOARD_BIAS_OFFSET + h] = .03; }
        original = new Brn2Trainer(new Brn2Model(weights), new BrnAdamConfig(.001));
        double predictedDelta = -original.predict(child) - original.predict(parent);
        var p = new Brn2Workspace(); p.evaluate(parent, weights);
        var c = new Brn2Workspace(); c.evaluate(child, weights);
        var indices = new LinkedHashSet<Integer>();
        for (var ws : List.of(p, c)) {
            for (int i = 1; i <= ws.features.nodeCount(); i++) indices.add(nodeIndex(ws.features.indexAt(i), 0));
            int firstPair = 1 + ws.features.nodeCount();
            for (int i = firstPair; i < firstPair + ws.features.relationCount(); i++) {
                indices.add(relationAIndex(ws.features.indexAt(i), 0)); indices.add(relationBIndex(ws.features.indexAt(i), 0));
            }
            for (int i = firstPair + ws.features.relationCount(); i < ws.features.size(); i++)
                indices.add(statusIndex(ws.features.indexAt(i) - BrnFeatureSchema.STATUS_OFFSET, 0));
        }
        indices.addAll(List.of(LOCAL_BIAS_OFFSET, BOARD_BIAS_OFFSET, OUTPUT_WEIGHT_OFFSET, OUTPUT_BIAS));
        for (double error : new double[]{.1, -.1, .6, -.6}) for (double lambda : new double[]{.5, 2}) {
            double delta = predictedDelta - error;
            assertTrue(Math.abs(delta) <= 1);
            var trainer = new Brn2Trainer(new Brn2Model(weights), new BrnAdamConfig(.001));
            double y = trainer.predict(parent), yc = trainer.predict(child), target = .23;
            var loss = trainer.trainCapture(parent, target, child, delta, lambda);
            assertEquals(.5 * (y - target) * (y - target), loss.base());
            assertEquals(huber(-yc - y - delta), loss.auxiliary());
            // Native STM auxiliary output derivatives have the SAME negative sign.
            double aux = -lambda * Math.max(-.25, Math.min(.25, -yc - y - delta));
            assertEquals((y - target + aux) * (1 - y*y) + aux * (1 - yc*yc),
                    trainer.optimizer().firstMoment(OUTPUT_BIAS) / (1 - .9), 1e-14);
            for (int index : indices) {
                double old = weights[index], eps = 1e-6;
                weights[index] = old + eps; double plus = objective(weights, child, target, delta, lambda);
                weights[index] = old - eps; double minus = objective(weights, child, target, delta, lambda);
                weights[index] = old;
                double gradient = (plus - minus) / (2 * eps);
                assertEquals(gradient, trainer.optimizer().firstMoment(index) / (1 - .9), 2e-8, "parameter " + index);
                assertEquals(gradient * gradient, trainer.optimizer().secondMoment(index) / (1 - .999), 2e-8);
            }
            assertEquals(1, trainer.optimizer().step()); // Never an ordinary child update.
        }
    }
    double objective(double[] weights, long[] child, double target, double delta, double lambda) {
        var ws = new Brn2Workspace(); double p = ws.evaluate(parent, weights), c = ws.evaluate(child, weights);
        return .5 * (p-target)*(p-target) + lambda * huber(-c-p-delta);
    }
    static double huber(double e) { return Math.abs(e) <= .25 ? .5*e*e : .25*(Math.abs(e)-.125); }
    @Test void disabledIsExactOrdinaryUpdateAndDoesNotInspectChildOrAuxiliaryTarget() throws Exception {
        var ordinary = new Brn2Trainer(.001); var integrated = new Brn2Trainer(.001);
        for (int i = 0; i < 24; i++) {
            double target = (i % 3 - 1) * .5;
            assertEquals(ordinary.train(parent, target), integrated.trainCapture(parent, target, null, Double.NaN, 0).base());
        }
        assertArrayEquals(Brn2Codec.encodeTraining(ordinary), Brn2Codec.encodeTraining(integrated));
    }
    @Test void nonfiniteCandidateDoesNotPartiallyPublishWeightsOrOptimizer() throws Exception {
        var trainer = new Brn2Trainer(.001); byte[] before = Brn2Codec.encodeTraining(trainer);
        assertThrows(ArithmeticException.class, () -> trainer.trainCapture(parent, .2, child(), 1, Double.MAX_VALUE));
        assertArrayEquals(before, Brn2Codec.encodeTraining(trainer));
    }
}
