package com.ohinteractive.seedv6.training.checkpoint;

/** Generation-number sampling; no clocks, floating-point logarithms, or filesystem state. */
public final class GenerationRetention {
    public static boolean retain(long generation, long latestCompleted) {
        if (generation < 0 || latestCompleted < 0) throw new IllegalArgumentException("Negative generation.");
        if (generation > latestCompleted) return true;
        long age = latestCompleted - generation;
        long interval = 1;
        // Division avoids overflowing the age threshold, including at Long.MAX_VALUE.
        while (age >= 100) {
            age /= 10;
            interval *= 10;
        }
        return generation % interval == 0;
    }

    private GenerationRetention() {}
}
