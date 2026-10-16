package com.ohinteractive.seedv6.search.exact;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
class QuiescenceDepthResearchTest {
    private static final int[] LIMITS = {4, 8, 12};
    private static final String CAPTURE = "4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1";
    private static final String BLOCK = "4r3/8/8/8/8/8/8/2B1K1k1 w - - 0 1";

    @Test void noncheckBoundaryIsExactAndQplyIncrementsIndependentlyOfPathPly() throws Exception {
        long[] b = Board.fromFen(CAPTURE);
        for(int limit : LIMITS) {
            AtomicInteger visits = new AtomicInteger();
            ExactSearch at = owner(limit, new ExactEvaluator() {
                public int evaluate(long[] b, int p) { assertEquals(17, p); return Eval.evaluate(b); }
                public void child(long[] a, long[] b, int p) { visits.incrementAndGet(); }
            });
            assertEquals(Eval.evaluate(b), node(at, b, 17, limit, -32769, 32769));
            assertEquals(0, visits.get()); assertEquals(1, at.visitedNodes());
            assertEquals(0, ((int[])field(at, "pvLength"))[17]);
            assertEquals(limit, field(at, "maximumQply"));
            ExactSearch before = owner(limit, QuiescenceSeeCorpus.HCE);
            assertTrue(node(before, b, 17, limit - 1, -32769, 32769) > Eval.evaluate(b));
            assertEquals(2, before.visitedNodes()); assertEquals(limit, field(before, "maximumQply"));
            assertEquals(1, ((int[])field(before, "pvLength"))[17]);
            ExactSearch beyond = owner(limit, QuiescenceSeeCorpus.HCE);
            assertEquals(Eval.evaluate(b), node(beyond, b, 17, limit + 3, -32769, 32769));
            assertEquals(1, beyond.visitedNodes());
        }
    }

    @Test void checkedLimitMustResolveCounterchecksAndStopAtFirstNoncheck() throws Exception {
        long[] b = Board.fromFen(BLOCK);
        for(int limit : LIMITS) {
            AtomicInteger children = new AtomicInteger();
            ExactSearch q = owner(limit, new ExactEvaluator() {
                public int evaluate(long[] b, int p) { assertFalse(QuiescenceOracle.inCheck(b)); return 37; }
                public void child(long[] parent, long[] child, int p) {
                    assertTrue(QuiescenceOracle.inCheck(parent), "Noncheck expansion beyond horizon");
                    children.incrementAndGet();
                }
            });
            int actual = node(q, b, 17, limit, -32769, 32769);
            int oracle = new QuiescenceOracle((a, p) -> 37, limit)
                    .score(b, new SearchLineHistory(GameHistory.initial(b)), 0, 17, limit);
            assertEquals(oracle, actual); assertTrue(children.get() > 1);
            assertEquals(limit + 3, field(q, "maximumQply"));
        }
    }

    @Test void mateAtAndBeyondLimitUsesActualPathPly() throws Exception {
        long[] captureMate = Board.fromFen("7k/6p1/5KQ1/8/8/8/8/8 w - - 0 1");
        long[] mate = Board.fromFen("7k/6Q1/5K2/8/8/8/8/8 b - - 100 1");
        for(int limit : LIMITS) {
            assertEquals(32768 - 18, node(owner(limit, QuiescenceSeeCorpus.HCE), captureMate, 17, limit - 1, -32769, 32769));
            assertEquals(-32768 + 17, node(owner(limit, (b, p) -> { throw new AssertionError(); }), mate, 17, limit + 6, -32769, 32769));
        }
    }

    @Test void terminalRuleDrawAndQuietOnlyExistencePrecedeTheHorizon() throws Exception {
        for(int limit : LIMITS) {
            for(String fen : new String[] {"7k/5K2/6Q1/8/8/8/8/8 b - - 0 1",
                    "4k3/8/8/8/8/8/8/R3K3 w - - 100 1", "4k3/8/8/8/8/8/8/4K3 w - - 0 1"})
                assertEquals(0, node(owner(limit, (b, p) -> { throw new AssertionError("Terminal evaluated"); }),
                        Board.fromFen(fen), 17, limit, -32769, 32769));
            long[] quiet = Board.fromFen("4k3/8/8/8/8/8/P7/4K3 w - - 0 1");
            assertEquals(37, node(owner(limit, (b, p) -> 37), quiet, 17, limit, -32769, 32769));
            long[] fifty = Board.fromFen("4r1k1/8/8/8/8/8/8/4K3 w - - 99 1");
            assertEquals(0, node(owner(limit, (b, p) -> { throw new AssertionError("Rule50 child evaluated"); }),
                    fifty, 17, limit, -32769, 32769));
        }
    }

    @Test void boundaryReturnsFailSoftAndEmptyPv() throws Exception {
        long[] b = Board.fromFen(CAPTURE);
        int value = Eval.evaluate(b);
        for(int limit : LIMITS) {
            for(int alpha : new int[] {value - 20, value + 10}) {
                ExactSearch q = owner(limit, QuiescenceSeeCorpus.HCE);
                assertEquals(value, node(q, b, 17, limit, alpha, alpha + 5));
                assertEquals(0, ((int[])field(q, "pvLength"))[17]);
            }
        }
    }

