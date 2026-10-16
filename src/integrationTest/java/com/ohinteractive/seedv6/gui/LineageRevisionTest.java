package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.ModelLibrary;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LineageRevisionTest {
    @TempDir Path root;
    @Test void configurationRevisionsAreAdditiveIdempotentAndAllowedBeforeBootstrap() throws Exception {
        var entry = LineageRedesignTest.fixture(root, NetworkArchitecture.BRN2, "History");
        var selected = TrainingLineages.read(entry);
        TrainingLineages.save(selected, selected.settings().withLearningRate(.004));
        assertTrue(CheckpointInspection.freshRoot(entry.root(), selected.lineage().architecture()));
        var first = LineageRevision.read(entry.root()); assertEquals(1, first.size());
        assertEquals(selected.lineage(), first.getFirst().lineage());
        var saved = TrainingLineages.read(entry);
        TrainingLineages.save(saved, saved.settings()); assertEquals(first, LineageRevision.read(entry.root()));
        TrainingLineages.save(saved, saved.settings().withLearningRate(.008));
        var next = LineageRevision.read(entry.root()); assertEquals(2, next.size()); assertEquals(first.getFirst(), next.getFirst());
        var config = LineageConfiguration.decode(next.getLast().lineage().configuration(), entry.root(), entry.architecture());
        assertEquals(.004, config.recipeLearningRate());
        var view = ModelLibrary.browse(ModelLibrary.identify(entry.root()));
        assertEquals(2, view.revisions().size()); assertTrue(view.provenance().isEmpty());
        assertEquals("Cumulative sampled positions: not recorded", view.exposureDescription());
        assertTrue(view.diagnostics().isEmpty(), view.diagnostics().toString());
    }
    @Test void corruptedRevisionNeverBecomesAcceptedFreshStoreMetadata() throws Exception {
        var entry = LineageRedesignTest.fixture(root, NetworkArchitecture.BRN2, "History");
        var selected = TrainingLineages.read(entry); TrainingLineages.save(selected, selected.settings().withLearningRate(.004));
        Path file;
        try (var files = Files.list(entry.root().resolve(LineageRevision.DIRECTORY))) { file = files.findFirst().orElseThrow(); }
        byte[] bytes = Files.readAllBytes(file); bytes[20] ^= 1; Files.write(file, bytes);
        assertThrows(java.io.IOException.class, () -> TrainingLineages.read(entry));
        assertArrayEquals(bytes, Files.readAllBytes(file));
    }
}
