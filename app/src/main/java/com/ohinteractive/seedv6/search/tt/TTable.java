package com.ohinteractive.seedv6.search.tt;

import java.util.Arrays;

public class TTable {

    public static final class TEntry {
        public long data;
        public long hashMove;
    }

    static final class StripeLock {
        /*
         * Each StripeLock object has unused padding to separate it from other
         * nearby StripeLock objects in memory to assist with cache-line collisions
         * A cache-line collision means that two variables being worked on in
         * separate threads might exist in the same cache-line, usually a
         * 64 byte block, and they will get
         * synchronized so only one can be worked on at a time.
         * Adding padding means that each StripeLock has enough room not to be
         * synchronized by the CPU and they are almost guaranteed to be able to
         * work independently
         */

        @SuppressWarnings("unused")
        private volatile long
        p0, p1, p2, p3, p4, p5, p6,
        p7, p8, p9, p10, p11, p12, p13;

    }
    
    public static final int TYPE_EVAL = 3;
    public static final int TYPE_EXACT = 0;
    public static final int TYPE_LOWER = 1;
    public static final int TYPE_UPPER = 2;

    public TTable() {
        this(DEFAULT_TABLE_SIZE_IN_MB);
    }

    public TTable(int sizeInMb) {
        int entrySizeInBytes = 24; // three longs, key + data + hashMove
        int totalBytes = sizeInMb * 1024 * 1024;
        int rawEntries = totalBytes / entrySizeInBytes;
        int entryCount = Integer.highestOneBit(rawEntries);
        this.key = new long[entryCount];
        this.data = new long[entryCount];
        this.hashMove = new long[entryCount];
        this.indexMask = entryCount - 1;
        this.locks = new StripeLock[STRIPE_COUNT];
        for(int i = 0; i < STRIPE_COUNT; i ++) {
            locks[i] = new StripeLock();
        }
        this.generation = 0;
    }

    public void advanceGeneration() {
        generation ++;
        if((generation & GENERATION_BITS) == 0) clear();
    }

    public boolean probe(long key, TEntry entry) {
        int index = (int) key & indexMask;
        int stripe = index & STRIPE_MASK;
        synchronized (locks[stripe]) {
            if(this.key[index] != key) return false;
            long data = this.data[index];
            if((data & VALID_MASK) == 0) return false;
            entry.data = data;
            entry.hashMove = this.hashMove[index];
            return true;
        }
    }

    public boolean probeEval(long key, TEntry entry) {
        int index = (int) key & indexMask;
        int stripe = index & STRIPE_MASK;
        synchronized (locks[stripe]) {
            if(this.key[index] != key) return false;
            entry.data = this.data[index];
            return true;
        }
    }

    public void save(long key, int depth, int type, int score, long hashMove) {
        int index = (int) key & indexMask;
        int stripe = index & (STRIPE_MASK);
        synchronized (locks[stripe]) {
            if(type == TYPE_EVAL) {
                this.key[index] = key;
                this.data[index] = score;
                return;
            }
            long existingData = this.data[index];
            if((existingData & VALID_MASK) != 0) {
                long existingKey = this.key[index];
                int existingDepth = (int) (existingData & DEPTH_BITS);
                int existingType = (int) (existingData >>> TYPE_SHIFT & TYPE_BITS);
                int existingGen = (int) (existingData >>> GENERATION_SHIFT & GENERATION_BITS);
                if(key != existingKey && existingGen == (this.generation & GENERATION_BITS) && depth <= existingDepth && type >= existingType) return;
                if(key == existingKey && depth <= existingDepth && type >= existingType) return;
            }
            this.key[index] = key;
            this.data[index] = ((long) score << SCORE_SHIFT) | ((long) (generation & GENERATION_BITS) << GENERATION_SHIFT) | ((long) (type & TYPE_BITS) << TYPE_SHIFT) | (depth & DEPTH_BITS) | VALID_MASK;
            this.hashMove[index] = hashMove;
        }
    }

    public void clear() {
        Arrays.fill(this.data, 0L); 
    }

    private static final long DEPTH_BITS = 0xffL;
    private static final int TYPE_SHIFT = 8;
    private static final long TYPE_BITS = 0b11L;
    private static final int GENERATION_SHIFT = 10;
    private static final long GENERATION_BITS = 0xffL;
    private static final int VALID_SHIFT = 18;
    private static final long VALID_MASK = 1L << VALID_SHIFT;
    private static final int SCORE_SHIFT = 32;

    private static final int DEFAULT_TABLE_SIZE_IN_MB = 192;
    private static final int STRIPE_COUNT = 32;
    private static final int STRIPE_MASK = STRIPE_COUNT - 1;

    private final long[] key;
    private final long[] data;
    private final long[] hashMove;
    private final StripeLock[] locks;
    private final int indexMask;
    private int generation;

}
