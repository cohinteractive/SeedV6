package com.ohinteractive.seedv6.training.service;

/** Campaign validation, independent of the actor that generates training positions. */
public enum ValidationMethod {
    GAME_PAIRS("Game Pair"), HELD_OUT("WDL Loss");
    private final String label;
    ValidationMethod(String label) { this.label = label; }
    @Override public String toString() { return label; }
    public static ValidationMethod legacy(TrainingSource source) {
        return source != null && source.bootstrap() ? HELD_OUT : GAME_PAIRS;
    }
}
