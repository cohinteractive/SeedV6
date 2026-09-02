package com.ohinteractive.seedv6.search.manage;

/** Normalized limits for one managed search generation. */
public record SearchLimits(
    int depth,
    long nodes,
    long timeMillis,
    boolean infinite
) {

    public static final int NO_DEPTH = 0;
    public static final long NO_LIMIT = -1L;

    public SearchLimits {
        if(depth < NO_DEPTH) throw new IllegalArgumentException("Depth must not be negative.");
        if(nodes < NO_LIMIT) throw new IllegalArgumentException("Node limit is invalid.");
        if(timeMillis < NO_LIMIT) throw new IllegalArgumentException("Time limit is invalid.");
        if(infinite && (depth != NO_DEPTH || nodes != NO_LIMIT || timeMillis != NO_LIMIT)) {
            throw new IllegalArgumentException("Infinite cannot be combined with another limit.");
        }
        if(!infinite && depth == NO_DEPTH && nodes == NO_LIMIT && timeMillis == NO_LIMIT) {
            throw new IllegalArgumentException("At least one search limit is required.");
        }
    }

    public boolean pureDepth() {
        return depth != NO_DEPTH && nodes == NO_LIMIT && timeMillis == NO_LIMIT;
    }
}