    @Test void absoluteSafetyCancellationAndInterruptionRemainIncomplete() throws Exception {
        long[] b = Board.fromFen(QuiescenceDepthCorpus.MATE_FIVE);
        for(int limit : LIMITS) {
            ExactSearch q = owner(limit, QuiescenceSeeCorpus.HCE);
            aborted(q.search(b, GameHistory.initial(b), 0, () -> true));
            aborted(q.search(b, GameHistory.initial(b), 0, () -> q.visitedNodes() >= 3));
            aborted(q.searchQuiescenceSuffix(b, GameHistory.initial(b), limit, () -> true));
            try { Thread.currentThread().interrupt(); aborted(q.search(b, 0)); }
            finally { Thread.interrupted(); }
            assertTrue(q.search(b, 0).completed());
            InvocationTargetException failure = assertThrows(InvocationTargetException.class,
                    () -> node(owner(limit, QuiescenceSeeCorpus.HCE), Board.fromFen(BLOCK), 256, limit, -32769, 32769));
            assertEquals("Aborted", failure.getCause().getClass().getSimpleName());
        }
    }

    @Test void suffixKeepsConsumedHorizonAndRealRepetitionHistory() {
        long[] b = Board.fromFen("r6k/8/8/8/8/8/8/1K6 b - - 1 1");
        var history = GameHistory.builder(b);
        String[] cycle = {"a8b8", "b1a1", "b8a8", "a1b1"};
        for(int i = 0; i < 7; i++) {
            b = ExhaustiveOracle.child(b, QuiescenceDepthCorpus.move(b, cycle[i % 4])); history.appendPosition(b);
        }
        for(int limit : LIMITS) {
            ExactSearch q = owner(limit, (a, p) -> 37);
            assertEquals(0, q.searchQuiescenceSuffix(b, history.snapshot(), limit, ExactSearch.NEVER_CANCELLED).score());
            assertEquals(-37, q.searchQuiescenceSuffix(b, GameHistory.initial(b), limit, ExactSearch.NEVER_CANCELLED).score());
        }
        long[] mate = Board.fromFen(QuiescenceDepthCorpus.MATE_FIVE);
        ExactSearch q = owner(4, QuiescenceSeeCorpus.HCE);
        long[] child = ExhaustiveOracle.child(mate, QuiescenceDepthCorpus.move(mate, "h3h7"));
        var path = GameHistory.builder(mate); path.appendPosition(child);
        assertEquals(q.search(mate, 0).score(), -q.searchQuiescenceSuffix(child, path.snapshot(), 1, ExactSearch.NEVER_CANCELLED).score());
        assertNotEquals(q.search(mate, 0).score(), -q.search(child, 0).score());
        assertThrows(IllegalArgumentException.class, () -> new ExactSearch().searchQuiescenceSuffix(mate, GameHistory.initial(mate), 0, ExactSearch.NEVER_CANCELLED));
    }

    @Test void fullCorpusAndCausalWitnessesValidate() {
        try(PrintStream out = new PrintStream(OutputStream.nullOutputStream())) { QuiescenceDepthCorpus.run(out); }
    }

    @Test void laterMateAndDrawAreLostButTheEarlierSeeCounterexamplesSurvive() {
        for(int limit : LIMITS) {
            ExactSearch q = owner(limit, QuiescenceSeeCorpus.HCE);
            assertEquals(limit == 4 ? 2659 : 32763, q.search(Board.fromFen(QuiescenceDepthCorpus.MATE_FIVE), 0).score());
            assertEquals(limit == 4 ? -232 : 0, q.search(Board.fromFen(QuiescenceDepthCorpus.DRAW_FIVE), 0).score());
            assertEquals(32765, q.search(Board.fromFen("5r1k/4Qpbp/4NN1N/8/8/8/8/4K3 w - - 0 1"), 0).score());
            assertEquals(-410, q.search(Board.fromFen("b1q4k/8/8/2p5/N7/8/8/R3K3 w - - 0 1"), 0).score());
            assertEquals(0, q.search(Board.fromFen("3r4/1P6/8/8/8/1k6/p7/K7 w - - 0 1"), 0).score());
        }
    }

    @Test void boundedNormalTransitionMatchesExhaustiveEnumeration() {
        for(String fen : new String[] {CAPTURE, QuiescenceDepthCorpus.MATE_FIVE, "1r5k/P7/8/8/8/8/8/7K w - - 0 1"}) {
            long[] b = Board.fromFen(fen);
            for(int mode : LIMITS) {
                int expected = new QuiescenceOracle(QuiescenceSeeCorpus.HCE, mode)
                        .score(b, new SearchLineHistory(GameHistory.initial(b)), 1, 0);
                ExactSearchResult actual = owner(mode, QuiescenceSeeCorpus.HCE).search(b, 1);
                assertTrue(actual.completed()); assertEquals(expected, actual.score());
            }
        }
    }

    private static ExactSearch owner(int limit, ExactEvaluator e) { return ExactSearch.quiescenceResearch(e, limit); }
    private static int node(ExactSearch q, long[] b, int ply, int qply, int alpha, int beta) throws Exception {
        System.arraycopy(b, 0, ((long[][])field(q, "boards"))[ply], 0, Board.MAX_BITBOARDS);
        set(q, "history", new SearchLineHistory(GameHistory.initial(b)));
        set(q, "cancelled", ExactSearch.NEVER_CANCELLED);
        Method method = ExactSearch.class.getDeclaredMethod("quiescence", int.class, int.class, int.class, int.class);
        method.setAccessible(true);
        return (int)method.invoke(q, ply, qply, alpha, beta);
    }
    private static Object field(ExactSearch q, String name) throws Exception {
        Field f = ExactSearch.class.getDeclaredField(name); f.setAccessible(true); return f.get(q);
    }
    private static void set(ExactSearch q, String name, Object value) throws Exception {
        Field f = ExactSearch.class.getDeclaredField(name); f.setAccessible(true); f.set(q, value);
    }
    private static void aborted(ExactSearchResult r) {
        assertFalse(r.completed()); assertEquals(-1, r.completedDepth()); assertEquals(Value.INVALID, r.score());
        assertFalse(r.hasMove()); assertEquals(0, r.principalVariation().length);
    }
}
