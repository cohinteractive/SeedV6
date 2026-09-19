package com.ohinteractive.seedv6.tools.nnue.research;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class StrengthArenaTest {
    @TempDir Path root;
    static final String MATE = "7k/5Q2/6K1/8/8/8/8/8 w - - 0 1";
    static ValidationConfig resources(int pairs, int cap) { return new ValidationConfig(pairs, 314159, 0, 0, 1, 1, ResearchTool.MAPPING, cap); }
    static StrengthArena.Config config(int pairs, int cap) {
        return new StrengthArena.Config(resources(pairs, cap), StrengthArena.Mode.EVALUATION_ISOLATION, 2, 0.05);
    }
    static ValidationResult.Game game(GameTermination t) { return new ValidationResult.Game(t, 10); }
    static ValidationResult.Pair win() {
        return new ValidationResult.Pair("opening", game(GameTermination.WHITE_CHECKMATES_BLACK), game(GameTermination.BLACK_CHECKMATES_WHITE));
    }
    static ValidationResult result(List<ValidationResult.Pair> pairs) {
        long[] board = Board.startingPosition();
        return new ValidationResult(resources(pairs.size(), 1024), ValidationArena.stateHash(board, GameHistory.initial(board)), pairs);
    }
    Path checkpoint(long seed) throws Exception {
        Path storeRoot = root.resolve("store" + seed);
        try (var store = new CheckpointStore(storeRoot)) {
            var c = store.initialize(new NnueTrainer(TrainableNnue.initialized(seed)), new CheckpointManifest.Metadata(0, 1, ""));
            return storeRoot.resolve("checkpoints").resolve(c.manifest().id());
        }
    }
    @Test void policiesAreEqualInIsolationAndDistinctInPracticalMode() throws Exception {
        var nnue = StrengthArena.Actor.checkpoint(checkpoint(1)); var handcrafted = StrengthArena.Actor.handcrafted();
        var cfg = resources(1, 16);
        var a = nnue.evaluation(StrengthArena.Mode.EVALUATION_ISOLATION, cfg);
        var b = handcrafted.evaluation(StrengthArena.Mode.EVALUATION_ISOLATION, cfg);
        assertEquals(a.selectiveSearchPolicy(), b.selectiveSearchPolicy());
        assertTrue(a.selectiveSearchPolicy().mateDistanceBounds());
        assertFalse(b.selectiveSearchPolicy().futility()); assertFalse(b.selectiveSearchPolicy().razoring());
        assertFalse(a.usesAspiration()); assertFalse(b.usesAspiration());
        var production = handcrafted.evaluation(StrengthArena.Mode.PRACTICAL_ENGINE, cfg);
        assertEquals(SearchEvaluation.handcrafted().selectiveSearchPolicy(), production.selectiveSearchPolicy());
        assertTrue(production.usesAspiration()); assertTrue(production.selectiveSearchPolicy().futility());
        assertFalse(nnue.evaluation(StrengthArena.Mode.PRACTICAL_ENGINE, cfg).usesAspiration());
    }
    @Test void pairedIdentityAndExactColourReversalIncludeHistoryRightsEpAndClocks() {
        long[] board = Board.fromFen("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2");
        var history = GameHistory.initial(board);
        var a = new StrengthArena.Actor("A", null); var b = new StrengthArena.Actor("B", null);
        List<String> construction = new ArrayList<>(), starts = new ArrayList<>();
        var result = new StrengthArena().match(a, b, config(1, 1), board, history, new ValidationControl(), (actor, config) -> {
            construction.add(actor.id());
            return request -> {
                long[] position = new long[Board.MAX_BITBOARDS]; request.copyBoardInto(position);
                starts.add(actor.id() + ":" + ValidationArena.stateHash(position, request.gameHistory()));
                return new HeadlessGame(position, request.gameHistory(), 1).legalMoves()[0];
            };
        });
        String identity = ValidationArena.stateHash(board, history);
        assertEquals(List.of("A", "B", "B", "A"), construction);
        assertEquals(List.of("A:" + identity, "B:" + identity), starts);
        assertEquals(identity, result.games().pairs().getFirst().openingHash());
        assertEquals(1, result.games().statistics().incompletePairs());
        assertEquals(0, result.games().statistics().wins());
    }
    @Test void deterministicRandomOpeningIsActorIndependentAndCopiesEveryStateBit() {
        var cfg = new ValidationConfig(2, 45, 4, 8, 1, 1, ResearchTool.MAPPING, 100);
        var board = Board.startingPosition(); var history = GameHistory.initial(board);
        var a = ValidationArena.opening(board, history, cfg, 1);
        var b = ValidationArena.opening(board, history, cfg, 1);
        assertEquals(a.identity(), b.identity()); assertArrayEquals(a.board(), b.board());
        assertEquals(a.history().size(), b.history().size());
        for (int i = 0; i < a.history().size(); i++) assertEquals(a.history().keyAt(i), b.history().keyAt(i));
        a.board()[0] = 0; assertEquals(a.identity(), b.identity());
        assertNotEquals(a.identity(), ValidationArena.opening(board, history, cfg, 0).identity());
    }
    @Test void incompletePairExcludesBothGamesButRetainsAllPliesAndReasons() {
        var invalid = new ValidationResult.Pair("x", game(GameTermination.WHITE_CHECKMATES_BLACK), game(GameTermination.PLY_CAP));
        var mixed = result(List.of(win(), invalid)); var s = mixed.statistics();
        assertEquals(1, s.validPairs()); assertEquals(1, s.incompletePairs());
        assertEquals(2, s.wins()); assertEquals(0, s.draws()); assertEquals(0, s.losses());
        assertEquals(1, s.white().wins()); assertEquals(1, s.black().wins()); assertEquals(40, s.totalPlies());
        assertEquals(StrengthArena.Conclusion.INSUFFICIENT_EVIDENCE, StrengthArena.assess(s, 1, 0.05).conclusion());
    }
    @Test void scoreArithmeticColourAccountingAndHoeffdingInterval() {
        var draws = new ValidationResult.Pair("x", game(GameTermination.STALEMATE), game(GameTermination.THREEFOLD_REPETITION));
        var loss = new ValidationResult.Pair("x", game(GameTermination.BLACK_CHECKMATES_WHITE), game(GameTermination.WHITE_CHECKMATES_BLACK));
        var s = result(List.of(win(), win(), draws, loss)).statistics();
        var e = StrengthArena.assess(s, 4, 0.05);
        assertEquals(4, s.wins()); assertEquals(2, s.draws()); assertEquals(2, s.losses());
        assertEquals(0.625, e.mean()); assertEquals(62.5, e.percentage());
        assertEquals(Math.sqrt(Math.log(40) / 8), e.radius(), 1e-15);
        assertEquals(400 * Math.log10(0.625 / 0.375), e.eloEquivalent().orElseThrow());
        assertEquals(StrengthArena.Conclusion.NO_CLEAR_ADVANTAGE, e.conclusion());
    }
    @Test void everyConclusionAndEndpointEloAreExplicit() {
        var wins = result(Collections.nCopies(32, win())).statistics();
        assertEquals(StrengthArena.Conclusion.CLEAR_A_ADVANTAGE, StrengthArena.assess(wins, 32, .05).conclusion());
        assertTrue(StrengthArena.assess(wins, 32, .05).eloEquivalent().isEmpty());
        var lose = new ValidationResult.Pair("x", game(GameTermination.BLACK_CHECKMATES_WHITE), game(GameTermination.WHITE_CHECKMATES_BLACK));
        assertEquals(StrengthArena.Conclusion.CLEAR_B_ADVANTAGE, StrengthArena.assess(result(Collections.nCopies(32, lose)).statistics(), 32, .05).conclusion());
        assertEquals(StrengthArena.Conclusion.INSUFFICIENT_EVIDENCE, StrengthArena.assess(wins, 33, .05).conclusion());
        var capped = new ValidationResult.Pair("x", game(GameTermination.PLY_CAP), game(GameTermination.CANCELLED));
        var empty = StrengthArena.assess(result(List.of(capped)).statistics(), 1, .05);
        assertTrue(Double.isNaN(empty.mean())); assertEquals(0, empty.lower()); assertEquals(1, empty.upper());
        assertThrows(IllegalArgumentException.class, () -> StrengthArena.assess(wins, 1, Double.NaN));
    }
    @Test void realHandcraftedAndPersistedNnueSmokeAndNnueVersusNnueNeverMutateStore() throws Exception {
        var path = checkpoint(11); var other = checkpoint(12);
        Map<Path, String> before = hashes(root);
        var nnue = StrengthArena.Actor.checkpoint(path);
        long[] board = Board.fromFen(MATE);
        for (var mode : StrengthArena.Mode.values()) {
            var cfg = new StrengthArena.Config(resources(1, 8), mode, 1, .05);
            var result = new StrengthArena().match(StrengthArena.Actor.handcrafted(), nnue, cfg, board,
                    GameHistory.initial(board), new ValidationControl(), StrengthArena::search);
            assertEquals(1, result.games().statistics().validPairs());
            assertEquals(1, result.games().statistics().wins()); assertEquals(1, result.games().statistics().losses());
            var replay = new StrengthArena().match(StrengthArena.Actor.handcrafted(), nnue, cfg, board,
                    GameHistory.initial(board), new ValidationControl(), StrengthArena::search);
            assertEquals(result, replay);
        }
        var result = new StrengthArena().match(nnue, StrengthArena.Actor.checkpoint(other), config(1, 8), board,
                GameHistory.initial(board), new ValidationControl(), StrengthArena::search);
        assertEquals(1, result.games().statistics().validPairs());
        assertEquals(before, hashes(root));
    }
    @Test void failureAndCancellationCannotBecomeDraws() {
        long[] board = Board.startingPosition();
        var control = new ValidationControl(); control.cancel();
        var result = new StrengthArena().match(StrengthArena.Actor.handcrafted(), StrengthArena.Actor.handcrafted(), config(2, 10),
                board, GameHistory.initial(board), control, (a, c) -> { fail("Cancelled search was constructed."); return null; });
        assertEquals(2, result.games().statistics().incompletePairs()); assertEquals(0, result.games().statistics().draws());
        var failed = new StrengthArena().match(StrengthArena.Actor.handcrafted(), StrengthArena.Actor.handcrafted(), config(1, 10),
                board, GameHistory.initial(board), new ValidationControl(), (a, c) -> request -> { throw new IllegalStateException("fixture"); });
        assertEquals(2, failed.games().statistics().terminations().get(GameTermination.SEARCH_FAILURE));
    }
    @Test void publicCancellationReportsEveryUnstartedPairToObserver() {
        ValidationControl control = new ValidationControl(); control.cancel();
        List<ValidationResult.Pair> observed = new ArrayList<>();
        var result = new StrengthArena().match(StrengthArena.Actor.handcrafted(), StrengthArena.Actor.handcrafted(),
                config(3, 10), control, observed::add);
        assertEquals(result.games().pairs(), observed);
        assertEquals(3, result.games().statistics().incompletePairs());
        assertEquals(0, result.games().statistics().draws());
    }
    static Map<Path, String> hashes(Path root) throws Exception {
        Map<Path, String> result = new HashMap<>();
        try (var paths = Files.walk(root)) {
            for (Path p : paths.filter(Files::isRegularFile).toList()) result.put(p, HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p))));
        }
        return result;
    }
}
