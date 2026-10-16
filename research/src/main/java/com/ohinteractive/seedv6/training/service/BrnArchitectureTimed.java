package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.brn3.*;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.LongSupplier;

/** Measured optimization-budget endpoints; research only, never promotes a network. */
public final class BrnArchitectureTimed {
    static final List<String> FAMILIES = List.of("brn", "nnue-v2", "nnue-strict", "linear",
            "tuple2", "tuple3", "edge", "self", "context", "logic", "logic-fixed");

    static final class Learner {
        final String family;
        BrnArchitectureControls.Control control;
        BrnEnergyTrainer energy;
        BrnTupleTrainer tuple;
        BrnSplineTrainer edge;
        BrnContextTrainer context;
        BrnLogicTrainer logic;

        Learner(String family, long seed) {
            this.family = family;
            switch (family) {
                case "brn" -> control = new BrnArchitectureControls.Control(BrnArchitectureControls.Kind.BRN3, seed);
                case "nnue-v2" -> control = new BrnArchitectureControls.Control(BrnArchitectureControls.Kind.NNUE_MATERIAL_V2, seed);
                case "nnue-strict" -> control = new BrnArchitectureControls.Control(BrnArchitectureControls.Kind.NNUE_STRICT, seed);
                case "linear" -> energy = new BrnEnergyTrainer(0, seed, .003);
                case "tuple2", "tuple3" -> tuple = new BrnTupleTrainer(family.equals("tuple2") ? 2 : 3, .01);
                case "edge" -> edge = new BrnSplineTrainer(16, seed, .003, .5);
                case "self", "context" -> context = new BrnContextTrainer(family.equals("context"), seed, .003);
                case "logic", "logic-fixed" -> {
                    logic = new BrnLogicTrainer(1, seed, .003, .003);
                    if (family.equals("logic-fixed")) logic.harden(.003);
                }
                default -> throw new IllegalArgumentException("Unknown frozen family: " + family);
            }
        }
        String kind() {
            if (control != null) return control.kind.name();
            if (energy != null) return "ENERGY2";
            if (tuple != null) return "TUPLE";
            if (edge != null) return "EDGE_PWL";
            return context != null ? "CONTEXT_LINEAR" : "LOGIC";
        }
        void train(long[][] boards, double[] targets, int size) {
            if (control != null) control.train(boards, targets, size);
            else if (energy != null) energy.trainBatch(boards, targets, size);
            else if (tuple != null) tuple.trainBatch(boards, targets, size);
            else if (edge != null) edge.trainBatch(boards, targets, size);
            else if (context != null) context.trainBatch(boards, targets, size);
            else logic.trainBatch(boards, targets, size);
        }
        long step() {
            if (control != null) return control.step();
            if (energy != null) return energy.step();
            if (tuple != null) return tuple.step();
            if (edge != null) return edge.step();
            return context != null ? context.step() : logic.step();
        }
        int parameters(boolean inference) {
            if (control != null) return control.brn != null
                    ? inference ? Brn3Layout.MODEL_PARAMETERS : Brn3Layout.TRAINING_PARAMETERS : NnueNetwork.PARAMETER_COUNT;
            if (energy != null) return inference ? energy.inferenceParameters() : energy.parameters();
            if (tuple != null) return inference ? tuple.inferenceParameters() : tuple.parameters();
            if (edge != null) return inference ? edge.inferenceParameters() : edge.parameters();
            if (context != null) return inference ? context.inferenceParameters() : context.parameters();
            return inference ? logic.inferenceParameters() : logic.parameters();
        }
        BrnArchitectureControls.View view() {
            if (control != null) return control.view();
            if (energy != null) return BrnEnergyExperiment.view(energy.snapshot());
            if (tuple != null) return BrnTupleExperiment.view(tuple.snapshot());
            if (edge != null) return BrnSplineExperiment.view(edge.snapshot());
            if (context != null) return BrnContextExperiment.view(context.snapshot());
            return BrnLogicExperiment.view(logic.snapshot());
        }
        void save(Path out, String prefix) throws IOException {
            if (control != null) control.save(out, prefix);
            else if (energy != null) BrnEnergyExperiment.save(energy, out, prefix);
            else if (tuple != null) BrnTupleExperiment.save(tuple, out, prefix);
            else if (edge != null) BrnSplineExperiment.save(edge, out, prefix);
            else if (context != null) BrnContextExperiment.save(context, out, prefix);
            else BrnLogicExperiment.save(logic, out, prefix);
        }
        void restore(Path state) throws IOException {
            if (control != null) control = BrnArchitectureControls.Control.load(control.kind, state);
            else if (energy != null) energy = BrnEnergyExperiment.load(state);
            else if (tuple != null) tuple = BrnTupleExperiment.load(state);
            else if (edge != null) edge = BrnSplineExperiment.load(state);
            else if (context != null) context = BrnContextExperiment.load(state);
            else logic = BrnLogicExperiment.load(state);
        }
        void finalPhase() {
            if (logic == null) throw new IllegalStateException("No separate final phase");
            if (logic.hardened()) logic.refitRate(.0003);
            else logic.harden(.0003);
        }
    }

