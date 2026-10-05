package com.ohinteractive.seedv6.training.data;

import com.ohinteractive.seedv6.corpus.CorpusPreparation;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import com.ohinteractive.seedv6.training.service.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class TrainingDataLibraryTest {
    @TempDir Path temporary;
    @Test void sharedRegistrationPreservesSavedMixesCampaignBindingsAndIndependentCursors() throws Exception {
        var library = new TrainingDataLibrary(temporary.resolve("models"));
        assertTrue(library.browse().sources().isEmpty()); assertFalse(Files.exists(library.directory()));
        var source = DataSource.register("Lichess", Files.writeString(temporary.resolve("source.jsonl"), SourceReadersTest.line(100)), 7);
        Path first = temporary.resolve("first"), second = temporary.resolve("second");
        var mix = new DataSources(1, List.of(source), false); mix.save(DataSources.directory(first)); mix.save(DataSources.directory(second));
        var config = new LearningArenaConfig("Historical campaign", new LearningArenaConfig.Competitor("one", TrainingArchitecture.BRN3, 71, 2),
                new LearningArenaConfig.Competitor("two", TrainingArchitecture.BRN3, 72, 2), source, 1, 1, 1, 71,
                new LearningArenaConfig.Arena(2, LearningArenaConfig.Limit.DEPTH, 1, 100, 1, 0, 0, 4, TrainerConfig.STANDARD_START));
        String identity = config.identity(), original = Files.readString(DataSources.directory(first).resolve("sources.json"));
        var registered = library.remember(source); assertEquals(1, registered.weight());
        library.register(source.withDisplay("Reusable corpus", 1)); library.remember(source);
        assertEquals("Reusable corpus", library.browse().sources().getFirst().name());
        assertEquals(1, library.browse().sources().size()); assertEquals(identity, config.identity());
        assertEquals(original, Files.readString(DataSources.directory(first).resolve("sources.json")));
        var ledger = new SourceLedger(first);
        ledger.reserve(1, "attempt", mix.identity(), List.of(new SourceLedger.Range(source.identity(), 0, 1, 1, 0, 0)), "a".repeat(64), "b".repeat(64));
        assertEquals(1, new SourceLedger(first).next(source.identity())); assertEquals(0, new SourceLedger(second).next(source.identity()));
        assertFalse(Files.exists(library.directory().resolve("cursors.json")));
        assertTrue(TrainingDataLibrary.inspect(registered).ready());
        Files.delete(source.path());
        assertFalse(TrainingDataLibrary.inspect(library.browse().sources().getFirst()).ready());
        assertEquals(1, library.browse().sources().size(), "Missing backing data remains visible");
    }
    @Test void binpackPreparationIsReusedAndCapabilityIsExplicit() throws Exception {
        String before = System.getProperty("seedv6.preparedDataRoot");
        System.setProperty("seedv6.preparedDataRoot", temporary.resolve("prepared").toString());
        try {
            var source = BinpackFixtures.source(temporary.resolve("bt4")); var library = new TrainingDataLibrary(temporary.resolve("models"));
            library.register(source); assertFalse(TrainingDataLibrary.inspect(source).ready());
            PreparedBinpack.prepare(source, CorpusPreparation.NONE);
            var saved = library.browse().sources().getFirst(); assertTrue(TrainingDataLibrary.inspect(saved).ready());
            assertEquals(16, TrainingDataLibrary.inspect(saved).positions());
            assertEquals(PreparedBinpack.directory(source), PreparedBinpack.directory(saved));
            assertEquals("", TrainingDataLibrary.incompatibility(saved, List.of(TrainingArchitecture.NNUE_MATERIAL, TrainingArchitecture.BRN3)));
            assertTrue(TrainingDataLibrary.incompatibility(saved, List.of(TrainingArchitecture.BRN2)).contains("requires scaled centipawn targets"));
        } finally { if (before == null) System.clearProperty("seedv6.preparedDataRoot"); else System.setProperty("seedv6.preparedDataRoot", before); }
    }
    @Test void corruptCatalogRecordsAreVisibleAndNotSilentlyReplaced() throws Exception {
        var library = new TrainingDataLibrary(temporary); var source = DataSource.register("Data", Files.writeString(temporary.resolve("data.jsonl"), "{}"), 1);
        library.register(source); Path record = library.directory().resolve(source.identity() + ".json"); Files.writeString(record, "broken");
        assertFalse(library.browse().diagnostics().isEmpty()); assertTrue(library.browse().sources().isEmpty());
        assertThrows(java.io.IOException.class, () -> library.remember(source)); assertEquals("broken", Files.readString(record));
    }
}
