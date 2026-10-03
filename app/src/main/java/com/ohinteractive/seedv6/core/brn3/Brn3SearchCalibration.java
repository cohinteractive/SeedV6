package com.ohinteractive.seedv6.core.brn3;

/** Frozen play calibration, separate from raw corpus supervision and checkpoint weights.
 * The gain changes only the learned residual; material keeps its original pawn values.
 * Evidence and limits: docs/brn/BRN_LEARNING_CONTRACT.md and BRN_LEARNING_RESEARCH.md. */
public final class Brn3SearchCalibration {
    public static final String ID = "BRN3_MATERIAL_RESIDUAL_QUARTER_V1";
    public static final double RESIDUAL_GAIN = .25;
    private Brn3SearchCalibration() {}
}
