package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.selfplay.HeadlessGame;
import com.ohinteractive.seedv6.training.telemetry.ActiveGameFeed;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.edt;
import static com.ohinteractive.seedv6.gui.TrainingWorkspaceSmokeTest.named;
import static com.ohinteractive.seedv6.training.telemetry.ActiveGameFeedTest.move;
import static org.junit.jupiter.api.Assertions.*;

class ArenaMatchPresentationTest {
    @Test void exactReversedBindingsMixedScoreUnitsAndInactiveBoardAreShared() throws Exception {
        var a = new ModelLibrary.Binding(Path.of("a"), Optional.of(UUID.randomUUID()), "Material experiment", TrainingArchitecture.NNUE_MATERIAL, "first", 14);
        var b = new ModelLibrary.Binding(Path.of("b"), Optional.of(UUID.randomUUID()), "Relational experiment", TrainingArchitecture.BRN3, "second", 10);
        var feed = new ActiveGameFeed(); feed.arena(2, a, b);
        var game = new HeadlessGame(Board.startingPosition(), 20); feed.start(game, 2, 2);
        assertEquals(b, feed.latest().white().model()); assertEquals(a, feed.latest().black().model());
        long first = move(game, "e2e4"); game.play(first); feed.moved(game, first, new SearchResult(first, true, 200, 2, 10, 1, true));
        assertEquals(.5 + .48 * Math.tanh(200 / 400.0), MatchPresentation.score(feed.latest(), NetworkArchitecture.NNUE).whiteFraction());
        assertTrue(MatchPresentation.evaluationCaption(feed.latest(), NetworkArchitecture.NNUE).contains("100 = 1 pawn"));
        edt(() -> {
            SeedTheme.initialize(); var view = new MatchView("fixture"); var board = named(view, "fixtureBoard", BoardPanel.class);
            assertTrue(board.positionUnavailable()); view.showGame(feed.latest(), null);
            assertTrue(named(view, "fixtureWhite", JTextArea.class).getText().contains("Relational experiment"));
            assertTrue(named(view, "fixtureBlack", JTextArea.class).getText().contains("Gen 14"));
            assertArrayEquals(game.boardSnapshot(), board.displayedBoard());
            if (!GraphicsEnvironment.isHeadless()) {
                JFrame frame = new JFrame("Arena live match"); frame.setContentPane(view); frame.setSize(1100, 760); frame.setVisible(true);
                try {
                    frame.validate();
                    var image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
                    var graphics = image.createGraphics(); frame.paint(graphics); graphics.dispose();
                    try { Path path = Path.of("build/learning-arena/live-match.png"); Files.createDirectories(path.getParent()); ImageIO.write(image, "png", path.toFile()); }
                    catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
                    assertTrue(board.getHeight() >= 400, "Board height " + board.getHeight() + "; view " + view.getSize());
                } finally { frame.dispose(); }
            }
            view.showGame(null, null); assertTrue(board.positionUnavailable());
        });
        long second = move(game, "e7e5"); game.play(second); feed.moved(game, second, new SearchResult(second, true, 200, 2, 10, 1, true));
        assertEquals(.5 + .48 * Math.tanh(-200 / 2000.0), MatchPresentation.score(feed.latest(), NetworkArchitecture.BRN3).whiteFraction());
        assertTrue(MatchPresentation.evaluationCaption(feed.latest(), NetworkArchitecture.BRN3).contains("material + residual units"));
        feed.close(); assertNull(feed.latest());
    }
}
