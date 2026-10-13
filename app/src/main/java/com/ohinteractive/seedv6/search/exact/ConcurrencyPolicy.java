package com.ohinteractive.seedv6.search.exact;

/** Explicit SR-036 research controls; OFF preserves the inherited coordinator. */
public enum ConcurrencyPolicy {
    OFF, PRIVATE, COOPERATIVE, PLAIN, OWNER, OFFSET, MIXED;

    boolean reservations() { return this != PLAIN; }
    boolean transfer() { return this != OFF && this != PRIVATE; }
    boolean ownerOnly() { return this == OWNER || this == OFFSET; }
}
