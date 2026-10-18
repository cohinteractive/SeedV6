package com.ohinteractive.seedv6.training.selfplay;

import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.service.BrnCaptureConsistency;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IncrementalGeneratedTest {
    @Test void completedGamesStreamWithUpfrontHoldoutWithoutSearchOrOutcomeDependentSelection() throws Exception {
        int games = 1024; long split = 71;
        var config = new SelfPlayConfig(games, 1, 1, 17, 0, 0, 4, 8, NnueScoreMapping.V1, -1, -1);
        var control = new SelfPlayControl(); var completed = new AtomicInteger(); var first = new AtomicInteger(-1);
        var ids = new TreeSet<Integer>(); var fixture = HeadlessGameTest.foolsMate().trajectory();
        var actor = NnueNetwork.initialized(1);
        var state = NetworkTrainingState.initialized(TrainingArchitecture.BRN2, 71, .001);
        try (var workspace = new SampleStore.Workspace()) {
            var result = IncrementalTraining.train(state, state.snapshot(), new SelfPlayTraining.Config(1, 1, true, 19), control, sink -> {
                control.acquiredSamples((game, sample) -> {
                    if (!BootstrapPartition.heldOutGame(game, games, split)) { ids.add(game); sink.accept(sample); }
                });
                try {
                    return SelfPlayBatch.generate(actor, config, control, game -> fixture, p -> completed.incrementAndGet());
                } finally { control.acquiredSamples(null); }
            }, sample -> new IncrementalTraining.Prepared(sample, null), BrnCaptureConsistency.OFF,
                    p -> { if (p.optimizerUpdates() == 1) first.set(completed.get()); }, s -> {});
            assertTrue(first.get() >= 0 && first.get() < games);
            var partition = BootstrapPartition.splitAssigned(result.source(), split);
            assertEquals((games + 4) / 5, partition.heldOutGames().size());
            assertEquals(ids, new TreeSet<>(partition.trainingGames()));
            assertTrue(Collections.disjoint(ids, partition.heldOutGames()));
            assertEquals(partition.training().size(), result.samples().size());
            assertEquals(partition.training().size(), state.step());
            for (int i = 0; i < result.samples().size(); i++) {
                assertArrayEquals(partition.training().get(i).board(), result.samples().get(i).board());
                assertEquals(partition.training().get(i).target(), result.samples().get(i).target());
            }
        }
    }
}
