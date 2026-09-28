package com.ohinteractive.seedv6.search.exact;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.See;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.DrawAdjudicator;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
class QuiescenceQuietCheckResearchTest {
    private static final int MODE = ExactSearch.QSEARCH_QUIET_CHECKS;

    @Test void fullDifferentialCorpusRecordsIncompleteAttemptsAndValidatesCompletedPvValues() {
        try(PrintStream out = new PrintStream(OutputStream.nullOutputStream())) { QuiescenceQuietCheckCorpus.run(out, false); }
    }

    @Test void naturalAbsolutePlyWitnessesContainNoMissedTerminalOrRuleDraw() {
        try(PrintStream out = new PrintStream(OutputStream.nullOutputStream())) { QuiescenceQuietCheckCorpus.safety(out); }
    }

    @Test void earlierQuietChecksCanExhaustBudgetBeforeTheAvailableMateInOneIsVisited() {
        long[] b = Board.fromFen(QuiescenceQuietCheckCorpus.TARGETED.get(9).fen());
        List<Long> seen = new ArrayList<>();
        var r = search(tracing(seen), b, GameHistory.initial(b));
        aborted(r); assertEquals(1_000_000, r.nodes());
        assertFalse(seen.contains(move(b, "g6g7")));
        assertFalse(seen.isEmpty());
    }

    @Test void sevenBoundedOraclesMatchIncludingDirectDiscoveredDoublePawnKingAndCastlingChecks() {
        int comparisons = 0;
        for(var f : QuiescenceQuietCheckCorpus.TARGETED) if(f.oracle()) {
            long[] b = Board.fromFen(f.fen()); var game = GameHistory.initial(b);
            var oracle = new QuiescenceOracle(QuiescenceSeeCorpus.HCE, MODE);
            int expected = oracle.score(b, new SearchLineHistory(game), 0, 0);
            var result = search(owner(QuiescenceSeeCorpus.HCE), b, game);
            assertTrue(result.completed(), f.name()); assertEquals(expected, result.score(), f.name());
            verifyPv(b, game, result); comparisons++;
        }
        assertEquals(7, comparisons);
    }

    @Test void everyQuietFormIsSearchedAndNoncheckingQuietsAreExcluded() {
        for(var f : QuiescenceQuietCheckCorpus.TARGETED) if(f.oracle()) {
            long[] b = Board.fromFen(f.fen()); List<Long> seen = new ArrayList<>();
            var result = search(tracing(seen), b, GameHistory.initial(b));
            assertTrue(result.completed());
            List<Long> expected = admitted(b);
            assertEquals(expected.size(), seen.size(), f.name());
            assertTrue(seen.containsAll(expected)); assertEquals(seen.size(), seen.stream().distinct().count());
            assertTrue(seen.contains(move(b, f.resource())), f.name() + " intended check not searched");
        }
        long[] b = Board.fromFen(QuiescenceQuietCheckCorpus.TARGETED.get(1).fen());
        long[] c = ExhaustiveOracle.child(b, move(b, "a6b7"));
        int status = (int)c[Board.STATUS], player = Board.player(status);
        long color = ~(-(long)player ^ c[3]);
        long checkers = Board.getCheckersPext(c[0], c[1], c[2], c[3], color, player,
                Long.numberOfTrailingZeros(c[0] & ~c[1] & ~c[2] & color), c[0] | c[1] | c[2]);
        assertEquals(2, Long.bitCount(checkers));
    }

