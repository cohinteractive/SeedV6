package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;

/** Explicit fixed-model SR-029 research, outside routine tests. Full legal prefixes retained.
 * Writes every completed observation immediately; a missing final status is an incomplete study. */
public final class Sr029Study {
    public static void main(String[] args) throws Exception {
        if(args.length < 3 || args.length > 4) throw new IllegalArgumentException("store corpus.tsv new-output.jsonl [policies]");
        int[] policies = Arrays.stream((args.length == 4 ? args[3] : "0,1,2,3").split(","))
                .mapToInt(Integer::parseInt).toArray();
        var snapshot = CheckpointStore.readSnapshot(Path.of(args[0]),
                "g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304");
        if(!snapshot.manifest().networkSha256().equals("5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7"))
            throw new AssertionError("Wrong network");
        var evaluation = snapshot.model().evaluation(NnueScoreMapping.V1);
        var gson = new GsonBuilder().create();
        try(var output = Files.newBufferedWriter(Path.of(args[2]), StandardOpenOption.CREATE_NEW)) {
            int observations = 0;
            // Warm every family before timing. No diagnostic callbacks/allocation in these warmups.
            for(int p : policies) try(var driver = new SearchDriver(
                    ExactSearchAdapter.lateMoveReductionResearch(evaluation, p, null))) {
                for(int i=0; i<3; i++) driver.search(new SearchRequest(Board.startingPosition(), 4));
            }
            int rootIndex = 0;
            for(String line : Files.readAllLines(Path.of(args[1]))) {
                if(line.isBlank() || line.startsWith("#")) continue;
                var fields = line.split("\t", -1);
                var board = Board.fromFen(fields[1]); var builder = GameHistory.builder(board);
                if(fields.length > 2 && !fields[2].isBlank()) for(String uci : fields[2].split(" ")) {
                    long move = Arrays.stream(ExhaustiveOracle.legalMoves(board))
                            .filter(m -> Move.coordinate(m).equals(uci)).findFirst().orElseThrow();
                    board = ExhaustiveOracle.child(board, move); builder.appendPosition(board);
                }
                var history = builder.snapshot();
                for(int mode=0; mode<3; mode++) for(int offset=0; offset<policies.length; offset++) {
                    int policy = policies[(rootIndex + offset) % policies.length];
                    int depth = mode == 0 ? 4 : mode == 1 ? 6 : 20;
                    long nodeBudget = mode == 2 ? 100_000 : 5_000_000;
                    var lmr = new LmrDiagnostics();
                    var iterations = new ArrayList<Map<String, Object>>();
                    var observer = new SearchObserver() {
                        public void onIterationCompleted(IterationSnapshot result) {
                            var r = new LinkedHashMap<String,Object>();
                            r.put("depth", result.depth()); r.put("completed", true);
                            r.put("nodes", result.nodes()); r.put("score", result.score());
                            r.put("lmr", gson.fromJson(gson.toJson(lmr), Map.class));
                            iterations.add(r);
                        }
                    };
                    try(var driver = new SearchDriver(ExactSearchAdapter.lateMoveReductionResearch(evaluation, policy, lmr))) {
                        long start = System.nanoTime();
                        var control = SearchControl.controlled(nodeBudget, start, 15_000_000_000L, TimeSource.SYSTEM);
                        var result = driver.search(new SearchRequest(board, history, depth, observer, control, true));
                        long elapsed = System.nanoTime() - start;
                        var row = new LinkedHashMap<String,Object>();
                        row.put("id", fields[0]); row.put("fen", Fen.fromBoard(board)); row.put("historySize", history.size());
                        row.put("policy", policy); row.put("mode", mode == 2 ? "nodes100k" : "depth" + depth);
                        row.put("elapsedNanos", elapsed); row.put("termination", control.termination().name());
                        row.put("outcome", result); row.put("iterations", iterations);
                        if(result.iterationIncomplete()) row.put("incompleteLmr", gson.fromJson(gson.toJson(lmr), Map.class));
                        if(result.nodes() != result.diagnostics().totalEnteredNodes()) throw new AssertionError("Accounting mismatch");
                        if(result.lastCompletedResult() != null)
                            Phase2QuiescenceTest.legalPv(board, result.lastCompletedResult().principalVariation());
                        output.write(gson.toJson(row)); output.newLine(); output.flush(); observations++;
                    }
                }
                rootIndex++;
                System.out.println("completed root " + fields[0]);
            }
            output.write(gson.toJson(Map.of("status", "passed", "observations", observations)));
            output.newLine();
        }
    }
}
