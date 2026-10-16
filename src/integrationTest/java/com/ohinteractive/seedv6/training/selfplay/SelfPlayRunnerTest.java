package com.ohinteractive.seedv6.training.selfplay;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.search.common.IterationSnapshot;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.driver.SearchDriver;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
class SelfPlayRunnerTest {
    static final NnueNetwork NETWORK = NnueNetwork.initialized(73);
    static final NnueScoreMapping MAPPING = new NnueScoreMapping(1_000_000);
    static final String FIFTY_SOON = "4k3/8/8/8/8/8/8/R3K3 w - - 98 1";

    static SelfPlayConfig config(int games, long seed, int minOpening, int maxOpening, int cap) {
        return new SelfPlayConfig(games, 1, 1, seed, minOpening, maxOpening, 32, cap, MAPPING, -1, -1);
    }

    @Test
    void seededOpeningsAreReproducibleDiverseBoundedAndAlwaysLegal() {
        SelfPlayConfig config = config(1, 419, 2, 8, 12);
        Set<List<Long>> openings = new HashSet<>();
        for (int index = 0; index < 12; index++) {
            GameTrajectory first = openingOnly(config, index);
            GameTrajectory again = openingOnly(config, index);
            assertSameTrajectory(first, again);
            assertTrue(first.playedPlies() >= 2 && first.playedPlies() <= 8);
            assertEquals(GameTermination.SEARCH_FAILURE, first.termination());
            HeadlessGameTest.replay(first);
            openings.add(first.positions().stream().map(GameTrajectory.Position::playedMove).toList());
        }
        assertTrue(openings.size() > 1);
        Set<Long> seeds = new HashSet<>();
        for (int i = 0; i < 20; i++) seeds.add(SelfPlayRunner.gameSeed(419, i));
        assertEquals(20, seeds.size());
        Set<List<Long>> differentSeeds = new HashSet<>();
        for (int i = 0; i < 8; i++) {
            differentSeeds.add(openingOnly(config(1, i, 4, 4, 8), 0).positions().stream()
                    .map(GameTrajectory.Position::playedMove).toList());
        }
        assertTrue(differentSeeds.size() > 1);
    }

    @Test
    void zeroExplorationSearchesImmediatelyAndFailuresNeverSupplyResults() {
        AtomicInteger calls = new AtomicInteger();
        HeadlessGame game = new HeadlessGame(Board.startingPosition(), 8);
        GameTrajectory failure = SelfPlayRunner.drive(game, config(1, 3, 0, 0, 8), 0,
                new SelfPlayControl(), request -> { calls.incrementAndGet(); return 0; });
        assertEquals(1, calls.get());
        assertEquals(0, failure.playedPlies());
        assertEquals(GameTermination.SEARCH_FAILURE, failure.termination());
        assertTrue(failure.result().isEmpty());
        assertTrue(failure.failure().isPresent());
        GameTrajectory exception = openingOnly(config(1, 3, 0, 0, 8), 0);
        assertEquals(GameTermination.SEARCH_FAILURE, exception.termination());
        assertTrue(exception.result().isEmpty());
        game = new HeadlessGame(Board.startingPosition(), 8);
        game.abort(GameTermination.INFRASTRUCTURE_FAILURE, "fixture");
        assertTrue(game.trajectory().result().isEmpty());
    }

    @Test
    void actualSearchWithNodeOrTimeExhaustionAbortsInsteadOfUsingFallbackMove() {
        for (boolean nodes : new boolean[] {true, false}) {
            SelfPlayConfig config = new SelfPlayConfig(1, 3, 1, 0, 0, 0, 32, 16,
                    MAPPING, nodes ? 0 : -1, nodes ? -1 : 0);
            GameTrajectory game = SelfPlayRunner.play(NETWORK, config, 0,
                    Board.startingPosition(), new SelfPlayControl());
            assertEquals(GameTermination.SEARCH_FAILURE, game.termination());
            assertEquals(0, game.playedPlies());
            assertTrue(game.result().isEmpty());
        }
    }

    @Test
    void cancellationFromAnotherThreadStopsActualNnueSearchAndExcludesTheGame() throws Exception {
        SelfPlayControl control = new SelfPlayControl();
        CountDownLatch inSearch = new CountDownLatch(1);
        CountDownLatch cancelSent = new CountDownLatch(1);
        SelfPlayConfig config = new SelfPlayConfig(1, 8, 1, 11, 0, 0, 32, 16, MAPPING, -1, -1);
        try (var executor = Executors.newSingleThreadExecutor();
             var search = new SearchDriver(SearchEvaluation.incremental(NETWORK, MAPPING))) {
            var future = executor.submit(() -> SelfPlayRunner.drive(new HeadlessGame(Board.startingPosition(), 16),
                    config, 0, control, request -> {
                        long[] board = new long[Board.MAX_BITBOARDS];
                        request.copyBoardInto(board);
                        var outcome = search.search(new SearchRequest(board, request.gameHistory(), request.depth(),
                                new SearchObserver() {
                                    @Override public void onIterationCompleted(IterationSnapshot snapshot) {
                                        inSearch.countDown();
                                        try { assertTrue(cancelSent.await(5, TimeUnit.SECONDS)); }
                                        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
                                    }
                                }, request.control()));
                        assertFalse(outcome.targetDepthCompleted());
                        return outcome.lastCompletedResult().bestMove();
                    }));
            try {
                assertTrue(inSearch.await(5, TimeUnit.SECONDS));
            } finally {
                control.cancel();
                cancelSent.countDown();
            }
            GameTrajectory game = future.get(5, TimeUnit.SECONDS);
            assertEquals(GameTermination.CANCELLED, game.termination());
            assertTrue(game.result().isEmpty());
            assertEquals(0, game.playedPlies());
            assertThrows(IllegalArgumentException.class, () -> TrajectorySampler.sample(game, 32));
        }
    }

