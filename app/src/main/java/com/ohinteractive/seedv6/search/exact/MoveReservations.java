package com.ohinteractive.seedv6.search.exact;

import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Advisory in-flight move ownership, separate from TT score evidence. A collision
 * may change ordering only: every deferred move still needs an ordinary search
 * unless the node obtains an ordinary alpha-beta cutoff.
 */
final class MoveReservations {
    private static final int MASK = 65535;
    private final AtomicLongArray slots = new AtomicLongArray(MASK + 1);

    static long fingerprint(long key, int depth, long move) {
        long value = key ^ Long.rotateLeft(move, 29) ^ ((long) depth * 0x9e3779b97f4a7c15L);
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return (value ^ (value >>> 31)) | 1;
    }

    boolean busy(long token) { return slots.get((int) (token >>> 1) & MASK) == token; }

    long claim(long token) {
        return slots.compareAndSet((int) (token >>> 1) & MASK, 0, token) ? token : 0;
    }

    void release(long token) {
        if(!slots.compareAndSet((int) (token >>> 1) & MASK, token, 0))
            throw new AssertionError("Search move reservation lost its owner.");
    }

    /** Quiescent validation only; no table scan in the production request path. */
    boolean isEmpty() {
        for(int i = 0; i < slots.length(); i++) if(slots.get(i) != 0) return false;
        return true;
    }
}
