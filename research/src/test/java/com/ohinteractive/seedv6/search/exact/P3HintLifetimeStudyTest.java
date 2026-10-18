package com.ohinteractive.seedv6.search.exact;

import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

class P3HintLifetimeStudyTest {
    @Test void reversibleRealHistoryRemainsPartOfHintIdentity() throws Exception {
        var board=Board.startingPosition();var history=GameHistory.builder(board);
        for(String uci:new String[]{"g1f3","g8f6","f3g1","f6g8"}) {
            long move=java.util.Arrays.stream(ExhaustiveOracle.legalMoves(board))
                    .filter(m->Move.coordinate(m).equals(uci)).findFirst().orElseThrow();
            board=ExhaustiveOracle.child(board,move);history.appendPosition(board);
        }
        long full=SearchKey.key(board,SearchKey.rootHistory(board,history.snapshot()));
        long unknown=SearchKey.key(board,SearchKey.rootHistory(board,GameHistory.initial(board)));
        assertNotEquals(full,unknown);
        var table=new TTable(1);long legal=ExhaustiveOracle.legalMoves(board)[0];
        assertTrue(table.saveHint(full,4,legal));assertEquals(legal,table.probeHint(full));assertEquals(0,table.probeHint(unknown));
    }

    @Test void agePressureAndProofCountersDescribeActualTableDecisions() throws Exception {
        var t=new P3HintLifetimeStudy.Observed(1);long key=42,collision=key+t.data.length;
        t.advanceGeneration();assertTrue(t.saveHint(key,3,11));assertEquals(11,t.probeHint(key));
        t.advanceGeneration();assertEquals(11,t.probeHint(key));
        assertTrue(t.saveHint(key,4,12));assertFalse(t.saveHint(collision,3,13));
        assertTrue(t.saveHint(collision,5,13));t.save(collision,2,TTable.TYPE_EXACT,6,14);
        assertFalse(t.saveHint(key,6,15));
        assertEquals(1,t.currentHits);assertEquals(1,t.olderHits);assertEquals(1,t.hitAge[1]);
        assertEquals(1,t.emptyWrites);assertEquals(1,t.sameKeyWrites);assertEquals(1,t.collisionWrites);
        assertEquals(1,t.proofBlocked);assertEquals(1,t.deeperBlocked);
        assertEquals(1L,t.snapshot().get("occupiedProof"));assertEquals(0L,t.snapshot().get("occupiedHints"));
        t.resetCounters();assertEquals(0,t.hits);assertEquals(1L,t.snapshot().get("occupiedProof"));
    }

    @Test void observationPreservesCombinedSearchAcrossRequestsAndRealPrefixes() throws Exception {
        var e=P3InformationBaselineTest.evaluation();var board=Board.startingPosition();var h=GameHistory.builder(board);
        var t=new P3HintLifetimeStudy.Observed(1);
        var observed=P3HintLifetimeStudy.search(e,t,7);var plain=P3HintLifetimeStudy.search(e,new TTable(1),7);
        for(int request=0;request<3;request++) {
            plain.beginRequest();observed.beginRequest();t.resetCounters();
            try {for(int depth=1;depth<=3;depth++) {
                var a=plain.search(board,h.snapshot(),depth,()->false);
                var b=observed.search(board,h.snapshot(),depth,()->false);P3InformationBaseline.same(a,b);
            }}finally {plain.endRequest();observed.endRequest();}
            assertTrue(t.hits>0);assertTrue((long)t.snapshot().get("occupiedHints")>0);
            board=ExhaustiveOracle.child(board,ExhaustiveOracle.legalMoves(board)[0]);h.appendPosition(board);
        }
    }
}
