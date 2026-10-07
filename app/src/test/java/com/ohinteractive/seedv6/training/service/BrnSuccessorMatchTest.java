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
        var opposite=BrnSuccessorMatch.play(terminal,driver,driver,1,1,10,System.nanoTime()+1_000_000_000L);
        assertEquals(0,opposite.score());assertEquals(result.termination(),opposite.termination());
    }
    @Test void identicalFixedDepthPlayersKeepMovesAndReverseActorNodeAccounting() {
        Supplier<SearchDriver> driver=()->new SearchDriver(new ExactSearchAdapter((b,ply)->0,new TTable(1)));
        var board=Board.startingPosition();var opening=new ValidationArena.Opening(board,GameHistory.initial(board),0);
        var white=BrnSuccessorMatch.play(opening,driver,driver,0,1,10,System.nanoTime()+5_000_000_000L,5);
        var black=BrnSuccessorMatch.play(opening,driver,driver,1,1,10,System.nanoTime()+5_000_000_000L,5);
        assertNull(white.failure());assertNull(black.failure());
        assertEquals(white.moves(),black.moves());assertEquals(white.finalFen(),black.finalFen());
        assertEquals(white.candidateNodes(),black.opponentNodes());
        assertEquals(white.opponentNodes(),black.candidateNodes());
        assertEquals("PLY_CAP",white.termination());assertEquals(white.termination(),black.termination());
        assertNull(white.score());assertNull(black.score());
    }
    @Test void explicitPlyCapIsEnforcedAndIsNeverScoredAsADraw() {
        Supplier<SearchDriver> driver=()->new SearchDriver(new ExactSearchAdapter((b,ply)->0,new TTable(1)));
        var board=Board.startingPosition();var opening=new ValidationArena.Opening(board,GameHistory.initial(board),0);
        for(int cap:new int[]{1,2,5}) {
            var result=BrnSuccessorMatch.play(opening,driver,driver,0,1,10,System.nanoTime()+5_000_000_000L,cap);
            assertEquals(cap,result.plies());assertEquals("PLY_CAP",result.termination());assertNull(result.score());assertNull(result.failure());
        }
    }
}
