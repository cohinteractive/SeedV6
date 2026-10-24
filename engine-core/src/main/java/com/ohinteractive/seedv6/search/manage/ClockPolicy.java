package com.ohinteractive.seedv6.search.manage;

import java.util.Locale;
import java.util.Objects;
import com.ohinteractive.seedv6.search.common.SingleDepthSearch;

/** One selection for both the managed-game-clock budget and completed-iteration policy.
 * Explicit depth/node/movetime/infinite limits are normalized separately and never doubled. */
public enum ClockPolicy {
    BOUNDARY2("boundary2-d6-v1"), FACTUAL("factual");

    public static final String PROPERTY = "seedv6.clockPolicy";
    private final String identity;
    ClockPolicy(String identity) { this.identity = identity; }
    public String identity() { return identity; }

    /** Snapshot at composition time; changing one engine's setting never mutates another. */
    public static ClockPolicy configured() {
        return parse(System.getProperty(PROPERTY, "BOUNDARY2"));
    }
    public static ClockPolicy parse(String value) {
        try { return valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException | NullPointerException invalid) {
            throw new IllegalArgumentException("ClockPolicy must be BOUNDARY2 or FACTUAL", invalid);
        }
    }
    public long allocateClockMillis(long remaining, long increment, int movesToGo) {
        long base = TimeManager.allocateClockMillis(remaining, increment, movesToGo);
        return this == FACTUAL ? base : Math.min(Math.max(0L, remaining - TimeManager.CLOCK_RESERVE_MILLIS),
                TimeManager.saturatedAdd(base, base));
    }
    public SingleDepthSearch apply(SingleDepthSearch owner) {
        Objects.requireNonNull(owner, "owner");
        return this == FACTUAL ? owner : new Boundary2ClockPolicy(owner);
    }
}
