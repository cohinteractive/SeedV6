package com.ohinteractive.seedv6.gui;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.service.TrainerSnapshot;
import com.ohinteractive.seedv6.training.validation.*;
import static com.ohinteractive.seedv6.gui.TrainingDashboardModel.*;
import static org.junit.jupiter.api.Assertions.*;

class TrainingDashboardTest {
    static final String CANDIDATE = "g000187-s000005984-" + "a".repeat(64), BEST = "g000181-s000005700-" + "b".repeat(64);
    static TrainingSettings settings(Path root) { return new TrainingSettings(root, 4, 6, 32, 0, 8, 32, 32, 1, 32, 71, 1024, 0); }

    static TrainerSnapshot snapshot(TrainerSnapshot.State state, boolean finalMatch, boolean promoted, boolean nextGeneration) {
        var config = settings(Path.of("build/unused-dashboard-store")).config(com.ohinteractive.seedv6.training.service.TrainerConfig.DepthChange.REQUIRE_SAME).validation(187);
        var policy = new PromotionPolicy(32, .05, 0);
        var colour = new ValidationResult.ColourRecord(finalMatch ? 24 : 10, finalMatch ? 6 : 5, finalMatch ? 2 : 1);
        var reasons = Map.of(GameTermination.WHITE_CHECKMATES_BLACK, finalMatch ? 26 : 11,
                GameTermination.BLACK_CHECKMATES_WHITE, finalMatch ? 26 : 11, GameTermination.FIFTY_MOVE_RULE, finalMatch ? 12 : 10);
        var results = new ValidationResult.Statistics(finalMatch ? 32 : 16, 0, colour, colour, 1200, reasons);
        long now = System.nanoTime();
        var progress = new ValidationProgress(32, 17, 1, 33, 83, ValidationProgress.CandidateColour.WHITE,
                !finalMatch && !nextGeneration, finalMatch, finalMatch ? 32 : 16, 0, colour, colour, finalMatch ? 64 : 32, 1200, 96,
                reasons, now - 214_000_000_000L, now - 42_000_000_000L, now,
                now, Optional.of(new ValidationProgress.MoveSearch(4, 350000, 839, 417000)));
        return new TrainerSnapshot(state, state == TrainerSnapshot.State.FAILED ? "fixture failure" : "", Duration.ofSeconds(1961), nextGeneration ? 188 : 187,
                promoted ? CANDIDATE : BEST, CANDIDATE, nextGeneration ? "" : CANDIDATE, 5984, 4,
                new SelfPlayBatch.Statistics(32, 24, 8, 10, 8, 6, 8, 1200, 10, 80, 42, 1000, 768),
                Optional.empty(), 24, 768, .0412, finalMatch ? Optional.of(results) : Optional.empty(),
                finalMatch ? Optional.of(policy.assess(32, results.pairScoreSum())) : Optional.empty(),
                new TrainerSnapshot.Totals(finalMatch ? 1 : 0, 32, 24, 8, 8, 768, 24, promoted ? 1 : 0, 0, 0, 0),
                Optional.of(nextGeneration ? progress.withoutCurrentGame() : progress),
                Optional.of(new TrainerSnapshot.ValidationDetails(CANDIDATE, BEST, config, policy, OptionalLong.of(187), OptionalLong.of(181))));
    }

    static TrainingController.ViewState view(TrainerSnapshot snapshot) {
        boolean active = snapshot.running();
        return new TrainingController.ViewState(settings(Path.of("build/unused-dashboard-store")), snapshot.failed() ? TrainingController.Phase.FAILED
                : active ? TrainingController.Phase.RUNNING : TrainingController.Phase.STOPPED, snapshot, "Fixture: immutable service publication", active, !active, true, 4, "");
    }

    @Test void liveValidationUsesValidPairsAndNeverPredictsPromotion() {
        var s = snapshot(TrainerSnapshot.State.VALIDATING, false, false, false); var m = match(s);
        assertEquals("187", m.candidate()); assertEquals("181", m.incumbent());
        assertEquals(20, m.wins()); assertEquals(10, m.draws()); assertEquals(2, m.losses());
        assertEquals("78.1%", m.score()); assertEquals("Validation in progress", m.decision());
        assertTrue(m.current()); assertFalse(m.finished()); assertEquals(50, validation(s, view(s).settings()).percent());
    }

