package com.ohinteractive.seedv6.gui;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Value;
import static com.ohinteractive.seedv6.gui.NnueGuiFixtures.edt;
import static org.junit.jupiter.api.Assertions.*;

class PlayPresentationTest {
    @Test void scoresUseWhitePerspectiveAndKeepUncalibratedNnueUnitsDistinct() {
        var blackSearch = info("cp 137", Value.BLACK);
        var handcrafted = PlayScore.from(blackSearch, false);
        assertEquals("-1.37", handcrafted.text()); assertTrue(handcrafted.whiteFraction() < 0.5);
        assertEquals("-137", PlayScore.from(blackSearch, true).text());
        assertEquals(0.5, PlayScore.from(info("cp 0", Value.WHITE), false).whiteFraction());
        assertEquals("+M3", PlayScore.from(info("mate -3", Value.BLACK), false).text());
        assertEquals(1, PlayScore.from(info("mate -3", Value.BLACK), false).whiteFraction());
        assertEquals(0, PlayScore.from(info("mate 3", Value.BLACK), false).whiteFraction());
        assertFalse(PlayScore.from(GameController.SearchInfo.idle(), false).available());
        assertEquals(0.5, PlayScore.from(info("unavailable", Value.WHITE), false).whiteFraction());
        assertTrue(PlayScore.from(info("cp 29999", Value.WHITE), false).whiteFraction() < 1);
    }

    @Test void scoresheetKeepsCoordinateNotationAndBlackFirstFenAlignment() throws Exception {
        edt(() -> {
            SeedTheme.initialize();
            MoveScoresheet sheet = new MoveScoresheet();
            JTable table = descendants(sheet, JTable.class);
            var start = position("4k3/8/8/8/8/8/8/R3K3 b - - 0 17", List.of());
            sheet.showPosition(start); assertEquals(0, table.getRowCount());
            sheet.showPosition(position("3k4/8/8/8/8/8/8/R3K3 w - - 1 18", List.of("e8d8")));
            assertEquals(17, table.getValueAt(0, 0)); assertEquals("", table.getValueAt(0, 1));
            assertEquals("e8d8", table.getValueAt(0, 2));
            sheet.showPosition(position("3k4/8/8/8/8/8/R7/4K3 b - - 2 18", List.of("e8d8", "a1a2")));
            assertEquals(18, table.getValueAt(1, 0)); assertEquals("a1a2", table.getValueAt(1, 1));
            sheet.showPosition(position(Board.FEN_STARTING_POSITION, List.of())); assertEquals(0, table.getRowCount());
        });
    }

    @Test void scoresheetFollowsBottomButPreservesUserScrollAndSelectionOnlyRefresh() throws Exception {
        final MoveScoresheet sheet = edt(() -> { SeedTheme.initialize(); return new MoveScoresheet(); });
        final JScrollPane pane = edt(() -> descendants(sheet, JScrollPane.class));
        final JTable table = edt(() -> descendants(sheet, JTable.class));
        edt(() -> { pane.setSize(500, 180); pane.doLayout(); sheet.showPosition(position(Board.FEN_STARTING_POSITION, List.of())); });
        edt(() -> sheet.showPosition(position(Board.FEN_STARTING_POSITION, java.util.Collections.nCopies(80, "e2e4"))));
        edt(() -> {});
        edt(() -> {
            pane.doLayout(); pane.getViewport().doLayout();
            assertTrue(pane.getVerticalScrollBar().getValue() > 0, "New moves follow bottom");
            pane.getVerticalScrollBar().setValue(60);
            sheet.showPosition(position(Board.FEN_STARTING_POSITION, java.util.Collections.nCopies(82, "e2e4")));
        });
        edt(() -> {});
        edt(() -> {
            assertEquals(60, pane.getVerticalScrollBar().getValue());
            sheet.showPosition(position(Board.FEN_STARTING_POSITION, java.util.Collections.nCopies(82, "e2e4")));
            assertEquals(60, pane.getVerticalScrollBar().getValue()); assertEquals(41, table.getRowCount());
        });
    }

    private static GameController.SearchInfo info(String score, int side) {
        return new GameController.SearchInfo("Thinking", 3, score, 100, 1000, "e2e4", "—", 100, side);
    }
    static GameController.PositionView position(String fen, List<String> moves) {
        GameSession session = GameSession.fromFen(fen);
        return new GameController.PositionView(session.boardSnapshot(), session.status(), moves, -1, new int[0], -1, -1, -1);
    }
    static <T extends Component> T descendants(Container parent, Class<T> type) {
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container container) {
                T found = descendants(container, type); if (found != null) return found;
            }
        }
        return null;
    }
}
