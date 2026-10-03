package com.ohinteractive.seedv6.training.service;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrnResearchMatchTest {
    static BrnResearchMatch.Game game(String termination,Double score){return new BrnResearchMatch.Game(termination,null,10,score,"",List.of(),0,0,0,0,0,0);}
    @Test void administrativeStopsAreNeverDrawsOrCompletePairs(){
        var pair=new BrnResearchMatch.Pair(0,"","",game("PLY_CAP",null),game("STALEMATE",.5));var summary=BrnResearchMatch.summary(List.of(pair));
        assertFalse(pair.complete());assertEquals(0,summary.get("completedPairs"));assertEquals(1,summary.get("incompletePairs"));
        assertEquals(0,summary.get("draws"));assertFalse(summary.containsKey("brnPointScore"));assertEquals(false,summary.get("acceptanceSampleSizeMet"));
    }
    @Test void pairAccountingUsesBothColorsAndDoesNotGrantSmallSampleAcceptance(){
        var pair=new BrnResearchMatch.Pair(0,"","",game("WHITE_CHECKMATES_BLACK",1.0),game("WHITE_CHECKMATES_BLACK",0.0));
        var summary=BrnResearchMatch.summary(List.of(pair));assertEquals(.5,pair.score());assertEquals(.5,summary.get("brnPointScore"));
        assertEquals(1,summary.get("brnWins"));assertEquals(1,summary.get("brnLosses"));assertEquals(false,summary.get("acceptanceSampleSizeMet"));
    }
}
