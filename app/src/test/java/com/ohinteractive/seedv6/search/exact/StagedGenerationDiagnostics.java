package com.ohinteractive.seedv6.search.exact;

import java.util.Arrays;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.tools.search.ExactSearchHarness;

/** Test-only counters used by tools/search-staging-diagnostics.py's shadow class.
 * Never present in production ExactSearch or timed benchmark classes. */
public final class StagedGenerationDiagnostics {
    private static final String[] NAMES = {"nodes", "depth_zero_nodes", "static_evaluations", "checked_nodes",
            "genAll", "genTactical", "genQuiet", "genEvasion", "leaf_quiet_avoided", "positive_cutoff_quiet_avoided",
            "quiet_hash_forced_early", "no_tactical_quiet_required", "both_subset_calls", "generated_moves",
            "generated_unsearched", "positive_generated_unsearched", "deferred_history_nodes", "positive_resolved_without_quiet",
            "searched_edges", "positive_nodes", "checked_positive_nodes", "checked_static_nodes",
            "both_positive_subset_calls", "both_leaf_subset_calls", "no_tactical_quiet_required_positive"};
    private static final long[] totals = new long[NAMES.length];
    private static final int[] depth = new int[257], generated = new int[257], children = new int[257], tacticals = new int[257];
    private static final boolean[] tactical = new boolean[257], quiet = new boolean[257];
    private static int ply;
    private static boolean freezeHistory;
    private static long trace;

    static void enter(int remaining, int at, long[] board) {
        if(at > 0) { children[at - 1]++; totals[18]++; }
        ply = at; depth[at] = remaining; generated[at] = 0; children[at] = 0; tacticals[at] = 0;
        tactical[at] = false; quiet[at] = false;
        totals[0]++; if(remaining <= 0) totals[1]++; else totals[19]++;
        if(Board.isPlayerInCheck(board[0], board[1], board[2], board[3], Board.player((int) board[Board.STATUS]))) {
            totals[3]++; totals[remaining > 0 ? 20 : 21]++;
        }
        trace = trace * 31 + board[Board.KEY] + at;
    }

    static void exit(int at) {
        totals[14] += generated[at] - children[at];
        if(depth[at] > 0) totals[15] += generated[at] - children[at];
        if(tactical[at] && quiet[at]) { totals[12]++; totals[depth[at] > 0 ? 22 : 23]++; }
        if(tactical[at] && tacticals[at] > 0 && !quiet[at]) {
            if(depth[at] <= 0) totals[8]++;
            else if(children[at] > 0) totals[9]++;
            else totals[17]++;
        }
        ply = at - 1;
    }
    static void staticLeaf() { totals[2]++; }
    static void recordCutoff(int[] history, long[] moves, int cutoff, int ep, int remaining) {
        if(!freezeHistory) QuietHistory.recordCutoff(history, moves, cutoff, ep, remaining);
    }
    private static int generated(int count) { generated[ply] += count; totals[13] += count; return count; }

    static int genAll(long a, long b, long c, long d, int status, long key, boolean legal, long[] moves, long[] scratch) {
        totals[4]++; return generated(Gen.genAll(a, b, c, d, status, key, legal, moves, scratch));
    }
    static int genTactical(long a, long b, long c, long d, int status, long key, boolean legal, long[] moves, long[] scratch) {
        totals[5]++; tactical[ply] = true;
        int count = Gen.genTactical(a, b, c, d, status, key, legal, moves, scratch);
        tacticals[ply] = count; return generated(count);
    }
    static int genQuiet(long a, long b, long c, long d, int status, long key, boolean legal, long[] moves, long[] scratch) {
        totals[6]++; quiet[ply] = true;
        if(tacticals[ply] == 0) { totals[11]++; if(depth[ply] > 0) totals[24]++; }
        else if(depth[ply] > 0 && children[ply] == 0) totals[10]++;
        if(children[ply] > 0) totals[16]++;
        return generated(Gen.genQuiet(a, b, c, d, status, key, legal, moves, scratch));
    }
    static int genEvasion(long a, long b, long c, long d, int status, long key, boolean legal, long checkers, long[] moves, long[] scratch) {
        totals[7]++; return generated(Gen.genEvasion(a, b, c, d, status, key, legal, checkers, moves, scratch));
    }

    private static void reset() { Arrays.fill(totals, 0); trace = 17; }
    private static void require(boolean condition, String message) { if(!condition) throw new AssertionError(message); }
    private static void print(String label, long[] counts) {
        System.out.print(label);
        for(int i = 0; i < counts.length; i++) System.out.print(" " + NAMES[i] + "=" + counts[i]);
        System.out.println();
    }
    private static ExactSearchResult run(long[] board, int remaining, int mode, TTable table, long hash, boolean cutoff) {
        reset();
        var search = new ExactSearch(cutoff ? (b, p) -> -80 : (b, p) -> Eval.evaluate(b), table,
                ExactSearch.SEE_MATERIAL_QUIET_HISTORY, mode);
        search.beginRequest();
        if(hash != 0) table.save(SearchKey.key(board, SearchKey.rootHistory(board, GameHistory.initial(board))), remaining + 1, TTable.TYPE_EXACT, 999, hash);
        var result = search.searchWindow(board, GameHistory.initial(board), remaining,
                cutoff ? -100 : -ExactSearch.INFINITY, cutoff ? 50 : ExactSearch.INFINITY, ExactSearch.NEVER_CANCELLED);
        search.endRequest();
        require(result.nodes() == totals[0], "Instrumented node count mismatch; run through the diagnostic script");
        return result;
    }

