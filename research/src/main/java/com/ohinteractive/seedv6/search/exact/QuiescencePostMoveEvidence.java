package com.ohinteractive.seedv6.search.exact;

import java.util.Arrays;

/** SR-001I test-only extension of H's primitive recorder, populated by a build-only
 * ExactSearch copy. Immediate child adjudication is separate from selected-line cause. */
final class QuiescencePostMoveEvidence {
    static final int UNKNOWN=-1, NONTERMINAL=0, CHECKED=6, NO_STATIC=Integer.MIN_VALUE;
    final QuiescenceDeltaEvidence base;
    final int[] postStatic, childKind, childNodes;
    final boolean[] childStandPatCutoff;
    final int[] kind=new int[ExactSearch.MAX_DEPTH+1];
    private final int[] naturalStatic=new int[ExactSearch.MAX_DEPTH+1];
    private final boolean[] standPatCutoff=new boolean[ExactSearch.MAX_DEPTH+1];

    QuiescencePostMoveEvidence(int capacity) {
        base=new QuiescenceDeltaEvidence(capacity);
        postStatic=new int[capacity]; childKind=new int[capacity]; childNodes=new int[capacity];
        childStandPatCutoff=new boolean[capacity]; reset();
    }
    static ExactSearch search(ExactEvaluator evaluator, QuiescencePostMoveEvidence evidence) {
        try {
            var m=ExactSearch.class.getDeclaredMethod("postMoveEvidenceResearch",ExactEvaluator.class,QuiescencePostMoveEvidence.class);
            m.setAccessible(true);return (ExactSearch)m.invoke(null,evaluator,evidence);
        } catch(ReflectiveOperationException e) {
            throw new IllegalStateException("Run tools/search-postmove-diagnostics.py for the diagnostic shadow class",e);
        }
    }
    void reset() { Arrays.fill(kind,UNKNOWN); Arrays.fill(naturalStatic,NO_STATIC); Arrays.fill(standPatCutoff,false); }
    void enter(int ply) { kind[ply]=UNKNOWN; naturalStatic[ply]=NO_STATIC; standPatCutoff[ply]=false; }
    void evaluated(int ply,int score,int beta) { kind[ply]=NONTERMINAL;naturalStatic[ply]=score;standPatCutoff[ply]=score>=beta; }
    void before(int id,long nodes) {
        if(id<0)return;
        childKind[id]=UNKNOWN;postStatic[id]=NO_STATIC;childNodes[id]=-(int)nodes;childStandPatCutoff[id]=false;
    }
    void returned(int id,int ply,long nodes) {
        if(id<0)return;
        childKind[id]=kind[ply+1];childNodes[id]+=(int)nodes;
        if(childKind[id]==NONTERMINAL) {
            postStatic[id]=-naturalStatic[ply+1];childStandPatCutoff[id]=standPatCutoff[ply+1];
        }
    }
}
