package com.ohinteractive.seedv6.search.exact;

import java.util.Arrays;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;

/** SR-005 request-private heuristic information. Never supplies a score or TT proof.
 * Scratch becomes eligible only after a completed full-window iteration. */
public final class IterationReuse {
    public static final int OFF=0, OBSERVE=1, ROOT=2, PV=3, SCORE=4, EFFORT=5, HISTORY=6, PV_HISTORY=7;
    final int policy;
    final Diagnostics diagnostics;
    private final long[] root = new long[Board.MAX_BITBOARDS];
    private GameHistory game;
    private int completedDepth, lineLength, count, pendingCount;
    private boolean eligible, accepting;
    private final long[] line = new long[ExactSearch.MAX_DEPTH];
    private final boolean[] prefix = new boolean[ExactSearch.MAX_DEPTH+1];
    private final long[] ranked = new long[512], pending = new long[512], effort = new long[512];
    private final int[] scores = new int[512];

    IterationReuse(int policy, Diagnostics diagnostics) {
        if(policy < OBSERVE || policy > PV_HISTORY) throw new IllegalArgumentException("Invalid reuse policy");
        this.policy=policy; this.diagnostics=diagnostics;
    }
    void clear() { game=null; completedDepth=lineLength=count=pendingCount=0; eligible=accepting=false; }
    /** Coordinator-only, after every worker has drained. Copies completed ordering facts,
     * never pending/aborted work, scores, mutable history or mathematical proof. */
    void copyCompletedFrom(IterationReuse source) {
        if(source == this) return;
        clear();
        if(source == null || source.completedDepth == 0) return;
        System.arraycopy(source.root, 0, root, 0, root.length);
        game=source.game; completedDepth=source.completedDepth;
        lineLength=source.lineLength; count=source.count;
        System.arraycopy(source.line, 0, line, 0, lineLength);
        System.arraycopy(source.ranked, 0, ranked, 0, count);
    }
    boolean history() { return policy==HISTORY || policy==PV_HISTORY; }
    boolean ranking() { return policy==SCORE || policy==EFFORT; }
    /** Read a completed full-root observation during a restricted root job.
     * No scout/job may replace it or contribute pending root observations. */
    void prepareRootJob(long[] board, GameHistory history, int depth, boolean request) {
        prepare(board, history, depth, request, -ExactSearch.INFINITY, ExactSearch.INFINITY);
        accepting=false;
    }
    boolean prepare(long[] board, GameHistory history, int depth, boolean request, int alpha, int beta) {
        accepting=request && alpha==-ExactSearch.INFINITY && beta==ExactSearch.INFINITY && depth>0;
        eligible=accepting && completedDepth+1==depth && game!=null
                && Arrays.equals(root,0,root.length,board,0,root.length) && sameHistory(game,history);
        if(!eligible) { completedDepth=lineLength=count=0; }
        pendingCount=0;
        prefix[0]=eligible;
        System.arraycopy(board,0,root,0,root.length); game=history;
        if(diagnostics!=null) diagnostics.reset();
        return eligible && history();
    }
    private static boolean sameHistory(GameHistory a, GameHistory b) {
        if(a.size()!=b.size()) return false;
        for(int i=0;i<a.size();i++) if(a.keyAt(i)!=b.keyAt(i)) return false;
        return true;
    }
    long hint(int ply, long hashMove) {
        long move=eligible && prefix[ply] && ply<lineLength ? line[ply] : 0;
        if(move!=0 && diagnostics!=null) {
            diagnostics.candidates++;
            if(hashMove==move) diagnostics.matches++; else if(hashMove==0) diagnostics.missing++; else diagnostics.different++;
        }
        if(policy==OBSERVE || policy==HISTORY || (policy==ROOT && ply!=0)) return 0;
        return move;
    }
    void descend(int ply,long move) {
        prefix[ply+1]=eligible && prefix[ply] && ply<lineLength && line[ply]==move;
    }
    boolean rootRanking() { return eligible && ranking() && count>0; }
    void order(long[] moves,int n,int[] keys,int base,long hash,long hint,int ply) {
        if(hint==0 && !(ply==0 && rootRanking())) return;
        for(int i=0;i<n;i++) {
            if(moves[i]==hash) continue; // Existing legal TT precedence is unconditional.
            if(moves[i]==hint) { keys[base+i]=1_900_000_000; if(diagnostics!=null) diagnostics.promoted++; }
            else if(ply==0 && rootRanking()) {
                for(int j=0;j<count;j++) if(moves[i]==ranked[j]) { keys[base+i]=1_800_000_000-j; break; }
            }
        }
    }
    void rootResult(long move,int score,long nodes) {
        if(!accepting || !ranking()) return;
        pending[pendingCount]=move; scores[pendingCount]=score; effort[pendingCount++]=nodes;
    }
    void complete(int depth,long[] pv,int length) {
        if(!accepting) { clear(); return; }
        completedDepth=depth; lineLength=length; System.arraycopy(pv,0,line,0,length);
        // Stable ordering of heuristic fail-soft returns or inclusive prior effort.
        // Fail-low values are NOT exact alternative scores; no additional searches.
        for(int i=1;i<pendingCount;i++) {
            long move=pending[i], cost=effort[i]; int score=scores[i],j=i;
            while(j>0 && (policy==EFFORT ? effort[j-1]<cost : scores[j-1]<score)) {
                pending[j]=pending[j-1]; scores[j]=scores[j-1]; effort[j]=effort[j-1]; j--;
            }
            pending[j]=move; scores[j]=score; effort[j]=cost;
        }
        count=pendingCount; System.arraycopy(pending,0,ranked,0,count);
    }
    /** Optional research counters, reset each invocation; no diagnostics allocated when absent. */
    public static final class Diagnostics {
        public long candidates,matches,missing,different,promoted,scouts,researches,cutoffs,reductions,omissions;
        void reset() { candidates=matches=missing=different=promoted=scouts=researches=cutoffs=reductions=omissions=0; }
    }
}
