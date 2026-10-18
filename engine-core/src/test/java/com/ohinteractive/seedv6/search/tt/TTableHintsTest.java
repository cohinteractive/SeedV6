package com.ohinteractive.seedv6.search.tt;

import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TTableHintsTest {
    @Test void hintsNeverProbeAsProofAndNeverDisplaceOrdinaryProof() {
        var t=new TTable(1);var entry=new TTable.TEntry();
        assertTrue(t.saveHint(7,8,123));assertEquals(123,t.probeHint(7));assertFalse(t.probe(7,entry));
        t.save(7,1,TTable.TYPE_UPPER,-37,456);
        assertTrue(t.probe(7,entry));assertEquals(-37,(int)(entry.data>>32));assertEquals(456,entry.hashMove);
        long proof=entry.data;
        assertFalse(t.saveHint(7,9,789));assertFalse(t.saveHint(7+(1L<<32),9,789));
        assertTrue(t.probe(7,entry));assertEquals(proof,entry.data);assertEquals(0,t.probeHint(7));
        t.advanceGeneration();assertFalse(t.saveHint(7,9,789));assertTrue(t.probe(7,entry));
    }

    @Test void hintDepthAgeIdentityClearAndWrapAreExplicit() {
        var t=new TTable(1);
        assertFalse(t.saveHint(7,0,11));assertFalse(t.saveHint(7,256,11));assertFalse(t.saveHint(7,2,0));
        assertTrue(t.saveHint(7,8,11));assertFalse(t.saveHint(7,7,22));assertTrue(t.saveHint(7,8,33));
        assertEquals(33,t.probeHint(7));assertEquals(0,t.probeHint(7+(1L<<32)));
        t.advanceGeneration();assertTrue(t.saveHint(7+(1L<<32),1,44));assertEquals(0,t.probeHint(7));
        assertEquals(44,t.probeHint(7+(1L<<32)));
        for(int i=1;i<256;i++) t.advanceGeneration();assertEquals(0,t.probeHint(7+(1L<<32)));
        t.saveHint(7,1,11);t.clear();assertEquals(0,t.probeHint(7));
    }

    @Test void collidingWorkersNeverObserveAnotherKeysMove() throws Exception {
        var t=new TTable(1);var workers=Executors.newFixedThreadPool(4);
        try {
            var tasks=new java.util.ArrayList<Future<?>>();
            for(int worker=1;worker<=4;worker++) {
                final long key=7+((long)worker<<32),move=worker;
                tasks.add(workers.submit(()->{
                    var entry=new TTable.TEntry();
                    for(int i=0;i<10000;i++) {
                        t.saveHint(key,4,move);long found=t.probeHint(key);
                        assertTrue(found==0 || found==move);assertFalse(t.probe(key,entry));
                    }
                }));
            }
            for(var task:tasks) task.get(15,TimeUnit.SECONDS);
        } finally {workers.shutdownNow();assertTrue(workers.awaitTermination(5,TimeUnit.SECONDS));}
    }
}
