package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.validation.*;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.edt;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static com.ohinteractive.seedv6.training.selfplay.GameTermination.*;
import static com.ohinteractive.seedv6.training.service.LearningArenaState.*;
import static org.junit.jupiter.api.Assertions.*;

class ArenaSemanticsTest {
    @TempDir Path temporary;
    private LearningArenaState fixture() throws Exception {
        var f = new ArenaHistoryViewTest(); f.temporary = temporary; return f.fixture();
    }
    @Test void liveAndGameRowsUseStableCumulativePairedChessScoring() throws Exception {
        var state = fixture(); var base = state.history().getFirst();
        var win = new ValidationResult.Game(WHITE_CHECKMATES_BLACK, 11, new MaterialCount(9, 0));
        var draw = new ValidationResult.Game(STALEMATE, 12);
        var loss = new ValidationResult.Game(BLACK_CHECKMATES_WHITE, 13);
        var waiting = new ValidationResult.Game(CANCELLED, 0);
        var one = new Round(0, "", 0, base.a(), base.b(), List.of(new ValidationResult.Pair("one", win, waiting)), false);
        var pending = ArenaMatchSummary.from(state.config(), one);
        assertEquals(1, pending.games()); assertEquals(0, pending.pairs()); assertEquals(0, pending.wins());
        assertEquals("0-0-0", pending.history().getFirst().cumulativeWdl());
        var round = new Round(0, "", 0, base.a(), base.b(), List.of(new ValidationResult.Pair("one", win, draw), new ValidationResult.Pair("two", loss, draw)), true);
        var match = ArenaMatchSummary.from(state.config(), round);
        assertEquals(4, match.games()); assertEquals(2, match.pairs());
        assertEquals(1, match.wins()); assertEquals(2, match.draws()); assertEquals(1, match.losses());
        assertEquals("50.0%", match.score(true)); assertEquals("50.0%", match.score(false));
        assertEquals("1-1-0", match.history().get(1).cumulativeWdl());
        assertEquals("75.0% / 25.0%", match.history().get(1).cumulativeScore());
        assertEquals("1-1-0", match.history().get(2).cumulativeWdl());
        assertEquals("1-2-1", match.history().get(3).cumulativeWdl());
        assertEquals("9 / 0", match.history().getFirst().material());
        assertEquals("\u2014", match.history().get(1).material());
        assertEquals("NNUE", match.history().getFirst().winner());
        assertFalse(match.history().get(1).firstIsWhite());
        var capped = new Round(0, "", 0, base.a(), base.b(), List.of(new ValidationResult.Pair("cap", win, new ValidationResult.Game(PLY_CAP, 50))), false);
        var unscored = ArenaMatchSummary.from(state.config(), capped);
        assertEquals(2, unscored.games()); assertEquals(1, unscored.unscoredPairs()); assertEquals(0, unscored.wins());
        assertEquals("\u2014", unscored.score(true));
    }
    @Test void historyFiltersPendingRoundsOrdersWinnerAndKeepsBothTrainedModels() throws Exception {
        var state = fixture(); var rounds = new ArrayList<>(state.history()); var old = rounds.get(4);
        var a = new Endpoint(old.a().checkpoint(), 4, 400, 3200, new TrainingMetrics(800, 2_000_000_000L, .001, 32, 8, .12));
        var b = new Endpoint(old.b().checkpoint(), 4, 400, 3200, new TrainingMetrics(800, 4_000_000_000L, .01, 128, 8, .34));
        rounds.set(4, new Round(4, old.trancheHash(), old.sourceEnd(), a, b, old.pairs(), true));
        var value = new LearningArenaState(1, state.id(), state.binding(), state.config(), rounds, state.status(), state.message());
        edt(() -> {
            var view = new ArenaHistoryView(); view.showState(value, temporary);
            var table = named(view, "arenaHistory", JTable.class); assertEquals(6, table.getRowCount());
            assertEquals("BRE-Pair 2", table.getValueAt(4, 1)); assertEquals("4-0-0", table.getValueAt(4, 2));
            assertEquals("100.0% / 0.0%", table.getValueAt(4, 3));
            assertEquals("Tie (A / B)", table.getValueAt(2, 1)); assertEquals("2-0-2", table.getValueAt(2, 2));
            assertEquals("50.0% / 50.0%", table.getValueAt(2, 3));
            assertEquals("400.0 / 200.0", table.getValueAt(4, 4));
            assertEquals("6s", table.getValueAt(4, 5)); assertEquals("32 / 128", table.getValueAt(4, 7));
            assertEquals("0.120000 / 0.340000", table.getValueAt(4, 9));
            assertEquals("\u2014", table.getValueAt(0, 4)); assertEquals("\u2014 / \u2014", table.getValueAt(1, 4));
            assertTrue(view.detailsText(4).contains("A · Trained model: Material learner"));
            assertTrue(view.detailsText(4).contains("B · Trained model: Relational learner"));
            assertTrue(view.detailsText(4).contains("BRN_PAIR2 / seedv6.brn.pair2"));
        });
    }
    @Test void oldCampaignLoadsWithoutNewFieldsOrChangingBinding() throws Exception {
        var state = fixture(); String json = DataFiles.JSON.toJson(state);
        assertFalse(json.contains("finalMaterial")); assertFalse(json.contains("training\""));
        assertTrue(json.contains("BRN_PAIR2"));
        var loaded = DataFiles.JSON.fromJson(json, LearningArenaState.class);
        assertEquals(state, loaded); assertEquals(state.binding(), loaded.config().identity());
        assertNull(loaded.current().a().training()); assertNull(loaded.current().pairs().getFirst().candidateWhite().finalMaterial());
    }
    @Test void materialUsesPiecesIncludingPromotionsNeverEvaluation() {
        assertEquals(new MaterialCount(39, 39), MaterialCount.from(Board.startingPosition()));
        assertEquals(new MaterialCount(18, 5), MaterialCount.from(Board.fromFen("7k/8/8/8/8/QQ6/7r/K7 w - - 0 1")));
    }
    @Test void chartSlotsStayLeftPackedOnResizeAndEmptySpaceHasNoRoundTooltip() throws Exception {
        var state = fixture();
        edt(() -> {
            var chart = new ArenaScoreChart();
            chart.showRounds(state.history().subList(0, 2).stream().map(r -> ArenaRoundSummary.from(state.config(), r)).toList());
            chart.setSize(700, 280);
            var event = new MouseEvent(chart, MouseEvent.MOUSE_MOVED, 0, 0, ArenaScoreChart.barX(1) + 5, 100, 0, false);
            String before = chart.getToolTipText(event); assertTrue(before.startsWith("Round 1:"));
            chart.setSize(1400, 280); assertEquals(before, chart.getToolTipText(event));
            assertEquals(ArenaScoreChart.barWidth() + ArenaScoreChart.gap(), ArenaScoreChart.barX(1) - ArenaScoreChart.barX(0));
            assertNull(chart.getToolTipText(new MouseEvent(chart, MouseEvent.MOUSE_MOVED, 0, 0, 1000, 100, 0, false)));
            assertNull(chart.getToolTipText(new MouseEvent(chart, MouseEvent.MOUSE_MOVED, 0, 0, ArenaScoreChart.barX(0) + ArenaScoreChart.barWidth() + 3, 100, 0, false)));
        });
    }
}
