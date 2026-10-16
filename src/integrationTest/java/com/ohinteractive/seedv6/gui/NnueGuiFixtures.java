package com.ohinteractive.seedv6.gui;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.SwingUtilities;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.MoveIntent.Promotion;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.GameTermination;
import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.validation.*;
import static org.junit.jupiter.api.Assertions.*;

final class NnueGuiFixtures {
    static final String SHORT_GAME = "4k3/8/8/8/8/8/8/R3K3 w - - 98 1";

    static <T> T edt(Callable<T> action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) return action.call();
        FutureTask<T> task = new FutureTask<>(action);
        SwingUtilities.invokeAndWait(task);
        return task.get(10, TimeUnit.SECONDS);
    }
    static void edt(Runnable action) throws Exception { edt(() -> { action.run(); return null; }); }

    static void until(Callable<Boolean> condition) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (!condition.call() && System.nanoTime() < end) Thread.sleep(10);
        assertTrue(condition.call(), "Timed out waiting for GUI/lifecycle condition");
    }

    static TrainingSettings settings(Path root, int depth, long generations) {
        return new TrainingSettings(root, depth, 1, 2, 0, 0, 2, 2, 1, 2, 71, 8, generations);
    }

    static class ShortBackend extends TrainingController.Backend {
        volatile int fresh, resumed;
        @Override TrainingController.Handle create(TrainingSettings settings, boolean resume, TrainerConfig.DepthChange change) throws java.io.IOException {
            assertFalse(SwingUtilities.isEventDispatchThread());
            if (resume) resumed++; else fresh++;
            TrainerConfig c = settings.config(change);
            TrainerConfig fixture = new TrainerConfig(c.checkpointRoot(), c.masterSeed(), c.selfPlay(), c.training(),
                    c.validation(), c.maximumGenerations(), change, SHORT_GAME);
            return handle(resume ? TrainerService.resume(fixture) : TrainerService.fresh(fixture,
                    new NnueTrainer(TrainableNnue.initialized(settings.seed()))));
        }
    }

    static String bootstrap(Path root) throws Exception {
        try (var store = new CheckpointStore(root)) {
            return store.initialize(new NnueTrainer(TrainableNnue.initialized(71)), new CheckpointManifest.Metadata(0, 1, "")).manifest().id();
        }
    }

    static String candidate(Path root, boolean promote) throws Exception {
        try (var store = new CheckpointStore(root)) {
            return candidate(store, promote);
        }
    }

    static String candidate(CheckpointStore store, boolean promote) throws Exception {
        var refs = store.recover();
        var parent = refs.latestTraining().orElseThrow().manifest();
        var best = refs.best().orElseThrow().manifest();
        var trainer = store.resume(parent.id());
        trainer.trainBatch(new long[][] {Board.fromFen(SHORT_GAME)}, new double[] {0.9}, 1);
        String id = store.publish(trainer, new CheckpointManifest.Metadata(parent.generation() + 1, 1, parent.id())).manifest().id();
        ValidationConfig config = new ValidationConfig(2, 42, 0, 0, 1, 1, TrainingSettings.SCORE_MAPPING, 8);
        var w = new ValidationResult.Game(promote ? GameTermination.WHITE_CHECKMATES_BLACK : GameTermination.FIFTY_MOVE_RULE, 2);
        var b = new ValidationResult.Game(promote ? GameTermination.BLACK_CHECKMATES_WHITE : GameTermination.FIFTY_MOVE_RULE, 2);
        long[] board = Board.fromFen(SHORT_GAME);
        var result = new ValidationResult(config, ValidationArena.stateHash(board, GameHistory.initial(board)),
                Collections.nCopies(2, new ValidationResult.Pair("fixture", w, b)));
        var record = store.recordValidation(id, best.id(), result, new PromotionPolicy(1, 0.9, 0));
        if (promote) store.completePromotion(record.id());
        return id;
    }

    static class View implements GameController.View {
        volatile GameController.PositionView position;
        volatile GameController.SearchInfo search;
        volatile PlayEvaluator evaluator = PlayEvaluator.handcrafted();
        volatile boolean changing, running;
        volatile String error;
        public void showPosition(GameController.PositionView value) { assertTrue(SwingUtilities.isEventDispatchThread()); position = value; }
        public void showSearch(GameController.SearchInfo value) { assertTrue(SwingUtilities.isEventDispatchThread()); search = value; }
        public void setSearchRunning(boolean value) { running = value; }
        public Promotion choosePromotion(List<Promotion> choices) { return choices.getFirst(); }
        public void showError(String title, String message) { error = message; }
        public void showEvaluator(PlayEvaluator value, boolean pending) { evaluator = value; changing = pending; }
    }
}
