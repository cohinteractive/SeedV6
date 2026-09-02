package com.ohinteractive.seedv6.search.alphabeta;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.iterative.IterativeDeepeningSearch;
import com.ohinteractive.seedv6.search.manage.ManagedSearchResult;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.search.manage.SearchLimits;
import com.ohinteractive.seedv6.search.tt.TranspositionTable;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Bound;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Probe;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RootParallelSearchTest {

    @Test
    void workerWidthIsExplicitAndHardBounded() {
        assertThrows(IllegalArgumentException.class, () -> new RootParallelSearch(0));
        assertThrows(IllegalArgumentException.class, () -> new RootParallelSearch(17));
        try(RootParallelSearch one = new RootParallelSearch(1);
            RootParallelSearch two = new RootParallelSearch(2);
            RootParallelSearch representative = new RootParallelSearch(4)) {
            assertEquals(1, one.workerCount());
            assertEquals(2, two.workerCount());
            assertEquals(4, representative.workerCount());
        }
    }

    @Test
    void threadsOneIsTheAcceptedWs13PathIncludingNodesPvAndDiagnostics() {
        for(String fen : List.of(
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            "r3k2r/p1ppqpb1/bn2pnp1/2pP4/1p2P3/2N2N2/PPQBBPPP/R3K2R w KQkq - 0 1",
            "7k/6p1/5KQ1/8/8/8/8/8 w - - 0 1"
        )) {
            final long[] board = Board.fromFen(fen);
            final SearchResult expected = iterative(
                new AlphaBetaPvsSearch(new TranspositionTable(1 << 14)), board, 3, true
            );
            try(RootParallelSearch parallel = new RootParallelSearch(
                new TranspositionTable(1 << 14), 1
            )) {
                final SearchResult actual = iterative(parallel, board, 3, true);
                assertEquals(expected, actual, fen);
            }
        }
    }

    @Test
    void exactIndexedRootCoverageHandlesMoreFewerAndOneMove() {
        assertCoverage(Board.startingPosition(), 4);
        final long[] oneLegalMove = Board.fromFen(
            "7k/8/5K2/6Q1/8/8/8/8 b - - 0 1"
        );
        assertEquals(1, legalMoves(oneLegalMove).length);
        assertCoverage(oneLegalMove, 4);
    }

    @Test
    void threadsTwoAndFourAgreeOnScoreTerminalMeaningAndLegalPv() {
        for(String fen : List.of(
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            "4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1",
            "7k/6p1/5KQ1/8/8/8/8/8 w - - 0 1",
            "7k/p7/5KQ1/8/8/8/8/8 b - - 0 1",
            "7k/5Q2/6K1/8/8/8/8/8 b - - 0 1"
        )) {
            final long[] board = Board.fromFen(fen);
            final SearchResult oracle = iterative(new AlphaBetaPvsSearch(), board, 3, false);
            for(int threads : List.of(2, 4)) {
                try(RootParallelSearch parallel = new RootParallelSearch(
                    new TranspositionTable(1 << 15), threads
                )) {
                    final SearchResult actual = iterative(parallel, board, 3, false);
                    assertEquals(oracle.score(), actual.score(), fen + " threads=" + threads);
                    assertEquals(oracle.hasMove(), actual.hasMove(), fen);
                    assertEquals(oracle.legalRootMoves(), actual.legalRootMoves(), fen);
                    assertLegalPv(board, actual);
                }
            }
        }
    }

    @Test
    void equalScoreReductionIsStableAcrossRepeatedSchedulingVariation() {
        final long[] board = Board.fromFen("4k3/8/8/8/8/8/8/3QK3 w - - 0 1");
        long selected = 0L;
        for(int run = 0; run < 24; run ++) {
            final int delayIndex = run % 4;
            final RootParallelSearch.RootWorkerHook hook = (index, move) -> {
                if((index & 3) == delayIndex) Thread.yield();
            };
            try(RootParallelSearch parallel = new RootParallelSearch(
                new TranspositionTable(1 << 10), SelectiveSearchPolicy.production(), 4, hook
            )) {
                final SearchResult result = direct(parallel, board, 1, false, -1L);
                assertTrue(result.completed());
                assertLegalPv(board, result);
                if(run == 0) selected = result.bestMove();
                else assertEquals(selected, result.bestMove());
            }
        }
        assertNotEquals(0L, selected);

        final AlphaBetaPvsSearch.RootChildResult[] tied = {
            new AlphaBetaPvsSearch.RootChildResult(30L, 12, 4L, true, false, new long[] {30L}),
            new AlphaBetaPvsSearch.RootChildResult(10L, 12, 2L, true, false, new long[] {10L}),
            new AlphaBetaPvsSearch.RootChildResult(20L, 11, 1L, true, false, new long[] {20L})
        };
        assertEquals(0, RootParallelSearch.selectBestIndex(tied, tied.length));
    }

    @Test
    void concurrentNodeAdmissionIsExactAndNeverPublishesPartialAttempt() {
        final long[] board = Board.startingPosition();
        try(RootParallelSearch parallel = new RootParallelSearch(4)) {
            final SearchControl control = SearchControl.controlled(3L, 0L, -1L, () -> 0L);
            final SearchResult result = parallel.search(new SearchRequest(
                board, GameHistory.initial(board), 3, SearchObserver.NONE, control
            ));
            assertEquals(3L, control.nodes());
            assertEquals(SearchTermination.NODE_LIMIT, control.termination());
            assertFalse(result.completed());
            assertFalse(result.hasMove());
            assertEquals(3L, result.nodes());
        }

        final AtomicInteger launches = new AtomicInteger();
        try(RootParallelSearch parallel = new RootParallelSearch(
            new TranspositionTable(1 << 10), SelectiveSearchPolicy.production(), 4,
            (index, move) -> launches.incrementAndGet()
        )) {
            final SearchControl stopped = SearchControl.controlled(-1L, 0L, -1L, () -> 0L);
            stopped.request(SearchTermination.STOPPED);
            final SearchResult result = parallel.search(new SearchRequest(
                board, GameHistory.initial(board), 3, SearchObserver.NONE, stopped
            ));
            assertFalse(result.completed());
            assertEquals(0, launches.get());
        }
    }

    @Test
    void diagnosticsMergeExactlyOnceAndRemainResultNeutral() {
        final long[] board = Board.startingPosition();
        final SearchResult enabled;
        try(RootParallelSearch parallel = new RootParallelSearch(4)) {
            enabled = direct(parallel, board, 2, true, -1L);
        }
        final SearchResult disabled;
        try(RootParallelSearch parallel = new RootParallelSearch(4)) {
            disabled = direct(parallel, board, 2, false, -1L);
        }
        assertTrue(enabled.diagnostics().enabled());
        assertEquals(enabled.nodes(), enabled.diagnostics().totalEnteredNodes());
        assertEquals(disabled.score(), enabled.score());
        assertEquals(disabled.bestMove(), enabled.bestMove());
        assertArrayEquals(disabled.principalVariation(), enabled.principalVariation());
    }

    @Test
    void workerFailureUnwindsAttemptAndReusablePoolSearchesAgain() {
        final AtomicBoolean failOnce = new AtomicBoolean(true);
        final RuntimeException expected = new RuntimeException("injected root worker failure");
        final RootParallelSearch.RootWorkerHook hook = (index, move) -> {
            if(index == 2 && failOnce.compareAndSet(true, false)) throw expected;
        };
        final long[] board = Board.startingPosition();
        try(RootParallelSearch parallel = new RootParallelSearch(
            new TranspositionTable(1 << 12), SelectiveSearchPolicy.production(), 4, hook
        )) {
            final SearchControl failed = SearchControl.controlled(-1L, 0L, -1L, () -> 0L);
            assertEquals(expected, assertThrows(RuntimeException.class, () -> parallel.search(
                new SearchRequest(
                    board, GameHistory.initial(board), 3, SearchObserver.NONE, failed
                )
            )));
            assertEquals(SearchTermination.FAILURE, failed.termination());

            final SearchResult recovered = direct(parallel, board, 2, false, -1L);
            assertTrue(recovered.completed());
            assertTrue(recovered.hasMove());
            assertLegalPv(board, recovered);
        }
    }

    @Test
    void tinySharedTableSurvivesCollisionHeavyRepeatedSearches() {
        final long[] board = Board.startingPosition();
        try(RootParallelSearch parallel = new RootParallelSearch(
            new TranspositionTable(1), 4
        )) {
            for(int run = 0; run < 20; run ++) {
                final SearchResult result = direct(parallel, board, 3, false, -1L);
                assertTrue(result.completed());
                assertLegalPv(board, result);
            }
        }
    }

    @Test
    void completedRootWindowStoresItsBoundButCancelledAttemptNeverDoes() {
        final long[] board = Board.fromFen(
            "4k3/8/8/8/8/8/4P3/3QK3 w - - 0 1"
        );
        final int depth = 2;
        final int exactScore = new AlphaBetaPvsSearch(
            new TranspositionTable(1 << 10)
        ).search(new SearchRequest(board, depth)).score();
        final TranspositionTable completedTable = new TranspositionTable(1 << 10);
        try(RootParallelSearch parallel = new RootParallelSearch(completedTable, 4)) {
            parallel.beginTopLevelSearch();
            try {
                final SearchResult result = parallel.searchWindow(
                    new SearchRequest(board, depth), exactScore - 1, exactScore
                );
                assertTrue(result.completed());
                assertTrue(result.score() >= exactScore);
            } finally {
                parallel.endTopLevelSearch();
            }
        }
        final Probe completedProbe = new Probe();
        completedTable.probe(
            board[Board.KEY], depth, -TranspositionScores.MATE_SCORE - 1,
            TranspositionScores.MATE_SCORE + 1, 0, completedProbe
        );
        assertTrue(completedProbe.keyMatches());
        assertEquals(Bound.LOWER, completedProbe.bound());

        final TranspositionTable cancelledTable = new TranspositionTable(1 << 10);
        try(RootParallelSearch parallel = new RootParallelSearch(cancelledTable, 4)) {
            final SearchControl control = SearchControl.controlled(0L, 0L, -1L, () -> 0L);
            assertFalse(parallel.search(new SearchRequest(
                board, GameHistory.initial(board), depth, SearchObserver.NONE, control
            )).completed());
        }
        final Probe cancelledProbe = new Probe();
        cancelledTable.probe(
            board[Board.KEY], depth, -TranspositionScores.MATE_SCORE - 1,
            TranspositionScores.MATE_SCORE + 1, 0, cancelledProbe
        );
        assertFalse(cancelledProbe.keyMatches());
    }

    @Test
    void repetitionAndHalfmoveContextsRemainIsolatedInBothPopulationOrders() {
        final long[] ordinaryBoard = Board.fromFen(
            "4k3/8/8/8/8/8/8/Q3K3 w - - 2 1"
        );
        final GameHistory ordinary = GameHistory.initial(ordinaryBoard);
        final GameHistory repeated = GameHistory.builder(ordinaryBoard)
            .appendPosition(ordinaryBoard).appendPosition(ordinaryBoard).snapshot();
        final int ordinaryScore = new AlphaBetaPvsSearch(
            new TranspositionTable(1 << 12)
        ).search(new SearchRequest(ordinaryBoard, ordinary, 2)).score();
        assertNotEquals(0, ordinaryScore);

        try(RootParallelSearch drawnFirst = new RootParallelSearch(
            new TranspositionTable(1 << 12), 4
        )) {
            assertEquals(0, direct(drawnFirst, ordinaryBoard, repeated, 2).score());
            assertEquals(ordinaryScore,
                direct(drawnFirst, ordinaryBoard, ordinary, 2).score());
        }
        try(RootParallelSearch ordinaryFirst = new RootParallelSearch(
            new TranspositionTable(1 << 12), 4
        )) {
            assertEquals(ordinaryScore,
                direct(ordinaryFirst, ordinaryBoard, ordinary, 2).score());
            assertEquals(0, direct(ordinaryFirst, ordinaryBoard, repeated, 2).score());
        }

        final long[] hundred = Board.fromFen(
            "4k3/8/8/8/8/8/8/Q3K3 w - - 100 1"
        );
        assertEquals(ordinaryBoard[Board.KEY], hundred[Board.KEY]);
        try(RootParallelSearch search = new RootParallelSearch(
            new TranspositionTable(1 << 12), 4
        )) {
            assertEquals(0, direct(search, hundred, GameHistory.initial(hundred), 2).score());
            assertEquals(ordinaryScore,
                direct(search, ordinaryBoard, ordinary, 2).score());
        }
    }

    @Test
    void everyWorkerOwnsDistinctHeuristicsAndNewGameResetsThem() {
        final long[] board = Board.startingPosition();
        final long quiet = Arrays.stream(legalMoves(board))
            .filter(move -> !com.ohinteractive.seedv6.search.order.MoveOrdering.isTactical(board, move))
            .findFirst().orElseThrow();
        try(RootParallelSearch parallel = new RootParallelSearch(4)) {
            for(int index = 0; index < 4; index ++) {
                assertNotSame(parallel.rootOrderingForTest(), parallel.workerOrderingForTest(index));
                for(int other = 0; other < index; other ++) {
                    assertNotSame(
                        parallel.workerOrderingForTest(other),
                        parallel.workerOrderingForTest(index)
                    );
                }
                parallel.workerOrderingForTest(index).recordQuietCutoff(board, 0, quiet, 2);
                assertTrue(parallel.workerOrderingForTest(index).historyScore(quiet) > 0);
            }
            parallel.rootOrderingForTest().recordQuietCutoff(board, 0, quiet, 2);
            parallel.newGame();
            assertTrue(direct(parallel, board, 1, false, -1L).completed());
            assertEquals(0, parallel.rootOrderingForTest().historyScore(quiet));
            for(int index = 0; index < 4; index ++) {
                assertEquals(0, parallel.workerOrderingForTest(index).historyScore(quiet));
                assertEquals(0L, parallel.workerOrderingForTest(index).killer(0, 0));
            }
        }
    }

    @Test
    void managedWorkerFailurePublishesFailureThenAcceptsAnotherGeneration() throws Exception {
        final AtomicBoolean failOnce = new AtomicBoolean(true);
        final RuntimeException expected = new RuntimeException("managed injected failure");
        final RootParallelSearch parallel = new RootParallelSearch(
            new TranspositionTable(1 << 12), SelectiveSearchPolicy.production(), 4,
            (index, move) -> {
                if(index == 1 && failOnce.compareAndSet(true, false)) throw expected;
            }
        );
        final AtomicReference<ManagedSearchResult> first = new AtomicReference<>();
        final AtomicReference<ManagedSearchResult> second = new AtomicReference<>();
        final CountDownLatch firstDone = new CountDownLatch(1);
        final CountDownLatch secondDone = new CountDownLatch(1);
        try(SearchLifecycleService service = new SearchLifecycleService(
            TimeSource.SYSTEM, () -> parallel
        )) {
            final long[] board = Board.startingPosition();
            service.start(
                board, GameHistory.initial(board), new SearchLimits(2, -1L, -1L, false),
                result -> {
                    first.set(result);
                    firstDone.countDown();
                }
            );
            assertTrue(firstDone.await(5L, TimeUnit.SECONDS));
            assertEquals(SearchTermination.FAILURE, first.get().termination());
            assertSame(expected, first.get().failure());
            assertTrue(first.get().hasMove());

            service.start(
                board, GameHistory.initial(board), new SearchLimits(2, -1L, -1L, false),
                result -> {
                    second.set(result);
                    secondDone.countDown();
                }
            );
            assertTrue(secondDone.await(5L, TimeUnit.SECONDS));
            assertEquals(SearchTermination.COMPLETED, second.get().termination());
            assertLegalPv(board, second.get().lastCompletedResult());
        }
    }

    @Test
    void managedCancellationReplacementDeadlineAndShutdownRemainAuthoritative() throws Exception {
        final AtomicReference<CountDownLatch> blocker = new AtomicReference<>(
            new CountDownLatch(1)
        );
        final CountDownLatch twoWorkers = new CountDownLatch(2);
        final RootParallelSearch parallel = new RootParallelSearch(
            new TranspositionTable(1 << 12), SelectiveSearchPolicy.production(), 4,
            (index, move) -> {
                final CountDownLatch current = blocker.get();
                if(current != null) {
                    twoWorkers.countDown();
                    current.await();
                }
            }
        );
        final AtomicReference<ManagedSearchResult> stopped = new AtomicReference<>();
        final CountDownLatch stoppedDone = new CountDownLatch(1);
        try(SearchLifecycleService service = new SearchLifecycleService(
            TimeSource.SYSTEM, () -> parallel
        )) {
            final long[] board = Board.startingPosition();
            service.start(
                board, GameHistory.initial(board), new SearchLimits(0, -1L, -1L, true),
                result -> {
                    stopped.set(result);
                    stoppedDone.countDown();
                }
            );
            assertTrue(twoWorkers.await(5L, TimeUnit.SECONDS));
            service.stop();
            blocker.getAndSet(null).countDown();
            assertTrue(stoppedDone.await(5L, TimeUnit.SECONDS));
            assertEquals(SearchTermination.STOPPED, stopped.get().termination());
            assertTrue(stopped.get().hasMove());

            final AtomicInteger stalePublications = new AtomicInteger();
            service.start(
                board, GameHistory.initial(board), new SearchLimits(0, -1L, -1L, true),
                ignored -> stalePublications.incrementAndGet()
            );
            final long[] middle = Board.fromFen(
                "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1"
            );
            service.start(
                middle, GameHistory.initial(middle), new SearchLimits(0, -1L, -1L, true),
                ignored -> stalePublications.incrementAndGet()
            );
            final long[] newest = Board.fromFen(
                "4k3/8/8/8/8/8/3P4/4K3 w - - 0 1"
            );
            final AtomicReference<ManagedSearchResult> current = new AtomicReference<>();
            final CountDownLatch currentDone = new CountDownLatch(1);
            service.start(
                newest, GameHistory.initial(newest), new SearchLimits(2, -1L, -1L, false),
                result -> {
                    current.set(result);
                    currentDone.countDown();
                }
            );
            assertTrue(currentDone.await(5L, TimeUnit.SECONDS));
            assertEquals(0, stalePublications.get());
            assertEquals(SearchTermination.COMPLETED, current.get().termination());
            assertLegalPv(newest, current.get().lastCompletedResult());
        }

        final AtomicInteger ticks = new AtomicInteger();
        final TimeSource clock = () -> ticks.getAndIncrement() * 1_000_000L;
        final AtomicReference<ManagedSearchResult> timed = new AtomicReference<>();
        final CountDownLatch timedDone = new CountDownLatch(1);
        try(SearchLifecycleService service = new SearchLifecycleService(
            clock, () -> new RootParallelSearch(4)
        )) {
            final long[] board = Board.startingPosition();
            service.start(
                board, GameHistory.initial(board), new SearchLimits(0, -1L, 20L, false),
                result -> {
                    timed.set(result);
                    timedDone.countDown();
                }
            );
            assertTrue(timedDone.await(5L, TimeUnit.SECONDS));
            assertEquals(SearchTermination.TIME_LIMIT, timed.get().termination());
            assertTrue(timed.get().hasMove());
            assertTrue(timed.get().nodes() > 0L);
        }

        final SearchLifecycleService closing = new SearchLifecycleService(4);
        final CountDownLatch depthOne = new CountDownLatch(1);
        final long[] board = Board.startingPosition();
        closing.start(
            board, GameHistory.initial(board), new SearchLimits(0, -1L, -1L, true),
            new SearchObserver() {
                @Override
                public void onIterationCompleted(
                    com.ohinteractive.seedv6.search.common.IterationSnapshot snapshot
                ) {
                    depthOne.countDown();
                }
            }, ignored -> {}
        );
        assertTrue(depthOne.await(5L, TimeUnit.SECONDS));
        final long closeStart = System.nanoTime();
        closing.close();
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - closeStart) < 4_000L);
        assertTrue(closing.isTerminated());
        assertTrue(Thread.getAllStackTraces().keySet().stream().noneMatch(
            thread -> thread.isAlive() && thread.getName().startsWith("seedv6-root-worker-")
        ));
    }

    private static void assertCoverage(long[] board, int threads) {
        final int expected = legalMoves(board).length;
        final AtomicIntegerArray visits = new AtomicIntegerArray(expected);
        final RootParallelSearch.RootWorkerHook hook = (index, move) -> {
            assertEquals(0, visits.getAndIncrement(index));
        };
        try(RootParallelSearch parallel = new RootParallelSearch(
            new TranspositionTable(1 << 10), SelectiveSearchPolicy.production(), threads, hook
        )) {
            final SearchResult result = direct(parallel, board, 1, false, -1L);
            assertTrue(result.completed());
            assertEquals(expected, result.legalRootMoves());
            for(int index = 0; index < expected; index ++) assertEquals(1, visits.get(index));
        }
    }

    private static SearchResult iterative(
        com.ohinteractive.seedv6.search.common.SingleDepthSearch search,
        long[] board, int depth, boolean diagnostics
    ) {
        final SearchControl control = SearchControl.controlled(
            -1L, System.nanoTime(), -1L, TimeSource.SYSTEM
        );
        return new IterativeDeepeningSearch(search).search(new SearchRequest(
            board, GameHistory.initial(board), depth, SearchObserver.NONE, control, diagnostics
        )).lastCompletedResult();
    }

    private static SearchResult direct(
        RootParallelSearch search, long[] board, int depth,
        boolean diagnostics, long nodeLimit
    ) {
        final SearchControl control = SearchControl.controlled(
            nodeLimit, System.nanoTime(), -1L, TimeSource.SYSTEM
        );
        return search.search(new SearchRequest(
            board, GameHistory.initial(board), depth, SearchObserver.NONE, control, diagnostics
        ));
    }

    private static SearchResult direct(
        RootParallelSearch search, long[] board, GameHistory history, int depth
    ) {
        final SearchControl control = SearchControl.controlled(
            -1L, System.nanoTime(), -1L, TimeSource.SYSTEM
        );
        return search.search(new SearchRequest(
            board, history, depth, SearchObserver.NONE, control
        ));
    }

    private static long[] legalMoves(long[] board) {
        final long[] moves = new long[256];
        final int count = Gen.genAll(
            board[0], board[1], board[2], board[3],
            Math.toIntExact(board[Board.STATUS]), board[Board.KEY], true,
            moves, new long[Board.MAX_BITBOARDS]
        );
        return Arrays.copyOf(moves, count);
    }

    private static void assertLegalPv(long[] suppliedRoot, SearchResult result) {
        long[] board = suppliedRoot.clone();
        for(long move : result.principalVariation()) {
            assertTrue(Arrays.stream(legalMoves(board)).anyMatch(candidate -> candidate == move));
            final long[] child = new long[Board.MAX_BITBOARDS];
            Board.makeMoveInto(
                board[0], board[1], board[2], board[3],
                Math.toIntExact(board[Board.STATUS]), board[Board.KEY], move, child
            );
            board = child;
        }
    }
}
