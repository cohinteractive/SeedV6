package com.ohinteractive.seedv6.search.alphabeta;

import java.util.Locale;
import java.util.Objects;

/**
 * Immutable, independently switchable WS13 selective-search policy.
 *
 * <p>{@link #allOff()} is the accepted WS10/WS11 identity path. Production
 * enables only the heuristics retained by the WS13 safety audit. Tests and the
 * deterministic benchmark can enable any retained heuristic alone or remove
 * one heuristic from a cumulative policy without changing search mechanics.</p>
 */
public record SelectiveSearchPolicy(
    boolean mateDistanceBounds,
    boolean razoring,
    boolean futility
) {

    public enum Heuristic {
        MATE_DISTANCE,
        RAZORING,
        FUTILITY;

        public static Heuristic parse(String value) {
            Objects.requireNonNull(value, "value");
            return switch(value.trim().toLowerCase(Locale.ROOT)) {
                case "mate", "mate-distance", "mate_distance" -> MATE_DISTANCE;
                case "razor", "razoring" -> RAZORING;
                case "futility" -> FUTILITY;
                default -> throw new IllegalArgumentException("Unknown WS13 heuristic: " + value);
            };
        }
    }

    public static SelectiveSearchPolicy allOff() {
        return ALL_OFF;
    }

    public static SelectiveSearchPolicy production() {
        return PRODUCTION;
    }

    public static SelectiveSearchPolicy only(Heuristic heuristic) {
        return ALL_OFF.with(heuristic, true);
    }

    public SelectiveSearchPolicy with(Heuristic heuristic, boolean enabled) {
        Objects.requireNonNull(heuristic, "heuristic");
        return switch(heuristic) {
            case MATE_DISTANCE -> new SelectiveSearchPolicy(
                enabled, razoring, futility
            );
            case RAZORING -> new SelectiveSearchPolicy(
                mateDistanceBounds, enabled, futility
            );
            case FUTILITY -> new SelectiveSearchPolicy(
                mateDistanceBounds, razoring, enabled
            );
        };
    }

    public boolean anyEnabled() {
        return mateDistanceBounds || razoring || futility;
    }

    public boolean needsStaticEvaluation() {
        return razoring || futility;
    }

    public String id() {
        if(equals(ALL_OFF)) return "all-off";
        if(equals(PRODUCTION)) return "production";
        final StringBuilder value = new StringBuilder();
        append(value, mateDistanceBounds, "mate");
        append(value, razoring, "razor");
        append(value, futility, "futility");
        return value.toString();
    }

    private static void append(StringBuilder value, boolean enabled, String name) {
        if(!enabled) return;
        if(!value.isEmpty()) value.append(',');
        value.append(name);
    }

    private static final SelectiveSearchPolicy ALL_OFF = new SelectiveSearchPolicy(
        false, false, false
    );
    private static final SelectiveSearchPolicy PRODUCTION = new SelectiveSearchPolicy(
        true, true, true
    );
}