    @Test void capturesAllPromotionsAndBadSeeTacticalsPrecedeGeneratedQuietChecks() {
        for(String fen : new String[] {"r6b/4k1P1/8/8/8/8/8/4K2R w - - 0 1",
                "7k/6p1/5KQ1/8/8/8/8/8 w - - 0 1", "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"}) {
            long[] b = Board.fromFen(fen); List<Long> seen = new ArrayList<>();
            assertTrue(search(tracing(seen), b, GameHistory.initial(b)).completed());
            List<Long> expected = admitted(b); assertEquals(expected.size(), seen.size()); assertTrue(seen.containsAll(expected));
            if(QuiescenceOracle.inCheck(b)) {
                assertEquals(Arrays.stream(ExhaustiveOracle.legalMoves(b)).boxed().toList(), seen);
                continue; // Checked nodes deliberately retain generated evasion order.
            }
            boolean quiet = false, bad = false; List<Long> quietOrder = new ArrayList<>();
            for(long m : seen) {
                if(!QuiescenceOracle.tactical(b, m)) { quiet = true; quietOrder.add(m); }
                else {
                    assertFalse(quiet, "Tactical after quiet check: " + fen + " " + seen.stream().map(Move::coordinate).toList());
                    boolean good = See.atLeastGeneratedLegal(b, m, 0);
                    if(!good) bad = true; else assertFalse(bad, "Good tactical after bad tactical");
                }
            }
            long[] generated = new long[512];
            int n = Gen.genQuiet(b[0], b[1], b[2], b[3], (int)b[Board.STATUS], b[Board.KEY], true, generated, new long[6]);
            List<Long> generatedChecks = new ArrayList<>();
            for(int i = 0; i < n; i++) if(QuiescenceOracle.inCheck(ExhaustiveOracle.child(b, generated[i]))) generatedChecks.add(generated[i]);
            assertEquals(generatedChecks, quietOrder);
            if(fen.startsWith("r6b")) assertEquals(8, seen.stream().filter(QuiescenceOracle::promotion).count());
        }
    }

