package com.ohinteractive.seedv6.training.validation;

import java.util.List;
import java.util.function.ToDoubleFunction;
import com.ohinteractive.seedv6.core.brn.BrnFeatures;
import com.ohinteractive.seedv6.core.brn1.Brn1Workspace;
import com.ohinteractive.seedv6.core.brn2.Brn2Workspace;
import com.ohinteractive.seedv6.training.model.NetworkModel;
import com.ohinteractive.seedv6.training.selfplay.TrajectorySampler.Sample;

/** Prediction accuracy on the selected target (terminal W/D/L by default), never a playing-strength assessment. */
public final class HeldOutLoss {
    public record Comparison(int samples, double candidateLoss, double bestLoss) {
        public Comparison {
            if (samples < 2 || !Double.isFinite(candidateLoss) || !Double.isFinite(bestLoss)
                    || candidateLoss < 0 || bestLoss < 0) throw new IllegalArgumentException("Invalid held-out loss.");
        }
        /** Strict improvement; exact ties retain the incumbent. */
        public PromotionPolicy.Decision decision() {
            return candidateLoss < bestLoss ? PromotionPolicy.Decision.PROMOTE : PromotionPolicy.Decision.RETAIN_INCUMBENT;
        }
    }
    public static Comparison compare(NetworkModel candidate, NetworkModel best, List<Sample> samples) {
        return compare(candidate, best, samples, Sample::target);
    }
    /** Explicit bounded target objective, shared by both actors; normal callers retain terminal WDL. */
    public static Comparison compare(NetworkModel candidate, NetworkModel best, List<Sample> samples,
                                     ToDoubleFunction<Sample> target) {
        return compare(candidate, best, samples, target, done -> {});
    }
    public static Comparison compare(NetworkModel candidate, NetworkModel best, List<Sample> samples,
                                     ToDoubleFunction<Sample> target, java.util.function.IntConsumer progress) {
        if (candidate.architecture() != best.architecture()) throw new IllegalArgumentException("Student architecture mismatch.");
        return compare(predictor(candidate), predictor(best), samples, target, progress);
    }
    // Both actors traverse the same immutable list, in the same order. No search or score mapping.
    public static Comparison compare(ToDoubleFunction<long[]> candidate, ToDoubleFunction<long[]> best, List<Sample> samples) {
        return compare(candidate, best, samples, Sample::target);
    }
    public static Comparison compare(ToDoubleFunction<long[]> candidate, ToDoubleFunction<long[]> best, List<Sample> samples,
                                     ToDoubleFunction<Sample> target) {
        return compare(candidate, best, samples, target, done -> {});
    }
    public static Comparison compare(ToDoubleFunction<long[]> candidate, ToDoubleFunction<long[]> best, List<Sample> samples,
                                     ToDoubleFunction<Sample> target, java.util.function.IntConsumer progress) {
        double candidateSum = 0, bestSum = 0;
        int completed = 0;
        for (var sample : samples) {
            long[] board = sample.board();
            double value = target.applyAsDouble(sample);
            if (!Double.isFinite(value) || Math.abs(value) > 1)
                throw new IllegalArgumentException("Target must be finite and in [-1,+1].");
            double c = candidate.applyAsDouble(board) - value;
            double b = best.applyAsDouble(board) - value;
            candidateSum += .5 * c * c; bestSum += .5 * b * b;
            if ((++completed & 255) == 0) progress.accept(completed);
        }
        if ((completed & 255) != 0 || completed == 0) progress.accept(completed);
        return new Comparison(samples.size(), candidateSum / samples.size(), bestSum / samples.size());
    }
    private static ToDoubleFunction<long[]> predictor(NetworkModel model) {
        return switch (model) {
            case NetworkModel.Brn b -> {
                var scratch = new BrnFeatures(); yield board -> b.model().evaluate(board, scratch);
            }
            case NetworkModel.Brn1 b -> {
                var scratch = new Brn1Workspace(); yield board -> b.model().evaluate(board, scratch);
            }
            case NetworkModel.Brn2 b -> {
                var scratch = new Brn2Workspace(); yield board -> b.model().evaluate(board, scratch);
            }
            default -> throw new IllegalArgumentException("Held-out bootstrap validation requires a BRN student.");
        };
    }
    private HeldOutLoss() {}
}
