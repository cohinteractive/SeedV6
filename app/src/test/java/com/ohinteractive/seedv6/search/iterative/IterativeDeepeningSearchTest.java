package com.ohinteractive.seedv6.search.iterative;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.move.LegalMoveResolver;
import com.ohinteractive.seedv6.core.move.MoveIntent;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch;
import com.ohinteractive.seedv6.search.common.IterationSnapshot;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.common.WindowedSearch;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import com.ohinteractive.seedv6.search.tt.TranspositionTable;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IterativeDeepeningSearchTest {

    @Test
    void publishesExactDepthsOneThroughTargetOnceWithImmutableCumulativeSnapshots() {
        final long[] board = Board.startingPosition();
        final List<IterationSnapshot> snapshots = new ArrayList<>();
        final StepClock clock = new StepClock(500_000L);
        final SearchControl control = SearchControl.controlled(-1L, 0L, -1L, clock);
        final IterativeSearchOutcome outcome = new IterativeDeepeningSearch(
            new AlphaBetaPvsSearch(new TranspositionTable(1 << 14))
        ).search(new SearchRequest(
            board, GameHistory.initial(board), 3, observer(snapshots), control
        ));

        assertTrue(outcome.targetDepthCompleted());
        assertEquals(List.of(1, 2, 3), snapshots.stream().map(IterationSnapshot::depth).toList());
        assertEquals(3, outcome.lastCompletedResult().depth());
        assertEquals(control.nodes(), outcome.lastCompletedResult().nodes());
        assertEquals(control.nodes(), snapshots.get(2).nodes());
        assertTrue(snapshots.get(0).nodes() < snapshots.get(1).nodes());
        assertTrue(snapshots.get(1).nodes() < snapshots.get(2).nodes());
        assertTrue(snapshots.get(0).elapsedMillis() <= snapshots.get(1).elapsedMillis());
        assertTrue(snapshots.get(1).elapsedMillis() <= snapshots.get(2).elapsedMillis());
        assertFalse(snapshots.get(0).hasNps());
        assertTrue(snapshots.get(1).hasNps());
        for(IterationSnapshot snapshot : snapshots) assertLegalPv(board, snapshot.result());

        final long[] retained = snapshots.get(0).principalVariation();
        final long[] callerCopy = snapshots.get(0).principalVariation();
        if(callerCopy.length != 0) callerCopy[0] = 0L;
        assertArrayEquals(retained, snapshots.get(0).principalVariation());
    }

    @Test
    void aspirationAndColdFullWindowAgreeAcrossOrdinaryTacticalDrawMateAndTerminalRoots() {
        final List<SearchCase> cases = List.of(
            new SearchCase(Board.startingPosition(), GameHistory.initial(Board.startingPosition()), 3),
            ordinary("4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1", 3),
            repeated("4k3/8/8/8/8/8/8/Q3K3 w - - 2 1", 2),
            ordinary("7k/6p1/5KQ1/8/8/8/8/8 w - - 0 1", 2),
            ordinary("7k/p7/5KQ1/8/8/8/8/8 b - - 0 1", 2),
            ordinary("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1", 3)
        );

        for(SearchCase searchCase : cases) {
            final SearchResult expected = new AlphaBetaPvsSearch().search(
                new SearchRequest(
                    searchCase.board, searchCase.history,
                    terminal(searchCase.board) ? 1 : searchCase.depth
                )
            );
            final List<IterationSnapshot> snapshots = new ArrayList<>();
            final SearchControl control = SearchControl.controlled(
                -1L, System.nanoTime(), -1L, TimeSource.SYSTEM
            );
            final IterativeSearchOutcome actual = new IterativeDeepeningSearch(
                new AlphaBetaPvsSearch()
            ).search(new SearchRequest(
                searchCase.board, searchCase.history, searchCase.depth,
                observer(snapshots), control
            ));
            final SearchResult completed = actual.lastCompletedResult();
            assertNotNull(completed);
            assertEquals(expected.score(), completed.score(), searchCase.toString());
            assertEquals(expected.hasMove(), completed.hasMove(), searchCase.toString());
            assertEquals(expected.legalRootMoves(), completed.legalRootMoves());
            assertLegalPv(searchCase.board, completed);
            for(IterationSnapshot snapshot : snapshots) {
                assertTrue(snapshot.result().completed());
                assertLegalPv(searchCase.board, snapshot.result());
            }
        }
    }

    @Test
    void forcedFailHighFailLowAndMultipleWideningConvergeThroughBoundedFullWindow() {
        assertForcedWidening(500, true);
        assertForcedWidening(-500, false);
    }

    @Test
    void mateBandOnBothSidesBypassesAspirationWithoutUnsafeArithmetic() {
        assertMateBypassesAspiration(TranspositionScores.MATE_SCORE - 1);
        assertMateBypassesAspiration(-TranspositionScores.MATE_SCORE + 2);
    }

    @Test
    void nodeLimitDuringRetryAndStopBetweenIterationsRetainOnlyLastCompletedDepth() {
        final ScriptedWindowSearch limitedSearch = new ScriptedWindowSearch(0, 500);
        final List<IterationSnapshot> limitedSnapshots = new ArrayList<>();
        final SearchControl limited = SearchControl.controlled(3L, 0L, -1L, () -> 0L);
        final IterativeSearchOutcome limitedOutcome = new IterativeDeepeningSearch(
            limitedSearch, 10, 4, 0
        ).search(new SearchRequest(
            Board.startingPosition(), GameHistory.initial(Board.startingPosition()), 2,
            observer(limitedSnapshots), limited
        ));
        assertFalse(limitedOutcome.targetDepthCompleted());
        assertEquals(SearchTermination.NODE_LIMIT, limited.termination());
        assertEquals(3L, limited.nodes());
        assertEquals(List.of(1), limitedSnapshots.stream().map(IterationSnapshot::depth).toList());
        assertEquals(1, limitedOutcome.lastCompletedResult().depth());
        assertEquals(1L, limitedOutcome.lastCompletedResult().nodes());

        final AtomicLong now = new AtomicLong();
        final ScriptedWindowSearch timedSearch = new ScriptedWindowSearch(
            0, 500, () -> now.set(5_000_000L)
        );
        final SearchControl timed = SearchControl.controlled(
            -1L, 0L, 5_000_000L, now::get
        );
        final List<IterationSnapshot> timedSnapshots = new ArrayList<>();
        final IterativeSearchOutcome timedOutcome = new IterativeDeepeningSearch(
            timedSearch, 10, 4, 0
        ).search(new SearchRequest(
            Board.startingPosition(), GameHistory.initial(Board.startingPosition()), 2,
            observer(timedSnapshots), timed
        ));
        assertEquals(SearchTermination.TIME_LIMIT, timed.termination());
        assertEquals(List.of(1), timedSnapshots.stream().map(IterationSnapshot::depth).toList());
        assertEquals(1, timedOutcome.lastCompletedResult().depth());

        final SearchControl stopped = SearchControl.controlled(-1L, 0L, -1L, () -> 0L);
        final List<IterationSnapshot> stoppedSnapshots = new ArrayList<>();
        final SearchObserver stopAfterOne = new SearchObserver() {
            @Override
            public void onIterationCompleted(IterationSnapshot snapshot) {
                stoppedSnapshots.add(snapshot);
                stopped.request(SearchTermination.STOPPED);
            }
        };
        final IterativeSearchOutcome stoppedOutcome = new IterativeDeepeningSearch(
            new ScriptedWindowSearch(0, 0)
        ).search(new SearchRequest(
            Board.startingPosition(), GameHistory.initial(Board.startingPosition()), 3,
            stopAfterOne, stopped
        ));
        assertEquals(List.of(1), stoppedSnapshots.stream().map(IterationSnapshot::depth).toList());
        assertEquals(1, stoppedOutcome.lastCompletedResult().depth());
        assertEquals(SearchTermination.STOPPED, stopped.termination());
    }

    @Test
    void oneTtGenerationCoversAllDepthsAndRetriesAndNewGameResetsHeuristics() {
        final long[] board = Board.startingPosition();
        final TranspositionTable table = new TranspositionTable(1 << 12);
        final AlphaBetaPvsSearch alphaBeta = new AlphaBetaPvsSearch(table);
        final IterativeDeepeningSearch controller = new IterativeDeepeningSearch(alphaBeta);
        final long quiet = resolve(board, "e2e4");
        alphaBeta.ordering().recordQuietCutoff(board, 0, quiet, 2);
        final int retainedHistory = alphaBeta.ordering().historyScore(quiet);
        final SearchControl firstControl = SearchControl.controlled(
            -1L, System.nanoTime(), -1L, TimeSource.SYSTEM
        );
        controller.search(new SearchRequest(
            board, GameHistory.initial(board), 3, SearchObserver.NONE, firstControl
        ));
        assertEquals(1, table.generation());
        assertTrue(alphaBeta.ordering().historyScore(quiet) >= retainedHistory);

        controller.newGame();
        final SearchControl secondControl = SearchControl.controlled(
            -1L, System.nanoTime(), -1L, TimeSource.SYSTEM
        );
        controller.search(new SearchRequest(
            board, GameHistory.initial(board), 1, SearchObserver.NONE, secondControl
        ));
        assertEquals(1, table.generation());
        assertEquals(0, alphaBeta.ordering().historyScore(quiet));
        assertEquals(0L, alphaBeta.ordering().killer(0, 0));
    }

    @Test
    void scoreEquivalentRootMovesUseDeterministicAuthoritativeFallbackOrder() {
        final long[] board = Board.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1");
        final long[] moves = new long[256];
        final int count = Gen.genAll(
            board[0], board[1], board[2], board[3],
            Math.toIntExact(board[Board.STATUS]), board[Board.KEY], true,
            moves, new long[Board.MAX_BITBOARDS]
        );
        assertTrue(count > 1);
        for(int run = 0; run < 2; run ++) {
            final SearchControl control = SearchControl.controlled(
                -1L, System.nanoTime(), -1L, TimeSource.SYSTEM
            );
            final SearchResult result = new IterativeDeepeningSearch(
                new AlphaBetaPvsSearch()
            ).search(new SearchRequest(
                board, GameHistory.initial(board), 3, SearchObserver.NONE, control
            )).lastCompletedResult();
            assertEquals(0, result.score());
            assertEquals(moves[0], result.bestMove());
        }
    }

    private static void assertForcedWidening(int score, boolean high) {
        final ScriptedWindowSearch exact = new ScriptedWindowSearch(0, score);
        final List<IterationSnapshot> snapshots = new ArrayList<>();
        final SearchControl control = SearchControl.controlled(-1L, 0L, -1L, () -> 0L);
        final IterativeSearchOutcome outcome = new IterativeDeepeningSearch(
            exact, 10, 3, 0
        ).search(new SearchRequest(
            Board.startingPosition(), GameHistory.initial(Board.startingPosition()), 2,
            observer(snapshots), control
        ));
        assertTrue(outcome.targetDepthCompleted());
        assertEquals(score, outcome.lastCompletedResult().score());
        assertEquals(List.of(1, 2), snapshots.stream().map(IterationSnapshot::depth).toList());
        assertEquals(5, exact.windows.size());
        assertEquals(new Window(2, -10, 10), exact.windows.get(1));
        assertEquals(new Window(2, -20, 20), exact.windows.get(2));
        assertEquals(new Window(2, -40, 40), exact.windows.get(3));
        assertEquals(new Window(
            2, -TranspositionScores.MATE_SCORE - 1,
            TranspositionScores.MATE_SCORE + 1
        ), exact.windows.get(4));
        assertTrue(high ? score >= exact.windows.get(1).beta : score <= exact.windows.get(1).alpha);
    }

    private static void assertMateBypassesAspiration(int mateScore) {
        final ScriptedWindowSearch exact = new ScriptedWindowSearch(mateScore, mateScore);
        final SearchControl control = SearchControl.controlled(-1L, 0L, -1L, () -> 0L);
        final IterativeSearchOutcome outcome = new IterativeDeepeningSearch(exact).search(
            new SearchRequest(
                Board.startingPosition(), GameHistory.initial(Board.startingPosition()), 2,
                SearchObserver.NONE, control
            )
        );
        assertEquals(mateScore, outcome.lastCompletedResult().score());
        assertEquals(2, exact.windows.size());
        assertEquals(-TranspositionScores.MATE_SCORE - 1, exact.windows.get(1).alpha);
        assertEquals(TranspositionScores.MATE_SCORE + 1, exact.windows.get(1).beta);
    }

    private static SearchObserver observer(List<IterationSnapshot> snapshots) {
        return new SearchObserver() {
            @Override
            public void onIterationCompleted(IterationSnapshot snapshot) {
                snapshots.add(snapshot);
            }
        };
    }

    private static SearchCase ordinary(String fen, int depth) {
        final long[] board = Board.fromFen(fen);
        return new SearchCase(board, GameHistory.initial(board), depth);
    }

    private static SearchCase repeated(String fen, int depth) {
        final long[] board = Board.fromFen(fen);
        return new SearchCase(
            board, GameHistory.builder(board).appendPosition(board).appendPosition(board).snapshot(),
            depth
        );
    }

    private static boolean terminal(long[] board) {
        return Gen.genAll(
            board[0], board[1], board[2], board[3],
            Math.toIntExact(board[Board.STATUS]), board[Board.KEY], true,
            new long[256], new long[Board.MAX_BITBOARDS]
        ) == 0;
    }

    private static void assertLegalPv(long[] root, SearchResult result) {
        long[] board = root.clone();
        for(long move : result.principalVariation()) {
            final long[] moves = new long[256];
            final int count = Gen.genAll(
                board[0], board[1], board[2], board[3],
                Math.toIntExact(board[Board.STATUS]), board[Board.KEY], true,
                moves, new long[Board.MAX_BITBOARDS]
            );
            boolean legal = false;
            for(int index = 0; index < count; index ++) legal |= moves[index] == move;
            assertTrue(legal, "Illegal PV move " + Long.toUnsignedString(move));
            final long[] child = new long[Board.MAX_BITBOARDS];
            Board.makeMoveInto(
                board[0], board[1], board[2], board[3],
                Math.toIntExact(board[Board.STATUS]), board[Board.KEY], move, child
            );
            board = child;
        }
    }

    private static long resolve(long[] board, String coordinate) {
        return new LegalMoveResolver().resolve(
            board, new MoveIntent(
                square(coordinate.substring(0, 2)), square(coordinate.substring(2, 4))
            )
        );
    }

    private static int square(String coordinate) {
        return (coordinate.charAt(1) - '1') * 8 + coordinate.charAt(0) - 'a';
    }

    private record SearchCase(long[] board, GameHistory history, int depth) {}
    private record Window(int depth, int alpha, int beta) {}

    private static final class StepClock implements TimeSource {
        private final long step;
        private final AtomicLong now = new AtomicLong();

        StepClock(long step) {
            this.step = step;
        }

        @Override
        public long nanoTime() {
            return now.addAndGet(step);
        }
    }

    private static final class ScriptedWindowSearch implements WindowedSearch {
        final List<Window> windows = new ArrayList<>();
        private final int firstScore;
        private final int laterScore;
        private final Runnable afterLaterAttempt;
        private boolean active;

        ScriptedWindowSearch(int firstScore, int laterScore) {
            this(firstScore, laterScore, () -> {});
        }

        ScriptedWindowSearch(
            int firstScore, int laterScore, Runnable afterLaterAttempt
        ) {
            this.firstScore = firstScore;
            this.laterScore = laterScore;
            this.afterLaterAttempt = afterLaterAttempt;
        }

        @Override
        public SearchResult search(SearchRequest request) {
            beginTopLevelSearch();
            try {
                return searchWindow(
                    request, -TranspositionScores.MATE_SCORE - 1,
                    TranspositionScores.MATE_SCORE + 1
                );
            } finally {
                endTopLevelSearch();
            }
        }

        @Override
        public void beginTopLevelSearch() {
            assertFalse(active);
            active = true;
        }

        @Override
        public SearchResult searchWindow(SearchRequest request, int alpha, int beta) {
            assertTrue(active);
            windows.add(new Window(request.depth(), alpha, beta));
            if(!request.control().tryEnterNode()) {
                return new SearchResult(0L, false, 0, request.depth(), 0L, 1, false);
            }
            final SearchResult result = new SearchResult(
                0L, false, request.depth() == 1 ? firstScore : laterScore,
                request.depth(), 1L, 1, true
            );
            if(request.depth() > 1) afterLaterAttempt.run();
            return result;
        }

        @Override
        public void endTopLevelSearch() {
            assertTrue(active);
            active = false;
        }

        @Override
        public int maxSupportedDepth() {
            return 8;
        }
    }
}
