package com.ohinteractive.seedv6.search.exact;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.See;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.tools.search.ExactSearchHarness;

class CaptureHistoryTest {
    private static final int MODE = ExactSearch.SEE_MATERIAL_CAPTURE_HISTORY;
    private static final ExactEvaluator HCE = (b, p) -> Eval.evaluate(b);
    private static final String EP = "4k3/8/8/3pP3/3r4/8/8/4K3 w - d6 0 1";
    private static final String PROMOTION = "1r5k/P7/8/8/8/8/8/7K w - - 0 1";

    @Test void indexSeparatesSidePieceDestinationAndVictimButNotSourceOrPromotionChoice() {
        var indices = new HashSet<Integer>();
        for(int side = 0; side < 2; side++) for(int piece = 1; piece <= Piece.PAWN; piece++)
            for(int to = 0; to < 64; to++) for(int victim = 0; victim <= Piece.PAWN; victim++) {
                long move = ((long) (piece | side << Board.PLAYER_SHIFT) << Board.START_PIECE_SHIFT)
                        | ((long) to << Board.TARGET_SQUARE_SHIFT)
                        | ((long) victim << Board.TARGET_PIECE_SHIFT);
                int index = CaptureHistory.index(move, -1);
                assertTrue(index >= 0 && index < CaptureHistory.SIZE);
                assertTrue(indices.add(index), "Each side/piece/to/victim tuple must be distinct");
                assertEquals(index, CaptureHistory.index(move | 31, -1), "Source is deliberately omitted");
            }
        assertEquals(5376, indices.size()); // All packed dimensions, including unused legal-capture king victim.
        for(String fen : List.of(EP, "4k3/8/8/3R4/3Pp3/8/8/4K3 b - d3 0 1")) {
            long[] board = Board.fromFen(fen);
            int ep = ep(board);
            long move = legal(board, fen.equals(EP) ? "e5d6" : "e4d3");
            assertEquals(0, (move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS);
            assertEquals(Piece.PAWN, CaptureHistory.index(move, ep) & 7);
            assertEquals(CaptureHistory.index(move | ((long) Piece.PAWN << Board.TARGET_PIECE_SHIFT), -1),
                    CaptureHistory.index(move, ep));
        }
        for(String fen : List.of(PROMOTION, "4k3/8/8/8/8/8/6p1/4K2R b - - 0 1")) {
            long[] board = Board.fromFen(fen);
            String quiet = fen.equals(PROMOTION) ? "a7a8" : "g2g1";
            String capture = fen.equals(PROMOTION) ? "a7b8" : "g2h1";
            for(String suffix : List.of("q", "r", "b", "n")) {
                int none = CaptureHistory.index(legal(board, quiet + suffix), ep(board));
                int rook = CaptureHistory.index(legal(board, capture + suffix), ep(board));
                assertEquals(0, none & 7);
                assertEquals(Piece.ROOK, rook & 7);
                assertEquals(CaptureHistory.index(legal(board, quiet + "q"), ep(board)), none);
                assertEquals(CaptureHistory.index(legal(board, capture + "q"), ep(board)), rook);
            }
        }
    }

    @Test void signedGravityIsBoundedSaturatingAndReversesAccumulatedEvidence() {
        int[] table = new int[CaptureHistory.SIZE];
        CaptureHistory.update(table, 0, 4096);
        assertEquals(4096, table[0]);
        CaptureHistory.update(table, 0, 4096);
        assertEquals(7168, table[0]);
        CaptureHistory.update(table, 0, -4096);
        assertEquals(1280, table[0]);
        CaptureHistory.update(table, 0, Integer.MAX_VALUE);
        assertEquals(CaptureHistory.LIMIT, table[0]);
        CaptureHistory.update(table, 0, Integer.MIN_VALUE);
        assertEquals(-CaptureHistory.LIMIT, table[0]);
        for(int sign : new int[] {1, -1}) {
            for(int i = 0; i < 100_000; i++) {
                CaptureHistory.update(table, 0, sign * 9);
                assertTrue(Math.abs(table[0]) <= CaptureHistory.LIMIT);
            }
            assertEquals(sign * CaptureHistory.LIMIT, table[0]);
            CaptureHistory.update(table, 0, sign);
            assertEquals(sign * CaptureHistory.LIMIT, table[0]);
        }
    }

    @Test void cutoffUpdateUsesCappedDepthSquaredAndOnlyTheSearchedTacticalPrefix() {
        long[] board = Board.fromFen(PROMOTION);
        long loser = legal(board, "a7b8q"), winner = legal(board, "a7a8q");
        long quiet = legal(board, "h1g1");
        long[] moves = {quiet, loser, winner, legal(board, "h1g2")};
        for(int depth : new int[] {1, 3, 64, 256}) {
            int[] table = new int[CaptureHistory.SIZE];
            CaptureHistory.recordCutoff(table, moves, 2, ep(board), depth);
            int expected = depth == 1 ? 1 : depth == 3 ? 9 : 4096;
            assertEquals(expected, table[CaptureHistory.index(winner, ep(board))]);
            assertEquals(-expected, table[CaptureHistory.index(loser, ep(board))]);
            assertEquals(2, Arrays.stream(table).filter(v -> v != 0).count());
            assertEquals(0, table[CaptureHistory.index(quiet, ep(board))]);
            int[] before = table.clone();
            CaptureHistory.recordCutoff(table, moves, 3, ep(board), depth);
            assertArrayEquals(before, table, "A quiet cutoff must not update any tactical or quiet");
        }
        long[] epBoard = Board.fromFen(EP);
        long epMove = legal(epBoard, "e5d6");
        int[] table = new int[CaptureHistory.SIZE];
        CaptureHistory.recordCutoff(table, new long[] {epMove}, 0, ep(epBoard), 2);
        assertEquals(4, table[CaptureHistory.index(epMove, ep(epBoard))]);
    }

    @Test void actualSearchRewardsTacticalCutoffsIncludingHashAndPenalizesOnlyEarlierTacticals() throws Exception {
        long[] board = Board.fromFen(ExactSearchHarness.orderingPositions().get(1).fen());
        var reference = new ExactSearch(HCE, null, MODE);
        List<Long> order = ranked(reference, board, 0);
        List<Long> tactical = order.stream().filter(m -> CaptureHistory.isTactical(m, ep(board))).toList();
        assertTrue(tactical.size() >= 2);
        long winner = tactical.get(1);
        assertNotEquals(CaptureHistory.index(tactical.get(0), ep(board)), CaptureHistory.index(winner, ep(board)));
        long quiet = order.stream().filter(m -> !CaptureHistory.isTactical(m, ep(board))).findFirst().orElseThrow();
        for(long hash : new long[] {0, quiet, winner}) {
            var visits = new ArrayList<Long>();
            var byKey = new HashMap<Long, Long>();
            for(long move : order) byKey.put(ExhaustiveOracle.child(board, move)[Board.KEY], move);
            var table = hash == 0 ? null : new TTable(1);
            var search = new ExactSearch(new ExactEvaluator() {
                public int evaluate(long[] b, int p) { return byKey.get(b[Board.KEY]) == winner ? -80 : 0; }
                public void child(long[] parent, long[] child, int ply) { visits.add(byKey.get(child[Board.KEY])); }
            }, table, MODE);
            search.beginRequest();
            if(table != null) table.save(ExactSearchTTableTest.key(board, GameHistory.initial(board)),
                    2, TTable.TYPE_EXACT, 999, hash); // Hint only; no score cutoff.
            var result = search.searchWindow(board, GameHistory.initial(board), 1, -100, 50, ExactSearch.NEVER_CANCELLED);
            search.endRequest();
            assertTrue(result.completed()); assertEquals(80, result.score()); assertEquals(winner, result.bestMove());
            assertEquals(winner, visits.getLast());
            if(hash != 0) assertEquals(hash, visits.getFirst());
            int[] learned = history(search);
            assertEquals(1, learned[CaptureHistory.index(winner, ep(board))]);
            var failed = visits.subList(0, visits.size() - 1).stream()
                    .filter(m -> CaptureHistory.isTactical(m, ep(board))).toList();
            for(long move : failed) assertEquals(-1, learned[CaptureHistory.index(move, ep(board))]);
            assertEquals(failed.size() + 1, Arrays.stream(learned).filter(v -> v != 0).count());
            assertEquals(0, learned[CaptureHistory.index(quiet, ep(board))]);
        }
        // An actual quiet hash cutoff leaves the entire table at zero.
        var table = new TTable(1);
        var search = new ExactSearch((b, p) -> -80, table, MODE);
        search.beginRequest();
        table.save(ExactSearchTTableTest.key(board, GameHistory.initial(board)), 2, TTable.TYPE_EXACT, 999, quiet);
        assertEquals(quiet, search.searchWindow(board, GameHistory.initial(board), 1, -100, 50, ExactSearch.NEVER_CANCELLED).bestMove());
        search.endRequest();
        assertTrue(Arrays.stream(history(search)).allMatch(v -> v == 0));
    }

    @Test void adaptiveOrderingKeepsMaterialPrimaryStableTiesCompleteMovesAndEveryHashClass() throws Exception {
        var fens = new ArrayList<String>();
        for(var p : ExactSearchHarness.orderingPositions()) fens.add(p.fen());
        fens.addAll(List.of(EP, PROMOTION, "4k3/8/8/3R4/3Pp3/8/8/4K3 b - d3 0 1",
                "4k3/8/8/8/8/8/6p1/4K2R b - - 0 1"));
        boolean reorderedTie = false, stableTie = false, primaryDominates = false;
        boolean good = false, bad = false, quiet = false;
        for(String fen : fens) {
            long[] board = Board.fromFen(fen);
            int ep = ep(board);
            long[] generated = ExhaustiveOracle.legalMoves(board);
            var search = new ExactSearch(HCE, null, MODE);
            int[] history = history(search);
            // Extreme opposing evidence exercises the whole bounded secondary range.
            for(int i = 0; i < generated.length; i++) if(CaptureHistory.isTactical(generated[i], ep))
                history[CaptureHistory.index(generated[i], ep)] = i % 3 == 0 ? -CaptureHistory.LIMIT : CaptureHistory.LIMIT;
            List<Long> actual = ranked(search, board, 0);
            assertEquals(expected(board, history, 0), actual);
            assertEquals(actual, ranked(search, board, 0), "Deterministic with identical history");
            assertEquals(generated.length, new HashSet<>(actual).size());
            for(int i = 0; i < generated.length; i++) for(int j = i + 1; j < generated.length; j++) {
                long a = generated[i], b = generated[j];
                if(!CaptureHistory.isTactical(a, ep) || !CaptureHistory.isTactical(b, ep)) continue;
                if(See.atLeastGeneratedLegal(board, a, 0) != See.atLeastGeneratedLegal(board, b, 0)) continue;
                int ma = ExactSearch.tacticalMaterialValue(a, ep), mb = ExactSearch.tacticalMaterialValue(b, ep);
                int ha = history[CaptureHistory.index(a, ep)], hb = history[CaptureHistory.index(b, ep)];
                if(ma == mb && ha < hb) { reorderedTie = true; assertTrue(actual.indexOf(b) < actual.indexOf(a)); }
                if(ma == mb && ha == hb) { stableTie = true; assertTrue(actual.indexOf(a) < actual.indexOf(b)); }
                if(ma > mb && ha < hb || ma < mb && ha > hb) {
                    primaryDominates = true;
                    assertEquals(ma > mb, actual.indexOf(a) < actual.indexOf(b));
                }
            }
            for(long hash : generated) {
                if(!CaptureHistory.isTactical(hash, ep)) quiet = true;
                else if(See.atLeastGeneratedLegal(board, hash, 0)) good = true;
                else bad = true;
                List<Long> withHash = ranked(search, board, hash);
                assertEquals(expected(board, history, hash), withHash);
                assertEquals(hash, withHash.getFirst());
                assertEquals(generated.length, new HashSet<>(withHash).size());
            }
        }
        assertTrue(reorderedTie && stableTie && primaryDominates && good && bad && quiet,
                "Fixtures must exercise adaptive ties, stable ties, opposing material/history and every hash class");
    }

    @Test void everyInvocationResetsHistoryIncludingDriverRequestsCancellationAndOwnerReuse() throws Exception {
        long[] board = Board.fromFen(ExactSearchHarness.orderingPositions().get(1).fen());
        var first = new ExactSearch(HCE, null, MODE);
        var second = new ExactSearch(HCE, null, MODE);
        assertNotSame(history(first), history(second));
        first.beginRequest();
        var a = first.search(board, 3);
        int[] learned = history(first).clone();
        assertTrue(Arrays.stream(learned).anyMatch(v -> v > 0));
        // Final signs depend on this tree; the controlled-cutoff test proves negative maluses separately.
        assertTrue(Arrays.stream(history(second)).allMatch(v -> v == 0), "Independent owners never share evidence");
        Arrays.fill(history(first), 73);
        var b = first.search(board, 3);
        assertEquals(a.score(), b.score()); assertEquals(a.bestMove(), b.bestMove()); assertEquals(a.nodes(), b.nodes());
        assertArrayEquals(a.principalVariation(), b.principalVariation());
        assertArrayEquals(learned, history(first), "Reset even within a single beginRequest lifetime");
        Arrays.fill(history(first), 73);
        assertFalse(first.search(board, GameHistory.initial(board), 3, () -> true).completed());
        assertTrue(Arrays.stream(history(first)).allMatch(v -> v == 0));
        Arrays.fill(history(first), 73);
        first.search(board, 0);
        assertTrue(Arrays.stream(history(first)).allMatch(v -> v == 0));
        first.endRequest();
        assertNull(history(new ExactSearch(HCE, null, ExactSearch.CONTROL)));
        assertNull(history(new ExactSearch(HCE, null, ExactSearch.SEE_MATERIAL)));
    }

    @Test void drawsTerminalsAndApplicableTtCutoffsDoNotLearn() throws Exception {
        for(var p : ExactSearchHarness.positions().subList(4, 6)) {
            var search = new ExactSearch(HCE, null, MODE);
            search.search(Board.fromFen(p.fen()), 3);
            assertTrue(Arrays.stream(history(search)).allMatch(v -> v == 0));
        }
        long[] draw = Board.fromFen("3rk3/8/8/3p4/8/8/8/3QK3 w - - 100 1");
        var search = new ExactSearch(HCE, null, MODE);
        search.search(draw, 3);
        assertTrue(Arrays.stream(history(search)).allMatch(v -> v == 0));
        long[] board = Board.fromFen(EP);
        long hash = legal(board, "e5d6");
        for(int type : new int[] {TTable.TYPE_EXACT, TTable.TYPE_LOWER, TTable.TYPE_UPPER}) {
            var table = new TTable(1);
            var cutoff = new ExactSearch(HCE, table, MODE);
            cutoff.beginRequest();
            int value = type == TTable.TYPE_UPPER ? -50 : 50;
            table.save(ExactSearchTTableTest.key(board, GameHistory.initial(board)), 2, type, value, hash);
            var result = cutoff.searchWindow(board, GameHistory.initial(board), 2, -50, 50, ExactSearch.NEVER_CANCELLED);
            assertEquals(1, result.nodes());
            assertTrue(Arrays.stream(history(cutoff)).allMatch(v -> v == 0), "TT evidence isn't a searched tactical cutoff");
            cutoff.endRequest();
        }
    }

    private static int[] history(ExactSearch search) throws Exception {
        var field = ExactSearch.class.getDeclaredField("captureHistory");
        field.setAccessible(true);
        return (int[]) field.get(search);
    }

    private static List<Long> ranked(ExactSearch search, long[] board, long hash) throws Exception {
        long[] moves = ExhaustiveOracle.legalMoves(board);
        var method = ExactSearch.class.getDeclaredMethod("orderSeeClassified", long[].class, long[].class,
                int.class, int.class, long.class, int.class);
        method.setAccessible(true);
        method.invoke(search, board, moves, moves.length, (int) board[Board.STATUS], hash, -1);
        return Arrays.stream(moves).boxed().toList();
    }

    private static List<Long> expected(long[] board, int[] history, long hash) {
        var good = new ArrayList<Long>(); var bad = new ArrayList<Long>(); var quiet = new ArrayList<Long>();
        int ep = ep(board);
        for(long move : ExhaustiveOracle.legalMoves(board)) {
            if(move == hash) continue;
            if(!CaptureHistory.isTactical(move, ep)) quiet.add(move);
            else if(See.atLeastGeneratedLegal(board, move, 0)) good.add(move);
            else bad.add(move);
        }
        Comparator<Long> comparator = Comparator.comparingInt((Long m) -> ExactSearch.tacticalMaterialValue(m, ep)).reversed()
                .thenComparing(Comparator.comparingInt((Long m) -> history[CaptureHistory.index(m, ep)]).reversed());
        good.sort(comparator); bad.sort(comparator);
        if(hash != 0) good.addFirst(hash);
        good.addAll(bad); good.addAll(quiet);
        return good;
    }

    private static int ep(long[] board) { return Board.enPassantSquare((int) board[Board.STATUS]); }

    private static long legal(long[] board, String coordinate) {
        for(long move : ExhaustiveOracle.legalMoves(board)) if(Move.coordinate(move).equals(coordinate)) return move;
        throw new AssertionError("Missing legal move " + coordinate);
    }
}
