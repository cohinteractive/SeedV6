package com.ohinteractive.seedv6.search.exact;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.core.brn.BrnFeatureSchema;
import com.ohinteractive.seedv6.core.brn.BrnModel;
import com.ohinteractive.seedv6.core.brn1.Brn1Model;
import com.ohinteractive.seedv6.core.brn2.Brn2Model;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import com.ohinteractive.seedv6.tools.search.ExactSearchHarness;
import static org.junit.jupiter.api.Assertions.*;

class ExactSearchTTableTest {
    private static final ExactEvaluator HCE = (b, p) -> Eval.evaluate(b);
    private static final String PAWNS = "4k3/pp6/8/8/8/8/PP6/4K3 w - - 0 1";

    @Test void thirtyFiveIndependentExhaustiveOracleComparisonsIncludeOptimalMovesAndPvPrefixes() {
        List<String> fens = new ArrayList<>();
        for(var position : ExactSearchHarness.positions()) fens.add(position.fen());
        fens.addAll(List.of("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2", "1r5k/P7/8/8/8/8/8/7K w - - 0 1",
                "4r1k1/8/8/8/8/8/8/2B1K3 w - - 0 1"));
        int comparisons = 0;
        for(int i = 0; i < fens.size(); i++) {
            long[] board = Board.fromFen(fens.get(i)); var game = GameHistory.initial(board);
            var search = new ExactSearch(HCE, new TTable(1));
            search.beginRequest(); // old depth entries also exercise ordering across horizons
            for(int depth = 0; depth <= (i < 6 && i != 1 ? 3 : 2); depth++) {
                int expected = oracle(board, game, depth);
                var result = search.search(board, game, depth, ExactSearch.NEVER_CANCELLED);
                assertEquals(expected, result.score(), "fixture=" + i + " depth=" + depth);
                assertOptimalAndPv(board, game, depth, result, HCE);
                comparisons++;
            }
            search.endRequest();
        }
        assertEquals(35, comparisons);
    }

    @Test void fixedDepthFourMatrixPreservesOffRegressionAndOnValue() {
        String[] best = {"b1c3", "d5e6", "b4f4", "g6g7"};
        int[] scores = {-4, -211, 14, 32767}; long[] nodes = {4864, 15130, 748, 769};
        int i = 0;
        for(var position : ExactSearchHarness.positions().subList(0, 4)) {
            long[] board = Board.fromFen(position.fen());
            var off = new ExactSearch().search(board, 4);
            var on = new ExactSearch(HCE, new TTable(4)).search(board, 4);
            assertEquals(scores[i], off.score()); assertEquals(nodes[i], off.nodes());
            assertEquals(best[i++], Move.coordinate(off.bestMove()));
            assertEquals(off.score(), on.score()); assertLegalPv(board, on);
            // Exact child re-search verifies ties without requiring identical visitation/PVs.
            assertOptimalByExact(board, 4, on);
        }
    }

    @Test void terminalsWinOverConflictingCurrentExactEntries() {
        for(String fen : List.of("7k/6Q1/5K2/8/8/8/8/8 b - - 100 1",
                "7k/5K2/6Q1/8/8/8/8/8 b - - 0 1", "4k3/8/8/8/8/8/P7/4K3 w - - 100 1",
                "4k3/8/8/8/8/8/8/4K3 w - - 0 1")) {
            long[] board = Board.fromFen(fen); assertTerminalPoison(board, GameHistory.initial(board));
        }
        long[] board = Board.startingPosition(); var game = GameHistory.builder(board);
        for(int i = 0; i < 8; i++) { board = play(board, new String[] {"g1f3", "g8f6", "f3g1", "f6g8"}[i % 4]); game.appendPosition(board); }
        assertTerminalPoison(board, game.snapshot());
    }

