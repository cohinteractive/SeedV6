package com.ohinteractive.seedv6.search.exact;

import java.util.Arrays;
import com.ohinteractive.seedv6.core.Board;

/** SR-001H opt-in recorder, not a Search policy. Fixed primitive storage is allocated
 * once by the research caller. No strings, callbacks, move lists or allocations in recursion.
 * Package access keeps export/analysis in the test-only headless research harness. */
final class QuiescenceDeltaEvidence {
    static final int NODE=0, MOVE=1, PREVIOUS=2, QPLY=3, PLY=4, ALPHA=5, BETA=6,
            STAND=7, GAIN=8, SEE=9, SCORE=10, FINAL=11, FLAGS=12, CAUSE=13, NEXT=14, BOARD=15,
            WIDTH=BOARD+Board.MAX_BITBOARDS;
    static final int MOVE_DONE=1, NODE_DONE=2, RAISED=4, CUTOFF=8, BEST_CHANGED=16, FINAL_BEST=32;
    // Selected returned-line evidence, not a claim that a draw/mate was uniquely necessary.
    static final int NONE=0, MATE=1, STALEMATE=2, RULE50=3, REPETITION=4, INSUFFICIENT=5;
    final long[] data;
    final int[] cause = new int[ExactSearch.MAX_DEPTH+1];
    final long[] previous = new long[ExactSearch.MAX_DEPTH+1];
    private final int[] head = new int[ExactSearch.MAX_DEPTH+1];
    private final long[] node = new long[ExactSearch.MAX_DEPTH+1];
    int size;

    static ExactSearch search(ExactEvaluator evaluator, QuiescenceDeltaEvidence evidence) {
        try {
            var factory=ExactSearch.class.getDeclaredMethod("deltaEvidenceResearch",ExactEvaluator.class,QuiescenceDeltaEvidence.class);
            factory.setAccessible(true);
            return (ExactSearch)factory.invoke(null,evaluator,evidence);
        } catch(ReflectiveOperationException e) {
            throw new IllegalStateException("Run tools/search-delta-diagnostics.py; requires the diagnostic shadow class",e);
        }
    }

    QuiescenceDeltaEvidence(int capacity) {
        if(capacity < 1) throw new IllegalArgumentException("Positive evidence capacity required");
        data = new long[Math.multiplyExact(capacity, WIDTH)];
        reset();
    }
    void reset() { size=0; Arrays.fill(head,-1); Arrays.fill(cause,0); Arrays.fill(previous,0); }
    void enter(int ply, int qply, long id) {
        head[ply]=-1; cause[ply]=NONE; node[ply]=id;
        if(qply==0) previous[ply]=0;
    }
    int start(long[] board, long move, int ply, int qply, int alpha, int beta,
              int standPat, int gain, int seeClass) {
        if(size == data.length/WIDTH) throw new IllegalStateException("SR-001H evidence capacity exhausted; no sampling permitted");
        int id=size++, b=id*WIDTH;
        Arrays.fill(data,b,b+WIDTH,0);
        data[b+NODE]=node[ply]; data[b+MOVE]=move; data[b+PREVIOUS]=previous[ply];
        data[b+QPLY]=qply; data[b+PLY]=ply; data[b+ALPHA]=alpha; data[b+BETA]=beta;
        data[b+STAND]=standPat; data[b+GAIN]=gain; data[b+SEE]=seeClass==1 ? -1 : 1;
        data[b+NEXT]=head[ply]; head[ply]=id;
        System.arraycopy(board,0,data,b+BOARD,Board.MAX_BITBOARDS);
        return id;
    }
    void returned(int id, int ply, int score, int best, int alpha, int beta) {
        if(id>=0) {
            int b=id*WIDTH;
            data[b+SCORE]=score; data[b+CAUSE]=cause[ply+1];
            data[b+FLAGS]=MOVE_DONE | (score>alpha ? RAISED : 0) | (score>=beta ? CUTOFF : 0)
                    | (score>best ? BEST_CHANGED : 0);
        }
        if(score>best) cause[ply]=cause[ply+1];
    }
    void completed(int ply, int score, long bestMove) {
        for(int id=head[ply]; id>=0; id=(int)data[id*WIDTH+NEXT]) {
            data[id*WIDTH+FINAL]=score;
            data[id*WIDTH+FLAGS]|=NODE_DONE;
            if(data[id*WIDTH+MOVE]==bestMove) data[id*WIDTH+FLAGS]|=FINAL_BEST;
        }
    }
    long get(int id,int field) { return data[id*WIDTH+field]; }
}
