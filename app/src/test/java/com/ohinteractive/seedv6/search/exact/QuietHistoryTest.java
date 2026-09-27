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
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.tools.search.ExactSearchHarness;

class QuietHistoryTest {
    private static final int MODE = ExactSearch.SEE_MATERIAL_QUIET_HISTORY;
    private static final ExactEvaluator HCE = (b, p) -> Eval.evaluate(b);
    private static final String EP = "4k3/8/8/3pP3/3r4/8/8/4K3 w - d6 0 1";
    private static final String PROMOTION = "1r5k/P7/8/8/8/8/8/7K w - - 0 1";
    private static final String CASTLING = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1";

    @Test void directIndexSeparatesEverySidePieceFromAndToTuple() {
        assertEquals(3, Board.PLAYER_SHIFT);
        assertEquals(15, Board.PIECE_BITS);
        assertEquals(1, Piece.KING); assertEquals(6, Piece.PAWN);
        boolean[] seen = new boolean[QuietHistory.SIZE];
        int count = 0;
        for(int moving : new int[] {1, 2, 3, 4, 5, 6, 9, 10, 11, 12, 13, 14})
            for(int from = 0; from < 64; from++) for(int to = 0; to < 64; to++) {
                long move = ((long) moving << Board.START_PIECE_SHIFT)
                        | from | ((long) to << Board.TARGET_SQUARE_SHIFT);
                int index = QuietHistory.index(move);
                assertEquals((moving * 64 + from) * 64 + to, index);
                assertTrue(index >= 0 && index < QuietHistory.SIZE);
                assertFalse(seen[index]);
                seen[index] = true;
                count++;
            }
        assertEquals(49_152, count);
        assertEquals(262_144, QuietHistory.SIZE * Integer.BYTES);
    }

    @Test void zeroStateSignedGravityBoundsRepeatedUpdatesAndReversal() {
        int[] table = new int[QuietHistory.SIZE];
        assertZero(table);
        QuietHistory.update(table, 0, 4096); assertEquals(4096, table[0]);
        QuietHistory.update(table, 0, 4096); assertEquals(7168, table[0]);
        QuietHistory.update(table, 0, -4096); assertEquals(1280, table[0]);
        QuietHistory.update(table, 1, -4096); assertEquals(-4096, table[1]);
        QuietHistory.update(table, 1, -4096); assertEquals(-7168, table[1]);
        QuietHistory.update(table, 1, 4096); assertEquals(-1280, table[1]);
        QuietHistory.update(table, 0, Integer.MAX_VALUE); assertEquals(QuietHistory.LIMIT, table[0]);
        QuietHistory.update(table, 0, Integer.MIN_VALUE); assertEquals(-QuietHistory.LIMIT, table[0]);
        for(int sign : new int[] {1, -1}) {
            for(int i = 0; i < 100_000; i++) {
                long h = table[0], bonus = sign * 9;
                long expected = h + bonus - h * Math.abs(bonus) / 16_384;
                QuietHistory.update(table, 0, (int) bonus);
                assertEquals(expected, table[0]);
                assertTrue(Math.abs(table[0]) <= QuietHistory.LIMIT);
            }
            assertEquals(sign * QuietHistory.LIMIT, table[0]);
            QuietHistory.update(table, 0, sign);
            assertEquals(sign * QuietHistory.LIMIT, table[0]);
        }
    }

    @Test void onlyQuietWinnerAndSearchedQuietPrefixLearnWithCappedDepthSquared() {
        long[] board = Board.fromFen(PROMOTION);
        long loser = legal(board, "h1g1"), winner = legal(board, "h1g2"), unsearched = legal(board, "h1h2");
        long promotion = legal(board, "a7a8q"), capturePromotion = legal(board, "a7b8n");
        long[] moves = {loser, promotion, capturePromotion, winner, unsearched};
        for(int depth : new int[] {1, 3, 64, 256}) {
            int[] table = new int[QuietHistory.SIZE];
            QuietHistory.recordCutoff(table, moves, 3, ep(board), depth);
            int bonus = depth == 1 ? 1 : depth == 3 ? 9 : 4096;
            assertEquals(bonus, table[QuietHistory.index(winner)]);
            assertEquals(-bonus, table[QuietHistory.index(loser)]);
            assertEquals(0, table[QuietHistory.index(unsearched)]);
            assertEquals(0, table[QuietHistory.index(promotion)]);
            assertEquals(0, table[QuietHistory.index(capturePromotion)]);
            assertEquals(2, Arrays.stream(table).filter(v -> v != 0).count());
            for(int cutoff : new int[] {1, 2}) {
                int[] before = table.clone();
                QuietHistory.recordCutoff(table, moves, cutoff, ep(board), depth);
                assertArrayEquals(before, table, "A tactical cutoff cannot reward or penalize quiet history");
            }
        }
    }

