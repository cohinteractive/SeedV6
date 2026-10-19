package com.ohinteractive.seedv6.search.exact;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.*;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.*;
import static com.ohinteractive.seedv6.search.exact.FlatSearchConfig.Feature.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(180)
class ConstructionDepthTest {
    static final ExactEvaluator HCE=(b,p)->Eval.evaluate(b);
    static FlatSearchState state(ExactSearch search) throws Exception {
        return (FlatSearchState)Policy4ProductionTest.field(search,"constructionState");
    }
    @Test void injectedWorkerFailureDrainsCandidateAndAllowsFreshRequest() throws Exception {
        var e=P3InformationBaselineTest.evaluation();var fail=new java.util.concurrent.atomic.AtomicBoolean(true);
        try(var owner=ParallelSearch.constructionResearch(4,e,new TTable(1),FlatSearchConfig.candidate(),index->{
            var delegate=ExactEvaluator.from(e);
            return new ExactEvaluator(){
                public void initialize(long[] b){delegate.initialize(b);}
                public void child(long[] p,long[] c,int ply){delegate.child(p,c,ply);}
                public int evaluate(long[] b,int ply){if(ply>=4 && fail.get())throw new IllegalStateException("injected construction failure");return delegate.evaluate(b,ply);}
            };
        });var driver=new SearchDriver(new AspirationSearch(owner,AspirationSearch.Policy.WIDEN16))) {
            var b=Board.startingPosition();var c=SearchControl.controlled(1000000,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
            assertThrows(IllegalStateException.class,()->driver.search(new SearchRequest(b,GameHistory.initial(b),8,c)));
            assertEquals(SearchTermination.FAILURE,c.termination());assertEquals(0,driver.activeSearchThreads());
            fail.set(false);driver.newGame();assertTrue(driver.search(new SearchRequest(b,3)).targetDepthCompleted());
            var expired=SearchControl.controlled(10000,System.nanoTime()-10000,1,TimeSource.SYSTEM);
            assertNull(driver.search(new SearchRequest(b,GameHistory.initial(b),8,expired)).lastCompletedResult());
            assertEquals(SearchTermination.TIME_LIMIT,expired.termination());assertEquals(0,driver.activeSearchThreads());
        }
    }
    @Test void dependenciesMarginsAndEvaluatorBoundariesAreExplicit() {
        assertThrows(IllegalArgumentException.class,()->FlatSearchConfig.exact().with(IMPROVING,true));
        assertThrows(IllegalArgumentException.class,()->FlatSearchConfig.exact().with(CAUTION,true));
        assertThrows(IllegalArgumentException.class,()->FlatSearchConfig.exact().with(HISTORY_PROTECTION,true));
        assertThrows(IllegalArgumentException.class,()->new FlatSearchConfig(0,0,100));
        assertThrows(IllegalArgumentException.class,()->new ConstructionSearch(FlatSearchConfig.candidate(),1,1,null).create(SearchEvaluation.handcrafted()));
    }
    @Test void historyProtectionHasObservableEffectBeyondItsGuardCounter() throws Exception {
        var root=P3HistoryStudy.roots(java.nio.file.Path.of("../docs/research/search/p3-construction-2026-10-20/diagnostic-corpus.tsv")).get(0);
        var e=P3InformationBaselineTest.evaluation();var config=FlatSearchConfig.candidate();
        var protectedSearch=ExactSearch.constructionResearch(ExactEvaluator.from(e),new TTable(64),config);
        var control=ExactSearch.constructionResearch(ExactEvaluator.from(e),new TTable(64),config.with(HISTORY_PROTECTION,false));
        var a=protectedSearch.search(root.board(),root.history(),6,()->protectedSearch.visitedNodes()>=1_000_000);
        var b=control.search(root.board(),root.history(),6,()->control.visitedNodes()>=1_000_000);
        assertTrue(a.completed());assertTrue(b.completed());
        Phase2Oracle.legalPv(root.board(),a.principalVariation());Phase2Oracle.legalPv(root.board(),b.principalVariation());
        // The existing counter includes guard bypasses before all move-eligibility
        // checks. A changed traversal independently establishes an effective control.
        assertTrue(state(protectedSearch).protectedMoves>0);
        assertEquals(0,state(control).protectedMoves);
        assertNotEquals(a.nodes(),b.nodes());
        System.out.printf("history-protection root=%s enabled nodes=%d score=%d disabled nodes=%d score=%d guardBypasses=%d%n",
                root.id(),a.nodes(),a.score(),b.nodes(),b.score(),state(protectedSearch).protectedMoves);
    }
    @Test void predictionObservationIsNeutralAndUsesRealLegalDomain() throws Exception {
        var e=P3InformationBaselineTest.evaluation();var b=Board.fromFen(SeePruningTest.MIDDLE);
        var config=FlatSearchConfig.candidate();long[] events={0,0};
        var plain=ExactSearch.constructionResearch(ExactEvaluator.from(e),new TTable(1),config);
        var observed=ExactSearch.constructionResearch(ExactEvaluator.from(e),new TTable(1),config)
                .withConstructionTrace((reverse,parent,move,ply,depth,alpha,beta,raw,path)->{
                    assertTrue(ply>0 && depth>=1 && depth<=2);assertEquals(alpha+1,beta);
                    assertFalse(QuiescenceOracle.inCheck(parent));assertTrue(Long.bitCount(parent[0]|parent[1]|parent[2])>6);
                    long[] replay=b;
                    for(int i=1;i<=ply;i++) replay=ExhaustiveOracle.child(replay,path[i]);
                    assertArrayEquals(parent,replay);
                    if(reverse) {events[0]++;assertTrue(raw-config.reverseMargin()*depth>=beta);}
                    else {events[1]++;assertTrue(raw+config.futilityMargin()*depth<=alpha);
                        assertTrue(Arrays.stream(ExhaustiveOracle.legalMoves(parent)).anyMatch(m->m==move));
                        assertFalse(QuiescenceOracle.tactical(parent,move));
                        assertFalse(QuiescenceOracle.inCheck(ExhaustiveOracle.child(parent,move)));
                    }
                });
        var a=plain.search(b,5);var r=observed.search(b,5);
        assertEquals(a.score(),r.score());assertEquals(a.nodes(),r.nodes());assertArrayEquals(a.principalVariation(),r.principalVariation());
        assertTrue(events[0]>0);assertTrue(events[1]>0);assertEquals(plain.constructionCounters(),observed.constructionCounters());
    }

    @Test void extensionsMatchIndependentEnumerationWithOriginalWindowsAndOneSharedCredit() throws Exception {
        long extensions=0;
        for(String fen:List.of(TacticalExtensionTest.FENS[1],CheckExtensionTest.FENS[4],
                CheckExtensionTest.FENS[2],"7k/6Q1/5K2/8/8/8/8/8 b - - 100 1")) {
            var board=Board.fromFen(fen);var game=GameHistory.initial(board);
            for(boolean check:new boolean[]{false,true}) for(boolean recapture:new boolean[]{false,true}) {
                var config=FlatSearchConfig.exact().with(CHECK_EXTENSION,check).with(RECAPTURE_EXTENSION,recapture);
                for(int depth=1;depth<=2;depth++) {
                    int expected=new Oracle(check,recapture).value(board,new SearchLineHistory(game),depth,0,false,0);
                    for(TTable table:new TTable[]{null,new TTable(1)}) {
                        var search=ExactSearch.constructionResearch(HCE,table,config);
                        for(int[] window:new int[][]{{-32769,32769},{expected-1,expected},{expected,expected+1},{-32767,-32766},{32765,32766}}) {
                            var r=search.searchWindow(board,game,depth,window[0],window[1],()->false);
                            assertTrue(r.completed(),fen);
                            if(r.score()<=window[0]) assertTrue(expected<=r.score(),fen+" upper");
                            else if(r.score()>=window[1]) assertTrue(expected>=r.score(),fen+" lower");
                            else assertEquals(expected,r.score(),fen);
                            assertTrue(r.maximumExtensionPlies()<=1);Phase2Oracle.legalPv(board,r.principalVariation());
                            if(check||recapture) extensions+=state(search).extensions;
                        }
                    }
                }
            }
        }
        assertTrue(extensions>0);
    }

    @Test void extensionCreditCannotConsumeOrPublishOrdinaryProof() {
        var board=Board.fromFen(TacticalExtensionTest.FENS[1]);var game=GameHistory.initial(board);
        var table=new TTable(1){@Override public void save(long k,int d,int t,int s,long m){
            if(s!=12345) fail("Extended-policy ordinary proof store");super.save(k,d,t,s,m);
        }};
        var config=FlatSearchConfig.exact().with(RECAPTURE_EXTENSION,true).with(COMPLETED_HINTS,true);
        var search=ExactSearch.constructionResearch(HCE,table,config);search.beginRequest();
        try {
            long key=SearchKey.key(board,SearchKey.rootHistory(board,game));
            table.save(key,2,TTable.TYPE_EXACT,12345,ExhaustiveOracle.legalMoves(board)[0]);
            var r=search.search(board,game,2,()->false);assertTrue(r.completed());assertNotEquals(12345,r.score());
            assertEquals(new Oracle(false,true).value(board,new SearchLineHistory(game),2,0,false,0),r.score());
        } finally {search.endRequest();}
    }

    @Test void fullCandidateExecutesConsumersAndRestoresRollingEvaluator() throws Exception {
        var e=P3InformationBaselineTest.evaluation();var rolling=ExactEvaluator.from(e);var fresh=e.newState(1);
        long[] evaluations={0};
        var checked=new ExactEvaluator(){
            public void initialize(long[] b){rolling.initialize(b);}
            public void child(long[] p,long[] c,int ply){rolling.child(p,c,ply);}
            public int evaluate(long[] b,int ply){
                int actual=rolling.evaluate(b,ply);fresh.initialize(b,0);assertEquals(fresh.evaluate(b,0),actual);evaluations[0]++;return actual;
            }
        };
        var board=Board.fromFen(SeePruningTest.MIDDLE);var original=board.clone();var game=GameHistory.initial(board);
        var table=new TTable(1);var search=ExactSearch.constructionResearch(checked,table,FlatSearchConfig.candidate());
        var r=search.search(board,game,6,()->search.visitedNodes()>2_000_000);
        assertTrue(r.completed());assertArrayEquals(original,board);assertTrue(evaluations[0]>100);
        var s=state(search);
        System.out.printf("reductions=%d verifications=%d full=%d relief=%d extensions=%d reverse=%d futility=%d protected=%d%n",
                s.reductions,s.verifications,s.fullRetries,s.relief,s.extensions,s.reverseCutoffs,s.futilityOmissions,s.protectedMoves);
        assertTrue(s.reductions>0,"reductions");assertTrue(s.verifications>0,"verifications");assertTrue(s.fullRetries>0,"retries");
        assertTrue(s.relief>0,"relief");assertTrue(s.extensions>0,"extensions");assertTrue(s.reverseCutoffs>0,"reverse");assertTrue(s.futilityOmissions>0,"futility");
        // A protected late positive-history move need not occur in this one tree;
        // wider diagnostics report its actual incidence instead of inventing it.
        assertFalse(table.probe(SearchKey.key(board,SearchKey.rootHistory(board,game)),new TTable.TEntry()));
        Phase2Oracle.legalPv(board,r.principalVariation());
    }

    @Test void fullCandidateWorkersArePrivateAndDrainOnLimitsAndReuse() throws Exception {
        var e=P3InformationBaselineTest.evaluation();var board=Board.startingPosition();
        for(int workers:new int[]{1,2,4,16}) {
            var config=FlatSearchConfig.candidate();
            var parallel=ParallelSearch.constructionResearch(workers,e,new TTable(1),config);
            var seen=Collections.newSetFromMap(new IdentityHashMap<>());
            for(Object w:(Object[])Policy4ProductionTest.field(parallel,"workers")) {
                var s=state((ExactSearch)Policy4ProductionTest.field(w,"search"));
                assertTrue(seen.add(s));assertTrue(seen.add(s.continuation));assertTrue(seen.add(s.rawEval));
            }
            try(var driver=new SearchDriver(new AspirationSearch(parallel,AspirationSearch.Policy.WIDEN16))) {
                var c=SearchControl.controlled(5000,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
                var r=driver.search(new SearchRequest(board,GameHistory.initial(board),12,SearchObserver.NONE,c,true));
                assertEquals(5000,r.nodes());assertEquals(r.nodes(),r.diagnostics().totalEnteredNodes());
                assertEquals(0,driver.activeSearchThreads());assertNotNull(r.lastCompletedResult());
                Phase2Oracle.legalPv(board,r.lastCompletedResult().principalVariation());
                driver.newGame();assertTrue(driver.search(new SearchRequest(board,2)).targetDepthCompleted());
            }
        }
    }

    @Test void terminalAndHistoryDomainsPrecedeNewConsumers() throws Exception {
        var e=P3InformationBaselineTest.evaluation();
        for(String fen:List.of("7k/6Q1/5K2/8/8/8/8/8 b - - 100 1","7k/5Q2/6K1/8/8/8/8/8 b - - 100 1",
                Board.FEN_STARTING_POSITION.replace("0 1","100 1"))) {
            var b=Board.fromFen(fen);var s=ExactSearch.constructionResearch(ExactEvaluator.from(e),new TTable(1),FlatSearchConfig.candidate());
            var r=s.search(b,4);var expected=ExactSearch.quiescenceResearch(ExactEvaluator.from(e)).search(b,4);
            assertEquals(expected.score(),r.score());assertEquals(0,state(s).evaluations);assertEquals(0,state(s).extensions);
        }
        var b=Board.startingPosition();var builder=GameHistory.builder(b);
        var s=ExactSearch.constructionResearch(ExactEvaluator.from(e),new TTable(1),FlatSearchConfig.candidate());
        for(int cycle=0;cycle<2;cycle++) {
            for(String uci:List.of("g1f3","g8f6","f3g1","f6g8")) {
                long selected=0;
                for(long move:ExhaustiveOracle.legalMoves(b))
                    if((Board.squareToString(Move.fromSquare(move))+Board.squareToString(Move.toSquare(move))).equals(uci)) selected=move;
                assertNotEquals(0,selected);b=ExhaustiveOracle.child(b,selected);builder.appendPosition(b);
            }
            if(cycle==0) assertTrue(s.search(b,builder.snapshot(),2,()->false).hasMove());
        }
        assertEquals(0,s.search(b,builder.snapshot(),4,()->false).score());assertEquals(0,state(s).evaluations);
    }

    /** Separate exhaustive traversal with unpruned quiescence. No Search policy helpers. */
    static final class Oracle {
        final boolean check,recapture;long nodes;
        Oracle(boolean check,boolean recapture){this.check=check;this.recapture=recapture;}
        int value(long[] b,SearchLineHistory h,int depth,int ply,boolean used,long previous) {
            if(++nodes>1_000_000) throw new AssertionError("Oversized oracle fixture");
            if(depth==0) return new QuiescenceOracle(HCE).score(b,h,0,ply);
            long[] moves=ExhaustiveOracle.legalMoves(b);boolean checked=QuiescenceOracle.inCheck(b);
            if(moves.length==0) return checked?-32768+ply:0;
            if(DrawAdjudicator.adjudicateNonTerminal(b,h)!=DrawAdjudicator.RuleDraw.NONE) return 0;
            int best=-32769;
            for(long move:moves) {
                var child=ExhaustiveOracle.child(b,move);
                boolean retake=previous!=0 && ((previous>>>Board.TARGET_PIECE_SHIFT)&Board.PIECE_BITS)!=0
                        && ((move>>>Board.TARGET_PIECE_SHIFT)&Board.PIECE_BITS)!=0
                        && ((move>>>Board.PROMOTE_PIECE_SHIFT)&Board.PIECE_BITS)==0 && Move.toSquare(move)==Move.toSquare(previous);
                boolean extra=!used && ((check && checked && moves.length==1 && depth<=2)
                        || (recapture && !checked && depth==1 && retake && !QuiescenceOracle.inCheck(child)));
                h.pushRealPosition(child);
                try{best=Math.max(best,-value(child,h,depth-1+(extra?1:0),ply+1,used||extra,move));}
                finally{h.popRealPosition();}
            }
            return best;
        }
    }
}
