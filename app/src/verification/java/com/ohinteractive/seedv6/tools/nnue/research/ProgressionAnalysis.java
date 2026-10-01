package com.ohinteractive.seedv6.tools.nnue.research;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.training.checkpoint.*;

/** Retains bootstrap + previous + current matrices only; consumers receive metrics, never networks. */
public final class ProgressionAnalysis {
    public record Row(CheckpointManifest checkpoint, String bootstrapId, boolean best, boolean latest,
                      boolean accepted, Optional<ValidationRecord> validation, List<String> researchMeasurements,
                      Optional<NetworkHealth.Distance> fromParent, Optional<NetworkHealth.Distance> fromBootstrap,
                      Optional<NetworkHealth.Report> health) {}
    @FunctionalInterface interface Loader { NnueNetwork load(Path directory) throws IOException; }
    public static void analyze(Path root, long seed, int positions, Consumer<Row> sink) throws IOException {
        analyze(root, NetworkHealth.corpus(seed, positions), sink, p -> CheckpointStore.inspect(p).network());
    }
    static void analyze(Path root, List<long[]> corpus, Consumer<Row> sink, Loader loader) throws IOException {
        String best = CheckpointInspection.reference(root, "best"), latest = CheckpointInspection.reference(root, "latest-training");
        var lineage = CheckpointInspection.lineage(root, latest);
        var validations = CheckpointInspection.validations(root);
        var accepted = CheckpointInspection.accepted(root);
        List<String> measurements = Files.exists(root.resolve("generations.tsv")) ? Files.readAllLines(root.resolve("generations.tsv")) : List.of();
        List<String> service = Files.exists(root.resolve("service.tsv")) ? Files.readAllLines(root.resolve("service.tsv")) : List.of();
        NnueNetwork bootstrap = null, parent = null;
        String bootstrapId = lineage.getFirst().id();
        for (var manifest : lineage) {
            NnueNetwork current;
            try { current = loader.load(root.resolve("checkpoints").resolve(manifest.id())); }
            catch (CheckpointPrunedException pruned) { current = null; }
            if (manifest.id().equals(bootstrapId)) bootstrap = current;
            List<String> recorded = new ArrayList<>();
            measurements.stream().filter(s -> {
                String[] columns = s.split("\t");
                return columns.length > 1 && columns[1].equals(manifest.id());
            }).forEach(recorded::add);
            service.stream().filter(s -> s.contains("latestTrainingId=" + manifest.id() + ",")
                    && s.contains(" generation=" + manifest.generation() + ","))
                    .reduce((first, last) -> last).ifPresent(recorded::add);
            sink.accept(new Row(manifest, bootstrapId, manifest.id().equals(best), manifest.id().equals(latest),
                    accepted.contains(manifest.id()), Optional.ofNullable(validations.get(manifest.id())),
                    List.copyOf(recorded),
                    current == null || (parent == null && !manifest.id().equals(bootstrapId)) ? Optional.empty()
                            : Optional.of(NetworkHealth.distance(parent == null ? current : parent, current)),
                    current == null || bootstrap == null ? Optional.empty() : Optional.of(NetworkHealth.distance(bootstrap, current)),
                    current == null ? Optional.empty() : Optional.of(NetworkHealth.analyze(current, corpus))));
            parent = current;
        }
    }
    private ProgressionAnalysis() {}
}
