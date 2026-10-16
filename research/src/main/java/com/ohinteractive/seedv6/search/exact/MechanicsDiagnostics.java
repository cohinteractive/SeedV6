package com.ohinteractive.seedv6.search.exact;

import java.util.Locale;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.See;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.tools.search.ExactSearchHarness;

/** Explicitly UNTIMED research tool. No instrumentation in production Search. */
public final class MechanicsDiagnostics implements ExactEvaluator {
    private final boolean[] live = new boolean[ExactSearch.MAX_DEPTH + 1];
    private final int[] sizes = new int[live.length], consumed = new int[live.length];
    private final long[][] histograms = new long[5][513];
    private final long[] legal = new long[512], scratch = new long[Board.MAX_BITBOARDS];
    private final int[][] ranked = new int[3][512];
    private final int[] counts = new int[3];
    private final TTable.TEntry entry = new TTable.TEntry();
    private int[] quietHistory;
    private long[] historyKeys;
    private TTable table;
    private long generated, searched, expanded, cutoffs, scans;
    private long insertionComparisons, insertionShifts, quietShifts, zeroShiftQuietNodes;

    @Override public int evaluate(long[] board, int ply) { return Eval.evaluate(board); }

    @Override public void child(long[] parent, long[] child, int ply) {
        // A sibling callback closes the previous subtree, including TT/terminal leaves.
        for(int p = live.length - 1; p > ply; p--) close(p);
        if(!live[ply]) {
            int n = Gen.genAll(parent[0], parent[1], parent[2], parent[3], (int) parent[Board.STATUS],
                    parent[Board.KEY], true, legal, scratch);
            int good = 0, bad = 0, quiet = 0;
            int ep = Board.enPassantSquare((int) parent[Board.STATUS]);
            long hash = table != null && table.probe(SearchKey.key(parent, historyKeys[ply]), entry) ? entry.hashMove : 0;
            java.util.Arrays.fill(counts, 0);
            long priorQuietShifts = quietShifts;
            for(int i = 0; i < n; i++) {
                int group;
                if(!CaptureHistory.isTactical(legal[i], ep)) { quiet++; group = 0; }
                else if(See.atLeastGeneratedLegal(parent, legal[i], 0)) { good++; group = 2; }
                else { bad++; group = 1; }
                if(legal[i] == hash) continue;
                int score = group == 0 ? quietHistory[QuietHistory.index(legal[i])] : ExactSearch.tacticalMaterialValue(legal[i], ep);
                int j = counts[group]++;
                while(j > 0) {
                    insertionComparisons++;
                    if(ranked[group][j - 1] >= score) break;
                    ranked[group][j] = ranked[group][j - 1]; j--;
                    insertionShifts++;
                    if(group == 0) quietShifts++;
                }
                ranked[group][j] = score;
            }
            if(priorQuietShifts == quietShifts) zeroShiftQuietNodes++;
            histograms[0][n]++; histograms[1][good]++; histograms[2][bad]++; histograms[3][quiet]++;
            sizes[ply] = n; consumed[ply] = 0; live[ply] = true;
            generated += n; expanded++;
        }
        scans += sizes[ply] - consumed[ply] - 1; // Comparisons for full-list next-best selection.
        consumed[ply]++; searched++;
    }

    private void close(int ply) {
        if(!live[ply]) return;
        histograms[4][consumed[ply]]++;
        if(consumed[ply] < sizes[ply]) cutoffs++;
        live[ply] = false;
    }

