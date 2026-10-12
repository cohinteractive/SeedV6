package com.ohinteractive.seedv6.search.exact;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(90)
class IterationReuseTest {
    static final ExactEvaluator HCE=(b,p)->Eval.evaluate(b);
    static ExactSearch engine(int policy, boolean selective, IterationReuse.Diagnostics d) {
        return ExactSearch.iterationReuseResearch(HCE,new TTable(1),policy,selective,d);
    }
    static void same(ExactSearchResult a,ExactSearchResult b) {
        assertEquals(a.completed(),b.completed()); assertEquals(a.score(),b.score());
        assertEquals(a.bestMove(),b.bestMove()); assertArrayEquals(a.principalVariation(),b.principalVariation());
        assertEquals(a.nodes(),b.nodes()); assertEquals(a.qnodes(),b.qnodes()); assertEquals(a.selective(),b.selective());
    }
    @Test void observationIsTreeNeutralAndFindsInformationAbsentFromTt() {
        var b=Board.startingPosition();var g=GameHistory.initial(b);var diag=new IterationReuse.Diagnostics();
        var off=engine(0,true,null);var observed=engine(1,true,diag);
        off.beginRequest();observed.beginRequest();long missing=0;
        for(int d=1;d<=5;d++) {same(off.search(b,g,d,()->false),observed.search(b,g,d,()->false));missing+=diag.missing;}
        assertTrue(missing>0);off.endRequest();observed.endRequest();
    }
    @Test void exactQsearchValuesRemainIndependentOfHintsAndHistory() {
        for(String fen:CheckExtensionTest.FENS) {
            var b=Board.fromFen(fen);var g=GameHistory.initial(b);
            for(int p=2;p<=7;p++) {
                var e=engine(p,false,null);e.beginRequest();
                for(int depth=1;depth<=3;depth++) {
                    var ref=ExactSearch.quiescencePvsResearch(HCE,null,0).search(b,g,depth,()->false);
                    var r=e.search(b,g,depth,()->false);assertTrue(r.completed());assertEquals(ref.score(),r.score(),fen+"/"+p);
                    Phase2QuiescenceTest.legalPv(b,r.principalVariation());
                }
                e.endRequest();
            }
        }
    }
    @Test void independentEnumerationChecksMatesRulesAndEpWithRetainedInformation() {
        for(String fen:Arrays.copyOf(CheckExtensionTest.FENS,4)) for(int policy=2;policy<=7;policy++) {
            var b=Board.fromFen(fen);var g=GameHistory.initial(b);var e=engine(policy,false,null);e.beginRequest();
            for(int d=1;d<=2;d++) {
                int expected=new Phase2QuiescenceTest.RestrictedOracle(HCE,0)
                        .score(b,new com.ohinteractive.seedv6.rules.SearchLineHistory(g),d,0,0,-1);
                assertEquals(expected,e.search(b,g,d,()->false).score(),fen+"/"+policy+"/"+d);
            }
            e.endRequest();
        }
    }

    @Test void standaloneInvocationsAndRepeatedRequestsCannotReuseStaleState() {
        var b=Board.startingPosition();var g=GameHistory.initial(b);
        for(int p=2;p<=7;p++) {
            var e=engine(p,true,null);var fresh=engine(0,true,null);
            same(fresh.search(b,3),e.search(b,3));same(fresh.search(b,4),e.search(b,4));
            for(int repeat=0;repeat<2;repeat++) {
                e.newGame();e.beginRequest();
                var control=engine(p,true,null);control.beginRequest();
                for(int d=1;d<=4;d++) same(control.search(b,g,d,()->false),e.search(b,g,d,()->false));
                control.endRequest();e.endRequest();
            }
        }
    }
    @Test void interruptedIterationInvalidatesInformationAndPublishesNoLine() {
        var b=Board.startingPosition();var g=GameHistory.initial(b);
        for(int p=2;p<=7;p++) {
            var diag=new IterationReuse.Diagnostics();var e=engine(p,true,diag);e.beginRequest();
            e.search(b,g,2,()->false);
            var r=e.search(b,g,3,()->e.visitedNodes()>100);assertFalse(r.completed());assertEquals(0,r.principalVariation().length);
            e.search(b,g,4,()->false);assertEquals(0,diag.candidates);e.endRequest();
        }
    }
    @Test void narrowWindowsSkippedDepthAndChangedHistoryInvalidateCompletedHints() {
        var b=Board.startingPosition();var g=GameHistory.initial(b);var diag=new IterationReuse.Diagnostics();
        var e=engine(3,false,diag);e.beginRequest();e.search(b,g,1,()->false);
        e.searchWindow(b,g,2,-1,0,()->false);assertEquals(0,diag.candidates);
        e.search(b,g,3,()->false);assertEquals(0,diag.candidates);
        e.search(b,g,5,()->false);assertEquals(0,diag.candidates);
        var other=GameHistory.builder(g).appendPosition(b).snapshot();
        e.search(b,other,6,()->true);assertEquals(0,diag.candidates);e.endRequest();
    }
    @Test void terminalAndClockBoundaryHaveNoHeuristicOverride() {
        for(String fen:new String[]{"7k/6Q1/5K2/8/8/8/8/8 b - - 0 1","7k/5K2/6Q1/8/8/8/8/8 b - - 0 1",
                "4k3/8/8/8/8/8/8/R3K3 w - - 100 1"}) for(int p=2;p<=7;p++) {
            var b=Board.fromFen(fen);var e=engine(p,true,null);e.beginRequest();
            for(int d=1;d<=3;d++)same(engine(0,true,null).search(b,d),e.search(b,d));e.endRequest();
        }
    }
    @Test void completedRootHintsCannotCreateSelectiveTtProof() {
        var b=Board.startingPosition();var g=GameHistory.initial(b);
        for(int p=2;p<=7;p++) {
            var tt=new TTable(1);var e=ExactSearch.iterationReuseResearch(HCE,tt,p,true,null);e.beginRequest();
            for(int d=1;d<=4;d++) {
                assertTrue(e.search(b,g,d,()->false).selective());
                assertFalse(tt.probe(SearchKey.key(b,SearchKey.rootHistory(b,g)),new TTable.TEntry()));
            }
            e.endRequest();
        }
    }
    @Test void hintOrderingPreservesLegalMembershipAndTtPrecedence() {
        var r=new IterationReuse(IterationReuse.PV,null);var b=Board.startingPosition();var g=GameHistory.initial(b);
        r.prepare(b,g,1,true,-32769,32769);r.complete(1,new long[]{12,13},2);
        r.prepare(b,g,2,true,-32769,32769);long hint=r.hint(0,14);
        long[] moves={11,12,14};int[] keys={2,1,Integer.MAX_VALUE};r.order(moves,3,keys,0,14,hint,0);
        Sort.next(moves,keys,0,0,3);assertEquals(14,moves[0]);Sort.next(moves,keys,0,1,3);assertEquals(12,moves[1]);
        r.descend(0,11);assertEquals(0,r.hint(1,0));r.descend(0,12);assertEquals(13,r.hint(1,0));
        long[] legal={20,21};int[] k={2,1};r.order(legal,2,k,0,0,999,0);assertArrayEquals(new long[]{20,21},legal);
    }
}