    @Test void selfPlayShowsProcessedVersusChessCompleteAndTrainingHasNoInventedPercentage() {
        var s = snapshot(TrainerSnapshot.State.TRAINING, false, false, false);
        var p = selfPlay(s, view(s).settings());
        assertEquals(100, p.percent()); assertEquals("32 / 32 processed", p.value());
        assertEquals("24 complete · 8 aborted (8 capped)", p.detail());
        assertEquals(-1, training(s).percent()); assertTrue(training(s).detail().contains("768 sample visits"));
    }

    @Test void finalPromotionRequiresPublishedBestAndKeepsTheOriginalIncumbent() {
        var pending = match(snapshot(TrainerSnapshot.State.RECORDING_DECISION, true, false, false));
        assertEquals("Promotion publication pending", pending.decision());
        var published = match(snapshot(TrainerSnapshot.State.STOPPED, true, true, false));
        assertEquals("PROMOTED", published.decision()); assertEquals("181", published.incumbent());
        assertEquals(BEST, published.incumbentId()); assertEquals("00:03:34", published.duration());
    }

    @Test void retainedMatchDoesNotBecomeCurrentGenerationProgress() {
        var s = snapshot(TrainerSnapshot.State.GENERATING_SELF_PLAY, true, true, true); var m = match(s);
        assertFalse(m.current()); assertEquals("187", m.candidate()); assertEquals("181", m.incumbent());
        assertEquals(0, validation(s, view(s).settings()).percent()); assertEquals("Waiting for this Candidate", validation(s, view(s).settings()).detail());
        assertEquals("SELF-PLAY", phase(view(s)));
    }

    @Test void currentFailureBlocksPromotionButLaterFailurePreservesPriorResult() {
        assertEquals("PROMOTION BLOCKED", match(snapshot(TrainerSnapshot.State.FAILED, true, false, false)).decision());
        assertEquals("PROMOTED", match(snapshot(TrainerSnapshot.State.FAILED, true, true, true)).decision());
    }

    @Test void noDataHasNoFabricatedNetworkLossScoreOrHistory() {
        assertNull(match(null)); assertEquals("Not published", network("")); assertEquals("—", number(Double.NaN));
        assertEquals("—", score(Double.NaN)); assertEquals(-1, training(null).percent());
        assertEquals("Gen 181", network(BEST)); assertFalse(network("g123-not-a-checkpoint").equals("Gen 123"));
    }

    @Test void retainedAndInconclusiveOutcomesUseTheAssessmentNotFiftyPercentHeuristics() {
        var draw = new ValidationResult.ColourRecord(0, 32, 0);
        var empty = new ValidationResult.ColourRecord(0, 0, 0);
        var retained = new ValidationResult.Statistics(32, 0, draw, draw, 1000, Map.of(GameTermination.FIFTY_MOVE_RULE, 64));
        var inconclusive = new ValidationResult.Statistics(0, 32, empty, empty, 2000, Map.of(GameTermination.PLY_CAP, 64));
        var s = snapshot(TrainerSnapshot.State.STOPPED, true, false, false);
        for (var v : List.of(retained, inconclusive)) {
            var result = withResult(s, v); var m = match(result);
            assertEquals(v.validPairs() == 0 ? "INCONCLUSIVE · KEEP BEST" : "KEEP BEST", m.decision());
            assertEquals(v.validPairs() == 0 ? "—" : "50.0%", m.score());
            assertEquals("—", m.duration(), "Recovered evidence must not invent timing");
            assertEquals(100, validation(result, view(s).settings()).percent());
        }
    }

    static TrainerSnapshot withResult(TrainerSnapshot s, ValidationResult.Statistics v) {
        return new TrainerSnapshot(s.state(), s.failureSummary(), s.elapsed(), s.generation(), s.bestId(), s.latestTrainingId(), s.candidateId(),
                s.optimizerStep(), s.trainingDepth(), s.selfPlay(), s.training(), s.generationOptimizerUpdates(), s.generationSamplesTrained(),
                s.meanTrainingLoss(), Optional.of(v), Optional.of(s.validationDetails().orElseThrow().policy().assess(v.validPairs(), v.pairScoreSum())),
                s.totals(), Optional.empty(), s.validationDetails());
    }
}
