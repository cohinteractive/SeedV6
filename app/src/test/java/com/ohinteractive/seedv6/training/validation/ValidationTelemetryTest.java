package com.ohinteractive.seedv6.training.validation;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.selfplay.HeadlessGame;
import static com.ohinteractive.seedv6.training.validation.ValidationArenaTest.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
class ValidationTelemetryTest {
    @Test void actualNnueV1ResultsOpeningsEvidenceAndDecisionsMatchWithTelemetryOffOnAndThrowing() {
        var arena = new ValidationArena();
        for (String fen : List.of("7k/6p1/5KQ1/8/8/8/8/8 w - - 0 1", "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")) {
            long[] board = Board.fromFen(fen);
            boolean mate = fen.startsWith("7k");
            var cfg = new ValidationConfig(2, 8123, mate ? 0 : 2, mate ? 0 : 2, 1, 1, NnueScoreMapping.V1, 4);
            var history = GameHistory.initial(board);
            var off = arena.validate(CANDIDATE, INCUMBENT, cfg, board, history, new ValidationControl());
            var noop = arena.validate(CANDIDATE, INCUMBENT, cfg, board, history, new ValidationControl(), p -> {});
            List<ValidationProgress> seen = new ArrayList<>();
            long start = System.nanoTime();
            var on = arena.validate(CANDIDATE, INCUMBENT, cfg, board, history, new ValidationControl(), p -> seen.add(p));
            long onNanos = System.nanoTime() - start;
            AtomicInteger calls = new AtomicInteger();
            var throwing = arena.validate(CANDIDATE, INCUMBENT, cfg, board, history, new ValidationControl(), p -> {
                calls.incrementAndGet(); throw new IllegalStateException("broken consumer");
            });
            var assertion = arena.validate(CANDIDATE, INCUMBENT, cfg, board, history, new ValidationControl(), p -> { throw new AssertionError("broken assertion"); });
            assertEquals(off, noop); assertEquals(off, on); assertEquals(off, throwing); assertEquals(off, assertion);
            assertEquals(1, calls.get(), "Broken consumer is detached");
            assertEquals(off.assess(PromotionPolicy.DEFAULT), on.assess(PromotionPolicy.DEFAULT));
            var end = seen.getLast();
            var moves = seen.stream().filter(p -> p.gameActive() && p.currentGamePlies() > 0).toList();
            assertFalse(moves.isEmpty());
            for(var progress : moves) {
                var move = progress.lastMoveSearch().orElseThrow();
                assertEquals(cfg.depth(), move.depth());
                assertTrue(move.nodes() > 0); assertTrue(move.elapsedMillis() >= 0);
                assertTrue(move.nps() == -1 || move.nps() >= 0);
            }
            assertTrue(seen.stream().filter(p -> p.currentGamePlies() == 0).allMatch(p -> p.lastMoveSearch().isEmpty()),
                    "New games never display the preceding game's move metrics");
            assertTrue(end.withoutCurrentGame().lastMoveSearch().isEmpty());
            assertEquals(off.statistics().white(), end.white()); assertEquals(off.statistics().black(), end.black());
            assertEquals(off.statistics().terminations(), end.terminations());
            assertEquals(off.statistics().validPairs(), end.validPairs());
            assertEquals(off.statistics().incompletePairs(), end.incompletePairs());
            assertEquals(off.statistics().totalPlies(), end.totalCompletedGamePlies());
            // Initial + final, six state transitions per pair, exactly one publication per applied move.
            assertEquals(2 + 6 * cfg.openingPairs() + off.statistics().totalPlies(), seen.size());
            System.out.println("TELEMETRY_NNUE_EQUAL mapping=" + cfg.scoreMapping() + " evidence=" + off.statistics()
                    + " decision=" + on.assess(PromotionPolicy.DEFAULT).decision() + " publications=" + seen.size()
                    + " observedOnMs=" + onNanos / 1_000_000.0);
        }
    }

