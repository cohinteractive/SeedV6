package com.ohinteractive.seedv6.gui;

/** Built-in training choices, mirrored by explicit persisted schema identities. */
enum NetworkArchitecture {
    NNUE_MATERIAL("NNUE (material parity)"), NNUE("NNUE (legacy, no material)"), BRN("BRN-0"), BRN1("BRN-1"), BRN2("BRN-2"), BRN3("BRN-3"), BRN_PAIR2("BRE-Pair 2");

    private final String label;
    NetworkArchitecture(String label) { this.label = label; }
    @Override public String toString() { return label; }

    com.ohinteractive.seedv6.training.model.TrainingArchitecture trainingArchitecture() {
        return com.ohinteractive.seedv6.training.model.TrainingArchitecture.valueOf(name());
    }

    String optimizerName() {
        return switch (this) { case NNUE, NNUE_MATERIAL, BRN, BRN1, BRN2 -> "Adam"; case BRN3, BRN_PAIR2 -> "Masked Adam"; };
    }

    String evaluationUnits() {
        return switch (this) { case NNUE -> "Legacy NNUE units (uncalibrated)"; case NNUE_MATERIAL -> "NNUE material + residual units (uncalibrated)"; case BRN, BRN1, BRN2 -> "BRN units (uncalibrated)"; case BRN3 -> "BRN-3 units (100 = 1 pawn)"; case BRN_PAIR2 -> "BRE-Pair 2 units (100 = 1 pawn)"; };
    }

    /** Presentation scale only; neither a calibrated score nor a win probability. */
    double evaluationBar(int whiteScore) {
        return switch (this) { case NNUE, NNUE_MATERIAL, BRN, BRN1, BRN2 -> 0.5 + 0.48 * Math.tanh(whiteScore / 2000.0); case BRN3, BRN_PAIR2 -> 0.5 + 0.48 * Math.tanh(whiteScore / 400.0); };
    }
    boolean corpusOnly(){return trainingArchitecture().corpusOnly();}
    boolean nnueFamily() { return this == NNUE || this == NNUE_MATERIAL; }
    String folderName() { return trainingArchitecture().folderName(); }
    boolean supportsTrainingData(){return nnueFamily()||this==BRN2||corpusOnly();}
    boolean usesMinibatches(){return nnueFamily()||corpusOnly();}
}
