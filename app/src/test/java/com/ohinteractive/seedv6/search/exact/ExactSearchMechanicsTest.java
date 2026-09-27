package com.ohinteractive.seedv6.search.exact;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.See;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.tools.search.ExactSearchHarness;

class ExactSearchMechanicsTest {
    private static final int POLICY = ExactSearch.SEE_MATERIAL_QUIET_HISTORY;
    private static final ExactEvaluator HCE = (b, p) -> Eval.evaluate(b);
    private static final String HIGH110 = "7k/6pp/Q1Q1Q3/1Q1Q1Q2/Q1Q1Q3/R1B1N1R1/2B1N3/K7 w - - 0 1";
    private static final String HIGH99 = "7k/6pp/Q1Q1Q3/1Q1Q1Q2/Q1Q1Q3/8/1RBNBRN1/K7 w - - 0 1";
    private static final String EP = "4k3/8/8/3pP3/3r4/8/8/4K3 w - d6 0 1";
    private static final String PROMOTION = "1r5k/P7/8/8/8/8/8/7K w - - 0 1";

    static List<String> fens() {
        var fens = new ArrayList<String>();
        for(var p : ExactSearchHarness.orderingPositions()) fens.add(p.fen());
        fens.addAll(List.of(HIGH110, HIGH99, EP, PROMOTION,
                "4k3/8/8/3R4/3Pp3/8/8/4K3 b - d3 0 1",
                "4k3/8/8/8/8/8/6p1/4K2R b - - 0 1",
                "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
                "r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 0 1",
                "3rk3/8/8/3p4/8/8/8/3QK3 w - - 0 1"));
        return fens;
    }

    @Test void fullEnumerationMatchesUnchangedInsertionAndIndependentStablePolicyForEveryLegalHash() throws Exception {
        assertEquals(110, ExhaustiveOracle.legalMoves(Board.fromFen(HIGH110)).length);
        assertEquals(99, ExhaustiveOracle.legalMoves(Board.fromFen(HIGH99)).length);
        boolean good = false, bad = false, quiet = false, tacticalTie = false, quietTie = false, learned = false;
        int comparisons = 0;
        var control = new ExactSearch(HCE, null, POLICY);
        var learnedSearch = new ExactSearch(HCE, null, POLICY);
        learnedSearch.search(Board.startingPosition(), 4);
        int[] learnedState = history(learnedSearch).clone();
        assertTrue(Arrays.stream(learnedState).anyMatch(x -> x != 0));
        for(String fen : fens()) {
            long[] board = Board.fromFen(fen), generated = ExhaustiveOracle.legalMoves(board);
            for(int state = 0; state < 3; state++) {
                int[] hist = history(control); Arrays.fill(hist, 0);
                if(state == 1) for(int i = 0; i < generated.length; i++)
                    hist[QuietHistory.index(generated[i])] = switch(i % 3) { case 0 -> -16384; case 1 -> 16384; default -> 0; };
                if(state == 2) System.arraycopy(learnedState, 0, hist, 0, hist.length);
                learned |= state == 2 && Arrays.stream(generated).anyMatch(m -> hist[QuietHistory.index(m)] != 0);
                for(long hash : withInvalidHashes(generated)) {
                    long[] expected = insertion(control, board, hash);
                    var independent = new ArrayList<Long>(); for(long m : generated) independent.add(m);
                    independent.sort(Comparator.comparingInt((Long m) -> group(board, m, hash)).reversed()
                            .thenComparing(Comparator.comparingInt((Long m) -> evidence(board, m, hist)).reversed()));
                    assertEquals(independent, Arrays.stream(expected).boxed().toList());
                    for(int threshold : new int[] {8, 16, 24, 32, 512}) {
                        long[] full = generated.clone(); int[] keys = snapshot(control, board, full, hash);
                        Sort.full(full, keys, 0, full.length, threshold);
                        assertArrayEquals(expected, full);
                        comparisons++;
                    }
                    long[] lazy = generated.clone(); int[] keys = snapshot(control, board, lazy, hash);
                    int[] saved = hist.clone(); Arrays.fill(hist, -777); // Later child learning cannot change this snapshot.
                    for(int i = 0; i < lazy.length; i++) Sort.next(lazy, keys, 0, i, lazy.length);
                    System.arraycopy(saved, 0, hist, 0, hist.length);
                    assertArrayEquals(expected, lazy);
                    assertEquals(generated.length, new HashSet<>(Arrays.stream(lazy).boxed().toList()).size());
                    long[] set = generated.clone(), actual = lazy.clone(); Arrays.sort(set); Arrays.sort(actual); assertArrayEquals(set, actual);
                    if(hash != 0 && hash != Long.MAX_VALUE) {
                        assertEquals(hash, lazy[0]);
                        int g = group(board, hash, 0); good |= g == 2; bad |= g == 1; quiet |= g == 0;
                    }
                    if(hash == 0) for(int i = 0; i < expected.length; i++) for(int j = i + 1; j < expected.length; j++) {
                        if(group(board, expected[i], 0) == group(board, expected[j], 0)
                                && evidence(board, expected[i], hist) == evidence(board, expected[j], hist)) {
                            assertTrue(index(generated, expected[i]) < index(generated, expected[j]));
                            if(group(board, expected[i], 0) == 0) quietTie = true; else tacticalTie = true;
                        }
                    }
                }
            }
        }
        assertTrue(good && bad && quiet && tacticalTie && quietTie && learned);
        System.out.println("SR-017 exact full-sort sequence comparisons=" + comparisons + " plus lazy, every hash/history state");
    }

