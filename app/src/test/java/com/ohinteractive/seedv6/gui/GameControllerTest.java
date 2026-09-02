package com.ohinteractive.seedv6.gui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.move.LegalMoveResolver;
import com.ohinteractive.seedv6.core.move.MoveIntent;
import com.ohinteractive.seedv6.core.move.MoveIntent.Promotion;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.manage.ManagedSearchResult;
import com.ohinteractive.seedv6.search.manage.SearchLimits;

class GameControllerTest {

    @Test
    void humanAndEngineMovesUseExactLegalValuesAndRemainSynchronized() throws Exception {
        final Harness harness = onEdt(Harness::new);
        onEdt(() -> humanMove(harness.controller, "e2e4"));
        assertEquals(1, harness.search.starts);
        assertTrue(harness.search.searching);
        assertEquals(4, harness.search.pending.limits.depth());
        assertEquals(SearchLimits.NO_LIMIT, harness.search.pending.limits.timeMillis());
        final long engineMove = resolve(harness.search.pending.board, "e7e5");
        onEdt(() -> harness.search.completeCurrent(managed(engineMove, SearchTermination.COMPLETED)));

        final long[] board = onEdt(harness.controller::boardSnapshot);
        assertEquals(Piece.PAWN | 8, Board.getSquare(board[0], board[1], board[2], board[3], square("e5")));
        assertEquals(3, onEdt(harness.controller::historySize));
        assertEquals(List.of("e2e4", "e7e5"), onEdt(harness.controller::displayedMoves));
        assertTrue(harness.view.allMutationsOnEdt);
    }

    @Test
    void illegalInputAndInvalidFenLeaveTheCurrentSessionUnchanged() throws Exception {
        final Harness harness = onEdt(Harness::new);
        final long[] before = onEdt(harness.controller::boardSnapshot);
        final int historyBefore = onEdt(harness.controller::historySize);
        onEdt(() -> humanMove(harness.controller, "e2e5"));
        assertArrayEquals(before, onEdt(harness.controller::boardSnapshot));
        assertEquals(historyBefore, onEdt(harness.controller::historySize));
        assertFalse(onEdt(() -> harness.controller.loadFen("not a fen")));
        assertArrayEquals(before, onEdt(harness.controller::boardSnapshot));
        assertFalse(harness.view.errors.isEmpty());
    }

    @Test
    void promotionRequiresTheViewChoiceAndAppliesThatExactGeneratedMove() throws Exception {
        final Harness harness = onEdt(Harness::new);
        harness.view.promotion = Promotion.KNIGHT;
        assertTrue(onEdt(() -> harness.controller.loadFen(
            "7k/P7/8/8/8/8/8/7K w - - 0 1"
        )));
        onEdt(() -> humanMove(harness.controller, "a7a8"));
        final long[] board = onEdt(harness.controller::boardSnapshot);
        assertEquals(Piece.KNIGHT,
            Board.getSquare(board[0], board[1], board[2], board[3], square("a8")));
        assertEquals(List.of("a7a8n"), onEdt(harness.controller::displayedMoves));
        assertEquals(
            List.of(Promotion.QUEEN, Promotion.ROOK, Promotion.BISHOP, Promotion.KNIGHT),
            harness.view.lastPromotionChoices
        );
    }

    @Test
    void noMoveManagedResultCannotMutateTheBoard() throws Exception {
        final Harness harness = onEdt(Harness::new);
        onEdt(() -> humanMove(harness.controller, "e2e4"));
        final long[] before = onEdt(harness.controller::boardSnapshot);
        onEdt(() -> harness.search.completeCurrent(new ManagedSearchResult(
            1L, 0L, false, null, SearchTermination.COMPLETED, 0L, null
        )));
        assertArrayEquals(before, onEdt(harness.controller::boardSnapshot));
        assertEquals(2, onEdt(harness.controller::historySize));
    }

    @Test
    void terminalPositionsNeverStartOrAcceptAnotherEngineMove() throws Exception {
        final Harness harness = onEdt(Harness::new);
        assertTrue(onEdt(() -> harness.controller.loadFen(
            "7k/6Q1/6K1/8/8/8/8/8 b - - 0 1"
        )));
        assertEquals(PositionStatus.Outcome.CHECKMATE,
            onEdt(harness.controller::positionStatus).outcome());
        assertEquals(0, harness.search.starts);
        assertFalse(harness.search.searching);
    }

