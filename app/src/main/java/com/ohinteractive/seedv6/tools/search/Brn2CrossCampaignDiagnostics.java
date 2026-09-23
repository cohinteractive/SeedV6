package com.ohinteractive.seedv6.tools.search;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.ToDoubleFunction;
import com.ohinteractive.seedv6.core.brn2.Brn2Workspace;
import com.ohinteractive.seedv6.core.nnue.NnueEvaluator;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.history.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.selfplay.TrajectorySampler.Sample;
import com.ohinteractive.seedv6.training.service.BrnSupervision;
import static com.ohinteractive.seedv6.tools.search.DiagnosticReport.*;

/** Read-only retrospective comparison. No writer, recovery, training or search is invoked.
 * Every batch retains its own persisted teacher pin; pooling weights sampled positions equally.
 */
public final class Brn2CrossCampaignDiagnostics {
    record Batch(CheckpointManifest manifest, BootstrapPlan plan, BootstrapData data, GenerationRecord history) {}
    record Dataset(Path root, List<Batch> batches, String best) {}
    static final class Losses {
        long count;
        double wdl, teacher, blended;
        void add(double prediction, double terminal, double targetTeacher) {
            for (double value : new double[] {prediction, terminal, targetTeacher})
                if (!Double.isFinite(value) || Math.abs(value) > 1) throw new IllegalArgumentException("Unbounded value");
            double d = prediction - terminal; wdl += .5 * d * d;
            d = prediction - targetTeacher; teacher += .5 * d * d;
            d = prediction - BrnSupervision.blended(.75).target(terminal, targetTeacher); blended += .5 * d * d;
            count++;
        }
        Map<String, Object> result() {
            if (count == 0) throw new IllegalStateException("Empty population");
            return fields("samples", count, "wdlLoss", wdl / count, "teacherLoss", teacher / count, "blended75Loss", blended / count);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Expected ORIGINAL_STORE FRESH_STORE EXACT_REPLAY_MODEL EXACT_NNUE_CHECKPOINT; JSONL to stdout");
        var out = new PrintWriter(System.out, true);
        Dataset original = inspect(Path.of(args[0]), out, "A"), fresh = inspect(Path.of(args[1]), out, "B");
        Map<String, Brn2Diagnostics.Loaded> models = new LinkedHashMap<>();
        models.put("fresh", Brn2Diagnostics.load(fresh.root().resolve("checkpoints").resolve(fresh.best()).toString(), TrainingArchitecture.BRN2));
        models.put("replay", Brn2Diagnostics.load(args[2], TrainingArchitecture.BRN2));
        models.put("wdl", Brn2Diagnostics.load(original.root().resolve("checkpoints").resolve(original.best()).toString(), TrainingArchitecture.BRN2));
        models.put("nnue", Brn2Diagnostics.load(args[3], TrainingArchitecture.NNUE));
        for (var entry : models.entrySet()) write(out, "type", "model", "name", entry.getKey(), "identity", entry.getValue().identity());
        int matching = 0;
        for (int i = 0; i < Math.min(original.batches().size(), fresh.batches().size()); i++) {
            var a = original.batches().get(i); var b = fresh.batches().get(i);
            boolean equal = sampleHash(a.data().partition().training()).equals(sampleHash(b.data().partition().training()))
                    && sampleHash(a.data().partition().heldOut()).equals(sampleHash(b.data().partition().heldOut()))
                    && a.data().partition().trainingGames().equals(b.data().partition().trainingGames())
                    && a.data().partition().heldOutGames().equals(b.data().partition().heldOutGames())
                    && a.data().statistics().equals(b.data().statistics());
            if (equal) matching++;
            write(out, "type", "datasetOverlap", "generation", a.manifest().generation(), "exactPartitionAndStatistics", equal);
        }
        write(out, "type", "overlapSummary", "identicalGenerations", matching,
                "comparedGenerations", Math.min(original.batches().size(), fresh.batches().size()));
        Map<String, NnueEvaluator> teachers = new HashMap<>();
        evaluate(original, "A", models, teachers, out);
        evaluate(fresh, "B", models, teachers, out);
        write(out, "type", "end", "completed", true);
    }

