package com.ohinteractive.seedv6.search.exact;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

/** SR-011 exact static-horizon domain; TT-off remains the independent oracle. */
class MateDistancePruningTest {
    private static final int M = ExactSearch.MATE_SCORE, S = ExactSearch.MAX_STATIC_SCORE;
    private static final int INF = ExactSearch.INFINITY;
    private static final ExactEvaluator HCE = (b, p) -> Eval.evaluate(b);
    private static final String WIN = "7k/8/5KQ1/8/8/8/8/8 w - - 0 1";
    private static final String LOSS = "8/7k/5K2/8/8/8/8/6Q1 b - - 0 1";

    private static Field field(String name) throws Exception {
        var f = ExactSearch.class.getDeclaredField(name); f.setAccessible(true); return f;
    }

    private static int node(ExactSearch search, long[] b, int d, int p, int a, int beta) throws Exception {
        var game = GameHistory.initial(b);
        System.arraycopy(b, 0, ((long[][]) field("boards").get(search))[p], 0, 6);
        field("history").set(search, new SearchLineHistory(game, d));
        field("cancelled").set(search, ExactSearch.NEVER_CANCELLED);
        long[] keys = (long[]) field("historyKeys").get(search);
        if(keys != null) keys[p] = SearchKey.rootHistory(b, game);
        var method = ExactSearch.class.getDeclaredMethod("negamax", int.class, int.class, int.class,
                int.class, long.class, boolean.class);
        method.setAccessible(true);
        try { return (int) method.invoke(search, d, p, a, beta, 0L, false); }
        catch(InvocationTargetException e) { throw (Exception) e.getCause(); }
    }

    private static long key(long[] b) { return SearchKey.key(b, SearchKey.rootHistory(b, GameHistory.initial(b))); }
    private static long[] legal(long[] b) {
        long[] ms = new long[512];
        int n = Gen.genAll(b[0], b[1], b[2], b[3], (int) b[4], b[5], true, ms, new long[6]);
        return Arrays.copyOf(ms, n);
    }
    private static void bound(int v, int score, int a, int b) {
        if(score <= a) assertTrue(v <= score, "UPPER");
        else if(score >= b) assertTrue(v >= score, "LOWER");
        else assertEquals(v, score, "EXACT");
    }
    private static final class Writes extends TTable {
        int stores;
        Writes() { super(1); }
        @Override public void save(long key, int depth, int type, int score, long move) {
            stores++; super.save(key, depth, type, score, move);
        }
    }

    @Test void genericRootBoundsHaveNoMoveOrTtWriteAndUseStrongestDomainEndpoint() {
        long[] b = Board.startingPosition(); var game = GameHistory.initial(b);
        for(int d : new int[] {1, 2, 7}) {
            int lower = d == 1 ? -S : -M + 2, upper = M - 1;
            for(int[] w : new int[][] {{upper, upper + 1, upper}, {lower - 1, lower, lower}}) {
                var table = new Writes();
                var search = new ExactSearch((bb, p) -> fail("Domain return cannot evaluate"), table);
                var r = search.searchWindow(b, game, d, w[0], w[1], ExactSearch.NEVER_CANCELLED);
                assertTrue(r.completed()); assertFalse(r.selective()); assertEquals(w[2], r.score());
                assertEquals(1, r.nodes()); assertFalse(r.hasMove()); assertEquals(0, r.principalVariation().length);
                assertEquals(0, table.stores);
            }
        }
    }

