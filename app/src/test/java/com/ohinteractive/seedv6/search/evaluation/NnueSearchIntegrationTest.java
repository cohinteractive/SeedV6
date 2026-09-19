package com.ohinteractive.seedv6.search.evaluation;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.nnue.NnueEvaluator;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch;
import com.ohinteractive.seedv6.search.alphabeta.RootParallelSearch;
import com.ohinteractive.seedv6.search.alphabeta.SelectiveSearchPolicy;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.common.SingleDepthSearch;
import com.ohinteractive.seedv6.search.iterative.IterativeDeepeningSearch;
import com.ohinteractive.seedv6.search.iterative.IterativeSearchOutcome;
import com.ohinteractive.seedv6.search.manage.ManagedSearchResult;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.search.manage.SearchLimits;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

import static org.junit.jupiter.api.Assertions.*;

class NnueSearchIntegrationTest {
    private static final NnueNetwork NETWORK = NnueNetwork.initialized(73);
    private static final NnueScoreMapping MAPPING = NnueScoreMapping.V1;
    private static final SearchEvaluation INCREMENTAL = SearchEvaluation.incremental(NETWORK, MAPPING);
    private static final SearchEvaluation REFERENCE = SearchEvaluation.fullRecompute(NETWORK, MAPPING);
    private static final String[] POSITIONS = {
        Board.FEN_STARTING_POSITION,
        "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
        "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2",
        "7k/P7/8/8/8/8/8/7K w - - 0 1",
        "1r5k/P7/8/8/8/8/8/7K w - - 0 1",
        "4r1k1/8/8/8/8/8/8/2B1K3 w - - 0 1",
        "8/8/4k3/3r4/8/1B6/8/3RK3 w - - 0 1",
        "7k/6p1/5KQ1/8/8/8/8/8 w - - 0 1",
        "7k/6Q1/5K2/8/8/8/8/8 b - - 100 1",
        "7k/5Q2/6K1/8/8/8/8/8 b - - 0 1"
    };

    @Test
    void deterministicSingleWorkerMatchesFullRebuildIncludingNodesPvAndDiagnostics() {
        Set<Integer> scores = new HashSet<>();
        for (String fen : POSITIONS) {
            long[] board = Board.fromFen(fen);
            for (int depth : new int[] {0, 1, 3}) {
                SearchResult reference = direct(new AlphaBetaPvsSearch(REFERENCE), board, depth);
                SearchResult actual = direct(new AlphaBetaPvsSearch(INCREMENTAL), board, depth);
                assertEquals(reference, actual, fen + " depth=" + depth);
                assertTrue(actual.completed());
                legalPv(board, actual);
                scores.add(actual.score());
            }
        }
        assertTrue(scores.size() > 5, "Exercise score variation, not a constant network.");
    }

    @Test
    void searchedRootMovesIncludeKingMovesCastlingEnPassantAndAllPromotionChoices() {
        assertRootMoves(POSITIONS[1], "e1f1", "e1g1", "e1c1");
        assertRootMoves(POSITIONS[2], "e5d6");
        assertRootMoves(POSITIONS[3], "a7a8q", "a7a8r", "a7a8b", "a7a8n");
        assertRootMoves(POSITIONS[4], "a7b8q", "a7b8r", "a7b8b", "a7b8n");
        assertRootMoves("4k3/p7/8/8/8/8/3n4/4K3 w - - 0 1", "e1d2");
    }

