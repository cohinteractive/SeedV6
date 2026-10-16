package com.ohinteractive.seedv6.gui;

/** Common selection identity. Empty means explicitly follow Best at the next load. */
record ModelChoice(String checkpointId) {
    static final ModelChoice BEST = new ModelChoice("");
    @Override public String toString() {
        return checkpointId.isEmpty() ? "Best" : "Gen " + TrainingProgress.generation(java.util.OptionalLong.empty(), checkpointId);
    }
}
