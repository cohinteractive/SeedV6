package com.ohinteractive.seedv6.training.service;

import java.time.Duration;
import java.util.function.Consumer;
import com.ohinteractive.seedv6.core.brn2.Brn2Trainer;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.diagnostics.RootSearchObservation;
import com.ohinteractive.seedv6.search.driver.SearchDriverOutcome;
import com.ohinteractive.seedv6.training.model.NetworkModel;
import com.ohinteractive.seedv6.training.model.NetworkTrainingState;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.telemetry.ActiveGameFeed;
import com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot;
import com.ohinteractive.seedv6.training.validation.*;

/** Diagnostic adapter around the unmodified production lifecycle and its existing operations seam. */
public final class BrnDiagnosticTraining {
    public interface Listener {
        void phase(TrainerSnapshot snapshot);
        void game(SelfPlayBatch.Progress progress);
        void started(ActiveGameSnapshot game, SearchRequest request);
        void searched(ActiveGameSnapshot game, SearchRequest request, SearchDriverOutcome outcome, long nanos);
    }

    public static TrainerSnapshot run(TrainerConfig config, Brn2Trainer initial, Listener listener) throws Exception {
        // This entry point deliberately offers no resume/teacher/store discovery mechanism.
        if (config.architecture() != com.ohinteractive.seedv6.training.model.TrainingArchitecture.BRN2
                || config.source() == null || config.source().nnue() || config.source().frozen()
                || !BrnSupervision.WDL.equals(config.supervision()) || config.effectiveCaptureConsistency().enabled()
                || config.teacherStore() == null || !config.teacherStore().isEmpty() || !config.frozenReplayHash().isEmpty()
                || config.maximumGenerations() < 1 || config.maximumRunMillis() != 0)
            throw new IllegalArgumentException("Diagnostics require bounded virgin BRN-2 WDL training without external stores or time limits.");
        if (java.nio.file.Files.exists(config.checkpointRoot()))
            throw new IllegalArgumentException("Diagnostic training requires a nonexistent, privately allocated store.");
        var operations = new TrainerService.Operations() {
            private RootSearchObservation scope(ActiveGameFeed feed) {
                return RootSearchObservation.observe(new RootSearchObservation.Listener() {
                    public void started(SearchRequest request) { listener.started(feed.latest(), request); }
                    public void finished(SearchRequest request, SearchDriverOutcome outcome, long nanos) {
                        listener.searched(feed.latest(), request, outcome, nanos);
                    }
                });
            }
            @Override SelfPlayBatch generate(NetworkModel actor, SelfPlayConfig c, long[] board,
                    SelfPlayControl control, Consumer<SelfPlayBatch.Progress> progress) {
                try (var scope = scope(control.presentation())) {
                    return super.generate(actor, c, board, control, p -> { progress.accept(p); listener.game(p); });
                }
            }
            @Override SelfPlayBatch generateHandcrafted(SelfPlayConfig c, long[] board,
                    SelfPlayControl control, Consumer<SelfPlayBatch.Progress> progress) {
                try (var scope = scope(control.presentation())) {
                    return super.generateHandcrafted(c, board, control, p -> { progress.accept(p); listener.game(p); });
                }
            }
            @Override ValidationResult validate(NetworkModel candidate, NetworkModel incumbent, ValidationConfig c,
                    long[] board, GameHistory history, ValidationControl control, Consumer<ValidationProgress> progress) {
                try (var scope = scope(control.presentation())) {
                    return super.validate(candidate, incumbent, c, board, history, control, progress);
                }
            }
        };
        try (var service = TrainerService.fresh(config, new NetworkTrainingState.Brn2(initial), operations, listener::phase)) {
            service.start();
            while (!service.awaitTermination(Duration.ofMillis(250))) { /* no timeout changes production work */ }
            if (service.failure().isPresent()) throw new IllegalStateException("Diagnostic training failed", service.failure().get());
            var result = service.snapshot();
            if (result.totals().completedGenerations() != config.maximumGenerations())
                throw new IllegalStateException("Diagnostic training ended before requested generations: " + result.state());
            return result;
        }
    }
    private BrnDiagnosticTraining() {}
}
