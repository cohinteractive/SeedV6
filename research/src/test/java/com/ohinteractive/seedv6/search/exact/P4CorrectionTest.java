package com.ohinteractive.seedv6.search.exact;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(120)
class P4CorrectionTest {
    static ExactSearch kernel() {return kernel("corrected");}
    static ExactSearch kernel(String profile) {return ExactSearch.constructionResearch(
            ExactEvaluator.from(SearchEvaluation.handcrafted()),new TTable(1),P4CorrectionStudy.flags(profile,1));}
    static FlatSearchState state(ExactSearch k) throws Exception {return (FlatSearchState)P4TtFoundationStudy.field(k,"constructionState");}

    @Test void boundsOnlyLearnInTheirSupportedDirectionAndUpdatesRemainBounded() {
        var h=new CorrectionHistory();var b=Board.startingPosition();
        h.update(b,8,100,101,0,0,100);assertEquals(0,h.apply(b,0)); // upper cannot prove eval too low
        h.update(b,8,-101,-100,0,0,-100);assertEquals(0,h.apply(b,0)); // lower cannot prove eval too high
        h.update(b,8,-101,-100,0,0,-101);assertTrue(h.apply(b,0)<0);
        h.clear();h.update(b,8,100,101,0,0,101);assertTrue(h.apply(b,0)>0);
        for(int i=0;i<1000;i++)h.update(b,8,-1000,1000,0,0,900);
        assertTrue(h.apply(b,0)<=256);assertEquals(ExactSearch.MAX_STATIC_SCORE,h.apply(b,ExactSearch.MAX_STATIC_SCORE));
        h.clear();assertEquals(0,h.apply(b,0));
    }
    @Test void pawnContextGeneralizesAcrossQuietPiecesButSeparatesSideToMove() {
        var a=Board.startingPosition();var b=Board.fromFen("rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R w KQkq - 0 1");
        assertEquals(CorrectionHistory.index(a),CorrectionHistory.index(b));
        var c=Board.fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 0 1");
        assertNotEquals(CorrectionHistory.index(a),CorrectionHistory.index(c));
    }
    @Test void rawValuesSurviveCorrectionsAndRequestAbortResetClearsLearning() throws Exception {
        var k=kernel();var b=Board.startingPosition();var saved=b.clone();var s=state(k);
        k.beginRequest();s.correction.values[CorrectionHistory.index(b)]=1600;
        assertTrue(k.search(b,4).completed());
        assertEquals(s.rawEval[0]+100,s.workingEval[0]);assertArrayEquals(saved,b);
        assertTrue(k.search(b,3).completed());assertNotEquals(0,s.correction.values[CorrectionHistory.index(b)]);
        assertFalse(k.search(b,GameHistory.initial(b),8,()->true).completed());
        for(short v:s.correction.values)assertEquals(0,v);
        assertTrue(k.search(b,3).completed());k.endRequest();
        for(short v:s.correction.values)assertEquals(0,v);
        assertEquals(Integer.MIN_VALUE,s.workingEval[0]);k.newGame();
    }
    @Test void observationsDoNotChangeTraversalAndWorkersDoNotShareCorrection() throws Exception {
        for(String profile:List.of("corrected","corrected-q")) {
        var a=kernel(profile);var b=kernel(profile);var board=Board.startingPosition();
        var d=new CorrectionHistory.Diagnostics();state(a).correction.diagnostics=d;
        a.beginRequest();b.beginRequest();
        for(int depth=1;depth<=5;depth++) {
            var x=a.search(board,depth);var y=b.search(board,depth);
            assertEquals(x.score(),y.score());assertEquals(x.nodes(),y.nodes());assertArrayEquals(x.principalVariation(),y.principalVariation());
        }
        assertTrue(d.reads>0);assertTrue(Arrays.stream(d.updates).sum()>0);assertTrue(d.changed>0);
        assertNotSame(state(a).correction.values,state(b).correction.values);
        a.endRequest();b.endRequest();
        if(profile.equals("corrected-q"))assertTrue(d.qreads>0 && d.qchanged>0);
        }
    }
    @Test void realHistoryTerminalRulesMateAndCancellationRemainValid() {
        for(String profile:List.of("corrected","corrected-q"))for(int workers:new int[]{1,4})try(var driver=new SearchDriver(new ConstructionSearch(
                P4CorrectionStudy.flags(profile,workers),workers,1,AspirationSearch.Policy.ADAPTIVE16).create(SearchEvaluation.handcrafted()))) {
            var b=Board.startingPosition();var saved=b.clone();
            for(int budget:new int[]{1,100,5000,20000}) {
                var c=SearchControl.controlled(budget,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
                var r=driver.search(new SearchRequest(b,GameHistory.initial(b),20,SearchObserver.NONE,c,true));
                assertEquals(budget,r.nodes());assertEquals(r.nodes(),r.diagnostics().totalEnteredNodes());
                assertFalse(r.targetDepthCompleted());assertEquals(0,driver.activeSearchThreads());assertArrayEquals(saved,b);
                if(r.lastCompletedResult()!=null)Phase2Oracle.legalPv(b,r.lastCompletedResult().principalVariation());
                driver.newGame();
            }
            var mate=Board.fromFen("7k/6Q1/5K2/8/8/8/8/8 b - - 0 1");
            assertEquals(-ExactSearch.MATE_SCORE,driver.search(new SearchRequest(mate,3)).lastCompletedResult().score());
            var stale=Board.fromFen("7k/5K2/6Q1/8/8/8/8/8 b - - 0 1");
            assertEquals(0,driver.search(new SearchRequest(stale,3)).lastCompletedResult().score());
        }
    }
    @Test void defaultsAndIndependentControlsStayUnchanged() {
        assertEquals(104071167L,BaselineEngine.configuration(1,64).mechanisms().flags());
        assertEquals(104071167L,P4CorrectionStudy.flags("guided",1).flags());
        assertEquals(238288895L,P4CorrectionStudy.flags("corrected",1).flags());
        assertEquals(506724351L,P4CorrectionStudy.flags("corrected-q",1).flags());
        assertNull(new FlatSearchState(FlatSearchConfig.exact()).correction);
        assertThrows(IllegalArgumentException.class,()->FlatSearchConfig.exact().with(FlatSearchConfig.Feature.EVAL_CORRECTION,true));
        assertThrows(IllegalArgumentException.class,()->P4CorrectionStudy.flags("guided",1).with(FlatSearchConfig.Feature.CORRECTED_QSEARCH,true));
    }
    @Test void discardedNullProbesCannotChangeCorrectionLabelsOrWorkingEvaluation() throws Exception {
        var model=ConventionalNnueEngine.load();
        var roots=P3HistoryStudy.roots(java.nio.file.Path.of("../docs/research/search/p4-tt-memory/h1-diagnostic-roots.tsv"));
        long probes=0;
        for(String profile:List.of("corrected","corrected-q"))for(var root:roots.subList(0,3)) {
            var cfg=P4CorrectionStudy.flags(profile,1);
            var base=ExactSearch.constructionResearch(ExactEvaluator.from(model.evaluation()),new TTable(8),
                    cfg.with(FlatSearchConfig.Feature.VERIFIED_NULL,false));
            var d=new NullMovePolicy.Diagnostics();
            var observed=ExactSearch.constructionResearch(ExactEvaluator.from(model.evaluation()),new TTable(8),cfg).withFlatNullObservation(true,d);
            base.beginRequest();observed.beginRequest();
            try {for(int depth=1;depth<=6;depth++) {
                long before=d.probeNodes+d.verificationNodes;
                var a=base.search(root.board(),root.history(),depth,()->base.visitedNodes()>=2000000);
                var b=observed.search(root.board(),root.history(),depth,()->observed.visitedNodes()>=2000000);
                assertTrue(a.completed());assertTrue(b.completed());assertEquals(a.score(),b.score());
                assertArrayEquals(a.principalVariation(),b.principalVariation());
                assertEquals(a.nodes()+d.probeNodes+d.verificationNodes-before,b.nodes());
                assertArrayEquals(state(base).correction.values,state(observed).correction.values);
            }} finally {base.endRequest();observed.endRequest();}
            probes+=d.attempts;
        }
        assertTrue(probes>0);
    }
    @Test void omissionTraceKeepsRawEvaluatorMeaningWhenWorkingEvaluationIsCorrected() throws Exception {
        var rawZero=new ExactEvaluator() {public int evaluate(long[] board,int ply) {return 0;}};
        var k=ExactSearch.constructionResearch(rawZero,new TTable(1),P4CorrectionStudy.flags("corrected",1)
                .with(FlatSearchConfig.Feature.VERIFIED_NULL,false));
        int[] observations={0};
        k.withSelectivityTrace(new SelectivityTrace() {
            @Override public void omission(long[] board,long move,long[] path,int ply,int depth,int alpha,int beta,
                    int raw,int history,int continuation,int assigned,int margin) {
                observations[0]++;assertEquals(0,raw,"Trace raw must equal the independent constant evaluator");
            }
        });
        var board=Board.startingPosition();k.beginRequest();
        try {
            Arrays.fill(state(k).correction.values,(short)1600);
            assertTrue(k.searchWindow(board,GameHistory.initial(board),6,1000,1001,()->k.visitedNodes()>2000000).completed());
            assertTrue(observations[0]>0,"Must exercise deeper omissions with a nonzero correction");
        } finally {k.endRequest();}
    }
    @Test void correctedStandPatChangesOnlyOptInOrdinaryLeavesAndKeepsRawMainValue() throws Exception {
        var rawZero=new ExactEvaluator() {public int evaluate(long[] board,int ply) {return 0;}};
        var board=Board.startingPosition();
        for(String profile:List.of("corrected","corrected-q")) {
            var k=ExactSearch.constructionResearch(rawZero,new TTable(1),P4CorrectionStudy.flags(profile,1));
            k.beginRequest();
            try {
                Arrays.fill(state(k).correction.values,(short)1600);
                var r=k.search(board,1);assertTrue(r.completed());
                assertEquals(profile.equals("corrected-q")?-100:0,r.score());
                assertEquals(0,state(k).rawEval[0]);assertEquals(100,state(k).workingEval[0]);
                Phase2Oracle.legalPv(board,r.principalVariation());
                var mate=Board.fromFen("7k/6Q1/5K2/8/8/8/8/8 b - - 0 1");
                assertEquals(-ExactSearch.MATE_SCORE,k.search(mate,0).score());
                var stale=Board.fromFen("7k/5K2/6Q1/8/8/8/8/8 b - - 0 1");
                assertEquals(0,k.search(stale,0).score());
            } finally {k.endRequest();}
        }
    }
}
