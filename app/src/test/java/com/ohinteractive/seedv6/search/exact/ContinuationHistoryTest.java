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

class ContinuationHistoryTest {
    private static final int MODE = ExactSearch.SEE_MATERIAL_CONTINUATION_HISTORY;
    private static final ExactEvaluator HCE = (b, p) -> Eval.evaluate(b);
    private static final String PROMOTION = "1r5k/P6p/8/8/8/8/8/7K w - - 0 1";
    private static final String EP = "4k3/8/8/3pP3/3r4/8/8/4K3 w - d6 0 1";
    private static final String CASTLING = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1";

    @Test void compactIndexIsBijectiveAcrossEveryPieceSideAndDestinationDimension() {
        int[] pieces = {1, 2, 3, 4, 5, 6, 9, 10, 11, 12, 13, 14};
        boolean[] seen = new boolean[ContinuationHistory.SIZE];
        int count = 0;
        for(int p = 0; p < 12; p++) {
            assertEquals(p, ContinuationHistory.compactPiece(pieces[p]));
            for(int previousTo = 0; previousTo < 64; previousTo++) {
                int context = ContinuationHistory.context(encoded(pieces[p], previousTo));
                assertEquals((p * 64 + previousTo) * 768, context);
                for(int c = 0; c < 12; c++) for(int to = 0; to < 64; to++) {
                    long move = encoded(pieces[c], to);
                    int index = ContinuationHistory.index(context, move);
                    assertEquals(((p * 64 + previousTo) * 12 + c) * 64 + to, index);
                    assertFalse(seen[index]); seen[index] = true; count++;
                    assertEquals(index, ContinuationHistory.index(context, move | 31), "Current source is omitted");
                    assertEquals(context, ContinuationHistory.context(encoded(pieces[p], previousTo) | 17), "Previous source is omitted");
                }
            }
        }
        assertEquals(589_824, count);
        assertEquals(1_179_648, ContinuationHistory.SIZE * Short.BYTES);
        assertEquals(-1, ContinuationHistory.context(0));
    }

