package com.ohinteractive.seedv6.training.service;

import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.driver.PinnedSearch;
import com.ohinteractive.seedv6.search.exact.FlatSearchConfig;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.training.service.LearningArenaConfig.*;

@Timeout(60)
class ConstructionArenaTest {
    @TempDir Path temporary;
    @Test void matchOnlyHarnessConstructsSelectedSearchAndPersistsIdentityAcrossResume() throws Exception {
        var candidate = Competitor.pinned(PinnedSearch.Policy.CONSTRUCTION_V1);
        var champion = Competitor.pinned(PinnedSearch.Policy.POLICY4);
        // Two two-ply fixture games exercise routing/persistence; this is not a strength match.
        var config = LearningArenaConfig.match("activation fixture", candidate, champion, 17,
                new Arena(2, Limit.DEPTH, 1, 1000, 4, 0, 0, 2, Board.FEN_STARTING_POSITION));
        Path campaign = temporary.resolve("campaign");
        try (var service = LearningArenaService.create(campaign, config, ignored -> {})) {
            for (int workers : new int[]{1, 4, 16}) {
                var factory = service.searchFactory(null, LearningArenaState.Endpoint.pinned(Evaluator.CONSTRUCTION_V1));
                try (var search = factory.apply(workers)) {
                    assertEquals(262143L, ((FlatSearchConfig) field(workers == 1 ? field(search, "exact") : search, "construction")).flags());
                }
            }
            service.run();
            assertEquals(LearningArenaState.Status.COMPLETE, service.state().status());
            assertEquals(Evaluator.CONSTRUCTION_V1, service.state().current().a().evaluator());
            assertEquals(Evaluator.POLICY4_PAIR2, service.state().current().b().evaluator());
        }
        assertFalse(Files.exists(campaign.resolve("A/checkpoints")));
        try (var resumed = LearningArenaService.resume(campaign, ignored -> {})) {
            assertEquals(config.identity(), resumed.state().binding());
            resumed.run(); assertEquals(LearningArenaState.Status.COMPLETE, resumed.state().status());
        }
        assertThrows(IllegalArgumentException.class, () -> new Competitor("bad", com.ohinteractive.seedv6.training.model.TrainingArchitecture.NNUE,
                0, 1, null, null, null, Evaluator.CONSTRUCTION_V1));
    }
    private static Object field(Object object, String name) throws Exception {
        var field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
}
