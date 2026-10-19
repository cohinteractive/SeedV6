package com.ohinteractive.seedv6.tools.eval;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnpair2.*;
import com.ohinteractive.seedv6.core.brnnext.*;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.selfplay.HeadlessGame;
import com.ohinteractive.seedv6.search.evaluation.*;
import java.nio.file.*;
import java.util.*;

/** Bounded feasibility only. Does not train, play a match, or write a model store. */
public final class BrnSuccessorProbe {
    private static volatile long sink;
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("MODEL ORIGINAL_LINEAGE NEW_RESULT_JSON");
        Path output = Path.of(args[2]);
        if (Files.exists(output)) throw new IllegalArgumentException("Preserve prior result: " + output);
        long deadline = System.nanoTime() + 300_000_000_000L;
        var reference = ProductionModel.load(Path.of(args[0]));
        var checkpoint = CheckpointStore.readSnapshot(Path.of(args[1]), ProductionModel.CHECKPOINT);
        if (!checkpoint.manifest().networkSha256().equals(ProductionModel.SHA256)) throw new IllegalStateException("Baseline store mismatch");
        var state = CheckpointStore.readTrainingSnapshot(Path.of(args[1]), ProductionModel.CHECKPOINT);
        if (state.step() != 1753088) throw new IllegalStateException("Unexpected reference optimizer step");
        var boards = new ArrayList<long[]>(); var random = new SplittableRandom(20261020);
        var game = new HeadlessGame(Board.startingPosition(), 256);
        while (boards.size() < 32) {
            boards.add(game.boardSnapshot());
            for (int i = 0; i < 3; i++) {
                if (!game.active()) game = new HeadlessGame(Board.startingPosition(), 256);
                var moves = game.legalMoves(); game.play(moves[random.nextInt(moves.length)]);
            }
        }
        var names = new ArrayList<String>(); var evaluations = new ArrayList<SearchEvaluation>();
        var sizes = new ArrayList<Long>(); var weightBytes = new ArrayList<Long>();
        names.add("Gen32"); evaluations.add(SearchEvaluation.brnPair2(reference)); sizes.add((long) BrnPair2Codec.MODEL_BYTES); weightBytes.add(reference.immutablePrimitiveBytes());
        for (var kind : BrnNextArchitecture.values()) {
            var trainer = new BrnNextTrainer(kind, 1, .003); names.add(kind.name());
            evaluations.add(SearchEvaluation.brnNext(trainer.snapshot())); sizes.add((long) BrnNextCodec.modelBytes(kind));
            weightBytes.add(kind.parameters() * 8L);
        }
        var workspaces = evaluations.stream().map(e -> e.newState(2)).toList();
        for (var workspace : workspaces) run(workspace, boards, 20000);
        long[][] timings = new long[names.size()][7];
        for (int pass = 0; pass < 7; pass++) for (int offset = 0; offset < names.size(); offset++) {
            if (System.nanoTime() > deadline) throw new IllegalStateException("Feasibility budget exhausted");
            int index = (offset + pass) % names.size(); long start = System.nanoTime();
            run(workspaces.get(index), boards, 20000); timings[index][pass] = System.nanoTime() - start;
        }
        var rows = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < names.size(); i++) {
            long[] ordered = timings[i].clone(); Arrays.sort(ordered);
            rows.add(Map.of("model", names.get(i), "payloadBytes", sizes.get(i), "immutablePrimitiveBytes", weightBytes.get(i),
                    "passNanos", timings[i], "evaluationsPerPass", 20000, "medianNanosPerEvaluation", ordered[3] / 20000.0));
        }
        var result = new LinkedHashMap<String, Object>();
        result.put("plan", "wave1-feasibility-v1"); result.put("java", System.getProperty("java.runtime.version"));
        result.put("vm", System.getProperty("java.vm.name")); result.put("os", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        result.put("activeProcessors", Runtime.getRuntime().availableProcessors());
        result.put("baseline", checkpoint.manifest()); result.put("baselineOptimizerVerified", true);
        result.put("fenRoots", boards.stream().map(Fen::fromBoard).toList()); result.put("measurements", rows);
        result.put("candidateState", "Gen0-material-only; seed1; no strength claim"); result.put("checksum", sink);
        DataFiles.write(output, result); System.out.println("Verified baseline and wrote bounded feasibility results: " + output);
    }
    private static void run(SearchEvaluation.State state, List<long[]> boards, int count) {
        long sum = 0; for (int i = 0; i < count; i++) sum += state.evaluate(boards.get(i % boards.size()), 0); sink = sum;
    }
    private BrnSuccessorProbe() {}
}