    /** Cursor is external to optimizer state; its full position is persisted at every endpoint. */
    static final class Cursor {
        final int count;
        final long seed;
        int epoch = 1, offset;
        long samples;
        int[] order;
        Cursor(int count, long seed) {
            if (count < 128 || count > 1048576) throw new IllegalArgumentException("Training population bounds");
            this.count = count; this.seed = seed;
            order = BrnResearchMain.order(count, seed);
        }
        Cursor copy() {
            Cursor copy = new Cursor(count, seed);
            copy.epoch = epoch; copy.offset = offset; copy.samples = samples; copy.order = order.clone();
            return copy;
        }
        int fill(List<BrnResearchData.Example> data, long[][] boards, double[] targets) {
            int size = Math.min(128, count - offset);
            for (int j = 0; j < size; j++) {
                var e = data.get(order[offset + j]); boards[j] = e.board(); targets[j] = e.outcome();
            }
            return size;
        }
        void advance(int size) {
            if (size != Math.min(128, count - offset)) throw new IllegalArgumentException("Batch/cursor mismatch");
            offset += size; samples += size;
            if (offset == count) { epoch++; offset = 0; order = BrnResearchMain.order(count, seed + epoch - 1); }
        }
        Map<String,Object> evidence() {
            return Map.of("nextEpoch", epoch, "nextOffset", offset, "samples", samples, "shuffleSeed", seed,
                    "population", count, "shuffle", "seed+epoch-1 Fisher-Yates; same as frozen exposure runs");
        }
    }
    interface Batch { void train(long[][] boards, double[] targets, int size); }
    record Timed(long nanos, long maximumBatchNanos, long batches) {}

    /** Time includes batch gathering and optimization, excludes shuffle/validation/persistence. */
    static Timed trainTo(Batch trainer, Cursor cursor, List<BrnResearchData.Example> data,
                         long already, long target, LongSupplier clock) {
        if (already < 0 || target < already) throw new IllegalArgumentException("Budget order");
        long used = already, maximum = 0, batches = 0;
        long[][] boards = new long[128][]; double[] targets = new double[128];
        while (used < target) {
            if (cursor.samples >= 134217728L) throw new IllegalStateException("128M exposure guard reached before time budget");
            long start = clock.getAsLong();
            int size = cursor.fill(data, boards, targets); trainer.train(boards, targets, size);
            long duration = clock.getAsLong() - start;
            if (duration <= 0) throw new IllegalStateException("Nonpositive monotonic batch interval");
            used = Math.addExact(used, duration); maximum = Math.max(maximum, duration); batches++;
            cursor.advance(size);
        }
        return new Timed(used, maximum, batches);
    }

    static double[] budgets(String text) {
        String[] parts = text.split(",", -1); double[] result = new double[parts.length];
        if (parts.length < 1 || parts.length > 8) throw new IllegalArgumentException("1..8 declared endpoints");
        double last = 0;
        for (int i = 0; i < parts.length; i++) {
            double value = Double.parseDouble(parts[i]);
            if (!Double.isFinite(value) || value <= last || value > 1200 || value < .001)
                throw new IllegalArgumentException("Strictly increasing finite budgets in [.001,1200] seconds");
            result[i] = value; last = value;
        }
        return result;
    }

