package com.ohinteractive.seedv6.search.exact;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.driver.ExactSearchAdapter;
import com.ohinteractive.seedv6.search.driver.SearchDriver;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

class StaticNullPruningTest {
    private static final String QUEEN = "7k/8/8/8/8/8/Q7/K7 w - - %d 1";
    private static final String KIWIPETE = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
    private static final class Writes extends TTable {
        final List<Long> keys = new ArrayList<>();
        Writes() { super(1); }
        @Override public void save(long key, int depth, int type, int score, long move) {
            keys.add(key);
            super.save(key, depth, type, score, move);
        }
    }
    private static Field field(String name) throws Exception {
        Field f = ExactSearch.class.getDeclaredField(name); f.setAccessible(true); return f;
    }
    // Exercise the precise interior invocation, without a production test seam.
    private static int scout(ExactSearch s, long[] board, GameHistory game, int depth,
                             int ply, int beta, boolean scout) throws Exception {
        long[][] boards = (long[][]) field("boards").get(s);
        System.arraycopy(board, 0, boards[ply], 0, Board.MAX_BITBOARDS);
        field("history").set(s, new SearchLineHistory(game, depth));
        if(field("cancelled").get(s) == null) field("cancelled").set(s, ExactSearch.NEVER_CANCELLED);
        ((long[]) field("historyKeys").get(s))[ply] = SearchKey.rootHistory(board, game);
        var method = ExactSearch.class.getDeclaredMethod("negamax", int.class, int.class,
                int.class, int.class, long.class, boolean.class);
        method.setAccessible(true);
        return (int) method.invoke(s, depth, ply, beta - 1, beta, 0L, scout);
    }

    @Test void predictsOnlyBetaWithNoPvOrMathematicalTtWrite() throws Exception {
        long[] b = Board.fromFen(QUEEN.formatted(0)); var table = new Writes();
        var calls = new AtomicInteger();
        var s = ExactSearch.withStaticNullPruning((board, ply) -> { calls.incrementAndGet(); return Eval.evaluate(board); }, table);
        assertEquals(1, scout(s, b, GameHistory.initial(b), 2, 7, 1, true));
        assertEquals(1, calls.get()); assertEquals(1, s.visitedNodes());
        assertEquals(1, field("selectiveEpoch").getLong(s));
        assertEquals(0, ((int[]) field("pvLength").get(s))[7]);
        assertTrue(table.keys.isEmpty());
        assertTrue(new ExactSearch().search(b, 2).score() >= 1);
    }

    @Test void depthProvenanceCheckAndMateWindowsAreEligibilityNotMargins() throws Exception {
        for(int depth : new int[] {1, 2, 3}) for(boolean provenance : new boolean[] {false, true}) {
            long[] b = Board.fromFen(QUEEN.formatted(0));
            var rootCalls = new AtomicInteger();
            var s = ExactSearch.withStaticNullPruning((board, ply) -> {
                if(ply == 7) rootCalls.incrementAndGet(); return Eval.evaluate(board);
            }, new Writes());
            scout(s, b, GameHistory.initial(b), depth, 7, 1, provenance);
            assertEquals(depth == 2 && provenance ? 1 : 0, rootCalls.get());
            if(depth <= 2 && !(depth == 2 && provenance)) assertEquals(0, field("selectiveEpoch").getLong(s));
        }
        for(String fen : new String[] {"7k/8/8/8/8/8/Q7/K6r w - - 0 1", QUEEN.formatted(0)}) {
            long[] b = Board.fromFen(fen);
            var s = ExactSearch.withStaticNullPruning((board, ply) -> Eval.evaluate(board), new Writes());
            scout(s, b, GameHistory.initial(b), 2, 7, fen.contains("K6r") ? -2000 : -32512, true);
            assertEquals(0, field("selectiveEpoch").getLong(s));
        }
    }