    private static void assertTerminalPoison(long[] board, GameHistory game) {
        for(int depth : new int[] {0, 3}) {
            var table = new TTable(1);
            var search = new ExactSearch((b, p) -> { throw new AssertionError("Terminal evaluation"); }, table);
            search.beginRequest();
            table.save(key(board, game), depth, TTable.TYPE_EXACT, 12345, 0);
            var result = search.search(board, game, depth, ExactSearch.NEVER_CANCELLED);
            assertEquals(new ExactSearch().search(board, game, depth, ExactSearch.NEVER_CANCELLED).score(), result.score());
            assertEquals(1, result.nodes()); assertFalse(result.hasMove()); search.endRequest();
        }
    }

    @Test void shallowerDeeperAndOldGenerationPoisonCannotProveScores() {
        long[] board = Board.startingPosition(); var game = GameHistory.initial(board);
        for(int storedDepth : new int[] {0, 1, 3, 4}) for(int type = 0; type < 3; type++) {
            var table = new TTable(1); var search = new ExactSearch(HCE, table); search.beginRequest();
            table.save(key(board, game), storedDepth, type, type == TTable.TYPE_UPPER ? -30000 : 30000, move(board, "a2a3"));
            assertEquals(new ExactSearch().searchWindow(board, game, 2, -100, 100, ExactSearch.NEVER_CANCELLED).score(),
                    search.searchWindow(board, game, 2, -100, 100, ExactSearch.NEVER_CANCELLED).score()); search.endRequest();
        }
        for(int type = 0; type < 3; type++) {
            var table = new TTable(1); var search = new ExactSearch(HCE, table);
            search.beginRequest(); table.save(key(board, game), 2, type, type == 2 ? -30000 : 30000, move(board, "a2a3")); search.endRequest();
            assertEquals(new ExactSearch().searchWindow(board, game, 2, -100, 100, ExactSearch.NEVER_CANCELLED).score(),
                    search.searchWindow(board, game, 2, -100, 100, ExactSearch.NEVER_CANCELLED).score());
        }
    }

    @Test void exactAndCuttingBoundsReturnWhileNonCuttingBoundsLeaveOriginalWindowAlone() {
        long[] board = Board.startingPosition(); var game = GameHistory.initial(board);
        // Constant -37 child values yield +37. Poisoned non-cutting bounds would
        // break the result if used to tighten the window.
        int[][] cases = {{0, 7, 7, 1}, {1, 50, 50, 1}, {2, -50, -50, 1}, {1, 49, 37, 21}, {2, -49, 37, 21}};
        for(int[] c : cases) {
            var table = new TTable(1); var search = new ExactSearch((b, p) -> -37, table); search.beginRequest();
            table.save(key(board, game), 1, c[0], c[1], move(board, "a2a3"));
            var result = search.searchWindow(board, game, 1, -50, 50, ExactSearch.NEVER_CANCELLED);
            assertEquals(c[2], result.score()); assertEquals(c[3], result.nodes()); assertLegalPv(board, result); search.endRequest();
        }
        for(int type : new int[] {1, 2}) {
            var table = new TTable(1); var search = new ExactSearch((b, p) -> 37, table); search.beginRequest();
            table.save(key(board, game), 2, type, type == 1 ? 49 : -49, move(board, "a2a3"));
            var result = search.searchWindow(board, game, 2, -50, 50, ExactSearch.NEVER_CANCELLED);
            var reference = new ExactSearch((b, p) -> 37).searchWindow(board, game, 2, -50, 50, ExactSearch.NEVER_CANCELLED);
            assertEquals(reference.score(), result.score());
            // Tightening either side would change interior cutoffs/node count.
            assertEquals(reference.nodes(), result.nodes()); search.endRequest();
        }
    }

