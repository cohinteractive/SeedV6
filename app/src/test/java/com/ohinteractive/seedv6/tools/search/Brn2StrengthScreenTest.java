package com.ohinteractive.seedv6.tools.search;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn2.Brn2Model;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;
import com.ohinteractive.seedv6.search.manage.ManagedSearchResult;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(60)
class Brn2StrengthScreenTest {
    @TempDir Path root;
    static Brn2StrengthScreen.Config config(int pairs, int cap) {
        return new Brn2StrengthScreen.Config(pairs, 20260923, 8, 50, cap);
    }
    static Brn2StrengthScreen.Actor brn() {
        return new Brn2StrengthScreen.Actor("brn", new NetworkModel.Brn2(new Brn2Model()), Map.of());
    }
    static Brn2StrengthScreen.Actor nnue() {
        return new Brn2StrengthScreen.Actor("nnue", new NetworkModel.Nnue(NnueNetwork.initialized(1)), Map.of());
    }
    static ValidationArena.Opening opening(String fen) {
        long[] board = Board.fromFen(fen);
        return new ValidationArena.Opening(board, GameHistory.initial(board), 0);
    }
    static ManagedSearchResult legalFallback(HeadlessGame game) {
        return new ManagedSearchResult(1, game.legalMoves()[0], true, null, SearchTermination.TIME_LIMIT,
                0, null, SearchDiagnosticsSnapshot.enabledEmpty());
    }
    @Test void selectedPayloadsLoadReadOnlyAndRejectIdentityOrArchitectureMismatch() throws Exception {
        for (var model : List.of(brn().model(), nnue().model())) {
            Path file = root.resolve(model.architecture().networkFile());
            try (var out = Files.newOutputStream(file)) { model.write(out); }
            byte[] before = Files.readAllBytes(file);
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(before));
            var actor = Brn2StrengthScreen.load("fixture", file.toString(), model.architecture(), hash);
            assertEquals(model.architecture(), actor.model().architecture());
            assertEquals(hash, actor.identity().get("modelSha256"));
            assertArrayEquals(before, Files.readAllBytes(file));
            assertThrows(IOException.class, () -> Brn2StrengthScreen.load("x", file.toString(), model.architecture(), "0".repeat(64)));
            var wrong = model.architecture() == TrainingArchitecture.BRN2 ? TrainingArchitecture.NNUE : TrainingArchitecture.BRN2;
            assertThrows(IOException.class, () -> Brn2StrengthScreen.load("x", file.toString(), wrong, hash));
        }
        assertThrows(IllegalArgumentException.class, () -> Brn2StrengthScreen.load("x", "initialized", TrainingArchitecture.BRN2, ""));
    }
    @Test void sixtyFourSharedOpeningsAreUniqueRepeatableAndPreserveFullHistory() {
        var cfg = config(64, 1024);
        var first = Brn2StrengthScreen.openings(cfg);
        var again = Brn2StrengthScreen.openings(cfg);
        assertEquals(64, first.stream().map(ValidationArena.Opening::identity).distinct().count());
        for (int i = 0; i < 64; i++) {
            assertEquals(8, first.get(i).randomizedPlies());
            assertEquals(9, first.get(i).history().size());
            assertEquals(first.get(i).identity(), again.get(i).identity());
            assertArrayEquals(first.get(i).board(), again.get(i).board());
        }
        assertEquals(0, cfg.limits().depth()); assertEquals(-1, cfg.limits().nodes());
        assertEquals(50, cfg.limits().timeMillis());
    }
    @Test void swappingColoursPreservesOpeningAndRecordingAndCapsAreNotDraws() throws Exception {
        var a = brn(); var b = nnue(); var cfg = config(1, 8);
        var op = Brn2StrengthScreen.openings(cfg).getFirst();
        List<String> construction = new ArrayList<>(), starts = new ArrayList<>();
        StringWriter text = new StringWriter();
        var pair = Brn2StrengthScreen.playPair("fixture", 0, op, a, b, cfg, new PrintWriter(text), actor -> {
            construction.add(actor.id());
            return (game, c) -> {
                if (game.playedPlies() == 0) starts.add(ValidationArena.stateHash(game.boardSnapshot(), game.historySnapshot()));
                return legalFallback(game);
            };
        });
        assertEquals(List.of("brn", "nnue", "nnue", "brn"), construction);
        assertEquals(List.of(op.identity(), op.identity()), starts);
        assertFalse(pair.valid());
        assertEquals(GameTermination.PLY_CAP, pair.candidateWhite().termination());
        var result = new ValidationResult(cfg.validation(1), op.identity(), List.of(pair));
        assertEquals(0, result.statistics().draws());
        assertEquals(1, result.statistics().incompletePairs());
        assertTrue(text.toString().contains("\"white\":\"nnue\",\"black\":\"brn\""));
        assertTrue(text.toString().contains("\"type\":\"pair\""));
    }
    @Test void realMixedTimedSearchUsesNativeLifecycleAndAdjudicatesMate() throws Exception {
        var op = opening("7k/5Q2/6K1/8/8/8/8/8 w - - 0 1");
        var cfg = new Brn2StrengthScreen.Config(1, 1, 0, 100, 16);
        var pair = Brn2StrengthScreen.playPair("mixed", 0, op, brn(), nnue(), cfg,
                new PrintWriter(new StringWriter()), Brn2StrengthScreen::player);
        assertTrue(pair.valid());
        assertEquals(GameTermination.WHITE_CHECKMATES_BLACK, pair.candidateWhite().termination());
        assertEquals(GameTermination.WHITE_CHECKMATES_BLACK, pair.candidateBlack().termination());
        assertEquals(0.5, pair.score());
    }
    @Test void ruleDrawAndSearchFailureKeepTheirExistingMeanings() throws Exception {
        var a = brn(); var b = nnue(); var cfg = config(1, 1024);
        var op = opening("7k/8/6K1/8/8/8/8/8 w - - 0 1");
        var draw = Brn2StrengthScreen.playPair("draw", 1, op, a, b, cfg, new PrintWriter(new StringWriter()),
                actor -> (g, c) -> { throw new AssertionError("Terminal position searched"); });
        assertTrue(draw.valid()); assertEquals(0.5, draw.score());
        var failed = Brn2StrengthScreen.playPair("fail", 0, Brn2StrengthScreen.openings(cfg).getFirst(), a, b, cfg,
                new PrintWriter(new StringWriter()), actor -> (g, c) -> { throw new IllegalStateException("fixture"); });
        assertFalse(failed.valid()); assertEquals(GameTermination.SEARCH_FAILURE, failed.candidateWhite().termination());
    }
    @Test void nativeTimeFallbackIsAcceptedButFailureWithLegalMoveIsRejected() {
        var game = new HeadlessGame(Board.startingPosition(), 8);
        assertDoesNotThrow(() -> Brn2StrengthScreen.requireUsable(legalFallback(game)));
        var failed = new ManagedSearchResult(1, game.legalMoves()[0], true, null, SearchTermination.FAILURE,
                0, new IllegalStateException("failure"), SearchDiagnosticsSnapshot.enabledEmpty());
        assertThrows(IllegalStateException.class, () -> Brn2StrengthScreen.requireUsable(failed));
    }
}
