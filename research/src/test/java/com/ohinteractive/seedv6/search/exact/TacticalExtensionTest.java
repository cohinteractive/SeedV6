package com.ohinteractive.seedv6.search.exact;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.*;
import com.ohinteractive.seedv6.search.tt.*;

@Timeout(60)
class TacticalExtensionTest {
    static final ExactEvaluator HCE=(b,p)->Eval.evaluate(b);
    static final String[] FENS={
        "7k/P7/8/8/8/8/8/4K3 w - - 0 1",
        "7k/8/8/3pr3/4P3/8/8/K7 w - - 0 1",
        "K7/8/3p4/8/1r6/8/4p1k1/8 w - - 0 12",
        "8/8/8/8/8/8/4Q3/k2K4 b - - 99 5"
    };
    static ExactSearch search(TacticalExtension policy,TTable tt,TacticalExtensionDiagnostics d) {
        return ExactSearch.iterationReuseResearch(HCE,tt,IterationReuse.EFFORT,true,null)
                .withSeeResearch(SeePolicy.QXRAY,null).withTacticalExtension(policy,d);
    }
    @Test void offIsIdenticalAndReferenceCannotBeReconfigured() {
        var b=Board.startingPosition();
        var ordinary=ExactSearch.iterationReuseResearch(HCE,new TTable(1),IterationReuse.EFFORT,true,null).withSeeResearch(SeePolicy.QXRAY,null).search(b,5);
        var off=search(TacticalExtension.OFF,new TTable(1),null).search(b,5);
        assertEquals(ordinary.score(),off.score());assertEquals(ordinary.nodes(),off.nodes());
        assertArrayEquals(ordinary.principalVariation(),off.principalVariation());
        assertThrows(IllegalStateException.class,()->new ExactSearch(HCE).withTacticalExtension(TacticalExtension.UNIQUE,null));
    }
    @Test void independentlyEnumeratedMainHorizonsAndOriginalWindowBounds() {
        for(var policy:TacticalExtension.values())for(String fen:FENS)for(int depth=1;depth<=2;depth++) {
            var board=Board.fromFen(fen);var history=GameHistory.initial(board);
            int expected=new Oracle(policy).value(board,new SearchLineHistory(history),depth,0,false,0);
            for(int[] w:new int[][]{{-32769,32769},{expected-1,expected},{expected,expected+1}}) {
                var r=search(policy,new TTable(1),null).searchWindow(board,history,depth,w[0],w[1],()->false);
                assertTrue(r.completed());
                if(r.score()<=w[0])assertTrue(expected<=r.score(),fen+" upper "+policy);
                else if(r.score()>=w[1])assertTrue(expected>=r.score(),fen+" lower "+policy);
                else assertEquals(expected,r.score(),fen+" exact "+policy);
                assertTrue(r.maximumExtensionPlies()<=1);Phase2Oracle.legalPv(board,r.principalVariation());
            }
        }
    }
    @Test void actualEdgesAreLegalNoncheckingAndCreditResets() {
        for(var policy:List.of(TacticalExtension.RECAPTURE,TacticalExtension.PROMOTION,TacticalExtension.UNIQUE)) {
            var sink=new TacticalExtensionDiagnostics(){
                @Override public void extended(long[] b,long[] c,long m,int ply,int d,int used) {
                    assertArrayEquals(ExhaustiveOracle.child(b,m),c);assertEquals(1,d);assertEquals(1,used);
                    assertFalse(QuiescenceOracle.inCheck(b));assertFalse(QuiescenceOracle.inCheck(c));
                    if(policy==TacticalExtension.UNIQUE)assertEquals(1,ExhaustiveOracle.legalMoves(b).length);
                }
            };
            var b=Board.fromFen(FENS[policy==TacticalExtension.RECAPTURE?1:policy==TacticalExtension.PROMOTION?0:2]);
            int depth=policy==TacticalExtension.RECAPTURE?2:1;
            var engine=search(policy,new TTable(1),sink);var a=engine.search(b,depth);
            assertTrue(a.completed());assertTrue(sink.extensions>0,policy.name());assertTrue(a.selective());
            assertEquals(1,a.maximumExtensionPlies());engine.newGame();var again=engine.search(b,depth);
            assertEquals(a.score(),again.score());assertEquals(a.nodes(),again.nodes());
        }
    }
    @Test void unusedCreditRejectsOrdinaryProof() {
        for(var policy:List.of(TacticalExtension.RECAPTURE,TacticalExtension.PROMOTION,TacticalExtension.UNIQUE)) {
            var b=Board.fromFen(FENS[0]);var history=GameHistory.initial(b);var table=new TTable(1);
            var engine=search(policy,table,null);engine.beginRequest();
            try {
                long key=SearchKey.key(b,SearchKey.rootHistory(b,history));
                table.save(key,1,TTable.TYPE_EXACT,12345,ExhaustiveOracle.legalMoves(b)[0]);
                var r=engine.search(b,1);assertTrue(r.nodes()>1);assertNotEquals(12345,r.score());
                assertEquals(search(policy,null,null).search(b,1).score(),r.score());
            }finally{engine.endRequest();}
        }
    }
    @Test void cancellationAtExtendedEdgeCannotPublishAndReuseIsClean() {
        for(var policy:List.of(TacticalExtension.RECAPTURE,TacticalExtension.PROMOTION,TacticalExtension.UNIQUE)) {
            var stop=new AtomicBoolean();var sink=new TacticalExtensionDiagnostics(){
                @Override public void extended(long[] b,long[] c,long m,int ply,int d,int used){stop.set(true);}
            };
            var b=Board.fromFen(FENS[policy==TacticalExtension.RECAPTURE?1:policy==TacticalExtension.PROMOTION?0:2]);
            int depth=policy==TacticalExtension.RECAPTURE?2:1;
            var original=b.clone();var engine=search(policy,new TTable(1),sink);
            assertFalse(engine.search(b,GameHistory.initial(b),depth,stop::get).completed());assertArrayEquals(original,b);
            engine.withTacticalExtension(policy,null);engine.newGame();
            assertEquals(search(policy,null,null).search(b,depth).score(),engine.search(b,depth).score());
        }
    }
    @Test void restrictedRootIsNotFactualUniqueness() {
        var b=Board.startingPosition();var sink=new TacticalExtensionDiagnostics();var engine=search(TacticalExtension.UNIQUE,new TTable(1),sink);
        engine.searchRootMove(b,GameHistory.initial(b),1,ExhaustiveOracle.legalMoves(b)[0],-32769,32769,false,()->false);
        assertEquals(0,sink.extensions);
    }
    @Test void observedAndPlainPvsRetriesHaveIdenticalContext() {
        var b=Board.fromFen(SeePruningTest.MIDDLE);long scouts=0,retries=0;
        for(var policy:List.of(TacticalExtension.RECAPTURE,TacticalExtension.PROMOTION,TacticalExtension.UNIQUE)) {
            var sink=new TacticalExtensionDiagnostics();var a=search(policy,new TTable(1),sink).search(b,5);
            var plain=search(policy,new TTable(1),null).search(b,5);
            assertEquals(plain.score(),a.score());assertEquals(plain.nodes(),a.nodes());
            assertArrayEquals(plain.principalVariation(),a.principalVariation());
            scouts+=sink.scouts;retries+=sink.fullResearches;
        }
        assertTrue(scouts>0);assertTrue(retries>0);
    }
    /** Independent full legal minimax at shallow sparse fixtures; unpruned qsearch
     * is an independent oracle here (no lossy QXRAY capture in these fixtures). */
    static class Oracle {
        final TacticalExtension policy;long nodes;
        Oracle(TacticalExtension policy){this.policy=policy;}
        int value(long[] board,SearchLineHistory h,int depth,int ply,boolean used,long previous) {
            if(++nodes>500_000)throw new AssertionError("oracle fixture too large");
            if(depth==0)return new QuiescenceOracle(HCE).score(board,h,0,ply);
            var moves=ExhaustiveOracle.legalMoves(board);boolean checked=QuiescenceOracle.inCheck(board);
            if(moves.length==0)return checked?-32768+ply:0;
            if(DrawAdjudicator.adjudicateNonTerminal(board,h)!=DrawAdjudicator.RuleDraw.NONE)return 0;
            int best=-32769;
            for(long move:moves) {
                var child=ExhaustiveOracle.child(board,move);
                boolean promo=((move>>>Board.PROMOTE_PIECE_SHIFT)&Board.PIECE_BITS)!=0;
                boolean takes=((move>>>Board.TARGET_PIECE_SHIFT)&Board.PIECE_BITS)!=0;
                boolean retakes=previous!=0&&takes&&!promo&&((previous>>>Board.TARGET_PIECE_SHIFT)&Board.PIECE_BITS)!=0&&Move.toSquare(move)==Move.toSquare(previous);
                boolean extra=!used&&depth==1&&!checked&&!QuiescenceOracle.inCheck(child)&&switch(policy){case OFF->false;case RECAPTURE->retakes;case PROMOTION->promo;case UNIQUE->moves.length==1;};
                h.pushRealPosition(child);
                try{best=Math.max(best,-value(child,h,depth-1+(extra?1:0),ply+1,used||extra,move));}
                finally{h.popRealPosition();}
            }
            return best;
        }
    }
}
