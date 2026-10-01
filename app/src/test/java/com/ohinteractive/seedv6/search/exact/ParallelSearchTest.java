package com.ohinteractive.seedv6.search.exact;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.search.tablebase.TablebaseWin;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(60)
class ParallelSearchTest {
    private static final SearchEvaluation EXACT_HCE = SearchEvaluation.handcraftedIsolation();

    @Test void everyLaterRootMoveBusyStillGetsItsMandatoryPassAfterCancellationAndReuse() {
        var board = Board.startingPosition();
        var history = GameHistory.initial(board);
        var reservations = new MoveReservations();
        long key = SearchKey.key(board, SearchKey.rootHistory(board, history));
        long[] tokens = Arrays.stream(legal(board)).map(move -> MoveReservations.fingerprint(key, 3, move)).toArray();
        for(long token : tokens) assertEquals(token, reservations.claim(token));
        var table = new TTable(1);
        var search = ExactSearch.sharedWorker(ExactEvaluator.from(EXACT_HCE), table, reservations, false);
        table.advanceGeneration();
        search.beginSharedRequest(1);
        try {
            assertFalse(search.search(board, history, 3, () -> search.visitedNodes() >= 30).completed());
            var actual = search.search(board, history, 3, ExactSearch.NEVER_CANCELLED);
            assertTrue(actual.completed());
            assertEquals(new ExactSearch().search(board, 3).score(), actual.score());
            // Every token belongs to this fixture, not to a searching worker.
            for(long token : tokens) assertTrue(reservations.busy(token));
        } finally {
            search.endRequest();
            for(long token : tokens) reservations.release(token);
        }
        assertTrue(reservations.isEmpty());
    }

    @Test void concurrentExactRootsMatchIndependentOracleAndEveryChosenMoveIsAWitness() {
        String[] fens = {
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
            "7k/8/8/3pP3/8/8/8/K7 w - d6 0 1",
            "7k/1P6/8/8/8/8/6p1/K7 w - - 0 1",
            "7k/8/8/8/8/8/8/KQ6 w - - 99 1",
            "7k/6Q1/6K1/8/8/8/8/8 b - - 100 1",
            "7k/5Q2/6K1/8/8/8/8/8 b - - 100 1"
        };
        var oracle = new ExactSearch();
        for(int threads : new int[] {1, 2, 4, 16}) {
            try(var search = new ParallelSearch(threads, EXACT_HCE, new TTable(1))) {
                for(String fen : fens) {
                    var board = Board.fromFen(fen);
                    var history = GameHistory.initial(board);
                    var expected = oracle.search(board, history, 4, ExactSearch.NEVER_CANCELLED);
                    var actual = search.search(new SearchRequest(board, history, 4));
                    assertTrue(actual.completed());
                    assertFalse(actual.selective());
                    assertEquals(expected.score(), actual.score(), fen);
                    validateWitness(oracle, board, history, actual);
                }
            }
        }
    }

    @Test void neuralWorkersOwnIndependentIncrementalStacks() {
        var network = NnueNetwork.initialized(36243624);
        var definition = SearchEvaluation.incremental(network);
        var oracle = new ExactSearch(ExactEvaluator.from(SearchEvaluation.fullRecompute(network, NnueScoreMapping.V1)));
        var board = Board.fromFen("r3k2r/1P6/8/3pP3/8/8/6p1/R3K2R w KQkq d6 0 1");
        var history = GameHistory.initial(board);
        try(var search = new ParallelSearch(4, definition, new TTable(1))) {
            for(int i = 0; i < 8; i++) {
                var actual = search.search(new SearchRequest(board, history, 3));
                assertFalse(actual.selective());
                assertEquals(oracle.search(board, history, 3, ExactSearch.NEVER_CANCELLED).score(), actual.score());
                validateWitness(oracle, board, history, actual);
            }
        }
    }

    @Test void oneGlobalBudgetIncludesEveryWorkerAndNeverFabricatesFallback() {
        var board = Board.startingPosition();
        for(int threads : new int[] {2, 4, 6}) {
            try(var driver = new SearchDriver(new ParallelSearch(threads, EXACT_HCE, new TTable(1)))) {
                for(long budget : new long[] {0, 1, 19, 20, 21, 40, 100, 1000, 5000}) {
                    driver.newGame();
                    var control = SearchControl.controlled(budget, 0, -1, TimeSource.SYSTEM);
                    var actual = driver.search(new SearchRequest(board, GameHistory.initial(board), 8,
                            SearchObserver.NONE, control, true));
                    assertEquals(budget, control.nodes());
                    assertEquals(budget, actual.nodes());
                    if(budget < 20) assertNull(actual.lastCompletedResult());
                    if(actual.lastCompletedResult() != null) {
                        assertTrue(actual.lastCompletedResult().completed());
                        assertTrue(actual.lastCompletedResult().depth() < 8);
                    }
                }
            }
        }
    }

