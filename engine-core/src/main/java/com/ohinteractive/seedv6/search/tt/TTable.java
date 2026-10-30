package com.ohinteractive.seedv6.search.tt;

import java.util.Arrays;

public class TTable {

    public static final class TEntry {
        public long data;
        public long hashMove;
        public int rawEvaluation = Integer.MIN_VALUE;

        public boolean hasOrdinaryScore() { return (data & (VALID_MASK|HINT_MASK|HEURISTIC_MASK))==VALID_MASK; }
        public boolean hasHeuristicScore() { return (data & (VALID_MASK|HEURISTIC_MASK))==(VALID_MASK|HEURISTIC_MASK); }
        public boolean hasCompletedHint() { return (data & VALID_MASK)!=0 && (data & (HINT_MASK|HEURISTIC_MASK))!=0; }
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
        this(sizeInMb, false);
    }

    /** Explicit research layout: raw evaluation uses the spare high32 bits of
     * the move word. Moves in this mode must use the supported unsigned32 encoding.
     * Validity/request epoch remain independent of score and hint freshness. */
    public TTable(int sizeInMb, boolean rawEvaluationLane) {
        int entrySizeInBytes = 24;
        int totalBytes = sizeInMb * 1024 * 1024;
        int rawEntries = totalBytes / entrySizeInBytes;
        int entryCount = Integer.highestOneBit(rawEntries);
        this.key = new long[entryCount];
        this.data = new long[entryCount];
        this.hashMove = new long[entryCount];
        this.informationLayout = rawEvaluationLane;
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
            if((data & (VALID_MASK | HINT_MASK | HEURISTIC_MASK)) != VALID_MASK) return false;
            entry.data = data;
            entry.hashMove = moveAt(index);
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
        if(informationLayout && type==TYPE_EVAL)
            throw new IllegalArgumentException("Use the independent raw lane in an information table");
        requireMove(hashMove);
        int index = (int) key & indexMask;
        int stripe = index & (STRIPE_MASK);
        synchronized (locks[stripe]) {
            if(type == TYPE_EVAL) {
                if(informationLayout) this.hashMove[index] &= MOVE_BITS;
                replaceKey(index,key);
                this.data[index] = score;
                return;
            }
            long existingData = this.data[index];
            if((existingData & (VALID_MASK | HINT_MASK | HEURISTIC_MASK)) == VALID_MASK) {
                long existingKey = this.key[index];
                int existingDepth = (int) (existingData & DEPTH_BITS);
                int existingType = (int) (existingData >>> TYPE_SHIFT & TYPE_BITS);
                int existingGen = (int) (existingData >>> GENERATION_SHIFT & GENERATION_BITS);
                if(key != existingKey && existingGen == (this.generation & GENERATION_BITS) && depth <= existingDepth && type >= existingType) return;
                if(key == existingKey && depth <= existingDepth && type >= existingType) return;
            }
            replaceKey(index,key);
            this.data[index] = ((long) score << SCORE_SHIFT) | ((long) (generation & GENERATION_BITS) << GENERATION_SHIFT) | ((long) (type & TYPE_BITS) << TYPE_SHIFT) | (depth & DEPTH_BITS) | VALID_MASK;
            setMove(index,hashMove);
        }
    }

    public void clear() {
        Arrays.fill(this.data, 0L); 
        if(informationLayout) Arrays.fill(hashMove, 0L);
    }

    public boolean hasRawEvaluationLane() { return informationLayout; }

    /** Research observation at actual first materialization, not mere availability. */
    public void rawEvaluationUsed() {}

