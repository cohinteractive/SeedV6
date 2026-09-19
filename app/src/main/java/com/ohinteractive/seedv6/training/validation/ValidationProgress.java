package com.ohinteractive.seedv6.training.validation;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import com.ohinteractive.seedv6.search.common.IterationSnapshot;
import com.ohinteractive.seedv6.training.selfplay.GameTermination;

/**
 * Constant-size, immutable runtime telemetry; never promotion authority or durable evidence.
 * W/D/L includes only valid pairs. Raw terminations include every recorded game slot (including
 * unplayed cancellation slots). Lengths include chess results and explicit ply caps only, excluding
 * cancelled/failed games. Plies count moves after the shared opening, as in ValidationResult.
 * Timestamps use the process-local monotonic TimeSource, not wall time; zero is a valid timestamp.
 */
public record ValidationProgress(int configuredPairs, int currentPair, int gameInPair, int gameOrdinal,
                                 int currentGamePlies, CandidateColour candidateColour,
                                 boolean gameActive, boolean complete, int validPairs, int incompletePairs,
                                 ValidationResult.ColourRecord white, ValidationResult.ColourRecord black,
                                 int completedGames, long totalCompletedGamePlies, int longestCompletedGamePlies,
                                 Map<GameTermination, Integer> terminations,
                                 long phaseStartedNanos, long gameStartedNanos, long gameEndedNanos,
                                 long lastProgressNanos, Optional<MoveSearch> lastMoveSearch) {
    public enum CandidateColour { NONE, WHITE, BLACK }
    /** Primitive summary of the last played move's existing cumulative search result. */
    public record MoveSearch(int depth, long nodes, long elapsedMillis, long nps) {
        static MoveSearch from(IterationSnapshot result) {
            return new MoveSearch(result.depth(), result.nodes(), result.elapsedMillis(), result.nps());
        }
    }

    public ValidationProgress(int configuredPairs, int currentPair, int gameInPair, int gameOrdinal,
                              int currentGamePlies, CandidateColour candidateColour,
                              boolean gameActive, boolean complete, int validPairs, int incompletePairs,
                              ValidationResult.ColourRecord white, ValidationResult.ColourRecord black,
                              int completedGames, long totalCompletedGamePlies, int longestCompletedGamePlies,
                              Map<GameTermination, Integer> terminations,
                              long phaseStartedNanos, long gameStartedNanos, long gameEndedNanos, long lastProgressNanos) {
        this(configuredPairs, currentPair, gameInPair, gameOrdinal, currentGamePlies, candidateColour,
                gameActive, complete, validPairs, incompletePairs, white, black, completedGames,
                totalCompletedGamePlies, longestCompletedGamePlies, terminations,
                phaseStartedNanos, gameStartedNanos, gameEndedNanos, lastProgressNanos, Optional.empty());
    }

    public ValidationProgress { terminations = Map.copyOf(terminations); }

    public static ValidationProgress initial(int configuredPairs, long now) {
        var empty = new ValidationResult.ColourRecord(0, 0, 0);
        return new ValidationProgress(configuredPairs, 0, 0, 0, 0, CandidateColour.NONE,
                false, false, 0, 0, empty, empty, 0, 0, 0, Map.of(), now, 0, 0, now);
    }

    public int maximumGames() { return configuredPairs * 2; }
    /** Keep the last generation's summary visible during self-play, without stale live game fields. */
    public ValidationProgress withoutCurrentGame() {
        return new ValidationProgress(configuredPairs, 0, 0, 0, 0, CandidateColour.NONE, false, complete,
                validPairs, incompletePairs, white, black, completedGames, totalCompletedGamePlies,
                longestCompletedGamePlies, terminations, phaseStartedNanos, 0, 0, lastProgressNanos);
    }
    public int wins() { return white.wins() + black.wins(); }
    public int draws() { return white.draws() + black.draws(); }
    public int losses() { return white.losses() + black.losses(); }
    public int terminationCount(GameTermination reason) { return terminations.getOrDefault(reason, 0); }
    public int checkmates() {
        return terminationCount(GameTermination.WHITE_CHECKMATES_BLACK) + terminationCount(GameTermination.BLACK_CHECKMATES_WHITE);
    }
    public int failedGames() {
        return terminationCount(GameTermination.SEARCH_FAILURE) + terminationCount(GameTermination.INFRASTRUCTURE_FAILURE);
    }
    public double averageCompletedGamePlies() {
        return completedGames == 0 ? 0 : (double) totalCompletedGamePlies / completedGames;
    }
    public Duration validationElapsed(long now) { return elapsed(complete ? lastProgressNanos : now, phaseStartedNanos); }
    public Duration gameElapsed(long now) {
        return gameInPair == 0 ? Duration.ZERO : elapsed(gameActive ? now : gameEndedNanos, gameStartedNanos);
    }
    public Duration lastProgressAge(long now) { return elapsed(now, lastProgressNanos); }
    private static Duration elapsed(long end, long start) { return Duration.ofNanos(Math.max(0, end - start)); }
}