    @Test void allCaptureEpAndPromotionKindsAreExcludedWhileCastlingIsQuiet() {
        for(String fen : List.of(EP, PROMOTION, "4k3/8/8/3R4/3Pp3/8/8/4K3 b - d3 0 1",
                "4k3/8/8/8/8/8/6p1/4K2R b - - 0 1", CASTLING, CASTLING.replace(" w ", " b "))) {
            long[] board = Board.fromFen(fen);
            for(long move : ExhaustiveOracle.legalMoves(board)) {
                int[] table = new int[QuietHistory.SIZE];
                boolean tactical = tactical(board, move);
                QuietHistory.recordCutoff(table, new long[] {move}, 0, ep(board), 2);
                assertEquals(tactical ? 0 : 4, table[QuietHistory.index(move)], Move.coordinate(move));
                assertEquals(tactical ? 0 : 1, Arrays.stream(table).filter(v -> v != 0).count());
            }
            if(fen.startsWith("r3k2r")) for(String coordinate : fen.contains(" w ")
                    ? List.of("e1g1", "e1c1") : List.of("e8g8", "e8c8"))
                assertFalse(tactical(board, legal(board, coordinate)));
        }
    }

    @Test void realCutoffsRewardQuietHashAndPenalizeOnlyActuallySearchedQuiets() throws Exception {
        long[] board = Board.fromFen(ExactSearchHarness.orderingPositions().get(1).fen());
        List<Long> order = ranked(new ExactSearch(HCE, null, ExactSearch.SEE_MATERIAL), board, 0);
        List<Long> quiets = order.stream().filter(m -> !tactical(board, m)).toList();
        long failedQuiet = quiets.getFirst(), winner = quiets.get(1);
        long tacticalHash = order.stream().filter(m -> tactical(board, m)).findFirst().orElseThrow();
        for(long hash : new long[] {0, failedQuiet, winner, tacticalHash}) {
            var visits = new ArrayList<Long>();
            var byKey = new HashMap<Long, Long>();
            for(long move : order) assertNull(byKey.put(ExhaustiveOracle.child(board, move)[Board.KEY], move));
            var table = hash == 0 ? null : new TTable(1);
            var search = new ExactSearch(new ExactEvaluator() {
                public int evaluate(long[] b, int p) { return byKey.get(b[Board.KEY]) == winner ? -80 : 0; }
                public void child(long[] parent, long[] child, int ply) { visits.add(byKey.get(child[Board.KEY])); }
            }, table, MODE);
            search.beginRequest();
            if(table != null) table.save(ExactSearchTTableTest.key(board, GameHistory.initial(board)),
                    2, TTable.TYPE_EXACT, 999, hash); // Depth mismatch supplies ordering only.
            var result = search.searchWindow(board, GameHistory.initial(board), 1, -100, 50, ExactSearch.NEVER_CANCELLED);
            search.endRequest();
            assertTrue(result.completed()); assertEquals(80, result.score()); assertEquals(winner, result.bestMove());
            assertEquals(winner, visits.getLast());
            if(hash != 0) assertEquals(hash, visits.getFirst());
            int[] expected = new int[QuietHistory.SIZE];
            for(long move : visits) if(!tactical(board, move)) expected[QuietHistory.index(move)] = move == winner ? 1 : -1;
            assertArrayEquals(expected, history(search), "No generated-but-unsearched move or tactical may learn");
        }
        // Actual tactical hash cutoffs (including EP and quiet/capture underpromotions) never learn.
        for(String fen : List.of(EP, PROMOTION.replace("P7", "P6p"))) {
            long[] b = Board.fromFen(fen);
            for(long move : ExhaustiveOracle.legalMoves(b)) if(tactical(b, move)) {
                var table = new TTable(1);
                var search = new ExactSearch((child, ply) -> -80, table, MODE);
                search.beginRequest();
                table.save(ExactSearchTTableTest.key(b, GameHistory.initial(b)), 2, TTable.TYPE_EXACT, 999, move);
                var result = search.searchWindow(b, GameHistory.initial(b), 1, -100, 50, ExactSearch.NEVER_CANCELLED);
                assertEquals(move, result.bestMove()); assertEquals(80, result.score());
                assertZero(history(search));
                search.endRequest();
            }
        }
    }

