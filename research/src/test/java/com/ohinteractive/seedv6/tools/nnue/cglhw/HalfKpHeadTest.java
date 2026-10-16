package com.ohinteractive.seedv6.tools.nnue.cglhw;

import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HalfKpHeadTest {
    @Test void baselineWrapperMatchesProductionExactlyAndOddHeadChangesSignWithoutChangingBoard() {
        var base=new HalfKpHead(41001,false,false).worker(3);var odd=new HalfKpHead(41001,true,false).worker(3);
        var original=SearchEvaluation.incremental(NnueNetwork.initialized(41001)).newState(3);
        for(var e:BootstrapAudit.corpus(2,80)) {
            var p=e.parent();var c=e.child();var untouched=p.clone();
            base.refresh(p,0);base.transition(p,c,0,1);original.initialize(p,0);original.child(p,c,0);
            assertEquals(original.evaluate(p,0),base.evaluate(p,0));assertEquals(original.evaluate(c,1),base.evaluate(c,1));
            odd.refresh(p,0);odd.transition(p,c,0,1);odd.refresh(c,2);
            assertEquals(odd.raw(c,2),odd.raw(c,1),1e-9);
            var flip=p.clone();flip[4]^=1;
            assertEquals(-odd.raw(p,0),odd.raw(flip,0),0);
            assertEquals(-odd.evaluate(p,0),odd.evaluate(flip,0));
            assertArrayEquals(untouched,p);
        }
    }
}
