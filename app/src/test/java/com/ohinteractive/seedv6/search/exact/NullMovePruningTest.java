package com.ohinteractive.seedv6.search.exact;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.core.util.Zobrist;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

class NullMovePruningTest {
    private static final String QUEEN = "7k/8/8/8/8/8/Q7/K7 w - - %d 1";
    private static Field field(String name) throws ReflectiveOperationException {
        var f = ExactSearch.class.getDeclaredField(name); f.setAccessible(true); return f;
    }
    private static final class Writes extends TTable {
        final List<Long> keys = new ArrayList<>();
        ExactSearch owner;
        Writes() { super(1); }
        private void requireLegalDomain() {
            if(owner == null) return;
            try { assertFalse(field("nullProbeActive").getBoolean(owner), "Synthetic subtree cannot touch Search TT"); }
            catch(ReflectiveOperationException e) { throw new AssertionError(e); }
        }
        @Override public boolean probe(long key, TEntry entry) {
            requireLegalDomain(); return super.probe(key, entry);
        }
        @Override public void save(long key, int depth, int type, int score, long move) {
            requireLegalDomain();
            keys.add(key); super.save(key, depth, type, score, move);
        }
    }
    private static int scout(ExactSearch search, long[] b, int depth, int beta, boolean scout) throws Exception {
        int ply = 7;
        var game = GameHistory.initial(b);
        System.arraycopy(b, 0, ((long[][])field("boards").get(search))[ply], 0, 6);
        field("history").set(search, new SearchLineHistory(game, depth));
        ((long[])field("historyKeys").get(search))[ply] = SearchKey.rootHistory(b, game);
        if(field("cancelled").get(search) == null) field("cancelled").set(search, ExactSearch.NEVER_CANCELLED);
        var method = ExactSearch.class.getDeclaredMethod("negamax", int.class, int.class,
                int.class, int.class, long.class, boolean.class);
        method.setAccessible(true);
        return (int)method.invoke(search, depth, ply, beta - 1, beta, 0L, scout);
    }
    private static long[] pass(long[] b) throws Exception {
        var method = ExactSearch.class.getDeclaredMethod("syntheticPass", long[].class, long[].class);
        method.setAccessible(true); long[] child = new long[6]; method.invoke(null, b, child); return child;
    }
    private static long[] play(long[] b, String coordinate) {
        long[] moves = new long[512], scratch = new long[6];
        int n = Gen.genAll(b[0], b[1], b[2], b[3], (int)b[4], b[5], true, moves, scratch);
        for(int i = 0; i < n; i++) if(Move.coordinate(moves[i]).equals(coordinate)) {
            long[] child = new long[6]; Board.makeMoveInto(b[0], b[1], b[2], b[3], (int)b[4], b[5], moves[i], child);
            return child;
        }
        throw new AssertionError(coordinate);
    }

    @Test void syntheticTransitionPreservesParentAndPiecesAndExpiresEp() throws Exception {
        for(String fen : List.of(Board.FEN_STARTING_POSITION,
                "r3k2r/8/8/3pP3/8/8/8/R3K2R w KQkq d6 95 31",
                "4k3/8/8/8/3Pp3/8/8/4K3 b - d3 99 32")) {
            long[] b = Board.fromFen(fen), before = b.clone(), c = pass(b);
            assertArrayEquals(before, b);
            assertArrayEquals(Arrays.copyOf(b, 4), Arrays.copyOf(c, 4));
            int bs = (int)b[4], cs = (int)c[4];
            assertNotEquals(Board.player(bs), Board.player(cs));
            assertEquals(Value.INVALID, Board.enPassantSquare(cs));
            assertEquals(bs & 30, cs & 30);
            assertEquals(Board.halfMoveClock(bs) + 1, Board.halfMoveClock(cs));
            assertEquals(Board.fullMoveNumber(bs) + Board.player(bs), Board.fullMoveNumber(cs));
            int[] squares = new int[64];
            for(int q = 0; q < 64; q++) squares[q] = Board.getSquare(c[0], c[1], c[2], c[3], q);
            assertEquals(Zobrist.getKey(squares, Board.player(cs), (cs >>> 1) & 15, 0), c[5]);
        }
    }