    @Test void completedPositiveDepthNodesClassifyAgainstOriginalWindowIncludingEquality() {
        long[] board = Board.startingPosition(); var game = GameHistory.initial(board);
        for(int depth : new int[] {1, 2}) for(int[] window : new int[][] {{-50, 50}, {37, 50}, {-50, 37}}) {
            var table = new TTable(1); var search = new ExactSearch((b, p) -> (depth % 2 == 0 ? 37 : -37), table);
            var result = search.searchWindow(board, game, depth, window[0], window[1], ExactSearch.NEVER_CANCELLED);
            assertEquals(37, result.score()); var entry = new TTable.TEntry(); assertTrue(table.probe(key(board, game), entry));
            assertEquals(window[0] == 37 ? 2 : window[1] == 37 ? 1 : 0, (entry.data >>> 8) & 3);
        }
    }

    @Test void legalHashMovesOrderEvenWhenDepthOrGenerationFailsAndInvalidMovesAreIgnored() {
        long[] board = Board.startingPosition(); var game = GameHistory.initial(board);
        long promoted = move(board, "h2h4");
        for(boolean old : new boolean[] {false, true}) {
            var visits = new ArrayList<Long>(); var table = new TTable(1);
            var search = new ExactSearch(new ExactEvaluator() {
                public int evaluate(long[] b, int p) { return 0; }
                public void child(long[] parent, long[] child, int ply) { if(ply == 0) visits.add(child[Board.KEY]); }
            }, table);
            search.beginRequest(); table.save(key(board, game), old ? 1 : 2, 0, 999, promoted);
            if(old) { search.endRequest(); search.beginRequest(); }
            var result = search.search(board, 1); assertEquals(0, result.score()); assertEquals(promoted, result.bestMove());
            List<Long> expected = new ArrayList<>(); expected.add(ExhaustiveOracle.child(board, promoted)[Board.KEY]);
            for(long m : ExhaustiveOracle.legalMoves(board)) if(m != promoted) expected.add(ExhaustiveOracle.child(board, m)[Board.KEY]);
            assertEquals(expected, visits); search.endRequest();
        }
        for(long invalid : new long[] {0, Long.MAX_VALUE, move(Board.startingPosition(), "a2a3") ^ (1L << 40)}) {
            var table = new TTable(1); var search = new ExactSearch((b, p) -> 0, table); search.beginRequest();
            table.save(key(board, game), 3, 0, 999, invalid);
            var result = search.search(board, 1); assertEquals(0, result.score());
            assertEquals(ExhaustiveOracle.legalMoves(board)[0], result.bestMove()); search.endRequest();
        }
    }

    @Test void incompleteRootAndLastLeafNeverStoreCompletedEvidence() {
        long[] board = Board.startingPosition(); var game = GameHistory.initial(board);
        for(int limit : new int[] {0, 15, 60}) {
            var table = new TTable(1); var search = new ExactSearch(HCE, table); var checks = new AtomicInteger();
            var result = search.search(board, game, 3, () -> checks.incrementAndGet() > limit);
            assertFalse(result.completed()); assertFalse(table.probe(key(board, game), new TTable.TEntry()));
        }
        for(int depth : new int[] {0, 1}) {
            var table = new TTable(1); var stop = new AtomicBoolean(); var evaluations = new AtomicInteger();
            int lastLeaf = depth == 0 ? 1 : ExhaustiveOracle.legalMoves(board).length;
            var search = new ExactSearch((b, p) -> {
                if(evaluations.incrementAndGet() == lastLeaf) stop.set(true);
                return 42;
            }, table);
            assertFalse(search.search(board, game, depth, stop::get).completed());
            assertEquals(lastLeaf, evaluations.get());
            assertFalse(table.probe(key(board, game), new TTable.TEntry()));
        }
    }

