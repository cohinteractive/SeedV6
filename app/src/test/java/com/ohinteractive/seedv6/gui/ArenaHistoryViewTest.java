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
                new Competitor("Relational learner", TrainingArchitecture.BRN3, 71, 128), source, 100, 8, 6, 71,
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
            assertTrue(s.first().contains("Material learner / NNUE (material parity) / Gen " + r));
            assertEquals(r < 2 ? "Material learner" : r > 2 ? "Relational learner" : "Draw", s.winner());
        }
        var capped = ArenaRoundSummary.from(state.config(), state.history().get(5)); assertNull(capped.firstScore()); assertEquals(2, capped.unscoredPairs()); assertEquals("Unscored", capped.winner());
        var pending = ArenaRoundSummary.from(state.config(), state.history().get(6)); assertNull(pending.firstScore()); assertEquals("Pending", pending.winner());
        var draw = new ValidationResult.Game(STALEMATE, 4); var cap = new ValidationResult.Game(PLY_CAP, 4);
        var base = state.history().getFirst();
        var partialScoring = new Round(0, "", 0, base.a(), base.b(), List.of(new ValidationResult.Pair("draw", draw, draw), new ValidationResult.Pair("cap", cap, cap)), true);
        var summary = ArenaRoundSummary.from(state.config(), partialScoring);
        assertEquals(.5, summary.firstScore()); assertEquals(2, summary.draws()); assertEquals(1, summary.unscoredPairs()); assertEquals("Draw", summary.winner());
        var chart = edt(() -> new ArenaScoreChart());
        edt(() -> {
            chart.showRounds(state.history().stream().map(r -> ArenaRoundSummary.from(state.config(), r)).toList()); chart.setSize(555, 232);
            var image = new BufferedImage(555, 232, BufferedImage.TYPE_INT_RGB); var g = image.createGraphics(); chart.paint(g); g.dispose();
            // Every scored column fills the entire 100% height from zero; 25/75 shares complement.
            assertEquals(ArenaScoreChart.FIRST.getRGB(), image.getRGB(71, 25));
            assertEquals(ArenaScoreChart.SECOND.getRGB(), image.getRGB(351, 25));
            assertEquals(ArenaScoreChart.FIRST.getRGB(), image.getRGB(281, 184));
            assertEquals(ArenaScoreChart.SECOND.getRGB(), image.getRGB(281, 25));
            assertEquals(SeedTheme.INSET.getRGB(), image.getRGB(421, 25));
        });
    }
    @Test void nativeSavedHistoryAndLiveWorkspaceRenderAtBothDesktopSizes() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless()); var state = fixture();
        Path root = temporary.resolve("campaign"); Files.createDirectories(root); DataFiles.write(root.resolve("campaign.json"), state);
        var folders = new TrainingFolders(TrainingDashboardTest.settings(temporary.resolve("settings")));
        var panel = edt(() -> { SeedTheme.initialize(); return new LearningArenaPanel(folders); });
        var frame = edt(() -> { var f = new JFrame("Arena integration"); f.setContentPane(panel); f.setSize(1100, 760); f.setVisible(true); panel.openCampaign(root); return f; });
        try {
            until(() -> edt(() -> { panel.poll(); return named(panel, "arenaHistory", JTable.class).getRowCount() == 14; }));
            edt(() -> {
                var tabs = named(panel, "arenaViews", JTabbedPane.class); assertEquals("Live", tabs.getTitleAt(1)); assertEquals("History", tabs.getTitleAt(2));
                var table = named(panel, "arenaHistory", JTable.class); assertTrue(table.getValueAt(0, 1).toString().contains("Material learner")); assertEquals(0L, table.getValueAt(0, 2)); assertEquals("100.0%", table.getValueAt(0, 6)); assertEquals("0.0%", table.getValueAt(1, 6));
                for (int width : new int[]{1100, 1440}) {
                    frame.setSize(width, width == 1100 ? 760 : 950); tabs.setSelectedIndex(2); frame.validate(); capture(frame, "history-" + width);
                    assertTrue(table.getHeight() >= 200);
                    tabs.setSelectedIndex(1);
                    var feed = new ActiveGameFeed(); feed.arena(1,
                            new ModelLibrary.Binding(root.resolve("A"), Optional.empty(), "Material learner", TrainingArchitecture.NNUE_MATERIAL, "first", 1),
                            new ModelLibrary.Binding(root.resolve("B"), Optional.empty(), "Relational learner", TrainingArchitecture.BRN3, "second", 1));
                    feed.start(new HeadlessGame(com.ohinteractive.seedv6.core.Board.startingPosition(), 10), 1, 1);
                    named(panel, "arenaMatch", MatchView.class).showGame(feed.latest(), null);
                    named(panel, "arenaOptimization", TrainingProgressView.class).showProgress(new OptimizationSnapshot("Relational learner - BRN-3", "Round 1 - training from Gen 0", 8, 128, .003, 512, 800, 4, 0, 4, .012, 4_000_000_000L, "This training segment", 512L, 512));
                    frame.validate(); assertTrue(named(panel, "arenaMatchBoard", BoardPanel.class).getHeight() > 300); capture(frame, "workspace-live-" + width);
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
