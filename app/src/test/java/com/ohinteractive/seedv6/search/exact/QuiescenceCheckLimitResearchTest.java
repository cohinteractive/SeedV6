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
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.DrawAdjudicator;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(45)
class QuiescenceCheckLimitResearchTest {
    private static final int[] MODES = {33, 34, 35};

    @Test void corpusMatchesCandidateOraclesAndEveryCompletedPvSuffix() {
        try(PrintStream out = new PrintStream(OutputStream.nullOutputStream())) { QuiescenceCheckLimitCorpus.run(out, false); }
    }

    @Test void initialOnlyRejectsTheFirstCheckAfterAForcedCaptureButOneRecoversMate() {
        long[] b = Board.fromFen(QuiescenceCheckLimitCorpus.DEEPER.get(0).fen());
        for(int mode : MODES) {
            var q = owner(mode, QuiescenceSeeCorpus.HCE, true); var r = search(q, b);
            assertTrue(r.completed()); assertEquals(mode == 33 ? 546 : -32766, r.score());
            assertEquals(mode == 33 ? "f8g8 h6g8 h8g8" : "f8g8 h6f7", QuiescenceSeeCorpus.pv(r));
            assertEquals(mode == 33 ? 0 : 1, q.quietCheckDiagnostics()[4]);
        }
        assertEquals(1, ExhaustiveOracle.legalMoves(b).length);
    }

    @Test void allInitialQuietCheckFormsRemainEligibleAndNormalLeavesResetTheirAllowance() {
        for(var f : QuiescenceQuietCheckCorpus.TARGETED) if(f.oracle()) for(int mode : MODES) {
            long[] b = Board.fromFen(f.fen()); Trace trace = new Trace(mode);
            assertTrue(search(owner(mode, trace, false), b).completed());
            assertTrue(trace.rootQuiet.contains(QuiescenceDepthCorpus.move(b, f.resource())), f.name());
        }
        long[] b = Board.fromFen(QuiescenceQuietCheckCorpus.TARGETED.get(0).fen());
        for(int mode : MODES) {
            var oracle = new QuiescenceOracle(QuiescenceSeeCorpus.HCE, mode);
            int expected = oracle.score(b, new SearchLineHistory(GameHistory.initial(b)), 1, 0);
            var q = owner(mode, QuiescenceSeeCorpus.HCE, false);
            var r = q.search(b, GameHistory.initial(b), 1, () -> q.visitedNodes() >= 1_000_000);
            assertTrue(r.completed()); assertEquals(expected, r.score());
        }
    }

    @Test void secondCheckIsRequiredForTheAcceptedQueenSacrificeFixture() {
        long[] b = Board.fromFen(QuiescenceQuietCheckCorpus.TARGETED.get(6).fen());
        for(int mode : MODES) {
            var r = search(owner(mode, QuiescenceSeeCorpus.HCE, false), b);
            assertEquals(mode == 35 ? 32765 : 152, r.score());
            if(mode == 35) assertEquals("e6g8 f8g8 h6f7", QuiescenceSeeCorpus.pv(r));
        }
    }

    @Test void thirdCheckMateIsPresentOnlyInUnrestrictedMoveSet() {
        long[] b = Board.fromFen(QuiescenceCheckLimitCorpus.DEEPER.get(3).fen());
        for(int mode : MODES) assertTrue(search(owner(mode, QuiescenceSeeCorpus.HCE, false), b).score() < 32000);
        var full = search(owner(32, QuiescenceSeeCorpus.HCE, false), b);
        assertTrue(full.completed()); assertEquals(32763, full.score());
        assertEquals("c2a3 b1a1 d3b1 c1b1 a3c2", QuiescenceSeeCorpus.pv(full));
        int quiet = 0;
        for(long m : full.principalVariation()) {
            if(QuiescenceCheckLimitCorpus.ordinary(b, m)) quiet++;
            else if(QuiescenceOracle.inCheck(b)) assertEquals(1, ExhaustiveOracle.legalMoves(b).length, "Forced reply");
            b = ExhaustiveOracle.child(b, m);
        }
        assertEquals(3, quiet); assertTrue(QuiescenceOracle.inCheck(b)); assertEquals(0, ExhaustiveOracle.legalMoves(b).length);
    }

