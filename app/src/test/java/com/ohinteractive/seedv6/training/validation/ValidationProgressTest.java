package com.ohinteractive.seedv6.training.validation;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.ohinteractive.seedv6.training.selfplay.GameTermination;
import static com.ohinteractive.seedv6.training.selfplay.GameTermination.*;
import static org.junit.jupiter.api.Assertions.*;

class ValidationProgressTest {
    @Test void lifecycleIndexesMovesPairEvidenceAndFrozenTimers() {
        List<ValidationProgress> seen = new ArrayList<>();
        AtomicLong clock = new AtomicLong();
        var tracker = new ValidationProgressTracker(2, seen::add, clock::get);
        var initial = seen.getLast();
        assertEquals(ValidationProgress.initial(2, 0), initial);
        assertEquals(4, initial.maximumGames());
        assertEquals(Duration.ofSeconds(2), initial.validationElapsed(2_000_000_000L));
        tracker.startPair(1);
        assertEquals(1, seen.getLast().currentPair());
        assertEquals(0, seen.getLast().gameInPair());
        clock.set(1_000_000_000L); tracker.startGame(1);
        var first = seen.getLast();
        assertTrue(first.gameActive()); assertEquals(1, first.gameOrdinal());
        assertEquals(ValidationProgress.CandidateColour.WHITE, first.candidateColour());
        assertEquals(Duration.ofSeconds(3), first.gameElapsed(4_000_000_000L));
        assertEquals(Duration.ofSeconds(4), first.validationElapsed(4_000_000_000L));
        clock.set(4_000_000_000L); tracker.moved(1);
        var moved = seen.getLast();
        assertEquals(1, moved.currentGamePlies()); assertEquals(0, first.currentGamePlies());
        assertEquals(clock.get(), moved.lastProgressNanos());
        assertEquals(Duration.ofSeconds(2), moved.lastProgressAge(6_000_000_000L));
        var a = game(WHITE_CHECKMATES_BLACK, 7);
        tracker.moved(7); tracker.endGame(a);
        assertFalse(seen.getLast().gameActive()); assertEquals(0, seen.getLast().wins());
        assertEquals(1, seen.getLast().checkmates());
        assertEquals(Duration.ofSeconds(3), seen.getLast().gameElapsed(99_000_000_000L));
        clock.set(6_000_000_000L); tracker.startGame(2);
        assertEquals(Duration.ZERO, seen.getLast().gameElapsed(clock.get()));
        assertEquals(2, seen.getLast().gameOrdinal());
        assertEquals(ValidationProgress.CandidateColour.BLACK, seen.getLast().candidateColour());
        var b = game(BLACK_CHECKMATES_WHITE, 4);
        tracker.endGame(b);
        assertEquals(0, seen.getLast().wins(), "Game B alone still cannot publish provisional evidence");
        tracker.endPair(new ValidationResult.Pair("", a, b), false);
        assertEquals(2, seen.getLast().wins()); assertEquals(1, seen.getLast().validPairs());
        tracker.startPair(2);
        assertEquals(2, seen.getLast().currentPair()); assertEquals(0, seen.getLast().currentGamePlies());
        tracker.startGame(1); assertEquals(3, seen.getLast().gameOrdinal());
        var c = game(STALEMATE, 10); tracker.endGame(c);
        tracker.startGame(2); assertEquals(4, seen.getLast().gameOrdinal());
        var d = game(PLY_CAP, 12); tracker.endGame(d);
        tracker.endPair(new ValidationResult.Pair("", c, d), false);
        clock.set(10_000_000_000L); tracker.finish();
        var end = seen.getLast();
        assertTrue(end.complete()); assertFalse(end.gameActive());
        assertEquals(1, end.validPairs()); assertEquals(1, end.incompletePairs());
        assertEquals(0, end.draws(), "Incomplete pair's completed draw is raw evidence only");
        assertEquals(4, end.completedGames()); assertEquals(33, end.totalCompletedGamePlies());
        assertEquals(8.25, end.averageCompletedGamePlies()); assertEquals(12, end.longestCompletedGamePlies());
        assertEquals(Duration.ofSeconds(10), end.validationElapsed(999_000_000_000L));
        assertThrows(UnsupportedOperationException.class, () -> end.terminations().clear());
        assertTrue(initial.terminations().isEmpty());
    }

