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
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
class QuiescenceTtResearchTest {
    private static final String TACTICAL = "4k3/8/2p1p3/3q4/2P1P3/3Q4/8/4K3 w - - 0 1";
    private static final String WINNING = "4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1";
    private static final int INF = ExactSearch.INFINITY;

    @Test void establishedCorpusFullWindowsAndNarrowBoundsMatchReferenceAndExhaustiveOracle() {
        assertEquals(42, QuiescenceDepthCorpus.fixtures().size());
        try(PrintStream out = new PrintStream(OutputStream.nullOutputStream())) { QuiescenceTtCorpus.run(out); }
    }

    @Test void naturalCaptureTranspositionsReuseExactAndBoundEvidenceAndReduceDuplicateWork() {
        long[] b = Board.fromFen(TACTICAL);
        // Two actual admitted qlines converge after different pawn recaptures.
        var left = route(b, "d3d5", "c6d5", "c4d5", "e6d5", "e4d5");
        var right = route(b, "d3d5", "c6d5", "e4d5", "e6d5", "c4d5");
        assertArrayEquals(left.board, right.board);
        assertEquals(key(left.board, left.game), key(right.board, right.game));
        var off = ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE).search(b, 0);
        ExactSearch on = owner(QuiescenceSeeCorpus.HCE);
        var result = on.search(b, 0);
        assertEquals(1072, result.score()); assertEquals(off.score(), result.score());
        assertEquals(31, off.qnodes()); assertEquals(28, result.qnodes());
        assertEquals(7, on.qttDiagnostics()[1]); assertEquals(3, on.qttDiagnostics()[2]);
        assertEquals(2, on.qttDiagnostics()[3]);
        QuiescenceTtCorpus.verifyPv(b, GameHistory.initial(b), result, ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE));
    }

    @Test void exactAndCuttingBoundsReturnFailSoftWithoutConsumingHashMoves() throws Exception {
        long[] b = Board.fromFen(WINNING); var game = GameHistory.initial(b);
        ExactSearch q = owner((a, p) -> { throw new AssertionError("Hit evaluated"); });
        for(int[] c : new int[][] {{0, 7}, {1, 53}, {2, -53}}) {
            table(q).clear(); table(q).save(key(b, game), 0, c[0], c[1], Long.MAX_VALUE);
            long before = q.visitedNodes();
            assertEquals(c[1], node(q, b, game, 17, 3, -50, 50));
            assertEquals(1, q.visitedNodes() - before);
            assertEquals(0, ((int[])field(q, "pvLength"))[17]);
            assertTrue(q.qttDiagnostics()[2 + c[0]] > 0);
        }
    }

    @Test void noncuttingBoundsLeaveBothWindowAndGeneratedTraversalUnchanged() throws Exception {
        long[] b = Board.fromFen(TACTICAL); var game = GameHistory.initial(b);
        List<Long> visits = new ArrayList<>();
        ExactSearch q = owner(new ExactEvaluator() {
            public int evaluate(long[] a, int p) { return Eval.evaluate(a); }
            public void child(long[] a, long[] child, int p) { visits.add(child[Board.KEY]); }
        });
        int expected = node(q, b, game, 0, 0, -2000, 2000);
        List<Long> reference = List.copyOf(visits);
        for(int type : new int[] {TTable.TYPE_LOWER, TTable.TYPE_UPPER}) {
            table(q).clear(); visits.clear();
            table(q).save(key(b, game), 0, type, type == 1 ? 1999 : -1999, Long.MAX_VALUE);
            assertEquals(expected, node(q, b, game, 0, 0, -2000, 2000));
            assertEquals(reference, visits, "Bound tightening or hash promotion changed traversal");
        }
    }

    @Test void completedStoresUseOriginalWindowIncludingEqualityAndActualFailSoftScores() throws Exception {
        ExactSearch q = owner(QuiescenceSeeCorpus.HCE);
        for(long[] b : new long[][] {Board.startingPosition(), Board.fromFen(WINNING), Board.fromFen(TACTICAL)}) {
            var game = GameHistory.initial(b);
            int score = q.search(b, 0).score();
            for(int[] window : new int[][] {{-INF, INF}, {score, score + 50}, {score - 50, score}}) {
                var r = q.searchWindow(b, game, 0, window[0], window[1], ExactSearch.NEVER_CANCELLED);
                var entry = new TTable.TEntry(); assertTrue(table(q).probe(key(b, game), entry));
                int type = r.score() <= window[0] ? 2 : r.score() >= window[1] ? 1 : 0;
                assertEquals(type, (entry.data >>> 8) & 3); assertEquals(0, entry.data & 255);
                assertEquals(r.score(), (int)(entry.data >> 32));
                QuiescenceTtCorpus.bound(r.score(), score, window[0], window[1]);
            }
        }
        long[] b = Board.fromFen(WINNING);
        var r = q.searchWindow(b, GameHistory.initial(b), 0, -INF, 0, ExactSearch.NEVER_CANCELLED);
        assertTrue(r.score() > 0); assertTrue(r.hasMove()); // discovered tactical value, not beta
        ExactSearch stand = owner((a, p) -> 37);
        var s = stand.searchWindow(Board.startingPosition(), GameHistory.initial(Board.startingPosition()),
                0, -50, 20, ExactSearch.NEVER_CANCELLED);
        assertEquals(37, s.score()); assertFalse(s.hasMove());
    }

    @Test void mateEntriesNormalizeActualPathPlyAndNeverQply() throws Exception {
        long[] positive = Board.fromFen("7k/6p1/5KQ1/8/8/8/8/8 w - - 0 1");
        long[] m = Board.fromFen(QuiescenceDepthCorpus.MATE_FIVE);
        long[] negative = ExhaustiveOracle.child(m, QuiescenceDepthCorpus.move(m, "h3h7"));
        ExactSearch q = owner(QuiescenceSeeCorpus.HCE);
        for(long[] b : new long[][] {positive, negative}) {
            table(q).clear(); var game = GameHistory.initial(b);
            int first = node(q, b, game, 11, 73, -INF, INF);
            assertTrue(Math.abs(first) >= TranspositionScores.MATE_THRESHOLD);
            var entry = new TTable.TEntry(); assertTrue(table(q).probe(key(b, game), entry));
            assertEquals(TranspositionScores.toTableScore(first, 11), (int)(entry.data >> 32));
            long before = q.visitedNodes();
            int second = node(q, b, game, 23, 1, -INF, INF);
            assertEquals(first > 0 ? first - 12 : first + 12, second);
            assertEquals(1, q.visitedNodes() - before);
        }
    }

    @Test void terminalDrawAdjudicationPrecedesPoisonedQttAndStaticEvaluation() throws Exception {
        ExactSearch q = owner((b, p) -> { throw new AssertionError("Terminal evaluated"); });
        for(String fen : new String[] {"7k/6Q1/5K2/8/8/8/8/8 b - - 100 1",
                "7k/5K2/6Q1/8/8/8/8/8 b - - 0 1", "4k3/8/8/8/8/8/P7/4K3 w - - 100 1",
                "4k3/8/8/8/8/8/8/4K3 w - - 0 1"}) {
            long[] b = Board.fromFen(fen); var game = GameHistory.initial(b);
            table(q).save(key(b, game), 0, 0, 12345, 0);
            int expected = ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE).search(b, 0).score();
            assertEquals(expected, node(q, b, game, 0, 0, -INF, INF));
        }
        long[] b = Board.startingPosition(); var path = GameHistory.builder(b);
        for(int i = 0; i < 8; i++) { b = play(b, new String[] {"g1f3", "g8f6", "f3g1", "f6g8"}[i % 4]); path.appendPosition(b); }
        table(q).save(key(b, path.snapshot()), 0, 0, 12345, 0);
        assertEquals(0, node(q, b, path.snapshot(), 0, 0, -INF, INF));
        assertEquals(0, q.qttDiagnostics()[0]);
    }

    @Test void sameBoardWithDifferentHistoryDoesNotReuseAndQlineRepetitionIsPreserved() throws Exception {
        long[] b = Board.fromFen("r6k/8/8/8/8/8/8/1K6 b - - 1 1");
        var path = GameHistory.builder(b);
        for(int i = 0; i < 7; i++) { b = play(b, new String[] {"a8b8", "b1a1", "b8a8", "a1b1"}[i % 4]); path.appendPosition(b); }
        var fresh = GameHistory.initial(b); var repeated = path.snapshot();
        assertNotEquals(key(b, fresh), key(b, repeated));
        ExactSearch q = owner((a, p) -> 37);
        assertEquals(-37, node(q, b, fresh, 0, 0, -INF, INF));
        assertEquals(0, node(q, b, repeated, 0, 0, -INF, INF));
        assertEquals(0, q.qttDiagnostics()[1]); // the same board's old proof did not apply
        ExactSearch off = ExactSearch.quiescenceResearch((a, p) -> 37);
        assertEquals(off.search(b, repeated, 0, ExactSearch.NEVER_CANCELLED).score(), q.search(b, repeated, 0, ExactSearch.NEVER_CANCELLED).score());
    }

    @Test void ruleStateAndConservativeQuietPathIdentityAreRetained() throws Exception {
        long[] zero = Board.fromFen("4r1k1/8/8/8/8/8/8/4K3 w - - 0 1");
        long[] ninetyNine = Board.fromFen("4r1k1/8/8/8/8/8/8/4K3 w - - 99 1");
        assertEquals(zero[Board.KEY], ninetyNine[Board.KEY]);
        ExactSearch q = owner((a, p) -> 37);
        assertEquals(-37, node(q, zero, GameHistory.initial(zero), 0, 0, -INF, INF));
        assertEquals(0, node(q, ninetyNine, GameHistory.initial(ninetyNine), 0, 0, -INF, INF));
        assertEquals(0, q.qttDiagnostics()[1]);
        // Same board/rule state after commuting quiet knight moves: history differs.
        var a = route("g1f3", "g8f6", "b1c3", "b8c6");
        var b = route("b1c3", "b8c6", "g1f3", "g8f6");
        assertArrayEquals(a.board, b.board); assertNotEquals(key(a.board, a.game), key(b.board, b.game));
    }

    @Test void incompleteNodesDoNotStoreEvenIfLastEvaluationRaisesCancellation() throws Exception {
        var stop = new AtomicBoolean();
        ExactSearch q = owner((b, p) -> { stop.set(true); return 37; });
        long[] b = Board.startingPosition(); var game = GameHistory.initial(b);
        assertFalse(q.search(b, game, 0, stop::get).completed());
        assertFalse(table(q).probe(key(b, game), new TTable.TEntry())); assertEquals(0, q.qttDiagnostics()[5]);
        q = owner(QuiescenceSeeCorpus.HCE); final ExactSearch budgeted = q;
        b = Board.fromFen(TACTICAL); game = GameHistory.initial(b);
        assertFalse(q.search(b, game, 0, () -> budgeted.visitedNodes() >= 4).completed());
        assertFalse(table(q).probe(key(b, game), new TTable.TEntry()));
        table(q).clear();
        final long[] position = b; final GameHistory h = game;
        InvocationTargetException aborted = assertThrows(InvocationTargetException.class,
                () -> node(budgeted, position, h, 256, 0, -INF, INF));
        assertEquals("Aborted", aborted.getCause().getClass().getSimpleName());
        assertFalse(table(q).probe(key(b, game), new TTable.TEntry()));
        long[] checked = Board.fromFen("4r3/8/8/8/8/8/8/2B1K1k1 w - - 0 1");
        aborted = assertThrows(InvocationTargetException.class,
                () -> node(budgeted, checked, GameHistory.initial(checked), 256, 100, -INF, INF));
        assertEquals("Aborted", aborted.getCause().getClass().getSimpleName());
        assertFalse(table(q).probe(key(checked, GameHistory.initial(checked)), new TTable.TEntry()));
        Thread.currentThread().interrupt();
        try { assertFalse(q.search(b, 0).completed()); }
        finally { Thread.interrupted(); }
        assertTrue(q.search(b, 0).completed());
    }

    @Test void eachFixedDepthInvocationIsColdEvenInsideOneRequestAndBaselinesStayIndependent() throws Exception {
        ExactSearch q = owner(QuiescenceSeeCorpus.HCE); long[] b = Board.fromFen(TACTICAL);
        assertNull(field(q, "table")); assertEquals(Integer.MAX_VALUE, field(q, "qdepthLimit"));
        assertEquals(0, field(q, "qsearchPruning")); assertNull(field(q, "quietHistory"));
        assertNull(field(ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE), "qtable"));
        assertNull(field(new ExactSearch(), "qtable"));
        q.beginRequest(); var first = q.search(b, 2); long[] stats = q.qttDiagnostics();
        q.search(b, 1); var again = q.search(b, 2); q.endRequest();
        assertEquals(first.score(), again.score()); assertEquals(first.nodes(), again.nodes());
        assertArrayEquals(first.principalVariation(), again.principalVariation()); assertArrayEquals(stats, q.qttDiagnostics());
        assertEquals(new QuiescenceOracle(QuiescenceSeeCorpus.HCE).score(b,
                new SearchLineHistory(GameHistory.initial(b)), 2, 0), first.score());
        assertThrows(IllegalArgumentException.class, () -> ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE, 0, true));
        assertThrows(IllegalStateException.class, () -> ExactSearch.quiescenceResearch(QuiescenceSeeCorpus.HCE).qttDiagnostics());
    }

    private record Route(long[] board, GameHistory game) {}
    private static Route route(String... moves) {
        return route(Board.startingPosition(), moves);
    }
    private static Route route(long[] b, String... moves) {
        var h = GameHistory.builder(b);
        for(String move : moves) { b = play(b, move); h.appendPosition(b); }
        return new Route(b, h.snapshot());
    }
    private static long[] play(long[] b, String move) { return ExhaustiveOracle.child(b, QuiescenceDepthCorpus.move(b, move)); }
    private static ExactSearch owner(ExactEvaluator e) { return ExactSearch.quiescenceResearch(e, ExactSearch.QSEARCH_QTT, true); }
    private static long key(long[] b, GameHistory h) { return SearchKey.key(b, SearchKey.rootHistory(b, h)); }
    private static TTable table(ExactSearch q) throws Exception { return (TTable)field(q, "qtable"); }
    private static int node(ExactSearch q, long[] b, GameHistory game, int ply, int qply, int alpha, int beta) throws Exception {
        System.arraycopy(b, 0, ((long[][])field(q, "boards"))[ply], 0, Board.MAX_BITBOARDS);
        ((long[])field(q, "historyKeys"))[ply] = SearchKey.rootHistory(b, game);
        set(q, "history", new SearchLineHistory(game)); set(q, "cancelled", ExactSearch.NEVER_CANCELLED);
        Method method = ExactSearch.class.getDeclaredMethod("quiescence", int.class, int.class, int.class, int.class);
        method.setAccessible(true); return (int)method.invoke(q, ply, qply, alpha, beta);
    }
    private static Object field(ExactSearch q, String name) throws Exception {
        Field f = ExactSearch.class.getDeclaredField(name); f.setAccessible(true); return f.get(q);
    }
    private static void set(ExactSearch q, String name, Object value) throws Exception {
        Field f = ExactSearch.class.getDeclaredField(name); f.setAccessible(true); f.set(q, value);
    }
}
