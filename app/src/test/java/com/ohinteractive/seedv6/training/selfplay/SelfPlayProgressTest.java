package com.ohinteractive.seedv6.training.selfplay;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import static org.junit.jupiter.api.Assertions.*;

class SelfPlayProgressTest {
    @Test void observerFailureStopsBatchBeforeStartingAnotherGame() {
        var actor = NnueNetwork.initialized(1);
        var config = new SelfPlayConfig(3, 1, 1, 0, 0, 0, 2, 8, new NnueScoreMapping(1), -1, -1);
        var played = new java.util.concurrent.atomic.AtomicInteger();
        assertThrows(IllegalStateException.class, () -> SelfPlayBatch.generate(actor, config, new SelfPlayControl(), index -> {
            played.incrementAndGet(); return HeadlessGameTest.foolsMate().trajectory();
        }, progress -> { throw new IllegalStateException("infrastructure failure observed"); }));
        assertEquals(1, played.get());
    }

    @Test void incrementalAggregatesRemainImmutableAndCancellationStartsNoMoreGames() {
        var actor = NnueNetwork.initialized(1);
        var config = new SelfPlayConfig(3, 1, 1, 0, 0, 0, 2, 8, new NnueScoreMapping(1), -1, -1);
        var control = new SelfPlayControl();
        List<SelfPlayBatch.Progress> snapshots = new ArrayList<>();
        var batch = SelfPlayBatch.generate(actor, config, control, index -> HeadlessGameTest.foolsMate().trajectory(), progress -> {
            snapshots.add(progress);
            if (snapshots.size() == 2) control.cancel();
        });
        assertEquals(1, snapshots.get(0).statistics().completedGames());
        assertEquals(2, snapshots.get(1).statistics().completedGames());
        assertEquals(batch.statistics(), snapshots.get(1).statistics());
        assertEquals(4, batch.samples().size()); assertEquals(1, batch.unstartedGames());
    }

    @Test void infrastructureFailureAfterTerminalGameDoesNotSupplySamplesOrFalseWin() {
        var game = HeadlessGameTest.foolsMate();
        assertTrue(game.termination().completed());
        var failed = SelfPlayRunner.infrastructureFailure(game, new IllegalStateException("search close failed"));
        assertEquals(GameTermination.INFRASTRUCTURE_FAILURE, failed.termination());
        assertTrue(failed.result().isEmpty()); assertTrue(failed.failure().orElseThrow().contains("search close failed"));
        assertThrows(IllegalArgumentException.class, () -> TrajectorySampler.sample(failed, 2));
    }
}