    static Dataset inspect(Path supplied, PrintWriter out, String label) throws Exception {
        Path root = supplied.toRealPath();
        String latest = CheckpointInspection.reference(root, "latest-training"), best = CheckpointInspection.reference(root, "best");
        var lineage = CheckpointInspection.lineage(root, latest);
        var history = new HistoryRepository(root).refresh();
        var accepted = CheckpointInspection.accepted(root);
        var validations = CheckpointInspection.validations(root);
        require(history.warnings().isEmpty() && lineage.size() == history.records().size() + 1, "Incomplete history " + root);
        require(lineage.getFirst().generation() == 0 && lineage.getFirst().optimizerStep() == 0, "Missing g0");
        var objective = CheckpointStore.readBrnSupervision(root).orElse(BrnSupervision.WDL);
        List<Batch> batches = new ArrayList<>();
        String incumbent = lineage.getFirst().id(); long step = 0; int promotions = 0;
        for (int i = 1; i < lineage.size(); i++) {
            var manifest = lineage.get(i); var row = history.records().get(i - 1);
            var plan = CheckpointInspection.bootstrapPlan(root, manifest.parentId());
            var data = CheckpointInspection.bootstrapData(root, plan);
            require(manifest.architecture() == TrainingArchitecture.BRN2 && manifest.generation() == i && row.generation() == i
                    && manifest.parentId().equals(lineage.get(i - 1).id()) && row.candidate().equals(manifest.id())
                    && row.incumbent().equals(incumbent) && plan.incumbentId().equals(incumbent), "Lineage mismatch g" + i);
            require(plan.supervision().equals(objective), "Changed lineage objective g" + i);
            Brn2SupervisionAblation.trainingConfig(plan.settings());
            var e = row.bootstrap(); require(e != null, "Missing bootstrap evidence");
            var reconstructed = BootstrapEvidence.create(plan, data, e.comparison(), e.wdlLoss(), e.teacherLoss());
            require(reconstructed.equals(e) && validations.get(manifest.id()) != null
                    && validations.get(manifest.id()).bootstrap().equals(e), "Data/history/validation mismatch g" + i);
            step += data.partition().training().size();
            require(manifest.optimizerStep() == step, "Optimizer step mismatch g" + i);
            require(accepted.contains(manifest.id()) == row.promoted(), "Acceptance mismatch g" + i);
            if (row.promoted()) { incumbent = manifest.id(); promotions++; }
            require(row.resultingBest().equals(incumbent), "Best progression mismatch g" + i);
            batches.add(new Batch(manifest, plan, data, row));
            write(out, "type", "batch", "dataset", label, "generation", i, "candidate", manifest.id(),
                    "modelSha256", manifest.networkSha256(), "trainingSha256", manifest.trainingSha256(),
                    "planSha256", plan.hash(), "dataSha256", data.hash(), "teacherId", plan.generatorId(),
                    "teacherHash", plan.generatorHash(), "supervision", objective.mode(), "teacherWeight", objective.teacherWeight(),
                    "trainingSamples", data.partition().training().size(), "heldOutSamples", data.partition().heldOut().size(),
                    "trainingSamplesSha256", sampleHash(data.partition().training()), "heldOutSamplesSha256", sampleHash(data.partition().heldOut()));
        }
        require(best.equals(incumbent) && validations.size() == batches.size() && accepted.size() == promotions + 1, "Unsettled or extra evidence");
        write(out, "type", "store", "dataset", label, "root", root.toString(), "latest", latest, "best", best,
                "generations", batches.size(), "promotions", promotions, "retentions", batches.size() - promotions, "step", step);
        return new Dataset(root, List.copyOf(batches), best);
    }

    static String sampleHash(List<Sample> samples) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        try (var out = new DataOutputStream(new java.security.DigestOutputStream(OutputStream.nullOutputStream(), digest))) {
            out.writeInt(samples.size());
            for (var sample : samples) { for (long v : sample.board()) out.writeLong(v); out.writeDouble(sample.target()); }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    static ToDoubleFunction<long[]> predictor(NetworkModel model) {
        if (model instanceof NetworkModel.Brn2 brn) {
            var workspace = new Brn2Workspace(); return board -> brn.model().evaluate(board, workspace);
        }
        if (model instanceof NetworkModel.Nnue nnue) {
            var evaluator = new NnueEvaluator(nnue.nnue());
            return board -> { evaluator.evaluate(board); return evaluator.boundedValue(); };
        }
        throw new IllegalArgumentException("Unsupported evaluator");
    }

    static void evaluate(Dataset dataset, String label, Map<String, Brn2Diagnostics.Loaded> models,
                         Map<String, NnueEvaluator> teachers, PrintWriter out) throws Exception {
        Map<String, ToDoubleFunction<long[]>> predictors = new LinkedHashMap<>();
        Map<String, Losses> pooled = new LinkedHashMap<>();
        models.forEach((name, loaded) -> { predictors.put(name, predictor(loaded.model())); pooled.put(name, new Losses()); });
        for (var batch : dataset.batches()) {
            var plan = batch.plan();
            Path path = Path.of(plan.source().generatorStore()).toRealPath().resolve("checkpoints").resolve(plan.generatorId());
            String key = path + "#" + plan.generatorHash();
            if (!teachers.containsKey(key)) {
                var loaded = Brn2Diagnostics.load(path.toString(), TrainingArchitecture.NNUE);
                require(loaded.identity().get("modelSha256").equals(plan.generatorHash()), "Teacher pin mismatch");
                teachers.put(key, new NnueEvaluator(loaded.model().nnue()));
                write(out, "type", "teacher", "identity", loaded.identity());
            }
            var teacher = teachers.get(key); Map<String, Losses> local = new LinkedHashMap<>();
            predictors.keySet().forEach(name -> local.put(name, new Losses()));
            for (var sample : batch.data().partition().heldOut()) {
                double target = BrnSupervision.teacherValue(teacher, sample);
                for (var entry : predictors.entrySet()) {
                    double p = entry.getValue().applyAsDouble(sample.board());
                    pooled.get(entry.getKey()).add(p, sample.target(), target);
                    local.get(entry.getKey()).add(p, sample.target(), target);
                }
            }
            for (var entry : local.entrySet()) write(out, "type", "generationLoss", "dataset", label,
                    "generation", batch.manifest().generation(), "model", entry.getKey(), "losses", entry.getValue().result());
        }
        for (var entry : pooled.entrySet()) write(out, "type", "pooledLoss", "dataset", label, "model", entry.getKey(), "losses", entry.getValue().result());
    }
    private static void require(boolean ok, String message) throws IOException { if (!ok) throw new IOException(message); }
    private Brn2CrossCampaignDiagnostics() {}
}
