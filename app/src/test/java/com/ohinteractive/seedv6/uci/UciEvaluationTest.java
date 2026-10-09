package com.ohinteractive.seedv6.uci;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;
import static org.junit.jupiter.api.Assertions.*;

class UciEvaluationTest {
    @TempDir Path root;

    @Test void researchIsExplicitRejectsUnknownPoliciesAndRequiresPair2() {
        assertEquals(-1, UciEvaluation.load(new Properties()).qpolicy());
        var properties = new Properties();
        properties.setProperty(UciEvaluation.PREFIX + "qsearchResearch", "see-promo");
        assertThrows(IllegalArgumentException.class, () -> UciEvaluation.load(properties));
        assertThrows(IllegalArgumentException.class, () -> UciEvaluation.researchPolicy("typo"));
        assertEquals(2, UciEvaluation.researchPolicy("see-promo"));
        var selected = new UciEvaluation(SearchEvaluation.handcraftedIsolation(), "test", 2);
        assertThrows(IllegalArgumentException.class, () -> selected.service(2));
        try(var service = selected.service(1)) { assertNotNull(service); }
    }

    @Test void defaultIsUnchangedAndPartialConfigurationCannotFallBack() {
        assertSame(SearchEvaluation.handcrafted(), UciEvaluation.load(new Properties()).evaluation());
        var props = new Properties();
        props.setProperty(UciEvaluation.PREFIX + "checkpoint", "missing");
        assertThrows(IllegalArgumentException.class, () -> UciEvaluation.load(props));
        props.setProperty(UciEvaluation.PREFIX + "pair2Store", root.toString());
        assertThrows(IllegalArgumentException.class, () -> UciEvaluation.load(props));
        props.setProperty(UciEvaluation.PREFIX + "networkSha256", "0".repeat(64));
        assertThrows(IllegalArgumentException.class, () -> UciEvaluation.load(props));
    }

    @Test void bestAndExactSelectionBindVerifiedPayloadAndRejectMismatchOrCorruption() throws Exception {
        CheckpointStore.Checkpoint best;
        CheckpointStore.Checkpoint latest;
        var trainer = (NetworkTrainingState.BrnPair2) NetworkTrainingState.initialized(TrainingArchitecture.BRN_PAIR2, 1, .01);
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN_PAIR2)) {
            best = store.initialize(trainer, new CheckpointManifest.Metadata(0, 1, ""));
            trainer.trainer().trainBatch(new long[][] {Board.startingPosition()}, new double[] {.8}, 1);
            latest = store.publish(trainer, new CheckpointManifest.Metadata(1, 1, best.manifest().id()));
        }
        var props = new Properties();
        props.setProperty(UciEvaluation.PREFIX + "pair2Store", root.toString());
        props.setProperty(UciEvaluation.PREFIX + "networkSha256", best.manifest().networkSha256());
        props.setProperty(UciEvaluation.PREFIX + "warmupDepth", "1");
        assertTrue(UciEvaluation.load(props).identity().contains(best.manifest().id()));
        props.setProperty(UciEvaluation.PREFIX + "checkpoint", latest.manifest().id());
        assertThrows(IllegalArgumentException.class, () -> UciEvaluation.load(props));
        props.setProperty(UciEvaluation.PREFIX + "networkSha256", latest.manifest().networkSha256());
        var loaded = UciEvaluation.load(props);
        assertTrue(loaded.identity().contains(latest.manifest().id()));
        var state = loaded.evaluation().newState(2);
        var expected = latest.model().evaluation(com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping.V1).newState(2);
        assertEquals(expected.evaluate(Board.startingPosition(), 0), state.evaluate(Board.startingPosition(), 0));
        Path file = root.resolve("checkpoints").resolve(latest.manifest().id()).resolve("network.brn-pair2");
        byte[] bytes = Files.readAllBytes(file); bytes[100] ^= 1; Files.write(file, bytes);
        assertThrows(IllegalArgumentException.class, () -> UciEvaluation.load(props));
    }
}
