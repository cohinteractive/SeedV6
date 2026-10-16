package com.ohinteractive.seedv6.search.exact;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.tools.search.ExactSearchHarness;

class KillerMovesTest {
    private static final int MODE = ExactSearch.SEE_MATERIAL_QUIET_HISTORY_KILLERS;
    private static final ExactEvaluator HCE = (b, p) -> Eval.evaluate(b);
    private static final String EP = "4k3/8/8/3pP3/3r4/8/8/4K3 w - d6 0 1";
    private static final String PROMOTION = "1r5k/P6p/8/8/8/8/8/7K w - - 0 1";
    private static final String CASTLING = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1";
    private static final String EVASION = "4r1k1/8/8/8/8/8/8/2B1K3 w - - 0 1";

    @Test void primitiveStorageUsesEstablishedPlyCapacityWithRecentFirstDistinctReplacement() throws Exception {
        var search = new ExactSearch(HCE, null, MODE);
        long[] killers = killers(search);
        assertEquals((ExactSearch.MAX_DEPTH + 1) * 2, killers.length);
        assertEquals(4_112, killers.length * Long.BYTES);
        assertZero(killers);
        assertNull(field(search, "continuationHistory"));
        assertNull(field(search, "captureHistory"));
        long[] board = Board.startingPosition();
        long a = legal(board, "e2e4"), b = legal(board, "d2d4"), c = legal(board, "g1f3");
        for(int ply = 0; ply <= ExactSearch.MAX_DEPTH; ply++) {
            int base = ply * 2;
            assertEquals(0, killers[base]); assertEquals(0, killers[base + 1]);
            KillerMoves.recordCutoff(killers, ply, a, ep(board));
            assertEquals(a, killers[base]); assertEquals(0, killers[base + 1]);
            KillerMoves.recordCutoff(killers, ply, a, ep(board));
            assertEquals(a, killers[base]); assertEquals(0, killers[base + 1]);
            KillerMoves.recordCutoff(killers, ply, b, ep(board));
            assertEquals(b, killers[base]); assertEquals(a, killers[base + 1]);
            KillerMoves.recordCutoff(killers, ply, b, ep(board));
            assertEquals(b, killers[base]); assertEquals(a, killers[base + 1]);
            KillerMoves.recordCutoff(killers, ply, a, ep(board));
            assertEquals(a, killers[base]); assertEquals(b, killers[base + 1]);
            KillerMoves.recordCutoff(killers, ply, c, ep(board));
            assertEquals(c, killers[base]); assertEquals(a, killers[base + 1]);
            KillerMoves.recordCutoff(killers, ply, 0, ep(board));
            assertEquals(c, killers[base]); assertEquals(a, killers[base + 1]);
            for(int earlier = 0; earlier < ply; earlier++) {
                assertEquals(c, killers[earlier * 2]); assertEquals(a, killers[earlier * 2 + 1]);
            }
        }
        for(int mode = ExactSearch.CONTROL; mode < MODE; mode++)
            assertNull(killers(new ExactSearch(HCE, null, mode)), "Existing modes allocate no killers");
    }

    @Test void fullGeneratedMoveIdentityExcludesCapturesEpAndEveryPromotionButAllowsCastling() {
        for(String fen : List.of(EP, PROMOTION, "4k3/8/8/3R4/3Pp3/8/8/4K3 b - d3 0 1",
                "4k3/8/8/8/8/8/6p1/4K2R b - - 0 1", CASTLING, CASTLING.replace(" w ", " b "), EVASION)) {
            long[] board = Board.fromFen(fen);
            for(long move : ExhaustiveOracle.legalMoves(board)) {
                assertNotEquals(0, move);
                long[] killers = new long[4];
                KillerMoves.recordCutoff(killers, 1, move, ep(board));
                assertArrayEquals(new long[] {0, 0, tactical(board, move) ? 0 : move, 0}, killers);
            }
        }
    }