    @Test void allMechanicsReproduceDepthFiveRecordedTreesAndFullVisitationTraces() throws Exception {
        long[][] nodes = {{31418, 83934, 4861, 20741, 141150, 1563}, {27911, 65515, 4280, 19834, 120175, 1532}};
        int[] scores = {197, 403, 1177, -81, 250, 6};
        String[] best = {"e2e3", "d5e6", "e4d5", "c4c5", "c3d5", "e1d2"};
        for(int tt = 0; tt < 2; tt++) for(int p = 0; p < 6; p++) {
            ExactSearchResult baseline = null; long trace = 0; int[] learned = null;
            for(int mechanics = 0; mechanics < 3; mechanics++) {
                final long[] visits = {17};
                var evaluator = new ExactEvaluator() {
                    public int evaluate(long[] b, int ply) { return Eval.evaluate(b); }
                    public void child(long[] parent, long[] child, int ply) { visits[0] = visits[0] * 31 + child[Board.KEY] + ply; }
                };
                var search = new ExactSearch(evaluator, tt == 0 ? null : new TTable(4), POLICY, mechanics);
                var result = search.search(Board.fromFen(ExactSearchHarness.orderingPositions().get(p).fen()), 5);
                assertEquals(nodes[tt][p], result.nodes()); assertEquals(scores[p], result.score());
                assertEquals(best[p], Move.coordinate(result.bestMove()));
                if(baseline == null) { baseline = result; trace = visits[0]; learned = history(search).clone(); }
                else { same(baseline, result); assertEquals(trace, visits[0]); assertArrayEquals(learned, history(search)); }
            }
        }
    }

    @Test void oracleTerminalsDrawsCancellationAndOwnerReuseRemainExact() throws Exception {
        var positions = fens(); positions.addAll(ExactSearchHarness.positions().subList(3, 6).stream().map(ExactSearchHarness.Position::fen).toList());
        positions.add("3rk3/8/8/3p4/8/8/8/3QK3 w - - 100 1");
        positions.add("4k3/8/8/8/8/8/8/4K3 w - - 0 1");
        for(String fen : positions) for(boolean tt : new boolean[] {false, true}) for(int depth = 0; depth <= 2; depth++) {
            long[] board = Board.fromFen(fen), before = board.clone();
            var baseline = new ExactSearch(HCE, tt ? new TTable(1) : null, POLICY).search(board, depth);
            assertEquals(new ExhaustiveOracle(HCE).score(board, new com.ohinteractive.seedv6.rules.SearchLineHistory(GameHistory.initial(board)), depth, 0), baseline.score());
            for(int mechanics = 1; mechanics < 3; mechanics++) {
                var result = new ExactSearch(HCE, tt ? new TTable(1) : null, POLICY, mechanics).search(board, depth);
                same(baseline, result); ExactSearchTTableTest.assertOptimalAndPv(board, GameHistory.initial(board), depth, result, HCE);
                assertArrayEquals(before, board);
            }
        }
        for(boolean tt : new boolean[] {false, true}) for(int limit : new int[] {0, 40, 400}) {
            ExactSearchResult baseline = null;
            for(int mechanics = 0; mechanics < 3; mechanics++) {
                var table = tt ? new TTable(1) : null;
                var search = new ExactSearch(HCE, table, POLICY, mechanics);
                long[] board = Board.fromFen(ExactSearchHarness.orderingPositions().get(1).fen());
                var game = GameHistory.initial(board); var checks = new AtomicInteger();
                var result = search.search(board, game, 5, () -> checks.incrementAndGet() > limit);
                assertFalse(result.completed()); assertEquals(Value.INVALID, result.score()); assertFalse(result.hasMove());
                assertEquals(0, result.principalVariation().length); assertTrue(Arrays.stream(history(search)).allMatch(x -> x == 0));
                if(table != null) { assertFalse(table.probe(ExactSearchTTableTest.key(board, game), new TTable.TEntry())); table.clear(); }
                if(baseline == null) baseline = result; else same(baseline, result);
                same(new ExactSearch(HCE, tt ? new TTable(1) : null, POLICY).search(board, 3), search.search(board, 3));
            }
        }
        long[] board = Board.startingPosition(); var game = GameHistory.builder(board);
        for(int cycle = 0; cycle < 2; cycle++) for(String move : List.of("g1f3", "g8f6", "f3g1", "f6g8")) {
            board = ExactSearchTTableTest.play(board, move); game.appendPosition(board);
        }
        for(boolean tt : new boolean[] {false, true}) for(int mechanics = 0; mechanics < 3; mechanics++) {
            var result = new ExactSearch(HCE, tt ? new TTable(1) : null, POLICY, mechanics).search(board, game.snapshot(), 3, ExactSearch.NEVER_CANCELLED);
            assertTrue(result.completed()); assertEquals(0, result.score()); assertEquals(1, result.nodes()); assertFalse(result.hasMove());
        }
    }

