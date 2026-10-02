package com.ohinteractive.seedv6.training.selfplay;

import java.util.stream.Stream;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.BrnModel;
import com.ohinteractive.seedv6.core.brn1.Brn1Model;
import com.ohinteractive.seedv6.core.brn2.Brn2Model;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.search.driver.SearchWorkerAssertions.assertCapacity;

@Timeout(30)
class SelfPlaySearchWorkersTest {
    private static final String FEN = "r3k3/8/8/8/8/8/8/R3K3 w Qq - 0 1";
    private static final NnueNetwork NETWORK = NnueNetwork.initialized(73);

    private static SelfPlayConfig config(int threads) {
        return new SelfPlayConfig(1, 3, threads, 8123, 0, 0, 2, 2, NnueScoreMapping.V1, -1, -1);
    }

    @ParameterizedTest @ValueSource(ints = {1, 2, 4})
    void configuredCapacityAndRealNeuralMoveSearches(int threads) {
        var config = config(threads);
        try (var driver = SelfPlayRunner.search(SearchEvaluation.incremental(NETWORK), config)) {
            assertCapacity(driver, threads);
        }
        var game = SelfPlayRunner.play(NETWORK, config, 0, Board.fromFen(FEN), new SelfPlayControl());
        assertEquals(GameTermination.PLY_CAP, game.termination(), game.failure().orElse(""));
        assertEquals(2, game.playedPlies());
        HeadlessGameTest.replay(game);
    }

    static Stream<SearchEvaluation> evaluators() {
        return Stream.of(SearchEvaluation.handcrafted(), SearchEvaluation.incremental(NETWORK),
                SearchEvaluation.brn(new BrnModel()), SearchEvaluation.brn1(new Brn1Model()),
                SearchEvaluation.brn2(new Brn2Model()));
    }

    @ParameterizedTest @MethodSource("evaluators")
    void allTrainingEvaluatorsCompleteBoundedFourWorkerGames(SearchEvaluation evaluation) {
        var config = config(4);
        try (var driver = SelfPlayRunner.search(evaluation, config)) { assertCapacity(driver, 4); }
        var game = SelfPlayRunner.play(evaluation, config, 0, Board.fromFen(FEN), new SelfPlayControl());
        assertEquals(GameTermination.PLY_CAP, game.termination(), game.failure().orElse(""));
        assertEquals(2, game.playedPlies());
        HeadlessGameTest.replay(game);
    }
}
