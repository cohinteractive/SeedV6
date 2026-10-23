package com.ohinteractive.seedv6.gui;

import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.search.common.SearchResult;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.gui.ConventionalGuiProbe.edt;

class BaselineScoreTest {
    @Test void scoresScaleExactlyOnceWithExplicitUnitsAndKeepMateSeparate() throws Exception {
        for (var mode : new PlayEvaluator.Mode[]{PlayEvaluator.Mode.CONVENTIONAL_CP64, PlayEvaluator.Mode.BREP2_32, PlayEvaluator.Mode.HANDCRAFTED}) {
        var binding = mode == PlayEvaluator.Mode.HANDCRAFTED ? PlayEvaluator.handcrafted() : PlayEvaluator.bundled(mode);
        for (int side : new int[]{Value.WHITE, Value.BLACK}) {
            var info = new GameController.SearchInfo("Idle", 5, "cp 550", 10, 10, "", "DEPTH", 1, side);
            var displayed = edt(() -> new EngineCard().showSearch(info, binding, 4));
            assertEquals(side == Value.WHITE ? "+5.50" : "-5.50", displayed.text());
            var score = PlayedMove.Score.from(new SearchResult(0, false, 550, 5, 10, 0, true), side, 0, binding);
            assertEquals(displayed.text(), score.text());
            var participant = new com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot.Participant(
                    mode == PlayEvaluator.Mode.HANDCRAFTED ? com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot.Role.HCE
                            : com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot.Role.MODEL,
                    binding.checkpointId(), null, binding.description(), binding.baseline().evaluator());
            var arena = snapshot(participant, side == Value.WHITE ? 550 : -550, side);
            assertEquals(displayed, MatchPresentation.score(arena, NetworkArchitecture.NNUE));
            assertTrue(MatchPresentation.evaluationCaption(arena, NetworkArchitecture.NNUE).contains(binding.scoreUnits()));
            if (mode == PlayEvaluator.Mode.CONVENTIONAL_CP64)
                assertTrue(MatchPresentation.evaluationCaption(arena, NetworkArchitecture.NNUE).contains("teacher expected-score"));
            assertEquals("+M1", MatchPresentation.score(snapshot(participant, 32767, side), NetworkArchitecture.NNUE).text());
            assertTrue(score.description().contains(binding.scoreUnits()));
            if (mode == PlayEvaluator.Mode.CONVENTIONAL_CP64) {
                assertFalse(score.description().contains("pawns"));
                assertTrue(binding.scoreExplanation().contains("teacher expected-score"));
            } else assertTrue(score.description().contains("pawns"));
        }
        }
        assertEquals("+M1", PlayScore.from(new GameController.SearchInfo("Idle", 1, "mate 1", 1, 1, "", "DEPTH"), false).text());
        assertFalse(PlayScore.from(GameController.SearchInfo.idle(), false).available());
    }
    @Test void retainedPinnedTelemetryKeepsItsHistoricalUnits() {
        var old = new com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot.Participant(
                com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot.Role.MODEL, "g32", null, "construction-v1 Pair-2 historical");
        var game = snapshot(old, 550, Value.WHITE);
        assertEquals("+550", MatchPresentation.score(game, NetworkArchitecture.NNUE).text());
        assertTrue(MatchPresentation.evaluationCaption(game, NetworkArchitecture.NNUE).contains(NetworkArchitecture.BRN_PAIR2.evaluationUnits()));
    }
    private static com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot snapshot(
            com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot.Participant participant, int whiteScore, int side) {
        return new com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot(
                com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot.Phase.ARENA, 0, 1, 1, 1, 1, 0,
                participant, participant, com.ohinteractive.seedv6.core.Board.startingPosition(), 0,
                new com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot.MoveEvaluation(whiteScore, side, 5, 0), 0, 0);
    }
}
