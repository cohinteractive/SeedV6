package com.ohinteractive.seedv6.search.exact;

/** Portable user preference. Zero means Max; concrete engine APIs still receive positive counts. */
public record SearchThreads(int requested) {
    public static final int MAX = 0;
    public SearchThreads {
        if (requested < 0 || requested > ParallelSearch.MAX_WORKERS)
            throw new IllegalArgumentException("Search threads must be Max or 1.." + ParallelSearch.MAX_WORKERS);
    }
    public static int available() { return available(Runtime.getRuntime().availableProcessors()); }
    public static int available(int logicalProcessors) {
        return Math.max(1, Math.min(logicalProcessors, ParallelSearch.MAX_WORKERS));
    }
    public int resolve() { return resolve(Runtime.getRuntime().availableProcessors()); }
    public int resolve(int logicalProcessors) {
        return requested == MAX ? available(logicalProcessors) : Math.min(requested, available(logicalProcessors));
    }
}
