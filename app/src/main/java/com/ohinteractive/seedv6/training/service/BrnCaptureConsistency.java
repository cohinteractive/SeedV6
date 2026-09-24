package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.training.model.TrainingArchitecture;

/** Experimental BRN-2 training objective only; absent legacy selection is OFF. */
public record BrnCaptureConsistency(double lambda) {
    public static final BrnCaptureConsistency OFF = new BrnCaptureConsistency(0);
    public BrnCaptureConsistency {
        if (!Double.isFinite(lambda) || lambda < 0)
            throw new IllegalArgumentException("Capture consistency lambda must be finite and non-negative.");
        if (lambda == 0) lambda = 0; // Canonical positive zero in durable identity.
    }
    public boolean enabled() { return lambda != 0; }
    public void requireSupported(TrainingArchitecture architecture, BrnSupervision supervision, TrainingSource source) {
        if (!enabled()) return;
        if (architecture != TrainingArchitecture.BRN2 || source != null && source.frozen())
            throw new IllegalArgumentException("Capture consistency requires BRN-2 with a pinned NNUE teacher.");
        if (supervision != null && (!supervision.blended() || supervision.teacherWeight() != .5))
            throw new IllegalArgumentException("Capture consistency requires NNUE blended supervision with exactly 50% teacher weight.");
    }
    public String settingsSuffix() { return enabled() ? "|brn-capture-v1:lambda=" + lambda : ""; }
    public String description() {
        return enabled() ? "Capture consistency (experimental): lambda=" + lambda : "Capture consistency: OFF (lambda=0)";
    }
}