    @Test void cancellationDrainsHelpersBeforeReturnAndFailureAllowsSafeReuse() throws Exception {
        var board = Board.startingPosition();
        var history = GameHistory.initial(board);
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var gate = new AtomicBoolean();
        TimeSource clock = () -> {
            if(isHelper() && gate.compareAndSet(false, true)) {
                entered.countDown();
                try { release.await(); } catch(InterruptedException problem) { Thread.currentThread().interrupt(); }
            }
            return 0;
        };
        try(var search = new ParallelSearch(4, EXACT_HCE, new TTable(1)); var driver = new SearchDriver(search)) {
            var control = SearchControl.controlled(-1, 0, Long.MAX_VALUE, clock);
            var outcome = new AtomicReference<SearchDriverOutcome>();
            var failure = new AtomicReference<Throwable>();
            var owner = new Thread(() -> {
                try { outcome.set(driver.search(new SearchRequest(board, history, 9, control))); }
                catch(Throwable problem) { failure.set(problem); }
            });
            owner.start();
            try {
                assertTrue(entered.await(10, TimeUnit.SECONDS));
                control.request(SearchTermination.STOPPED);
                assertTrue(owner.isAlive(), "A blocked helper must still be drained.");
            } finally {
                release.countDown();
                owner.join(10000);
            }
            assertFalse(owner.isAlive());
            assertNull(failure.get());
            assertEquals(control.nodes(), outcome.get().nodes());
            var injected = new AtomicBoolean();
            TimeSource broken = () -> {
                if(isHelper() && injected.compareAndSet(false, true)) throw new IllegalStateException("helper fault");
                return 0;
            };
            assertThrows(IllegalStateException.class, () -> driver.search(new SearchRequest(board, history, 8,
                    SearchControl.controlled(-1, 0, Long.MAX_VALUE, broken))));
            assertTrue(injected.get());
            search.newGame();
            assertEquals(new ExactSearch().search(board, 4).score(),
                    driver.search(new SearchRequest(board, history, 4)).lastCompletedResult().score());
        }
    }

    @Test void generationAdvancesOncePerRequestIncludingWrapAndReset() {
        class ObservedTable extends TTable {
            int advances;
            ObservedTable() { super(1); }
            @Override public void advanceGeneration() { advances++; super.advanceGeneration(); }
        }
        var table = new ObservedTable();
        var board = Board.startingPosition();
        int expected = new ExactSearch().search(board, 3).score();
        try(var driver = new SearchDriver(new ParallelSearch(4, EXACT_HCE, table))) {
            for(int i = 0; i < 270; i++) {
                if(i % 17 == 0) driver.newGame();
                assertEquals(expected, driver.search(new SearchRequest(board, 3)).lastCompletedResult().score());
                assertEquals(i + 1, table.advances);
            }
        }
    }

    @Test void standaloneLifecycleAndOwnerReentryAreGuarded() {
        var board = Board.startingPosition();
        var search = new ParallelSearch(2, EXACT_HCE, new TTable(1));
        assertThrows(IllegalArgumentException.class, () -> search.search(new SearchRequest(board, 257)));
        var request = new SearchRequest(board, 3, new SearchObserver() {
            @Override public void onSearchFinished(SearchResult result, long elapsedNanos) {
                assertThrows(IllegalStateException.class, search::close);
                assertThrows(IllegalStateException.class, search::newGame);
                assertThrows(IllegalStateException.class, () -> search.search(new SearchRequest(board, 1)));
            }
        });
        assertTrue(search.search(request).completed());
        search.newGame();
        search.close();
        search.close();
        assertThrows(IllegalStateException.class, () -> search.search(new SearchRequest(board, 1)));
    }

    @Test void tablebaseRootDecisionPrecedesAnyParallelGenerationOrWorker() {
        var board = Board.fromFen("7k/8/5KQ1/8/8/8/8/8 w - - 0 1");
        var move = legal(board)[0];
        var table = new TTable(1) {
            @Override public void advanceGeneration() { fail("No nominal Search for a tablebase outcome."); }
        };
        try(var driver = new SearchDriver(new ParallelSearch(4, EXACT_HCE, table),
                (root, history, control) -> new TablebaseWin(move, 1))) {
            var result = driver.search(new SearchRequest(board, 5));
            assertEquals(0, result.nodes());
            assertNull(result.lastCompletedResult());
            assertNotNull(result.tablebaseWin());
        }
    }

    private static boolean isHelper() { return Thread.currentThread().getName().equals("seedv6-search-helper"); }

    private static long[] legal(long[] board) {
        long[] moves = new long[512];
        int count = Gen.genAll(board[0], board[1], board[2], board[3], (int) board[Board.STATUS],
                board[Board.KEY], true, moves, new long[Board.MAX_BITBOARDS]);
        return Arrays.copyOf(moves, count);
    }

    private static long[] child(long[] board, long move) {
        long[] child = new long[Board.MAX_BITBOARDS];
        Board.makeMoveInto(board[0], board[1], board[2], board[3], (int) board[Board.STATUS], board[Board.KEY], move, child);
        return child;
    }

    private static void validateWitness(ExactSearch oracle, long[] board, GameHistory history, SearchResult result) {
        if(result.hasMove()) {
            assertTrue(Arrays.stream(legal(board)).anyMatch(move -> move == result.bestMove()));
            var child = child(board, result.bestMove());
            int value = -oracle.search(child, GameHistory.builder(history).appendPosition(child).snapshot(),
                    result.depth() - 1, ExactSearch.NEVER_CANCELLED).score();
            if(TranspositionScores.isMateScore(value)) value += value > 0 ? -1 : 1;
            assertEquals(result.score(), value);
        }
        for(long move : result.principalVariation()) {
            assertTrue(Arrays.stream(legal(board)).anyMatch(legal -> legal == move));
            board = child(board, move);
        }
    }
}
