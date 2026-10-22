package com.ohinteractive.seedv6.gui;

import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.search.common.SearchResult;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.gui.ConventionalGuiProbe.edt;

class BaselineScoreTest {
    @Test void centipawnsConvertExactlyOnceToWhitePawnsAndKeepMateSeparate() throws Exception {
        for (var mode : new PlayEvaluator.Mode[]{PlayEvaluator.Mode.CONVENTIONAL_CP64, PlayEvaluator.Mode.BREP2_32}) {
        var binding = PlayEvaluator.bundled(mode);
        for (int side : new int[]{Value.WHITE, Value.BLACK}) {
            var info = new GameController.SearchInfo("Idle", 5, "cp 550", 10, 10, "", "DEPTH", 1, side);
            var displayed = edt(() -> new EngineCard().showSearch(info, binding, 4));
            assertEquals(side == Value.WHITE ? "+5.50" : "-5.50", displayed.text());
            var score = PlayedMove.Score.from(new SearchResult(0, false, 550, 5, 10, 0, true), side, 0, binding);
            assertEquals(displayed.text(), score.text());
            assertTrue(score.description().contains("pawns"));
        }
        }
        assertEquals("+M1", PlayScore.from(new GameController.SearchInfo("Idle", 1, "mate 1", 1, 1, "", "DEPTH"), false).text());
        assertFalse(PlayScore.from(GameController.SearchInfo.idle(), false).available());
    }
}