    private void report(String label) {
        for(int p = 0; p < live.length; p++) close(p);
        System.out.printf(Locale.ROOT, "%s expanded=%d generated=%d searched=%d unconsumed=%d tail_pct=%.3f early_cutoffs=%d selection_comparisons=%d%n",
                label, expanded, generated, searched, generated - searched,
                100.0 * (generated - searched) / generated, cutoffs, scans);
        System.out.printf(Locale.ROOT, "  insertion_comparisons=%d insertion_shifts=%d quiet_shifts=%d zero_shift_quiet_nodes_pct=%.3f%n",
                insertionComparisons, insertionShifts, quietShifts, 100.0 * zeroShiftQuietNodes / expanded);
        String[] names = {"list", "good_including_hash", "bad_including_hash", "quiet_including_hash", "consumed"};
        for(int h = 0; h < histograms.length; h++) {
            long sum = 0; int max = 0;
            for(int i = 0; i <= 512; i++) { sum += i * histograms[h][i]; if(histograms[h][i] != 0) max = i; }
            System.out.printf(Locale.ROOT, "  %s mean=%.3f p50=%d p90=%d p99=%d max=%d", names[h], sum / (double) expanded,
                    percentile(h, .5), percentile(h, .9), percentile(h, .99), max);
            for(int threshold : new int[] {8, 16, 24, 32}) {
                long above = 0;
                for(int i = threshold + 1; i <= 512; i++) above += histograms[h][i];
                System.out.printf(Locale.ROOT, " above_%d_pct=%.3f", threshold, 100.0 * above / expanded);
            }
            System.out.println();
        }
    }

    private int percentile(int h, double p) {
        long cumulative = 0;
        for(int i = 0; i <= 512; i++) { cumulative += histograms[h][i]; if(cumulative >= Math.ceil(p * expanded)) return i; }
        return 512;
    }

    public static void main(String[] args) throws Exception {
        int depth = args.length == 0 ? 5 : Integer.parseInt(args[0]);
        for(boolean tt : new boolean[] {false, true}) {
            var aggregate = new MechanicsDiagnostics();
            for(var p : ExactSearchHarness.orderingPositions()) {
                if(depth == 6 && !p.name().equals("kiwipete") && !p.name().equals("middlegame") && !p.name().equals("evasion")) continue;
                var diagnostic = new MechanicsDiagnostics();
                diagnostic.table = tt ? new TTable(4) : null;
                var search = new ExactSearch(diagnostic, diagnostic.table, ExactSearch.SEE_MATERIAL_QUIET_HISTORY);
                var historyField = ExactSearch.class.getDeclaredField("quietHistory"); historyField.setAccessible(true);
                var keysField = ExactSearch.class.getDeclaredField("historyKeys"); keysField.setAccessible(true);
                diagnostic.quietHistory = (int[]) historyField.get(search);
                diagnostic.historyKeys = (long[]) keysField.get(search);
                var actual = search.search(Board.fromFen(p.fen()), depth);
                var expected = new ExactSearch((b, ply) -> Eval.evaluate(b), tt ? new TTable(4) : null, ExactSearch.SEE_MATERIAL_QUIET_HISTORY)
                        .search(Board.fromFen(p.fen()), depth);
                if(actual.nodes() != expected.nodes() || actual.score() != expected.score()
                        || actual.bestMove() != expected.bestMove()
                        || !java.util.Arrays.equals(actual.principalVariation(), expected.principalVariation()))
                    throw new AssertionError("Diagnostic changed Search");
                diagnostic.report("depth=" + depth + " tt=" + tt + " position=" + p.name());
                aggregate.expanded += diagnostic.expanded; aggregate.generated += diagnostic.generated;
                aggregate.searched += diagnostic.searched; aggregate.cutoffs += diagnostic.cutoffs; aggregate.scans += diagnostic.scans;
                aggregate.insertionComparisons += diagnostic.insertionComparisons; aggregate.insertionShifts += diagnostic.insertionShifts;
                aggregate.quietShifts += diagnostic.quietShifts; aggregate.zeroShiftQuietNodes += diagnostic.zeroShiftQuietNodes;
                for(int h = 0; h < 5; h++) for(int i = 0; i <= 512; i++) aggregate.histograms[h][i] += diagnostic.histograms[h][i];
            }
            aggregate.report("depth=" + depth + " tt=" + tt + " aggregate");
        }
    }
}
