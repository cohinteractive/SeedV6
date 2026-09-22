package com.ohinteractive.seedv6.gui;

/** Built-in training choices, mirrored by explicit persisted schema identities. */
enum NetworkArchitecture {
    NNUE("NNUE"), BRN("BRN-0"), BRN1("BRN-1"), BRN2("BRN-2");

    private final String label;
    NetworkArchitecture(String label) { this.label = label; }
    @Override public String toString() { return label; }

    com.ohinteractive.seedv6.training.model.TrainingArchitecture trainingArchitecture() {
        return com.ohinteractive.seedv6.training.model.TrainingArchitecture.valueOf(name());
    }

    String optimizerName() {
        return switch (this) { case NNUE, BRN, BRN1, BRN2 -> "Adam"; };
    }

    String evaluationUnits() {
        return switch (this) { case NNUE -> "NNUE units (uncalibrated)"; case BRN, BRN1, BRN2 -> "BRN units (uncalibrated)"; };
    }

    /** Presentation scale only; neither a calibrated score nor a win probability. */
    double evaluationBar(int whiteScore) {
        return switch (this) { case NNUE, BRN, BRN1, BRN2 -> 0.5 + 0.48 * Math.tanh(whiteScore / 2000.0); };
    }
}