    /** One coherent main-node snapshot. Raw evaluation, heuristic/ordinary score
     * provenance and completed hint remain distinct, independently eligible fields.
     * A miss clears the caller's scratch so prior contents cannot be consumed. */
    public boolean probeInformation(long key,TEntry entry) {
        if(!informationLayout) throw new IllegalStateException("No information lane");
        int index=(int)key & indexMask;
        synchronized(locks[index & STRIPE_MASK]) {
            entry.data=0;entry.hashMove=0;entry.rawEvaluation=Integer.MIN_VALUE;
            if(this.key[index]!=key) return false;
            long value=data[index];int raw=(int)(hashMove[index]>>>32);
            if((value&VALID_MASK)!=0) {entry.data=value;entry.hashMove=moveAt(index);}
            if((raw&RAW_VALID)!=0 && ((raw>>>16)&255)==(generation&255)) entry.rawEvaluation=(short)raw;
            return entry.data!=0 || entry.rawEvaluation!=Integer.MIN_VALUE;
        }
    }

    /** Raw board-only ordinary evaluation, never a searched bound or mate score.
     * Zero is valid. The evaluator definition is fixed during this request.
     * Individual reads/writes are coherent under the existing stripe lock. */
    public int probeRawEvaluation(long key) {
        if(!informationLayout) return Integer.MIN_VALUE;
        int index=(int)key & indexMask;
        synchronized(locks[index & STRIPE_MASK]) {
            int raw=(int)(hashMove[index]>>>32);
            return this.key[index]==key && (raw & RAW_VALID)!=0
                    && ((raw>>>16)&255)==(generation&255) ? (short)raw : Integer.MIN_VALUE;
        }
    }

    /** Evaluation-only admission cannot displace any searched score or legal hint,
     * even an old one. Same-key evaluation updates never refresh either lifetime.
     * Eval-only slots have data=0 and are invisible to all Search-score probes. */
    public boolean saveRawEvaluation(long key,int value) {
        if(!informationLayout) throw new IllegalStateException("No raw evaluation lane");
        if(value < -TranspositionScores.MAX_NORMAL_SCORE || value > TranspositionScores.MAX_NORMAL_SCORE)
            throw new IllegalArgumentException("Raw evaluation enters the mate domain");
        int index=(int)key & indexMask;
        synchronized(locks[index & STRIPE_MASK]) {
            if(this.key[index]!=key) {
                if((data[index]&VALID_MASK)!=0) return false;
                replaceKey(index,key);data[index]=0;hashMove[index]=0;
            }
            hashMove[index]=(hashMove[index]&MOVE_BITS)|((long)(RAW_VALID|((generation&255)<<16)|(value&65535))<<32);
            return true;
        }
    }

    /** Called only under the slot's stripe lock. A collision must invalidate raw
     * information even if the new score/hint has the same request generation. */
    private void replaceKey(int index,long key) {
        if(informationLayout && this.key[index]!=key) hashMove[index]&=MOVE_BITS;
        this.key[index]=key;
    }

    /** Experimental completed-move guidance, with the caller's unchanged SearchKey.
     * Ordinary probe never exposes these records as score evidence. No extra arrays,
     * entry count or locks, and no ordinary proof entry is replaced by a hint. */
    public long probeHint(long key) {
        int index=(int)key & indexMask;
        synchronized(locks[index & STRIPE_MASK]) {
            return this.key[index]==key && (data[index] & VALID_MASK)!=0
                    && (data[index] & (HINT_MASK | HEURISTIC_MASK))!=0
                    ? moveAt(index) : 0;
        }
    }

