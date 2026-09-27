package com.ohinteractive.seedv6.search.exact;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
    private static final int[] ALL_MODES = {ExactSearch.CONTROL, ExactSearch.SEE_TIERED, ExactSearch.SEE_TACTICAL,
            ExactSearch.SEE_MATERIAL, ExactSearch.SEE_MATERIAL_LVA, ExactSearch.SEE_MATERIAL_CAPTURE_HISTORY,
            ExactSearch.SEE_MATERIAL_QUIET_HISTORY, ExactSearch.SEE_MATERIAL_CONTINUATION_HISTORY,
            ExactSearch.SEE_MATERIAL_QUIET_HISTORY_KILLERS};
    private static final int[] SEE_MODES = {ExactSearch.SEE_TIERED, ExactSearch.SEE_TACTICAL,
            ExactSearch.SEE_MATERIAL, ExactSearch.SEE_MATERIAL_LVA, ExactSearch.SEE_MATERIAL_CAPTURE_HISTORY,
            ExactSearch.SEE_MATERIAL_QUIET_HISTORY, ExactSearch.SEE_MATERIAL_CONTINUATION_HISTORY,
            ExactSearch.SEE_MATERIAL_QUIET_HISTORY_KILLERS};
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

    @Test void establishedModesRetainRecordedDepthFiveStartVisitation() {
        // Recorded 4d294d6/58f8b99/543cdec visits remain distinct from semantic equivalence checks.
        long[][] recordedNodes = {{48_266, 103_653, 48_287, 48_224}, {43_779, 95_495, 43_800, 43_737}};
        for(int tt = 0; tt < 2; tt++) for(int mode : new int[] {ExactSearch.CONTROL, ExactSearch.SEE_TIERED,
                ExactSearch.SEE_TACTICAL, ExactSearch.SEE_MATERIAL}) {
            var result = new ExactSearch(HCE, tt == 0 ? null : new TTable(4), mode).search(Board.startingPosition(), 5);
            assertTrue(result.completed());
            assertEquals(recordedNodes[tt][mode], result.nodes());
            assertEquals(197, result.score());
            assertEquals("e2e3", Move.coordinate(result.bestMove()));
        }
    }

    @Test void mainHistoryRetainsRecordedSixPositionDepthFiveVisitation() {
        // Commit 79670d4 evidence is a visitation contract independent of candidate equivalence.
        long[][] nodes = {{31_418, 83_934, 4_861, 20_741, 141_150, 1_563},
                {27_911, 65_515, 4_280, 19_834, 120_175, 1_532}};
        int[] scores = {197, 403, 1177, -81, 250, 6};
        String[] best = {"e2e3", "d5e6", "e4d5", "c4c5", "c3d5", "e1d2"};
        for(int tt = 0; tt < 2; tt++) for(int i = 0; i < 6; i++) {
            var result = new ExactSearch(HCE, tt == 0 ? null : new TTable(4), ExactSearch.SEE_MATERIAL_QUIET_HISTORY)
                    .search(Board.fromFen(ExactSearchHarness.orderingPositions().get(i).fen()), 5);
            assertTrue(result.completed()); assertEquals(nodes[tt][i], result.nodes());
            assertEquals(scores[i], result.score()); assertEquals(best[i], Move.coordinate(result.bestMove()));
        }
    }

    @Test void allPoliciesAndTtModesMatchExhaustiveValuesOptimalMovesAndConsistentPvs() {
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
                for(boolean tt : new boolean[] {false, true}) for(int mode : ALL_MODES) {
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
            for(int mode : ALL_MODES) {
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
        for(int mode : SEE_MODES) for(String fen : List.of(ExactSearchHarness.positions().get(1).fen(), EP, PROMOTION)) {
            long[] board = Board.fromFen(fen);
            for(long hash : ExhaustiveOracle.legalMoves(board)) {
                if(!tactical(board, hash)) quiet = true;
                else if(See.evaluate(board, hash) >= 0) good = true;
                else bad = true;
                for(boolean old : new boolean[] {false, true}) {
                    var visits = rootVisits(board, mode, hash, old);
                    assertEquals(expectedOrder(board, mode, hash), visits);
                    assertEquals(hash, visits.get(0));
                    assertEquals(visits.size(), new HashSet<>(visits).size());
                }
            }
            assertEquals(expectedOrder(board, mode, 0), rootVisits(board, mode, Long.MAX_VALUE, false));
        }
        assertTrue(good && bad && quiet, "Hash coverage must include all three classes");
    }

    @Test void resolvedLeavesDrawsAndTtCutoffsBypassTheOrderingPartition() throws Exception {
        for(int mode : SEE_MODES) assertResolvedNodesBypassPartition(mode);
    }

    @Test void immediateMaterialMatchesIndependentBoardAccountingAndSpecialMoveValues() {
        var fens = new ArrayList<String>();
        for(var p : ExactSearchHarness.orderingPositions()) fens.add(p.fen());
        fens.addAll(List.of(EP, PROMOTION, "4k3/8/8/3R4/3Pp3/8/8/4K3 b - d3 0 1",
                "4k3/8/8/8/8/8/6p1/4K2R b - - 0 1"));
        int comparisons = 0;
        for(String fen : fens) {
            long[] board = Board.fromFen(fen);
            for(long move : ExhaustiveOracle.legalMoves(board)) if(tactical(board, move)) {
                assertEquals(referenceGain(board, move), ExactSearch.tacticalMaterialValue(move,
                        Board.enPassantSquare((int) board[Board.STATUS])), fen + " " + Move.coordinate(move));
                comparisons++;
            }
        }
        int pawn = Eval.exchangeValue(Piece.PAWN);
        assertMaterial("4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1", "e4d5", Eval.exchangeValue(Piece.QUEEN));
        assertMaterial(EP, "e5d6", pawn);
        assertMaterial("4k3/8/8/3R4/3Pp3/8/8/4K3 b - d3 0 1", "e4d3", pawn);
        int[] types = {Piece.QUEEN, Piece.ROOK, Piece.BISHOP, Piece.KNIGHT};
        String[] suffixes = {"q", "r", "b", "n"};
        for(int i = 0; i < types.length; i++) {
            int gain = Eval.exchangeValue(types[i]) - pawn;
            assertMaterial(PROMOTION, "a7a8" + suffixes[i], gain);
            assertMaterial(PROMOTION, "a7b8" + suffixes[i], gain + Eval.exchangeValue(Piece.ROOK));
            String black = "4k3/8/8/8/8/8/6p1/4K2R b - - 0 1";
            assertMaterial(black, "g2g1" + suffixes[i], gain);
            assertMaterial(black, "g2h1" + suffixes[i], gain + Eval.exchangeValue(Piece.ROOK));
        }
        System.out.println("material board-accounting comparisons=" + comparisons + "; explicit special/capture values=19");
    }

    @Test void materialPoliciesHaveStableTiesAndLvaCannotOverridePrimaryEvidence() {
        boolean stableTie = false, lvaChangesOrder = false, primaryDominatesLva = false;
        boolean goodRanked = false, badRanked = false;
        var fens = new ArrayList<String>();
        for(var p : ExactSearchHarness.orderingPositions()) fens.add(p.fen());
        fens.addAll(List.of(EP, PROMOTION));
        for(String fen : fens) {
            long[] board = Board.fromFen(fen);
            var material = rootVisits(board, ExactSearch.SEE_MATERIAL, 0, false);
            var lva = rootVisits(board, ExactSearch.SEE_MATERIAL_LVA, 0, false);
            assertEquals(expectedOrder(board, ExactSearch.SEE_MATERIAL, 0), material);
            assertEquals(expectedOrder(board, ExactSearch.SEE_MATERIAL_LVA, 0), lva);
            long[] generated = ExhaustiveOracle.legalMoves(board);
            for(int i = 0; i < generated.length; i++) for(int j = i + 1; j < generated.length; j++) {
                long a = generated[i], b = generated[j];
                if(!tactical(board, a) || !tactical(board, b)) continue;
                boolean good = See.evaluate(board, a) >= 0;
                if(good != (See.evaluate(board, b) >= 0)) continue;
                goodRanked |= good; badRanked |= !good;
                int primary = Integer.compare(referenceGain(board, a), referenceGain(board, b));
                int attacker = Integer.compare(movingValue(a), movingValue(b));
                if(primary == 0) {
                    assertTrue(material.indexOf(a) < material.indexOf(b), "Material-only ties keep generator order");
                    if(attacker == 0) {
                        stableTie = true;
                        assertTrue(lva.indexOf(a) < lva.indexOf(b), "Equal primary/secondary ties remain stable");
                    } else {
                        assertEquals(attacker < 0, lva.indexOf(a) < lva.indexOf(b));
                        lvaChangesOrder |= attacker > 0;
                    }
                } else {
                    assertEquals(primary > 0, material.indexOf(a) < material.indexOf(b));
                    assertEquals(primary > 0, lva.indexOf(a) < lva.indexOf(b));
                    primaryDominatesLva |= primary == attacker;
                }
            }
        }
        assertTrue(stableTie && lvaChangesOrder && primaryDominatesLva && goodRanked && badRanked,
                "Fixtures must exercise ties, changed LVA ties, conflicting primary/LVA and both SEE classes");
    }

    @Test void materialScratchIsUntouchedForResolvedNodesQuietsAndTheOnlyTacticalHash() throws Exception {
        long[] board = Board.fromFen(LOSING);
        long hash = legalMove(board, "d1d5");
        assertEquals(1, Arrays.stream(ExhaustiveOracle.legalMoves(board)).filter(m -> tactical(board, m)).count());
        for(int mode : new int[] {ExactSearch.SEE_MATERIAL, ExactSearch.SEE_MATERIAL_LVA, ExactSearch.SEE_MATERIAL_CAPTURE_HISTORY}) {
            var table = new TTable(1);
            var search = new ExactSearch(HCE, table, mode);
            var field = ExactSearch.class.getDeclaredField("materialScores"); field.setAccessible(true);
            int[] scratch = (int[]) field.get(search);
            Arrays.fill(scratch, Integer.MIN_VALUE);
            int[] untouched = scratch.clone();
            search.search(board, 0);
            search.search(Board.fromFen(LOSING.replace("0 1", "100 1")), 3);
            for(var p : ExactSearchHarness.positions().subList(4, 6)) search.search(Board.fromFen(p.fen()), 3);
            assertArrayEquals(untouched, scratch);
            search.beginRequest();
            var key = ExactSearchTTableTest.key(board, GameHistory.initial(board));
            table.save(key, 2, TTable.TYPE_EXACT, 999, hash); // Ordering hint only at depth one.
            search.search(board, 1);
            assertArrayEquals(untouched, scratch, "Hash and remaining quiets must not be scored");
            table.clear();
            table.save(key, 1, TTable.TYPE_EXACT, 12, hash);
            assertEquals(12, search.search(board, 1).score());
            assertArrayEquals(untouched, scratch, "Applicable TT cutoff must skip scoring");
            search.endRequest();
        }
    }

    private static void assertResolvedNodesBypassPartition(int mode) throws Exception {
        // Scratch sentinels detect an unintended partition; the only SEE call site is inside that partition.
        long[] board = Board.fromFen(LOSING);
        var search = new ExactSearch(HCE, null, mode);
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
            var cutoff = new ExactSearch((b, p) -> { throw new AssertionError("TT must resolve this node"); }, table, mode);
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

    @Test void repetitionAndCancellationKeepTheirExistingSemanticsInAllModes() {
        var builder = GameHistory.builder(Board.startingPosition());
        long[] board = Board.startingPosition();
        for(int cycle = 0; cycle < 2; cycle++) for(String coordinate : List.of("g1f3", "g8f6", "f3g1", "f6g8")) {
            board = ExactSearchTTableTest.play(board, coordinate);
            builder.appendPosition(board);
        }
        for(boolean tt : new boolean[] {false, true}) for(int mode : ALL_MODES) {
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
        if(mode >= ExactSearch.SEE_TACTICAL) {
            boolean quietSeen = false;
            for(long move : visits) {
                if(move == hash) continue; // A quiet hash alone may precede the tactical block.
                if(tactical(board, move)) assertFalse(quietSeen, "Every non-hash tactical must precede every quiet");
                else quietSeen = true;
            }
        }
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
        if(mode >= ExactSearch.SEE_MATERIAL) {
            // Test-only stable library sorting and full-board accounting are independent of the primitive ranker.
            Comparator<Long> comparator = Comparator.comparingInt((Long m) -> referenceGain(board, m)).reversed();
            if(mode == ExactSearch.SEE_MATERIAL_LVA) comparator = comparator.thenComparingInt(ExactSearchOrderingTest::movingValue);
            first.sort(comparator); bad.sort(comparator);
        }
        if(legalHash) first.add(0, hash);
        if(mode >= ExactSearch.SEE_TACTICAL) { first.addAll(bad); first.addAll(quiet); }
        else { first.addAll(quiet); first.addAll(bad); }
        return first;
    }

    private static boolean tactical(long[] board, long move) {
        return ((move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                || ((move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                || (((move >>> Board.START_PIECE_SHIFT) & Piece.TYPE) == Piece.PAWN
                && Move.toSquare(move) == Board.enPassantSquare((int) board[Board.STATUS]));
    }

    private static long legalMove(long[] board, String coordinate) {
        for(long move : ExhaustiveOracle.legalMoves(board)) if(Move.coordinate(move).equals(coordinate)) return move;
        throw new AssertionError("Missing legal fixture move: " + coordinate);
    }

    private static void assertMaterial(String fen, String coordinate, int expected) {
        long[] board = Board.fromFen(fen);
        long move = legalMove(board, coordinate);
        assertEquals(expected, ExactSearch.tacticalMaterialValue(move, Board.enPassantSquare((int) board[Board.STATUS])));
    }

    private static int movingValue(long move) {
        return Eval.exchangeValue((int) (move >>> Board.START_PIECE_SHIFT) & Piece.TYPE);
    }

    private static int referenceGain(long[] board, long move) {
        int player = Board.player((int) board[Board.STATUS]);
        return materialBalance(ExhaustiveOracle.child(board, move), player) - materialBalance(board, player);
    }

    private static int materialBalance(long[] board, int player) {
        int total = 0;
        for(int square = 0; square < 64; square++) {
            int piece = Board.getSquare(board[0], board[1], board[2], board[3], square);
            if(piece != 0) total += ((piece >>> Board.PLAYER_SHIFT) == player ? 1 : -1) * Eval.exchangeValue(piece & Piece.TYPE);
        }
        return total;
    }
}