    @Test void depthZeroIgnoresOtherwiseApplicableExactAndBoundEvidenceAndHashMoves() {
        long[] board = Board.startingPosition(); var game = GameHistory.initial(board);
        int expected = new ExactSearch((b, p) -> 37)
                .searchWindow(board, game, 0, -50, 50, ExactSearch.NEVER_CANCELLED).score();
        for(long hashMove : new long[] {0, Long.MAX_VALUE, move(board, "h2h4")}) {
            for(int[] evidence : new int[][] {{0, 7}, {1, 50}, {2, -50}}) {
                var table = new TTable(1) {
                    void seed() { super.save(key(board, game), 0, evidence[0], evidence[1], hashMove); }
                    @Override public boolean probe(long key, TEntry entry) {
                        throw new AssertionError("Static leaf must not enter a Search TT probe");
                    }
                    @Override public void save(long key, int depth, int type, int score, long move) {
                        throw new AssertionError("Static leaf must not enter a Search TT store");
                    }
                };
                var evaluations = new AtomicInteger();
                var search = new ExactSearch((b, p) -> { evaluations.incrementAndGet(); return 37; }, table);
                search.beginRequest();
                table.seed();
                var result = search.searchWindow(board, game, 0, -50, 50, ExactSearch.NEVER_CANCELLED);
                assertTrue(result.completed()); assertEquals(expected, result.score()); assertEquals(1, result.nodes());
                assertEquals(1, evaluations.get());
                assertArrayEquals(new long[0], result.principalVariation()); assertFalse(result.hasMove());
                search.endRequest();
            }
        }
    }

    @Test void onlyPositiveDepthNodesEnterSearchTableIncludingRootAndRecursiveStaticLeaves() {
        long[] board = Board.startingPosition(); var game = GameHistory.initial(board);
        for(int depth : new int[] {0, 1, 2}) {
            var positiveKeys = new HashSet<Long>();
            if(depth > 0) positiveKeys.add(key(board, game));
            if(depth > 1) for(long move : ExhaustiveOracle.legalMoves(board)) {
                long[] child = ExhaustiveOracle.child(board, move);
                positiveKeys.add(key(child, GameHistory.builder(game).appendPosition(child).snapshot()));
            }
            int[] traffic = new int[2];
            var table = new TTable(1) {
                @Override public boolean probe(long key, TEntry entry) {
                    assertTrue(positiveKeys.contains(key), "Static leaf entered a Search TT probe");
                    traffic[0]++;
                    return super.probe(key, entry);
                }
                @Override public void save(long key, int remainingDepth, int type, int score, long move) {
                    assertTrue(remainingDepth > 0, "Static leaf entered a Search TT store");
                    assertTrue(positiveKeys.contains(key));
                    traffic[1]++;
                    super.save(key, remainingDepth, type, score, move);
                }
            };
            var reference = new ExactSearch().search(board, depth);
            var result = new ExactSearch(HCE, table).search(board, depth);
            assertEquals(reference.score(), result.score()); assertLegalPv(board, result);
            assertEquals(positiveKeys.size(), traffic[0]);
            assertEquals(positiveKeys.size(), traffic[1]);
            System.out.printf("static-boundary depth=%d leafProbes=0 leafStores=0 positiveProbes=%d positiveStores=%d%n",
                    depth, traffic[0], traffic[1]);
        }
    }

    @Test void positiveDepthCutoffsPreserveLegalPvAndRootMoveRequirements() {
        long[] board = Board.startingPosition(); var game = GameHistory.initial(board);
        long legal = move(board, "h2h4");
        for(long hashMove : new long[] {0, Long.MAX_VALUE, legal}) {
            var table = new TTable(1); var search = new ExactSearch((b, p) -> 0, table);
            search.beginRequest(); table.save(key(board, game), 1, 0, 7, hashMove);
            var result = search.search(board, 1);
            assertEquals(hashMove == legal ? 1 : 21, result.nodes());
            assertEquals(hashMove == legal ? 7 : 0, result.score());
            assertEquals(hashMove == legal ? legal : ExhaustiveOracle.legalMoves(board)[0], result.bestMove());
            assertLegalPv(board, result); search.endRequest();
        }
        for(int mode = 0; mode < 3; mode++) {
            var table = new TTable(1); var search = new ExactSearch((b, p) -> { throw new AssertionError("Interior TT cutoff expected"); }, table);
            search.beginRequest();
            for(long move : ExhaustiveOracle.legalMoves(board)) {
                long[] child = ExhaustiveOracle.child(board, move);
                var childGame = GameHistory.builder(game).appendPosition(child).snapshot();
                table.save(key(child, childGame), 1, 0, 0,
                        mode == 0 ? 0 : mode == 1 ? Long.MAX_VALUE : ExhaustiveOracle.legalMoves(child)[0]);
            }
            var result = search.search(board, 2);
            assertEquals(21, result.nodes()); assertEquals(0, result.score());
            assertEquals(mode == 2 ? 2 : 1, result.principalVariation().length);
            assertLegalPv(board, result); search.endRequest();
        }
    }

