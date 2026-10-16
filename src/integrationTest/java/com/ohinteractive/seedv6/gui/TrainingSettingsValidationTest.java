package com.ohinteractive.seedv6.gui;

import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.validation.*;
import static com.ohinteractive.seedv6.training.selfplay.GameTermination.*;
import static com.ohinteractive.seedv6.training.validation.PromotionPolicy.Decision.*;
import static org.junit.jupiter.api.Assertions.*;

class TrainingSettingsValidationTest {
    @ParameterizedTest @ValueSource(ints = {32, 64, 96})
    void configuredSampleCanFinishWithoutAnUnreachableRequirement(int pairs) {
        var config = settings(pairs).config(TrainerConfig.DepthChange.REQUIRE_SAME);
        var policy = config.validation().policy();
        assertEquals(pairs, config.validation().openingPairs());
        assertEquals(pairs, config.validation(1).openingPairs());
        assertEquals(pairs, policy.minimumPairs());
        assertEquals(PromotionPolicy.DEFAULT.alpha(), policy.alpha());
        assertEquals(PromotionPolicy.DEFAULT.requiredMargin(), policy.requiredMargin());
        if (pairs == 64) assertEquals(PromotionPolicy.DEFAULT, policy);
        assertEquals(PROMOTE, policy.assess(pairs, pairs).decision());
        assertEquals(0.5, policy.assess(pairs, pairs).threshold());

        // A terminal chess position exercises the real arena's full pair accounting without searches.
        long[] board = Board.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1");
        var network = NnueNetwork.initialized(73);
        List<ValidationProgress> progress = new ArrayList<>();
        var result = new ValidationArena().validate(network, network, config.validation(1), board,
                GameHistory.initial(board), new ValidationControl(), progress::add);
        assertEquals(pairs, result.pairs().size());
        assertEquals(pairs, result.statistics().validPairs());
        assertEquals(0, result.statistics().incompletePairs());
        var end = progress.getLast();
        assertTrue(end.complete());
        assertEquals(pairs, end.configuredPairs());
        assertEquals(pairs, end.validPairs());
        assertEquals(2 * pairs, end.maximumGames());
        assertEquals(2 * pairs, end.completedGames());
        assertEquals(RETAIN_INCUMBENT, result.assess(policy).decision());
        String text = presentation(config, result);
        assertTrue(text.startsWith("Validation finished - KEEP BEST | Score 50.0%"), text);
        assertTrue(text.contains("Games: " + (2 * pairs) + " / " + (2 * pairs) + " | Valid pairs: " + pairs), text);
        assertFalse(text.contains("required"), text);
    }

    @ParameterizedTest @ValueSource(ints = {32, 64, 96})
    void incompletePairsDoNotMeetTheConfiguredRequirementOrContributeScores(int pairs) {
        var config = settings(pairs).config(TrainerConfig.DepthChange.REQUIRE_SAME);
        var whiteWin = new ValidationResult.Game(WHITE_CHECKMATES_BLACK, 7);
        var blackWin = new ValidationResult.Game(BLACK_CHECKMATES_WHITE, 4);
        var valid = new ValidationResult.Pair("", whiteWin, blackWin);
        for (var reason : List.of(PLY_CAP, CANCELLED, SEARCH_FAILURE, INFRASTRUCTURE_FAILURE)) {
            var sample = new ArrayList<>(Collections.nCopies(pairs - 1, valid));
            sample.add(new ValidationResult.Pair("", whiteWin, new ValidationResult.Game(reason, 0)));
            var result = new ValidationResult(config.validation(1), "a".repeat(64), sample);
            assertEquals(pairs - 1, result.statistics().validPairs());
            assertEquals(1, result.statistics().incompletePairs());
            assertEquals(2 * (pairs - 1), result.statistics().wins(), "The incomplete pair's win is excluded");
            assertEquals(INCONCLUSIVE, result.assess(config.validation().policy()).decision());
            String text = presentation(config, result);
            if (reason == SEARCH_FAILURE || reason == INFRASTRUCTURE_FAILURE) {
                assertTrue(text.startsWith("Validation failed - PROMOTION BLOCKED"), text);
            } else {
                assertTrue(text.startsWith("Validation finished - INCONCLUSIVE - KEEP BEST | Valid pairs: "
                        + (pairs - 1) + " < " + pairs + " required | Score 100.0%"), text);
            }
            assertFalse(text.contains("PROMOTE CANDIDATE"), text);
        }
    }

    private static TrainingSettings settings(int pairs) {
        var d = TrainingSettings.defaults();
        return new TrainingSettings(d.root(), d.depth(), d.threads(), d.games(), d.openingMin(), d.openingMax(),
                d.samples(), d.minibatch(), d.epochs(), pairs, d.seed(), d.maximumPlies(), d.maximumGenerations());
    }

    private static String presentation(TrainerConfig config, ValidationResult result) {
        var base = new TrainingControllerTest.FakeHandle().snapshot();
        var policy = config.validation().policy();
        var details = new TrainerSnapshot.ValidationDetails("candidate", "best", result.config(), policy);
        var snapshot = new TrainerSnapshot(TrainerSnapshot.State.STOPPED, "", base.elapsed(), 1,
                "best", "candidate", "candidate", 1, config.selfPlay().depth(), base.selfPlay(), Optional.empty(),
                0, 0, 0, Optional.of(result.statistics()), Optional.of(result.assess(policy)), base.totals(),
                Optional.empty(), Optional.of(details));
        var view = new TrainingController.ViewState(settings(result.config().openingPairs()), TrainingController.Phase.STOPPED,
                snapshot, "", false, true, false, 1, "best");
        return TrainingProgress.validation(view, 0);
    }
}