    /** Research-only hint replacement: current-generation deeper hints survive;
     * equal-depth/latest completions refresh, old-generation hints may be replaced.
     * Ordinary score saves retain their existing policy and can displace any hint. */
    public boolean saveHint(long key,int depth,long move) {
        requireMove(move);
        if(depth<1 || depth>255 || move==0) return false;
        int index=(int)key & indexMask;
        synchronized(locks[index & STRIPE_MASK]) {
            long old=data[index];
            if((old & VALID_MASK)!=0) {
                if((old & HEURISTIC_MASK)!=0) {
                    int oldGen=(int)(old>>>GENERATION_SHIFT & GENERATION_BITS);
                    if(oldGen==(generation & GENERATION_BITS)) {
                        int hintDepth=(int)(old>>>HINT_DEPTH_SHIFT & DEPTH_BITS);
                        if(this.key[index]==key) {
                            if(depth<hintDepth) return false;
                            data[index]=(old & ~(DEPTH_BITS<<HINT_DEPTH_SHIFT)) | ((long)depth<<HINT_DEPTH_SHIFT);
                            setMove(index,move);
                            return true;
                        }
                        if(depth<Math.max(hintDepth,(int)(old & DEPTH_BITS))) return false;
                    }
                    // Stale score eligibility is not refreshed by a newer hint.
                    // A collision or new-generation hint becomes hint-only below.
                } else {
                    if((old & HINT_MASK)==0) return false;
                    int oldGen=(int)(old>>>GENERATION_SHIFT & GENERATION_BITS);
                    if(oldGen==(generation & GENERATION_BITS) && (old & DEPTH_BITS)>depth) return false;
                }
            }
            replaceKey(index,key);
            data[index]=VALID_MASK | HINT_MASK | ((long)(generation & GENERATION_BITS)<<GENERATION_SHIFT) | depth;
            setMove(index,move);
            return true;
        }
    }

    /** Explicit Phase 4 score domain. Never exposed by ordinary proof probes.
     * Raw evaluation tables remain separate, with caller-managed validity.
     * TYPE_EXACT here means an inside-window selective estimate, not exact minimax.
     * Consumer eligibility is intentionally heuristic and must mark provenance. */
    public boolean probeHeuristic(long key,TEntry entry) {
        int index=(int)key & indexMask;
        synchronized(locks[index & STRIPE_MASK]) {
            long value=data[index];
            if(this.key[index]!=key || (value & (VALID_MASK | HEURISTIC_MASK))!=(VALID_MASK | HEURISTIC_MASK)) return false;
            entry.data=value;entry.hashMove=moveAt(index);return true;
        }
    }

    /** Ordinary scores only, same physical arrays/locks. Preserve an independently
     * depth-qualified completed move even when a score refresh is rejected.
     * Current-generation deeper collision information survives. Same-key score
     * refresh allows at most two plies of depth loss; this is a research policy. */
    public boolean saveHeuristic(long key,int depth,int type,int score,long move) {
        requireMove(move);
        if(depth<1 || depth>255 || type<TYPE_EXACT || type>TYPE_UPPER
                || score < -TranspositionScores.MAX_NORMAL_SCORE
                || score > TranspositionScores.MAX_NORMAL_SCORE) return false;
        int index=(int)key & indexMask;
        synchronized(locks[index & STRIPE_MASK]) {
            long old=data[index];int hintDepth=move==0?0:depth;long retainedMove=move;
            if((old & VALID_MASK)!=0) {
                boolean heuristic=(old & HEURISTIC_MASK)!=0;
                if(!heuristic && (old & HINT_MASK)==0) return false;
                int oldDepth=(int)(old & DEPTH_BITS);
                int oldHintDepth=heuristic?(int)(old>>>HINT_DEPTH_SHIFT & DEPTH_BITS):oldDepth;
                boolean current=((old>>>GENERATION_SHIFT)&GENERATION_BITS)==(generation & GENERATION_BITS);
                if(this.key[index]==key) {
                    if(move==0 || (current && oldHintDepth>hintDepth)) {
                        retainedMove=moveAt(index);hintDepth=oldHintDepth;
                    }
                    if(heuristic && current && depth+2<oldDepth) {
                        if(move!=0 && depth>=oldHintDepth) {
                            data[index]=(old & ~(DEPTH_BITS<<HINT_DEPTH_SHIFT)) | ((long)depth<<HINT_DEPTH_SHIFT);
                            setMove(index,move);
                        }
                        return false;
                    }
                } else if(current && depth<Math.max(oldDepth,oldHintDepth)) return false;
            }
            replaceKey(index,key);
            data[index]=((long)score<<SCORE_SHIFT) | VALID_MASK | HEURISTIC_MASK
                    | ((long)(generation & GENERATION_BITS)<<GENERATION_SHIFT)
                    | ((long)type<<TYPE_SHIFT) | depth | ((long)hintDepth<<HINT_DEPTH_SHIFT);
            setMove(index,retainedMove);
            return true;
        }
    }

