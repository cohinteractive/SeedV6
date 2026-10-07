package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.exact.ExactEvaluator;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrnArchitectureTransitionsTest {
    @Test void commonStreamIsDeterministicAndIncludesEverySpecialCategory()throws Exception {
        var parents=List.of(Board.startingPosition());var before=parents.get(0).clone();
        var a=BrnArchitectureTransitions.requests(parents);var b=BrnArchitectureTransitions.requests(parents);
        assertEquals(BrnArchitectureTransitions.digest(a),BrnArchitectureTransitions.digest(b));assertArrayEquals(before,parents.get(0));
        for(int g=0;g<=6;g++){final int group=g;assertTrue(a.stream().anyMatch(r->BrnArchitectureTransitions.inGroup(r,group)),"Missing category "+g);}
        b.get(0).child()[0]^=1;assertNotEquals(BrnArchitectureTransitions.digest(a),BrnArchitectureTransitions.digest(b));
    }
    @Test void wrongChildScoreOrBoardMutationBlocksTheProbe() {
        var requests=BrnArchitectureTransitions.requests(List.of(Board.startingPosition()));
        var created=new java.util.concurrent.atomic.AtomicInteger();
        var okay=BrnArchitectureTransitions.verify(requests,(b,p)->0,()->{created.incrementAndGet();return (b,p)->0;});
        assertEquals(requests.size(),created.get());assertEquals(0,okay.get("maximumAbsoluteIntegerScoreDifference"));
        assertThrows(AssertionError.class,()->BrnArchitectureTransitions.verify(requests,(b,p)->p==0?0:20,()->(b,p)->0));
        var mutating=new ExactEvaluator(){public int evaluate(long[] b,int p){b[5]^=1;return 0;}};
        assertThrows(AssertionError.class,()->BrnArchitectureTransitions.verify(requests,mutating,()->(b,p)->0));
    }
}