    @Test void actualRootSearchUsesEveryLegalHashExactlyOnceAndSnapshotsBeforeSiblings() throws Exception {
        for(String fen : List.of(ExactSearchHarness.orderingPositions().get(1).fen(), EP, PROMOTION)) {
            long[] board = Board.fromFen(fen), generated = ExhaustiveOracle.legalMoves(board);
            for(long hash : withInvalidHashes(generated)) {
                List<Long> baseline = null;
                for(int mechanics = 0; mechanics < 3; mechanics++) {
                    var visits = new ArrayList<Long>(); var table = new TTable(1);
                    int[][] history = new int[1][];
                    var search = new ExactSearch(new ExactEvaluator() {
                        public int evaluate(long[] b, int ply) { return 0; }
                        public void initialize(long[] b) { for(int i = 0; i < generated.length; i++) history[0][QuietHistory.index(generated[i])] = (i % 3 - 1) * 100; }
                        public void child(long[] parent, long[] child, int ply) {
                            for(long move : generated) if(ExhaustiveOracle.child(parent, move)[Board.KEY] == child[Board.KEY]) visits.add(move);
                            // Deliberately mutate history between siblings after the node snapshot.
                            for(int i = 0; i < generated.length; i++) history[0][QuietHistory.index(generated[i])] = (generated.length - i) * 7;
                        }
                    }, table, POLICY, mechanics);
                    history[0] = history(search);
                    search.beginRequest(); table.save(ExactSearchTTableTest.key(board, GameHistory.initial(board)), 2, TTable.TYPE_EXACT, 999, hash);
                    assertTrue(search.search(board, 1).completed()); search.endRequest();
                    if(baseline == null) baseline = visits; else assertEquals(baseline, visits);
                    assertEquals(generated.length, visits.size()); assertEquals(visits.size(), new HashSet<>(visits).size());
                    if(hash != 0 && hash != Long.MAX_VALUE) assertEquals(hash, visits.getFirst());
                }
            }
        }
    }

