package com.ohinteractive.seedv6.training.validation;

import java.util.List;

/** Fixed-sample, one-sided Hoeffding gate on independent opening-pair scores, not Elo. */
public record PromotionPolicy(int minimumPairs, double alpha, double requiredMargin) {
    public static final PromotionPolicy DEFAULT = new PromotionPolicy(64, 0.05, 0.0);
    public enum Decision { PROMOTE, RETAIN_INCUMBENT, INCONCLUSIVE }

    public PromotionPolicy {
        if (minimumPairs < 1 || !Double.isFinite(alpha) || alpha <= 0 || alpha >= 1
                || !Double.isFinite(requiredMargin) || requiredMargin < 0) {
            throw new IllegalArgumentException("Invalid promotion policy.");
        }
    }

    public record Assessment(int validPairs, double mean, double radius, double lowerBound,
                             double threshold, Decision decision) {}

    public Assessment assess(List<Double> pairScores) {
        double sum = 0;
        for (double score : pairScores) {
            if (!Double.isFinite(score) || score < 0 || score > 1) {
                throw new IllegalArgumentException("Pair score must be finite in [0,1].");
            }
            sum += score;
        }
        return assess(pairScores.size(), sum);
    }

    public Assessment assess(int validPairs, double pairScoreSum) {
        if (validPairs < 0 || !Double.isFinite(pairScoreSum) || pairScoreSum < 0 || pairScoreSum > validPairs) {
            throw new IllegalArgumentException("Invalid pair sample.");
        }
        double mean = validPairs == 0 ? Double.NaN : pairScoreSum / validPairs;
        // -log(alpha) avoids overflow in 1/alpha for very small, valid alpha.
        double radius = validPairs == 0 ? Double.POSITIVE_INFINITY
                : Math.sqrt(-Math.log(alpha) / (2.0 * validPairs));
        double lower = validPairs == 0 ? Double.NEGATIVE_INFINITY : mean - radius;
        double threshold = 0.5 + requiredMargin;
        Decision decision = validPairs < minimumPairs ? Decision.INCONCLUSIVE
                : lower > threshold ? Decision.PROMOTE : Decision.RETAIN_INCUMBENT;
        return new Assessment(validPairs, mean, radius, lower, threshold, decision);
    }
}