    @Test void rule50HorizonAndTerminalPrecedenceExcludeDemonstratedFalsePrunes() throws Exception {
        for(int clock : new int[] {98, 99, 100}) {
            long[] b = Board.fromFen(QUEEN.formatted(clock));
            assertTrue(Eval.evaluate(b) - 960 >= 1); // The rejected unguarded policy cuts here.
            var s = ExactSearch.withStaticNullPruning((board, ply) -> Eval.evaluate(board), new Writes());
            assertEquals(0, scout(s, b, GameHistory.initial(b), 2, 7, 1, true));
            assertEquals(0, field("selectiveEpoch").getLong(s));
            assertEquals(0, new ExactSearch().search(b, 2).score());
        }
        long[] mate = Board.fromFen("7k/6Q1/6K1/8/8/8/8/8 b - - 100 1");
        var s = ExactSearch.withStaticNullPruning((b, ply) -> fail("Terminal must not evaluate"), new Writes());
        assertEquals(-ExactSearch.MATE_SCORE + 7, scout(s, mate, GameHistory.initial(mate), 2, 7, -2000, true));
    }

    @Test void exactTtEvidenceResolvesBeforeAddedEvaluation() throws Exception {
        long[] b = Board.fromFen(QUEEN.formatted(0)); var game = GameHistory.initial(b); var table = new Writes();
        table.save(SearchKey.key(b, SearchKey.rootHistory(b, game)), 2, TTable.TYPE_EXACT, 17, 0);
        var s = ExactSearch.withStaticNullPruning((board, ply) -> fail("Applicable exact TT precedes raw E"), table);
        assertEquals(17, scout(s, b, game, 2, 7, 1, true));
        assertEquals(0, field("selectiveEpoch").getLong(s));
    }

    @Test void predictionTaintsAncestorsAndCompletedPublicationButLeavesIndependentExactPaths() {
        long[] b = Board.fromFen("7k/8/8/8/8/8/Q7/K7 b - - 0 1"); var game = GameHistory.initial(b);
        var table = new Writes();
        var s = ExactSearch.withStaticNullPruning((board, ply) -> Eval.evaluate(board), table);
        var result = s.searchWindow(b, game, 3, -1, 0, ExactSearch.NEVER_CANCELLED);
        assertTrue(result.completed()); assertTrue(result.selective());
        assertFalse(table.keys.contains(SearchKey.key(b, SearchKey.rootHistory(b, game))), "Tainted root must not publish an ordinary TT bound");
        assertFalse(new ExactSearch().search(b, 3).selective());
        assertFalse(new ExactSearch(SearchEvaluation.handcrafted(), new TTable(1)).search(b, 3).selective());
        var published = new SearchDriver().search(new SearchRequest(Board.fromFen(KIWIPETE), 5));
        assertTrue(published.lastCompletedResult().selective());
        assertTrue(published.targetDepthCompleted());
    }

    @Test void neuralIsolationAndUnknownEvaluatorDefinitionsDoNotInheritHceCalibration() throws Exception {
        for(var adapter : List.of(new ExactSearchAdapter(SearchEvaluation.incremental(NnueNetwork.initialized(194)), new TTable(1)),
                new ExactSearchAdapter(SearchEvaluation.handcraftedIsolation(), new TTable(1)),
                new ExactSearchAdapter((b, ply) -> Eval.evaluate(b), new TTable(1)),
                new ExactSearchAdapter(SearchEvaluation.handcrafted(), null))) {
            Field f = ExactSearchAdapter.class.getDeclaredField("exact"); f.setAccessible(true);
            assertFalse(field("reverseFutility").getBoolean(f.get(adapter)));
        }
    }

    @Test void cancellationDuringAddedEvaluationPublishesNoPredictionOrTtEntry() throws Exception {
        long[] b = Board.fromFen(QUEEN.formatted(0)); var cancelled = new AtomicBoolean(); var table = new Writes();
        var s = ExactSearch.withStaticNullPruning((board, ply) -> { cancelled.set(true); return Eval.evaluate(board); }, table);
        field("cancelled").set(s, (java.util.function.BooleanSupplier) cancelled::get);
        assertThrows(InvocationTargetException.class, () -> scout(s, b, GameHistory.initial(b), 2, 7, 1, true));
        assertEquals(0, field("selectiveEpoch").getLong(s)); assertTrue(table.keys.isEmpty());
    }
}