    /** Optional research observation; ordinary tables incur no counter updates. */
    public void heuristicCutoff() {}

    /** H2 research: keep useful score depth for this or the preceding request,
     * independently of completed-hint freshness. HINT+HEURISTIC marks the H2
     * encoding; the three spare low-word bits carry a bounded freshness tag.
     * There are no new arrays, slots, locks or ordinary proof permissions. */
    public boolean saveHeuristicAged(long key,int depth,int type,int score,long move) {
        requireMove(move);
        if(depth<1 || depth>255 || type<TYPE_EXACT || type>TYPE_UPPER) return false;
        int index=(int)key & indexMask;
        synchronized(locks[index & STRIPE_MASK]) {
            long old=data[index];
            if(score < -TranspositionScores.MAX_NORMAL_SCORE || score > TranspositionScores.MAX_NORMAL_SCORE) {
                // A completed mate-domain result must not leave a same-key older
                // ordinary estimate available for another heuristic cutoff.
                if(this.key[index]==key && (old & (VALID_MASK|HEURISTIC_MASK))==(VALID_MASK|HEURISTIC_MASK)) {
                    int hintDepth=hintDepth(old);
                    int hintGeneration=currentHint(old)?generation:(int)(old>>>GENERATION_SHIFT&GENERATION_BITS);
                    data[index]=moveAt(index)==0 || hintDepth==0?0:
                            VALID_MASK|HINT_MASK|((long)(hintGeneration&GENERATION_BITS)<<GENERATION_SHIFT)|hintDepth;
                }
                return false;
            }
            long retainedMove=move;int retainedDepth=move==0?0:depth,tag=generation&7;
            if((old&VALID_MASK)!=0) {
                boolean heuristic=(old&HEURISTIC_MASK)!=0;
                if(!heuristic && (old&HINT_MASK)==0) return false;
                int oldDepth=(int)(old&DEPTH_BITS),oldHintDepth=hintDepth(old),age=scoreAge(old);
                boolean freshHint=currentHint(old);
                if(this.key[index]==key) {
                    if(move==0 || (freshHint && oldHintDepth>retainedDepth)) {
                        retainedMove=moveAt(index);retainedDepth=oldHintDepth;
                        // Normalize an old hint to a definitely stale tag. Copying
                        // arbitrary modulo8 bits into a fresh score could alias.
                        tag=(freshHint?generation:generation-1)&7;
                    }
                    if(heuristic && age<=1 && depth+2<oldDepth) {
                        if(move!=0 && (!freshHint || depth>=oldHintDepth)) {
                            data[index]=withFreshHint(old,depth);setMove(index,move);
                        }
                        return false;
                    }
                } else if(depth<protectedDepth(old)) return false;
            }
            replaceKey(index,key);
            data[index]=((long)score<<SCORE_SHIFT)|VALID_MASK|HEURISTIC_MASK|HINT_MASK
                    |((long)(generation&GENERATION_BITS)<<GENERATION_SHIFT)
                    |((long)type<<TYPE_SHIFT)|depth|((long)retainedDepth<<HINT_DEPTH_SHIFT)
                    |((long)tag<<HINT_FRESHNESS_SHIFT);
            setMove(index,retainedMove);
            return true;
        }
    }

