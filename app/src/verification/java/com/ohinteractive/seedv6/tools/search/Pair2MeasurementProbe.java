package com.ohinteractive.seedv6.tools.search;

import java.nio.file.Path;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Model;
import com.ohinteractive.seedv6.core.brnpair2.*;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.driver.ProductionSearch;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.search.exact.ExactSearch;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.model.NetworkModel;

/** Bounded real-checkpoint integration evidence, not a Search-feature experiment. */
public final class Pair2MeasurementProbe {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("STORE CHECKPOINT SHA256");
        var checkpoint = CheckpointStore.readSnapshot(Path.of(args[0]), args[1]);
        if (!checkpoint.manifest().networkSha256().equals(args[2])) throw new IllegalArgumentException("Hash mismatch");
        var model = ((NetworkModel.BrnPair2) checkpoint.model()).model();
        var definition = checkpoint.model().evaluation(NnueScoreMapping.V1);
        var state = definition.newState(257); var other = definition.newState(257);
        var rolling = model.newWorkspace();
        var rows = new ArrayList<Map<String, Object>>();
        var positions = new ArrayList<>(ExactSearchHarness.positions());
        positions.add(new ExactSearchHarness.Position("white-pawn", "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1"));
        positions.add(new ExactSearchHarness.Position("black-to-move", "4k3/8/8/8/8/8/4P3/4K3 b - - 0 1"));
        positions.add(new ExactSearchHarness.Position("clock100", "4k3/8/8/8/8/8/4P3/4K3 w - - 100 1"));
        for (var position : positions) {
            var board = Board.fromFen(position.fen());
            double raw = model.newWorkspace().evaluatePawns(board), material = BrnPair2Features.material(board);
            double mapped = material + .25 * (raw-material);
            int expected = Brn3Model.score(mapped);
            for (int repeat = 0; repeat < 20; repeat++) {
                state.evaluate(Board.startingPosition(), 0); other.evaluate(Board.fromFen(positions.get(2).fen()), 0);
                if (state.evaluate(board, 0) != expected || other.evaluate(board, 0) != expected
                        || Brn3Model.score(rolling.evaluatePawns(board, .25)) != expected)
                    throw new AssertionError("State/recompute mismatch: " + position.name());
            }
            var altered = board.clone(); altered[4] ^= ~Board.PLAYER_BIT; altered[5] ^= 789123;
            if (state.evaluate(altered, 0) != expected) throw new AssertionError("Forbidden status information");
            var reference = new ExactSearch(definition).search(board, 3);
            for (int workers : new int[] {1, 2}) try (var production = ProductionSearch.create(workers, definition)) {
                var result = production.search(new SearchRequest(board, 3));
                if (!result.completed() || result.selective() || result.score() != reference.score())
                    throw new AssertionError("Production/reference disagreement: " + position.name());
            }
            rows.add(Map.of("name", position.name(), "fen", position.fen(), "rawPawns", raw,
                    "materialPawns", material, "mappedPawns", mapped, "staticScore", expected,
                    "depth3ReferenceScore", reference.score(), "workerCounts", List.of(1, 2)));
        }
        if (Brn3Model.score(1e9) != 32511 || Brn3Model.score(-1e9) != -32511
                || Brn3Model.score(.125) != 13 || Brn3Model.score(-.125) != -13)
            throw new AssertionError("Score mapping");
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(Map.of(
                "checkpoint", args[1], "networkSha256", args[2], "mapping", BrnPair2Model.CALIBRATION_ID,
                "immutablePrimitiveBytes", model.immutablePrimitiveBytes(), "workerPrimitiveBytes", rolling.primitiveBytes(),
                "status", "passed", "positions", rows)));
    }
    private Pair2MeasurementProbe() {}
}