    @Test void moveBoundaryTraceAndEvidenceAreUnchangedAndOnePublicationPerMove() {
        List<String> offTrace = new ArrayList<>(), onTrace = new ArrayList<>();
        List<ValidationProgress> seen = new ArrayList<>();
        var off = scripted(offTrace, null, () -> 0);
        AtomicInteger ticks = new AtomicInteger();
        var on = scripted(onTrace, seen::add, () -> ticks.getAndIncrement() * 1_000_000L);
        assertEquals(off, on); assertEquals(offTrace, onTrace);
        assertEquals(11, onTrace.size()); assertEquals(19, seen.size());
        for (int game = 1; game <= 2; game++) {
            int ordinal = game;
            var active = seen.stream().filter(p -> p.gameActive() && p.gameOrdinal() == ordinal).toList();
            int length = game == 1 ? 7 : 4;
            assertEquals(length + 1, active.size());
            for (int ply = 0; ply <= length; ply++) assertEquals(ply, active.get(ply).currentGamePlies());
        }
        assertTrue(seen.stream().filter(p -> p.validPairs() == 0).allMatch(p -> p.wins() == 0));
    }

    @Test void preCancelledArenaKeepsExistingAttemptBudgetAndCancelledAccounting() {
        var control = new ValidationControl(); control.cancel();
        List<ValidationProgress> seen = new ArrayList<>();
        long[] root = Board.startingPosition();
        var result = new ValidationArena().validate(CANDIDATE, INCUMBENT, config(3, 0, 0, 8), root,
                GameHistory.initial(root), control, p -> seen.add(p));
        assertEquals(3, result.pairs().size()); assertEquals(3, seen.getLast().incompletePairs());
        assertEquals(result.statistics().terminations(), seen.getLast().terminations());
        assertEquals(0, seen.getLast().completedGames()); assertTrue(seen.getLast().complete());
        assertTrue(seen.stream().noneMatch(ValidationProgress::gameActive));
    }

    @Test void consumerThrowingInsideMoveBoundaryCannotTurnTheGameIntoSearchFailure() {
        List<String> offTrace = new ArrayList<>(), failedTrace = new ArrayList<>();
        var off = scripted(offTrace, null, () -> 0);
        AtomicInteger calls = new AtomicInteger();
        var failed = scripted(failedTrace, p -> {
            calls.incrementAndGet();
            if (p.currentGamePlies() == 1) throw new IllegalArgumentException("consumer failed after move");
        }, () -> 0);
        assertEquals(off, failed); assertEquals(offTrace, failedTrace);
        assertEquals(4, calls.get(), "No further consumer calls after its first failure");
    }

    @Test void actualIncompletePairNeverLeaksGameAWinIntoLiveEvidence() {
        List<ValidationProgress> seen = new ArrayList<>();
        var result = scripted(new ArrayList<>(), seen::add, () -> 0, true);
        assertEquals(1, result.statistics().incompletePairs());
        assertTrue(seen.stream().allMatch(p -> p.wins() + p.draws() + p.losses() == 0));
        var end = seen.getLast();
        assertEquals(1, end.checkmates()); assertEquals(1, end.failedGames());
        assertEquals(1, end.completedGames()); assertEquals(7, end.totalCompletedGamePlies());
        assertEquals(result.statistics().terminations(), end.terminations());
    }

    private static ValidationResult scripted(List<String> trace, java.util.function.Consumer<ValidationProgress> observer, TimeSource clock) {
        return scripted(trace, observer, clock, false);
    }
    private static ValidationResult scripted(List<String> trace, java.util.function.Consumer<ValidationProgress> observer, TimeSource clock, boolean failSecond) {
        long[] root = Board.startingPosition(); AtomicInteger players = new AtomicInteger();
        return new ValidationArena().validate(CANDIDATE, INCUMBENT, config(1, 0, 0, 16), root,
                GameHistory.initial(root), new ValidationControl(), (network, cfg) -> {
                    String[] script = players.getAndIncrement() < 2 ? WHITE_WIN : failSecond ? new String[] {"invalid"} : BLACK_WIN;
                    return request -> {
                        long[] board = new long[6]; request.copyBoardInto(board);
                        var game = new HeadlessGame(board, request.gameHistory(), 16);
                        long move = Arrays.stream(game.legalMoves()).filter(m -> Move.coordinate(m).equals(script[request.gameHistory().size() - 1])).findFirst().orElseThrow();
                        trace.add(ValidationArena.stateHash(board, request.gameHistory()) + ":" + move);
                        return move;
                    };
                }, observer, clock);
    }
}