    @Test void selectorRejectsPolicyChangesAndPreservesEstablishedConstructors() {
        for(int mode = 0; mode <= 9; mode++) if(mode != POLICY) {
            int policy = mode;
            assertThrows(IllegalArgumentException.class, () -> new ExactSearch(HCE, null, policy, ExactSearch.LAZY_SELECTION));
        }
        assertThrows(IllegalArgumentException.class, () -> new ExactSearch(HCE, null, POLICY, 3));
        assertThrows(IllegalArgumentException.class, () -> new ExactSearch(HCE, null, POLICY, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new ExactSearch(HCE, null, POLICY, 1, 513));
        same(new ExactSearch().search(Board.startingPosition(), 3), new ExactSearch(HCE, null, ExactSearch.CONTROL, 0).search(Board.startingPosition(), 3));
    }

    @Test void warmTtBoundsRequestLifetimesAndFinalChildCancellationMatch() throws Exception {
        long[] board = Board.startingPosition(); var game = GameHistory.initial(board);
        for(int type : new int[] {TTable.TYPE_EXACT, TTable.TYPE_LOWER, TTable.TYPE_UPPER}) {
            ExactSearchResult baseline = null;
            for(int mechanics = 0; mechanics < 3; mechanics++) {
                var table = new TTable(1); var search = new ExactSearch(HCE, table, POLICY, mechanics);
                search.beginRequest();
                int score = type == TTable.TYPE_EXACT ? 0 : type == TTable.TYPE_LOWER ? 50 : -50;
                table.save(ExactSearchTTableTest.key(board, game), 2, type, score, ExhaustiveOracle.legalMoves(board)[0]);
                var result = search.searchWindow(board, game, 2, -50, 50, ExactSearch.NEVER_CANCELLED);
                assertEquals(score, result.score()); assertEquals(1, result.nodes());
                if(baseline == null) baseline = result; else same(baseline, result);
                assertTrue(Arrays.stream(history(search)).allMatch(x -> x == 0)); search.endRequest();
            }
        }
        ExactSearchResult[] baseline = null;
        for(int mechanics = 0; mechanics < 3; mechanics++) {
            var search = new ExactSearch(HCE, new TTable(4), POLICY, mechanics);
            search.beginRequest(); var first = search.search(board, 4); var warm = search.search(board, 4); search.endRequest();
            assertEquals(1, warm.nodes());
            var later = search.search(board, 4);
            if(baseline == null) baseline = new ExactSearchResult[] {first, warm, later};
            else { same(baseline[0], first); same(baseline[1], warm); same(baseline[2], later); }
        }
        int count = ExhaustiveOracle.legalMoves(board).length;
        for(boolean tt : new boolean[] {false, true}) for(int mechanics = 0; mechanics < 3; mechanics++) {
            var calls = new AtomicInteger(); var table = tt ? new TTable(1) : null;
            var search = new ExactSearch((b, p) -> { calls.incrementAndGet(); return 0; }, table, POLICY, mechanics);
            var result = search.search(board, game, 1, () -> calls.get() == count);
            assertFalse(result.completed()); assertEquals(count + 1, result.nodes()); assertFalse(result.hasMove());
            assertTrue(Arrays.stream(history(search)).allMatch(x -> x == 0));
            if(table != null) assertFalse(table.probe(ExactSearchTTableTest.key(board, game), new TTable.TEntry()));
            Thread.currentThread().interrupt();
            try { assertFalse(search.search(board, 2).completed()); assertTrue(Thread.currentThread().isInterrupted()); }
            finally { Thread.interrupted(); }
        }
    }

    private static int[] history(ExactSearch search) throws Exception {
        var field = ExactSearch.class.getDeclaredField("quietHistory"); field.setAccessible(true); return (int[]) field.get(search);
    }
    private static long[] insertion(ExactSearch search, long[] board, long hash) throws Exception {
        long[] list = ExhaustiveOracle.legalMoves(board);
        Method method = ExactSearch.class.getDeclaredMethod("orderSeeClassified", long[].class, long[].class, int.class, int.class, long.class, int.class, int.class, int.class);
        method.setAccessible(true); method.invoke(search, board, list, list.length, (int) board[Board.STATUS], hash, -1, 0, -1); return list;
    }
    private static int[] snapshot(ExactSearch search, long[] board, long[] moves, long hash) throws Exception {
        int[] keys = new int[moves.length];
        Method method = ExactSearch.class.getDeclaredMethod("snapshotOrdering", long[].class, long[].class, int.class, long.class, int[].class, int.class);
        method.setAccessible(true); method.invoke(search, board, moves, moves.length, hash, keys, 0); return keys;
    }
    private static long[] withInvalidHashes(long[] moves) {
        long[] hashes = Arrays.copyOf(moves, moves.length + 2); hashes[moves.length + 1] = Long.MAX_VALUE; return hashes;
    }
    private static int group(long[] board, long move, long hash) {
        if(move == hash) return 3;
        if(!CaptureHistory.isTactical(move, Board.enPassantSquare((int) board[Board.STATUS]))) return 0;
        return See.evaluate(board, move) >= 0 ? 2 : 1;
    }
    private static int evidence(long[] board, long move, int[] history) {
        if(group(board, move, 0) == 0) return history[QuietHistory.index(move)];
        int side = Board.player((int) board[Board.STATUS]);
        return balance(ExhaustiveOracle.child(board, move), side) - balance(board, side);
    }
    private static int balance(long[] b, int side) {
        int score = 0;
        for(int square = 0; square < 64; square++) {
            int piece = Board.getSquare(b[0], b[1], b[2], b[3], square);
            if(piece != 0) score += ((piece >>> Board.PLAYER_SHIFT) == side ? 1 : -1) * Eval.exchangeValue(piece & Piece.TYPE);
        }
        return score;
    }
    private static int index(long[] moves, long move) { for(int i = 0; i < moves.length; i++) if(moves[i] == move) return i; throw new AssertionError(); }
    private static void same(ExactSearchResult a, ExactSearchResult b) {
        assertEquals(a.completed(), b.completed()); assertEquals(a.score(), b.score()); assertEquals(a.nodes(), b.nodes());
        assertEquals(a.bestMove(), b.bestMove()); assertArrayEquals(a.principalVariation(), b.principalVariation());
    }
}
