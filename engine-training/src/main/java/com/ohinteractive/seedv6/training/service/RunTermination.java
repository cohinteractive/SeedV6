package com.ohinteractive.seedv6.training.service;

/** Explicit run policy over the two historical persistence fields; both bounds remain compatible. */
public record RunTermination(long generations, long millis) {
    public enum Kind {
        UNLIMITED("Unlimited / Until Stopped"), GENERATIONS("Fixed Number of Generations"), TIME_BUDGET("Time budget"),
        COMBINED("Generations or time");
        private final String label;
        Kind(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    public RunTermination {
        if (generations < 0 || millis < 0) throw new IllegalArgumentException("Negative run limit");
    }
    public Kind kind() {
        return generations == 0 ? millis == 0 ? Kind.UNLIMITED : Kind.TIME_BUDGET
                : millis == 0 ? Kind.GENERATIONS : Kind.COMBINED;
    }
    public static RunTermination selected(Kind kind, long generations, long millis) {
        java.util.Objects.requireNonNull(kind, "kind");
        if ((kind == Kind.GENERATIONS || kind == Kind.COMBINED) && generations < 1
                || (kind == Kind.TIME_BUDGET || kind == Kind.COMBINED) && millis < 1)
            throw new IllegalArgumentException("Choose a positive run limit.");
        return new RunTermination(kind == Kind.GENERATIONS || kind == Kind.COMBINED ? generations : 0,
                kind == Kind.TIME_BUDGET || kind == Kind.COMBINED ? millis : 0);
    }
}