    @Test void deeperKnightForkChangesTheCaptureChoiceAndWinsTheQueenInThatLine() {
        long[] b = Board.fromFen(QuiescenceCheckLimitCorpus.DEEPER.get(2).fen());
        var initial = search(owner(33, QuiescenceSeeCorpus.HCE, false), b);
        var one = search(owner(34, QuiescenceSeeCorpus.HCE, false), b);
        assertEquals("a2b3", Move.coordinate(initial.bestMove())); assertEquals(944, initial.score());
        assertEquals("f2f4", Move.coordinate(one.bestMove())); assertEquals(587, one.score());
        var history = GameHistory.builder(b); b = child(b, "a2b3"); history.appendPosition(b);
        var fork = QuiescenceCheckLimitCorpus.search(owner(34, QuiescenceSeeCorpus.HCE, false), b, history.snapshot(), 1, 0);
        assertEquals("f4d3", Move.coordinate(fork.bestMove()));
        assertTrue(QuiescenceSeeCorpus.pv(fork).contains("d3f2"));
    }

    @Test void aFirstCheckAfterAnExchangeAndAfterPromotionIsNotInitialOnly() {
        long[] b = Board.fromFen(QuiescenceCheckLimitCorpus.DEEPER.get(1).fen());
        var w = QuiescenceCheckLimitCorpus.witness(b, GameHistory.initial(b), 34, 33);
        assertEquals("first-excluded-check", w.status()); assertTrue(w.qply() >= 2); assertEquals(1, w.ordinal());
        b = Board.fromFen("7k/P7/8/8/8/8/8/7K w - - 0 1");
        var initial = search(owner(33, QuiescenceSeeCorpus.HCE, false), b);
        var one = search(owner(34, QuiescenceSeeCorpus.HCE, true), b);
        assertEquals(1063, initial.score()); assertEquals(1081, one.score());
        assertEquals("2:1:a8g2", QuiescenceCheckLimitCorpus.pvChecks(b, one, 34));
    }

    @Test void pathAllowancesAreIndependentAcrossSiblingsAndCheckedEvasionsDoNotConsumeThem() {
        for(int mode : MODES) {
            Trace trace = new Trace(mode); var q = owner(mode, trace, true);
            long[] b = Board.fromFen(QuiescenceQuietCheckCorpus.TARGETED.get(9).fen());
            assertEquals(32767, search(q, b).score());
            assertEquals(6, trace.rootQuiet.size()); assertEquals(6, q.quietCheckDiagnostics()[4]);
            assertEquals(mode == 35 ? 2 : 1, trace.maxUsed);
            assertEquals(trace.maxUsed, q.quietCheckDiagnostics()[7]);
            assertEquals(trace.first, q.quietCheckDiagnostics()[4]); assertEquals(trace.second, q.quietCheckDiagnostics()[5]);
            assertTrue(q.quietCheckDiagnostics()[6] > 0);
        }
        for(int mode : new int[] {34, 35}) {
            Trace trace = new Trace(mode);
            assertTrue(search(owner(mode, trace, false), Board.fromFen("4r1k1/8/8/8/8/8/8/4K3 w - - 0 1")).completed());
            assertTrue(trace.quietEvasions > 0); assertTrue(trace.firstAfterEvasion > 0);
        }
    }

    @Test void closedAllowanceStillSearchesEveryCheckedEvasionAndCountercheck() throws Exception {
        long[] b = Board.fromFen("4r3/8/8/8/8/8/8/2B1K1k1 w - - 0 1");
        for(int mode : MODES) {
            List<Long> root = new ArrayList<>(); int used = mode == 35 ? 2 : 1;
            var q = owner(mode, new ExactEvaluator() {
                public int evaluate(long[] a, int p) { assertFalse(QuiescenceOracle.inCheck(a)); return 37; }
                public void child(long[] a, long[] c, int p) { if(p == 17) root.add(move(a, c)); }
            }, false);
            int score = node(q, b, 17, 10, used, -32769, 32769);
            int expected = new QuiescenceOracle((a,p) -> 37, mode).score(b, new SearchLineHistory(GameHistory.initial(b)), 0, 17, 10, used);
            assertEquals(expected, score);
            assertEquals(Arrays.stream(ExhaustiveOracle.legalMoves(b)).boxed().toList(), root);
            assertTrue(root.stream().anyMatch(m -> QuiescenceOracle.inCheck(ExhaustiveOracle.child(b, m))));
        }
    }

