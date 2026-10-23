package com.ohinteractive.seedv6.search.exact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class ConstructionTtStudyTest {
    @Test void currentCp64ReplayFixtureRetainsLegalCompleteHistories() throws Exception {
        var lines=java.nio.file.Files.readAllLines(java.nio.file.Path.of("../docs/research/search/p3-tt-effectiveness-2026-10-23/lifetime-lines.tsv"));
        assertEquals(4,lines.size());
        for(String line:lines) {
            var roots=ConstructionTtLifetimeStudy.prefixes(line);
            assertTrue(roots.size()>48);
            for(int ply=0;ply<roots.size();ply++) assertEquals(ply+1,roots.get(ply).history().size());
        }
    }

    @Test void observedRequestsMatchActualBaselineAcrossAbortHistoryAndReset() throws Exception {
        var e=BaselineEngine.load().evaluation();
        var table=new ConstructionTtStudy.ObservedTable(64);
        var adapter=ExactSearchAdapter.constructionResearch(e,table,FlatSearchConfig.candidate());
        long[] board=Board.startingPosition(); var history=GameHistory.builder(board);
        try (var plain=new SearchDriver(BaselineEngine.create(e,1,64));
             var observed=new SearchDriver(new AspirationSearch(adapter,AspirationSearch.Policy.WIDEN16))) {
            for (int index=0;index<4;index++) {
                var root=new P3HistoryStudy.Root("test-"+index,board,history.snapshot());
                long budget=index==1?500:100000;
                var a=ConstructionTtStudy.request(plain,root,5,budget);
                var b=ConstructionTtStudy.request(observed,root,5,budget);
                ConstructionTtStudy.same((SearchDriverOutcome)a.get("outcome"),(SearchDriverOutcome)b.get("outcome"));
                assertEquals(0,table.proofStores);assertEquals(0,table.proofHits);
                assertTrue(table.hintProbes>0); assertTrue(table.writes>0);
                board=ExhaustiveOracle.child(board,ExhaustiveOracle.legalMoves(board)[0]);history.appendPosition(board);
                if (index==2) { plain.newGame();observed.newGame(); }
            }
        }
    }

    @Test void observerDistinguishesMissesRefreshPressureProofAndWrap() throws Exception {
        var t=new ConstructionTtStudy.ObservedTable(1);long key=42,collision=key+t.data.length;
        t.advanceGeneration();assertEquals(0,t.probeHint(key));
        assertTrue(t.saveHint(key,3,11));assertEquals(11,t.probeHint(key));
        assertEquals(0,t.probeHint(collision));assertFalse(t.saveHint(collision,2,12));
        assertTrue(t.saveHint(key,3,11));assertTrue(t.saveHint(collision,4,12));
        t.advanceGeneration();assertEquals(12,t.probeHint(collision));
        t.save(collision,4,TTable.TYPE_EXACT,15,12);assertFalse(t.saveHint(key,5,11));
        assertTrue(t.probe(collision,new TTable.TEntry()));
        assertEquals(1,t.missingEmpty);assertEquals(1,t.missingCollision);assertEquals(1,t.sameMoveRefresh);
        assertEquals(1,t.collisionWrites);assertEquals(1,t.deeperBlocked);assertEquals(1,t.proofBlocked);
        assertEquals(1,t.hitAge[0]);assertEquals(1,t.hitAge[1]);
        for (int i=2;i<256;i++) t.advanceGeneration();
        assertEquals(0L,t.snapshot(true).get("occupiedProof"));
        assertFalse(t.probe(collision,new TTable.TEntry()));
    }
}
