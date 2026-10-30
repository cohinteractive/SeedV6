package com.ohinteractive.seedv6.search.tt;

import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TTableAgedMemoryTest {
    static int generation(TTable.TEntry e) {return (int)(e.data>>>10&255);}

    @Test void priorScoreAndFreshHintHaveIndependentLifetimes() {
        var t=new TTable(1);var e=new TTable.TEntry();t.advanceGeneration();
        assertTrue(t.saveHeuristicAged(7,8,TTable.TYPE_LOWER,37,100));
        t.advanceGeneration();assertTrue(t.saveHintAged(7,3,200));
        assertTrue(t.probeHeuristic(7,e));assertEquals(1,generation(e));assertEquals(8,e.data&255);
        assertEquals(37,(int)(e.data>>32));assertEquals(200,e.hashMove);
        assertFalse(t.saveHeuristicAged(7,2,TTable.TYPE_UPPER,-9,300));
        assertEquals(200,t.probeHint(7));assertTrue(t.saveHintAged(7,4,400));
        assertTrue(t.probeHeuristic(7,e));assertEquals(1,generation(e));assertEquals(400,e.hashMove);
        assertFalse(t.probe(7,e));
        t.advanceGeneration();assertTrue(t.saveHintAged(7,1,500));
        assertFalse(t.probeHeuristic(7,e));assertEquals(500,t.probeHint(7));
    }

    @Test void shallowScoreCanRefreshOldGuidanceWithoutErasingDeeperPriorScore() {
        var t=new TTable(1);var e=new TTable.TEntry();
        assertTrue(t.saveHeuristicAged(7,8,TTable.TYPE_LOWER,37,100));t.advanceGeneration();
        assertFalse(t.saveHeuristicAged(7,2,TTable.TYPE_UPPER,-9,200));
        assertTrue(t.probeHeuristic(7,e));assertEquals(0,generation(e));assertEquals(37,(int)(e.data>>32));
        assertEquals(200,e.hashMove);assertTrue(t.saveHintAged(7,10,400));
        assertTrue(t.saveHeuristicAged(7,6,TTable.TYPE_UPPER,-9,300));
        assertTrue(t.probeHeuristic(7,e));assertEquals(1,generation(e));assertEquals(6,e.data&255);
        assertEquals(-9,(int)(e.data>>32));assertEquals(400,e.hashMove);
        assertFalse(t.saveHintAged(7,9,999));
    }

    @Test void collisionsRespectDiscountedScoreDepthAndCurrentGuidance() {
        var t=new TTable(1);var e=new TTable.TEntry();long collision=7+(1L<<32);
        t.saveHeuristicAged(7,8,TTable.TYPE_LOWER,37,100);t.advanceGeneration();
        assertFalse(t.saveHeuristicAged(collision,5,TTable.TYPE_EXACT,12,200));
        assertFalse(t.saveHintAged(collision,5,200));
        assertTrue(t.saveHintAged(7,10,300));
        assertFalse(t.saveHeuristicAged(collision,9,TTable.TYPE_EXACT,12,200));
        assertTrue(t.saveHintAged(collision,10,200));
        assertFalse(t.probeHeuristic(7,e));assertEquals(200,t.probeHint(collision));
        var u=new TTable(1);u.saveHeuristicAged(7,8,TTable.TYPE_LOWER,37,100);u.advanceGeneration();
        assertTrue(u.saveHeuristicAged(collision,6,TTable.TYPE_EXACT,12,200));
    }

    @Test void moduloEightCannotMakeExpiredHintsCurrent() {
        var t=new TTable(1);var e=new TTable.TEntry();t.saveHeuristicAged(7,8,0,37,100);
        for(int i=0;i<8;i++)t.advanceGeneration();
        assertTrue(t.saveHintAged(7,1,200));assertFalse(t.probeHeuristic(7,e));
        assertEquals(200,t.probeHint(7));
        // An old hint copied into a new score must also stay stale on the NEXT
        // generation, even when its historical modulo8 tag would then coincide.
        var u=new TTable(1);u.saveHeuristicAged(7,8,0,37,100);
        for(int i=0;i<7;i++)u.advanceGeneration();
        assertTrue(u.saveHeuristicAged(7,3,0,38,0));u.advanceGeneration();
        assertTrue(u.saveHintAged(7,1,200));assertEquals(200,u.probeHint(7));
        assertTrue(u.probeHeuristic(7,e));assertEquals(7,generation(e));
    }

    @Test void ordinaryProofRawEvaluationAndMateDomainsStaySeparate() {
        var t=new TTable(1);var e=new TTable.TEntry();
        t.saveHeuristicAged(7,8,0,37,100);t.saveHintAged(7,10,200);
        assertFalse(t.saveHeuristicAged(7,12,0,32512,300));
        assertFalse(t.probeHeuristic(7,e));assertFalse(t.probe(7,e));assertEquals(200,t.probeHint(7));
        t.save(7,4,TTable.TYPE_LOWER,42,400);
        assertFalse(t.saveHeuristicAged(7,10,0,99,500));assertFalse(t.saveHintAged(7,11,500));
        assertFalse(t.saveHeuristicAged(7,10,0,-32512,500));
        assertTrue(t.probe(7,e));assertEquals(42,(int)(e.data>>32));assertEquals(400,e.hashMove);
        var raw=new TTable(1);raw.save(7,0,TTable.TYPE_EVAL,-30000,0);
        assertTrue(raw.probeEval(7,e));assertEquals(-30000,e.data);
    }

    @Test void legacyH1HintFreshnessConvertsWithoutRefreshingScoreAge() {
        var t=new TTable(1);var e=new TTable.TEntry();t.saveHeuristic(7,8,0,37,100);
        t.advanceGeneration();assertTrue(t.saveHintAged(7,2,200));
        assertTrue(t.probeHeuristic(7,e));assertEquals(0,generation(e));assertEquals(200,e.hashMove);
        assertFalse(t.saveHintAged(7,1,300));
    }

    @Test void invalidDepthsAndFullGenerationWrapCannotAlias() {
        var t=new TTable(64);var e=new TTable.TEntry();
        assertEquals(2097152,t.entryCapacity());assertEquals(50331648,t.entryPayloadBytes());
        for(int d:new int[]{-1,0,256}) {
            assertFalse(t.saveHeuristicAged(7,d,0,1,100));assertFalse(t.saveHintAged(7,d,100));
        }
        assertFalse(t.saveHeuristicAged(7,1,TTable.TYPE_EVAL,1,100));
        assertTrue(t.saveHeuristicAged(7,255,0,-32511,100));
        for(int i=0;i<256;i++)t.advanceGeneration();
        assertFalse(t.probeHeuristic(7,e));assertEquals(0,t.probeHint(7));
    }

    @Test void eightCollidingWorkersPublishCoherentAgedRecords() throws Exception {
        var t=new TTable(1);var pool=Executors.newFixedThreadPool(8);
        try {
            for(int epoch=0;epoch<2;epoch++) {
                var jobs=new java.util.ArrayList<Future<?>>();
                for(int w=1;w<=8;w++) {
                    final int worker=w;final long key=17+((long)w<<32);
                    jobs.add(pool.submit(()->{
                        var e=new TTable.TEntry();
                        for(int i=0;i<5000;i++) {
                            t.saveHeuristicAged(key,4,TTable.TYPE_LOWER,worker,worker);
                            if(t.probeHeuristic(key,e)) {
                                assertEquals(worker,(int)(e.data>>32));assertEquals(worker,e.hashMove);
                            }
                            assertFalse(t.probe(key,e));t.saveHintAged(key,4,worker);
                            long hint=t.probeHint(key);assertTrue(hint==0 || hint==worker);
                        }
                    }));
                }
                for(var job:jobs)job.get(20,TimeUnit.SECONDS);
                t.advanceGeneration();
            }
        } finally {pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
}
