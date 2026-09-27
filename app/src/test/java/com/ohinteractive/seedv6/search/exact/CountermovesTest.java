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
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.tools.search.ExactSearchHarness;

class CountermovesTest {
    private static final int MODE = ExactSearch.SEE_MATERIAL_QUIET_HISTORY_COUNTERMOVE;
    private static final ExactEvaluator HCE = (b, p) -> Eval.evaluate(b);
    private static final String EP = "4k3/8/8/3pP3/3r4/8/8/4K3 w - d6 0 1";
    private static final String PROMOTION = "1r5k/P6p/8/8/8/8/8/7K w - - 0 1";
    private static final String CASTLING = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1";
    private static final String EVASION = "4r1k1/8/8/8/8/8/8/2B1K3 w - - 0 1";

    @Test void contextSeparatesEveryResultingPieceSideAndDestinationWithoutUnusedCodes() {
        int[] pieces = {1, 2, 3, 4, 5, 6, 9, 10, 11, 12, 13, 14};
        boolean[] seen = new boolean[Countermoves.SIZE];
        for(int p = 0; p < pieces.length; p++) for(int to = 0; to < 64; to++) {
            long previous = encoded(pieces[p], to);
            int context = Countermoves.context(previous);
            assertEquals(p * 64 + to, context); assertFalse(seen[context]); seen[context] = true;
            assertEquals(context, Countermoves.context(previous | 31), "Source square is not context");
            assertEquals(context, Countermoves.context(previous | (3L << Board.TARGET_PIECE_SHIFT)), "Victim is not context");
            assertEquals(context * 768, ContinuationHistory.context(previous), "Reuse the proved piece mapping");
        }
        for(boolean used : seen) assertTrue(used);
        assertEquals(768, Countermoves.SIZE); assertEquals(6_144, Countermoves.SIZE * Long.BYTES);
        assertEquals(-1, Countermoves.context(0));
    }