    @Test void quietChecksFindMateSacrificeAndMaterialResourcesAbsentFromBaseline() {
        int[] indexes = {0, 5, 6, 7, 10}; int[] expected = {32767, 0, 32765, 0, 241};
        for(int i = 0; i < indexes.length; i++) {
            var f = QuiescenceQuietCheckCorpus.TARGETED.get(indexes[i]);
            long[] b = Board.fromFen(f.fen()); var game = GameHistory.initial(b);
            var r = search(owner(QuiescenceSeeCorpus.HCE), b, game);
            assertTrue(r.completed()); assertEquals(expected[i], r.score()); assertEquals(f.resource(), Move.coordinate(r.bestMove()));
            assertNotEquals(ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE).search(b, 0).score(), r.score());
            verifyPv(b, game, r);
        }
    }

    @Test void standpatRemainsEmptyAndQuietCheckCutoffsAreFailSoft() {
        long[] b = Board.fromFen(QuiescenceQuietCheckCorpus.TARGETED.get(8).fen());
        ExactSearch q = ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE, MODE, true);
        var r = search(q, b, GameHistory.initial(b));
        assertEquals(Eval.evaluate(b), r.score()); assertFalse(r.hasMove()); assertTrue(q.quietCheckDiagnostics()[1] > 0);
        b = Board.fromFen(QuiescenceQuietCheckCorpus.TARGETED.get(0).fen());
        r = q.searchWindow(b, GameHistory.initial(b), 0, -32769, 2000, ExactSearch.NEVER_CANCELLED);
        assertEquals(32767, r.score()); assertEquals(1, r.principalVariation().length); assertEquals(1, q.quietCheckDiagnostics()[2]);
        r = q.searchWindow(b, GameHistory.initial(b), 0, -32769, 1000, ExactSearch.NEVER_CANCELLED);
        assertEquals(Eval.evaluate(b), r.score()); assertFalse(r.hasMove()); assertEquals(0, q.quietCheckDiagnostics()[1]);
        assertTrue(q.quietCheckDiagnostics()[0] > 0, "Diagnostic existence counts even stand-pat cutoffs");
    }

    @Test void checkedNodesKeepAllEvasionsAndNoStandpat() {
        long[] b = Board.fromFen("4r1k1/8/8/8/8/8/8/4K3 w - - 99 1");
        List<Long> seen = new ArrayList<>();
        ExactSearch q = owner(new ExactEvaluator() {
            public int evaluate(long[] a, int p) { throw new AssertionError("Check or rule draw evaluated"); }
            public void child(long[] a, long[] c, int p) { if(p == 0) seen.add(childMove(a, c)); }
        });
        assertEquals(0, search(q, b, GameHistory.initial(b)).score());
        assertEquals(Arrays.stream(ExhaustiveOracle.legalMoves(b)).boxed().toList(), seen);
    }

    @Test void quietCheckingLineCanReachRealHistoryThreefold() {
        long[] b = Board.fromFen("7k/8/8/8/8/8/8/rK6 w - - 2 2");
        var h = GameHistory.builder(b);
        String[] cycle = {"b1b2", "a1a2", "b2b1", "a2a1"};
        for(int i = 0; i < 7; i++) { b = ExhaustiveOracle.child(b, move(b, cycle[i % 4])); h.appendPosition(b); }
        var game = h.snapshot();
        assertFalse(game.isFormalThreefold(b));
        long checking = move(b, "a2a1"); long[] child = ExhaustiveOracle.child(b, checking);
        var path = new SearchLineHistory(game); path.pushRealPosition(child);
        assertEquals(DrawAdjudicator.RuleDraw.FORMAL_THREEFOLD, DrawAdjudicator.adjudicateNonTerminal(child, path));
        List<Long> seen = new ArrayList<>();
        ExactSearch q = owner(new ExactEvaluator() {
            public int evaluate(long[] a, int p) { return -37; }
            public void child(long[] a, long[] c, int p) { if(p == 0) seen.add(childMove(a, c)); }
        });
        assertEquals(-37, ExactSearch.quiescenceResearch((a, p) -> -37).search(b, game, 0, ExactSearch.NEVER_CANCELLED).score());
        var r = q.searchWindow(b, game, 0, -37, 0, ExactSearch.NEVER_CANCELLED);
        assertTrue(r.completed()); assertEquals(0, r.score()); assertTrue(seen.contains(checking));
        assertEquals(checking, r.bestMove());
        assertEquals(0, new QuiescenceOracle((a, p) -> -37, MODE).score(child, path, 0, 1));
    }

    @Test void actualPlyMateDistanceTerminalsAndAbsoluteSafetyRemainDistinct() throws Exception {
        long[] mate = Board.fromFen(QuiescenceQuietCheckCorpus.TARGETED.get(0).fen());
        assertEquals(32750, node(owner(QuiescenceSeeCorpus.HCE), mate, 17, 91));
        ExactSearch q = owner((b, p) -> { throw new AssertionError("Terminal evaluated"); });
        assertEquals(-32751, node(q, Board.fromFen("7k/6Q1/5K2/8/8/8/8/8 b - - 100 1"), 17, 1));
        assertEquals(0, node(q, Board.fromFen("7k/5K2/6Q1/8/8/8/8/8 b - - 0 1"), 17, 1));
        for(String fen : new String[] {QuiescenceQuietCheckCorpus.TARGETED.get(0).fen(),
                "4r3/8/8/8/8/8/8/2B1K1k1 w - - 0 1"}) {
            var ex = assertThrows(InvocationTargetException.class, () -> node(owner(QuiescenceSeeCorpus.HCE), Board.fromFen(fen), 256, 0));
            assertEquals("Aborted", ex.getCause().getClass().getSimpleName());
        }
        // With no admitted check/capture, the last storage slot can resolve stand-pat normally.
        assertEquals(Eval.evaluate(Board.startingPosition()), node(owner(QuiescenceSeeCorpus.HCE), Board.startingPosition(), 256, 0));
    }

    @Test void cancellationAndDiagnosticModePreserveResultsWithoutHistoryOrOtherPolicies() throws Exception {
        long[] b = Board.fromFen(QuiescenceQuietCheckCorpus.TARGETED.get(6).fen()); var game = GameHistory.initial(b);
        ExactSearch clean = owner(QuiescenceSeeCorpus.HCE), diagnostic = ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE, MODE, true);
        var a = search(clean, b, game); var c = search(diagnostic, b, game); same(a, c); same(a, search(clean, b, game));
        for(String field : new String[] {"table", "qtable", "quietHistory", "killers", "countermoves", "continuationHistory"}) assertNull(field(clean, field));
        assertEquals(0, field(clean, "qsearchPruning")); assertEquals(Integer.MAX_VALUE, field(clean, "qdepthLimit"));
        var stop = new AtomicBoolean(); ExactSearch q = owner((x, p) -> { stop.set(true); return Eval.evaluate(x); });
        aborted(q.search(b, game, 0, stop::get));
        aborted(clean.search(b, game, 0, () -> clean.visitedNodes() >= 10));
        Thread.currentThread().interrupt(); try { aborted(clean.search(b, 0)); } finally { Thread.interrupted(); }
        same(a, search(clean, b, game));
    }

    private static List<Long> admitted(long[] b) {
        List<Long> result = new ArrayList<>();
        for(long m : ExhaustiveOracle.legalMoves(b)) if(QuiescenceOracle.inCheck(b) || QuiescenceOracle.tactical(b, m)
                || QuiescenceOracle.inCheck(ExhaustiveOracle.child(b, m))) result.add(m);
        return result;
    }
    private static ExactSearch tracing(List<Long> seen) {
        return owner(new ExactEvaluator() {
            public int evaluate(long[] b, int p) { return Eval.evaluate(b); }
            public void child(long[] a, long[] b, int p) { if(p == 0) seen.add(childMove(a, b)); }
        });
    }
    private static long childMove(long[] a, long[] b) {
        for(long m : ExhaustiveOracle.legalMoves(a)) if(Arrays.equals(ExhaustiveOracle.child(a, m), b)) return m;
        throw new AssertionError("Not a legal child");
    }
    static void verifyPv(long[] b, GameHistory game, ExactSearchResult r) {
        var path = GameHistory.builder(game); int ply = 0;
        ExactSearch reference = owner(QuiescenceSeeCorpus.HCE);
        for(long m : r.principalVariation()) {
            assertTrue(admitted(b).contains(m)); b = ExhaustiveOracle.child(b, m); path.appendPosition(b); ply++;
            var suffix = search(reference, b, path.snapshot()); assertTrue(suffix.completed());
            int s = suffix.score(); if(s >= TranspositionScores.MATE_THRESHOLD) s -= ply; else if(s <= -TranspositionScores.MATE_THRESHOLD) s += ply;
            assertEquals(r.score(), ply % 2 == 0 ? s : -s);
        }
    }
    private static ExactSearch owner(ExactEvaluator e) { return ExactSearch.quiescenceResearch(e, MODE); }
    private static ExactSearchResult search(ExactSearch q, long[] b, GameHistory h) { return QuiescenceQuietCheckCorpus.search(q, b, h); }
    private static long move(long[] b, String m) { return QuiescenceDepthCorpus.move(b, m); }
    private static void same(ExactSearchResult a, ExactSearchResult b) {
        assertEquals(a.completed(), b.completed()); assertEquals(a.score(), b.score()); assertEquals(a.nodes(), b.nodes());
        assertEquals(a.maximumQply(), b.maximumQply()); assertArrayEquals(a.principalVariation(), b.principalVariation());
    }
    private static void aborted(ExactSearchResult r) { assertFalse(r.completed()); assertEquals(Value.INVALID, r.score()); assertEquals(-1, r.completedDepth()); assertFalse(r.hasMove()); }
    private static int node(ExactSearch q, long[] b, int ply, int qply) throws Exception {
        System.arraycopy(b, 0, ((long[][])field(q, "boards"))[ply], 0, 6);
        set(q, "history", new SearchLineHistory(GameHistory.initial(b))); set(q, "cancelled", ExactSearch.NEVER_CANCELLED);
        Method method = ExactSearch.class.getDeclaredMethod("quiescence", int.class, int.class, int.class, int.class);
        method.setAccessible(true); return (int)method.invoke(q, ply, qply, -32769, 32769);
    }
    private static Object field(ExactSearch q, String n) throws Exception { Field f = ExactSearch.class.getDeclaredField(n); f.setAccessible(true); return f.get(q); }
    private static void set(ExactSearch q, String n, Object v) throws Exception { Field f = ExactSearch.class.getDeclaredField(n); f.setAccessible(true); f.set(q, v); }
}
