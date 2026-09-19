package com.ohinteractive.seedv6.tools.nnue.research;

import java.util.*;
import java.util.function.IntToDoubleFunction;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.validation.*;

/** Observations only. Quantiles are exact nearest ranks of finite absolute values, one tensor at a time. */
public final class NetworkHealth {
    public record Distribution(long finite, long nonfinite, double min, double max, double mean, double rms,
                               double stddev, double absMedian, double absP95, double absP99) {}
    public record Activations(long zero, long interior, long one, long nonfinite) {
        public long count() { return zero + interior + one + nonfinite; }
        public double zeroPercent() { return 100.0 * zero / count(); }
        public double interiorPercent() { return 100.0 * interior / count(); }
        public double onePercent() { return 100.0 * one / count(); }
        @Override public String toString() {
            return "Activations[zero=" + zero + ", interior=" + interior + ", one=" + one + ", nonfinite=" + nonfinite
                    + ", zeroPercent=" + zeroPercent() + ", interiorPercent=" + interiorPercent() + ", onePercent=" + onePercent() + "]";
        }
    }
    public record Report(Map<String, Distribution> parameters, Activations transformer, Activations hidden,
                         Distribution raw, Distribution prediction, double saturationPercent,
                         double positivePercent, double negativePercent, double nearZeroPercent) {
        public Report { parameters = Collections.unmodifiableMap(new LinkedHashMap<>(parameters)); }
        public double parameterRms() {
            double squares = 0; long count = 0;
            for (var tensor : parameters.values()) { squares += tensor.rms() * tensor.rms() * tensor.finite(); count += tensor.finite(); }
            return Math.sqrt(squares / count);
        }
    }
    public record Distance(long parameters, double l2, double rms, double maximumAbsolute) {}
    record Tensor(String name, int size, IntToDoubleFunction value) {}
    static List<Tensor> tensors(NnueNetwork n) {
        return List.of(new Tensor("transformerWeights", NnueNetwork.FEATURE_WEIGHT_COUNT, i -> n.featureWeight(i / 64, i % 64)),
                new Tensor("transformerBias", 64, n::featureBias),
                new Tensor("hiddenWeights", NnueNetwork.HIDDEN_WEIGHT_COUNT, i -> n.hiddenWeight(i / 128, i % 128)),
                new Tensor("hiddenBias", 32, n::hiddenBias), new Tensor("outputWeights", 32, n::outputWeight),
                new Tensor("outputBias", 1, i -> n.outputBias()));
    }
    static Distribution distribution(int size, IntToDoubleFunction f) {
        double[] absolute = new double[size];
        long bad = 0; int count = 0;
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY, mean = 0, m2 = 0, squares = 0;
        for (int i = 0; i < size; i++) {
            double v = f.applyAsDouble(i);
            if (!Double.isFinite(v)) { bad++; continue; }
            absolute[count++] = Math.abs(v);
            min = Math.min(min, v); max = Math.max(max, v); squares += v * v;
            double delta = v - mean; mean += delta / count; m2 += delta * (v - mean);
        }
        Arrays.sort(absolute, 0, count);
        return new Distribution(count, bad, count == 0 ? Double.NaN : min, count == 0 ? Double.NaN : max,
                count == 0 ? Double.NaN : mean, Math.sqrt(squares / count), Math.sqrt(Math.max(0, m2 / count)),
                quantile(absolute, count, 0.5), quantile(absolute, count, 0.95), quantile(absolute, count, 0.99));
    }
    private static double quantile(double[] a, int n, double p) { return n == 0 ? Double.NaN : a[Math.max(0, (int) Math.ceil(p * n) - 1)]; }
    public static Distance distance(NnueNetwork a, NnueNetwork b) {
        var first = tensors(a); var second = tensors(b);
        double squares = 0, max = 0; long count = 0;
        for (int t = 0; t < first.size(); t++) for (int i = 0; i < first.get(t).size(); i++) {
            double d = first.get(t).value().applyAsDouble(i) - second.get(t).value().applyAsDouble(i);
            squares += d * d; max = Math.max(max, Math.abs(d)); count++;
        }
        return new Distance(count, Math.sqrt(squares), Math.sqrt(squares / count), max);
    }
    public static List<long[]> corpus(long seed, int count) {
        if (count < 1) throw new IllegalArgumentException("Empty corpus.");
        var config = new ValidationConfig(count, seed, 0, 120, 1, 1, NnueScoreMapping.V1, 1024);
        long[] board = Board.startingPosition();
        List<long[]> result = new ArrayList<>();
        for (int i = 0; i < count; i++) result.add(ValidationArena.opening(board, GameHistory.initial(board), config, i).board());
        return List.copyOf(result);
    }
    public static Report analyze(NnueNetwork network, List<long[]> corpus) {
        if (corpus.isEmpty()) throw new IllegalArgumentException("Empty corpus.");
        Map<String, Distribution> parameters = new LinkedHashMap<>();
        for (var t : tensors(network)) parameters.put(t.name(), distribution(t.size(), t.value()));
        NnueEvaluator inference = new NnueEvaluator(network);
        long[] transformer = new long[4], hidden = new long[4];
        double[] raw = new double[corpus.size()], values = new double[corpus.size()];
        int saturated = 0, positive = 0, negative = 0, nearZero = 0;
        for (int p = 0; p < corpus.size(); p++) {
            raw[p] = inference.evaluate(corpus.get(p)); values[p] = inference.boundedValue();
            for (int colour : new int[]{Value.WHITE, Value.BLACK}) for (int i = 0; i < 64; i++) count(transformer, inference.accumulator(colour, i));
            for (int i = 0; i < 32; i++) count(hidden, inference.hidden(i));
            double value = values[p];
            if (Double.isFinite(value)) {
                if (Math.abs(value) >= 0.99) saturated++;
                if (Math.abs(value) <= 1e-6) nearZero++; else if (value > 0) positive++; else negative++;
            }
        }
        double percent = 100.0 / corpus.size();
        return new Report(parameters, activation(transformer), activation(hidden), distribution(raw.length, i -> raw[i]),
                distribution(values.length, i -> values[i]), saturated * percent, positive * percent, negative * percent, nearZero * percent);
    }
    private static void count(long[] counts, double v) { counts[!Double.isFinite(v) ? 3 : v == 0 ? 0 : v == 1 ? 2 : 1]++; }
    private static Activations activation(long[] a) { return new Activations(a[0], a[1], a[2], a[3]); }
    private NetworkHealth() {}
}
