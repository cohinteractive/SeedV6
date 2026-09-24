package com.ohinteractive.seedv6.search.driver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.search.exact.ExactSearch;
import com.ohinteractive.seedv6.search.manage.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
class ProductionSearchIntegrationTest {
    private static final long[] BOARD = Board.fromFen("4k3/8/8/8/8/8/P7/4K3 w - - 0 1");

    @Test void playLifecycleProducesDirectExactHceAndNnueResultsWithCompatibilityThreadLimits() throws Exception {
        var network = NnueNetwork.initialized(194);
        for(var evaluation : List.of(SearchEvaluation.handcrafted(), SearchEvaluation.incremental(network))) {
            var expected = new ExactSearch(evaluation).search(BOARD, 2);
            for(int threads : new int[] {1, 4}) {
                var done = new CompletableFuture<ManagedSearchResult>();
                try(var service = new SearchLifecycleService(threads, evaluation)) {
                    service.start(BOARD, GameHistory.initial(BOARD), new SearchLimits(2, -1, -1, false), done::complete);
                    var result = done.get(5, TimeUnit.SECONDS);
                    assertEquals(SearchTermination.COMPLETED, result.termination());
                    assertNull(result.failure());
                    SearchDriverTest.agree(expected, result.lastCompletedResult());
                }
            }
        }
    }

    @Test void selfPlayDataGenerationUsesRebuiltHceAndNnueMovesAndRejectsIncompleteDepth() {
        var network = NnueNetwork.initialized(195);
        for(var evaluation : List.of(SearchEvaluation.handcrafted(), SearchEvaluation.incremental(network))) {
            var config = new SelfPlayConfig(1, 2, 4, 1, 0, 0, 1, 1, NnueScoreMapping.V1, -1, -1);
            var result = SelfPlayRunner.play(evaluation, config, 0, BOARD, new SelfPlayControl());
            assertEquals(GameTermination.PLY_CAP, result.termination());
            assertEquals(1, result.playedPlies());
            assertEquals(new ExactSearch(evaluation).search(BOARD, 2).bestMove(), result.positions().getFirst().playedMove());
            // A completed depth 1 does not satisfy this consumer's depth-2 data contract.
            long budget = new ExactSearch(evaluation).search(BOARD, 1).nodes() - 1;
            var limited = new SelfPlayConfig(1, 2, 1, 1, 0, 0, 1, 1, NnueScoreMapping.V1, budget, -1);
            var stopped = SelfPlayRunner.play(evaluation, limited, 0, BOARD, new SelfPlayControl());
            assertEquals(GameTermination.SEARCH_FAILURE, stopped.termination());
            assertEquals(0, stopped.playedPlies());
            assertTrue(stopped.result().isEmpty());
        }
    }

    @Test void validationUsesEachPinnedNetworkAndPublishesCompletedDriverWork() {
        var candidate = NnueNetwork.initialized(196);
        var incumbent = NnueNetwork.initialized(197);
        var config = new ValidationConfig(1, 1, 0, 0, 2, 4, NnueScoreMapping.V1, 1);
        List<ValidationProgress> progress = new ArrayList<>();
        var result = new ValidationArena().validate(candidate, incumbent, config, BOARD,
                GameHistory.initial(BOARD), new ValidationControl(), progress::add);
        assertEquals(2, result.statistics().totalPlies());
        assertEquals(GameTermination.PLY_CAP, result.pairs().getFirst().candidateWhite().termination());
        assertEquals(GameTermination.PLY_CAP, result.pairs().getFirst().candidateBlack().termination());
        for(int game = 1; game <= 2; game++) {
            var evaluation = SearchEvaluation.incremental(game == 1 ? candidate : incumbent);
            var expected = new SearchDriver(evaluation).search(new SearchRequest(BOARD, 2));
            int ordinal = game;
            var observed = progress.stream().filter(p -> p.gameOrdinal() == ordinal && p.lastMoveSearch().isPresent())
                    .findFirst().orElseThrow().lastMoveSearch().orElseThrow();
            assertEquals(2, observed.depth());
            assertEquals(expected.nodes(), observed.nodes());
        }
    }
}
