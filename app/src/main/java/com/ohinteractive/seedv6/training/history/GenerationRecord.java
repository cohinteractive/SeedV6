package com.ohinteractive.seedv6.training.history;

import java.time.Instant;
import java.util.Objects;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;
import com.ohinteractive.seedv6.training.checkpoint.BootstrapEvidence;

/** Factual completed lifecycle, independent of checkpoint format. Null means unmeasured, never zero. */
public record GenerationRecord(long generation, String candidate, String incumbent, String resultingBest,
        Outcome outcome, PromotionPolicy.Decision decision, int wins, int draws, int losses,
        int validPairs, int incompletePairs, Double score, Double lowerBound, double threshold,
        Regime regime, Integer completedGames, Integer abortedGames, Long samples, Double loss,
        Instant started, Instant completed, Long selfPlayNanos, Long trainingNanos, Long validationNanos,
        Long totalNanos, BootstrapEvidence bootstrap) {
    public static final int SCHEMA = 1;
    public enum Outcome { PROMOTED, RETAINED, INCONCLUSIVE, CANCELLED_VALIDATION }
    public record Regime(int depth, int games, int pairs, int threads) {
        public Regime {
            if (depth < 1 || games < 1 || pairs < 0 || threads < 1) throw new IllegalArgumentException("Invalid regime");
        }
        @Override public String toString() { return "Depth " + depth + " · " + games + " games · " + pairs + " pairs · " + threads + " threads"; }
    }
    public GenerationRecord {
        Objects.requireNonNull(outcome); Objects.requireNonNull(decision); Objects.requireNonNull(regime);
        Objects.requireNonNull(completed);
        for (String id : new String[] {candidate, incumbent, resultingBest}) {
            if (id == null || !id.matches("[A-Za-z0-9_-]{1,180}")) throw new IllegalArgumentException("Invalid network identity");
        }
        if (generation < 1 || wins < 0 || draws < 0 || losses < 0 || validPairs < 0 || incompletePairs < 0
                || (long) validPairs + incompletePairs != regime.pairs()
                || (long) wins + draws + losses != 2L * validPairs) throw new IllegalArgumentException("Invalid generation / pair accounting");
        if (bootstrap == null) {
        if (regime.pairs() < 1) throw new IllegalArgumentException("Game-pair validation needs pairs.");
        if (validPairs == 0 ? score != null || lowerBound != null
                : score == null || lowerBound == null || !Double.isFinite(score) || !Double.isFinite(lowerBound)
                || lowerBound > score || Math.abs(score - (wins + .5 * draws) / (2.0 * validPairs)) > 1e-12)
            throw new IllegalArgumentException("Invalid valid-pair score");
        if (!Double.isFinite(threshold) || threshold < .5 || loss != null && (!Double.isFinite(loss) || loss < 0))
            throw new IllegalArgumentException("Invalid assessment / loss");
        boolean promoted = outcome == Outcome.PROMOTED;
        if (promoted != (decision == PromotionPolicy.Decision.PROMOTE)
                || promoted && (lowerBound == null || lowerBound <= threshold)
                || validPairs == 0 && decision != PromotionPolicy.Decision.INCONCLUSIVE
                || !resultingBest.equals(promoted ? candidate : incumbent)
                || outcome == Outcome.RETAINED && decision != PromotionPolicy.Decision.RETAIN_INCUMBENT
                || outcome == Outcome.INCONCLUSIVE && decision != PromotionPolicy.Decision.INCONCLUSIVE
                || outcome == Outcome.CANCELLED_VALIDATION && incompletePairs == 0)
            throw new IllegalArgumentException("Outcome disagrees with settled Best / assessment");
        } else {
            boolean promoted = bootstrap.comparison().decision() == PromotionPolicy.Decision.PROMOTE;
            if (regime.pairs() != 0 || validPairs != 0 || incompletePairs != 0 || wins != 0 || draws != 0 || losses != 0
                    || score != null || lowerBound != null || threshold != 0
                    || decision != bootstrap.comparison().decision()
                    || outcome != (promoted ? Outcome.PROMOTED : Outcome.RETAINED)
                    || !resultingBest.equals(promoted ? candidate : incumbent)
                    || samples == null || samples != (long) bootstrap.trainingSamples() + bootstrap.comparison().samples()
                    || loss != null && (!Double.isFinite(loss) || loss < 0))
                throw new IllegalArgumentException("Held-out history disagrees with evidence.");
        }
        for (Number n : new Number[] {completedGames, abortedGames, samples, selfPlayNanos, trainingNanos, validationNanos, totalNanos})
            if (n != null && n.longValue() < 0) throw new IllegalArgumentException("Negative measurement");
        // Wall clocks may move backwards; elapsed times are measured with the monotonic clock.
        if (totalNanos != null && selfPlayNanos != null && trainingNanos != null && validationNanos != null
                && (double) selfPlayNanos + trainingNanos + validationNanos > totalNanos.doubleValue())
            throw new IllegalArgumentException("Phase durations exceed total");
    }
    public GenerationRecord(long generation, String candidate, String incumbent, String resultingBest,
            Outcome outcome, PromotionPolicy.Decision decision, int wins, int draws, int losses,
            int validPairs, int incompletePairs, Double score, Double lowerBound, double threshold,
            Regime regime, Integer completedGames, Integer abortedGames, Long samples, Double loss,
            Instant started, Instant completed, Long selfPlayNanos, Long trainingNanos, Long validationNanos, Long totalNanos) {
        this(generation, candidate, incumbent, resultingBest, outcome, decision, wins, draws, losses, validPairs,
                incompletePairs, score, lowerBound, threshold, regime, completedGames, abortedGames, samples, loss,
                started, completed, selfPlayNanos, trainingNanos, validationNanos, totalNanos, null);
    }
    public static GenerationRecord bootstrap(long generation, String candidate, String incumbent, String resultingBest,
            Regime regime, Integer completedGames, Integer abortedGames, Long samples, Double loss,
            Instant started, Instant completed, Long selfPlayNanos, Long trainingNanos, Long validationNanos,
            Long totalNanos, BootstrapEvidence evidence) {
        var decision = evidence.comparison().decision();
        return new GenerationRecord(generation, candidate, incumbent, resultingBest,
                decision == PromotionPolicy.Decision.PROMOTE ? Outcome.PROMOTED : Outcome.RETAINED,
                decision, 0, 0, 0, 0, 0, null, null, 0, regime, completedGames, abortedGames, samples, loss,
                started, completed, selfPlayNanos, trainingNanos, validationNanos, totalNanos, evidence);
    }
    public String validationKind() { return bootstrap == null ? "Game pairs" : "Bootstrap " + bootstrap.supervision().description() + " loss"; }
    public boolean promoted() { return outcome == Outcome.PROMOTED; }
}
