package com.ohinteractive.seedv6.search.exact;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.See;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.tools.search.ExactSearchHarness;

class ExactSearchOrderingTest {
    private static final ExactEvaluator HCE = (b, p) -> Eval.evaluate(b);
    private static final String EP = "4k3/8/8/3pP3/3r4/8/8/4K3 w - d6 0 1";
    private static final String PROMOTION = "1r5k/P7/8/8/8/8/8/7K w - - 0 1";
    private static final String LOSING = "3rk3/8/8/3p4/8/8/8/3QK3 w - - 0 1";

    @Test void explicitControlPreservesDefaultScoresMovesPvsAndExactNodeCounts() {
        for(var position : ExactSearchHarness.positions()) {
            long[] board = Board.fromFen(position.fen());
            for(boolean tt : new boolean[] {false, true}) {
                var defaultResult = new ExactSearch(HCE, tt ? new TTable(1) : null).search(board, 4);
                var explicit = new ExactSearch(HCE, tt ? new TTable(1) : null, ExactSearch.CONTROL).search(board, 4);
                assertEquals(defaultResult.score(), explicit.score());
                assertEquals(defaultResult.bestMove(), explicit.bestMove());
                assertArrayEquals(defaultResult.principalVariation(), explicit.principalVariation());
                assertEquals(defaultResult.nodes(), explicit.nodes(), "CONTROL visitation is a regression contract");
            }
        }
        assertThrows(IllegalArgumentException.class, () -> new ExactSearch(HCE, null, -1));
    }

    @Test void bothPoliciesAndTtModesMatchExhaustiveValuesOptimalMovesAndConsistentPvs() {
        var positions = new ArrayList<>(ExactSearchHarness.orderingPositions());
        positions.addAll(ExactSearchHarness.positions().subList(3, 6));
        for(String fen : List.of(EP, PROMOTION, LOSING,
                "3rk3/8/8/3p4/8/8/8/3QK3 w - - 100 1", "4k3/8/8/8/8/8/8/4K3 w - - 0 1"))
            positions.add(new ExactSearchHarness.Position("edge", fen));
        int comparisons = 0;
        for(var position : positions) {
            long[] board = Board.fromFen(position.fen());
            long[] before = board.clone();
            var game = GameHistory.initial(board);
            for(int depth = 0; depth <= 2; depth++) {
                int expected = new ExhaustiveOracle(HCE).score(board, new SearchLineHistory(game), depth, 0);
                for(boolean tt : new boolean[] {false, true}) for(int mode : new int[] {ExactSearch.CONTROL, ExactSearch.SEE_TIERED}) {
                    var result = new ExactSearch(HCE, tt ? new TTable(1) : null, mode).search(board, depth);
                    assertEquals(expected, result.score(), position.name() + " depth=" + depth + " mode=" + mode);
                    ExactSearchTTableTest.assertOptimalAndPv(board, game, depth, result, HCE);
                    assertArrayEquals(before, board);
                    comparisons++;
                }
            }
        }
        System.out.println("ordering exhaustive score/optimal-move/PV comparisons=" + comparisons);
    }

    @Test void stablePartitionPreservesEveryMoveOnceIncludingSpecialMovesAndEvasions() {
        var fens = new ArrayList<String>();
        for(var p : ExactSearchHarness.orderingPositions()) fens.add(p.fen());
        fens.addAll(List.of(EP, PROMOTION,
                "7k/6pp/Q1Q1Q3/1Q1Q1Q2/Q1Q1Q3/R1B1N1R1/2B1N3/K7 w - - 0 1"));
        for(String fen : fens) {
            long[] board = Board.fromFen(fen);
            for(int mode : new int[] {ExactSearch.CONTROL, ExactSearch.SEE_TIERED}) {
                List<Long> visits = rootVisits(board, mode, 0, false);
                assertEquals(expectedOrder(board, mode, 0), visits);
                assertEquals(ExhaustiveOracle.legalMoves(board).length, visits.size());
                assertEquals(visits.size(), new HashSet<>(visits).size());
                assertEquals(visits, rootVisits(board, mode, 0, false));
            }
        }
    }

