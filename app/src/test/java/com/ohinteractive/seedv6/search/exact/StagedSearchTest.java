package com.ohinteractive.seedv6.search.exact;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.tools.search.ExactSearchHarness;

class StagedSearchTest {
    static final int POLICY = ExactSearch.SEE_MATERIAL_QUIET_HISTORY;
    static final ExactEvaluator HCE = (b, p) -> Eval.evaluate(b);

    @Test void productionStaticLeafUsesExactExistenceWithoutConsumingStaleMoves() {
        var calls = new AtomicInteger();
        var search = new ExactSearch((b, p) -> { calls.incrementAndGet(); return 37; }, new TTable(1));
        search.search(Board.startingPosition(), 2);
        for(String ep : new String[] {"b6", "-"}) {
            calls.set(0);
            long[] board = Board.fromFen("8/8/b7/Pp6/8/8/1rk5/K7 w - " + ep + " 0 1");
            var result = search.search(board, 0);
            assertTrue(result.completed());
            assertEquals(ep.equals("b6") ? 37 : 0, result.score());
            assertEquals(ep.equals("b6") ? 1 : 0, calls.get());
            assertEquals(1, result.nodes());
            assertFalse(result.hasMove());
            assertEquals(0, result.principalVariation().length);
        }
    }

    @Test void fullAndLeafStagedHaveIdenticalRecordedDepthFiveTreesWhileStagedValuesRemainExact() throws Exception {
        long[][] recorded = {{31418, 83934, 4861, 20741, 141150, 1563}, {27911, 65515, 4280, 19834, 120175, 1532}};
        for(int tt = 0; tt < 2; tt++) for(int p = 0; p < 6; p++) {
            long[] board = Board.fromFen(ExactSearchHarness.orderingPositions().get(p).fen());
            var game = GameHistory.initial(board);
            ExactSearchResult base = null; long trace = 0; int[] learned = null;
            for(int mode = ExactSearch.FULL_LAZY; mode <= ExactSearch.STAGED_LAZY; mode++) {
                final long[] visits = {17};
                var evaluator = new ExactEvaluator() {
                    public int evaluate(long[] b, int ply) { return Eval.evaluate(b); }
                    public void child(long[] parent, long[] child, int ply) { visits[0] = visits[0] * 31 + child[Board.KEY] + ply; }
                };
                var search = new ExactSearch(evaluator, tt == 0 ? null : new TTable(4), POLICY, mode);
                var result = search.search(board, 5);
                if(base == null) { base = result; trace = visits[0]; learned = history(search).clone(); assertEquals(recorded[tt][p], result.nodes()); }
                else if(mode == ExactSearch.LEAF_STAGED_LAZY) { same(base, result); assertEquals(trace, visits[0]); assertArrayEquals(learned, history(search)); }
                else {
                    assertEquals(base.score(), result.score());
                    ExactSearchTTableTest.assertOptimalAndPv(board, game, 5, result, HCE);
                }
            }
        }
    }

    @Test void exhaustiveOracleCoversSpecialMovesEvasionsAndTerminalsInEveryCandidate() {
        for(String fen : StagedGenerationTest.fens()) {
            long[] board = Board.fromFen(fen), before = board.clone(); var game = GameHistory.initial(board);
            for(int depth = 0; depth <= 2; depth++) {
                int oracle = new ExhaustiveOracle(HCE).score(board, new SearchLineHistory(game), depth, 0);
                for(boolean tt : new boolean[] {false, true}) {
                    ExactSearchResult base = null;
                    for(int mode = ExactSearch.FULL_LAZY; mode <= ExactSearch.STAGED_LAZY; mode++) {
                        var result = new ExactSearch(HCE, tt ? new TTable(1) : null, POLICY, mode).search(board, depth);
                        assertTrue(result.completed()); assertEquals(oracle, result.score());
                        if(base == null) base = result;
                        else if(mode == ExactSearch.LEAF_STAGED_LAZY) same(base, result);
                        ExactSearchTTableTest.assertOptimalAndPv(board, game, depth, result, HCE);
                        assertArrayEquals(before, board);
                    }
                }
            }
        }
    }