    @Test void actualQuietCutoffsPreserveMainRewardMalusAndRecordOnlyTheSearchedWinnerIncludingHash() throws Exception {
        long[] board = Board.fromFen(ExactSearchHarness.orderingPositions().get(1).fen());
        List<Long> order = ranked(new ExactSearch(HCE, null, ExactSearch.SEE_MATERIAL_QUIET_HISTORY), board, 0, 0);
        List<Long> quiets = order.stream().filter(m -> !tactical(board, m)).toList();
        long failed = quiets.getFirst(), winner = quiets.get(1);
        long tacticalHash = order.stream().filter(m -> tactical(board, m)).findFirst().orElseThrow();
        for(boolean tt : new boolean[] {false, true}) for(long hash : new long[] {0, failed, winner, tacticalHash}) {
            if(!tt && hash != 0) continue;
            var visits = new ArrayList<Long>(); var byKey = new HashMap<Long, Long>();
            for(long move : order) assertNull(byKey.put(ExhaustiveOracle.child(board, move)[Board.KEY], move));
            var table = tt ? new TTable(1) : null;
            var search = new ExactSearch(new ExactEvaluator() {
                public int evaluate(long[] b, int p) { return byKey.get(b[Board.KEY]) == winner ? -80 : 0; }
                public void child(long[] parent, long[] child, int ply) { visits.add(byKey.get(child[Board.KEY])); }
            }, table, MODE);
            search.beginRequest();
            if(hash != 0) table.save(ExactSearchTTableTest.key(board, GameHistory.initial(board)), 2, TTable.TYPE_EXACT, 999, hash);
            var result = search.searchWindow(board, GameHistory.initial(board), 1, -100, 50, ExactSearch.NEVER_CANCELLED);
            search.endRequest();
            assertTrue(result.completed()); assertEquals(80, result.score()); assertEquals(winner, result.bestMove());
            assertEquals(winner, visits.getLast());
            if(hash != 0) assertEquals(hash, visits.getFirst());
            int[] expectedMain = new int[QuietHistory.SIZE];
            for(long move : visits) if(!tactical(board, move)) expectedMain[QuietHistory.index(move)] = move == winner ? 1 : -1;
            assertArrayEquals(expectedMain, main(search), "Generated-but-unsearched moves and tacticals cannot learn");
            long[] expectedKillers = new long[killers(search).length]; expectedKillers[0] = winner;
            assertArrayEquals(expectedKillers, killers(search), "Failed or unsearched quiets cannot become killers");
        }
    }

    @Test void actualTacticalCutoffsNeverBecomeKillersAndCastlingCutoffsDo() throws Exception {
        for(String fen : List.of(EP, PROMOTION, "4k3/8/8/8/8/8/P5p1/4K2R b - - 0 1",
                CASTLING, CASTLING.replace(" w ", " b "))) {
            long[] board = Board.fromFen(fen);
            for(long move : ExhaustiveOracle.legalMoves(board)) {
                boolean castle = ((move >>> Board.START_PIECE_SHIFT) & Piece.TYPE) == Piece.KING
                        && Math.abs(Move.fromSquare(move) - Move.toSquare(move)) == 2;
                if(!tactical(board, move) && !castle) continue;
                var table = new TTable(1);
                var search = new ExactSearch((b, p) -> -80, table, MODE);
                search.beginRequest();
                table.save(ExactSearchTTableTest.key(board, GameHistory.initial(board)), 2, TTable.TYPE_EXACT, 999, move);
                var result = search.searchWindow(board, GameHistory.initial(board), 1, -100, 50, ExactSearch.NEVER_CANCELLED);
                assertTrue(result.completed()); assertEquals(move, result.bestMove()); assertEquals(80, result.score());
                assertEquals(castle ? move : 0, killers(search)[0]);
                assertEquals(castle ? 1 : 0, Arrays.stream(killers(search)).filter(v -> v != 0).count());
                assertEquals(castle ? 1 : 0, main(search)[QuietHistory.index(move)]);
                search.endRequest();
            }
        }
    }