    @Test void contextUsesActualPostMovePieceForQuietCaptureEpCastleAndAllPromotions() {
        boolean quiet = false, tactical = false, castle = false; int promotions = 0;
        for(String fen : List.of(CASTLING, CASTLING.replace(" w ", " b "), EP, PROMOTION,
                "4k3/8/8/3R4/3Pp3/8/8/4K3 b - d3 0 1", "4k3/8/8/8/8/8/P5p1/4K2R b - - 0 1")) {
            long[] board = Board.fromFen(fen);
            for(long move : ExhaustiveOracle.legalMoves(board)) {
                long[] child = ExhaustiveOracle.child(board, move); int to = Move.toSquare(move);
                int resulting = Board.getSquare(child[0], child[1], child[2], child[3], to);
                assertEquals(((resulting & 7) - 1 + (resulting >>> 3) * 6) * 64 + to, Countermoves.context(move));
                if(tactical(board, move)) tactical = true; else quiet = true;
                int promoted = (int) (move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS;
                if(promoted != 0) {
                    promotions++; assertEquals(promoted, resulting);
                    assertNotEquals(Countermoves.context(move & ~((long) Board.PIECE_BITS << Board.PROMOTE_PIECE_SHIFT)),
                            Countermoves.context(move), "Use promoted piece, not the former pawn");
                }
                if((resulting & Piece.TYPE) == Piece.KING && Math.abs(to - Move.fromSquare(move)) == 2) castle = true;
            }
        }
        assertTrue(quiet && tactical && castle); assertEquals(16, promotions);
    }

    @Test void newestQuietReplacesOneContextRepeatedIdenticalIsUnchangedAndTacticalsCannotReplaceIt() {
        long[] board = Board.startingPosition(), table = new long[Countermoves.SIZE]; assertZero(table);
        long a = legal(board, "e2e4"), b = legal(board, "d2d4");
        int first = Countermoves.context(encoded(14, 40)), second = Countermoves.context(encoded(13, 40));
        Countermoves.recordCutoff(table, -1, a, ep(board)); assertZero(table);
        Countermoves.recordCutoff(table, first, a, ep(board)); assertEquals(a, table[first]);
        long[] saved = table.clone();
        Countermoves.recordCutoff(table, first, a, ep(board)); assertArrayEquals(saved, table);
        Countermoves.recordCutoff(table, first, b, ep(board)); assertEquals(b, table[first]);
        Countermoves.recordCutoff(table, second, a, ep(board)); assertEquals(b, table[first]); assertEquals(a, table[second]);
        Countermoves.recordCutoff(table, first, 0, ep(board)); assertEquals(b, table[first]);
        for(String fen : List.of(EP, PROMOTION, CASTLING, CASTLING.replace(" w ", " b "), EVASION,
                "4k3/8/8/8/8/8/P5p1/4K2R b - - 0 1")) {
            long[] position = Board.fromFen(fen);
            for(long move : ExhaustiveOracle.legalMoves(position)) {
                table[first] = b;
                Countermoves.recordCutoff(table, first, move, ep(position));
                assertEquals(tactical(position, move) ? b : move, table[first]);
                assertEquals(a, table[second], "Other contexts remain independent");
                assertEquals(2, Arrays.stream(table).filter(v -> v != 0).count());
            }
        }
    }

    @Test void rootCutoffLearnsMainOnlyEvenWithPreviousRealGameHistory() throws Exception {
        long[] initial = Board.startingPosition(), board = ExhaustiveOracle.child(initial, legal(initial, "e2e4"));
        var game = GameHistory.builder(initial).appendPosition(board).snapshot();
        for(boolean tt : new boolean[] {false, true}) {
            var search = new ExactSearch((b, p) -> -80, tt ? new TTable(1) : null, MODE);
            var result = search.searchWindow(board, game, 1, -100, 50, ExactSearch.NEVER_CANCELLED);
            assertTrue(result.completed()); assertEquals(80, result.score());
            assertEquals(1, main(search)[QuietHistory.index(result.bestMove())]);
            assertEquals(1, Arrays.stream(main(search)).filter(v -> v != 0).count()); assertZero(countermoves(search));
        }
    }

    @Test void actualQuietReplyCutoffsUseImmediateQuietCaptureAndPromotionContextAndOnlySearchedEvidence() throws Exception {
        List<String> fens = List.of(ExactSearchHarness.positions().getFirst().fen(),
                "4k3/pp6/8/3p4/4P3/8/PP6/4K3 w - - 0 1", "7k/P7/7p/8/8/8/P7/7K w - - 0 1",
                "7k/p7/8/8/8/7P/p7/7K b - - 0 1");
        boolean previousQuiet = false, previousTactical = false, previousPromotion = false;
        for(String fen : fens) for(boolean tt : new boolean[] {false, true}) {
            long[] root = Board.fromFen(fen);
            var baseline = new ExactSearch(HCE, null, ExactSearch.SEE_MATERIAL_QUIET_HISTORY);
            long previous = ranked(baseline, root, 0, -1).getFirst();
            previousQuiet |= !tactical(root, previous); previousTactical |= tactical(root, previous);
            previousPromotion |= ((previous >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) != 0;
            long[] board = ExhaustiveOracle.child(root, previous);
            List<Long> order = ranked(baseline, board, 0, -1);
            List<Long> quiets = order.stream().filter(m -> !tactical(board, m)).toList();
            assertTrue(quiets.size() >= 2);
            long winner = quiets.get(1), failed = quiets.getFirst(); int context = Countermoves.context(previous);
            for(long hash : tt ? new long[] {0, winner, failed} : new long[] {0}) {
                var visits = new ArrayList<Long>(); var byKey = new HashMap<Long, Long>();
                for(long move : order) assertNull(byKey.put(ExhaustiveOracle.child(board, move)[Board.KEY], move));
                var table = tt ? new TTable(1) : null;
                var search = new ExactSearch(new ExactEvaluator() {
                    public int evaluate(long[] b, int ply) { return byKey.get(b[Board.KEY]) == winner ? -80 : 0; }
                    public void child(long[] parent, long[] child, int ply) {
                        if(ply == 0) assertEquals(board[Board.KEY], child[Board.KEY]);
                        if(ply == 1) visits.add(byKey.get(child[Board.KEY]));
                    }
                }, table, MODE);
                int[] main = main(search); long[] counters = countermoves(search); var game = GameHistory.initial(root);
                search.beginRequest();
                if(hash != 0) table.save(ExactSearchTTableTest.key(board, GameHistory.builder(game).appendPosition(board).snapshot()),
                        2, TTable.TYPE_EXACT, 999, hash); // Ordering hint only: actual child depth is one.
                var observed = new AtomicBoolean();
                var result = search.searchWindow(root, game, 2, -50, 100, () -> {
                    if(counters[context] == 0) return false;
                    observed.set(true); assertEquals(winner, visits.getLast());
                    if(hash != 0) assertEquals(hash, visits.getFirst());
                    int[] expectedMain = new int[QuietHistory.SIZE];
                    for(long move : visits) if(!tactical(board, move)) expectedMain[QuietHistory.index(move)] += move == winner ? 1 : -1;
                    long[] expected = new long[Countermoves.SIZE]; expected[context] = winner;
                    assertArrayEquals(expectedMain, main); assertArrayEquals(expected, counters);
                    return true; // Valid completed child evidence is inspected before parent abort cleanup.
                });
                search.endRequest();
                assertTrue(observed.get()); assertFalse(result.completed()); assertEquals(Value.INVALID, result.score());
                assertFalse(result.hasMove()); assertEquals(0, result.principalVariation().length);
                assertZero(counters); assertTrue(Arrays.stream(main).allMatch(v -> v == 0));
                if(table != null) assertFalse(table.probe(ExactSearchTTableTest.key(root, game), new TTable.TEntry()));
            }
        }
        assertTrue(previousQuiet && previousTactical && previousPromotion);
    }

    @Test void realTacticalAndPromotionReplyCutoffsDoNotStoreWhileCastlingRepliesDo() throws Exception {
        String[][] fixtures = {
                {"4k3/3p4/8/4P3/3r4/8/8/4K3 b - - 0 1", "d7d5"},
                {"1r2k3/P6p/8/8/8/8/8/7K b - - 0 1", "e8d8"},
                {"r3k2r/p7/8/8/8/8/8/R3K2R b KQkq - 0 1", "a7a6"}
        };
        boolean epReply = false, promotionReply = false, castleReply = false;
        for(String[] fixture : fixtures) {
            long[] root = Board.fromFen(fixture[0]); long previous = legal(root, fixture[1]);
            long[] board = ExhaustiveOracle.child(root, previous); var game = GameHistory.initial(root);
            var childGame = GameHistory.builder(game).appendPosition(board).snapshot();
            for(long reply : ExhaustiveOracle.legalMoves(board)) {
                boolean castle = ((reply >>> Board.START_PIECE_SHIFT) & Piece.TYPE) == Piece.KING
                        && Math.abs(Move.fromSquare(reply) - Move.toSquare(reply)) == 2;
                if(!tactical(board, reply) && !castle) continue;
                epReply |= ((reply >>> Board.START_PIECE_SHIFT) & Piece.TYPE) == Piece.PAWN && Move.toSquare(reply) == ep(board);
                promotionReply |= ((reply >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) != 0;
                castleReply |= castle;
                var table = new TTable(1); var rootVisits = new AtomicInteger(); var leaves = new AtomicInteger();
                var stop = new AtomicBoolean(); var observed = new AtomicBoolean();
                long[][] counters = new long[1][]; int[][] main = new int[1][];
                var search = new ExactSearch(new ExactEvaluator() {
                    public int evaluate(long[] b, int ply) { leaves.incrementAndGet(); return -80; }
                    public void child(long[] parent, long[] child, int ply) {
                        if(ply == 0 && rootVisits.incrementAndGet() == 2) {
                            // The first child has completed its beta cutoff; inspect before cancelling the next child.
                            assertEquals(1, leaves.get());
                            long[] expected = new long[Countermoves.SIZE];
                            if(castle) expected[Countermoves.context(previous)] = reply;
                            assertArrayEquals(expected, counters[0]);
                            assertEquals(castle ? 1 : 0, main[0][QuietHistory.index(reply)]);
                            observed.set(true); stop.set(true);
                        }
                    }
                }, table, MODE);
                counters[0] = countermoves(search); main[0] = main(search);
                search.beginRequest();
                table.save(ExactSearchTTableTest.key(root, game), 3, TTable.TYPE_EXACT, 999, previous);
                table.save(ExactSearchTTableTest.key(board, childGame), 2, TTable.TYPE_EXACT, 999, reply);
                var result = search.searchWindow(root, game, 2, -50, 100, stop::get);
                search.endRequest(); assertTrue(observed.get()); assertFalse(result.completed()); assertZero(counters[0]);
            }
        }
        assertTrue(epReply && promotionReply && castleReply);
    }

    @Test void countermoveClassPreservesHashTacticalsStableHistoryTiesAndUniqueCompleteVisitation() throws Exception {
        var fens = new ArrayList<String>(); for(var p : ExactSearchHarness.orderingPositions()) fens.add(p.fen());
        fens.addAll(List.of(EP, PROMOTION, CASTLING, CASTLING.replace(" w ", " b "), EVASION));
        boolean changed = false, tied = false, evasion = false;
        for(String fen : fens) {
            long[] board = Board.fromFen(fen);
            evasion |= Board.isPlayerInCheck(board[0], board[1], board[2], board[3], Board.player((int) board[Board.STATUS]));
            var baseline = new ExactSearch(HCE, null, ExactSearch.SEE_MATERIAL_QUIET_HISTORY);
            var candidate = new ExactSearch(HCE, null, MODE);
            long[] generated = ExhaustiveOracle.legalMoves(board);
            for(int i = 0; i < generated.length; i++) {
                int value = i % 3 == 0 ? -QuietHistory.LIMIT : i % 3 == 1 ? QuietHistory.LIMIT : 0;
                main(baseline)[QuietHistory.index(generated[i])] = value; main(candidate)[QuietHistory.index(generated[i])] = value;
            }
            List<Long> quiets = ranked(baseline, board, 0, -1).stream().filter(m -> !tactical(board, m)).toList();
            assertTrue(quiets.size() >= 2);
            var replies = new ArrayList<Long>(); replies.add(0L); replies.add(Long.MAX_VALUE);
            replies.add(quiets.getFirst() ^ (1L << Board.START_PIECE_SHIFT)); // Full identity mismatch at same coordinates.
            for(long move : generated) replies.add(move); // Includes tactical poison, ordinary quiets and castles.
            var hashes = new ArrayList<Long>(); hashes.add(0L); hashes.add(Long.MAX_VALUE);
            for(long move : generated) hashes.add(move);
            int context = Countermoves.context(encoded(14, 40));
            for(long reply : replies) for(long hash : hashes) {
                countermoves(candidate)[context] = reply;
                List<Long> base = ranked(baseline, board, hash, -1);
                var expected = new ArrayList<Long>(); var remaining = new ArrayList<Long>();
                for(long move : base) {
                    if(move == hash || tactical(board, move)) expected.add(move); else remaining.add(move);
                }
                remaining.sort(Comparator.comparingInt((Long m) -> m == reply ? 0 : 1));
                expected.addAll(remaining);
                List<Long> actual = ranked(candidate, board, hash, context);
                assertEquals(expected, actual); assertEquals(actual, ranked(candidate, board, hash, context));
                assertEquals(generated.length, actual.size()); assertEquals(generated.length, new HashSet<>(actual).size());
                assertEquals(new HashSet<>(base), new HashSet<>(actual));
                assertEquals(base, ranked(candidate, board, hash, -1), "No root context means baseline order");
                assertEquals(base, ranked(candidate, board, hash, context + 1), "No unrelated context contribution");
                if(Arrays.stream(generated).anyMatch(m -> m == hash)) assertEquals(hash, actual.getFirst());
                changed |= !actual.equals(base);
                for(int i = 1; i < remaining.size(); i++) {
                    long a = remaining.get(i - 1), b = remaining.get(i);
                    if(a != reply && b != reply && main(candidate)[QuietHistory.index(a)] == main(candidate)[QuietHistory.index(b)]) {
                        tied = true; assertTrue(base.indexOf(a) < base.indexOf(b));
                    }
                }
            }
        }
        assertTrue(changed && tied && evasion);
    }

    @Test void ownerIsolationAndDeterministicResetHoldForEveryInvocationIncludingOneRequest() throws Exception {
        long[] board = Board.startingPosition();
        for(boolean tt : new boolean[] {false, true}) {
            var table = tt ? new TTable(1) : null; var first = new ExactSearch(HCE, table, MODE);
            var second = new ExactSearch(HCE, tt ? new TTable(1) : null, MODE);
            assertNotSame(countermoves(first), countermoves(second)); assertNotSame(main(first), main(second));
            assertZero(countermoves(first)); assertZero(countermoves(second));
            assertNull(field(first, "continuationHistory")); assertNull(field(first, "killers")); assertNull(field(first, "captureHistory"));
            first.beginRequest(); var a = first.search(board, 3);
            long[] learned = countermoves(first).clone(); int[] learnedMain = main(first).clone();
            assertTrue(Arrays.stream(learned).anyMatch(v -> v != 0)); assertTrue(Arrays.stream(learnedMain).anyMatch(v -> v != 0));
            assertZero(countermoves(second)); assertTrue(Arrays.stream(main(second)).allMatch(v -> v == 0));
            Arrays.fill(countermoves(first), Long.MAX_VALUE); Arrays.fill(main(first), 73);
            if(table != null) table.clear(); var b = first.search(board, 3);
            assertEquals(a.score(), b.score()); assertEquals(a.bestMove(), b.bestMove()); assertEquals(a.nodes(), b.nodes());
            assertArrayEquals(a.principalVariation(), b.principalVariation());
            assertArrayEquals(learned, countermoves(first)); assertArrayEquals(learnedMain, main(first));
            for(int depth : new int[] {0, 3}) {
                Arrays.fill(countermoves(first), Long.MAX_VALUE); Arrays.fill(main(first), 73);
                first.search(board, GameHistory.initial(board), depth, () -> depth != 0);
                assertZero(countermoves(first)); assertTrue(Arrays.stream(main(first)).allMatch(v -> v == 0));
            }
            first.endRequest();
        }
        for(int mode = ExactSearch.CONTROL; mode < MODE; mode++) assertNull(countermoves(new ExactSearch(HCE, null, mode)));
    }

    @Test void incompleteChildCannotPublishBeforeCleanupAndInterruptionClearsBothTables() throws Exception {
        long[] board = Board.startingPosition(); var game = GameHistory.initial(board);
        for(boolean tt : new boolean[] {false, true}) for(int depth : new int[] {1, 2}) {
            var stop = new AtomicBoolean(); var table = tt ? new TTable(1) : null;
            var search = new ExactSearch((b, p) -> { stop.set(true); return -80; }, table, MODE);
            long[] counters = countermoves(search); int[] main = main(search);
            var result = search.searchWindow(board, game, depth, depth == 1 ? -100 : -50, depth == 1 ? 50 : 100, () -> {
                if(stop.get()) { assertZero(counters); assertTrue(Arrays.stream(main).allMatch(v -> v == 0)); }
                return stop.get();
            });
            assertFalse(result.completed()); assertEquals(Value.INVALID, result.score()); assertFalse(result.hasMove());
            assertEquals(depth + 1, result.nodes()); assertEquals(0, result.principalVariation().length); assertZero(counters);
            if(table != null) assertFalse(table.probe(ExactSearchTTableTest.key(board, game), new TTable.TEntry()));
            Thread.currentThread().interrupt();
            try {
                Arrays.fill(counters, 73); Arrays.fill(main, 73);
                assertFalse(search.search(board, 3).completed()); assertTrue(Thread.currentThread().isInterrupted());
                assertZero(counters); assertTrue(Arrays.stream(main).allMatch(v -> v == 0));
            } finally { Thread.interrupted(); }
        }
    }

    @Test void resolvedTerminalDrawStaticAndTtNodesDoNotLearnCountermoves() throws Exception {
        var search = new ExactSearch(HCE, null, MODE);
        for(var p : ExactSearchHarness.positions().subList(4, 6)) {
            search.search(Board.fromFen(p.fen()), 3); assertZero(countermoves(search));
        }
        for(String fen : List.of("4k3/8/8/8/8/8/P7/4K3 w - - 100 1", "4k3/8/8/8/8/8/8/4K3 w - - 0 1")) {
            assertEquals(0, search.search(Board.fromFen(fen), 3).score()); assertZero(countermoves(search));
        }
        long[] board = Board.startingPosition();
        search.search(board, 0); assertZero(countermoves(search)); search.search(board, 1); assertZero(countermoves(search));
        for(int type : new int[] {TTable.TYPE_EXACT, TTable.TYPE_LOWER, TTable.TYPE_UPPER}) {
            var table = new TTable(1); var resolved = new ExactSearch(HCE, table, MODE); resolved.beginRequest();
            table.save(ExactSearchTTableTest.key(board, GameHistory.initial(board)), 2, type,
                    type == TTable.TYPE_UPPER ? -50 : 50, legal(board, "e2e4"));
            var result = resolved.searchWindow(board, GameHistory.initial(board), 2, -50, 50, ExactSearch.NEVER_CANCELLED);
            assertEquals(1, result.nodes()); assertZero(countermoves(resolved));
            assertTrue(Arrays.stream(main(resolved)).allMatch(v -> v == 0)); resolved.endRequest();
        }
    }

    @Test void deeperOrdinaryCastlingPromotionAndEvasionSearchesMatchExhaustiveAndMainValuesAndPvs() {
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

    private static long encoded(int piece, int to) { return ((long) piece << Board.START_PIECE_SHIFT) | ((long) to << Board.TARGET_SQUARE_SHIFT); }
    private static int ep(long[] board) { return Board.enPassantSquare((int) board[Board.STATUS]); }
    private static boolean tactical(long[] board, long move) {
        return ((move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                || ((move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                || (((move >>> Board.START_PIECE_SHIFT) & Piece.TYPE) == Piece.PAWN && Move.toSquare(move) == ep(board));
    }
    private static void assertZero(long[] values) { assertTrue(Arrays.stream(values).allMatch(v -> v == 0)); }
    private static int[] main(ExactSearch search) throws Exception { return (int[]) field(search, "quietHistory"); }
    private static long[] countermoves(ExactSearch search) throws Exception { return (long[]) field(search, "countermoves"); }
    private static Object field(ExactSearch search, String name) throws Exception {
        var field = ExactSearch.class.getDeclaredField(name); field.setAccessible(true); return field.get(search);
    }
    private static List<Long> ranked(ExactSearch search, long[] board, long hash, int context) throws Exception {
        long[] moves = ExhaustiveOracle.legalMoves(board);
        var method = ExactSearch.class.getDeclaredMethod("orderSeeClassified", long[].class, long[].class,
                int.class, int.class, long.class, int.class, int.class, int.class);
        method.setAccessible(true); method.invoke(search, board, moves, moves.length, (int) board[Board.STATUS], hash, -1, 0, context);
        return Arrays.stream(moves).boxed().toList();
    }
    private static long legal(long[] board, String coordinate) {
        for(long move : ExhaustiveOracle.legalMoves(board)) if(Move.coordinate(move).equals(coordinate)) return move;
        throw new AssertionError("Missing fixture move " + coordinate);
    }
}