    static void verifyResume(Learner learner, Path endpoint, Cursor cursor,
                             List<BrnResearchData.Example> data) throws IOException {
        var restored = new Learner(learner.family, cursor.seed);
        restored.restore(endpoint.resolve("selected.state"));
        long[][] boards = new long[128][]; double[] targets = new double[128];
        int size = cursor.fill(data, boards, targets);
        learner.train(boards, targets, size); restored.train(boards, targets, size);
        learner.save(endpoint, "continuation-a"); restored.save(endpoint, "continuation-b");
        if (Files.mismatch(endpoint.resolve("continuation-a.state"), endpoint.resolve("continuation-b.state")) != -1
                || Files.mismatch(endpoint.resolve("continuation-a.model"), endpoint.resolve("continuation-b.model")) != -1)
            throw new AssertionError("Exact next scheduled batch resume");
        // The verification update is never part of the continuing training trajectory.
        learner.restore(endpoint.resolve("selected.state"));
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 8) throw new IllegalArgumentException(
                "DATA NEW_OUT FAMILY COUNT SEED BUDGET_SECONDS_CSV LOGIC_BASE_FRACTION timed-v1");
        Path dataPath = Path.of(args[0]), out = Path.of(args[1]); String family = args[2];
        int count = Integer.parseInt(args[3]); long seed = Long.parseLong(args[4]);
        double[] checkpoints = budgets(args[5]); double baseFraction = Double.parseDouble(args[6]);
        if (!args[7].equals("timed-v1") || !FAMILIES.contains(family) || count < 128 || count > 1048576
                || !Double.isFinite(baseFraction) || baseFraction <= 0 || baseFraction >= 1)
            throw new IllegalArgumentException("Frozen timed protocol");
        var data = BrnResearchData.read(dataPath, false);
        if (count != data.training().size()) throw new IllegalArgumentException("Use the whole declared data population; no silent prefix subset");
        var train = data.training(); var held = data.validation().subList(0, Math.min(4096, data.validation().size()));
        Files.createDirectory(out);
        var learner = new Learner(family, seed); var cursor = new Cursor(count, seed);
        var report = new LinkedHashMap<String,Object>(); var events = new ArrayList<Object>();
        report.put("schema", "brn-architecture-timed-v1"); report.put("kind", learner.kind());
        report.put("family", family); report.put("arguments", args); report.put("events", events);
        report.put("trainingParameters", learner.parameters(false)); report.put("modelParameters", learner.parameters(true));
        report.put("datasetManifest", DataFiles.read(dataPath.resolve("manifest.json"), Map.class));
        report.put("timingScope", "Measured gather+optimizer wall time; excludes shuffle, initialization, validation, persistence and exact-resume probes; full process time in runner receipt");
        report.put("selection", "Raw first4096 validation WDL half-MSE among Gen0 and declared time endpoints; no sealed test or gain selection");
        report.put("logicBaseFraction", baseFraction);
        report.put("logicBranchRule", "At each endpoint fork the continuing soft-gate/high-head-rate prefix, harden/freeze and refit at .0003 for remaining budget. Never feed endpoint refits into the next prefix. Extra branch work is charged separately.");
        report.put("recipe", "Original frozen fresh-run initializer/rates; time endpoints replace epoch stopping; batch128; independent low-rate terminal branch for LOGIC only");
        Path gen0 = out.resolve("gen0"); Files.createDirectory(gen0); learner.save(gen0, "selected");
        var initialMetrics = BrnArchitectureControls.metrics(held, learner.view());
        DataFiles.write(gen0.resolve("result.json"), Map.of("kind", learner.kind(), "complete", true,
                "selectedModelSha256", BrnResearchComparison.digest(gen0.resolve("selected.model")), "selectedValidation", initialMetrics));
        double best = (double) initialMetrics.get("outcomeHalfMse"); Path bestPath = gen0;
        long prefixNs = 0, branchNs = 0, branchSamples = 0, verificationSamples = 0; int index = 0;
        for (double budget : checkpoints) {
            long target = Math.round(budget * 1e9);
            long prefixTarget = learner.logic == null ? target : Math.round(target * baseFraction);
            Timed prefix = trainTo(learner::train, cursor, train, prefixNs, prefixTarget, System::nanoTime);
            prefixNs = prefix.nanos;
            Path endpoint = out.resolve(String.format(Locale.ROOT, "endpoint-%03d", ++index)); Files.createDirectory(endpoint);
            Learner deployed = learner; Cursor deployedCursor = cursor; long used = prefixNs;
            long maximumBatch = prefix.maximumBatchNanos;
            if (learner.logic != null) {
                learner.save(endpoint, "prefix");
                deployed = new Learner(family, seed); deployed.restore(endpoint.resolve("prefix.state"));
                deployed.finalPhase(); deployedCursor = cursor.copy();
                if (prefixNs >= target) throw new IllegalStateException("Base-phase batch exceeded total budget; declare a larger budget");
                Timed refit = trainTo(deployed::train, deployedCursor, train, prefixNs, target, System::nanoTime);
                used = refit.nanos; branchNs += used - prefixNs; maximumBatch = Math.max(maximumBatch, refit.maximumBatchNanos);
                branchSamples += deployedCursor.samples - cursor.samples;
            }
            deployed.save(endpoint, "selected");
            long expectedUpdates = (long) (deployedCursor.epoch - 1) * ((count + 127) / 128) + deployedCursor.offset / 128;
            if (deployed.step() != expectedUpdates) throw new AssertionError("Optimizer and persisted data cursor differ");
            var metrics = BrnArchitectureControls.metrics(held, deployed.view());
            var event = new LinkedHashMap<String,Object>();
            event.put("index", index); event.put("targetSeconds", budget); event.put("trainingSeconds", used / 1e9);
            event.put("prefixSeconds", prefixNs / 1e9); event.put("overshootSeconds", (used - target) / 1e9);
            event.put("maximumBatchSeconds", maximumBatch / 1e9); event.put("cursor", deployedCursor.evidence());
            event.put("prefixCursor", cursor.evidence()); event.put("updates", deployed.step());
            event.put("validation", metrics); event.put("modelSha256", BrnResearchComparison.digest(endpoint.resolve("selected.model")));
            event.put("stateSha256", BrnResearchComparison.digest(endpoint.resolve("selected.state")));
            if (deployed.logic != null) event.put("gateDiagnostics", deployed.logic.diagnostics());
            // Preserve incomplete endpoint evidence if the independent continuation check fails.
            DataFiles.write(endpoint.resolve("result.json"), Map.of("kind", deployed.kind(), "complete", false,
                    "selectedModelSha256", event.get("modelSha256"), "endpoint", event));
            verifyResume(deployed, endpoint, deployedCursor, train);
            verificationSamples += 2L * Math.min(128, count - deployedCursor.offset);
            DataFiles.write(endpoint.resolve("result.json"), Map.of("kind", deployed.kind(), "complete", true,
                    "selectedModelSha256", event.get("modelSha256"), "endpoint", event, "exactNextBatchResume", true,
                    "note", "This endpoint is complete; its parent run may still have later budgets pending"));
            events.add(event);
            if ((double) metrics.get("outcomeHalfMse") < best) { best = (double) metrics.get("outcomeHalfMse"); bestPath = endpoint; }
            event.put("bestWithinBudgetPath", bestPath.toString());
            event.put("bestWithinBudgetModelSha256", BrnResearchComparison.digest(bestPath.resolve("selected.model")));
            report.put("executedOptimizationSeconds", (prefixNs + branchNs) / 1e9);
            report.put("executedOptimizationSamples", cursor.samples + branchSamples);
            report.put("unretainedVerificationSamples", verificationSamples); report.put("unretainedVerificationUpdates", 2L * index);
            report.put("selectedSource", bestPath.toString()); DataFiles.write(out.resolve("result.json"), report);
            System.out.println("endpoint=" + index + " target=" + budget + " actual=" + used / 1e9 + " samples=" + deployedCursor.samples + " loss=" + metrics.get("outcomeHalfMse"));
        }
        Files.copy(bestPath.resolve("selected.model"), out.resolve("selected.model"));
        Files.copy(bestPath.resolve("selected.state"), out.resolve("selected.state"));
        report.put("selectedModelSha256", BrnResearchComparison.digest(out.resolve("selected.model")));
        report.put("selectedStateSha256", BrnResearchComparison.digest(out.resolve("selected.state")));
        report.put("exactNextBatchResume", true); report.put("complete", true);
        DataFiles.write(out.resolve("result.json"), report);
    }
    private BrnArchitectureTimed() {}
}