    @Test
    void resetFenModeAndServiceReplacementRejectQueuedOldResult() throws Exception {
        final Harness harness = onEdt(Harness::new);
        onEdt(() -> humanMove(harness.controller, "e2e4"));
        final FakeSearch.Pending obsolete = harness.search.pending;
        final long staleMove = resolve(obsolete.board, "e7e5");

        onEdt(harness.controller::newGame);
        assertEquals(SearchTermination.NEW_GAME, harness.search.invalidations.getLast());
        onEdt(() -> harness.controller.setWorkerCount(2));
        assertEquals(2, harness.search.workers);
        onEdt(() -> obsolete.listener.onComplete(
            obsolete.token, managed(staleMove, SearchTermination.COMPLETED)
        ));
        assertArrayEquals(Board.startingPosition(), onEdt(harness.controller::boardSnapshot));
        assertEquals(1, onEdt(harness.controller::historySize));

        onEdt(() -> humanMove(harness.controller, "d2d4"));
        final FakeSearch.Pending modeObsolete = harness.search.pending;
        onEdt(() -> harness.controller.setGameMode(GameController.GameMode.HUMAN_VS_HUMAN));
        onEdt(() -> modeObsolete.listener.onComplete(
            modeObsolete.token, managed(resolve(modeObsolete.board, "d7d5"), SearchTermination.COMPLETED)
        ));
        assertEquals(List.of("d2d4"), onEdt(harness.controller::displayedMoves));
    }

    @Test
    void loadFenDuringSearchIsTransactionalAndRejectsTheQueuedOldResult() throws Exception {
        final Harness harness = onEdt(Harness::new);
        onEdt(() -> humanMove(harness.controller, "e2e4"));
        final FakeSearch.Pending obsolete = harness.search.pending;
        final long staleMove = resolve(obsolete.board, "e7e5");
        final String fen = "4k3/8/8/8/8/8/8/R3K3 w - - 0 1";

        assertTrue(onEdt(() -> harness.controller.loadFen(fen)));
        assertEquals(SearchTermination.POSITION_CHANGED, harness.search.invalidations.getLast());
        onEdt(() -> obsolete.listener.onComplete(
            obsolete.token, managed(staleMove, SearchTermination.COMPLETED)
        ));
        assertArrayEquals(Board.fromFen(fen), onEdt(harness.controller::boardSnapshot));
        assertEquals(1, onEdt(harness.controller::historySize));
        assertEquals(List.of(), onEdt(harness.controller::displayedMoves));
    }

    @Test
    void depthMovetimeAndStopControlsUseTheExistingLimitSemantics() throws Exception {
        final Harness harness = onEdt(Harness::new);
        onEdt(() -> harness.controller.setSearchSettings(new GameController.SearchSettings(
            GameController.LimitKind.MOVETIME, 4, 250L
        )));
        onEdt(() -> harness.controller.setHumanSide(GameController.HumanSide.BLACK));
        assertNotNull(harness.search.pending);
        assertEquals(SearchLimits.NO_DEPTH, harness.search.pending.limits.depth());
        assertEquals(240L, harness.search.pending.limits.timeMillis());
        onEdt(harness.controller::stopSearch);
        assertEquals(1, harness.search.stops);
    }

    @Test
    void stoppedSelfPlayAppliesOneRetainedMoveAndDoesNotChain() throws Exception {
        final Harness harness = onEdt(Harness::new);
        onEdt(() -> harness.controller.setGameMode(GameController.GameMode.ENGINE_VS_ENGINE));
        assertEquals(1, harness.search.starts);
        onEdt(harness.controller::stopSearch);
        final long move = firstLegal(harness.search.pending.board);
        onEdt(() -> harness.search.completeCurrent(managed(move, SearchTermination.STOPPED)));
        assertEquals(1, harness.search.starts);
        assertEquals(2, onEdt(harness.controller::historySize));
    }

    @Test
    void boundedSelfPlayIsSequentialLegalAndHistorySynchronized() throws Exception {
        final Harness harness = onEdt(Harness::new);
        onEdt(() -> harness.controller.setGameMode(GameController.GameMode.ENGINE_VS_ENGINE));
        for(int ply = 0; ply < 8; ply ++) {
            final FakeSearch.Pending pending = harness.search.pending;
            assertNotNull(pending);
            final long move = firstLegal(pending.board);
            onEdt(() -> harness.search.completeCurrent(managed(move, SearchTermination.COMPLETED)));
        }
        assertEquals(9, onEdt(harness.controller::historySize));
        assertEquals(9, harness.search.starts);
        assertEquals(1, harness.search.maximumConcurrent);
        onEdt(() -> harness.controller.setGameMode(GameController.GameMode.HUMAN_VS_HUMAN));
    }

    @Test
    void controllerRejectsMutationAwayFromTheEdtAndShutdownCleanupIsSeparate() throws Exception {
        final Harness harness = onEdt(Harness::new);
        assertThrows(IllegalStateException.class, harness.controller::newGame);
        final Runnable cleanup = onEdt(harness.controller::beginShutdown);
        cleanup.run();
        assertTrue(harness.search.closed);
        assertTrue(harness.view.allMutationsOnEdt);
    }

