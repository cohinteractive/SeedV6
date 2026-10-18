package com.ohinteractive.seedv6.search.exact;

/** Explicit Phase 3 quiet-learning experiment. Nominal means the child was
 * searched at its assigned ordinary depth, not that selective descendants prove
 * its score. No labels exist for omitted or aborted children. Worker-private. */
public final class HistoryLearning {
    private static final int WORDS=8; // ExactSearch's 512 move slots per ply.
    private final long[] nominal=new long[(ExactSearch.MAX_DEPTH+1)*WORDS];
    private final boolean nominalOnly;
    private final Diagnostics diagnostics;

    HistoryLearning(boolean nominalOnly,Diagnostics diagnostics) {
        this.nominalOnly=nominalOnly;this.diagnostics=diagnostics;
    }

    void reset() { if(diagnostics!=null) diagnostics.reset(); }

    void searched(int ply,int index,boolean observed) {
        int word=ply*WORDS+(index>>>6);long bit=1L<<(index&63);
        if(observed) nominal[word]|=bit;else nominal[word]&=~bit;
    }

    boolean observed(int ply,int index) {
        return (nominal[ply*WORDS+(index>>>6)]&(1L<<(index&63)))!=0;
    }

    void cutoff(int[] table,long[] moves,int cutoff,int ep,int depth,int ply) {
        long winner=moves[cutoff];
        if(CaptureHistory.isTactical(winner,ep)) return;
        if(!observed(ply,cutoff)) throw new IllegalStateException("Unverified quiet cutoff");
        int capped=Math.min(depth,QuietHistory.MAX_BONUS_DEPTH),bonus=capped*capped;
        if(diagnostics!=null) diagnostics.rewards++;
        if(nominalOnly) QuietHistory.update(table,QuietHistory.index(winner),bonus);
        for(int i=0;i<cutoff;i++) {
            long move=moves[i];
            if(CaptureHistory.isTactical(move,ep)) continue;
            boolean verified=observed(ply,i);
            if(diagnostics!=null) {
                if(verified) diagnostics.nominalMaluses++;
                else { diagnostics.reducedMaluses++;if(nominalOnly) diagnostics.filteredMaluses++; }
            }
            if(nominalOnly && verified) QuietHistory.update(table,QuietHistory.index(move),-bonus);
        }
        if(!nominalOnly) QuietHistory.recordCutoff(table,moves,cutoff,ep,depth);
    }

    /** Counts learning events, including duplicates/retries; not independent labels. */
    public static final class Diagnostics {
        public long rewards,nominalMaluses,reducedMaluses,filteredMaluses;
        void reset() { rewards=nominalMaluses=reducedMaluses=filteredMaluses=0; }
    }
}