    @Test void hashPromotionKeepsEveryOtherTacticalAndQuietMoveInItsOriginalOrder() {
        for(String fen : List.of(ExactSearchHarness.positions().get(1).fen(),
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2",
                "1r5k/P7/8/8/8/8/8/7K w - - 0 1")) {
            long[] board = Board.fromFen(fen); var game = GameHistory.initial(board);
            var baseline = rootVisits(board, game, null, 0, false);
            long[] legalMoves = ExhaustiveOracle.legalMoves(board);
            for(boolean old : new boolean[] {false, true}) for(long promoted : legalMoves) {
                var table = new TTable(1);
                var expected = new ArrayList<>(baseline);
                Long childKey = ExhaustiveOracle.child(board, promoted)[Board.KEY];
                assertTrue(expected.remove(childKey)); expected.add(0, childKey);
                assertEquals(expected, rootVisits(board, game, table, promoted, old));
            }
            for(long invalid : new long[] {0, Long.MAX_VALUE}) {
                assertEquals(baseline, rootVisits(board, game, new TTable(1), invalid, false));
            }
        }
    }

    private static List<Long> rootVisits(long[] board, GameHistory game, TTable table, long hashMove, boolean old) {
        var visits = new ArrayList<Long>();
        var search = new ExactSearch(new ExactEvaluator() {
            public int evaluate(long[] b, int ply) { return 0; }
            public void child(long[] parent, long[] child, int ply) { if(ply == 0) visits.add(child[Board.KEY]); }
        }, table);
        search.beginRequest();
        if(table != null) {
            table.save(key(board, game), 2, 0, 999, hashMove);
            if(old) { search.endRequest(); search.beginRequest(); }
        }
        assertTrue(search.search(board, game, 1, ExactSearch.NEVER_CANCELLED).completed());
        search.endRequest();
        return visits;
    }

    @Test void sameBoardDifferentRuleClockHistoryAndBrnFullStatusCannotLeakEvidence() {
        long[] fresh = Board.fromFen("4k3/8/8/8/8/8/8/R3K3 w - - 0 1");
        long[] nearDraw = Board.fromFen("4k3/8/8/8/8/8/8/R3K3 w - - 99 1");
        assertEquals(fresh[Board.KEY], nearDraw[Board.KEY]);
        assertNotEquals(key(fresh, GameHistory.initial(fresh)), key(nearDraw, GameHistory.initial(nearDraw)));
        var search = new ExactSearch((b, p) -> -37, new TTable(1)); search.beginRequest();
        assertEquals(37, search.search(fresh, 1).score()); assertEquals(0, search.search(nearDraw, 1).score()); search.endRequest();
        long[] later = Board.fromFen("4k3/8/8/8/8/8/8/R3K3 w - - 0 2");
        assertEquals(fresh[Board.KEY], later[Board.KEY]); assertNotEquals(key(fresh, GameHistory.initial(fresh)), key(later, GameHistory.initial(later)));

        long[] board = Board.startingPosition(); var builder = GameHistory.builder(board);
        for(int i = 0; i < 7; i++) { board = play(board, new String[] {"g1f3", "g8f6", "f3g1", "f6g8"}[i % 4]); builder.appendPosition(board); }
        var known = builder.snapshot(); var unknown = GameHistory.initial(board);
        assertNotEquals(key(board, known), key(board, unknown));
        var repetitionSearch = new ExactSearch((b, p) -> 37, new TTable(1)); repetitionSearch.beginRequest();
        assertEquals(-37, repetitionSearch.search(board, unknown, 1, ExactSearch.NEVER_CANCELLED).score());
        assertEquals(0, repetitionSearch.search(board, known, 1, ExactSearch.NEVER_CANCELLED).score()); repetitionSearch.endRequest();
    }

