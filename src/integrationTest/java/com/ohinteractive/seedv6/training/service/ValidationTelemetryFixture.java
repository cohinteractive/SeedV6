package com.ohinteractive.seedv6.training.service;

import java.io.IOException;
import java.util.function.Consumer;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.training.nnue.NnueTrainer;
import com.ohinteractive.seedv6.training.validation.*;

/** Test-only move-boundary gate around the real arena and real TrainerService publication. */
public final class ValidationTelemetryFixture {
    public static TrainerService fresh(TrainerConfig config, NnueTrainer initial, Consumer<ValidationProgress> afterPublication) throws IOException {
        return TrainerService.fresh(config, initial, operations(afterPublication), ignored -> {});
    }
    static TrainerService.Operations operations(Consumer<ValidationProgress> afterPublication) {
        return new TrainerService.Operations() {
            @Override ValidationResult validate(NnueNetwork candidate, NnueNetwork incumbent, ValidationConfig config,
                    long[] board, GameHistory history, ValidationControl control, Consumer<ValidationProgress> observer) {
                return super.validate(candidate, incumbent, config, board, history, control, progress -> {
                    observer.accept(progress);
                    afterPublication.accept(progress);
                });
            }
        };
    }
    private ValidationTelemetryFixture() {}
}