    @Test void killerClassesPreserveHashTacticalsHistoryTiesAndEveryLegalMoveExactlyOnce() throws Exception {
        var fens = new ArrayList<String>();
        for(var p : ExactSearchHarness.orderingPositions()) fens.add(p.fen());
        fens.addAll(List.of(EP, PROMOTION, CASTLING, CASTLING.replace(" w ", " b "), EVASION));
        boolean reversed = false, tied = false, evasion = false, castleKiller = false;
        for(String fen : fens) {
            long[] board = Board.fromFen(fen);
            evasion |= Board.isPlayerInCheck(board[0], board[1], board[2], board[3], Board.player((int) board[Board.STATUS]));
            var baseline = new ExactSearch(HCE, null, ExactSearch.SEE_MATERIAL_QUIET_HISTORY);
            var candidate = new ExactSearch(HCE, null, MODE);
            long[] generated = ExhaustiveOracle.legalMoves(board);
            for(int i = 0; i < generated.length; i++) {
                int value = i % 3 == 0 ? -QuietHistory.LIMIT : i % 3 == 1 ? QuietHistory.LIMIT : 0;
                main(baseline)[QuietHistory.index(generated[i])] = value;
                main(candidate)[QuietHistory.index(generated[i])] = value;
            }
            List<Long> quiets = ranked(baseline, board, 0, 0).stream().filter(m -> !tactical(board, m)).toList();
            assertTrue(quiets.size() >= 2);
            long primary = quiets.getLast(), secondary = quiets.get(quiets.size() - 2);
            var pairs = new ArrayList<long[]>();
            pairs.add(new long[] {primary, secondary});
            pairs.add(new long[] {0, secondary});
            pairs.add(new long[] {Long.MAX_VALUE, secondary});
            // Same coordinates with a changed full moving-piece identity are not a match.
            pairs.add(new long[] {primary ^ (1L << Board.START_PIECE_SHIFT), secondary});
            for(long move : generated) if(tactical(board, move)) pairs.add(new long[] {move, secondary});
            if(fen.equals(CASTLING) || fen.equals(CASTLING.replace(" w ", " b "))) {
                pairs.add(fen.contains(" w ") ? new long[] {legal(board, "e1g1"), legal(board, "e1c1")}
                        : new long[] {legal(board, "e8g8"), legal(board, "e8c8")});
                castleKiller = true;
            }
            var hashes = new ArrayList<Long>(); hashes.add(0L); hashes.add(Long.MAX_VALUE);
            for(long move : generated) hashes.add(move);
            for(long[] pair : pairs) for(long hash : hashes) {
                int ply = 7;
                killers(candidate)[ply * 2] = pair[0]; killers(candidate)[ply * 2 + 1] = pair[1];
                List<Long> base = ranked(baseline, board, hash, ply);
                var expected = new ArrayList<Long>(); var remaining = new ArrayList<Long>();
                for(long move : base) {
                    if(move == hash || tactical(board, move)) expected.add(move);
                    else remaining.add(move);
                }
                // Independent stable reference classes; main history order already supplied by baseline.
                remaining.sort(Comparator.comparingInt((Long m) -> m == pair[0] ? 0 : m == pair[1] ? 1 : 2));
                expected.addAll(remaining);
                List<Long> actual = ranked(candidate, board, hash, ply);
                assertEquals(expected, actual); assertEquals(actual, ranked(candidate, board, hash, ply));
                assertEquals(generated.length, new HashSet<>(actual).size());
                assertEquals(new HashSet<>(base), new HashSet<>(actual));
                assertEquals(base, ranked(candidate, board, hash, ply + 1), "Killers belong to their ply only");
                if(Arrays.stream(generated).anyMatch(m -> m == hash)) assertEquals(hash, actual.getFirst());
                reversed |= !base.equals(actual);
                for(int i = 1; i < remaining.size(); i++) {
                    long a = remaining.get(i - 1), b = remaining.get(i);
                    if(a != pair[0] && a != pair[1] && b != pair[0] && b != pair[1]
                            && main(candidate)[QuietHistory.index(a)] == main(candidate)[QuietHistory.index(b)]) {
                        tied = true; assertTrue(base.indexOf(a) < base.indexOf(b));
                    }
                }
            }
        }
        assertTrue(reversed && tied && evasion && castleKiller);
    }

