package com.ohinteractive.seedv6.search.exact;

import java.util.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.tools.search.ExactSearchHarness;

/** Test-only callbacks injected into a shadow class by tools/search-pvs-diagnostics.py.
 * No counters, callback dispatch or event allocation exists in shipped/timed Search. */
public final class PvsDiagnostics {
    private static final boolean INSTRUMENTED = Boolean.getBoolean("sr003.instrumented");
    private static final String[] NAMES = {"nodes", "full_calls", "scouts", "scout_fail_lows", "scout_beta_cutoffs",
            "researches", "first_move_cutoffs", "later_move_cutoffs", "multiple_research_nodes", "stores_lower", "stores_upper", "stores_exact", "nodes_in_research"};
    private static final long[] counts = new long[NAMES.length], watched = new long[NAMES.length];
    private static final long[][] byDepth = new long[257][NAMES.length];
    private static final int[] originalAlpha = new int[257], originalBeta = new int[257], researches = new int[257];
    private static final int[] expectedAlpha = new int[257], expectedBeta = new int[257];
    private static final boolean[] expectedCall = new boolean[257];
    private static final long[] keys = new long[257];
    private static long watchKey;
    private static int researchNesting;

    private static void require(boolean condition, String message) { if(!condition) throw new AssertionError(message); }
    private static void add(int metric, int depth, int ply) {
        counts[metric]++; byDepth[depth][metric]++;
        if(ply == 1 && keys[ply] == watchKey) watched[metric]++;
    }
    static void enter(int depth, int ply, int alpha, int beta, long key) {
        require(-ExactSearch.INFINITY <= alpha && alpha < beta && beta <= ExactSearch.INFINITY, "Unsafe window");
        if(expectedCall[ply]) {
            require(alpha == expectedAlpha[ply] && beta == expectedBeta[ply], "Wrong negamax call window");
            expectedCall[ply] = false;
        }
        keys[ply] = key; originalAlpha[ply] = alpha; originalBeta[ply] = beta; researches[ply] = 0;
        add(0, depth, ply);
        if(researchNesting > 0) add(12, depth, ply);
    }
    private static void expect(int ply, int alpha, int beta) {
        expectedCall[ply+1] = true; expectedAlpha[ply+1] = alpha; expectedBeta[ply+1] = beta;
    }
    static void full(int depth, int ply, int index, int alpha, int beta, boolean research) {
        add(1, depth, ply); expect(ply, -beta, -alpha);
        if(index == 0) require(!research && alpha == originalAlpha[ply] && beta == originalBeta[ply], "First move lacks caller full window");
        if(research) {
            researchNesting++;
            require(index > 0, "First move re-search"); add(5, depth, ply);
            if(++researches[ply] == 2) add(8, depth, ply);
        }
    }
    static void researched() { require(--researchNesting >= 0, "Unbalanced re-search"); }
    static void scout(int depth, int ply, int index, int alpha, int beta) {
        require(index > 0, "First move scouted"); add(2, depth, ply); expect(ply, -alpha-1, -alpha);
    }
    static void outcome(int depth, int ply, int alpha, int beta, int score) {
        if(score <= alpha) add(3, depth, ply);
        else if(score >= beta) add(4, depth, ply);
    }
    static void cutoff(int depth, int ply, int index) { add(index == 0 ? 6 : 7, depth, ply); }
    static void store(int ply, int alpha, int beta, int score) {
        require(alpha == originalAlpha[ply] && beta == originalBeta[ply], "TT classification lost caller window");
        counts[score <= alpha ? 10 : score >= beta ? 9 : 11]++;
    }
    private static void reset() {
        Arrays.fill(counts,0); Arrays.fill(watched,0); Arrays.fill(expectedCall,false);
        researchNesting = 0;
        for(long[] row : byDepth) Arrays.fill(row,0);
    }
    private static ExactSearch owner(ExactEvaluator evaluator, TTable table, int traversal) {
        return new ExactSearch(evaluator, table, ExactSearch.SEE_MATERIAL_QUIET_HISTORY,
                ExactSearch.STAGED_LAZY, ExactSearch.SORT_CROSSOVER, traversal);
    }

