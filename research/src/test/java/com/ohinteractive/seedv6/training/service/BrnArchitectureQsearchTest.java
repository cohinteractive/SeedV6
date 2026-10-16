package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.NnueMaterialBootstrap;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.exact.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrnArchitectureQsearchTest {
    @Test void diagnosticUsesNativeEvaluatorAndRetainsGuardFailures() {
        var view=BrnArchitectureMaterial.view();var board=Board.startingPosition();var before=board.clone();
        ExactEvaluator independent=(b,ply)->NnueMaterialBootstrap.forSideToMove(b,NnueMaterialBootstrap.whiteScore(b));
        var direct=ExactSearch.quiescenceResearch(independent).search(board,GameHistory.initial(board),2,()->false);
        var measured=BrnArchitectureQsearch.probe(board,view.evaluator(),17,2,2_000_000,10_000);
        assertTrue((boolean)measured.get("completed"));assertEquals(direct.score(),measured.get("score"));
        assertEquals(direct.nodes(),measured.get("nodes"));assertEquals(direct.qnodes(),measured.get("qnodes"));
        assertEquals(17,measured.get("index"));assertArrayEquals(before,board);
        var limited=BrnArchitectureQsearch.probe(board,view.evaluator(),18,4,1,10_000);
        assertFalse((boolean)limited.get("completed"));assertTrue((boolean)limited.get("nodeGuardReached"));
        assertFalse(limited.containsKey("score"));assertFalse(limited.containsKey("move"));assertArrayEquals(before,board);
    }
    @Test void invalidBudgetsDoNotLaunchSearch() {
        var board=Board.startingPosition();var view=BrnArchitectureMaterial.view();
        assertThrows(IllegalArgumentException.class,()->BrnArchitectureQsearch.probe(board,view.evaluator(),0,5,100,100));
        assertThrows(IllegalArgumentException.class,()->BrnArchitectureQsearch.probe(board,view.evaluator(),0,4,0,100));
        assertThrows(IllegalArgumentException.class,()->BrnArchitectureQsearch.probe(board,view.evaluator(),0,4,100,10001));
    }
}