    @Test void learnedQuietRankingPreservesTacticalsStableTiesHashAndUniqueMoveSet() throws Exception {
        var fens = new ArrayList<String>();
        for(var position : ExactSearchHarness.orderingPositions()) fens.add(position.fen());
        fens.addAll(List.of(EP, PROMOTION, CASTLING, CASTLING.replace(" w ", " b "),
                "4k3/8/8/8/8/8/6p1/4K2R b - - 0 1"));
        boolean reversed = false, tied = false, evasion = false;
        for(String fen : fens) {
            long[] board = Board.fromFen(fen);
            evasion |= Board.isPlayerInCheck(board[0], board[1], board[2], board[3], Board.player((int) board[Board.STATUS]));
            var baseline = new ExactSearch(HCE, null, ExactSearch.SEE_MATERIAL);
            var candidate = new ExactSearch(HCE, null, MODE);
            int[] history = history(candidate);
            long[] generated = ExhaustiveOracle.legalMoves(board);
            for(int i = 0; i < generated.length; i++) {
                // Even poisoned tactical keys cannot affect their ordering.
                history[QuietHistory.index(generated[i])] = switch(i % 3) {
                    case 0 -> -QuietHistory.LIMIT;
                    case 1 -> QuietHistory.LIMIT;
                    default -> 0;
                };
            }
            var hashes = new ArrayList<Long>(); hashes.add(0L); hashes.add(Long.MAX_VALUE);
            for(long move : generated) hashes.add(move);
            for(long hash : hashes) {
                List<Long> base = ranked(baseline, board, hash);
                var expected = new ArrayList<Long>(); var quiets = new ArrayList<Long>();
                boolean legalHash = Arrays.stream(generated).anyMatch(m -> m == hash);
                for(long move : base) {
                    if(move == hash || tactical(board, move)) expected.add(move);
                    else quiets.add(move);
                }
                quiets.sort(Comparator.comparingInt((Long m) -> history[QuietHistory.index(m)]).reversed());
                expected.addAll(quiets);
                List<Long> actual = ranked(candidate, board, hash);
                assertEquals(expected, actual);
                assertEquals(actual, ranked(candidate, board, hash));
                assertEquals(generated.length, actual.size());
                assertEquals(new HashSet<>(base), new HashSet<>(actual));
                assertEquals(generated.length, new HashSet<>(actual).size());
                if(legalHash) assertEquals(hash, actual.getFirst());
                if(hash == 0) for(int i = 0; i < generated.length; i++) for(int j = i + 1; j < generated.length; j++) {
                    long a = generated[i], b = generated[j];
                    if(tactical(board, a) || tactical(board, b)) continue;
                    int ha = history[QuietHistory.index(a)], hb = history[QuietHistory.index(b)];
                    if(ha < hb) { reversed = true; assertTrue(actual.indexOf(b) < actual.indexOf(a)); }
                    if(ha == hb) { tied = true; assertTrue(actual.indexOf(a) < actual.indexOf(b)); }
                }
            }
        }
        assertTrue(reversed && tied && evasion);
    }

    @Test void historyIsOwnerLocalAndResetsEveryFixedDepthInvocationIncludingWithinOneRequest() throws Exception {
        long[] board = Board.startingPosition();
        for(boolean tt : new boolean[] {false, true}) {
            var table = tt ? new TTable(1) : null;
            var first = new ExactSearch(HCE, table, MODE);
            var second = new ExactSearch(HCE, tt ? new TTable(1) : null, MODE);
            assertNotSame(history(first), history(second)); assertZero(history(first)); assertZero(history(second));
            first.beginRequest();
            var a = first.search(board, 3);
            int[] learned = history(first).clone();
            assertTrue(Arrays.stream(learned).anyMatch(v -> v > 0));
            assertZero(history(second));
            Arrays.fill(history(first), 73);
            if(table != null) table.clear();
            var b = first.search(board, 3);
            assertEquals(a.score(), b.score()); assertEquals(a.bestMove(), b.bestMove()); assertEquals(a.nodes(), b.nodes());
            assertArrayEquals(a.principalVariation(), b.principalVariation());
            assertArrayEquals(learned, history(first), "No iterative-deepening cross-invocation history reuse");
            Arrays.fill(history(first), 73);
            assertFalse(first.search(board, GameHistory.initial(board), 3, () -> true).completed());
            assertZero(history(first));
            Arrays.fill(history(first), 73);
            first.search(board, 0); assertZero(history(first));
            first.endRequest();
        }
        for(int mode = ExactSearch.CONTROL; mode <= ExactSearch.SEE_MATERIAL_CAPTURE_HISTORY; mode++)
            assertNull(history(new ExactSearch(HCE, null, mode)), "Old modes have no quiet-history allocation");
    }

