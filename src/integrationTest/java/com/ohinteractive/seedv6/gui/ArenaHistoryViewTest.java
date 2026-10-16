package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.telemetry.*;
import com.ohinteractive.seedv6.training.validation.ValidationResult;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.*;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static com.ohinteractive.seedv6.training.service.LearningArenaConfig.*;
import static com.ohinteractive.seedv6.training.service.LearningArenaState.*;
import static com.ohinteractive.seedv6.training.selfplay.GameTermination.*;
import static org.junit.jupiter.api.Assertions.*;

class ArenaHistoryViewTest {
    @TempDir Path temporary;
    LearningArenaState fixture() throws Exception {
        var source = DataSource.register("Shared fixture", Files.writeString(temporary.resolve("data.jsonl"), SourceReadersTest.line(100)), 1);
        var config = new LearningArenaConfig("Recipe comparison", new Competitor("Material learner", TrainingArchitecture.NNUE_MATERIAL, 71, 32),
                new Competitor("Relational learner", TrainingArchitecture.BRN_PAIR2, 71, 128), source, 100, 8, 6, 71,
                new Arena(4, Limit.DEPTH, 1, 100, 1, 0, 0, 4, TrainerConfig.STANDARD_START));
        var rounds = new ArrayList<Round>();
        for (int round = 0; round <= 6; round++) {
            var pairs = new ArrayList<ValidationResult.Pair>();
            for (int pair = 0; pair < 2; pair++) {
                GameTermination w = round < 4 - pair * 2 ? WHITE_CHECKMATES_BLACK : BLACK_CHECKMATES_WHITE;
                GameTermination b = round < 3 - pair * 2 ? BLACK_CHECKMATES_WHITE : WHITE_CHECKMATES_BLACK;
                if (round == 5) w = b = PLY_CAP;
                if (round == 6) w = b = CANCELLED;
                pairs.add(new ValidationResult.Pair("a".repeat(64), new ValidationResult.Game(w, 4), new ValidationResult.Game(b, 4)));
            }
            var a = new Endpoint(String.format("g%06d-s000000000-", round) + "a".repeat(64), round, round * 100L, round * 800L);
            var b = new Endpoint(String.format("g%06d-s000000000-", round) + "b".repeat(64), round, round * 100L, round * 800L);
            rounds.add(new Round(round, round == 0 ? "" : "c".repeat(64), round * 100, a, b, pairs, round < 6));
        }
        return new LearningArenaState(1, UUID.randomUUID().toString(), config.identity(), config, rounds, Status.PAUSED, "Fixture paused");
    }
    @Test void scoreComplementsWdlWinnersAndUnscoredRoundsUseExistingPairRules() throws Exception {
        var state = fixture();
        for (int r = 0; r < 5; r++) {
            var s = ArenaRoundSummary.from(state.config(), state.history().get(r));
            assertEquals(1 - r * .25, s.firstScore()); assertEquals(4 - r, s.wins()); assertEquals(r, s.losses());
            assertEquals("NNUE", s.first());
            assertEquals(r < 2 ? "NNUE" : r > 2 ? "BRE-Pair 2" : "Tie (A / B)", s.winner());
        }
        var capped = ArenaRoundSummary.from(state.config(), state.history().get(5)); assertNull(capped.firstScore()); assertEquals(2, capped.unscoredPairs()); assertEquals("Unscored", capped.winner());
        var pending = ArenaRoundSummary.from(state.config(), state.history().get(6)); assertNull(pending.firstScore()); assertEquals("Pending", pending.winner());
        var draw = new ValidationResult.Game(STALEMATE, 4); var cap = new ValidationResult.Game(PLY_CAP, 4);
        var base = state.history().getFirst();
        var partialScoring = new Round(0, "", 0, base.a(), base.b(), List.of(new ValidationResult.Pair("draw", draw, draw), new ValidationResult.Pair("cap", cap, cap)), true);
        var summary = ArenaRoundSummary.from(state.config(), partialScoring);
        assertEquals(.5, summary.firstScore()); assertEquals(2, summary.draws()); assertEquals(1, summary.unscoredPairs()); assertEquals("Tie (A / B)", summary.winner());
        var chart = edt(() -> new ArenaScoreChart());
        edt(() -> {
            chart.showRounds(state.history().stream().map(r -> ArenaRoundSummary.from(state.config(), r)).toList()); chart.setSize(1200, 280);
            var image = new BufferedImage(1200, 280, BufferedImage.TYPE_INT_RGB); var g = image.createGraphics(); chart.paint(g); g.dispose();
            assertEquals(ArenaScoreChart.FIRST.getRGB(), image.getRGB(ArenaScoreChart.barX(0) + 5, 52));
            assertEquals(ArenaScoreChart.SECOND.getRGB(), image.getRGB(ArenaScoreChart.barX(4) + 5, 52));
            assertEquals(ArenaScoreChart.FIRST.getRGB(), image.getRGB(ArenaScoreChart.barX(3) + 5, 210));
            assertEquals(ArenaScoreChart.SECOND.getRGB(), image.getRGB(ArenaScoreChart.barX(3) + 5, 52));
            assertEquals(SeedTheme.INSET.getRGB(), image.getRGB(ArenaScoreChart.barX(5) + 5, 52));
        });
    }
    @Test void nativeSavedHistoryAndLiveWorkspaceRenderAtBothDesktopSizes() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless()); var state = fixture();
        Path root = temporary.resolve("campaign"); Files.createDirectories(root); DataFiles.write(root.resolve("campaign.json"), state);
        var folders = new TrainingFolders(TrainingDashboardTest.settings(temporary.resolve("settings")));
        var panel = edt(() -> { SeedTheme.initialize(); return new LearningArenaPanel(folders); });
        var frame = edt(() -> { var f = new JFrame("Arena integration"); f.setContentPane(panel); f.setSize(1100, 760); f.setVisible(true); panel.openCampaign(root); return f; });
        try {
            until(() -> edt(() -> { panel.poll(); return named(panel, "arenaHistory", JTable.class).getRowCount() == 6; }));
            edt(() -> {
                var tabs = named(panel, "arenaViews", JTabbedPane.class); assertEquals("Live", tabs.getTitleAt(1)); assertEquals("History", tabs.getTitleAt(2));
                var table = named(panel, "arenaHistory", JTable.class); assertEquals("NNUE", table.getValueAt(0, 1)); assertEquals("4-0-0", table.getValueAt(0, 2)); assertEquals("100.0% / 0.0%", table.getValueAt(0, 3));
                var recent = new LearningArenaState(1, state.id(), state.binding(), state.config(), state.history().subList(0, 2), Status.PAUSED, "Fixture");
                named(panel, "arenaHistoryView", ArenaHistoryView.class).showState(recent, root);
                var sourceRound = state.history().get(1);
                var active = new Round(1, sourceRound.trancheHash(), sourceRound.sourceEnd(), sourceRound.a(), sourceRound.b(), sourceRound.pairs().subList(0, 1), false);
                var liveState = new LearningArenaState(1, state.id(), state.binding(), state.config(), List.of(state.history().getFirst(), active), Status.RUNNING, "Fixture");
                for (int width : new int[]{1100, 1440}) {
                    frame.setSize(width, width == 1100 ? 760 : 950); tabs.setSelectedIndex(2); frame.validate(); capture(frame, "history-" + width);
                    assertTrue(table.getParent().getHeight() >= 3 * table.getRowHeight(), "At least three completed rounds fit at minimum window size");
                    tabs.setSelectedIndex(1);
                    var feed = new ActiveGameFeed(); feed.arena(1,
                            new ModelLibrary.Binding(root.resolve("A"), Optional.empty(), "Material learner", TrainingArchitecture.NNUE_MATERIAL, "first", 1),
                            new ModelLibrary.Binding(root.resolve("B"), Optional.empty(), "Relational learner", TrainingArchitecture.BRN_PAIR2, "second", 1));
                    var game = new HeadlessGame(com.ohinteractive.seedv6.core.Board.startingPosition(), 10); feed.start(game, 3, 1);
                    long move = ActiveGameFeedTest.move(game, "e2e4"); game.play(move);
                    feed.moved(game, move, new com.ohinteractive.seedv6.search.common.SearchResult(move, true, 100, 4, 1000, 1, true));
                    named(panel, "arenaLive", ArenaLiveView.class).showUpdate(new LearningArenaService.Update(liveState, "Arena game 3 / 4", null, feed.latest(), null));
                    assertEquals("100.0%", named(panel, "arenaFirstScore", JLabel.class).getText());
                    assertEquals(2, named(panel, "arenaGameHistory", JTable.class).getRowCount());
                    assertFalse(named(panel, "arenaOptimization", TrainingProgressView.class).isShowing());
                    frame.validate(); capture(frame, "workspace-live-" + width);
                    assertEquals(0, named(panel, "arenaContextScroll", JScrollPane.class).getViewport().getViewPosition().y, "Live updates must not scroll away the game facts");
                    var gameTable = named(panel, "arenaGameHistory", JTable.class);
                    assertTrue(gameTable.getParent().getHeight() >= 2 * gameTable.getRowHeight(), "Two completed games stay visible at minimum size");
                    assertTrue(named(panel, "arenaMatchBoard", BoardPanel.class).getHeight() > 300, "Board height: " + named(panel, "arenaMatchBoard", BoardPanel.class).getHeight());
                    var trainingRound = new Round(1, sourceRound.trancheHash(), sourceRound.sourceEnd(), null, null, List.of(), false);
                    var trainingState = new LearningArenaState(1, state.id(), state.binding(), state.config(), List.of(state.history().getFirst(), trainingRound), Status.RUNNING, "Fixture training");
                    named(panel, "arenaLive", ArenaLiveView.class).showUpdate(new LearningArenaService.Update(trainingState, "Training A", null, null,
                            new OptimizationSnapshot("NNUE", "Round 1", 8, 32, .001, 400, 800, 13, 0, 13, .12, 4_000_000_000L, "This training segment", 400L, 400)));
                    frame.validate(); assertTrue(named(panel, "arenaOptimization", TrainingProgressView.class).isShowing()); capture(frame, "workspace-training-" + width);
                }
            });
        } finally { var close = edt(panel::beginShutdown); close.run(); edt(frame::dispose); }
    }
    private static void capture(JFrame frame, String name) {
        var image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB); var g = image.createGraphics(); frame.paint(g); g.dispose();
        try { Path path = Path.of("build/learning-arena/" + name + ".png"); Files.createDirectories(path.getParent()); ImageIO.write(image, "png", path.toFile()); }
        catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
    }
}
