package com.ohinteractive.seedv6.tools.eval;

import com.google.gson.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.history.HistoryRepository;
import com.ohinteractive.seedv6.training.service.TrainingSession;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Read-only endpoint discovery and complete lineage payload/history audit; no search or updates.
 * An explicit checkpoint audits only that immutable payload (for the historical reference).
 * Omitted checkpoint discovers Best/latest and verifies every retained generation.
 */
public final class BrnLineageAudit {
    record Spec(String name, String root, String checkpoint, String initialCheckpoint) {}
    record Plan(String id, List<Spec> models) {}
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting()
            .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>) (v,t,c) -> new JsonPrimitive(v.toString())).create();

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("PLAN NEW_OUTPUT_DIRECTORY");
        Path planFile = Path.of(args[0]), output = Path.of(args[1]);
        var plan = DataFiles.read(planFile, Plan.class);
        Files.createDirectory(output);
        var evidence = new ArrayList<Object>();
        int verifiedPayloads = 0;
        var boards = BrnSuccessorAudit.boards();
        long deadline = System.nanoTime() + 300_000_000_000L;
        for (var spec : plan.models()) {
            Path root = Path.of(spec.root());
            String best = CheckpointInspection.reference(root, "best");
            String latest = CheckpointInspection.reference(root, "latest-training");
            var history = new HistoryRepository(root).refresh();
            var item = new LinkedHashMap<String,Object>();
            item.put("name", spec.name()); item.put("root", spec.root());
            item.put("best", best); item.put("latestTraining", latest);
            item.put("lineage", TrainingLineage.read(root).orElseThrow());
            item.put("initializer", LineageProvenance.read(root).orElse(null));
            item.put("session", TrainingSession.read(root).orElse(null));
            item.put("history", history); item.put("ledger", new SourceLedger(root).state());
            item.put("sources", DataSources.read(DataSources.directory(root)));
            var checkpoints = new ArrayList<Object>();
            List<String> ids;
            if (spec.checkpoint() != null) ids = List.of(spec.checkpoint());
            else {
                var available = CheckpointStore.availableCheckpoints(root);
                if (!available.diagnostics().isEmpty()) throw new IllegalStateException(available.diagnostics().toString());
                ids = available.checkpoints().stream().map(CheckpointManifest::id).toList();
                if (!ids.containsAll(List.of(best, latest, spec.initialCheckpoint())))
                    throw new IllegalStateException("Missing lineage endpoint or initial payload: " + spec.name());
            }
            for (String id : ids) {
                if (System.nanoTime() > deadline) throw new IllegalStateException("Audit time limit");
                var snapshot = CheckpointStore.readSnapshot(root, id);
                var optimizer = CheckpointStore.readTrainingSnapshot(root, id);
                var scores = BrnSuccessorAudit.scores(snapshot.model().evaluation(NnueScoreMapping.V1), boards);
                if (!scores.equals(BrnSuccessorAudit.scores(optimizer.snapshot().evaluation(NnueScoreMapping.V1), boards)))
                    throw new IllegalStateException("Optimizer/runtime mismatch: " + id);
                checkpoints.add(Map.of("manifest", snapshot.manifest(), "optimizerStep", optimizer.step(),
                        "optimizer", optimizer.hyperparameters(), "scores", scores, "optimizerRuntimeScoreParity", true));
                verifiedPayloads++;
            }
            item.put("checkpoints", checkpoints);
            item.put("chain", CheckpointInspection.lineage(root, spec.checkpoint() == null ? latest : spec.checkpoint()));
            if (!best.equals(CheckpointInspection.reference(root, "best"))
                    || !latest.equals(CheckpointInspection.reference(root, "latest-training"))
                    || !history.equals(new HistoryRepository(root).refresh()))
                throw new IllegalStateException("Store changed during audit: " + spec.name());
            evidence.add(item);
            Files.writeString(output.resolve("stores.json"), JSON.toJson(evidence) + "\n");
        }
        Files.writeString(output.resolve("complete.json"), JSON.toJson(Map.of(
                "plan", plan.id(), "completedUtc", Instant.now(), "java", System.getProperty("java.runtime.version"),
                "activeProcessors", Runtime.getRuntime().availableProcessors(),
                "verifiedLineages", evidence.size(), "verifiedPayloadAndOptimizerPairs", verifiedPayloads,
                "runtimeParityPositionsPerCheckpoint", boards.size(),
                "scope", "Checksum-validating read-only payload/metadata audit; no training or search")) + "\n");
        System.out.println("Completed lineage audit: " + output);
    }
    private BrnLineageAudit() {}
}