    @Test void fullRootEnumerationPreservesEveryMoveTieAndEveryLegalHash() throws Exception {
        for(String fen : StagedGenerationTest.fens()) {
            long[] board = Board.fromFen(fen), legal = ExhaustiveOracle.legalMoves(board);
            long[] hashes = Arrays.copyOf(legal, legal.length + 3);
            hashes[legal.length + 1] = Long.MAX_VALUE; // Invalid tactical-shaped hint.
            hashes[legal.length + 2] = Long.MIN_VALUE; // Invalid quiet-shaped hint forces exact validation.
            for(long hash : hashes) {
                List<Long> baseline = rootVisits(board, ExactSearch.FULL_LAZY, hash, false);
                assertEquals(legal.length, baseline.size());
                for(int mode : new int[] {ExactSearch.LEAF_STAGED_LAZY, ExactSearch.STAGED_LAZY, -1}) {
                    var visits = rootVisits(board, mode, hash, false);
                    assertEquals(visits.size(), new HashSet<>(visits).size());
                    if(mode == -1 && visits.size() < baseline.size()) {
                        // Production SR-011 can stop at a searched mate-in-one witness.
                        // Explicit mechanics controls still enumerate the complete root.
                        assertEquals(baseline.subList(0, visits.size()), visits);
                        long[] child = ExhaustiveOracle.child(board, visits.getLast());
                        assertEquals(-ExactSearch.MATE_SCORE, new ExactSearch(HCE).search(child, 0).score());
                        if(Arrays.stream(legal).anyMatch(m -> m == hash)) assertEquals(hash, visits.getFirst());
                        continue;
                    }
                    assertEquals(baseline, visits);
                    long[] sorted = visits.stream().mapToLong(Long::longValue).toArray(), expected = legal.clone();
                    Arrays.sort(sorted); Arrays.sort(expected); assertArrayEquals(expected, sorted);
                    if(Arrays.stream(legal).anyMatch(m -> m == hash)) assertEquals(hash, visits.getFirst());
                }
            }
        }
    }

    @Test void onlyDeferredQuietPhaseSamplesSiblingTimeHistory() throws Exception {
        long[] board = Board.fromFen(ExactSearchHarness.orderingPositions().get(1).fen());
        long[] quiet = StagedGenerationTest.subset(board, 2);
        assertTrue(quiet.length > 2); assertEquals(0, StagedGenerationTest.checkers(board));
        for(long hash : new long[] {0, quiet[0]}) {
            var full = rootVisits(board, ExactSearch.FULL_LAZY, hash, true);
            assertEquals(full, rootVisits(board, ExactSearch.LEAF_STAGED_LAZY, hash, true));
            var staged = rootVisits(board, ExactSearch.STAGED_LAZY, hash, true);
            assertEquals(staged, rootVisits(board, -1, hash, true), "Production samples the same deferred phase");
            if(hash == 0) {
                assertNotEquals(full, staged);
                assertEquals(quiet[quiet.length - 1], staged.stream().filter(m -> !CaptureHistory.isTactical(m,
                        Board.enPassantSquare((int) board[Board.STATUS]))).findFirst().orElseThrow());
                assertEquals(full.stream().filter(m -> CaptureHistory.isTactical(m, Board.enPassantSquare((int) board[Board.STATUS]))).toList(),
                        staged.stream().filter(m -> CaptureHistory.isTactical(m, Board.enPassantSquare((int) board[Board.STATUS]))).toList());
            } else assertEquals(full, staged, "Quiet hash forces early generation and node-entry evidence");
        }
    }

