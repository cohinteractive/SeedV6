package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.history.*;
import java.nio.file.*;

/** Catalog-time metadata only. Never loads a network and never runs from a renderer or poll. */
final class LineageSummary {
    static String read(TrainingLineages.Entry entry) {
        try {
            var metadata = TrainingLineage.read(entry.root());
            var history = new HistoryRepository(entry.root()).refresh();
            var records = history.records();
            var last = records.isEmpty() ? null : records.getLast();
            String best = "Unavailable", latest = "None", status = "Empty";
            long settled = records.size();
            Path ref = entry.root().resolve("refs/latest-training");
            if (Files.exists(ref)) {
                var id = CheckpointInspection.reference(entry.root(), "latest-training");
                var manifest = CheckpointInspection.manifest(entry.root().resolve("checkpoints").resolve(id));
                latest = "Gen " + manifest.generation();
                status = Files.isRegularFile(entry.root().resolve("checkpoints").resolve(id).resolve(entry.architecture().trainingArchitecture().networkFile()))
                        ? "Available" : "Checkpoint unavailable";
                var decisions = CheckpointInspection.validations(entry.root());
                settled = java.util.stream.Stream.concat(records.stream().map(GenerationRecord::candidate), decisions.keySet().stream()).distinct().count();
                if (manifest.generation() > 0 && !decisions.containsKey(id)) latest += " (Candidate, unfinished)";
            }
            if (Files.exists(entry.root().resolve("refs/best"))) {
                String id = CheckpointInspection.reference(entry.root(), "best");
                best = "Gen " + CheckpointInspection.manifest(entry.root().resolve("checkpoints").resolve(id)).generation();
            }
            String exposure = "Unavailable";
            if (!records.isEmpty() && records.stream().allMatch(r -> r.samples() != null)) {
                var total = records.stream().map(r -> java.math.BigInteger.valueOf(r.samples()))
                        .reduce(java.math.BigInteger.ZERO, java.math.BigInteger::add);
                exposure = total + " sampled positions in " + records.size() + " recorded generations (includes held-out; repeated positions count again)";
                if (records.size() < settled) exposure += "; other historical exposure unavailable";
            }
            return entry.name() + " | " + entry.architecture() + " | ID " + metadata.map(l -> l.id().toString()).orElse("not yet adopted")
                    + "\nCompleted generations: " + settled + " recorded | Latest checkpoint: " + latest + " | Best: " + best
                    + "\nLatest completed training loss: " + TrainingLoss.format(last == null ? null : last.loss())
                    + (last == null ? "" : " (Gen " + last.generation() + "; " + finalObjective(last) + ")")
                    + "\nExposure: " + exposure + "\nStatus: " + status
                    + (history.warnings().isEmpty() ? "" : " | History: " + String.join("; ", history.warnings()));
        } catch (Exception failure) { return entry + "\nUnavailable: " + TrainingController.concise(failure); }
    }
    private static String finalObjective(GenerationRecord record) {
        String settings = record.regime().effectiveSettings();
        String marker = "|finalLossObjective=";
        return settings != null && settings.contains(marker) ? settings.substring(settings.indexOf(marker) + marker.length())
                : "historical objective/units unavailable; compare only matching recipes";
    }
    private LineageSummary() {}
}
