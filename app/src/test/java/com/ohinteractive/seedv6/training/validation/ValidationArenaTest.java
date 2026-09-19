package com.ohinteractive.seedv6.training.validation;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.selfplay.GameTermination;
import com.ohinteractive.seedv6.training.selfplay.HeadlessGame;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
class ValidationArenaTest {
    static final NnueNetwork CANDIDATE = NnueNetwork.initialized(73);
    static final NnueNetwork INCUMBENT = NnueNetwork.initialized(74);
    static final String[] WHITE_WIN = {"e2e4", "e7e5", "f1c4", "b8c6", "d1h5", "g8f6", "h5f7"};
    static final String[] BLACK_WIN = {"f2f3", "e7e5", "g2g4", "d8h4"};
    static ValidationConfig config(int pairs, int min, int max, int cap) {
        return new ValidationConfig(pairs, 8123, min, max, 1, 1, new NnueScoreMapping(1_000_000), cap);
    }

    @Test void candidateWinsBothColoursFromActualLegalScriptedGames() {
        var result = scripted(WHITE_WIN, BLACK_WIN);
        var stats = result.statistics();
        assertEquals(1, stats.validPairs());
        assertEquals(0, stats.incompletePairs());
        assertEquals(2, stats.wins());
        assertEquals(new ValidationResult.ColourRecord(1, 0, 0), stats.white());
        assertEquals(stats.white(), stats.black());
        assertEquals(1.0, result.pairs().getFirst().score());
        assertEquals(11, stats.totalPlies());
        assertEquals(PromotionPolicy.Decision.PROMOTE, result.assess(new PromotionPolicy(1, 0.9, 0)).decision());
    }
    @Test void sameWhiteWinWithReversedActorsIsASplitPair() {
        var result = scripted(WHITE_WIN, WHITE_WIN);
        assertEquals(1, result.statistics().wins());
        assertEquals(1, result.statistics().losses());
        assertEquals(new ValidationResult.ColourRecord(1, 0, 0), result.statistics().white());
        assertEquals(new ValidationResult.ColourRecord(0, 0, 1), result.statistics().black());
        assertEquals(0.5, result.pairs().getFirst().score());
    }
    @Test void oneFailedGameInvalidatesTheWholePairIncludingItsCompletedWin() {
        var result = scripted(WHITE_WIN, new String[] {"invalid"});
        assertEquals(0, result.statistics().validPairs());
        assertEquals(1, result.statistics().incompletePairs());
        assertEquals(0, result.statistics().wins() + result.statistics().draws() + result.statistics().losses());
        assertEquals(GameTermination.SEARCH_FAILURE, result.pairs().getFirst().candidateBlack().termination());
        assertThrows(IllegalStateException.class, () -> result.pairs().getFirst().score());
    }
    @Test void openingsAreSeededLegalDiverseAndCopyExactBoardHistory() {
        long[] root = Board.startingPosition();
        GameHistory history = GameHistory.initial(root);
        Set<String> identities = new HashSet<>();
        for (int index = 0; index < 12; index++) {
            var a = ValidationArena.opening(root, history, config(12, 2, 8, 16), index);
            var b = ValidationArena.opening(root, history, config(12, 2, 8, 16), index);
            assertEquals(a.identity(), b.identity());
            assertArrayEquals(a.board(), b.board());
            assertTrue(a.randomizedPlies() >= 2 && a.randomizedPlies() <= 8);
            assertEquals(a.randomizedPlies() + 1, a.history().size());
            identities.add(a.identity());
            var first = a.newGame(16);
            var second = a.newGame(16);
            assertArrayEquals(first.boardSnapshot(), second.boardSnapshot());
            assertHistory(first.historySnapshot(), second.historySnapshot());
            first.play(first.legalMoves()[0]);
            assertArrayEquals(a.board(), second.boardSnapshot());
        }
        assertTrue(identities.size() > 1);
    }
    @Test void suppliedRepetitionHistorySurvivesOpeningAndBothGames() {
        HeadlessGame prior = new HeadlessGame(Board.startingPosition(), 32);
        for (String move : new String[] {"g1f3", "g8f6", "f3g1", "f6g8"}) play(prior, move);
        var opening = ValidationArena.opening(prior.boardSnapshot(), prior.historySnapshot(), config(1, 0, 0, 16), 0);
        for (int game = 0; game < 2; game++) {
            HeadlessGame next = opening.newGame(16);
            for (String move : new String[] {"g1f3", "g8f6", "f3g1", "f6g8"}) play(next, move);
            assertEquals(GameTermination.THREEFOLD_REPETITION, next.termination());
            assertEquals(9, next.historySnapshot().size());
        }
        assertNotEquals(opening.identity(), ValidationArena.stateHash(opening.board(), GameHistory.initial(opening.board())));
    }
    @Test void openingCopiesCastlingEnPassantSideAndClocksWithoutFenReconstruction() {
        for (String fen : List.of("r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 37 21",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2")) {
            long[] board = Board.fromFen(fen);
            var opening = ValidationArena.opening(board, GameHistory.initial(board), config(1, 0, 0, 8), 0);
            assertArrayEquals(board, opening.newGame(8).boardSnapshot());
            assertHistory(GameHistory.initial(board), opening.history());
        }
    }
    @Test void eachGameCreatesFreshPlayersBoundToTheCorrectImmutableNetworks() {
        List<NnueNetwork> actors = new ArrayList<>();
        List<ValidationArena.Player> players = new ArrayList<>();
        List<String> roots = new ArrayList<>();
        long[] board = Board.fromFen("4k3/8/8/8/8/8/8/R3K3 w - - 99 1");
        var cfg = config(1, 0, 0, 8);
        var result = new ValidationArena().validate(CANDIDATE, INCUMBENT, cfg, board, GameHistory.initial(board),
                new ValidationControl(), (network, settings) -> {
                    assertEquals(cfg, settings);
                    actors.add(network);
                    ValidationArena.Player player = request -> {
                        long[] current = new long[6]; request.copyBoardInto(current);
                        roots.add(ValidationArena.stateHash(current, request.gameHistory()));
                        return new HeadlessGame(current, request.gameHistory(), 8).legalMoves()[0];
                    };
                    players.add(player);
                    return player;
                });
        assertEquals(List.of(CANDIDATE, INCUMBENT, INCUMBENT, CANDIDATE), actors);
        assertEquals(4, new HashSet<>(players).size());
        assertEquals(2, roots.size());
        assertEquals(roots.get(0), roots.get(1));
        assertEquals(2, result.statistics().draws());
        assertEquals(0.5, result.pairs().getFirst().score());
    }
    @Test void cancellationAndInfrastructureFailuresNeverBecomeDraws() {
        var control = new ValidationControl();
        control.cancel();
        var result = new ValidationArena().validate(CANDIDATE, INCUMBENT, config(3, 0, 0, 8), control);
        assertEquals(3, result.statistics().incompletePairs());
        assertEquals(0, result.statistics().draws());
        long[] root = Board.startingPosition();
        var failure = new ValidationArena().validate(CANDIDATE, INCUMBENT, config(1, 0, 0, 8), root,
                GameHistory.initial(root), new ValidationControl(), (network, settings) -> { throw new IllegalStateException("fixture"); });
        assertEquals(2, failure.statistics().terminations().get(GameTermination.INFRASTRUCTURE_FAILURE));
        assertEquals(1, failure.statistics().incompletePairs());
    }
    @Test void cancellationInterruptsTheOwnedSearchControl() throws Exception {
        var control = new ValidationControl();
        CountDownLatch searching = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var future = executor.submit(() -> {
                var active = control.beginSearch();
                searching.countDown();
                while (active.checkpoint()) Thread.onSpinWait();
                control.endSearch();
                return active.termination();
            });
            assertTrue(searching.await(5, TimeUnit.SECONDS));
            control.cancel();
            assertEquals(com.ohinteractive.seedv6.search.common.SearchTermination.STOPPED, future.get(5, TimeUnit.SECONDS));
        }
    }
    @Test void realNnueMatePairIsDeterministicAndColourNeutral() {
        long[] root = Board.fromFen("7k/6p1/5KQ1/8/8/8/8/8 w - - 0 1");
        var cfg = config(1, 0, 0, 8);
        var arena = new ValidationArena();
        var first = arena.validate(CANDIDATE, INCUMBENT, cfg, root, GameHistory.initial(root), new ValidationControl());
        var again = arena.validate(CANDIDATE, INCUMBENT, cfg, root, GameHistory.initial(root), new ValidationControl());
        assertEquals(first, again);
        assertEquals(1, first.statistics().validPairs());
        assertEquals(1, first.statistics().wins());
        assertEquals(1, first.statistics().losses());
        assertEquals(2, first.statistics().totalPlies());
        assertEquals(first.assess(PromotionPolicy.DEFAULT), again.assess(PromotionPolicy.DEFAULT));
        System.out.println("REAL_NNUE_MATE_PAIR " + first.statistics() + " " + first.assess(PromotionPolicy.DEFAULT));
    }
    @Test void realStartingPositionSearchIsBoundedAndDeterministic() {
        var arena = new ValidationArena();
        var cfg = config(1, 2, 2, 2);
        var first = arena.validate(CANDIDATE, INCUMBENT, cfg, new ValidationControl());
        var again = arena.validate(CANDIDATE, INCUMBENT, cfg, new ValidationControl());
        assertEquals(first, again);
        assertEquals(1, first.statistics().incompletePairs());
        assertEquals(0, first.statistics().draws());
        assertEquals(4, first.statistics().totalPlies());
    }
    @Test void configurationRejectsUnsupportedAndUnboundedValues() {
        assertThrows(IllegalArgumentException.class, () -> config(0, 0, 0, 8));
        assertThrows(IllegalArgumentException.class, () -> config(1, -1, 0, 8));
        assertThrows(IllegalArgumentException.class, () -> config(1, 2, 1, 8));
        assertThrows(IllegalArgumentException.class, () -> config(1, 0, 9, 8));
        assertThrows(IllegalArgumentException.class, () -> config(1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ValidationConfig(1, 1, 0, 0, 0, 1, new NnueScoreMapping(1), 8));
        assertThrows(IllegalArgumentException.class, () -> new ValidationConfig(1, 1, 0, 0, 1, 0, new NnueScoreMapping(1), 8));
    }
    private static ValidationResult scripted(String[] gameA, String[] gameB) {
        AtomicInteger creations = new AtomicInteger();
        long[] board = Board.startingPosition();
        return new ValidationArena().validate(CANDIDATE, INCUMBENT, config(1, 0, 0, 16), board,
                GameHistory.initial(board), new ValidationControl(), (network, settings) -> {
                    String[] script = creations.getAndIncrement() < 2 ? gameA : gameB;
                    return request -> {
                        long[] current = new long[6]; request.copyBoardInto(current);
                        HeadlessGame game = new HeadlessGame(current, request.gameHistory(), 16);
                        String coordinate = script[request.gameHistory().size() - 1];
                        return find(game, coordinate);
                    };
                });
    }
    private static long find(HeadlessGame game, String coordinate) {
        return Arrays.stream(game.legalMoves()).filter(move -> Move.coordinate(move).equals(coordinate)).findFirst().orElseThrow();
    }
    private static void play(HeadlessGame game, String coordinate) { game.play(find(game, coordinate)); }
    private static void assertHistory(GameHistory a, GameHistory b) {
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) assertEquals(a.keyAt(i), b.keyAt(i));
    }
}