    @Test
    void cancellationRetainsCompletedSamplesAndCountsOnlyAttemptedGames() {
        SelfPlayControl control = new SelfPlayControl();
        SelfPlayConfig config = config(5, 1, 0, 0, 16);
        SelfPlayBatch batch = SelfPlayBatch.generate(NETWORK, config, control, index -> {
            if (index == 0) return HeadlessGameTest.foolsMate().trajectory();
            HeadlessGame game = new HeadlessGame(Board.startingPosition(), 16);
            game.play(game.legalMoves()[0]);
            control.cancel();
            game.abort(GameTermination.CANCELLED, null);
            return game.trajectory();
        });
        assertEquals(1, batch.statistics().completedGames());
        assertEquals(1, batch.statistics().abortedGames());
        assertEquals(1, batch.statistics().blackWins());
        assertEquals(3, batch.unstartedGames());
        assertEquals(5, batch.statistics().rawTrajectoryPositions());
        assertEquals(4, batch.samples().size());
        assertTrue(batch.cancelled());
        assertEquals(4, batch.statistics().minimumCompletedPlies());
        assertEquals(4, batch.statistics().maximumCompletedPlies());
        assertEquals(4.0, batch.statistics().meanCompletedPlies());
        assertSame(NETWORK, batch.generationNetwork());
    }

    @Test
    void realShallowStartingPositionGamesAreDeterministicAndCappedGamesAreExcluded() {
        SelfPlayConfig config = config(3, 2103, 2, 4, 12);
        SelfPlayBatch first = SelfPlayBatch.generate(NETWORK, config, new SelfPlayControl());
        SelfPlayBatch again = SelfPlayBatch.generate(NETWORK, config, new SelfPlayControl());
        assertEquals(first.statistics(), again.statistics());
        assertEquals(first.games(), again.games());
        assertEquals(3, first.statistics().completedGames() + first.statistics().abortedGames());
        assertTrue(first.samples().size() <= 3 * 32);
        assertEquals(first.samples().size(), again.samples().size());
        for (int i = 0; i < first.samples().size(); i++) {
            assertArrayEquals(first.samples().get(i).board(), again.samples().get(i).board());
            assertEquals(first.samples().get(i).target(), again.samples().get(i).target());
        }
        for (var game : first.games()) {
            if (game.termination().aborted()) assertEquals(0, game.sampledPositions());
            else assertTrue(game.termination().result().isPresent());
        }
        GameTrajectory actual = SelfPlayRunner.play(NETWORK, config, 0, Board.startingPosition(), new SelfPlayControl());
        GameTrajectory repeated = SelfPlayRunner.play(NETWORK, config, 0, Board.startingPosition(), new SelfPlayControl());
        assertSameTrajectory(actual, repeated);
        HeadlessGameTest.replay(actual);
    }

    @Test
    void genuineNnueMateAndDrawGamesHaveSupportedResultsAndWdlTargets() {
        GameTrajectory mate = SelfPlayRunner.play(NETWORK, config(1, 0, 0, 0, 8), 0,
                Board.fromFen("7k/6p1/5KQ1/8/8/8/8/8 w - - 0 1"), new SelfPlayControl());
        assertEquals(GameTermination.WHITE_CHECKMATES_BLACK, mate.termination());
        assertEquals(1, mate.playedPlies());
        assertEquals(1.0, TrajectorySampler.sample(mate, 32).getFirst().target());
        HeadlessGameTest.replay(mate);
        GameTrajectory draw = SelfPlayRunner.play(NETWORK, config(1, 21, 1, 1, 8), 0,
                Board.fromFen(FIFTY_SOON), new SelfPlayControl());
        assertEquals(GameTermination.FIFTY_MOVE_RULE, draw.termination());
        assertEquals(2, draw.playedPlies());
        for (var sample : TrajectorySampler.sample(draw, 32)) assertEquals(0.0, sample.target());
        HeadlessGameTest.replay(draw);
    }

    @Test
    void configurationRejectsUnboundedOrUnsupportedValues() {
        assertThrows(IllegalArgumentException.class, () -> config(0, 0, 0, 0, 8));
        assertThrows(IllegalArgumentException.class, () -> config(1, 0, -1, 0, 8));
        assertThrows(IllegalArgumentException.class, () -> config(1, 0, 3, 2, 8));
        assertThrows(IllegalArgumentException.class, () -> config(1, 0, 0, 9, 8));
        assertThrows(IllegalArgumentException.class, () -> config(1, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> config(Integer.MAX_VALUE, 0, 0, 0, 8));
        assertEquals(32, SelfPlayConfig.defaults(1, 1, MAPPING).maximumSamplesPerGame());
        assertEquals(1024, SelfPlayConfig.defaults(1, 1, MAPPING).maximumPlies());
    }

    private static GameTrajectory openingOnly(SelfPlayConfig config, int index) {
        return SelfPlayRunner.drive(new HeadlessGame(Board.startingPosition(), config.maximumPlies()), config,
                index, new SelfPlayControl(), request -> { throw new IllegalStateException("End of opening fixture"); });
    }

    static void assertSameTrajectory(GameTrajectory first, GameTrajectory other) {
        assertEquals(first.termination(), other.termination());
        assertEquals(first.playedPlies(), other.playedPlies());
        for (int i = 0; i < first.playedPlies(); i++) {
            assertArrayEquals(first.positions().get(i).board(), other.positions().get(i).board());
            assertEquals(first.positions().get(i).playedMove(), other.positions().get(i).playedMove());
        }
        assertArrayEquals(first.finalBoard(), other.finalBoard());
    }
}
