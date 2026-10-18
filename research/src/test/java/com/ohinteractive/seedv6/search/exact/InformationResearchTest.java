package com.ohinteractive.seedv6.search.exact;

import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import static org.junit.jupiter.api.Assertions.*;

class InformationResearchTest {
    @Test void policyAndWorkerDomainsAreExplicit() throws Exception {
        for(int p=0;p<8;p++) assertTrue(InformationResearch.accepts(1,p));
        for(int workers=2;workers<=16;workers++) for(int p=0;p<8;p++)
            assertEquals(p==0 || p==4,InformationResearch.accepts(workers,p));
        assertFalse(InformationResearch.accepts(0,0));assertFalse(InformationResearch.accepts(17,4));
        assertFalse(InformationResearch.accepts(1,8));
        assertThrows(IllegalArgumentException.class,()->InformationResearch.create(SearchEvaluation.handcrafted(),1,64,4));
        var e=P3InformationBaselineTest.evaluation();
        assertThrows(IllegalArgumentException.class,()->InformationResearch.create(e,4,64,7));
        assertThrows(IllegalArgumentException.class,()->InformationResearch.create(e,1,72,4));
    }
}