    @Test void terminalDrawCancellationAndTtPrecedenceSurviveStaging() throws Exception {
        for(int mode = ExactSearch.FULL_LAZY; mode <= ExactSearch.STAGED_LAZY; mode++) for(boolean tt : new boolean[] {false, true}) {
            for(String fen : List.of("7k/6Q1/5K2/8/8/8/8/8 b - - 100 1", "7k/5K2/6Q1/8/8/8/8/8 b - - 0 1",
                    "4k3/8/8/8/8/8/8/4K3 w - - 0 1", "4k3/8/8/8/8/8/8/R3K3 w - - 100 1")) {
                var result = new ExactSearch((b, p) -> { throw new AssertionError("Terminal/draw must precede evaluation"); },
                        tt ? new TTable(1) : null, POLICY, mode).search(Board.fromFen(fen), 0);
                assertEquals(fen.startsWith("7k/6Q1") ? -ExactSearch.MATE_SCORE : 0, result.score());
            }
            long[] board = Board.startingPosition(); var builder = GameHistory.builder(board);
            for(int cycle = 0; cycle < 2; cycle++) for(String move : List.of("g1f3", "g8f6", "f3g1", "f6g8")) {
                board = ExactSearchTTableTest.play(board, move); builder.appendPosition(board);
            }
            var draw = new ExactSearch(HCE, tt ? new TTable(1) : null, POLICY, mode).search(board, builder.snapshot(), 3, ExactSearch.NEVER_CANCELLED);
            assertEquals(0, draw.score()); assertEquals(1, draw.nodes()); assertFalse(draw.hasMove());
            board = Board.fromFen(ExactSearchHarness.orderingPositions().get(1).fen()); var game = GameHistory.initial(board);
            for(int limit : new int[] {0, 40, 400}) {
                var table = tt ? new TTable(1) : null; var calls = new AtomicInteger();
                var search = new ExactSearch(HCE, table, POLICY, mode);
                var result = search.search(board, game, 4, () -> calls.incrementAndGet() > limit);
                assertFalse(result.completed()); assertFalse(result.hasMove()); assertEquals(Value.INVALID, result.score());
                assertEquals(0, result.principalVariation().length); assertTrue(Arrays.stream(history(search)).allMatch(x -> x == 0));
                if(table != null) { assertFalse(table.probe(ExactSearchTTableTest.key(board, game), new TTable.TEntry())); table.clear(); }
                assertEquals(new ExactSearch(HCE, null, POLICY, ExactSearch.FULL_LAZY).search(board, 3).score(), search.search(board, 3).score());
            }
            var calls = new AtomicInteger(); int legalCount = ExhaustiveOracle.legalMoves(board).length;
            var search = new ExactSearch((b, p) -> { calls.incrementAndGet(); return 0; }, tt ? new TTable(1) : null, POLICY, mode);
            assertFalse(search.search(board, game, 1, () -> calls.get() == legalCount).completed());
            Thread.currentThread().interrupt();
            try { assertFalse(search.search(board, 1).completed()); assertTrue(Thread.currentThread().isInterrupted()); }
            finally { Thread.interrupted(); }
            var table = new TTable(1); search = new ExactSearch((b, p) -> 7, table, POLICY, mode);
            search.beginRequest();
            table.save(ExactSearchTTableTest.key(board, game), 0, TTable.TYPE_EXACT, 999, ExhaustiveOracle.legalMoves(board)[0]);
            assertEquals(7, search.search(board, 0).score()); search.endRequest();
            for(int type : new int[] {TTable.TYPE_EXACT, TTable.TYPE_LOWER, TTable.TYPE_UPPER}) {
                table = new TTable(1); search = new ExactSearch(HCE, table, POLICY, mode); search.beginRequest();
                long hash = StagedGenerationTest.subset(board, 2)[0];
                int score = type == TTable.TYPE_EXACT ? 0 : type == TTable.TYPE_LOWER ? 50 : -50;
                table.save(ExactSearchTTableTest.key(board, game), 2, type, score, hash);
                var result = search.searchWindow(board, game, 2, -50, 50, ExactSearch.NEVER_CANCELLED);
                assertEquals(1, result.nodes()); assertEquals(score, result.score()); assertEquals(hash, result.bestMove()); search.endRequest();
            }
        }
    }

    @Test void productionTacticalCutoffAndStaticLeafLeaveQuietsUngenerated() throws Exception {
        long[] board = Board.fromFen(ExactSearchHarness.orderingPositions().get(1).fen());
        int tacticalCount = StagedGenerationTest.subset(board, 1).length;
        int legalCount = ExhaustiveOracle.legalMoves(board).length;
        assertEquals(0, StagedGenerationTest.checkers(board));
        assertTrue(tacticalCount > 0 && tacticalCount < legalCount);
        for(int depth : new int[] {0, 1}) {
            var search = new ExactSearch((b, p) -> -80, new TTable(1));
            long[] rootMoves = rootBuffer(search);
            Arrays.fill(rootMoves, Long.MIN_VALUE);
            var result = search.searchWindow(board, GameHistory.initial(board), depth, -100, 50, ExactSearch.NEVER_CANCELLED);
            assertTrue(result.completed());
            assertEquals(depth == 0 ? -80 : 80, result.score());
            assertEquals(depth + 1, result.nodes());
            int materialized = depth == 0 ? 0 : tacticalCount;
            assertTrue(Arrays.stream(rootMoves, 0, materialized).allMatch(m -> m != Long.MIN_VALUE));
            assertTrue(Arrays.stream(rootMoves, materialized, rootMoves.length).allMatch(m -> m == Long.MIN_VALUE),
                    "Static leaves need no list; the positive-depth tactical cutoff needs no quiet phase");
        }
        var search = new ExactSearch((b, p) -> 0, new TTable(1));
        long[] rootMoves = rootBuffer(search);
        Arrays.fill(rootMoves, Long.MIN_VALUE);
        assertTrue(search.search(board, 1).completed());
        assertEquals(legalCount, Arrays.stream(rootMoves).filter(m -> m != Long.MIN_VALUE).count(),
                "Without a cutoff, quiets must eventually materialize");
    }