    @Test void unchangedTerminalPrecedenceActualPlyAndFailSoftScores() throws Exception {
        long[] mate = Board.fromFen(QuiescenceQuietCheckCorpus.TARGETED.get(0).fen());
        for(int mode : MODES) {
            assertEquals(32750, node(owner(mode, QuiescenceSeeCorpus.HCE, false), mate, 17, mode == 33 ? 0 : 91, 0, -32769, 32769));
            var q = owner(mode, (a,p) -> { throw new AssertionError("Terminal evaluation"); }, false);
            assertEquals(-32751, node(q, Board.fromFen("7k/6Q1/5K2/8/8/8/8/8 b - - 100 1"), 17, 1, 1, -32769, 32769));
            for(String fen : new String[] {"7k/5K2/6Q1/8/8/8/8/8 b - - 0 1", "4k3/8/8/8/8/8/8/R3K3 w - - 100 1", "4k3/8/8/8/8/8/8/4K3 w - - 0 1"})
                assertEquals(0, node(q, Board.fromFen(fen), 17, 1, 1, -32769, 32769));
            q = owner(mode, QuiescenceSeeCorpus.HCE, false);
            var r = q.searchWindow(mate, GameHistory.initial(mate), 0, -32769, 2000, ExactSearch.NEVER_CANCELLED);
            assertEquals(32767, r.score()); assertEquals(1, r.principalVariation().length);
            r = q.searchWindow(mate, GameHistory.initial(mate), 0, -32769, 1000, ExactSearch.NEVER_CANCELLED);
            assertEquals(Eval.evaluate(mate), r.score()); assertFalse(r.hasMove());
        }
    }

    @Test void realHistoryQuietCheckRepetitionDrawIsRetained() {
        long[] b = Board.fromFen("7k/8/8/8/8/8/8/rK6 w - - 2 2"); var h = GameHistory.builder(b);
        String[] cycle = {"b1b2", "a1a2", "b2b1", "a2a1"};
        for(int i = 0; i < 7; i++) { b = child(b, cycle[i % 4]); h.appendPosition(b); }
        assertFalse(h.snapshot().isFormalThreefold(b));
        for(int mode : MODES) {
            var q = owner(mode, (a,p) -> -37, false);
            var r = q.searchWindow(b, h.snapshot(), 0, -37, 0, ExactSearch.NEVER_CANCELLED);
            assertTrue(r.completed()); assertEquals(0, r.score()); assertEquals("a2a1", Move.coordinate(r.bestMove()));
        }
    }

    @Test void suffixRetainsUsedCountAndNoPolicyHistoryOrTableIsIntroduced() throws Exception {
        long[] b = Board.fromFen(QuiescenceQuietCheckCorpus.TARGETED.get(9).fen()); var game = GameHistory.initial(b);
        for(int mode : MODES) {
            var q = owner(mode, QuiescenceSeeCorpus.HCE, false);
            int closed = mode == 35 ? 2 : 1;
            assertEquals(Eval.evaluate(b), q.searchQuiescenceSuffix(b, game, 2, closed, ExactSearch.NEVER_CANCELLED).score());
            assertEquals(mode == 33 ? Eval.evaluate(b) : 32767, q.searchQuiescenceSuffix(b, game, 2, 0, ExactSearch.NEVER_CANCELLED).score());
            for(String f : new String[] {"table", "qtable", "quietHistory", "killers", "countermoves", "continuationHistory"}) assertNull(field(q, f));
            assertEquals(0, field(q, "qsearchPruning")); assertEquals(Integer.MAX_VALUE, field(q, "qdepthLimit"));
            assertThrows(IllegalArgumentException.class, () -> q.searchQuiescenceSuffix(b, game, 1, 2, ExactSearch.NEVER_CANCELLED));
        }
    }

    @Test void safetyCancellationAndDiagnosticRepeatabilityRemainHonest() throws Exception {
        long[] b = Board.fromFen(QuiescenceQuietCheckCorpus.TARGETED.get(6).fen()); var game = GameHistory.initial(b);
        for(int mode : MODES) {
            var clean = owner(mode, QuiescenceSeeCorpus.HCE, false); var diagnostic = owner(mode, QuiescenceSeeCorpus.HCE, true);
            var a = search(clean, b); same(a, search(clean, b)); same(a, search(diagnostic, b));
            aborted(clean.search(b, game, 0, () -> clean.visitedNodes() >= 3));
            AtomicBoolean stop = new AtomicBoolean(); var q = owner(mode, (x,p) -> { stop.set(true); return Eval.evaluate(x); }, false);
            aborted(q.search(b, game, 0, stop::get));
            Thread.currentThread().interrupt(); try { aborted(clean.search(b, 0)); } finally { Thread.interrupted(); }
            var ex = assertThrows(InvocationTargetException.class, () -> node(owner(mode, QuiescenceSeeCorpus.HCE, false),
                    Board.fromFen("4r1k1/8/8/8/8/8/8/4K3 w - - 0 1"), 256, 12, 2, -32769, 32769));
            assertEquals("Aborted", ex.getCause().getClass().getSimpleName());
            same(a, search(clean, b));
        }
    }

