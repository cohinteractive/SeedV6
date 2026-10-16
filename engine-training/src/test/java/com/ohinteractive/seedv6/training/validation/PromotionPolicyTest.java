package com.ohinteractive.seedv6.training.validation;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import static com.ohinteractive.seedv6.training.validation.PromotionPolicy.Decision.*;
import static org.junit.jupiter.api.Assertions.*;

class PromotionPolicyTest {
    @Test void rawBoundaryUsesTheDecisionRadiusMarginAndMinimumSampleGate() {
        for (int n : new int[]{16, 32, 64, 128}) for (double alpha : new double[]{.01, .05, .4})
            for (double margin : new double[]{0, .03, .1}) {
                var policy = new PromotionPolicy(16, alpha, margin);
                double raw = policy.rawScoreThreshold(n).orElseThrow();
                var assessment = policy.assess(n, n * .5);
                assertEquals(assessment.threshold() + assessment.radius(), raw);
                assertEquals(RETAIN_INCUMBENT, policy.assess(n, n * (raw - 1e-9)).decision());
                assertEquals(PROMOTE, policy.assess(n, n * (raw + 1e-9)).decision());
            }
        assertTrue(PromotionPolicy.DEFAULT.rawScoreThreshold(63).isEmpty());
        assertTrue(PromotionPolicy.DEFAULT.rawScoreThreshold(0).isEmpty());
        assertTrue(new PromotionPolicy(1, .000001, 0).rawScoreThreshold(1).isEmpty());
        assertTrue(new PromotionPolicy(1, .9, .5).rawScoreThreshold(128).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> PromotionPolicy.DEFAULT.rawScoreThreshold(-1));
    }
    @Test void defaultsAreExplicitAndConservative() {
        assertEquals(new PromotionPolicy(64, 0.05, 0), PromotionPolicy.DEFAULT);
        var pass = PromotionPolicy.DEFAULT.assess(64, 64);
        assertEquals(PROMOTE, pass.decision());
        assertEquals(Math.sqrt(Math.log(20) / 128), pass.radius());
        assertEquals(pass.mean() - pass.radius(), pass.lowerBound());
        assertEquals(0.5, pass.threshold());
    }
    @Test void exactBoundaryIsStrictAndEitherSideBehavesCorrectly() {
        double radius = new PromotionPolicy(64, 0.05, 0).assess(64, 64).radius();
        var policy = new PromotionPolicy(64, 0.05, 0.5 - radius);
        var boundary = policy.assess(64, 64);
        assertEquals(boundary.threshold(), boundary.lowerBound());
        assertEquals(RETAIN_INCUMBENT, boundary.decision());
        assertEquals(PROMOTE, new PromotionPolicy(64, 0.05, 0.5 - radius - 1e-6).assess(64, 64).decision());
        assertEquals(RETAIN_INCUMBENT, new PromotionPolicy(64, 0.05, 0.5 - radius + 1e-6).assess(64, 64).decision());
    }
    @Test void insufficientPerfectPairsAndNoPairsAreInconclusive() {
        assertEquals(INCONCLUSIVE, PromotionPolicy.DEFAULT.assess(63, 63).decision());
        var empty = PromotionPolicy.DEFAULT.assess(List.of());
        assertEquals(INCONCLUSIVE, empty.decision());
        assertTrue(Double.isNaN(empty.mean()));
        assertEquals(Double.POSITIVE_INFINITY, empty.radius());
        assertEquals(Double.NEGATIVE_INFINITY, empty.lowerBound());
    }
    @Test void drawHeavyAndColourBalancedScoresRetain() {
        assertEquals(RETAIN_INCUMBENT, PromotionPolicy.DEFAULT.assess(Collections.nCopies(64, 0.5)).decision());
        var mixed = new PromotionPolicy(4, 0.05, 0).assess(List.of(0.0, 0.25, 0.75, 1.0));
        assertEquals(0.5, mixed.mean());
        assertEquals(RETAIN_INCUMBENT, mixed.decision());
    }
    @Test void invalidPolicyAndSamplesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PromotionPolicy(0, 0.05, 0));
        for (double alpha : new double[] {0, 1, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new PromotionPolicy(1, alpha, 0));
        }
        for (double margin : new double[] {-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new PromotionPolicy(1, 0.05, margin));
        }
        for (double score : new double[] {-0.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> PromotionPolicy.DEFAULT.assess(List.of(score)));
        }
        assertThrows(IllegalArgumentException.class, () -> PromotionPolicy.DEFAULT.assess(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> PromotionPolicy.DEFAULT.assess(0, 1));
        assertTrue(Double.isFinite(new PromotionPolicy(1, Double.MIN_VALUE, 0).assess(1, 1).radius()));
    }
}
