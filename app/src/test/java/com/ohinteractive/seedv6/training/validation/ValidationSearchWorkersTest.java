package com.ohinteractive.seedv6.training.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.training.selfplay.GameTermination;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.search.driver.SearchWorkerAssertions.assertCapacity;

@Timeout(30)
class ValidationSearchWorkersTest {
    private static final NnueNetwork CANDIDATE = NnueNetwork.initialized(73);
    private static final NnueNetwork BEST = NnueNetwork.initialized(74);

    @ParameterizedTest @ValueSource(ints = {1, 2, 4})
    void bothActorsAndReversedColoursUseTheConfiguredCapacity(int threads) {
        var config = new ValidationConfig(1, 8123, 0, 0, 3, threads, NnueScoreMapping.V1, 2);
        var board = Board.fromFen("r3k3/8/8/8/8/8/8/R3K3 w Qq - 0 1");
        var actors = new ArrayList<NnueNetwork>();
        var candidateMoves = new AtomicInteger();
        var bestMoves = new AtomicInteger();
        var result = new ValidationArena().validate(CANDIDATE, BEST, config, board, GameHistory.initial(board),
                new ValidationControl(), (network, c) -> {
                    actors.add(network);
                    var player = ValidationArena.search(SearchEvaluation.incremental(network, c.scoreMapping()), c);
                    assertCapacity(player, threads);
                    return new ValidationArena.Player() {
                        public long move(SearchRequest request) {
                            (network == CANDIDATE ? candidateMoves : bestMoves).incrementAndGet();
                            return player.move(request);
                        }
                        public void close() { player.close(); }
                    };
                });
        assertEquals(List.of(CANDIDATE, BEST, BEST, CANDIDATE), actors);
        assertEquals(2, candidateMoves.get());
        assertEquals(2, bestMoves.get());
        var pair = result.pairs().getFirst();
        assertEquals(GameTermination.PLY_CAP, pair.candidateWhite().termination());
        assertEquals(GameTermination.PLY_CAP, pair.candidateBlack().termination());
        assertEquals(4, result.statistics().totalPlies());
    }

    @ParameterizedTest @ValueSource(ints = {2, 4})
    void publicValidationPathCompletesBoundedPairs(int threads) {
        var config = new ValidationConfig(1, 8123, 0, 0, 3, threads, NnueScoreMapping.V1, 2);
        var board = Board.fromFen("r3k3/8/8/8/8/8/8/R3K3 w Qq - 0 1");
        var result = new ValidationArena().validate(CANDIDATE, BEST, config, board, GameHistory.initial(board),
                new ValidationControl());
        assertEquals(GameTermination.PLY_CAP, result.pairs().getFirst().candidateWhite().termination());
        assertEquals(GameTermination.PLY_CAP, result.pairs().getFirst().candidateBlack().termination());
        assertEquals(4, result.statistics().totalPlies());
    }
}
