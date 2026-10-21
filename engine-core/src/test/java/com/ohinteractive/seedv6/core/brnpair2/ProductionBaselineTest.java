package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Model;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductionBaselineTest {
    @Test void frozenModelCacheAndSearchMappingMatchFreshEvaluationAcrossPositionsAndPerspectives() throws Exception {
        var model = ProductionModel.bundled();
        var definition = SearchEvaluation.brnPair2(model);
        var state = definition.newState(8);
        var qstate = definition.newState(8);
        var cache = model.newWorkspace();
        String[] placements = {
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR",
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR",
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR",
            "r3k2r/8/8/3pP3/8/8/8/R3K2R",
            "r3k2r/8/3P4/8/8/8/8/R3K2R",
            "r3k2r/8/3P4/8/8/8/8/R4RK1",
            "4k3/P7/8/8/8/8/7p/4K3",
            "4k3/P7/8/8/8/8/8/4K2q"
        };
        for (int direction : new int[]{1, -1}) for (int n = 0; n < placements.length; n++) {
            String placement = placements[direction == 1 ? n : placements.length - 1 - n];
            for (String side : new String[]{"w", "b"}) {
                var board = Board.fromFen(placement + " " + side + " - - 0 1");
                double fresh = model.newWorkspace().evaluatePawns(board, .25);
                assertTrue(Double.isFinite(fresh));
                assertEquals(fresh, cache.evaluatePawns(board, .25), 1e-10);
                state.initialize(board, 0);
                assertEquals(Brn3Model.score(fresh), state.evaluate(board, 0));
                qstate.initializeFrom(board, 0, state);
                assertEquals(state.evaluate(board, 0), qstate.evaluate(board, 0));
            }
        }
        assertThrows(IllegalArgumentException.class, () -> qstate.initializeFrom(Board.startingPosition(), 0,
                SearchEvaluation.brnPair2(model).newState(8)));
    }
}