    @Test
    void rootReportingUsesMappedNetworkAndNoHandcraftedFallback() {
        for (int threads : new int[] {1, 2, 4}) {
            for (SearchEvaluation definition : new SearchEvaluation[] {INCREMENTAL, REFERENCE}) {
                long[] board = Board.fromFen(POSITIONS[4]);
                NnueEvaluator oracle = new NnueEvaluator(NETWORK);
                oracle.evaluate(board);
                int expected = MAPPING.map(oracle.boundedValue());
                assertNotEquals(Eval.evaluate(board), expected);
                AtomicInteger reported = new AtomicInteger(Integer.MIN_VALUE);
                try (RootParallelSearch search = new RootParallelSearch(threads, definition)) {
                    search.search(request(board, GameHistory.initial(board), 1, new SearchObserver() {
                        @Override public void onSearchStarted(int depth, int score, int count) {
                            reported.set(score);
                        }
                    }, SearchControl.unlimited()));
                }
                assertEquals(expected, reported.get());
            }
        }
        // A quiet depth-zero root must be exactly the NNUE stand-pat, not a blend.
        long[] board = Board.startingPosition();
        NnueEvaluator oracle = new NnueEvaluator(NETWORK);
        oracle.evaluate(board);
        assertEquals(MAPPING.map(oracle.boundedValue()),
                direct(new AlphaBetaPvsSearch(INCREMENTAL), board, 0).score());
    }

    @Test
    void iterativePolicyIsFullWindowAndMateOnlyWhileHandcraftedDefaultsAreUnchanged() {
        assertEquals(SelectiveSearchPolicy.production(), SearchEvaluation.handcrafted().selectiveSearchPolicy());
        assertTrue(new AlphaBetaPvsSearch().usesAspiration());
        assertEquals(SelectiveSearchPolicy.only(SelectiveSearchPolicy.Heuristic.MATE_DISTANCE),
                INCREMENTAL.selectiveSearchPolicy());
        assertFalse(new AlphaBetaPvsSearch(INCREMENTAL).usesAspiration());
        for (String fen : new String[] {POSITIONS[0], POSITIONS[2], POSITIONS[4], POSITIONS[7]}) {
            long[] board = Board.fromFen(fen);
            IterativeSearchOutcome oracle = iterative(new AlphaBetaPvsSearch(REFERENCE), board, 3);
            IterativeSearchOutcome actual = iterative(new AlphaBetaPvsSearch(INCREMENTAL), board, 3);
            assertEquals(oracle, actual, fen);
            assertEquals(0, actual.diagnostics().iteration().aspirationAttempts());
            assertEquals(0, actual.diagnostics().iteration().fullWindowFallbacks());
            assertEquals(0, actual.diagnostics().worker().selective().razorAttempts());
            assertEquals(0, actual.diagnostics().worker().selective().futilityEligibleNodes());
        }
    }

    @Test
    void multipleWorkersAgreeOnScoresTerminalMeaningAndLegalPv() {
        for (String fen : POSITIONS) {
            long[] board = Board.fromFen(fen);
            SearchResult single = direct(new AlphaBetaPvsSearch(REFERENCE), board, 3);
            for (int threads : new int[] {1, 2, 4}) {
                try (RootParallelSearch reference = new RootParallelSearch(threads, REFERENCE);
                     RootParallelSearch incremental = new RootParallelSearch(threads, INCREMENTAL)) {
                    SearchResult expected = direct(reference, board, 3);
                    SearchResult actual = direct(incremental, board, 3);
                    sameMeaning(single, actual);
                    sameMeaning(expected, actual);
                    assertEquals(expected.bestMove(), actual.bestMove(), fen + " threads=" + threads);
                    if (threads == 1) assertEquals(expected, actual);
                    legalPv(board, expected);
                    legalPv(board, actual);
                }
            }
        }
    }

    @Test
    void terminalAndRuleDrawAdjudicationDoesNotDependOnNetwork() {
        String[] fens = {POSITIONS[8], POSITIONS[9],
                "4k3/8/8/8/8/8/8/R3K3 w - - 100 1",
                "4k3/8/8/8/8/8/8/4K3 w - - 0 1"};
        for (int i = 0; i < fens.length; i++) {
            long[] board = Board.fromFen(fens[i]);
            for (int depth : new int[] {0, 3}) {
                SearchResult result = direct(new AlphaBetaPvsSearch(INCREMENTAL), board, depth);
                assertEquals(i == 0 ? -TranspositionScores.MATE_SCORE : 0, result.score());
                assertEquals(i >= 2, result.hasMove());
            }
        }
        long[] board = Board.startingPosition();
        GameHistory.Builder history = GameHistory.builder(board);
        for (int cycle = 0; cycle < 2; cycle++) {
            for (String move : new String[] {"g1f3", "g8f6", "f3g1", "f6g8"}) {
                board = play(board, find(board, move));
                history.appendPosition(board);
            }
        }
        GameHistory repeated = history.snapshot();
        assertTrue(repeated.isFormalThreefold(board));
        for (int threads : new int[] {1, 2, 4}) {
            try (RootParallelSearch search = new RootParallelSearch(threads, INCREMENTAL)) {
                SearchResult result = search.search(request(board, repeated, 3, SearchObserver.NONE,
                        SearchControl.unlimited()));
                assertTrue(result.completed());
                assertEquals(0, result.score());
            }
        }
    }