    @Test void ownershipAndResetArePerFixedDepthInvocationEvenWithinOneRequest() throws Exception {
        long[] board = Board.startingPosition();
        for(boolean tt : new boolean[] {false, true}) {
            var table = tt ? new TTable(1) : null;
            var first = new ExactSearch(HCE, table, MODE);
            var second = new ExactSearch(HCE, tt ? new TTable(1) : null, MODE);
            assertNotSame(killers(first), killers(second)); assertNotSame(main(first), main(second));
            assertZero(killers(first)); assertZero(killers(second));
            first.beginRequest();
            var a = first.search(board, 3);
            long[] learned = killers(first).clone(); int[] learnedMain = main(first).clone();
            assertTrue(Arrays.stream(learned).anyMatch(v -> v != 0));
            assertEquals(0, learned[0]); assertEquals(0, learned[1]); // Full-window root has no beta cutoff.
            assertTrue(Arrays.stream(learnedMain).anyMatch(v -> v != 0));
            assertZero(killers(second)); assertTrue(Arrays.stream(main(second)).allMatch(v -> v == 0));
            Arrays.fill(killers(first), Long.MAX_VALUE); Arrays.fill(main(first), 73);
            if(table != null) table.clear();
            var b = first.search(board, 3);
            assertEquals(a.score(), b.score()); assertEquals(a.bestMove(), b.bestMove()); assertEquals(a.nodes(), b.nodes());
            assertArrayEquals(a.principalVariation(), b.principalVariation());
            assertArrayEquals(learned, killers(first)); assertArrayEquals(learnedMain, main(first));
            for(int depth : new int[] {0, 3}) {
                Arrays.fill(killers(first), Long.MAX_VALUE); Arrays.fill(main(first), 73);
                first.search(board, GameHistory.initial(board), depth, () -> depth != 0);
                assertZero(killers(first)); assertTrue(Arrays.stream(main(first)).allMatch(v -> v == 0));
            }
            first.endRequest();
        }
    }

    @Test void cutoffLearningUsesAbsolutePlyAndOnlyTheLastSearchedQuietAtThatPly() throws Exception {
        long[] lastMove = new long[4], lastParent = new long[4], learnedParent = new long[4];
        int[] lastEp = new int[4];
        var search = new ExactSearch(new ExactEvaluator() {
            public int evaluate(long[] b, int p) { return Eval.evaluate(b); }
            public void child(long[] parent, long[] child, int ply) {
                lastParent[ply] = parent[Board.KEY];
                lastEp[ply] = ep(parent);
                for(long move : ExhaustiveOracle.legalMoves(parent))
                    if(ExhaustiveOracle.child(parent, move)[Board.KEY] == child[Board.KEY]) { lastMove[ply] = move; return; }
                fail("Missing searched move");
            }
        }, null, MODE);
        long[] killers = killers(search), observed = new long[killers.length];
        var learned = new AtomicBoolean(); var shared = new AtomicBoolean();
        var result = search.search(Board.startingPosition(), GameHistory.initial(Board.startingPosition()), 3, () -> {
            for(int ply = 0; ply < 3; ply++) {
                int base = ply * 2;
                if(observed[base] != 0 && lastParent[ply] != learnedParent[ply]) shared.set(true);
                if(killers[base] != observed[base]) {
                    learned.set(true);
                    assertEquals(lastMove[ply], killers[base]); assertFalse(tactical(lastMove[ply], lastEp[ply]));
                    assertEquals(observed[base], killers[base + 1]);
                    learnedParent[ply] = lastParent[ply];
                    observed[base] = killers[base]; observed[base + 1] = killers[base + 1];
                }
            }
            return false;
        });
        assertTrue(result.completed()); assertTrue(learned.get()); assertTrue(shared.get());
        assertArrayEquals(observed, killers);
    }