    @Test void irreversibleMoveForgetsOnlyUnreachableHistoryAndEnPassantUsesCanonicalIdentity() {
        long[] board = Board.startingPosition(); var builder = GameHistory.builder(board);
        for(String m : List.of("g1f3", "g8f6", "f3g1", "f6g8", "a2a4")) { board = play(board, m); builder.appendPosition(board); }
        assertEquals(key(board, GameHistory.initial(board)), key(board, builder.snapshot()));
        long[] parent = play(Board.startingPosition(), "a2a4"); var game = GameHistory.initial(parent);
        long[] child = play(parent, "g8f6"); var history = new SearchLineHistory(game); history.pushRealPosition(child);
        long incremental = SearchKey.childHistory(SearchKey.rootHistory(parent, game), history.currentKey(), (int) child[Board.STATUS]);
        assertEquals(SearchKey.rootHistory(child, GameHistory.builder(game).appendPosition(child).snapshot()), incremental);
    }

    @Test void realPawnMoveTranspositionsReduceNodesAndPreserveExactOptimalResult() {
        long[] board = Board.fromFen(PAWNS);
        // Two distinct legal paths converge after irreversible moves, so both
        // ordinary boards AND conservative Search keys really are identical.
        long[] a = board, b = board;
        for(String m : List.of("a2a3", "a7a6", "b2b3", "b7b6")) a = play(a, m);
        for(String m : List.of("b2b3", "b7b6", "a2a3", "a7a6")) b = play(b, m);
        assertArrayEquals(a, b); assertEquals(key(a, GameHistory.initial(a)), key(b, GameHistory.initial(b)));
        var off = new ExactSearch().search(board, 5);
        var on = new ExactSearch(HCE, new TTable(4)).search(board, 5);
        assertEquals(off.score(), on.score()); assertTrue(on.nodes() < off.nodes(), "off=" + off.nodes() + " on=" + on.nodes());
        var scoresOnly = new ExactSearch(HCE, new TTable(4) {
            @Override public boolean probe(long key, TEntry entry) {
                boolean hit = super.probe(key, entry);
                if(hit) entry.hashMove = 0; // test-only: isolate score reuse from ordering effects
                return hit;
            }
        }).search(board, 5);
        assertEquals(off.score(), scoresOnly.score());
        assertTrue(scoresOnly.nodes() < off.nodes(), "Actual score reuse must save nodes without hash ordering.");
        assertOptimalByExact(board, 5, on); assertLegalPv(board, on);
        System.out.printf("transposition depth=5 off=%d on=%d scoresOnly=%d score=%d best=%s%n", off.nodes(), on.nodes(), scoresOnly.nodes(), on.score(), Move.coordinate(on.bestMove()));
    }