    @Test
    void fixedNetworkReusesItsTableAndDifferentNetworkStartsWithUncontaminatedScores() {
        long[] board = Board.startingPosition();
        NnueNetwork different = NnueNetwork.initialized(20260918);
        SearchEvaluation replacement = SearchEvaluation.incremental(different, MAPPING);
        AlphaBetaPvsSearch first = new AlphaBetaPvsSearch(INCREMENTAL);
        SearchResult cold = direct(first, board, 3);
        SearchResult warm = direct(first, board, 3);
        sameMeaning(cold, warm);
        assertEquals(cold.bestMove(), warm.bestMove());
        assertTrue(warm.diagnostics().worker().transpositionTable().usableBoundCutoffs() > 0);
        legalPv(board, warm);
        SearchResult fresh = direct(new AlphaBetaPvsSearch(replacement), board, 3);
        SearchResult oracle = direct(new AlphaBetaPvsSearch(SearchEvaluation.fullRecompute(different, MAPPING)), board, 3);
        assertEquals(oracle, fresh);
        assertNotEquals(cold.score(), fresh.score(), "Distinct networks exercise distinct TT scores.");
        // The original service's definition and TT remain usable after another is constructed.
        first.newGame();
        assertEquals(cold, direct(first, board, 3));
        // Root-parallel public construction has the same private-table lifetime rule.
        try (RootParallelSearch oldSearch = new RootParallelSearch(2, INCREMENTAL);
             RootParallelSearch newSearch = new RootParallelSearch(2, replacement);
             RootParallelSearch newOracle = new RootParallelSearch(2,
                     SearchEvaluation.fullRecompute(different, MAPPING))) {
            direct(oldSearch, board, 3);
            SearchResult expected = direct(newOracle, board, 3);
            SearchResult actual = direct(newSearch, board, 3);
            sameMeaning(expected, actual);
            assertEquals(expected.bestMove(), actual.bestMove());
            legalPv(board, actual);
        }
    }

    @Test
    void cancellationAndObserverFailureLeaveReusableWorkerStateSafe() {
        long[] board = Board.startingPosition();
        for (int threads : new int[] {1, 2, 4}) {
            try (RootParallelSearch search = new RootParallelSearch(threads, INCREMENTAL)) {
                SearchControl limited = SearchControl.controlled(40, 0, -1, () -> 0);
                SearchResult cancelled = search.search(request(board, GameHistory.initial(board), 6,
                        SearchObserver.NONE, limited));
                assertFalse(cancelled.completed());
                assertEquals(SearchTermination.NODE_LIMIT, limited.termination());
                assertEquals(40, limited.nodes());
                SearchResult reused = direct(search, board, 3);
                sameMeaning(direct(new AlphaBetaPvsSearch(REFERENCE), board, 3), reused);
                legalPv(board, reused);
                assertThrows(IllegalStateException.class, () -> search.search(request(board,
                        GameHistory.initial(board), 3, new SearchObserver() {
                            @Override public void onRootMoveStarted(int index, int total, long move) {
                                throw new IllegalStateException("observer test failure");
                            }
                        }, SearchControl.unlimited())));
                search.newGame();
                SearchResult recovered = direct(search, board, 3);
                sameMeaning(reused, recovered);
                legalPv(board, recovered);
            }
        }
    }