    @Test void contextMatchesActualDestinationPieceAfterQuietCaptureEpCastlingAndEveryPromotion() {
        boolean quiet = false, tactical = false, castle = false;
        int promotions = 0;
        for(String fen : List.of(CASTLING, CASTLING.replace(" w ", " b "), EP,
                "4k3/8/8/3R4/3Pp3/8/8/4K3 b - d3 0 1", PROMOTION,
                "4k3/8/8/8/8/8/6p1/4K2R b - - 0 1")) {
            long[] board = Board.fromFen(fen);
            for(long move : ExhaustiveOracle.legalMoves(board)) {
                long[] child = ExhaustiveOracle.child(board, move);
                int to = Move.toSquare(move);
                int resulting = Board.getSquare(child[0], child[1], child[2], child[3], to);
                int expected = ((resulting & 7) - 1 + (resulting >>> 3) * 6) * 64 + to;
                assertEquals(expected * 768, ContinuationHistory.context(move));
                if(tactical(board, move)) tactical = true; else quiet = true;
                int promoted = (int) (move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS;
                if(promoted != 0) {
                    promotions++;
                    assertEquals(promoted, resulting);
                    assertNotEquals(ContinuationHistory.context(move & ~((long) Board.PIECE_BITS << Board.PROMOTE_PIECE_SHIFT)),
                            ContinuationHistory.context(move), "Promotion context cannot use the pawn");
                }
                if((resulting & Piece.TYPE) == Piece.KING && Math.abs(to - Move.fromSquare(move)) == 2) castle = true;
            }
        }
        assertTrue(quiet && tactical && castle); assertEquals(16, promotions);
    }

    @Test void signedShortGravityUsesIntArithmeticAndPreservesBoundsUnderRepeatedEvidence() {
        short[] table = new short[ContinuationHistory.SIZE]; assertZero(table);
        ContinuationHistory.update(table, 0, 4096); assertEquals(4096, table[0]);
        ContinuationHistory.update(table, 0, 4096); assertEquals(7168, table[0]);
        ContinuationHistory.update(table, 0, -4096); assertEquals(1280, table[0]);
        ContinuationHistory.update(table, 1, -4096); assertEquals(-4096, table[1]);
        ContinuationHistory.update(table, 1, -4096); assertEquals(-7168, table[1]);
        ContinuationHistory.update(table, 1, 4096); assertEquals(-1280, table[1]);
        ContinuationHistory.update(table, 0, Integer.MAX_VALUE); assertEquals(16384, table[0]);
        ContinuationHistory.update(table, 0, Integer.MIN_VALUE); assertEquals(-16384, table[0]);
        for(int sign : new int[] {1, -1}) {
            for(int i = 0; i < 100_000; i++) {
                long old = table[0], bonus = sign * 9;
                long expected = old + bonus - old * Math.abs(bonus) / 16384;
                ContinuationHistory.update(table, 0, (int) bonus);
                assertEquals(expected, table[0]); assertTrue(Math.abs(table[0]) <= 16384);
            }
            assertEquals(sign * 16384, table[0]);
        }
    }

    @Test void cutoffMechanicsExcludeRootTacticalsAndUnsearchedMovesAndUseTheDepthCap() {
        long[] board = Board.fromFen(PROMOTION);
        long loser = legal(board, "h1g1"), winner = legal(board, "h1g2"), unsearched = legal(board, "h1h2");
        long[] moves = {loser, legal(board, "a7a8q"), legal(board, "a7b8n"), winner, unsearched};
        int context = ContinuationHistory.context(encoded(14, 42));
        for(int depth : new int[] {1, 3, 64, 256}) {
            short[] table = new short[ContinuationHistory.SIZE];
            ContinuationHistory.recordCutoff(table, -1, moves, 3, ep(board), depth); assertZero(table);
            ContinuationHistory.recordCutoff(table, context, moves, 3, ep(board), depth);
            int bonus = depth == 1 ? 1 : depth == 3 ? 9 : 4096;
            assertEquals(bonus, table[ContinuationHistory.index(context, winner)]);
            assertEquals(-bonus, table[ContinuationHistory.index(context, loser)]);
            assertEquals(0, table[ContinuationHistory.index(context, unsearched)]);
            assertEquals(0, table[ContinuationHistory.index(context, moves[1])]);
            assertEquals(0, table[ContinuationHistory.index(context, moves[2])]);
            assertEquals(2, nonzero(table));
            for(int cutoff : new int[] {1, 2}) {
                short[] before = table.clone();
                ContinuationHistory.recordCutoff(table, context, moves, cutoff, ep(board), depth);
                assertArrayEquals(before, table);
            }
        }
        for(String fen : List.of(EP, PROMOTION, CASTLING)) {
            long[] b = Board.fromFen(fen);
            for(long move : ExhaustiveOracle.legalMoves(b)) {
                short[] table = new short[ContinuationHistory.SIZE];
                ContinuationHistory.recordCutoff(table, context, new long[] {move}, 0, ep(b), 2);
                assertEquals(tactical(b, move) ? 0 : 4, table[ContinuationHistory.index(context, move)]);
            }
        }
    }

    @Test void rootLearnsMainOnlyEvenWhenGameHistoryContainsAPreviousMove() throws Exception {
        long[] initial = Board.startingPosition();
        long[] board = ExhaustiveOracle.child(initial, legal(initial, "e2e4"));
        var game = GameHistory.builder(initial).appendPosition(board).snapshot();
        for(boolean tt : new boolean[] {false, true}) {
            var search = new ExactSearch((b, p) -> -80, tt ? new TTable(1) : null, MODE);
            var result = search.searchWindow(board, game, 1, -100, 50, ExactSearch.NEVER_CANCELLED);
            assertTrue(result.completed()); assertEquals(80, result.score());
            assertEquals(1, main(search)[QuietHistory.index(result.bestMove())]);
            assertEquals(1, Arrays.stream(main(search)).filter(v -> v != 0).count());
            assertZero(continuation(search));
        }
    }

    @Test void actualChildCutoffsUseImmediateQuietCaptureAndPromotionContextWithHashRewardAndMalus() throws Exception {
        List<String> fens = List.of(ExactSearchHarness.positions().getFirst().fen(),
                "4k3/pp6/8/3p4/4P3/8/PP6/4K3 w - - 0 1",
                "7k/P7/7p/8/8/8/P7/7K w - - 0 1",
                "7k/p7/8/8/8/7P/p7/7K b - - 0 1");
        for(String fen : fens) for(boolean tt : new boolean[] {false, true}) {
            long[] root = Board.fromFen(fen);
            var baseline = new ExactSearch(HCE, null, ExactSearch.SEE_MATERIAL_QUIET_HISTORY);
            long previous = ranked(baseline, root, 0, -1).getFirst();
            long[] board = ExhaustiveOracle.child(root, previous);
            List<Long> order = ranked(baseline, board, 0, -1);
            List<Long> quiets = order.stream().filter(m -> !tactical(board, m)).toList();
            assertTrue(quiets.size() >= 2, fen);
            long winner = quiets.get(1), failed = quiets.getFirst();
            int context = ContinuationHistory.context(previous);
            for(long hash : tt ? new long[] {0, winner, failed} : new long[] {0}) {
                var visits = new ArrayList<Long>();
                var byKey = new HashMap<Long, Long>();
                for(long move : order) assertNull(byKey.put(ExhaustiveOracle.child(board, move)[Board.KEY], move));
                var table = tt ? new TTable(1) : null;
                var search = new ExactSearch(new ExactEvaluator() {
                    public int evaluate(long[] b, int ply) { return byKey.get(b[Board.KEY]) == winner ? -80 : 0; }
                    public void child(long[] parent, long[] child, int ply) {
                        if(ply == 0) assertEquals(board[Board.KEY], child[Board.KEY]);
                        if(ply == 1) visits.add(byKey.get(child[Board.KEY]));
                    }
                }, table, MODE);
                int[] main = main(search); short[] continuation = continuation(search);
                var game = GameHistory.initial(root);
                search.beginRequest();
                if(hash != 0) {
                    var childGame = GameHistory.builder(game).appendPosition(board).snapshot();
                    table.save(ExactSearchTTableTest.key(board, childGame), 2, TTable.TYPE_EXACT, 999, hash);
                }
                var observed = new AtomicBoolean();
                var result = search.searchWindow(root, game, 2, -50, 100, () -> {
                    if(continuation[ContinuationHistory.index(context, winner)] == 0) return false;
                    observed.set(true);
                    assertEquals(winner, visits.getLast());
                    if(hash != 0) assertEquals(hash, visits.getFirst());
                    int[] expectedMain = new int[QuietHistory.SIZE];
                    short[] expectedContinuation = new short[ContinuationHistory.SIZE];
                    for(long move : visits) if(!tactical(board, move)) {
                        int bonus = move == winner ? 1 : -1;
                        expectedMain[QuietHistory.index(move)] += bonus;
                        expectedContinuation[ContinuationHistory.index(context, move)] += bonus;
                    }
                    assertArrayEquals(expectedMain, main);
                    assertArrayEquals(expectedContinuation, continuation, "Only this immediate context and searched quiet prefix learn");
                    return true; // The completed child is valid evidence; abort the parent before another child.
                });
                search.endRequest();
                assertTrue(observed.get()); assertFalse(result.completed());
                assertEquals(Value.INVALID, result.score()); assertFalse(result.hasMove());
                assertTrue(Arrays.stream(main).allMatch(v -> v == 0)); assertZero(continuation);
                if(table != null) assertFalse(table.probe(ExactSearchTTableTest.key(root, game), new TTable.TEntry()));
            }
        }
    }

    @Test void combinedScoreRanksQuietsWithStableSumTiesAndUnchangedTacticalsAndHash() throws Exception {
        var fens = new ArrayList<String>();
        for(var p : ExactSearchHarness.orderingPositions()) fens.add(p.fen());
        fens.addAll(List.of(EP, PROMOTION, CASTLING, CASTLING.replace(" w ", " b ")));
        boolean changed = false, tied = false, evasion = false;
        for(String fen : fens) {
            long[] board = Board.fromFen(fen);
            evasion |= Board.isPlayerInCheck(board[0], board[1], board[2], board[3], Board.player((int) board[Board.STATUS]));
            var search = new ExactSearch(HCE, null, MODE);
            var baseline = new ExactSearch(HCE, null, ExactSearch.SEE_MATERIAL_QUIET_HISTORY);
            int context = ContinuationHistory.context(encoded(Board.player((int) board[Board.STATUS]) == 0 ? 14 : 6, 28));
            int[] main = main(search); short[] cont = continuation(search);
            long[] generated = ExhaustiveOracle.legalMoves(board);
            for(int i = 0; i < generated.length; i++) {
                long move = generated[i];
                main[QuietHistory.index(move)] = i % 2 == 0 ? 500 : -500;
                // Opposing components sum to zero; some get large positive/negative sums.
                cont[ContinuationHistory.index(context, move)] = (short) switch(i % 4) {
                    case 0 -> -500;
                    case 1 -> 500;
                    case 2 -> 16384;
                    default -> -16384;
                };
            }
            System.arraycopy(main, 0, main(baseline), 0, main.length);
            var hashes = new ArrayList<Long>(); hashes.add(0L); hashes.add(Long.MAX_VALUE);
            for(long move : generated) hashes.add(move);
            for(long hash : hashes) for(int ctx : new int[] {-1, context}) {
                List<Long> base = ranked(baseline, board, hash, -1);
                List<Long> actual = ranked(search, board, hash, ctx);
                var expected = new ArrayList<Long>(); var quiets = new ArrayList<Long>();
                for(long move : base) if(move == hash || tactical(board, move)) expected.add(move);
                // Stable ties refer to the original generated order, not a prior main-history ranking.
                for(long move : generated) if(move != hash && !tactical(board, move)) quiets.add(move);
                quiets.sort(Comparator.comparingInt((Long m) -> main[QuietHistory.index(m)]
                        + (ctx < 0 ? 0 : cont[ContinuationHistory.index(ctx, m)])).reversed());
                expected.addAll(quiets);
                assertEquals(expected, actual);
                assertEquals(generated.length, actual.size()); assertEquals(generated.length, new HashSet<>(actual).size());
                assertEquals(new HashSet<>(base), new HashSet<>(actual));
                if(Arrays.stream(generated).anyMatch(m -> m == hash)) assertEquals(hash, actual.getFirst());
                if(ctx < 0) assertEquals(base, actual, "No context uses main history alone");
                else if(hash == 0) {
                    changed |= !actual.equals(base);
                    for(int i = 0; i < quiets.size(); i++) for(int j = i + 1; j < quiets.size(); j++) {
                        long a = quiets.get(i), b = quiets.get(j);
                        int sa = main[QuietHistory.index(a)] + cont[ContinuationHistory.index(ctx, a)];
                        int sb = main[QuietHistory.index(b)] + cont[ContinuationHistory.index(ctx, b)];
                        if(sa == sb && main[QuietHistory.index(a)] != main[QuietHistory.index(b)]) tied = true;
                    }
                }
            }
        }
        assertTrue(changed && tied && evasion);
    }

    @Test void bothHistoriesResetPerInvocationAndRemainOwnerIsolatedInsideARequest() throws Exception {
        long[] board = Board.startingPosition();
        for(boolean tt : new boolean[] {false, true}) {
            var table = tt ? new TTable(1) : null;
            var first = new ExactSearch(HCE, table, MODE);
            var second = new ExactSearch(HCE, tt ? new TTable(1) : null, MODE);
            assertNotSame(main(first), main(second)); assertNotSame(continuation(first), continuation(second));
            assertZero(continuation(first)); assertZero(continuation(second));
            first.beginRequest();
            var a = first.search(board, 3);
            int[] learnedMain = main(first).clone(); short[] learnedContinuation = continuation(first).clone();
            assertTrue(Arrays.stream(learnedMain).anyMatch(v -> v != 0)); assertTrue(nonzero(learnedContinuation) > 0);
            assertTrue(Arrays.stream(main(second)).allMatch(v -> v == 0)); assertZero(continuation(second));
            Arrays.fill(main(first), 73); Arrays.fill(continuation(first), (short) 73);
            if(table != null) table.clear();
            var b = first.search(board, 3);
            assertEquals(a.score(), b.score()); assertEquals(a.bestMove(), b.bestMove()); assertEquals(a.nodes(), b.nodes());
            assertArrayEquals(a.principalVariation(), b.principalVariation());
            assertArrayEquals(learnedMain, main(first)); assertArrayEquals(learnedContinuation, continuation(first));
            for(int depth : new int[] {0, 3}) {
                Arrays.fill(main(first), 73); Arrays.fill(continuation(first), (short) 73);
                first.search(board, GameHistory.initial(board), depth, () -> depth != 0);
                assertTrue(Arrays.stream(main(first)).allMatch(v -> v == 0)); assertZero(continuation(first));
            }
            first.endRequest();
        }
        for(int mode = ExactSearch.CONTROL; mode <= ExactSearch.SEE_MATERIAL_QUIET_HISTORY; mode++)
            assertNull(continuation(new ExactSearch(HCE, null, mode)));
    }

    @Test void cancelledChildCannotUpdateEitherHistoryBeforeAbortCleanup() throws Exception {
        long[] board = Board.startingPosition();
        for(boolean tt : new boolean[] {false, true}) {
            var stop = new AtomicBoolean();
            var search = new ExactSearch((b, p) -> { stop.set(true); return -80; }, tt ? new TTable(1) : null, MODE);
            int[] main = main(search); short[] continuation = continuation(search);
            var result = search.searchWindow(board, GameHistory.initial(board), 2, -50, 100, () -> {
                if(stop.get()) { assertTrue(Arrays.stream(main).allMatch(v -> v == 0)); assertZero(continuation); }
                return stop.get();
            });
            assertFalse(result.completed()); assertEquals(3, result.nodes()); assertEquals(0, result.principalVariation().length);
            assertTrue(Arrays.stream(main).allMatch(v -> v == 0)); assertZero(continuation);
            Thread.currentThread().interrupt();
            try {
                Arrays.fill(main, 73); Arrays.fill(continuation, (short) 73);
                assertFalse(search.search(board, 3).completed());
                assertTrue(Thread.currentThread().isInterrupted());
                assertTrue(Arrays.stream(main).allMatch(v -> v == 0)); assertZero(continuation);
            } finally { Thread.interrupted(); }
        }
    }

    @Test void promotionPathAndCastlingEvasionsMatchDeeperExhaustiveValuesAndOptimalPvs() {
        for(String fen : List.of("7k/P7/7p/8/8/8/P7/7K w - - 0 1",
                "7k/p7/8/8/8/7P/p7/7K b - - 0 1", CASTLING,
                "4r1k1/8/8/8/8/8/8/2B1K3 w - - 0 1")) {
            long[] board = Board.fromFen(fen); var game = GameHistory.initial(board);
            int expected = new ExhaustiveOracle(HCE).score(board, new SearchLineHistory(game), 3, 0);
            for(boolean tt : new boolean[] {false, true}) {
                var result = new ExactSearch(HCE, tt ? new TTable(1) : null, MODE).search(board, 3);
                assertEquals(expected, result.score()); ExactSearchTTableTest.assertOptimalAndPv(board, game, 3, result, HCE);
            }
        }
    }

    private static long encoded(int piece, int to) { return ((long) piece << Board.START_PIECE_SHIFT) | ((long) to << Board.TARGET_SQUARE_SHIFT); }
    private static int ep(long[] board) { return Board.enPassantSquare((int) board[Board.STATUS]); }
    private static boolean tactical(long[] board, long move) { return CaptureHistory.isTactical(move, ep(board)); }
    private static int nonzero(short[] table) { int count = 0; for(short value : table) if(value != 0) count++; return count; }
    private static void assertZero(short[] table) { assertEquals(0, nonzero(table)); }
    private static int[] main(ExactSearch search) throws Exception { return (int[]) field(search, "quietHistory"); }
    private static short[] continuation(ExactSearch search) throws Exception { return (short[]) field(search, "continuationHistory"); }
    private static Object field(ExactSearch search, String name) throws Exception {
        var field = ExactSearch.class.getDeclaredField(name); field.setAccessible(true); return field.get(search);
    }
    private static List<Long> ranked(ExactSearch search, long[] board, long hash, int context) throws Exception {
        long[] moves = ExhaustiveOracle.legalMoves(board);
        var method = ExactSearch.class.getDeclaredMethod("orderSeeClassified", long[].class, long[].class,
                int.class, int.class, long.class, int.class, int.class);
        method.setAccessible(true); method.invoke(search, board, moves, moves.length, (int) board[Board.STATUS], hash, context, 0);
        return Arrays.stream(moves).boxed().toList();
    }
    private static long legal(long[] board, String coordinate) {
        for(long move : ExhaustiveOracle.legalMoves(board)) if(Move.coordinate(move).equals(coordinate)) return move;
        throw new AssertionError("Missing fixture move " + coordinate);
    }
}
