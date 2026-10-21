package com.ohinteractive.seedv6.gui;

/** Built-in training choices, mirrored by explicit persisted schema identities. */
enum NetworkArchitecture {
    NNUE_MATERIAL("NNUE (material parity)"), NNUE("NNUE (legacy, no material)"), BRN("BRN-0"), BRN1("BRN-1"), BRN2("BRN-2"), BRN3("BRN-3"), BRN_PAIR2("BRE-Pair 2"),
    BRN_GATED("BRN Gated Pairs (research)"), BRN_CUBIC("BRN Cubic-16 (research)"), BRN_POOLED("BRN Pooled-16 (control)"),
    BRN_GEN32_GATED("Gen32 + Gated Residual (research)"), BRN_GEN32_CUBIC("Gen32 + Cubic Residual (research)"),
    BRN_GEN32_CUBIC_R4("Gen32 + Cubic-4 Residual (research)"),
    BRN_GEN32_QUADRATIC("Gen32 + Quadratic Residual (control)"),
    BRN_GEN32_QUADRATIC_R4("Gen32 + Quadratic-4 Residual (control)"),
    BRN_GEN32_FIXED_QUADRATIC_R4("Fixed Basis + Quadratic-4 Heads (control)"),
    BRN_GEN32_FIXED_CUBIC_R4("Fixed Basis + Cubic-4 Heads (research)");

    private final String label;
    NetworkArchitecture(String label) { this.label = label; }
    @Override public String toString() { return label; }

    com.ohinteractive.seedv6.training.model.TrainingArchitecture trainingArchitecture() {
        return com.ohinteractive.seedv6.training.model.TrainingArchitecture.valueOf(name());
    }

    String optimizerName() {
        if (brnNext() && trainingArchitecture().nextKind().fixedBasis()) return "Head-only Adam";
        return switch (this) { case NNUE, NNUE_MATERIAL, BRN, BRN1, BRN2, BRN_GATED, BRN_CUBIC, BRN_POOLED, BRN_GEN32_GATED, BRN_GEN32_CUBIC, BRN_GEN32_CUBIC_R4, BRN_GEN32_QUADRATIC, BRN_GEN32_QUADRATIC_R4 -> "Adam"; case BRN3, BRN_PAIR2 -> "Masked Adam"; default -> throw new AssertionError(); };
    }

    String evaluationUnits() {
        if (brnNext()) return "BRN successor units (100 = 1 pawn; uncalibrated)";
        return switch (this) { case NNUE -> "Legacy NNUE units (uncalibrated)"; case NNUE_MATERIAL -> "NNUE material + residual units (uncalibrated)"; case BRN, BRN1, BRN2 -> "BRN units (uncalibrated)"; case BRN3 -> "BRN-3 units (100 = 1 pawn)"; case BRN_PAIR2 -> "BRE-Pair 2 units (100 = 1 pawn)"; default -> throw new AssertionError(); };
    }

    /** Presentation scale only; neither a calibrated score nor a win probability. */
    double evaluationBar(int whiteScore) {
        return switch (this) { case NNUE, NNUE_MATERIAL, BRN, BRN1, BRN2 -> 0.5 + 0.48 * Math.tanh(whiteScore / 2000.0); case BRN3, BRN_PAIR2, BRN_GATED, BRN_CUBIC, BRN_POOLED, BRN_GEN32_GATED, BRN_GEN32_CUBIC, BRN_GEN32_CUBIC_R4, BRN_GEN32_QUADRATIC, BRN_GEN32_QUADRATIC_R4, BRN_GEN32_FIXED_QUADRATIC_R4, BRN_GEN32_FIXED_CUBIC_R4 -> 0.5 + 0.48 * Math.tanh(whiteScore / 400.0); };
    }
    boolean corpusOnly(){return trainingArchitecture().corpusOnly();}
    boolean brnNext() { return trainingArchitecture().brnNext(); }
    boolean nnueFamily() { return this == NNUE || this == NNUE_MATERIAL; }
    String folderName() { return trainingArchitecture().folderName(); }
    boolean supportsTrainingData(){return nnueFamily()||this==BRN2||corpusOnly();}
    boolean usesMinibatches(){return nnueFamily()||corpusOnly();}
}
