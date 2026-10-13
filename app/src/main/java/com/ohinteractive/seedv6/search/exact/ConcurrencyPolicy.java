package com.ohinteractive.seedv6.search.exact;

/** Explicit SR-036 research controls; OFF preserves the inherited coordinator. */
public enum ConcurrencyPolicy {
    OFF, PRIVATE, COOPERATIVE, PLAIN, OWNER, OFFSET, MIXED, ROOT_SPLIT, ROOT_FULL, ROOT_REUSE;

    boolean reservations() { return this != PLAIN && !rootSplit(); }
    boolean transfer() { return this != OFF && this != PRIVATE && !rootSplit(); }
    boolean ownerOnly() { return this == OWNER || this == OFFSET; }
    boolean rootSplit() { return this == ROOT_SPLIT || this == ROOT_FULL || this == ROOT_REUSE; }
}