    @Test void cancellationBeforeCutoffCannotPublishFalseEvidenceEvenBeforeCleanup() throws Exception {
        long[] board = Board.startingPosition(); var game = GameHistory.initial(board);
        for(boolean tt : new boolean[] {false, true}) for(int depth : new int[] {1, 2}) {
            var stop = new AtomicBoolean(); var table = tt ? new TTable(1) : null;
            var search = new ExactSearch((b, p) -> { stop.set(true); return -80; }, table, MODE);
            long[] killers = killers(search); int[] main = main(search);
            var result = search.searchWindow(board, game, depth, depth == 1 ? -100 : -50, depth == 1 ? 50 : 100, () -> {
                if(stop.get()) { assertZero(killers); assertTrue(Arrays.stream(main).allMatch(v -> v == 0)); }
                return stop.get();
            });
            assertFalse(result.completed()); assertEquals(Value.INVALID, result.score()); assertFalse(result.hasMove());
            assertEquals(0, result.principalVariation().length); assertEquals(depth + 1, result.nodes());
            assertZero(killers); assertTrue(Arrays.stream(main).allMatch(v -> v == 0));
            if(table != null) assertFalse(table.probe(ExactSearchTTableTest.key(board, game), new TTable.TEntry()));
        }
    }

    @Test void laterAbortDiscardsValidEarlierLearningAndInterruptionPreservesIncompleteSemantics() throws Exception {
        long[] board = Board.startingPosition();
        for(boolean tt : new boolean[] {false, true}) {
            var table = tt ? new TTable(1) : null; var search = new ExactSearch(HCE, table, MODE);
            long[] killers = killers(search); int[] main = main(search); var learned = new AtomicBoolean();
            var result = search.search(board, GameHistory.initial(board), 3, () -> {
                boolean any = Arrays.stream(killers).anyMatch(v -> v != 0);
                if(any) { learned.set(true); assertTrue(Arrays.stream(main).anyMatch(v -> v != 0)); }
                return any;
            });
            assertTrue(learned.get()); assertFalse(result.completed()); assertEquals(Value.INVALID, result.score());
            assertFalse(result.hasMove()); assertEquals(0, result.principalVariation().length);
            assertZero(killers); assertTrue(Arrays.stream(main).allMatch(v -> v == 0));
            if(table != null) assertFalse(table.probe(ExactSearchTTableTest.key(board, GameHistory.initial(board)), new TTable.TEntry()));
            Thread.currentThread().interrupt();
            try {
                Arrays.fill(killers, 73); Arrays.fill(main, 73);
                assertFalse(search.search(board, 3).completed()); assertTrue(Thread.currentThread().isInterrupted());
                assertZero(killers); assertTrue(Arrays.stream(main).allMatch(v -> v == 0));
            } finally { Thread.interrupted(); }
        }
    }