    @Test void terminalAndStaticStagesPrecedeDomainAndTtEvenAtLastPathSlot() throws Exception {
        for(String fen : List.of("7k/6Q1/5K2/8/8/8/8/8 b - - 100 1",
                "7k/5Q2/5K2/8/8/8/8/8 b - - 0 1", "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
                "7k/8/5KQ1/8/8/8/8/8 w - - 100 1")) {
            long[] b = Board.fromFen(fen);
            boolean mate = fen.startsWith("7k/6Q1");
            for(int p : new int[] {0, 9, 254, 256}) {
                var table = new TTable(1); table.save(key(b), 2, TTable.TYPE_EXACT, 1234, 0);
                assertEquals(mate ? -M + p : 0, node(new ExactSearch((bb, pp) -> fail("terminal"), table),
                        b, Math.min(2, 256 - p), p, M - 1, M));
            }
        }
        for(int value : new int[] {-S, -S + 1, 0, S - 1, S}) {
            long[] b = Board.startingPosition(); var table = new TTable(1);
            table.save(key(b), 0, TTable.TYPE_EXACT, 712, 0);
            assertEquals(value, node(new ExactSearch((bb, p) -> value, table), b, 0, 256, M - 1, M));
            for(int d : new int[] {1, 2}) assertEquals((d & 1) == 0 ? value : -value,
                    node(new ExactSearch((bb, p) -> value, new TTable(1)), b, d, 256 - d, -INF, INF));
        }
    }

    @Test void mateDistancesAndLastSupportedPlyKeepExactScoresAndLegalWitnesses() throws Exception {
        String[] fens = {WIN, LOSS, "8/7k/5K2/8/8/8/8/Q7 w - - 0 1",
                "6k1/8/5K2/8/8/8/8/Q7 b - - 0 1", "8/7k/8/6K1/8/8/8/Q7 w - - 0 1",
                "8/7k/8/6K1/8/8/8/Q7 b - - 0 1", "8/8/7k/8/6K1/8/8/Q7 w - - 0 1"};
        for(int i = 0; i < fens.length; i++) {
            long[] b = Board.fromFen(fens[i]); int d = i + 1;
            int expected = (d & 1) == 1 ? M - d : -M + d;
            assertEquals(expected, new ExactSearch(HCE).search(b, d).score());
            for(int p : new int[] {0, 11, 256 - d}) {
                var search = new ExactSearch(HCE, new TTable(1));
                assertEquals(expected > 0 ? expected - p : expected + p, node(search, b, d, p, -INF, INF));
                int length = ((int[]) field("pvLength").get(search))[p];
                assertTrue(length > 0); long[] line = ((long[][]) field("pv").get(search))[p];
                long first = line[0]; assertTrue(Arrays.stream(legal(b)).anyMatch(m -> m == first));
            }
            var r = new ExactSearch(HCE, new TTable(1)).search(b, d);
            PvsSearchTest.assertExactPrefixes(b, GameHistory.initial(b), d, r, HCE);
        }
    }

    @Test void originalCallerWindowsAndFiniteHorizonGapDecisionsMatchTheOracle() throws Exception {
        for(String fen : List.of(WIN, LOSS, Board.FEN_STARTING_POSITION)) {
            long[] b = Board.fromFen(fen);
            for(int d : new int[] {1, 2, 3}) for(int p : new int[] {0, 7, 253}) {
                int v = node(new ExactSearch(HCE), b, d, p, -INF, INF);
                int l = d == 1 ? -S : -M + p + 2, u = M - p - 1;
                int slowWin = M - p - ((d - 1) | 1), slowLoss = -M + p + (d & ~1);
                for(int a : new int[] {l - 1, l, l + 1, u - 1, u, u + 1, -S - 1, -S,
                        S, S + 1, (S + slowWin) / 2, (slowLoss - S) / 2}) {
                    if(a < -INF || a >= INF) continue;
                    int result = node(new ExactSearch(HCE, new TTable(1)), b, d, p, a, a + 1);
                    bound(v, result, a, a + 1);
                }
            }
        }
        // Same gap window, opposite classifications. It must search, not return from crossing bounds.
        for(String fen : List.of(WIN, Board.FEN_STARTING_POSITION)) {
            long[] b = Board.fromFen(fen); var search = new ExactSearch(HCE, new TTable(1));
            int value = node(new ExactSearch(HCE), b, 3, 7, -INF, INF);
            bound(value, node(search, b, 3, 7, S + 20, S + 21), S + 20, S + 21);
            assertTrue(search.visitedNodes() > 1);
        }
    }