    @Test void legalGoodBadAndQuietHashHintsLeadOnceWithAllOtherRelativeOrdersPreserved() {
        boolean good = false, bad = false, quiet = false;
        for(String fen : List.of(ExactSearchHarness.positions().get(1).fen(), EP, PROMOTION)) {
            long[] board = Board.fromFen(fen);
            for(long hash : ExhaustiveOracle.legalMoves(board)) {
                if(!tactical(board, hash)) quiet = true;
                else if(See.evaluate(board, hash) >= 0) good = true;
                else bad = true;
                for(boolean old : new boolean[] {false, true}) {
                    var visits = rootVisits(board, ExactSearch.SEE_TIERED, hash, old);
                    assertEquals(expectedOrder(board, ExactSearch.SEE_TIERED, hash), visits);
                    assertEquals(hash, visits.get(0));
                    assertEquals(visits.size(), new HashSet<>(visits).size());
                }
            }
            assertEquals(expectedOrder(board, ExactSearch.SEE_TIERED, 0),
                    rootVisits(board, ExactSearch.SEE_TIERED, Long.MAX_VALUE, false));
        }
        assertTrue(good && bad && quiet, "Hash coverage must include all three classes");
    }

    @Test void resolvedLeavesDrawsAndTtCutoffsBypassTheOrderingPartition() throws Exception {
        // Scratch sentinels detect an unintended partition; the only SEE call site is inside that partition.
        long[] board = Board.fromFen(LOSING);
        var search = new ExactSearch(HCE, null, ExactSearch.SEE_TIERED);
        long[] scratch = badScratch(search);
        Arrays.fill(scratch, -1L);
        long[] untouched = scratch.clone();
        search.search(board, 0);
        assertArrayEquals(untouched, scratch);
        search.search(Board.fromFen(LOSING.replace("0 1", "100 1")), 3);
        assertArrayEquals(untouched, scratch);
        for(var p : ExactSearchHarness.positions().subList(4, 6)) search.search(Board.fromFen(p.fen()), 3);
        assertArrayEquals(untouched, scratch);
        search.search(board, 1);
        assertFalse(Arrays.equals(untouched, scratch), "Fixture must exercise bad-tactical storage when unresolved");

        for(int type : new int[] {TTable.TYPE_EXACT, TTable.TYPE_LOWER, TTable.TYPE_UPPER}) {
            var table = new TTable(1);
            var cutoff = new ExactSearch((b, p) -> { throw new AssertionError("TT must resolve this node"); }, table, ExactSearch.SEE_TIERED);
            long[] cutoffScratch = badScratch(cutoff);
            Arrays.fill(cutoffScratch, -1L);
            cutoff.beginRequest();
            var game = GameHistory.initial(board);
            int value = type == TTable.TYPE_EXACT ? 0 : type == TTable.TYPE_LOWER ? 50 : -50;
            table.save(ExactSearchTTableTest.key(board, game), 2, type, value, ExhaustiveOracle.legalMoves(board)[0]);
            var result = cutoff.searchWindow(board, game, 2, -50, 50, ExactSearch.NEVER_CANCELLED);
            assertEquals(value, result.score());
            assertEquals(1, result.nodes());
            assertArrayEquals(untouched, cutoffScratch);
            cutoff.endRequest();
        }
    }