    @Test void mateConversionsAtDifferentPliesRespectBothBandsAndDistanceOrdering() {
        for(int score : new int[] {-32511, -32510, -1, 0, 1, 32510, 32511}) for(int ply : new int[] {0, 1, 100, 256}) {
            assertEquals(score, TranspositionScores.toTableScore(score, ply));
            assertEquals(score, TranspositionScores.fromTableScore(score, ply));
        }
        for(int sign : new int[] {-1, 1}) for(int ply : new int[] {0, 1, 100, 256}) {
            int score = sign * (32768 - ply);
            assertEquals(sign * 32768, TranspositionScores.toTableScore(score, ply));
            assertEquals(score, TranspositionScores.fromTableScore(sign * 32768, ply));
        }
        assertTrue(TranspositionScores.fromTableScore(32767, 4) > TranspositionScores.fromTableScore(32765, 4));
        assertTrue(TranspositionScores.fromTableScore(-32765, 4) > TranspositionScores.fromTableScore(-32767, 4));
        for(String fen : List.of("7k/8/5KQ1/8/8/8/8/8 w - - 0 1", "6k1/8/5K2/8/8/8/8/Q7 b - - 0 1")) {
            long[] board = Board.fromFen(fen); var search = new ExactSearch((b, p) -> 0, new TTable(1)); search.beginRequest();
            for(int depth : new int[] {4, 5}) {
                var result = search.search(board, depth);
                assertEquals(fen.startsWith("7k") ? 32767 : -32764, result.score());
                assertLegalPv(board, result);
                if(result.score() < 0) assertNotEquals("g8f8", Move.coordinate(result.bestMove()));
            }
            search.endRequest();
        }
    }

    @Test void actualInteriorMateEntryDecodesAtANewRootPly() {
        long[] parent = Board.fromFen("7k/8/5KQ1/8/8/8/8/8 w - - 0 1"); var game = GameHistory.initial(parent);
        var table = new TTable(4); var search = new ExactSearch((b, p) -> 0, table); search.beginRequest();
        search.search(parent, game, 4, ExactSearch.NEVER_CANCELLED);
        boolean foundMate = false;
        for(long move : ExhaustiveOracle.legalMoves(parent)) {
            long[] child = ExhaustiveOracle.child(parent, move); var childGame = GameHistory.builder(game).appendPosition(child).snapshot();
            var entry = new TTable.TEntry();
            if(table.probe(key(child, childGame), entry) && (entry.data & 255) == 3
                    && ((entry.data >>> 8) & 3) == 0 && Math.abs((int) (entry.data >> 32)) >= 32512 && entry.hashMove != 0) {
                var fromRoot = search.search(child, childGame, 3, ExactSearch.NEVER_CANCELLED);
                assertEquals(1, fromRoot.nodes());
                assertEquals(new ExactSearch((b, p) -> 0).search(child, childGame, 3, ExactSearch.NEVER_CANCELLED).score(), fromRoot.score());
                assertLegalPv(child, fromRoot); foundMate = true;
            }
        }
        assertTrue(foundMate, "Fixture must actually cache an interior exact mate."); search.endRequest();
    }

    @Test void neuralCutoffsLeaveIncrementalStacksReusableAndMatchRebuild() {
        var network = NnueNetwork.initialized(73);
        var weights = new double[Brn2Model.PARAMETER_COUNT];
        for(int i = 0; i < weights.length; i++) weights[i] = ((i % 13) - 6) * 0.002;
        var brn2 = new Brn2Model(weights);
        List<SearchEvaluation[]> pairs = List.of(
                new SearchEvaluation[] {SearchEvaluation.incremental(network), SearchEvaluation.fullRecompute(network, NnueScoreMapping.V1)},
                new SearchEvaluation[] {SearchEvaluation.brn2(brn2), SearchEvaluation.brn2FullRecompute(brn2)},
                new SearchEvaluation[] {SearchEvaluation.brn(new BrnModel(new double[BrnFeatureSchema.PARAMETER_COUNT])), SearchEvaluation.brn(new BrnModel(new double[BrnFeatureSchema.PARAMETER_COUNT]))},
                new SearchEvaluation[] {SearchEvaluation.brn1(new Brn1Model(new double[Brn1Model.PARAMETER_COUNT])), SearchEvaluation.brn1(new Brn1Model(new double[Brn1Model.PARAMETER_COUNT]))});
        for(var pair : pairs) {
            var table = new TTable(4); var search = new ExactSearch(pair[0], table); search.beginRequest();
            for(String fen : List.of(PAWNS, "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2", "1r5k/P7/8/8/8/8/8/7K w - - 0 1")) {
                long[] board = Board.fromFen(fen);
                table.clear();
                table.save(key(board, GameHistory.initial(board)), 0, TTable.TYPE_EXACT, 12345, 0);
                var staticReference = new ExactSearch(pair[1]).search(board, 0);
                assertNotEquals(12345, staticReference.score());
                assertEquals(staticReference.score(), search.search(board, 0).score());
                var expected = new ExactSearch(pair[1]).search(board, 3);
                // Pre-search each real root child at the same horizon, then prove
                // the parent actually uses interior entries. Following siblings
                // must still rebuild/update from their own parent accumulator.
                table.clear(); var game = GameHistory.initial(board);
                for(long move : ExhaustiveOracle.legalMoves(board)) {
                    long[] child = ExhaustiveOracle.child(board, move);
                    search.search(child, GameHistory.builder(game).appendPosition(child).snapshot(), 2, ExactSearch.NEVER_CANCELLED);
                }
                var first = search.search(board, 3); assertEquals(expected.score(), first.score()); assertLegalPv(board, first);
                assertTrue(first.nodes() < expected.nodes(), "Preloaded interior evidence must actually cut off work.");
                var cached = search.search(board, 3); assertEquals(expected.score(), cached.score()); assertEquals(1, cached.nodes());
                assertEquals(new ExactSearch(pair[1]).search(board, 2).score(), search.search(board, 2).score());
            }
            search.endRequest();
        }
    }

