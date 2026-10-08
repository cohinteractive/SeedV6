package com.ohinteractive.seedv6.training.validation;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.search.driver.SearchWorkerAssertions.assertCapacity;

@Timeout(30)
class TimedValidationTest {
    @Test void bothColoursReceivePerMoveTimeLimitsAndActualFourWorkerSearch() {
        var cfg = new ValidationConfig(1, 1, 0, 0, 4, 4, NnueScoreMapping.V1, 2, 150);
        var board = Board.fromFen("r3k3/8/8/8/8/8/8/R3K3 w Qq - 0 1");
        var players = new ArrayList<NnueNetwork>(); var depths = new ArrayList<Integer>();
        var candidate = NnueNetwork.initialized(1); var best = NnueNetwork.initialized(2);
        var result = new ValidationArena().validate(candidate, best, cfg, board, GameHistory.initial(board), new ValidationControl(), (network, c) -> {
            players.add(network); var player = ValidationArena.search(SearchEvaluation.incremental(network, c.scoreMapping()), c);
            assertCapacity(player, 4);
            return new ValidationArena.Player() {
                public long move(SearchRequest request) {
                    depths.add(request.depth());
                    long move = player.move(request);
                    assertEquals(SearchTermination.TIME_LIMIT, request.control().termination());
                    return move;
                }
                public void close() { player.close(); }
            };
        });
        assertEquals(List.of(candidate, best, best, candidate), players);
        assertEquals(4, depths.size()); assertTrue(depths.stream().allMatch(d -> d == com.ohinteractive.seedv6.search.exact.ExactSearch.MAX_DEPTH));
        assertEquals(4, result.statistics().totalPlies());
    }
    @Test void cancellationStopsTimedControlWithoutWaitingForBudget() throws Exception {
        var control = new ValidationControl(); var search = control.beginSearch(60000);
        control.cancel(); assertFalse(search.checkpoint()); assertEquals(SearchTermination.STOPPED, search.termination());
        control.endSearch(); assertFalse(control.beginSearch(60000).checkpoint()); control.endSearch();
    }
    @Test void oldDepthFingerprintAndBoundsStayStable() {
        var c = new ValidationConfig(64, 1, 0, 8, 4, 1, NnueScoreMapping.V1, 1024);
        assertFalse(c.timed()); assertFalse(c.toString().contains("moveMillis"));
        assertThrows(IllegalArgumentException.class, () -> new ValidationConfig(1, 1, 0, 0, 4, 1, NnueScoreMapping.V1, 2, -1));
        assertThrows(IllegalArgumentException.class, () -> new ValidationConfig(1, 1, 0, 0, 4, 1, NnueScoreMapping.V1, 2, 86400001));
    }
}
