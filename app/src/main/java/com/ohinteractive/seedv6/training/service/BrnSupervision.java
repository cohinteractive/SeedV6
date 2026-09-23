package com.ohinteractive.seedv6.training.service;

import java.io.*;
import java.util.Objects;
import java.util.function.ToDoubleFunction;
import com.ohinteractive.seedv6.core.nnue.NnueEvaluator;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import com.ohinteractive.seedv6.training.selfplay.TrajectorySampler.Sample;

/** Lineage objective, independent of the BRN feature schema. WDL remains the default. */
public record BrnSupervision(Mode mode, double teacherWeight) {
    public enum Mode {
        WDL("WDL"), NNUE_BLENDED("NNUE blended");
        private final String label;
        Mode(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    public static final BrnSupervision WDL = new BrnSupervision(Mode.WDL, 0);
    public BrnSupervision {
        Objects.requireNonNull(mode);
        if (!Double.isFinite(teacherWeight) || teacherWeight < 0 || teacherWeight > 1
                || mode == Mode.WDL && teacherWeight != 0)
            throw new IllegalArgumentException("NNUE teacher weight must be finite and in [0, 1]; WDL requires zero.");
        if (teacherWeight == 0) teacherWeight = 0; // Canonical positive zero in durable identity.
    }
    public static BrnSupervision blended(double weight) { return new BrnSupervision(Mode.NNUE_BLENDED, weight); }
    public boolean blended() { return mode == Mode.NNUE_BLENDED; }
    public void requireSupported(TrainingArchitecture architecture, TrainingSource source) {
        if (source != null && source.frozen() && (architecture != TrainingArchitecture.BRN2 || blended()))
            throw new IllegalArgumentException("Frozen replay requires canonical teacher-free BRN-2 WDL supervision.");
        if (blended() && (architecture != TrainingArchitecture.BRN2 || source != null && !source.bootstrap()))
            throw new IllegalArgumentException("NNUE blended supervision requires BRN-2 and an external position generator. Select a separate fresh store to change supervision.");
    }
    /** Promoted verbatim arithmetic from the accepted replay: preserve exact endpoints. */
    public double target(double wdl, double teacher) {
        if (teacherWeight == 0) return wdl;
        if (teacherWeight == 1) return teacher;
        return (1 - teacherWeight) * wdl + teacherWeight * teacher;
    }
    /** Exact sampled board and side to move; no search score, calibration or CP mapping. */
    public static double teacherValue(NnueEvaluator teacher, Sample sample) {
        teacher.evaluate(sample.board());
        double value = teacher.boundedValue();
        if (!Double.isFinite(value) || Math.abs(value) > 1) throw new IllegalArgumentException("Invalid teacher value");
        return value;
    }
    public ToDoubleFunction<Sample> targets(NnueEvaluator teacher) {
        if (teacherWeight == 0) return Sample::target;
        Objects.requireNonNull(teacher, "Pinned NNUE teacher");
        return sample -> target(sample.target(), teacherValue(teacher, sample));
    }
    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(mode.name()); out.writeDouble(teacherWeight);
    }
    public static BrnSupervision read(DataInputStream in) throws IOException {
        return new BrnSupervision(Mode.valueOf(in.readUTF()), in.readDouble());
    }
    public String description() {
        return blended() ? "NNUE blended (teacher " + teacherWeight + ", WDL " + (1 - teacherWeight) + ")" : "WDL";
    }
}