    @Test void repetitionAndCancellationKeepTheirExistingSemanticsInBothModes() {
        var builder = GameHistory.builder(Board.startingPosition());
        long[] board = Board.startingPosition();
        for(int cycle = 0; cycle < 2; cycle++) for(String coordinate : List.of("g1f3", "g8f6", "f3g1", "f6g8")) {
            board = ExactSearchTTableTest.play(board, coordinate);
            builder.appendPosition(board);
        }
        for(boolean tt : new boolean[] {false, true}) for(int mode : new int[] {ExactSearch.CONTROL, ExactSearch.SEE_TIERED}) {
            var table = tt ? new TTable(1) : null;
            var search = new ExactSearch(HCE, table, mode);
            var draw = search.search(board, builder.snapshot(), 3, ExactSearch.NEVER_CANCELLED);
            assertEquals(0, draw.score()); assertFalse(draw.hasMove()); assertEquals(1, draw.nodes());
            long[] tacticalBoard = Board.fromFen(ExactSearchHarness.positions().get(1).fen());
            var game = GameHistory.initial(tacticalBoard);
            var checks = new AtomicInteger();
            var aborted = search.search(tacticalBoard, game, 3, () -> checks.incrementAndGet() > 40);
            assertFalse(aborted.completed()); assertFalse(aborted.hasMove()); assertEquals(0, aborted.principalVariation().length);
            if(table != null) assertFalse(table.probe(ExactSearchTTableTest.key(tacticalBoard, game), new TTable.TEntry()));
            assertEquals(new ExactSearch().search(tacticalBoard, 2).score(), search.search(tacticalBoard, 2).score());

            var stop = new AtomicBoolean();
            int legalCount = ExhaustiveOracle.legalMoves(tacticalBoard).length;
            var evaluations = new AtomicInteger();
            var lastLeaf = new ExactSearch((b, p) -> { if(evaluations.incrementAndGet() == legalCount) stop.set(true); return 0; },
                    tt ? new TTable(1) : null, mode);
            assertFalse(lastLeaf.search(tacticalBoard, game, 1, stop::get).completed());
        }
    }

    private static long[] badScratch(ExactSearch search) throws Exception {
        var field = ExactSearch.class.getDeclaredField("badTacticalScratch");
        field.setAccessible(true);
        return (long[]) field.get(search);
    }

    private static List<Long> rootVisits(long[] board, int mode, long hash, boolean old) {
        var byKey = new HashMap<Long, Long>();
        for(long move : ExhaustiveOracle.legalMoves(board)) assertNull(byKey.put(ExhaustiveOracle.child(board, move)[Board.KEY], move));
        var visits = new ArrayList<Long>();
        var table = hash == 0 ? null : new TTable(1);
        var search = new ExactSearch(new ExactEvaluator() {
            public int evaluate(long[] b, int p) { return 0; }
            public void child(long[] parent, long[] child, int ply) { if(ply == 0) visits.add(byKey.get(child[Board.KEY])); }
        }, table, mode);
        search.beginRequest();
        if(table != null) {
            // Depth-mismatched exact evidence or an older-generation entry supplies ordering only.
            table.save(ExactSearchTTableTest.key(board, GameHistory.initial(board)), old ? 1 : 2, TTable.TYPE_EXACT, 999, hash);
            if(old) { search.endRequest(); search.beginRequest(); }
        }
        assertTrue(search.search(board, 1).completed());
        search.endRequest();
        return visits;
    }

    private static List<Long> expectedOrder(long[] board, int mode, long hash) {
        var first = new ArrayList<Long>(); var quiet = new ArrayList<Long>(); var bad = new ArrayList<Long>();
        boolean legalHash = false;
        for(long move : ExhaustiveOracle.legalMoves(board)) {
            if(move == hash) { legalHash = true; continue; }
            if(!tactical(board, move)) quiet.add(move);
            else if(mode == ExactSearch.CONTROL || See.evaluate(board, move) >= 0) first.add(move);
            else bad.add(move);
        }
        if(legalHash) first.add(0, hash);
        first.addAll(quiet); first.addAll(bad);
        return first;
    }

    private static boolean tactical(long[] board, long move) {
        return ((move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                || ((move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                || (((move >>> Board.START_PIECE_SHIFT) & Piece.TYPE) == Piece.PAWN
                && Move.toSquare(move) == Board.enPassantSquare((int) board[Board.STATUS]));
    }
}