    @Test void cancellationBeforeCutoffPublicationCannotRewardAnIncompleteChild() throws Exception {
        long[] board = Board.startingPosition();
        var game = GameHistory.initial(board);
        for(boolean tt : new boolean[] {false, true}) {
            var stop = new AtomicBoolean();
            var table = tt ? new TTable(1) : null;
            var search = new ExactSearch((b, p) -> { stop.set(true); return -80; }, table, MODE);
            int[] history = history(search);
            var result = search.searchWindow(board, game, 1, -100, 50, () -> {
                if(stop.get()) assertZero(history); // Before abort unwinds/clears: no invalid update can hide here.
                return stop.get();
            });
            assertFalse(result.completed()); assertEquals(Value.INVALID, result.score()); assertFalse(result.hasMove());
            assertEquals(0, result.principalVariation().length); assertEquals(2, result.nodes());
            assertZero(history);
            if(table != null) assertFalse(table.probe(ExactSearchTTableTest.key(board, game), new TTable.TEntry()));
        }
    }

    @Test void laterCancellationDiscardsAlreadyLearnedHistoryAndThreadInterruptionIsIncomplete() throws Exception {
        long[] board = Board.startingPosition();
        for(boolean tt : new boolean[] {false, true}) {
            var table = tt ? new TTable(1) : null;
            var search = new ExactSearch(HCE, table, MODE);
            int[] history = history(search);
            var observedLearning = new AtomicBoolean();
            var result = search.search(board, GameHistory.initial(board), 3, () -> {
                boolean learned = Arrays.stream(history).anyMatch(v -> v != 0);
                if(learned) observedLearning.set(true);
                return learned;
            });
            assertTrue(observedLearning.get()); assertFalse(result.completed()); assertZero(history);
            if(table != null) assertFalse(table.probe(ExactSearchTTableTest.key(board, GameHistory.initial(board)), new TTable.TEntry()));
            Thread.currentThread().interrupt();
            try {
                Arrays.fill(history, 73);
                assertFalse(search.search(board, 3).completed()); assertZero(history);
                assertTrue(Thread.currentThread().isInterrupted());
            } finally { Thread.interrupted(); }
        }
    }

    @Test void terminalDrawStaticFullWindowAndTtResolvedNodesDoNotLearn() throws Exception {
        var search = new ExactSearch(HCE, null, MODE);
        for(var position : ExactSearchHarness.positions().subList(4, 6)) {
            search.search(Board.fromFen(position.fen()), 3); assertZero(history(search));
        }
        for(String fen : List.of("4k3/8/8/8/8/8/P7/4K3 w - - 100 1", "4k3/8/8/8/8/8/8/4K3 w - - 0 1")) {
            assertEquals(0, search.search(Board.fromFen(fen), 3).score()); assertZero(history(search));
        }
        long[] board = Board.startingPosition();
        search.search(board, 0); assertZero(history(search));
        search.search(board, 1); assertZero(history(search)); // Searched quiets without beta cutoff do not learn.
        for(int type : new int[] {TTable.TYPE_EXACT, TTable.TYPE_LOWER, TTable.TYPE_UPPER}) {
            var table = new TTable(1);
            var resolved = new ExactSearch(HCE, table, MODE);
            resolved.beginRequest();
            table.save(ExactSearchTTableTest.key(board, GameHistory.initial(board)), 2, type,
                    type == TTable.TYPE_UPPER ? -50 : 50, legal(board, "e2e4"));
            var result = resolved.searchWindow(board, GameHistory.initial(board), 2, -50, 50, ExactSearch.NEVER_CANCELLED);
            assertEquals(1, result.nodes()); assertZero(history(resolved)); resolved.endRequest();
        }
    }

    private static int[] history(ExactSearch search) throws Exception {
        var field = ExactSearch.class.getDeclaredField("quietHistory"); field.setAccessible(true);
        return (int[]) field.get(search);
    }

    private static List<Long> ranked(ExactSearch search, long[] board, long hash) throws Exception {
        long[] moves = ExhaustiveOracle.legalMoves(board);
        var method = ExactSearch.class.getDeclaredMethod("orderSeeClassified", long[].class, long[].class,
                int.class, int.class, long.class, int.class, int.class);
        method.setAccessible(true);
        method.invoke(search, board, moves, moves.length, (int) board[Board.STATUS], hash, -1, 0);
        return Arrays.stream(moves).boxed().toList();
    }

    private static void assertZero(int[] table) { assertTrue(Arrays.stream(table).allMatch(v -> v == 0)); }
    private static int ep(long[] board) { return Board.enPassantSquare((int) board[Board.STATUS]); }

    private static boolean tactical(long[] board, long move) {
        return ((move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                || ((move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) != 0
                || (((move >>> Board.START_PIECE_SHIFT) & Piece.TYPE) == Piece.PAWN && Move.toSquare(move) == ep(board));
    }

    private static long legal(long[] board, String coordinate) {
        for(long move : ExhaustiveOracle.legalMoves(board)) if(Move.coordinate(move).equals(coordinate)) return move;
        throw new AssertionError("Missing legal fixture move " + coordinate);
    }
}