    private static void fixtures() {
        for(int mode : new int[] {ExactSearch.LEAF_STAGED_LAZY, ExactSearch.STAGED_LAZY}) {
            long[] board = Board.fromFen("4k3/8/8/3pP3/3r4/8/8/4K3 w - d6 0 1");
            run(board, 0, mode, null, 0, false);
            require(totals[5] == 1 && totals[6] == 0 && totals[8] == 1 && totals[2] == 1, "Tactical leaf must avoid quiet generation");
            run(Board.startingPosition(), 0, mode, null, 0, false);
            require(totals[5] == 1 && totals[6] == 1 && totals[11] == 1 && totals[2] == 1, "Quiet-only leaf must generate quiets");
            var mate = run(Board.fromFen("7k/6Q1/5K2/8/8/8/8/8 b - - 100 1"), 0, mode, null, 0, false);
            require(mate.score() == -ExactSearch.MATE_SCORE && totals[7] == 1 && totals[2] == 0, "Mate must precede static evaluation and rule draw");
            var stale = run(Board.fromFen("7k/5K2/6Q1/8/8/8/8/8 b - - 0 1"), 0, mode, null, 0, false);
            require(stale.score() == 0 && totals[5] == 1 && totals[6] == 1 && totals[2] == 0, "Both empty subsets establish stalemate");
            run(Board.fromFen("4k3/8/8/8/8/8/4r3/4K3 w - - 0 1"), 0, mode, null, 0, false);
            require(totals[7] == 1 && totals[5] == 0 && totals[2] == 1, "Checked nonterminal leaf uses all evasions");
        }
        long[] board = Board.fromFen(ExactSearchHarness.orderingPositions().get(1).fen());
        long[] moves = new long[512], scratch = new long[Board.MAX_BITBOARDS];
        int n = Gen.genTactical(board[0], board[1], board[2], board[3], (int) board[Board.STATUS], board[Board.KEY], true, moves, scratch);
        require(n > 0, "Tactical hash fixture");
        run(board, 1, ExactSearch.STAGED_LAZY, new TTable(1), moves[0], true);
        require(totals[9] == 1 && totals[10] == 0, "Tactical hash cutoff avoids root quiets");
        n = Gen.genQuiet(board[0], board[1], board[2], board[3], (int) board[Board.STATUS], board[Board.KEY], true, moves, scratch);
        require(n > 0, "Quiet hash fixture");
        run(board, 1, ExactSearch.STAGED_LAZY, new TTable(1), moves[0], true);
        require(totals[10] == 1 && totals[9] == 0, "Quiet hash forces early root quiets");
        System.out.println("Instrumented leaf/terminal/hash fixtures passed.");
    }

    public static void main(String[] args) {
        int remaining = args.length == 0 ? 5 : Integer.parseInt(args[0]);
        freezeHistory = args.length > 1 && args[1].equals("--freeze-history");
        fixtures();
        for(boolean tt : new boolean[] {false, true}) {
            long[][] aggregate = new long[3][NAMES.length];
            for(var p : ExactSearchHarness.orderingPositions()) {
                if(remaining == 6 && !p.name().equals("kiwipete") && !p.name().equals("middlegame") && !p.name().equals("evasion")) continue;
                ExactSearchResult base = null; long baseTrace = 0;
                for(int mode = ExactSearch.FULL_LAZY; mode <= ExactSearch.STAGED_LAZY; mode++) {
                    var result = run(Board.fromFen(p.fen()), remaining, mode, tt ? new TTable(4) : null, 0, false);
                    if(base == null) { base = result; baseTrace = trace; }
                    else {
                        require(result.score() == base.score(), "Staging changed exact value");
                        if(mode == ExactSearch.LEAF_STAGED_LAZY || freezeHistory)
                            require(result.nodes() == base.nodes() && result.bestMove() == base.bestMove()
                                    && Arrays.equals(result.principalVariation(), base.principalVariation()) && trace == baseTrace,
                                    "Unexpected tree change");
                    }
                    print("depth=" + remaining + " tt=" + tt + " mode=" + mode + " freeze=" + freezeHistory + " position=" + p.name(), totals);
                    for(int i = 0; i < totals.length; i++) aggregate[mode - ExactSearch.FULL_LAZY][i] += totals[i];
                }
            }
            for(int mode = 0; mode < 3; mode++) print("depth=" + remaining + " tt=" + tt + " mode=" + (mode + ExactSearch.FULL_LAZY)
                    + " freeze=" + freezeHistory + " aggregate", aggregate[mode]);
        }
    }
}