    @Test void historyBarrierExcludesPassAndRealPrefixButCountsLocalLegalCycles() throws Exception {
        long[] b = Board.startingPosition(), c = pass(b);
        var history = new SearchLineHistory(GameHistory.initial(b), 16);
        history.enterSyntheticPosition(c);
        assertEquals(1, history.size()); assertEquals(0, history.currentOccurrences(c));
        long[] synthetic = c;
        assertThrows(IllegalStateException.class, () -> history.enterSyntheticPosition(synthetic));
        for(int repeat = 0; repeat < 3; repeat++) {
            for(String move : List.of("g8f6", "g1f3", "f6g8", "f3g1")) {
                c = play(c, move); history.pushRealPosition(c);
            }
            assertEquals(repeat + 1, history.currentOccurrences(c));
        }
        assertTrue(history.isFormalThreefold(c));
        for(int i = 0; i < 12; i++) history.popRealPosition();
        history.leaveSyntheticPosition();
        assertEquals(1, history.currentOccurrences(b));
    }

    @Test void acceptedPredictionReturnsBetaWithEmptyPvAndNoMathematicalStore() throws Exception {
        for(int depth : new int[] {4, 6}) {
            long[] b = Board.fromFen(QUEEN.formatted(0)); var table = new Writes();
            var search = ExactSearch.withCalibratedPruning((board, ply) -> Eval.evaluate(board), table);
            table.owner = search;
            assertEquals(1, scout(search, b, depth, 1, true));
            assertEquals(1, field("selectiveEpoch").getLong(search), "No predictions nested inside the probe");
            assertEquals(0, ((int[])field("pvLength").get(search))[7]);
            assertTrue(table.keys.isEmpty());
            assertFalse(field("nullProbeActive").getBoolean(search));
            assertTrue(new ExactSearch().search(b, depth).score() >= 1);
        }
    }

    @Test void invocationDepthMaterialClockAndWindowGuardsPrecedeAddedEvaluation() throws Exception {
        for(int depth : new int[] {3, 4, 6, 7}) for(boolean provenance : new boolean[] {false, true}) {
            var calls = new AtomicInteger();
            var search = ExactSearch.withCalibratedPruning((board, ply) -> {
                if(ply == 7) calls.incrementAndGet(); return Eval.evaluate(board);
            }, new Writes());
            scout(search, Board.fromFen(QUEEN.formatted(0)), depth, 1, provenance);
            assertEquals(provenance && depth >= 4 && depth <= 6 ? 1 : 0, calls.get());
        }
        for(String fen : List.of(QUEEN.formatted(96),
                "8/8/8/8/6q1/8/5k1P/7K w - - 99 1",
                "8/8/8/8/2k5/2p5/2K5/8 w - - 0 1",
                "7k/8/8/8/8/8/Q7/K6r w - - 0 1")) {
            var calls = new AtomicInteger();
            var search = ExactSearch.withCalibratedPruning((board, ply) -> {
                if(ply == 7) calls.incrementAndGet(); return Eval.evaluate(board);
            }, new Writes());
            scout(search, Board.fromFen(fen), 4, -2000, true);
            assertEquals(0, calls.get());
        }
        for(int beta : new int[] {-32511, 32000}) {
            var calls = new AtomicInteger();
            var search = ExactSearch.withCalibratedPruning((board, ply) -> {
                if(ply == 7) calls.incrementAndGet(); return Eval.evaluate(board);
            }, new Writes());
            scout(search, Board.fromFen(QUEEN.formatted(0)), 4, beta, true);
            assertEquals(0, calls.get());
        }
    }