    private static final class Harness {
        private final FakeSearch search = new FakeSearch();
        private final CapturingView view = new CapturingView();
        private final GameController controller = new GameController(search, view);

        private Harness() {
            controller.initialize();
        }
    }

    private static final class CapturingView implements GameController.View {
        private final List<String> errors = new ArrayList<>();
        private GameController.PositionView position;
        private GameController.SearchInfo search;
        private Promotion promotion = Promotion.QUEEN;
        private List<Promotion> lastPromotionChoices = List.of();
        private boolean allMutationsOnEdt = true;

        @Override
        public void showPosition(GameController.PositionView position) {
            recordThread();
            this.position = position;
        }

        @Override
        public void showSearch(GameController.SearchInfo search) {
            recordThread();
            this.search = search;
        }

        @Override
        public void setSearchRunning(boolean running) {
            recordThread();
        }

        @Override
        public Promotion choosePromotion(List<Promotion> choices) {
            recordThread();
            lastPromotionChoices = List.copyOf(choices);
            return promotion;
        }

        @Override
        public void showError(String title, String message) {
            recordThread();
            errors.add(title + ": " + message);
        }

        private void recordThread() {
            allMutationsOnEdt &= SwingUtilities.isEventDispatchThread();
        }
    }

    private static final class FakeSearch implements SearchGateway {
        private record Pending(
            long[] board, GameHistory history, SearchLimits limits,
            Object token, Listener listener
        ) {}

        private final List<SearchTermination> invalidations = new ArrayList<>();
        private Pending pending;
        private int workers = 1;
        private int starts;
        private int stops;
        private int concurrent;
        private int maximumConcurrent;
        private boolean searching;
        private boolean closed;

        @Override
        public void start(
            long[] board, GameHistory history, SearchLimits limits,
            Object uiToken, Listener listener
        ) {
            if(searching) throw new AssertionError("Concurrent controller search");
            searching = true;
            starts ++;
            concurrent ++;
            maximumConcurrent = Math.max(maximumConcurrent, concurrent);
            pending = new Pending(board.clone(), history.snapshot(), limits, uiToken, listener);
        }

        @Override
        public void stop() {
            stops ++;
        }

        @Override
        public void invalidate(SearchTermination reason) {
            invalidations.add(reason);
            if(searching) concurrent --;
            searching = false;
        }

        @Override
        public boolean isSearching() {
            return searching;
        }

        @Override
        public int workerCount() {
            return workers;
        }

        @Override
        public void replaceWorkerCount(int requestedWorkers) {
            workers = requestedWorkers;
        }

        @Override
        public Runnable beginShutdown() {
            searching = false;
            concurrent = 0;
            return () -> closed = true;
        }

        private void completeCurrent(ManagedSearchResult result) {
            final Pending completed = pending;
            searching = false;
            concurrent --;
            completed.listener.onComplete(completed.token, result);
        }
    }

    private static ManagedSearchResult managed(long move, SearchTermination termination) {
        return new ManagedSearchResult(1L, move, true, null, termination, 1L, null);
    }

    private static void humanMove(GameController controller, String coordinate) {
        controller.squarePressed(square(coordinate.substring(0, 2)));
        controller.squareReleased(square(coordinate.substring(2, 4)));
    }

    private static long resolve(long[] board, String coordinate) {
        final Promotion promotion = coordinate.length() == 4 ? Promotion.NONE : switch(coordinate.charAt(4)) {
            case 'q' -> Promotion.QUEEN;
            case 'r' -> Promotion.ROOK;
            case 'b' -> Promotion.BISHOP;
            case 'n' -> Promotion.KNIGHT;
            default -> throw new AssertionError();
        };
        return new LegalMoveResolver().resolve(
            board,
            new MoveIntent(
                square(coordinate.substring(0, 2)),
                square(coordinate.substring(2, 4)),
                promotion
            )
        );
    }

    private static long firstLegal(long[] board) {
        final long[] moves = new long[256];
        final int count = Gen.genAll(
            board[0], board[1], board[2], board[3],
            (int) board[Board.STATUS], board[Board.KEY], true,
            moves, new long[Board.MAX_BITBOARDS]
        );
        if(count == 0) throw new AssertionError("Expected a legal move");
        return moves[0];
    }

    private static int square(String coordinate) {
        return (coordinate.charAt(1) - '1') * 8 + coordinate.charAt(0) - 'a';
    }

    private static void onEdt(ThrowingRunnable action) throws Exception {
        onEdt(() -> {
            action.run();
            return null;
        });
    }

    private static <T> T onEdt(Callable<T> action) throws Exception {
        if(SwingUtilities.isEventDispatchThread()) return action.call();
        final FutureTask<T> task = new FutureTask<>(action);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
