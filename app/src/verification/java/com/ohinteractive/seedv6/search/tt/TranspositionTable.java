package com.ohinteractive.seedv6.search.tt;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Direct-mapped search transposition table keyed by the authoritative raw
 * {@code Board.KEY} value.
 *
 * <p>The table stores complete keys and complete V6 move values in unpacked
 * primitive arrays. A separate validity byte means zero-filled storage is
 * empty and a genuine key value of zero remains representable. Every probe
 * uses the low {@code log2(capacity)} key bits as its array index and compares
 * the complete stored 64-bit key for verification. Every probe
 * and store locks the entry's stripe. A writer publishes all fields before
 * unlocking; a later reader of that stripe acquires the same lock and
 * therefore observes one coherent entry under the Java memory model. Writers
 * to a colliding slot serialize, and readers may safely miss or wait for an
 * entry being changed but cannot assemble fields from different writes.</p>
 *
 * <p>Top-level ownership is explicit: call {@link #advanceGeneration()} once
 * before each top-level search. Old entries remain probeable but are preferred
 * for replacement. Generation wraps from 255 to 0 and clears the table so an
 * ancient entry cannot masquerade as current. {@link #newGame()} clears the
 * table and resets generation to zero; a future UCI owner must call it for
 * {@code ucinewgame}. No current exact-oracle owner is artificially coupled to
 * this facility.</p>
 *
 * <p>Depth is an unpacked non-negative node-local remaining-search depth.
 * There is deliberately no quiescence-depth encoding in WS7.</p>
 */
public final class TranspositionTable {

    public enum Bound {
        /** Stored value equals the searched node value. */
        EXACT,
        /** True searched value is at least the stored value (fail high). */
        LOWER,
        /** True searched value is at most the stored value (fail low). */
        UPPER
    }

    public enum Cacheability {
        /** The value depends only on the position represented by the raw key. */
        POSITION_ONLY,
        /**
         * The value depends on search history or rule state outside the raw key.
         * Repetition and 50-move adjudications must use this value.
         */
        PATH_DEPENDENT
    }

    public enum StoreOutcome {
        STORED,
        NOT_CACHEABLE,
        RETAINED_EXISTING
    }

    public enum ProbeOutcome {
        EMPTY,
        KEY_MISMATCH,
        DEPTH_INSUFFICIENT,
        BOUND_UNUSABLE,
        EXACT_HIT,
        LOWER_HIT,
        UPPER_HIT
    }

    /** Reusable allocation-free output holder for {@link #probe}. */
    public static final class Probe {

        public ProbeOutcome outcome() {
            return outcome;
        }

        public boolean keyMatches() {
            return switch(outcome) {
                case EMPTY, KEY_MISMATCH -> false;
                default -> true;
            };
        }

        public boolean scoreUsable() {
            return switch(outcome) {
                case EXACT_HIT, LOWER_HIT, UPPER_HIT -> true;
                default -> false;
            };
        }

        public long key() {
            return key;
        }

        /** Score converted to the probing root ply. */
        public int score() {
            return score;
        }

        /** Node-local remaining depth that backs the stored value. */
        public int depth() {
            return depth;
        }

        public Bound bound() {
            return bound;
        }

        /** Full authoritative V6 move value; an ordering hint, never legality proof. */
        public long move() {
            return move;
        }

        public int generation() {
            return generation;
        }

        public boolean currentGeneration() {
            return currentGeneration;
        }

        private ProbeOutcome outcome = ProbeOutcome.EMPTY;
        private long key;
        private int score;
        private int depth;
        private Bound bound;
        private long move;
        private int generation;
        private boolean currentGeneration;

        private void noMatch(ProbeOutcome newOutcome) {
            outcome = newOutcome;
            key = 0L;
            score = 0;
            depth = 0;
            bound = null;
            move = 0L;
            generation = 0;
            currentGeneration = false;
        }

        private void match(
            ProbeOutcome newOutcome, long newKey, int newScore, int newDepth,
            Bound newBound, long newMove, int newGeneration, boolean isCurrent
        ) {
            outcome = newOutcome;
            key = newKey;
            score = newScore;
            depth = newDepth;
            bound = newBound;
            move = newMove;
            generation = newGeneration;
            currentGeneration = isCurrent;
        }
    }

    public TranspositionTable() {
        this(DEFAULT_ENTRY_COUNT);
    }

    /**
     * Creates a table with a power-of-two number of direct-mapped entries.
     * Small capacities, including one, are supported for deterministic tests.
     */
    public TranspositionTable(int entryCount) {
        if(entryCount <= 0 || Integer.bitCount(entryCount) != 1) {
            throw new IllegalArgumentException(
                "Transposition-table entry count must be a positive power of two: " + entryCount
            );
        }
        keys = new long[entryCount];
        scores = new int[entryCount];
        depths = new int[entryCount];
        moves = new long[entryCount];
        bounds = new byte[entryCount];
        generations = new byte[entryCount];
        valid = new byte[entryCount];
        indexMask = entryCount - 1;

        final int lockCount = Math.min(MAX_STRIPES, entryCount);
        stripes = new ReentrantLock[lockCount];
        for(int i = 0; i < lockCount; i ++) stripes[i] = new ReentrantLock();
        stripeMask = lockCount - 1;
    }

    public int capacity() {
        return keys.length;
    }

    public int generation() {
        return generation;
    }

    /**
     * Advances the unsigned eight-bit generation. The wrap transition clears
     * every entry before generation zero becomes visible.
     */
    public void advanceGeneration() {
        lockAllStripes();
        try {
            final int next = (generation + 1) & GENERATION_MASK;
            if(next == 0) clearArrays();
            generation = next;
        } finally {
            unlockAllStripes();
        }
    }

    /** Clears all entries without changing the current generation. */
    public void clear() {
        lockAllStripes();
        try {
            clearArrays();
        } finally {
            unlockAllStripes();
        }
    }

    /** UCI new-game policy: clear every entry and restart generation at zero. */
    public void newGame() {
        lockAllStripes();
        try {
            clearArrays();
            generation = 0;
        } finally {
            unlockAllStripes();
        }
    }

    /**
     * Stores a search result after converting mate distance at {@code ply}.
     * No overload omits cacheability: repetition and 50-move results, and any
     * result conservatively derived from them, must use
     * {@link Cacheability#PATH_DEPENDENT} and are not stored.
     */
    public StoreOutcome store(
        long key, int depth, Bound bound, int score, int ply, long move,
        Cacheability cacheability
    ) {
        Objects.requireNonNull(bound, "bound");
        Objects.requireNonNull(cacheability, "cacheability");
        if(depth < 0) throw new IllegalArgumentException("Stored depth must not be negative: " + depth);
        final int storedScore = TranspositionScores.toTableScore(score, ply);
        if(cacheability == Cacheability.PATH_DEPENDENT) return StoreOutcome.NOT_CACHEABLE;

        final int index = index(key);
        final ReentrantLock stripe = stripe(index);
        stripe.lock();
        try {
            final int current = generation;
            if(valid[index] != 0 && !shouldReplace(index, key, depth, bound, current)) {
                return StoreOutcome.RETAINED_EXISTING;
            }
            keys[index] = key;
            scores[index] = storedScore;
            depths[index] = depth;
            moves[index] = move;
            bounds[index] = (byte) bound.ordinal();
            generations[index] = (byte) current;
            valid[index] = 1;
            return StoreOutcome.STORED;
        } finally {
            stripe.unlock();
        }
    }

    /**
     * Probes one direct-mapped slot. A key match always exposes the stored move
     * and metadata, including when depth or bound is unusable. Callers must
     * validate that move against current authoritative legal generation before
     * yielding or applying it.
     */
    public ProbeOutcome probe(
        long key, int requiredDepth, int alpha, int beta, int ply, Probe destination
    ) {
        Objects.requireNonNull(destination, "destination");
        if(requiredDepth < 0) {
            throw new IllegalArgumentException("Required depth must not be negative: " + requiredDepth);
        }
        if(alpha >= beta) {
            throw new IllegalArgumentException(
                "Probe window must satisfy alpha < beta: alpha=" + alpha + ", beta=" + beta
            );
        }
        TranspositionScores.requirePly(ply);

        final int index = index(key);
        final ReentrantLock stripe = stripe(index);
        stripe.lock();
        try {
            if(valid[index] == 0) {
                destination.noMatch(ProbeOutcome.EMPTY);
                return ProbeOutcome.EMPTY;
            }
            if(keys[index] != key) {
                destination.noMatch(ProbeOutcome.KEY_MISMATCH);
                return ProbeOutcome.KEY_MISMATCH;
            }

            final int score = TranspositionScores.fromTableScore(scores[index], ply);
            final int depth = depths[index];
            final Bound bound = BOUND_VALUES[bounds[index]];
            final int entryGeneration = generations[index] & GENERATION_MASK;
            final ProbeOutcome outcome;
            if(depth < requiredDepth) {
                outcome = ProbeOutcome.DEPTH_INSUFFICIENT;
            } else {
                outcome = switch(bound) {
                    case EXACT -> ProbeOutcome.EXACT_HIT;
                    case LOWER -> score >= beta
                        ? ProbeOutcome.LOWER_HIT
                        : ProbeOutcome.BOUND_UNUSABLE;
                    case UPPER -> score <= alpha
                        ? ProbeOutcome.UPPER_HIT
                        : ProbeOutcome.BOUND_UNUSABLE;
                };
            }
            destination.match(
                outcome, keys[index], score, depth, bound, moves[index], entryGeneration,
                entryGeneration == generation
            );
            return outcome;
        } finally {
            stripe.unlock();
        }
    }

    /** Logical primitive payload per entry, excluding array headers and lock stripes. */
    public static final int LOGICAL_BYTES_PER_ENTRY = Long.BYTES * 2 + Integer.BYTES * 2 + 3;

    private static final int DEFAULT_ENTRY_COUNT = 1 << 20;
    private static final int MAX_STRIPES = 64;
    private static final int GENERATION_MASK = 0xff;
    private static final Bound[] BOUND_VALUES = Bound.values();

    private final long[] keys;
    private final int[] scores;
    private final int[] depths;
    private final long[] moves;
    private final byte[] bounds;
    private final byte[] generations;
    private final byte[] valid;
    private final ReentrantLock[] stripes;
    private final int indexMask;
    private final int stripeMask;
    private volatile int generation;

    private int index(long key) {
        return (int) key & indexMask;
    }

    private ReentrantLock stripe(int index) {
        return stripes[index & stripeMask];
    }

    private boolean shouldReplace(int index, long key, int depth, Bound bound, int current) {
        final int existingGeneration = generations[index] & GENERATION_MASK;
        if(existingGeneration != current) return true;

        final int existingDepth = depths[index];
        if(depth != existingDepth) return depth > existingDepth;

        final Bound existingBound = BOUND_VALUES[bounds[index]];
        final boolean newExact = bound == Bound.EXACT;
        final boolean existingExact = existingBound == Bound.EXACT;
        if(newExact != existingExact) return newExact;

        return keys[index] == key;
    }

    private void lockAllStripes() {
        for(ReentrantLock stripe : stripes) stripe.lock();
    }

    private void unlockAllStripes() {
        for(int i = stripes.length - 1; i >= 0; i --) stripes[i].unlock();
    }

    private void clearArrays() {
        Arrays.fill(keys, 0L);
        Arrays.fill(scores, 0);
        Arrays.fill(depths, 0);
        Arrays.fill(moves, 0L);
        Arrays.fill(bounds, (byte) 0);
        Arrays.fill(generations, (byte) 0);
        Arrays.fill(valid, (byte) 0);
    }
}
