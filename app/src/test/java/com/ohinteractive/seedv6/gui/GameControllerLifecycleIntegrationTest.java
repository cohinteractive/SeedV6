package com.ohinteractive.seedv6.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.move.MoveIntent.Promotion;

class GameControllerLifecycleIntegrationTest {

    @Test
    void boundedSelfPlayRunsThroughOneRealLifecycleSequentially() throws Exception {
        final RealView view = new RealView(6);
        final GameController controller = onEdt(() -> {
            final GameController created = new GameController(new EngineSearchAdapter(2), view);
            created.initialize();
            created.setSearchSettings(new GameController.SearchSettings(
                GameController.LimitKind.DEPTH, 1, 1_000L
            ));
            created.setGameMode(GameController.GameMode.ENGINE_VS_ENGINE);
            return created;
        });
        assertTrue(view.targetMoves.await(15L, TimeUnit.SECONDS));
        onEdt(() -> controller.setGameMode(GameController.GameMode.HUMAN_VS_HUMAN));
        if(view.failure.get() != null) throw new AssertionError(view.failure.get());

        final int plies = onEdt(() -> controller.displayedMoves().size());
        assertTrue(plies >= 6 && plies <= 7, "plies=" + plies);
        assertEquals(plies + 1, onEdt(controller::historySize));
        assertFalse(onEdt(controller::positionStatus).terminal());

        final Runnable cleanup = onEdt(controller::beginShutdown);
        cleanup.run();
    }

    private static final class RealView implements GameController.View {
        private final CountDownLatch targetMoves;
        private final AtomicReference<String> failure = new AtomicReference<>();
        private final int target;

        private RealView(int target) {
            this.target = target;
            targetMoves = new CountDownLatch(target);
        }

        @Override
        public void showPosition(GameController.PositionView position) {
            if(!SwingUtilities.isEventDispatchThread()) failure.compareAndSet(null, "Position off EDT");
            while(targetMoves.getCount() > Math.max(0, target - position.moves().size())) {
                targetMoves.countDown();
            }
        }

        @Override
        public void showSearch(GameController.SearchInfo search) {
            if(!SwingUtilities.isEventDispatchThread()) failure.compareAndSet(null, "Search info off EDT");
        }

        @Override
        public void setSearchRunning(boolean running) {
            if(!SwingUtilities.isEventDispatchThread()) failure.compareAndSet(null, "Control mutation off EDT");
        }

        @Override
        public Promotion choosePromotion(List<Promotion> choices) {
            throw new AssertionError("Self-play must not ask the view to resolve engine moves");
        }

        @Override
        public void showError(String title, String message) {
            failure.compareAndSet(null, title + ": " + message);
            while(targetMoves.getCount() > 0L) targetMoves.countDown();
        }
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