    @Test void candidatePerspectiveIncludesBothWinsLossesAndDrawsOnlyAfterValidPair() {
        List<ValidationProgress> seen = new ArrayList<>();
        var tracker = new ValidationProgressTracker(4, seen::add, () -> 0);
        pair(tracker, 1, WHITE_CHECKMATES_BLACK, BLACK_CHECKMATES_WHITE);
        pair(tracker, 2, BLACK_CHECKMATES_WHITE, WHITE_CHECKMATES_BLACK);
        pair(tracker, 3, STALEMATE, FIFTY_MOVE_RULE);
        pair(tracker, 4, WHITE_CHECKMATES_BLACK, CANCELLED);
        tracker.finish();
        var end = seen.getLast();
        assertEquals(new ValidationResult.ColourRecord(1, 1, 1), end.white());
        assertEquals(end.white(), end.black());
        assertEquals(2, end.wins()); assertEquals(2, end.draws()); assertEquals(2, end.losses());
        assertEquals(5, end.checkmates()); assertEquals(1, end.incompletePairs());
    }

    @ParameterizedTest @EnumSource(value = GameTermination.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void everyAuthoritativeTerminationIsDistinctAndLengthsExcludeFailures(GameTermination reason) {
        List<ValidationProgress> seen = new ArrayList<>();
        var tracker = new ValidationProgressTracker(1, seen::add, () -> 0);
        tracker.startPair(1); tracker.startGame(1); tracker.endGame(game(reason, 123));
        var p = seen.getLast();
        assertEquals(Map.of(reason, 1), p.terminations());
        assertEquals(0, p.wins() + p.draws() + p.losses());
        boolean include = reason.completed() || reason == PLY_CAP;
        assertEquals(include ? 1 : 0, p.completedGames());
        assertEquals(include ? 123 : 0, p.totalCompletedGamePlies());
        assertEquals(include ? 123 : 0, p.averageCompletedGamePlies());
        assertEquals(include ? 123 : 0, p.longestCompletedGamePlies());
        assertEquals(reason == WHITE_CHECKMATES_BLACK || reason == BLACK_CHECKMATES_WHITE ? 1 : 0, p.checkmates());
        assertEquals(reason == SEARCH_FAILURE || reason == INFRASTRUCTURE_FAILURE ? 1 : 0, p.failedGames());
    }

    @Test void unplayedCancelledPairsCountSlotsButNeverFinishedGamesOrLengths() {
        List<ValidationProgress> seen = new ArrayList<>();
        var tracker = new ValidationProgressTracker(1, seen::add, () -> -100);
        tracker.startPair(1);
        tracker.endPair(new ValidationResult.Pair("", game(CANCELLED, 0), game(CANCELLED, 0)), true);
        tracker.finish();
        var p = seen.getLast();
        assertEquals(2, p.terminationCount(CANCELLED)); assertEquals(1, p.incompletePairs());
        assertEquals(0, p.completedGames()); assertEquals(0, p.gameOrdinal());
        assertEquals(ValidationProgress.CandidateColour.NONE, p.candidateColour());
    }

    @Test void timingAcceptsNegativeNanoTimeOriginAndWrapWithoutWallClockDependence() {
        assertEquals(Duration.ofNanos(30), ValidationProgress.initial(1, -20).validationElapsed(10));
        assertEquals(Duration.ofNanos(10), ValidationProgress.initial(1, Long.MAX_VALUE - 5).validationElapsed(Long.MIN_VALUE + 4));
        assertEquals(Duration.ZERO, ValidationProgress.initial(1, 20).validationElapsed(10));
    }

    private static ValidationResult.Game game(GameTermination termination, int plies) { return new ValidationResult.Game(termination, plies); }
    private static void pair(ValidationProgressTracker tracker, int number, GameTermination a, GameTermination b) {
        tracker.startPair(number); tracker.startGame(1); tracker.endGame(game(a, 4));
        tracker.startGame(2); tracker.endGame(game(b, 4));
        tracker.endPair(new ValidationResult.Pair("", game(a, 4), game(b, 4)), false);
    }
}