    /** Explicit interior-node windows, independent board-only leaf values and real legal chess edges. */
    private static void fixture(String name, int alpha, int beta, int[] gains, long full, long scouts,
                                long lows, long cuts, long research, boolean tt) {
        long[] root = Board.startingPosition();
        long[] child = ExhaustiveOracle.child(root, ExhaustiveOracle.legalMoves(root)[0]);
        long[] replies = ExhaustiveOracle.legalMoves(child); // All quiet, equal initial history: generated order.
        Map<Long,Integer> values = new HashMap<>();
        for(int i = 0; i < replies.length; i++) values.put(ExhaustiveOracle.child(child,replies[i])[Board.KEY],
                -(i < gains.length ? gains[i] : -1000));
        watchKey = child[Board.KEY]; reset();
        var result = owner((b,p)->values.getOrDefault(b[Board.KEY],0), tt ? new TTable(4) : null, ExactSearch.PVS)
                .searchWindow(root,GameHistory.initial(root),2,-beta,-alpha,ExactSearch.NEVER_CANCELLED);
        require(result.completed(), "Interior fixture incomplete");
        require(watched[0] == 1, "Interior fixture must be visited once");
        require(watched[1] == full && watched[2] == scouts && watched[3] == lows && watched[4] == cuts
                && watched[5] == research, "Interior scout fixture failed: " + name + " " + Arrays.toString(watched));
        System.out.printf("fixture interior=%s tt=%s window=[%d,%d] full=%d scouts=%d lows=%d cuts=%d researches=%d passed%n",
                name,tt,alpha,beta,full,scouts,lows,cuts,research);
    }
    private static void fixtures() {
        for(boolean tt : new boolean[] {false,true}) {
            fixture("fail-low",-100,100,new int[] {-10,-30,-10},1,19,19,0,0,tt);
            fixture("alpha-equality",5,100,new int[] {0,5,-10},1,19,19,0,0,tt);
            fixture("beta-equality",-100,50,new int[] {0,50,-10},1,1,0,1,0,tt);
            fixture("fail-soft-overshoot",-100,50,new int[] {0,83,-10},1,1,0,1,0,tt);
            fixture("multiple-improvements",-100,100,new int[] {-10,-5,0,5},4,19,16,0,3,tt);
            fixture("unit-window",0,1,new int[] {0,1,-10},1,1,0,1,0,tt);
        }
        watchKey = 0;
    }
    public static void main(String[] args) {
        int depth = 5; String positions = "ordering";
        for(String arg : args) {
            if(arg.startsWith("--depth=")) depth = Integer.parseInt(arg.substring(8));
            else if(arg.startsWith("--position=")) positions = arg.substring(11);
            else throw new IllegalArgumentException(arg);
        }
        if(INSTRUMENTED) fixtures();
        for(boolean tt : new boolean[] {false,true}) for(var position : ExactSearchHarness.orderingPositions()) {
            if(!positions.equals("ordering") && !Arrays.asList(positions.split(",")).contains(position.name())) continue;
            for(int traversal : new int[] {ExactSearch.ORDERED_ALPHA_BETA,ExactSearch.PVS}) {
                reset(); long[] board = Board.fromFen(position.fen());
                var result = owner((b,p)->Eval.evaluate(b), tt ? new TTable(4) : null, traversal).search(board,depth);
                String label = "position=" + position.name() + " depth=" + depth + " tt=" + (tt ? "on" : "off")
                        + " search=" + (traversal == ExactSearch.PVS ? "PVS" : "ORDERED_ALPHA_BETA");
                System.out.printf("result %s score=%d best=%s pv=[%s] nodes=%d%n",label,result.score(),Move.coordinate(result.bestMove()),
                        String.join(" ",Arrays.stream(result.principalVariation()).mapToObj(Move::coordinate).toList()),result.nodes());
                if(!INSTRUMENTED) continue;
                require(result.nodes() == counts[0] && counts[0] == 1 + counts[1] + counts[2], "Recursive call count mismatch");
                require(counts[2] == counts[3] + counts[4] + counts[5], "Scout outcomes must partition");
                System.out.print("diagnostic " + label);
                for(int i=0;i<NAMES.length;i++) System.out.print(" " + NAMES[i] + "=" + counts[i]);
                System.out.printf(Locale.ROOT," research_pct=%.4f%n",counts[2] == 0 ? 0 : counts[5]*100.0/counts[2]);
                if(traversal == ExactSearch.PVS) for(int d=1;d<=depth;d++)
                    System.out.printf(Locale.ROOT,"by-depth %s remaining=%d scouts=%d researches=%d research_pct=%.4f%n",
                            label,d,byDepth[d][2],byDepth[d][5],byDepth[d][2] == 0 ? 0 : byDepth[d][5]*100.0/byDepth[d][2]);
            }
        }
    }
    private PvsDiagnostics() {}
}
