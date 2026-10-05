package com.ohinteractive.seedv6.training.checkpoint;

import com.ohinteractive.seedv6.core.brn2.Brn2Trainer;
import com.ohinteractive.seedv6.training.model.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(60)
class ModelLibraryTest {
    @TempDir Path temp;
    final TrainingArchitecture architecture = TrainingArchitecture.BRN2;

    TrainingLineage lineage(String name) {
        return new TrainingLineage(UUID.randomUUID(), name, architecture, Instant.parse("2026-10-04T00:00:00Z"), "", "Fixture");
    }
    CheckpointStore.Checkpoint publish(CheckpointStore store, long generation, String parent, boolean best) throws IOException {
        var state = new NetworkTrainingState.Brn2(new Brn2Trainer(.001));
        var metadata = new CheckpointManifest.Metadata(generation, 1, parent);
        return best ? store.initialize(state, metadata) : store.publish(state, metadata);
    }
    Map<String, String> hashes(Path root) throws IOException {
        var result = new TreeMap<String, String>();
        try (var paths = Files.walk(root)) {
            for (Path p : paths.filter(Files::isRegularFile).toList()) result.put(root.relativize(p).toString(), SmallRecord.hash(p));
        }
        return result;
    }

    @Test void discoveryReusesManagedAndExternalLineagesWithoutDependingOnDisplayNames() throws Exception {
        var first = lineage("Same name");
        var second = lineage("Same name");
        Path managed = temp.resolve(architecture.folderName()).resolve(first.id().toString());
        Path external = temp.resolve("external");
        try (var store = new CheckpointStore(managed, architecture)) { store.writeLineage(first); }
        try (var store = new CheckpointStore(external, architecture)) { store.writeLineage(second); }
        var before = hashes(temp);
        var entries = ModelLibrary.discover(temp, architecture, List.of(external, managed, temp.resolve("missing")));
        assertEquals(3, entries.size());
        assertEquals(2, entries.stream().filter(e -> e.name().equals("Same name")).count());
        assertEquals(Set.of(first.id(), second.id()), entries.stream().flatMap(e -> e.lineage().stream()).map(TrainingLineage::id).collect(java.util.stream.Collectors.toSet()));
        assertTrue(entries.stream().anyMatch(e -> !e.diagnostic().isEmpty()));
        assertEquals(before, hashes(temp));
        assertThrows(IOException.class, () -> ModelLibrary.entry(managed, TrainingArchitecture.NNUE));
    }

    @Test void publishOnlyCampaignGenerationLoadsWithoutBestAndBrowsingDoesNotWriteLegacyStores() throws Exception {
        Path root = temp.resolve("campaign-competitor");
        CheckpointStore.Checkpoint first, second;
        try (var store = new CheckpointStore(root, architecture)) {
            first = publish(store, 0, "", false); second = publish(store, 1, first.manifest().id(), false);
        }
        Files.delete(root.resolve("payload.lock"));
        var before = hashes(root);
        var entry = ModelLibrary.entry(root, architecture);
        var snapshot = ModelLibrary.browse(entry);
        assertEquals(before, hashes(root));
        assertTrue(entry.lineage().isEmpty(), "Do not invent historical UUID or creation time while browsing");
        assertTrue(snapshot.best().isEmpty());
        assertEquals(second.manifest().id(), snapshot.latest().orElseThrow().id());
        assertEquals(List.of(1L, 0L), snapshot.generations().stream().map(ModelLibrary.Generation::number).toList());
        assertTrue(snapshot.generations().stream().allMatch(g -> g.history().isEmpty()));
        var resolved = ModelLibrary.resolve(entry, first.manifest().id());
        assertEquals(first.manifest(), resolved.checkpoint().manifest());
        assertEquals("BRN-2 · campaign-competitor · Gen 0", resolved.binding().description());
        assertThrows(IOException.class, () -> ModelLibrary.resolve(entry, ""));
        assertFalse(Files.exists(root.resolve("refs/best")));
    }

    @Test void advisoryCatalogDoesNotPretendToVerifyPayloadButSelectedLoadDoes() throws Exception {
        Path root = temp.resolve("corrupt"); CheckpointStore.Checkpoint checkpoint;
        try (var store = new CheckpointStore(root, architecture)) { checkpoint = publish(store, 0, "", true); }
        var entry = ModelLibrary.entry(root, architecture);
        Path payload = root.resolve("checkpoints").resolve(checkpoint.manifest().id()).resolve(checkpoint.manifest().networkFile());
        byte[] bytes = Files.readAllBytes(payload); bytes[bytes.length - 1] ^= 1; Files.write(payload, bytes);
        assertEquals(1, ModelLibrary.browse(entry).generations().size(), "Catalog verifies metadata and length only");
        assertThrows(IOException.class, () -> ModelLibrary.resolve(entry, checkpoint.manifest().id()));
        Files.delete(payload);
        var broken = ModelLibrary.browse(entry);
        assertTrue(broken.generations().isEmpty()); assertFalse(broken.diagnostics().isEmpty());
    }

    @Test void annotationRoundTripPreservesImmutableEvidenceAndRejectsStaleOrCorruptEdits() throws Exception {
        Path root = temp.resolve("notes"); String id;
        try (var store = new CheckpointStore(root, architecture)) {
            store.writeLineage(lineage("Research model")); id = publish(store, 0, "", true).manifest().id();
        }
        var before = hashes(root);
        var annotation = new GenerationAnnotation(id, "Performed strongly against g12", List.of("interesting", "against g12", "interesting"), Instant.now());
        try (var store = new CheckpointStore(root, architecture)) { store.writeAnnotation(annotation, Optional.empty()); }
        assertEquals(annotation, GenerationAnnotation.read(root, id).orElseThrow());
        assertEquals(List.of("interesting", "against g12"), annotation.tags());
        var snapshot = ModelLibrary.browse(ModelLibrary.entry(root, architecture));
        assertTrue(snapshot.generations().getFirst().label().contains("Best · Latest · interesting"));
        before.forEach((file, hash) -> assertEquals(hash, assertDoesNotThrow(() -> SmallRecord.hash(root.resolve(file))), file));
        try (var store = new CheckpointStore(root, architecture)) {
            assertThrows(IOException.class, () -> store.writeAnnotation(annotation, Optional.empty()));
            var edited = new GenerationAnnotation(id, "Revised", List.of(), Instant.now());
            store.writeAnnotation(edited, Optional.of(annotation));
            Path file = GenerationAnnotation.file(root, id); byte[] bad = Files.readAllBytes(file); bad[bad.length - 1] ^= 1; Files.write(file, bad);
            assertThrows(IOException.class, () -> store.writeAnnotation(annotation, Optional.of(edited)));
            assertArrayEquals(bad, Files.readAllBytes(file));
        }
        assertTrue(ModelLibrary.browse(ModelLibrary.entry(root, architecture)).diagnostics().stream().anyMatch(d -> d.contains("annotation")));
        assertThrows(IllegalArgumentException.class, () -> GenerationAnnotation.read(root, "../outside"));
    }

    @Test void exactLineageIdentityCannotBeSilentlyReplacedAfterSelection() throws Exception {
        Path root = temp.resolve("replaced");
        try (var store = new CheckpointStore(root, architecture)) {
            store.writeLineage(lineage("Original")); publish(store, 0, "", true);
        }
        var entry = ModelLibrary.entry(root, architecture);
        Files.write(root.resolve(TrainingLineage.FILE), lineage("Replacement").encode());
        assertThrows(IOException.class, () -> ModelLibrary.resolve(entry, ""));
    }
}
