package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Model;
import com.ohinteractive.seedv6.rules.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.model.NetworkModel;

/** Explicit external-checkpoint oracle experiment, not a machine-dependent routine test. */
public final class Sr001Pair2Probe {
    public static void main(String[] args) throws Exception {
        if(args.length != 2) throw new IllegalArgumentException("store new-output.json");
        String checkpoint = "g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304";
        String hash = "5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7";
        var snapshot = CheckpointStore.readSnapshot(Path.of(args[0]), checkpoint);
        if(!hash.equals(snapshot.manifest().networkSha256())) throw new AssertionError("Frozen model hash");
        var model = ((NetworkModel.BrnPair2)snapshot.model()).model();
        var definition = snapshot.model().evaluation(NnueScoreMapping.V1);
        // Full fresh recomputation at each oracle evaluation: no production rolling state.
        ExactEvaluator fresh = (b, p) -> Brn3Model.score(model.newWorkspace().evaluatePawns(b, .25));
        var rows = new ArrayList<Map<String, Object>>();
        for(var fixture : QuiescenceSeeCorpus.FIXTURES) {
            var board = Board.fromFen(fixture.fen()); var game = GameHistory.initial(board);
            for(int policy : new int[] {0, 2, 3, 4, 64, 66, 67}) {
                var oracle = new Phase2QuiescenceTest.RestrictedOracle(fresh, policy);
                int expected = oracle.score(board, new SearchLineHistory(game), 0, 0, 0, -1);
                var production = ExactSearch.quiescencePvsResearch(ExactEvaluator.from(definition), new TTable(1), policy);
                var result = production.search(board, 0);
                if(!result.completed() || expected != result.score())
                    throw new AssertionError("Frozen-model oracle mismatch " + fixture.name() + "/" + policy);
                Phase2QuiescenceTest.legalPv(board, result.principalVariation());
                rows.add(Map.of("fixture", fixture.name(), "fen", fixture.fen(), "policy", policy,
                        "score", result.score(), "oracleNodes", oracle.nodes, "qnodes", result.qnodes()));
            }
        }
        for(String fen : new String[] {"7k/8/5KQ1/8/8/8/8/8 w - - 98 1",
                "4r1k1/8/8/8/8/8/8/4K3 w - - 99 1",
                "4k3/8/8/3q4/4P3/8/8/4K3 w - - 98 1"}) {
            var board = Board.fromFen(fen); var game = GameHistory.initial(board);
            for(int policy : new int[] {33, 65}) for(int depth = 0; depth <= 1; depth++) {
                var oracle = new Phase2QuiescenceTest.RestrictedOracle(fresh, policy);
                int expected = oracle.score(board, new SearchLineHistory(game), depth, 0, 0, -1);
                var result = ExactSearch.quiescencePvsResearch(ExactEvaluator.from(definition), new TTable(1), policy).search(board, depth);
                if(!result.completed() || expected != result.score()) throw new AssertionError("Check family oracle mismatch");
                Phase2QuiescenceTest.legalPv(board, result.principalVariation());
                rows.add(Map.of("fixture", "checks", "fen", fen, "policy", policy, "depth", depth,
                        "score", result.score(), "oracleNodes", oracle.nodes, "qnodes", result.qnodes()));
            }
        }
        Files.writeString(Path.of(args[1]), new GsonBuilder().setPrettyPrinting().create().toJson(
                Map.of("checkpoint", checkpoint, "networkSha256", hash, "status", "passed", "comparisons", rows)) + "\n",
                StandardOpenOption.CREATE_NEW);
    }
}
