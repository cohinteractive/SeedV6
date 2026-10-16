package com.ohinteractive.seedv6.training.validation;

import java.util.EnumMap;
import java.util.Optional;
import java.util.function.Consumer;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.training.selfplay.GameTermination;

/** Arena-thread-owned accumulator. Consumers must return promptly; a failing consumer is detached. */
final class ValidationProgressTracker {
    private final int configuredPairs;
    private final TimeSource clock;
    private Consumer<ValidationProgress> observer;
    private final long phaseStart;
    private long gameStart, gameEnd, totalPlies;
    private int pair, game, plies, valid, incomplete, completed, longest;
    private boolean active, complete;
    private ValidationProgress.MoveSearch lastMoveSearch;
    private ValidationResult.ColourRecord white = new ValidationResult.ColourRecord(0, 0, 0), black = white;
    private final EnumMap<GameTermination, Integer> reasons = new EnumMap<>(GameTermination.class);

    ValidationProgressTracker(int configuredPairs, Consumer<ValidationProgress> observer, TimeSource clock) {
        this.configuredPairs = configuredPairs; this.observer = observer; this.clock = clock;
        phaseStart = clock.nanoTime();
        publish();
    }

    void startPair(int number) {
        lastMoveSearch = null;
        pair = number; game = 0; plies = 0; gameStart = 0; gameEnd = 0; active = false; publish();
    }
    void startGame(int number) {
        lastMoveSearch = null;
        game = number; plies = 0; active = true; gameStart = clock.nanoTime(); gameEnd = gameStart; publish();
    }
    void moved(int playedPlies) { moved(playedPlies, null); }
    void moved(int playedPlies, ValidationProgress.MoveSearch search) {
        plies = playedPlies; lastMoveSearch = search; publish();
    }
    void endGame(ValidationResult.Game result) {
        active = false; plies = result.plies(); gameEnd = clock.nanoTime(); count(result); publish();
    }
    void endPair(ValidationResult.Pair result, boolean unplayed) {
        if (unplayed) { count(result.candidateWhite()); count(result.candidateBlack()); }
        if (result.valid()) {
            valid++;
            white = add(white, result.candidateWhite().score(Value.WHITE));
            black = add(black, result.candidateBlack().score(Value.BLACK));
        } else incomplete++;
        publish();
    }
    void finish() { active = false; complete = true; publish(); }

    private void count(ValidationResult.Game result) {
        reasons.merge(result.termination(), 1, Integer::sum);
        if (result.termination().completed() || result.termination() == GameTermination.PLY_CAP) {
            completed++; totalPlies += result.plies(); longest = Math.max(longest, result.plies());
        }
    }
    private static ValidationResult.ColourRecord add(ValidationResult.ColourRecord before, double score) {
        return new ValidationResult.ColourRecord(before.wins() + (score == 1 ? 1 : 0),
                before.draws() + (score == 0.5 ? 1 : 0), before.losses() + (score == 0 ? 1 : 0));
    }
    private void publish() {
        if (observer == null) return;
        var colour = game == 0 ? ValidationProgress.CandidateColour.NONE : game == 1
                ? ValidationProgress.CandidateColour.WHITE : ValidationProgress.CandidateColour.BLACK;
        var progress = new ValidationProgress(configuredPairs, pair, game, game == 0 ? 0 : (pair - 1) * 2 + game,
                plies, colour, active, complete, valid, incomplete, white, black, completed, totalPlies, longest,
                reasons, phaseStart, gameStart, gameEnd, clock.nanoTime(), Optional.ofNullable(lastMoveSearch));
        try { observer.accept(progress); }
        catch (RuntimeException | AssertionError ignored) {
            // Ordinary observer failures (including assertions) cannot become SEARCH_FAILURE/evidence.
            // Fatal JVM errors are deliberately not swallowed. No retry, logging or worker-side formatting.
            observer = null;
        }
    }
}
