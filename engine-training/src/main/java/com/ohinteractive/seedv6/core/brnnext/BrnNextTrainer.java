package com.ohinteractive.seedv6.core.brnnext;

import java.io.*;
import java.util.*;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import com.ohinteractive.seedv6.core.brn3.Brn3Objective;
import com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets;
import static com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.*;

/** Bounded dense Adam over compact layouts. Single owner; exact binary64 continuation. */
public final class BrnNextTrainer {
    private final BrnNextArchitecture architecture;
    final double[] weights, first, second, gradient;
    private final BrnNextModel.Workspace workspace;
    private double rate;
    private long step;

    public BrnNextTrainer(BrnNextArchitecture architecture, long seed, double rate) {
        this.architecture = Objects.requireNonNull(architecture); setLearningRate(rate);
        int n = architecture.parameters();
        weights = new double[n]; first = new double[n]; second = new double[n]; gradient = new double[n];
        workspace = new BrnNextModel.Workspace(architecture);
        var random = new SplittableRandom(seed);
        // All residual heads/tables/unary terms start at zero: Gen0 is fixed material.
        // Nonzero embeddings avoid the all-zero multiplicative stationary point.
        for (int i = EMBEDDING; i < architecture.head(); i++) weights[i] = random.nextDouble(-.5, .5);
    }
    public BrnNextArchitecture architecture() { return architecture; }
    public double rate() { return rate; }
    public long step() { return step; }
    public void setLearningRate(double rate) { new BrnAdamConfig(rate); this.rate = rate; }
    public BrnNextModel snapshot() { return new BrnNextModel(architecture, weights); }
    public double predictPawns(long[] board) { return workspace.forward(weights, board, architecture); }
    public double predictOutcome(long[] board) { return Brn3Objective.outcome(predictPawns(board), board); }

    /** Adds the analytic derivative of the raw pawn value. Forward must precede this. */
    void backward(double derivative) {
        gradient[0] += derivative;
        int width = architecture.width(), head = architecture.head();
        for (int i = 0; i < workspace.count; i++) gradient[UNARY + workspace.features[i]] += derivative * PIECE_NORM;
        switch (architecture) {
            case CUBIC -> {
                for (int r = 0; r < width; r++) {
                    gradient[head + r] += derivative * workspace.second[r] * DEGREE2_NORM;
                    gradient[head + width + r] += derivative * workspace.third[r] * DEGREE3_NORM;
                    for (int i = 0; i < workspace.count; i++) {
                        int address = EMBEDDING + workspace.features[i] * width + r;
                        double excludingFirst = workspace.first[r] - weights[address];
                        double excludingSecond = workspace.second[r] - weights[address] * excludingFirst;
                        gradient[address] += derivative * (weights[head + r] * excludingFirst * DEGREE2_NORM
                                + weights[head + width + r] * excludingSecond * DEGREE3_NORM);
                    }
                }
            }
            case POOLED -> {
                for (int r = 0; r < width; r++) {
                    double value = workspace.output[r];
                    gradient[head + width + r] += derivative * value;
                    double delta = derivative * weights[head + width + r] * (1 - value * value);
                    gradient[head + r] += delta;
                    for (int i = 0; i < workspace.count; i++)
                        gradient[EMBEDDING + workspace.features[i] * width + r] += delta * PIECE_NORM;
                }
            }
            case GATED_PAIRS -> {
                double mean = 0;
                for (int r = 0; r < width; r++) mean += workspace.gates[r] * workspace.output[r];
                for (int r = 0; r < width; r++) {
                    double delta = derivative * workspace.gates[r] * (workspace.output[r] - mean);
                    gradient[head + r] += delta;
                    for (int i = 0; i < workspace.count; i++)
                        gradient[EMBEDDING + workspace.features[i] * width + r] += delta * PIECE_NORM;
                    int base = head + width + r * PAIR_STATES;
                    for (int address : workspace.pairs) gradient[base + address] += derivative * workspace.gates[r] * PAIR_NORM;
                }
            }
        }
    }
    public double trainBatch(long[][] boards, double[] targets, int count) {
        if (count < 1 || count > boards.length || count > targets.length) throw new IllegalArgumentException("Invalid batch");
        for (int i = 0; i < count; i++) if (boards[i] == null || !Double.isFinite(targets[i]) || Math.abs(targets[i]) > 1)
            throw new IllegalArgumentException("Invalid example");
        if (step == Long.MAX_VALUE) throw new ArithmeticException("Optimizer step exhausted");
        Arrays.fill(gradient, 0); double loss = 0;
        for (int i = 0; i < count; i++) {
            double[] objective = Brn3Objective.crossEntropy(predictPawns(boards[i]), NnueCorpusTargets.material(boards[i]), targets[i]);
            loss += objective[0]; backward(objective[1]);
        }
        // Validate the entire gradient before touching optimizer state.
        for (double g : gradient) if (!Double.isFinite(g)) throw new ArithmeticException("Nonfinite BRN-next gradient");
        step++;
        double c1 = 1 - Math.pow(.9, step), c2 = 1 - Math.pow(.999, step);
        for (int i = 0; i < weights.length; i++) {
            double g = gradient[i] / count;
            first[i] = .9 * first[i] + .1 * g; second[i] = .999 * second[i] + .001 * g * g;
            weights[i] -= rate * (first[i] / c1) / (Math.sqrt(second[i] / c2) + 1e-8);
            if (!Double.isFinite(weights[i])) throw new ArithmeticException("Nonfinite BRN-next update");
        }
        return loss / count;
    }
    public void write(OutputStream target) throws IOException {
        var bytes = new ByteArrayOutputStream(BrnNextCodec.trainingBytes(architecture));
        var out = new DataOutputStream(bytes); BrnNextCodec.header(out, BrnNextCodec.STATE_MAGIC, architecture);
        out.writeLong(step); out.writeDouble(rate);
        for (double[] values : new double[][]{weights, first, second}) for (double v : values) out.writeDouble(v);
        BrnNextCodec.finish(bytes, out, target);
    }
    public static BrnNextTrainer read(BrnNextArchitecture architecture, InputStream input) throws IOException {
        var in = BrnNextCodec.checked(input, BrnNextCodec.STATE_MAGIC, architecture, BrnNextCodec.trainingBytes(architecture));
        long step = in.readLong(); double rate = BrnNextCodec.finite(in);
        if (step < 0) throw new IOException("Negative BRN-next optimizer step");
        final BrnNextTrainer trainer;
        try { trainer = new BrnNextTrainer(architecture, 0, rate); }
        catch (IllegalArgumentException invalid) { throw new IOException("Invalid BRN-next optimizer configuration", invalid); }
        trainer.step = step;
        for (double[] values : new double[][]{trainer.weights, trainer.first, trainer.second})
            for (int i = 0; i < values.length; i++) values[i] = BrnNextCodec.finite(in);
        for (double value : trainer.second) if (value < 0) throw new IOException("Negative BRN-next second moment");
        return trainer;
    }
}