    @Test void terminalDrawStaticFullWindowAndTtResolvedNodesDoNotRecordKillers() throws Exception {
        var search = new ExactSearch(HCE, null, MODE);
        for(var p : ExactSearchHarness.positions().subList(4, 6)) {
            search.search(Board.fromFen(p.fen()), 3); assertZero(killers(search));
        }
        for(String fen : List.of("4k3/8/8/8/8/8/P7/4K3 w - - 100 1", "4k3/8/8/8/8/8/8/4K3 w - - 0 1")) {
            assertEquals(0, search.search(Board.fromFen(fen), 3).score()); assertZero(killers(search));
        }
        long[] board = Board.startingPosition();
        search.search(board, 0); assertZero(killers(search));
        search.search(board, 1); assertZero(killers(search));
        for(int type : new int[] {TTable.TYPE_EXACT, TTable.TYPE_LOWER, TTable.TYPE_UPPER}) {
            var table = new TTable(1); var resolved = new ExactSearch(HCE, table, MODE);
            resolved.beginRequest();
            table.save(ExactSearchTTableTest.key(board, GameHistory.initial(board)), 2, type,
                    type == TTable.TYPE_UPPER ? -50 : 50, legal(board, "e2e4"));
            var result = resolved.searchWindow(board, GameHistory.initial(board), 2, -50, 50, ExactSearch.NEVER_CANCELLED);
            assertEquals(1, result.nodes()); assertZero(killers(resolved));
            assertTrue(Arrays.stream(main(resolved)).allMatch(v -> v == 0)); resolved.endRequest();
        }
    }

    @Test void deeperOrdinaryCastlingPromotionAndEvasionSearchesMatchOracleAndMainHistory() {
        for(String fen : List.of("4k3/pp6/8/8/8/8/PP6/4K3 w - - 0 1", CASTLING, EVASION,
                "7k/P7/7p/8/8/8/P7/7K w - - 0 1", "7k/p7/8/8/8/7P/p7/7K b - - 0 1")) {
            long[] board = Board.fromFen(fen); var game = GameHistory.initial(board);
            int expected = new ExhaustiveOracle(HCE).score(board, new SearchLineHistory(game), 3, 0);
            for(boolean tt : new boolean[] {false, true}) {
                var base = new ExactSearch(HCE, tt ? new TTable(1) : null, ExactSearch.SEE_MATERIAL_QUIET_HISTORY).search(board, 3);
                var candidate = new ExactSearch(HCE, tt ? new TTable(1) : null, MODE).search(board, 3);
                assertTrue(candidate.completed()); assertEquals(expected, base.score()); assertEquals(expected, candidate.score());
                ExactSearchTTableTest.assertOptimalAndPv(board, game, 3, candidate, HCE);
            }
        }
    }

    private static int ep(long[] board) { return Board.enPassantSquare((int) board[Board.STATUS]); }
    private static boolean tactical(long[] board, long move) { return tactical(move, ep(board)); }
    private static boolean tactical(long move, int ep) {
        return ((move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                || ((move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                || (((move >>> Board.START_PIECE_SHIFT) & Piece.TYPE) == Piece.PAWN && Move.toSquare(move) == ep);
    }
    private static void assertZero(long[] values) { assertTrue(Arrays.stream(values).allMatch(v -> v == 0)); }
    private static int[] main(ExactSearch search) throws Exception { return (int[]) field(search, "quietHistory"); }
    private static long[] killers(ExactSearch search) throws Exception { return (long[]) field(search, "killers"); }
    private static Object field(ExactSearch search, String name) throws Exception {
        var field = ExactSearch.class.getDeclaredField(name); field.setAccessible(true); return field.get(search);
    }
    private static List<Long> ranked(ExactSearch search, long[] board, long hash, int ply) throws Exception {
        long[] moves = ExhaustiveOracle.legalMoves(board);
        var method = ExactSearch.class.getDeclaredMethod("orderSeeClassified", long[].class, long[].class,
                int.class, int.class, long.class, int.class, int.class, int.class);
        method.setAccessible(true); method.invoke(search, board, moves, moves.length, (int) board[Board.STATUS], hash, -1, ply, -1);
        return Arrays.stream(moves).boxed().toList();
    }
    private static long legal(long[] board, String coordinate) {
        for(long move : ExhaustiveOracle.legalMoves(board)) if(Move.coordinate(move).equals(coordinate)) return move;
        throw new AssertionError("Missing fixture move " + coordinate);
    }
}
