package com.ohinteractive.seedv6.tools.nnue.research;

import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.training.checkpoint.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class RetentionResearchTest {
    @TempDir Path temporary;

    @Test void recoveryAndProgressionVerifyMetadataOnlyAncestorsWithoutInventingMeasurements() throws Exception {
        Path root = temporary.resolve("store"), template = temporary.resolve("template");
        RetentionFixture.template(template);
        String bootstrap, previous, best, pruned, latest;
        try (var store = new CheckpointStore(root)) {
            var fixture = new RetentionFixture(root, template);
            var first = fixture.add(1, ""); bootstrap = first.id(); var accepted = fixture.bootstrap(first);
            var second = fixture.add(2, first.id()); previous = second.id(); accepted = fixture.accept(second, accepted);
            var third = fixture.add(3, second.id()); best = third.id(); fixture.accept(third, accepted);
            var sample = fixture.add(10, third.id()); fixture.validation(sample.id(), best, false);
            var old = fixture.add(11, sample.id()); pruned = old.id(); fixture.validation(old.id(), best, false);
            var last = fixture.add(120, old.id()); latest = last.id();
            CandidateLifecycle.completeDecision(store, fixture.validation(latest, best, false));
            assertThrows(CheckpointPrunedException.class, () -> store.load(bootstrap));
            assertEquals(best, CheckpointStore.readBestSnapshot(root).manifest().id());
        }
        assertEquals(latest, ResearchTool.recover(root).id());
        assertEquals(Set.of(bootstrap, previous, best), CheckpointInspection.accepted(root));
        List<ProgressionAnalysis.Row> rows = new ArrayList<>();
        ProgressionAnalysis.analyze(root, 7, 1, rows::add);
        assertEquals(6, rows.size());
        assertEquals(bootstrap, rows.getFirst().checkpoint().id());
        assertTrue(rows.getFirst().health().isEmpty());
        assertTrue(rows.stream().filter(r -> r.checkpoint().id().equals(pruned)).findFirst().orElseThrow().health().isEmpty());
        assertTrue(rows.getLast().health().isPresent());
        assertTrue(rows.getLast().fromParent().isEmpty(), "A sparse predecessor is not the actual parent.");
        assertTrue(rows.stream().allMatch(r -> r.fromBootstrap().isEmpty()), "Never substitute a later checkpoint for pruned bootstrap.");
    }
}
