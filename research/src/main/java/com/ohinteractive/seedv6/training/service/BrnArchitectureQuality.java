package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.brn3.Brn3Objective;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Frozen-plan quality diagnostics. Test labels require explicit final-frozen invocation. */
public final class BrnArchitectureQuality {
    static final class Reliability {
        final double[][] bins = new double[10][3];
        int count;
        void add(double predictedOutcome, double targetOutcome) {
            if (!Double.isFinite(predictedOutcome) || !Double.isFinite(targetOutcome)
                    || Math.abs(predictedOutcome) > 1 || Math.abs(targetOutcome) > 1)
                throw new IllegalArgumentException("Finite outcomes in [-1,1] required");
            double predicted = (1 + predictedOutcome) / 2, target = (1 + targetOutcome) / 2;
            int index = Math.min(9, (int) (predicted * 10));
            bins[index][0]++; bins[index][1] += predicted; bins[index][2] += target; count++;
        }
        Map<String,Object> report() {
            if (count == 0) throw new IllegalStateException("Empty calibration population");
            var rows = new ArrayList<Object>(); double gap = 0;
            for (int i = 0; i < bins.length; i++) {
                double[] bin = bins[i]; var row = new LinkedHashMap<String,Object>();
                row.put("lowerInclusive", i / 10.0); row.put("upper", (i + 1) / 10.0);
                row.put("upperInclusive", i == 9); row.put("positions", (int) bin[0]);
                if (bin[0] > 0) {
                    row.put("meanPredictedExpectedScore", bin[1] / bin[0]);
                    row.put("meanTeacherExpectedScore", bin[2] / bin[0]);
                    row.put("signedGap", (bin[1] - bin[2]) / bin[0]); gap += Math.abs(bin[1] - bin[2]);
                }
                rows.add(row);
            }
            return Map.of("positions", count, "bins", rows, "weightedAbsoluteBinGap", gap / count,
                    "scope", "Expected game score (win + half draw) derived from WDL expectation; teacher calibration diagnostic, not an empirical win-probability or strength estimate");
        }
    }
    static Map<String,Object> reliability(List<BrnResearchData.Example> examples, BrnArchitectureControls.View view) {
        var raw = new Reliability(); var search = new Reliability(); var evaluator = view.evaluator().get();
        for (var example : examples) {
            var board = example.board();
            double prediction = view.pawns() == null ? view.outcome().applyAsDouble(board)
                    : Brn3Objective.outcome(view.pawns().applyAsDouble(board), board);
            evaluator.initialize(board);
            double searchPrediction = Brn3Objective.outcome(evaluator.evaluate(board, 0) / 100.0, board);
            raw.add(prediction, example.outcome()); search.add(searchPrediction, example.outcome());
        }
        return Map.of("raw", raw.report(), "calibratedSearch", search.report());
    }

