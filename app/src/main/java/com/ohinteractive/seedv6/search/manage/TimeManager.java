package com.ohinteractive.seedv6.search.manage;

/** Conservative deterministic WS4 time allocation and overflow-safe units. */
public final class TimeManager {

    public static final int DEFAULT_MOVES_TO_GO = 30;
    public static final long CLOCK_RESERVE_MILLIS = 50L;
    public static final long ENGINE_OVERHEAD_MILLIS = 10L;

    public static long movetimeBudgetMillis(long requestedMillis) {
        requireNonNegative(requestedMillis, "movetime");
        return subtractFloorZero(requestedMillis, ENGINE_OVERHEAD_MILLIS);
    }

    public static long allocateClockMillis(long remainingMillis, long incrementMillis, int movesToGo) {
        requireNonNegative(remainingMillis, "remaining time");
        requireNonNegative(incrementMillis, "increment");
        if(movesToGo < 1) throw new IllegalArgumentException("Moves to go must be positive.");

        final long safelyUsable = subtractFloorZero(remainingMillis, CLOCK_RESERVE_MILLIS);
        final long clockShare = remainingMillis / movesToGo;
        final long incrementShare = incrementMillis - incrementMillis / 4L;
        final long raw = saturatedAdd(clockShare, incrementShare);
        final long afterOverhead = subtractFloorZero(raw, ENGINE_OVERHEAD_MILLIS);
        return Math.min(safelyUsable, afterOverhead);
    }

    public static long millisToNanos(long millis) {
        requireNonNegative(millis, "milliseconds");
        if(millis > Long.MAX_VALUE / NANOS_PER_MILLI) return Long.MAX_VALUE;
        return millis * NANOS_PER_MILLI;
    }

    public static long saturatedAdd(long left, long right) {
        if(left < 0L || right < 0L) throw new IllegalArgumentException("Values must be non-negative.");
        if(left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }

    private static final long NANOS_PER_MILLI = 1_000_000L;

    private TimeManager() {}

    private static long subtractFloorZero(long value, long deduction) {
        return value <= deduction ? 0L : value - deduction;
    }

    private static void requireNonNegative(long value, String name) {
        if(value < 0L) throw new IllegalArgumentException(name + " must not be negative.");
    }
}