    @Test void applicableLegalTtProofPrecedesProbeAndCancellationUnwindsSpeculation() throws Exception {
        long[] b = Board.fromFen(QUEEN.formatted(0)); var game = GameHistory.initial(b); var table = new Writes();
        table.save(SearchKey.key(b, SearchKey.rootHistory(b, game)), 4, TTable.TYPE_EXACT, 17, 0);
        var search = ExactSearch.withCalibratedPruning((board, ply) -> fail("TT precedes evaluation"), table);
        assertEquals(17, scout(search, b, 4, 1, true));
        var cancelled = new AtomicBoolean(); var clean = new Writes();
        search = ExactSearch.withCalibratedPruning(new ExactEvaluator() {
            @Override public int evaluate(long[] board, int ply) { return Eval.evaluate(board); }
            @Override public void child(long[] parent, long[] child, int ply) { cancelled.set(true); }
        }, clean);
        field("cancelled").set(search, (java.util.function.BooleanSupplier)cancelled::get);
        ExactSearch interrupted = search;
        assertThrows(InvocationTargetException.class, () -> scout(interrupted, b, 4, 1, true));
        assertFalse(field("nullProbeActive").getBoolean(search));
        assertEquals(0, field("selectiveEpoch").getLong(search)); assertTrue(clean.keys.isEmpty());
        assertEquals(1, ((SearchLineHistory)field("history").get(search)).currentOccurrences(b));
    }

    @Test void discardedProbeDoesNotTaintLegalProofOrPublishSyntheticWrites() throws Exception {
        // Outcome-blind legal-walk witness: E=266, beta=-445, reduced pass=-794.
        long[] b = {-3242591731706757037L, -9102599205724490679L, 5092746562049274918L,
                -2905646101317025792L, 1572893L, 3917038985170077503L};
        var table = new Writes(); var search = ExactSearch.withCalibratedPruning((board, ply) -> Eval.evaluate(board), table);
        // Isolate SR-018's discarded-probe boundary from independently committed SR-019 events.
        field("reverseFutility").setBoolean(search, false);
        int score = scout(search, b, 4, -445, true);
        var control = new ExactSearch((board, ply) -> Eval.evaluate(board), new Writes());
        assertEquals(scout(control, b, 4, -445, true), score);
        assertEquals(0, field("selectiveEpoch").getLong(search));
        assertTrue(table.keys.contains(SearchKey.key(b, SearchKey.rootHistory(b, GameHistory.initial(b)))));
        assertFalse(field("nullProbeActive").getBoolean(search));
    }

    @Test void committedPredictionSuppressesLegalAncestorsAndRetainsLegalPv() throws Exception {
        long[] b = Board.fromFen("7k/8/8/8/8/8/Q7/K7 b - - 0 1");
        var game = GameHistory.initial(b); var table = new Writes();
        var search = ExactSearch.withCalibratedPruning((board, ply) -> Eval.evaluate(board), table);
        field("reverseFutility").setBoolean(search, false);
        var result = search.searchWindow(b, game, 5, -1, 0, ExactSearch.NEVER_CANCELLED);
        assertTrue(result.completed()); assertTrue(result.selective());
        assertFalse(table.keys.contains(SearchKey.key(b, SearchKey.rootHistory(b, game))));
        for(long move : result.principalVariation()) b = play(b, Move.coordinate(move));
        assertFalse(field("nullProbeActive").getBoolean(search));
    }

    @Test void exactAndUncalibratedFactoriesRemainUnpruned() throws Exception {
        for(ExactSearch search : List.of(new ExactSearch(),
                new ExactSearch((board, ply) -> Eval.evaluate(board), new Writes()),
                ExactSearch.withStaticNullPruning((board, ply) -> Eval.evaluate(board), new Writes()))) {
            assertFalse(field("nullMovePruning").getBoolean(search));
        }
        for(var adapter : List.of(
                new com.ohinteractive.seedv6.search.driver.ExactSearchAdapter((board, ply) -> Eval.evaluate(board), new Writes()),
                new com.ohinteractive.seedv6.search.driver.ExactSearchAdapter(
                        com.ohinteractive.seedv6.search.evaluation.SearchEvaluation.handcraftedIsolation(), new Writes()))) {
            var f = adapter.getClass().getDeclaredField("exact"); f.setAccessible(true);
            assertFalse(field("nullMovePruning").getBoolean(f.get(adapter)));
        }
    }
}