    @Test void staticBaselineAndUnrestrictedRecordedTreesStayFrozen() {
        long[] b = Board.fromFen(QuiescenceQuietCheckCorpus.TARGETED.get(6).fen());
        assertEquals(82, new ExactSearch(QuiescenceSeeCorpus.HCE).search(b, 0).score());
        var baseline = ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE).search(b, 0);
        assertEquals(82, baseline.score()); assertEquals(1, baseline.nodes());
        var full = search(owner(32, QuiescenceSeeCorpus.HCE, false), b);
        assertEquals(32765, full.score()); assertEquals(571, full.nodes()); assertEquals(20, full.maximumQply());
        var start = new ExactSearch(QuiescenceSeeCorpus.HCE).search(Board.startingPosition(), 2);
        assertEquals(0, start.score()); assertEquals(128, start.nodes()); assertEquals("e2e4", Move.coordinate(start.bestMove()));
    }

    private static final class Trace implements ExactEvaluator {
        final int mode; final int[] used = new int[257]; final boolean[] quietEvasion = new boolean[257];
        final List<Long> rootQuiet = new ArrayList<>(); int maxUsed, first, second, quietEvasions, firstAfterEvasion;
        Trace(int mode) { this.mode = mode; }
        public int evaluate(long[] b, int p) { return Eval.evaluate(b); }
        public void child(long[] a, long[] b, int p) {
            long m = move(a, b); boolean ordinary = QuiescenceCheckLimitCorpus.ordinary(a, m);
            used[p + 1] = used[p] + (ordinary ? 1 : 0);
            quietEvasion[p + 1] = QuiescenceOracle.inCheck(a) && !QuiescenceOracle.tactical(a, m);
            if(quietEvasion[p + 1]) quietEvasions++;
            if(ordinary) {
                assertTrue(QuiescenceOracle.inCheck(b)); assertTrue(QuiescenceCheckLimitCorpus.eligible(mode, p, used[p]));
                if(used[p] == 0) { first++; if(quietEvasion[p]) firstAfterEvasion++; } else second++;
                if(p == 0) rootQuiet.add(m);
            }
            maxUsed = Math.max(maxUsed, used[p + 1]);
        }
    }
    private static ExactSearch owner(int mode, ExactEvaluator e, boolean diagnostic) { return ExactSearch.quiescenceResearch(e, mode, diagnostic); }
    private static ExactSearchResult search(ExactSearch q, long[] b) { return QuiescenceCheckLimitCorpus.search(q, b, GameHistory.initial(b), 0, 0); }
    private static long[] child(long[] b, String move) { return ExhaustiveOracle.child(b, QuiescenceDepthCorpus.move(b, move)); }
    private static long move(long[] a, long[] b) { for(long m : ExhaustiveOracle.legalMoves(a)) if(Arrays.equals(ExhaustiveOracle.child(a,m), b)) return m; throw new AssertionError("Illegal child"); }
    private static void same(ExactSearchResult a, ExactSearchResult b) { assertEquals(a.completed(),b.completed()); assertEquals(a.score(),b.score()); assertEquals(a.nodes(),b.nodes()); assertEquals(a.maximumQply(),b.maximumQply()); assertArrayEquals(a.principalVariation(),b.principalVariation()); }
    private static void aborted(ExactSearchResult r) { assertFalse(r.completed()); assertEquals(Value.INVALID,r.score()); assertEquals(-1,r.completedDepth()); assertFalse(r.hasMove()); }
    private static Object field(ExactSearch q, String n) throws Exception { Field f=ExactSearch.class.getDeclaredField(n);f.setAccessible(true);return f.get(q); }
    private static int node(ExactSearch q,long[] b,int ply,int qply,int used,int alpha,int beta) throws Exception {
        System.arraycopy(b,0,((long[][])field(q,"boards"))[ply],0,6);
        for(String n:new String[]{"history","cancelled"}) { Field f=ExactSearch.class.getDeclaredField(n);f.setAccessible(true);f.set(q,n.equals("history")?new SearchLineHistory(GameHistory.initial(b)):ExactSearch.NEVER_CANCELLED); }
        Method m=ExactSearch.class.getDeclaredMethod("quiescence",int.class,int.class,int.class,int.class,int.class);m.setAccessible(true);return (int)m.invoke(q,ply,qply,alpha,beta,used);
    }
}
