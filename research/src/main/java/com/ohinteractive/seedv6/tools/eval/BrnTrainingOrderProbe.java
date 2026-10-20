package com.ohinteractive.seedv6.tools.eval;

import com.google.gson.JsonParser;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Objective;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.service.BrnCaptureConsistency;
import java.nio.file.*;
import java.util.*;

/** Tiny order-sensitivity diagnostic through the actual GUI optimizer loop; no model publication. */
public final class BrnTrainingOrderProbe {
    private record Row(long[] board, double target) {}
    private record ModelPlan(String id, List<BrnSuccessorMatch.Model> models) {}
    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("TRAINED_MODEL_PLAN BT4_JSONL CP_JSONL NEW_OUTPUT");
        Path output = Path.of(args[3]);
        if (Files.exists(output)) throw new IllegalArgumentException("Preserve earlier evidence");
        long deadline = System.nanoTime() + 300_000_000_000L;
        var plan = DataFiles.read(Path.of(args[0]), ModelPlan.class);
        var sources = new ArrayList<List<Row>>();
        var bindings = new TreeMap<String, String>();
        for (int i = 0; i < 3; i++) bindings.put(args[i], DataFiles.hash(Files.readString(Path.of(args[i]))));
        for (int i = 1; i <= 2; i++) {
            var rows = new ArrayList<Row>();
            for (String line : Files.readAllLines(Path.of(args[i]))) {
                var json = JsonParser.parseString(line).getAsJsonObject();
                rows.add(new Row(Board.fromFen(json.get("fen").getAsString()), json.get("target").getAsDouble()));
            }
            if (rows.size() != 2048) throw new IllegalArgumentException("Expected preserved diagnostic source");
            sources.add(List.copyOf(rows));
        }
        var arms = new ArrayList<Object>();
        for (var spec : plan.models()) {
            if (spec.name().equals("Gen32")) continue;
            var manifest = CheckpointStore.readSnapshot(Path.of(spec.root()), spec.checkpoint()).manifest();
            if (!manifest.networkSha256().equals(spec.networkSha256())) throw new IllegalArgumentException("Model binding");
            for (String order : List.of("BT4-then-CP", "CP-then-BT4", "alternating", "pre-shuffled")) {
                var rows = new ArrayList<Row>();
                if (order.equals("alternating")) {
                    for (int i = 0; i < 1536; i++) for (var source : sources) rows.add(source.get(i));
                } else {
                    for (int source : order.equals("CP-then-BT4") ? new int[]{1, 0} : new int[]{0, 1})
                        rows.addAll(sources.get(source).subList(0, 1536));
                    if (order.equals("pre-shuffled")) Collections.shuffle(rows, new Random(71));
                }
                var state = CheckpointStore.readTrainingSnapshot(Path.of(spec.root()), spec.checkpoint());
                state.setLearningRate(.001);
                long initial = state.step(), started = System.nanoTime();
                var before = metrics(state, sources);
                var curve = new ArrayList<Object>();
                int[] lastEpoch = {0};
                var control = new SelfPlayControl();
                try (var workspace = new SampleStore.Workspace()) {
                    var result = IncrementalTraining.train(state, state.snapshot(),
                            new SelfPlayTraining.Config(8, 128, true, 71), control, sink -> {
                                for (var row : rows) {
                                    if (System.nanoTime() > deadline) throw new IllegalStateException("Diagnostic deadline");
                                    sink.accept(new TrajectorySampler.Sample(row.board(), 0, row.target()));
                                }
                                return rows.size();
                            }, sample -> new IncrementalTraining.Prepared(sample, null), BrnCaptureConsistency.OFF,
                            progress -> {
                                if (System.nanoTime() > deadline) throw new IllegalStateException("Diagnostic deadline");
                            }, status -> {
                                if ((status.epoch() == 2 || status.measuring()) && status.epoch() != lastEpoch[0]) {
                                    lastEpoch[0] = status.epoch();
                                    curve.add(Map.of("completedEpochs", status.measuring() ? 8 : 1,
                                            "step", state.step(), "metrics", metrics(state, sources)));
                                }
                            });
                    if (!result.verified() || result.training().orElseThrow().samplesTrained() != 24576
                            || state.step() - initial != 192)
                        throw new IllegalStateException("Exposure mismatch");
                }
                arms.add(Map.of("name", spec.name(), "manifest", manifest, "order", order,
                        "initialStep", initial, "finalStep", state.step(), "before", before, "curve", curve,
                        "elapsedSeconds", (System.nanoTime() - started) / 1e9));
                DataFiles.write(output, Map.of("id", "wave3-training-order-v1", "inputs", bindings,
                        "arms", arms, "completed", false));
            }
        }
        if (arms.size() != 12) throw new IllegalStateException("Expected three architectures");
        DataFiles.write(output, Map.of("id", "wave3-training-order-v1", "inputs", bindings,
                "arms", arms, "completed", true, "activeProcessors", Runtime.getRuntime().availableProcessors(),
                "java", System.getProperty("java.runtime.version")));
        System.out.println("Completed 12 bounded order arms; no models saved: " + output);
    }
    private static Object metrics(NetworkTrainingState state, List<List<Row>> sources) {
        var result = new LinkedHashMap<String, Object>();
        for (int source = 0; source < sources.size(); source++) {
            double fit = 0, held = 0;
            var rows = sources.get(source);
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.get(i);
                double raw = switch (state) {
                    case NetworkTrainingState.BrnPair2 p -> p.trainer().predictPawns(row.board());
                    case NetworkTrainingState.BrnNext p -> p.trainer().predictPawns(row.board());
                    default -> throw new IllegalArgumentException("W3 architectures only");
                };
                double loss = Brn3Objective.crossEntropy(raw, NnueCorpusTargets.material(row.board()), row.target())[0];
                if (i < 1536) fit += loss; else held += loss;
            }
            result.put(source == 0 ? "BT4" : "CP", Map.of("fitCE", fit / 1536, "heldCE", held / 512));
        }
        return result;
    }
    private BrnTrainingOrderProbe() {}
}