    @Test
    void explicitNnueLifecycleCanStopAndRunAgain() throws Exception {
        long[] board = Board.startingPosition();
        try (SearchLifecycleService service = new SearchLifecycleService(2, INCREMENTAL)) {
            CountDownLatch stopped = new CountDownLatch(1);
            AtomicReference<ManagedSearchResult> cancelled = new AtomicReference<>();
            service.start(board, GameHistory.initial(board), new SearchLimits(8, -1, -1, false),
                    new SearchObserver() {
                        @Override public void onIterationCompleted(com.ohinteractive.seedv6.search.common.IterationSnapshot snapshot) {
                            service.stop();
                        }
                    }, true, result -> { cancelled.set(result); stopped.countDown(); });
            assertTrue(stopped.await(10, TimeUnit.SECONDS));
            assertEquals(SearchTermination.STOPPED, cancelled.get().termination());
            CountDownLatch done = new CountDownLatch(1);
            AtomicReference<ManagedSearchResult> completed = new AtomicReference<>();
            service.start(board, GameHistory.initial(board), new SearchLimits(3, -1, -1, false),
                    result -> { completed.set(result); done.countDown(); });
            assertTrue(done.await(10, TimeUnit.SECONDS));
            assertEquals(SearchTermination.COMPLETED, completed.get().termination());
            assertNull(service.lastFailure());
            sameMeaning(iterative(new AlphaBetaPvsSearch(REFERENCE), board, 3).lastCompletedResult(),
                    completed.get().lastCompletedResult());
            legalPv(board, completed.get().lastCompletedResult());
        }
    }

    private static void assertRootMoves(String fen, String... required) {
        long[] board = Board.fromFen(fen);
        Set<String> searched = new HashSet<>();
        SearchResult actual = new AlphaBetaPvsSearch(INCREMENTAL).search(request(board,
                GameHistory.initial(board), 1, new SearchObserver() {
                    @Override public void onRootMoveFinished(int index, int total, long move, int score,
                            boolean best, long nodes, long nanos) { searched.add(Move.coordinate(move)); }
                }, SearchControl.unlimited()));
        assertEquals(direct(new AlphaBetaPvsSearch(REFERENCE), board, 1), actual);
        for (String move : required) assertTrue(searched.contains(move), move);
    }

    private static SearchRequest request(long[] board, GameHistory history, int depth,
                                         SearchObserver observer, SearchControl control) {
        return new SearchRequest(board, history, depth, observer, control, true);
    }

    private static SearchResult direct(SingleDepthSearch search, long[] board, int depth) {
        return search.search(request(board, GameHistory.initial(board), depth,
                SearchObserver.NONE, SearchControl.unlimited()));
    }

    private static IterativeSearchOutcome iterative(SingleDepthSearch search, long[] board, int depth) {
        return new IterativeDeepeningSearch(search).search(request(board, GameHistory.initial(board),
                depth, SearchObserver.NONE, SearchControl.unlimited()));
    }

    private static void sameMeaning(SearchResult expected, SearchResult actual) {
        assertEquals(expected.completed(), actual.completed());
        assertEquals(expected.score(), actual.score());
        assertEquals(expected.hasMove(), actual.hasMove());
        assertEquals(expected.legalRootMoves(), actual.legalRootMoves());
    }

    private static void legalPv(long[] root, SearchResult result) {
        long[] board = root;
        if (result.hasMove()) assertEquals(result.bestMove(), find(board, Move.coordinate(result.bestMove())));
        for (long move : result.principalVariation()) {
            assertEquals(move, find(board, Move.coordinate(move)));
            board = play(board, move);
        }
    }

    private static long find(long[] board, String coordinate) {
        long[] moves = new long[256];
        int count = Gen.genAll(board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], true, moves, new long[Board.MAX_BITBOARDS]);
        for (int i = 0; i < count; i++) if (Move.coordinate(moves[i]).equals(coordinate)) return moves[i];
        throw new AssertionError("Illegal move: " + coordinate);
    }

    private static long[] play(long[] board, long move) {
        long[] child = new long[Board.MAX_BITBOARDS];
        Board.makeMoveInto(board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], move, child);
        return child;
    }
}
