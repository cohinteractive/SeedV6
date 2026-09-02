package com.ohinteractive.seedv6.rules;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.rules.DrawAdjudicator.RuleDraw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrawAdjudicatorTest {

    @Test
    void fiftyMoveThresholdIsExactlyOneHundredHalfmoves() {
        final long[] at99 = Board.fromFen("4k3/8/8/8/8/8/8/R3K3 w - - 99 1");
        final long[] at100 = Board.fromFen("4k3/8/8/8/8/8/8/R3K3 w - - 100 1");

        assertFalse(DrawAdjudicator.isFiftyMoveClaimable((int) at99[Board.STATUS]));
        assertTrue(DrawAdjudicator.isFiftyMoveClaimable((int) at100[Board.STATUS]));
        assertEquals(
            RuleDraw.NONE,
            DrawAdjudicator.adjudicateNonTerminal(
                at99, new SearchLineHistory(GameHistory.initial(at99))
            )
        );
        assertEquals(
            RuleDraw.FIFTY_MOVE,
            DrawAdjudicator.adjudicateNonTerminal(
                at100, new SearchLineHistory(GameHistory.initial(at100))
            )
        );
    }

    @Test
    void conservativeAutomaticMaterialDrawClassesAreRecognized() {
        assertAutomatic("4k3/8/8/8/8/8/8/4K3 w - - 0 1");
        assertAutomatic("4k3/8/8/8/8/8/8/2B1K3 w - - 0 1");
        assertAutomatic("2b1k3/8/8/8/8/8/8/4K3 w - - 0 1");
        assertAutomatic("4k3/8/8/8/8/8/8/2N1K3 w - - 0 1");
        assertAutomatic("2n1k3/8/8/8/8/8/8/4K3 w - - 0 1");
        assertAutomatic("4kb2/8/8/8/8/8/8/2B1K3 w - - 0 1");
        assertAutomatic("4kb2/8/8/8/8/4B3/8/2B1K3 w - - 0 1");
    }

    @Test
    void nearbyMatingMaterialClassesAreNotFalselyClaimed() {
        assertNotAutomatic("4k3/8/8/8/8/8/8/1NN1K3 w - - 0 1");
        assertNotAutomatic("4k3/8/8/8/8/8/8/1BN1K3 w - - 0 1");
        assertNotAutomatic("2b1k3/8/8/8/8/8/8/2B1K3 w - - 0 1");
        assertNotAutomatic("4k3/8/8/8/8/8/8/3PK3 w - - 0 1");
        assertNotAutomatic("4k3/8/8/8/8/8/8/R3K3 w - - 0 1");
        assertNotAutomatic("4k3/8/8/8/8/8/8/Q3K3 w - - 0 1");
    }

    @Test
    void staticEvaluationNoLongerOwnsRuleDrawMaterial() {
        final long[] board = Board.fromFen("4k3/8/8/8/8/8/8/2B1K3 w - - 0 1");
        final int staticEval = Eval.eval(
            board[0], board[1], board[2], board[3],
            (int) board[Board.STATUS], board[Board.KEY]
        );

        assertEquals(330, staticEval);
        assertEquals(
            RuleDraw.INSUFFICIENT_MATERIAL,
            DrawAdjudicator.adjudicateNonTerminal(
                board, new SearchLineHistory(GameHistory.initial(board))
            )
        );
    }

    private static void assertAutomatic(String fen) {
        final long[] board = Board.fromFen(fen);
        assertTrue(InsufficientMaterial.isAutomaticDraw(board[0], board[1], board[2], board[3]));
    }

    private static void assertNotAutomatic(String fen) {
        final long[] board = Board.fromFen(fen);
        assertFalse(InsufficientMaterial.isAutomaticDraw(board[0], board[1], board[2], board[3]));
    }
}