    /** H2 hint completion never refreshes the score's generation. Expired scores
     * become ordinary hint-only records; current deeper hints remain protected. */
    public boolean saveHintAged(long key,int depth,long move) {
        requireMove(move);
        if(depth<1 || depth>255 || move==0) return false;
        int index=(int)key & indexMask;
        synchronized(locks[index & STRIPE_MASK]) {
            long old=data[index];
            if((old&VALID_MASK)!=0) {
                if((old&HEURISTIC_MASK)!=0) {
                    if(this.key[index]==key && scoreAge(old)<=1) {
                        if(currentHint(old) && depth<hintDepth(old)) return false;
                        data[index]=withFreshHint(old,depth);setMove(index,move);return true;
                    }
                    if(this.key[index]!=key && depth<protectedDepth(old)) return false;
                } else {
                    if((old&HINT_MASK)==0) return false;
                    if(currentHint(old) && depth<hintDepth(old)) return false;
                }
            }
            replaceKey(index,key);
            data[index]=VALID_MASK|HINT_MASK|((long)(generation&GENERATION_BITS)<<GENERATION_SHIFT)|depth;
            setMove(index,move);
            return true;
        }
    }

    private int scoreAge(long value) {
        return (generation-(int)(value>>>GENERATION_SHIFT&GENERATION_BITS))&(int)GENERATION_BITS;
    }

    private int hintDepth(long value) {
        return (int)(((value&HEURISTIC_MASK)!=0?value>>>HINT_DEPTH_SHIFT:value)&DEPTH_BITS);
    }

    private boolean currentHint(long value) {
        if((value&(HEURISTIC_MASK|HINT_MASK))!=(HEURISTIC_MASK|HINT_MASK)) return scoreAge(value)==0;
        // Modulo8 freshness can only be interpreted during this bounded score
        // lifetime. Older records cannot acquire current priority by aliasing.
        return scoreAge(value)<=1 && ((value>>>HINT_FRESHNESS_SHIFT)&7)==(generation&7);
    }

    private int protectedDepth(long value) {
        int age=scoreAge(value);
        int score=(value&HEURISTIC_MASK)!=0 && age<=1?(int)(value&DEPTH_BITS)-2*age:0;
        return Math.max(score,currentHint(value)?hintDepth(value):0);
    }

    private long withFreshHint(long value,int depth) {
        return (value&~((DEPTH_BITS<<HINT_DEPTH_SHIFT)|(7L<<HINT_FRESHNESS_SHIFT)))|HINT_MASK
                |((long)depth<<HINT_DEPTH_SHIFT)|((long)(generation&7)<<HINT_FRESHNESS_SHIFT);
    }

    public int entryCapacity() { return key.length; }
    public long entryPayloadBytes() { return (24L)*key.length; }

    private static final long MOVE_BITS = 0xffffffffL;
    private long moveAt(int index) { return informationLayout ? hashMove[index]&MOVE_BITS : hashMove[index]; }
    private void setMove(int index,long move) {
        hashMove[index]=informationLayout ? (hashMove[index]&~MOVE_BITS)|move : move;
    }
    private void requireMove(long move) {
        if(informationLayout && (move&~MOVE_BITS)!=0)
            throw new IllegalArgumentException("Information layout requires unsigned32 moves");
    }

    private static final int RAW_VALID = 1<<24;

    private static final long DEPTH_BITS = 0xffL;
    private static final int TYPE_SHIFT = 8;
    private static final long TYPE_BITS = 0b11L;
    private static final int GENERATION_SHIFT = 10;
    private static final long GENERATION_BITS = 0xffL;
    private static final int VALID_SHIFT = 18;
    private static final long VALID_MASK = 1L << VALID_SHIFT;
    private static final long HINT_MASK = 1L << 19;
    private static final long HEURISTIC_MASK = 1L << 20;
    private static final int HINT_DEPTH_SHIFT = 21;
    private static final int HINT_FRESHNESS_SHIFT = 29;
    private static final int SCORE_SHIFT = 32;

    private static final int DEFAULT_TABLE_SIZE_IN_MB = 64;
    private static final int STRIPE_COUNT = 32;
    private static final int STRIPE_MASK = STRIPE_COUNT - 1;

    private final long[] key;
    private final long[] data;
    private final long[] hashMove;
    private final boolean informationLayout;
    private final StripeLock[] locks;
    private final int indexMask;
    private int generation;

}