    @SuppressWarnings("unchecked")
    static Map<String,Object> entry(Path planPath, String expectedSha, String key, String protocol) throws Exception {
        if (!BrnResearchComparison.digest(planPath).equals(expectedSha)) throw new IOException("Frozen quality plan changed");
        var plan = DataFiles.read(planPath, Map.class);
        if (!"brn-architecture-quality-plan-v1".equals(plan.get("schema"))) throw new IOException("Quality plan schema");
        if (!List.of("validation-v1", "sealed-final-v1").contains(protocol)) throw new IllegalArgumentException("Explicit quality protocol required");
        if (protocol.equals("sealed-final-v1") && !Boolean.TRUE.equals(plan.get("finalDecisionsFrozen")))
            throw new IOException("Final decisions must be frozen before opening test labels");
        var entries = (Map<String,Map<String,Object>>) plan.get("entries");
        if (entries == null || !entries.containsKey(key)) throw new IOException("Entry absent from frozen plan");
        var entry = entries.get(key);
        String partition = protocol.equals("sealed-final-v1") ? "test" : "validation";
        if (!partition.equals(entry.get("partition"))) throw new IOException("Partition/protocol mismatch");
        Object gain = entry.get("gain"), count = entry.get("count");
        if (!(gain instanceof Number) || !(count instanceof Number)) throw new IOException("Gain/count required");
        BrnArchitectureControls.requireGain(((Number) gain).doubleValue());
        double n = ((Number) count).doubleValue();
        if (!Double.isFinite(n) || n != (int) n || n < 1) throw new IOException("Integer positive population count required");
        Path run = Path.of((String) entry.get("run"));
        if (!BrnArchitectureControls.modelIdentity(run).equals(entry.get("modelSha256"))) throw new IOException("Model differs from frozen plan");
        if (protocol.equals("sealed-final-v1") || entry.containsKey("modelMetadataIdentity")) {
            if (!BrnArchitectureControls.metadataIdentity(run).equals(entry.get("modelMetadataIdentity")))
                throw new IOException("Evaluator metadata differs from frozen plan or is missing");
        }
        Path data = Path.of((String) entry.get("data"));
        var manifest = DataFiles.read(data.resolve("manifest.json"), Map.class);
        if (!entry.get("dataPayloadSha256").equals(manifest.get("payloadSha256"))) throw new IOException("Dataset differs from frozen plan");
        if (!(manifest.get("counts") instanceof List<?> counts) || counts.size() != 3
                || !(counts.get(partition.equals("test") ? 2 : 1) instanceof Number available))
            throw new IOException("Frozen population counts missing from dataset manifest");
        if (n > available.doubleValue() || partition.equals("test") && n != available.doubleValue())
            throw new IOException("Test plan must cover the entire sealed population; validation count must be available");
        return entry;
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("PLAN EXPECTED_PLAN_SHA256 ENTRY_KEY NEW_OUT validation-v1|sealed-final-v1");
        Path planPath = Path.of(args[0]), out = Path.of(args[3]);
        if (Files.exists(out)) throw new IOException("Use a new quality output");
        var entry = entry(planPath, args[1], args[2], args[4]);
        Path run = Path.of((String) entry.get("run")), dataPath = Path.of((String) entry.get("data"));
        double gain = ((Number) entry.get("gain")).doubleValue(); int count = ((Number) entry.get("count")).intValue();
        String modelIdentity=BrnArchitectureControls.modelIdentity(run),metadataIdentity=BrnArchitectureControls.metadataIdentity(run);
        // Verify model loading before unsealing any real test data.
        var view = BrnArchitectureControls.loadView(run, gain);
        boolean test = args[4].equals("sealed-final-v1");
        var data = BrnResearchData.read(dataPath, test); var population = test ? data.test() : data.validation();
        if (count > population.size() || test && count != population.size())
            throw new IOException("Frozen test must use the entire held-out population; validation count must be available");
        var selected = population.subList(0, count); var report = new LinkedHashMap<String,Object>();
        report.put("schema", "brn-architecture-quality-v1"); report.put("plan", planPath.toString());
        report.put("planSha256", args[1]); report.put("entryKey", args[2]); report.put("entry", entry);
        report.put("protocol", args[4]); report.put("metrics", BrnArchitectureControls.metrics(selected, view));
        report.put("modelSha256",modelIdentity);report.put("modelMetadataIdentity",metadataIdentity);
        report.put("reliability", reliability(selected, view));
        report.put("scope", "Descriptive prediction/calibration and material/piece-count strata on the declared common population; not a playing-strength claim or proof of unknown native historical exposure");
        BrnArchitectureControls.requireIdentity(run,modelIdentity,metadataIdentity);report.put("modelIdentityStable",true);
        Files.createDirectory(out); DataFiles.write(out.resolve("result.json"), report);
        System.out.println(DataFiles.JSON.toJson(report));
    }
    private BrnArchitectureQuality() {}
}