    @Test void mateEndpointNormalizationAndOriginalWindowTtPrecedence() throws Exception {
        for(int p = 0; p <= 256; p++) {
            assertTrue(TranspositionScores.isMateScore(-M + p));
            assertEquals(-M, TranspositionScores.toTableScore(-M + p, p));
            if(p < 256) assertEquals(M - 1, TranspositionScores.toTableScore(M - p - 1, p));
            if(p < 255) assertEquals(-M + 2, TranspositionScores.toTableScore(-M + p + 2, p));
            assertEquals(S, TranspositionScores.toTableScore(S, p));
            assertEquals(-S, TranspositionScores.toTableScore(-S, p));
        }
        for(String fen : List.of(WIN, LOSS)) {
            long[] b = Board.fromFen(fen); int d = fen.equals(WIN) ? 1 : 2;
            for(int type : new int[] {TTable.TYPE_EXACT, TTable.TYPE_LOWER, TTable.TYPE_UPPER}) {
                var table = new TTable(1); var search = new ExactSearch(HCE, table);
                int normalized = fen.equals(WIN) ? M - 1 : -M + 2;
                table.save(key(b), d, type, normalized, legal(b)[0]);
                for(int p : new int[] {7, 128, 254}) {
                    int value = normalized > 0 ? normalized - p : normalized + p;
                    int a = type == TTable.TYPE_LOWER ? value - 1 : type == TTable.TYPE_UPPER ? value : -INF;
                    int beta = type == TTable.TYPE_LOWER ? value : type == TTable.TYPE_UPPER ? value + 1 : INF;
                    long before = search.visitedNodes();
                    assertEquals(value, node(search, b, d, p, a, beta));
                    assertEquals(1, search.visitedNodes() - before);
                }
            }
        }
    }

    @Test void nonCuttingTtEndpointDoesNotUpgradeItsUnrelatedHashMove() throws Exception {
        for(String fen : List.of(WIN, LOSS)) {
            long[] b = Board.fromFen(fen); int d = fen.equals(WIN) ? 1 : 2, p = 7;
            int value = node(new ExactSearch(HCE), b, d, p, -INF, INF);
            var table = new TTable(1); var search = new ExactSearch(HCE, table);
            table.save(key(b), d, value > 0 ? TTable.TYPE_LOWER : TTable.TYPE_UPPER,
                    TranspositionScores.toTableScore(value, p), legal(b)[0]);
            assertEquals(value, node(search, b, d, p, -INF, INF));
            assertTrue(search.visitedNodes() > 1);
            var entry = new TTable.TEntry(); assertTrue(table.probe(key(b), entry));
            assertEquals(TTable.TYPE_EXACT, (entry.data >>> 8) & 3, "Searched endpoint against ORIGINAL full window");
        }
    }

    @Test void depth256CannotAliasStaticTtAndCancelledBoundsCannotPublish() {
        long[] b = Board.fromFen(WIN); var table = new Writes(); var search = new ExactSearch(HCE, table);
        search.beginRequest(); long mating = Arrays.stream(legal(b)).filter(m -> Move.coordinate(m).equals("g6g7")).findFirst().orElseThrow();
        table.save(key(b), 255, TTable.TYPE_LOWER, 0, mating); int stores = table.stores;
        var r = search.search(b, 256); assertEquals(M - 1, r.score()); assertEquals(2, r.nodes());
        assertEquals(stores, table.stores); search.endRequest();
        var calls = new AtomicInteger();
        var cancelled = search.searchWindow(Board.startingPosition(), GameHistory.initial(Board.startingPosition()),
                2, M - 1, M, () -> calls.incrementAndGet() >= 3);
        assertFalse(cancelled.completed()); assertFalse(cancelled.hasMove());
        assertEquals(0, cancelled.principalVariation().length);
        assertTrue(search.search(b, 1).completed());
    }
}