    static long key(long[] board, GameHistory game) { return SearchKey.key(board, SearchKey.rootHistory(board, game)); }
    static long move(long[] board, String coordinate) {
        for(long move : ExhaustiveOracle.legalMoves(board)) if(Move.coordinate(move).equals(coordinate)) return move;
        throw new AssertionError("Illegal fixture move " + coordinate);
    }
    static long[] play(long[] board, String coordinate) { return ExhaustiveOracle.child(board, move(board, coordinate)); }
    static int oracle(long[] board, GameHistory game, int depth) { return new ExhaustiveOracle(HCE).score(board, new SearchLineHistory(game), depth, 0); }
    static void assertLegalPv(long[] board, ExactSearchResult result) {
        assertTrue(result.completed());
        if(result.hasMove()) assertEquals(result.bestMove(), result.principalVariation()[0]);
        for(long move : result.principalVariation()) {
            assertTrue(Arrays.stream(ExhaustiveOracle.legalMoves(board)).anyMatch(m -> m == move)); board = ExhaustiveOracle.child(board, move);
        }
    }
    static void assertOptimalByExact(long[] board, int depth, ExactSearchResult result) {
        long[] child = ExhaustiveOracle.child(board, result.bestMove()); var game = GameHistory.builder(board).appendPosition(child).snapshot();
        int childScore = new ExactSearch().search(child, game, depth - 1, ExactSearch.NEVER_CANCELLED).score();
        // Re-rooting removes one ply of mate distance.
        if(childScore >= TranspositionScores.MATE_THRESHOLD) childScore--;
        else if(childScore <= -TranspositionScores.MATE_THRESHOLD) childScore++;
        assertEquals(result.score(), -childScore);
    }
    static void assertOptimalAndPv(long[] board, GameHistory game, int depth, ExactSearchResult result, ExactEvaluator eval) {
        assertLegalPv(board, result); var line = new SearchLineHistory(game);
        if(result.hasMove()) {
            long[] child = ExhaustiveOracle.child(board, result.bestMove()); line.pushRealPosition(child);
            assertEquals(result.score(), -new ExhaustiveOracle(eval).score(child, line, depth - 1, 1)); line.popRealPosition();
        }
        long[] pv = result.principalVariation();
        for(long move : pv) { board = ExhaustiveOracle.child(board, move); line.pushRealPosition(board); }
        int leaf = new ExhaustiveOracle(eval).score(board, line, depth - pv.length, pv.length);
        assertEquals(result.score(), pv.length % 2 == 0 ? leaf : -leaf);
    }
}
