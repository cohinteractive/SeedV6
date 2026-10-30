package com.ohinteractive.seedv6.search.tt;

import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TTableInformationTest {
    @Test void explicitLayoutAndRawDomainIncludeZeroButNotMate() {
        var old=new TTable(64);var t=new TTable(64,true);var e=new TTable.TEntry();
        assertFalse(old.hasRawEvaluationLane());assertTrue(t.hasRawEvaluationLane());
        assertEquals(2097152,t.entryCapacity());assertEquals(old.entryCapacity(),t.entryCapacity());
        assertEquals(50331648,old.entryPayloadBytes());assertEquals(old.entryPayloadBytes(),t.entryPayloadBytes());
        assertEquals(Integer.MIN_VALUE,t.probeRawEvaluation(0));
        for(int v:new int[]{-32511,-1,0,1,32511}) {
            assertTrue(t.saveRawEvaluation(0,v));assertEquals(v,t.probeRawEvaluation(0));
            assertFalse(t.probe(0,e));assertFalse(t.probeHeuristic(0,e));assertEquals(0,t.probeHint(0));
        }
        for(int v:new int[]{Integer.MIN_VALUE,-32512,32512,Integer.MAX_VALUE})
            assertThrows(IllegalArgumentException.class,()->t.saveRawEvaluation(0,v));
        assertThrows(IllegalStateException.class,()->old.saveRawEvaluation(0,1));
        assertThrows(IllegalArgumentException.class,()->t.save(0,0,TTable.TYPE_EVAL,-30000,0));
        assertTrue(t.probeInformation(0,e));assertEquals(32511,e.rawEvaluation);assertFalse(e.hasOrdinaryScore());
    }

    @Test void rawScoreAndHintHaveIndependentLifetimes() {
        var t=new TTable(1,true);var e=new TTable.TEntry();
        t.saveRawEvaluation(7,-21);t.saveHeuristicAged(7,8,TTable.TYPE_UPPER,37,100);
        assertEquals(-21,t.probeRawEvaluation(7));
        t.advanceGeneration();assertEquals(Integer.MIN_VALUE,t.probeRawEvaluation(7));
        t.saveRawEvaluation(7,22);t.saveHintAged(7,3,200);
        assertTrue(t.probeHeuristic(7,e));assertEquals(0,(e.data>>>10)&255);
        assertEquals(37,(int)(e.data>>32));assertEquals(200,e.hashMove);
        assertEquals(22,t.probeRawEvaluation(7));
        t.saveHeuristicAged(7,12,0,32512,300); // mate invalidation keeps raw, never makes it a score
        assertFalse(t.probeHeuristic(7,e));assertEquals(22,t.probeRawEvaluation(7));
        assertFalse(t.probe(7,e));
    }

    @Test void coherentSnapshotKeepsMeaningsSeparateAndClearsMissScratch() {
        var t=new TTable(1,true);var e=new TTable.TEntry();
        t.saveRawEvaluation(7,0);assertTrue(t.probeInformation(7,e));
        assertEquals(0,e.rawEvaluation);assertFalse(e.hasOrdinaryScore());assertFalse(e.hasHeuristicScore());
        assertFalse(e.hasCompletedHint());
        t.saveHeuristicAged(7,8,TTable.TYPE_LOWER,42,100);assertTrue(t.probeInformation(7,e));
        assertEquals(0,e.rawEvaluation);assertEquals(42,(int)(e.data>>32));assertEquals(100,e.hashMove);
        assertTrue(e.hasHeuristicScore());assertTrue(e.hasCompletedHint());assertFalse(e.hasOrdinaryScore());
        t.advanceGeneration();t.saveRawEvaluation(7,-20);assertTrue(t.probeInformation(7,e));
        assertEquals(-20,e.rawEvaluation);assertEquals(0,(e.data>>>10)&255);
        assertFalse(t.probeInformation(8,e));assertEquals(0,e.data);assertEquals(0,e.hashMove);
        assertEquals(Integer.MIN_VALUE,e.rawEvaluation);assertFalse(e.hasHeuristicScore());
        t.save(7,9,TTable.TYPE_EXACT,99,200);assertTrue(t.probeInformation(7,e));
        assertTrue(e.hasOrdinaryScore());assertFalse(e.hasHeuristicScore());assertEquals(-20,e.rawEvaluation);
    }

    @Test void packedMoveAndRawFieldsPreserveAllSupportedBitsAndRejectOverflowBeforeMutation() {
        var t=new TTable(1,true);var e=new TTable.TEntry();
        t.saveRawEvaluation(7,-32511);t.saveHeuristicAged(7,8,TTable.TYPE_UPPER,99,0xffffffffL);
        assertTrue(t.probeInformation(7,e));assertEquals(0xffffffffL,e.hashMove);assertEquals(-32511,e.rawEvaluation);
        for(int method=0;method<5;method++) {
            final int which=method;
            assertThrows(IllegalArgumentException.class,()->{
                switch(which) {
                    case 0 -> t.save(8,9,0,2,1L<<32);
                    case 1 -> t.saveHeuristic(8,9,0,2,1L<<32);
                    case 2 -> t.saveHeuristicAged(8,9,0,2,1L<<32);
                    case 3 -> t.saveHint(8,9,1L<<32);
                    case 4 -> t.saveHintAged(8,9,1L<<32);
                }
            });
            assertTrue(t.probeInformation(7,e));assertEquals(0xffffffffL,e.hashMove);assertEquals(-32511,e.rawEvaluation);
        }
        // Legacy tables continue to preserve the full long API representation.
        var legacy=new TTable(1);legacy.saveHeuristicAged(7,8,0,99,1L<<50);
        assertTrue(legacy.probeHeuristic(7,e));assertEquals(1L<<50,e.hashMove);
    }

    @Test void everyScoreOrHintCollisionInvalidatesTheRawLane() {
        for(int policy=0;policy<5;policy++) {
            var t=new TTable(1,true);long collision=7+(1L<<32);
            t.saveRawEvaluation(7,123);
            switch(policy) {
                case 0 -> t.save(collision,4,TTable.TYPE_EXACT,71,11);
                case 1 -> t.saveHeuristic(collision,4,TTable.TYPE_UPPER,71,11);
                case 2 -> t.saveHeuristicAged(collision,4,TTable.TYPE_UPPER,71,11);
                case 3 -> t.saveHint(collision,4,11);
                case 4 -> t.saveHintAged(collision,4,11);
            }
            assertEquals(Integer.MIN_VALUE,t.probeRawEvaluation(collision));
            assertEquals(Integer.MIN_VALUE,t.probeRawEvaluation(7));
            assertFalse(t.saveRawEvaluation(7,456));
            assertTrue(t.saveRawEvaluation(collision,-45));assertEquals(-45,t.probeRawEvaluation(collision));
            t.advanceGeneration();t.advanceGeneration();
            assertFalse(t.saveRawEvaluation(7,456)); // even stale search information survives eval pressure
        }
    }

    @Test void rawCollisionsExpiryClearAndWrapCannotReturnStaleData() {
        var t=new TTable(1,true);long collision=7+(1L<<32);
        t.saveRawEvaluation(7,1);t.saveRawEvaluation(collision,2);
        assertEquals(Integer.MIN_VALUE,t.probeRawEvaluation(7));assertEquals(2,t.probeRawEvaluation(collision));
        for(int i=0;i<256;i++)t.advanceGeneration();
        assertEquals(Integer.MIN_VALUE,t.probeRawEvaluation(collision));
        t.saveRawEvaluation(collision,0);t.clear();assertEquals(Integer.MIN_VALUE,t.probeRawEvaluation(collision));
    }

    @Test void collidingWorkersNeverReadAnotherKeysEvaluation() throws Exception {
        var t=new TTable(1,true);var pool=Executors.newFixedThreadPool(8);
        try {
            var tasks=new java.util.ArrayList<Callable<Void>>();
            for(int worker=0;worker<8;worker++) {
                final int value=worker-4;final long key=7+((long)worker<<32);
                tasks.add(()->{
                    for(int n=0;n<10000;n++) {
                        t.saveRawEvaluation(key,value);int actual=t.probeRawEvaluation(key);
                        assertTrue(actual==Integer.MIN_VALUE || actual==value);
                        t.saveHeuristicAged(key,3,TTable.TYPE_UPPER,value,workerMove(value));
                        actual=t.probeRawEvaluation(key);
                        assertTrue(actual==Integer.MIN_VALUE || actual==value);
                        var entry=new TTable.TEntry();
                        if(t.probeInformation(key,entry)) {
                            assertTrue(entry.rawEvaluation==Integer.MIN_VALUE || entry.rawEvaluation==value);
                            if(entry.hasHeuristicScore()) {
                                assertEquals(value,(int)(entry.data>>32));assertEquals(workerMove(value),entry.hashMove);
                            }
                        }
                    }
                    return null;
                });
            }
            for(var f:pool.invokeAll(tasks))f.get();
        } finally {pool.shutdown();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS));}
    }
    private static long workerMove(int value) { return 100+value; }
}
