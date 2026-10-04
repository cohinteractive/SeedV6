package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.training.validation.ValidationArena;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrnSuccessorMatchTest {
    static BrnSuccessorMatch.Game game(Double score) {
        return new BrnSuccessorMatch.Game(score==null?"PLY_CAP":"STALEMATE",score,null,2048,0,0,0,0,"",List.of());
    }
    @Test void incompletePairsCannotBecomeDrawsOrEnterPairedUncertainty() {
        var report=BrnSuccessorMatch.summary(List.of(new BrnSuccessorMatch.Pair(0,"a","",game(1.0),game(0.0)),
                new BrnSuccessorMatch.Pair(1,"b","",game(null),game(.5))));
        assertEquals(1,report.get("completePairs"));assertEquals(1,report.get("incompletePairs"));
        assertEquals(1,report.get("wins"));assertEquals(1,report.get("losses"));assertEquals(0,report.get("draws"));
        assertEquals(.5,report.get("pointScore"));assertEquals(List.of(.5,.5),report.get("pairedBootstrap95"));
    }
    @Test void wallBudgetAbortsWithoutInventingOutcomeAndTerminalRulesRemainAuthoritative() {
        Supplier<SearchDriver> driver=()->new SearchDriver(new ExactSearchAdapter((b,ply)->0,new TTable(1)));
        var board=Board.startingPosition();var opening=new ValidationArena.Opening(board,GameHistory.initial(board),0);
        var aborted=BrnSuccessorMatch.play(opening,driver,driver,0,1,10,System.nanoTime()-1);
        assertEquals("CANCELLED",aborted.termination());assertNull(aborted.score());
        var mate=Board.fromFen("7k/6Q1/5K2/8/8/8/8/8 b - - 0 1");
        var terminal=new ValidationArena.Opening(mate,GameHistory.initial(mate),0);
        var result=BrnSuccessorMatch.play(terminal,driver,driver,0,1,10,System.nanoTime()+1_000_000_000L);
        assertEquals(1,result.score());assertEquals("WHITE_CHECKMATES_BLACK",result.termination());
    }
}
