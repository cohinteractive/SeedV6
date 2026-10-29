package com.ohinteractive.seedv6.search.tt;

import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TTableMemoryTest {
    @Test void heuristicDomainNeverEscapesIntoOrdinaryProof() {
        var t=new TTable(1);var e=new TTable.TEntry();
        assertTrue(t.saveHeuristic(7,8,TTable.TYPE_EXACT,37,123));
        assertFalse(t.probe(7,e));
        assertTrue(t.probeHeuristic(7,e));assertEquals(37,(int)(e.data>>32));
        assertEquals(123,t.probeHint(7));
        t.save(7,2,TTable.TYPE_LOWER,42,456);
        assertTrue(t.probe(7,e));assertFalse(t.probeHeuristic(7,e));
        assertFalse(t.saveHeuristic(7,20,TTable.TYPE_EXACT,9,999));
        assertTrue(t.probe(7,e));assertEquals(42,(int)(e.data>>32));
    }

    @Test void depthAndHintRetentionRemainIndependentAndAgeCannotRefreshScore() {
        var t=new TTable(1);var e=new TTable.TEntry();
        assertTrue(t.saveHint(7,10,100));
        assertTrue(t.saveHeuristic(7,8,TTable.TYPE_LOWER,37,200));
        assertEquals(100,t.probeHint(7));
        assertFalse(t.saveHint(7,9,300));assertTrue(t.saveHint(7,10,400));
        assertTrue(t.probeHeuristic(7,e));assertEquals(8,e.data&255);assertEquals(37,(int)(e.data>>32));
        assertEquals(400,e.hashMove);
        assertFalse(t.saveHeuristic(7,3,TTable.TYPE_EXACT,99,500));
        assertEquals(400,t.probeHint(7));
        assertFalse(t.saveHeuristic(7+(1L<<32),5,TTable.TYPE_EXACT,99,500));
        assertTrue(t.probeHeuristic(7,e));
        t.advanceGeneration();assertTrue(t.saveHint(7,2,600));
        assertFalse(t.probeHeuristic(7,e));assertEquals(600,t.probeHint(7));
        assertTrue(t.saveHeuristic(7,3,TTable.TYPE_UPPER,-9,0));assertEquals(600,t.probeHint(7));
        t.clear();assertFalse(t.probeHeuristic(7,e));assertEquals(0,t.probeHint(7));
    }

    @Test void ordinaryScoreLimitsDepthAndGenerationWrapAreExplicit() {
        var t=new TTable(64);var e=new TTable.TEntry();
        assertEquals(2097152,t.entryCapacity());assertEquals(50331648,t.entryPayloadBytes());
        for(int score:new int[]{32768,32512,-32512,-32768,Integer.MIN_VALUE,Integer.MAX_VALUE})
            assertFalse(t.saveHeuristic(1,3,TTable.TYPE_LOWER,score,2));
        for(int depth:new int[]{0,256,-1}) assertFalse(t.saveHeuristic(1,depth,0,0,2));
        assertFalse(t.saveHeuristic(1,2,TTable.TYPE_EVAL,0,2));
        assertTrue(t.saveHeuristic(1,255,0,32511,2));
        for(int i=0;i<256;i++)t.advanceGeneration();
        assertFalse(t.probeHeuristic(1,e));assertEquals(0,t.probeHint(1));
    }

    @Test void collidingWorkersPublishOnlyCoherentKeyScoreMoveTriples() throws Exception {
        var t=new TTable(1);var pool=Executors.newFixedThreadPool(8);
        try {
            var jobs=new java.util.ArrayList<Future<?>>();
            for(int w=1;w<=8;w++) {
                final int worker=w;final long key=17+((long)w<<32);
                jobs.add(pool.submit(()->{
                    var e=new TTable.TEntry();
                    for(int i=0;i<10000;i++) {
                        t.saveHeuristic(key,4,TTable.TYPE_LOWER,worker,worker);
                        if(t.probeHeuristic(key,e)) {
                            assertEquals(worker,(int)(e.data>>32));assertEquals(worker,e.hashMove);
                        }
                        long move=t.probeHint(key);assertTrue(move==0 || move==worker);
                        assertFalse(t.probe(key,e));t.saveHint(key,4,worker);
                    }
                }));
            }
            for(var job:jobs)job.get(20,TimeUnit.SECONDS);
        } finally {pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
}