    @Test void productionDeferredQuietSnapshotStaysFixedAcrossQuietSiblings() throws Exception {
        long[] board = Board.fromFen(ExactSearchHarness.orderingPositions().get(1).fen());
        long[] quiet = StagedGenerationTest.subset(board, 2);
        var byKey = new HashMap<Long, Long>();
        for(long move : ExhaustiveOracle.legalMoves(board)) byKey.put(ExhaustiveOracle.child(board, move)[Board.KEY], move);
        var visitedQuiets = new ArrayList<Long>();
        int[][] hist = new int[1][];
        var search = new ExactSearch(new ExactEvaluator() {
            public int evaluate(long[] b, int p) { return 0; }
            public void child(long[] parent, long[] child, int ply) {
                long move = byKey.get(child[Board.KEY]);
                if(CaptureHistory.isTactical(move, Board.enPassantSquare((int) board[Board.STATUS]))) {
                    hist[0][QuietHistory.index(quiet[quiet.length - 1])] = 1000;
                } else {
                    visitedQuiets.add(move);
                    // This newer evidence must not reorder the remaining quiet siblings.
                    hist[0][QuietHistory.index(quiet[quiet.length - 2])] = 2000;
                }
            }
        }, new TTable(1));
        hist[0] = history(search);
        assertTrue(search.search(board, 1).completed());
        var expected = new ArrayList<Long>();
        expected.add(quiet[quiet.length - 1]);
        for(int i = 0; i < quiet.length - 1; i++) expected.add(quiet[i]);
        assertEquals(expected, visitedQuiets);
    }

    @Test void productionLeavesAvoidNonCheckGenerationAndRetainCompleteEvasions() throws Exception {
        for(String fen : List.of(ExactSearchHarness.positions().getFirst().fen(),
                "4r1k1/8/8/8/8/8/8/2B1K3 w - - 0 1")) {
            long[] board = Board.fromFen(fen), legal = ExhaustiveOracle.legalMoves(board);
            assertTrue(legal.length > 0);
            var search = new ExactSearch((b, p) -> 19, new TTable(1));
            long[] rootMoves = rootBuffer(search);
            Arrays.fill(rootMoves, Long.MIN_VALUE);
            var result = search.search(board, 0);
            assertEquals(19, result.score()); assertEquals(1, result.nodes());
            if(StagedGenerationTest.checkers(board) != 0) {
                assertArrayEquals(legal, Arrays.copyOf(rootMoves, legal.length));
                assertEquals(Long.MIN_VALUE, rootMoves[legal.length]);
            } else assertTrue(Arrays.stream(rootMoves).allMatch(m -> m == Long.MIN_VALUE));
            assertFalse(result.hasMove());
        }
    }

    private static long[] rootBuffer(ExactSearch search) throws Exception {
        var field = ExactSearch.class.getDeclaredField("moves"); field.setAccessible(true);
        return ((long[][]) field.get(search))[0];
    }

    private static List<Long> rootVisits(long[] board, int mode, long hash, boolean mutate) throws Exception {
        long[] generated = ExhaustiveOracle.legalMoves(board), quiet = StagedGenerationTest.subset(board, 2);
        var byKey = new HashMap<Long, Long>();
        for(long m : generated) assertNull(byKey.put(ExhaustiveOracle.child(board, m)[Board.KEY], m));
        var visits = new ArrayList<Long>(); int[][] hist = new int[1][];
        var table = new TTable(1);
        var evaluator = new ExactEvaluator() {
            public int evaluate(long[] b, int p) { return 0; }
            public void initialize(long[] b) {
                if(!mutate) for(int i = 0; i < quiet.length; i++) hist[0][QuietHistory.index(quiet[i])] = (i % 3 - 1) * 17;
            }
            public void child(long[] parent, long[] child, int ply) {
                visits.add(byKey.get(child[Board.KEY]));
                if(mutate) hist[0][QuietHistory.index(quiet[quiet.length - 1])] = 1000;
            }
        };
        var search = mode == -1 ? new ExactSearch(evaluator, table) : new ExactSearch(evaluator, table, POLICY, mode);
        hist[0] = history(search); search.beginRequest();
        table.save(ExactSearchTTableTest.key(board, GameHistory.initial(board)), 2, TTable.TYPE_EXACT, 999, hash);
        assertTrue(search.search(board, 1).completed()); search.endRequest(); return visits;
    }

    static int[] history(ExactSearch search) throws Exception {
        var field = ExactSearch.class.getDeclaredField("quietHistory"); field.setAccessible(true); return (int[]) field.get(search);
    }
    static void same(ExactSearchResult a, ExactSearchResult b) {
        assertEquals(a.completed(), b.completed()); assertEquals(a.score(), b.score()); assertEquals(a.nodes(), b.nodes());
        assertEquals(a.bestMove(), b.bestMove()); assertArrayEquals(a.principalVariation(), b.principalVariation());
    }
}
