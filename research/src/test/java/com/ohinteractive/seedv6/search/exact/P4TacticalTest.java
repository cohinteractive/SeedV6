package com.ohinteractive.seedv6.search.exact;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.*;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;
import static com.ohinteractive.seedv6.search.exact.FlatSearchConfig.Feature.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(180)
class P4TacticalTest {
    static FlatSearchConfig config() {return P4TacticalStudy.flags("probcut",1);}
    static boolean flag(ExactSearch s,String name) throws Exception {
        return (boolean)P4TtFoundationStudy.field(s,name);
    }
    static TTable guarded(ExactSearch[] owner) {
        return new TTable(8) {
            @Override public boolean saveHint(long k,int d,long m) {
                try {assertFalse(flag(owner[0],"tacticalProbeActive"));}
                catch(Exception e){throw new AssertionError(e);}
                return super.saveHint(k,d,m);
            }
            @Override public void save(long k,int d,int t,int s,long m) {fail("Selective ordinary proof store");}
        };
    }

    @Test void discardedVerificationPreservesNominalSearchAndLearningButChargesWork() throws Exception {
        var model=ConventionalNnueEngine.load();
        var roots=P3HistoryStudy.roots(Path.of("../docs/research/search/p3-construction-2026-10-20/diagnostic-corpus.tsv"));
        long candidates=0,verified=0,cutoffs=0;
        for(var root:roots.subList(0,4)) {
            var base=ExactSearch.constructionResearch(ExactEvaluator.from(model.evaluation()),new TTable(8),P4LearningStudy.flags("guided",1));
            var holder=new ExactSearch[1];
            var observed=ExactSearch.constructionResearch(ExactEvaluator.from(model.evaluation()),guarded(holder),config()).withTacticalObservation(true);
            holder[0]=observed;var saved=root.board().clone();
            base.beginRequest();observed.beginRequest();
            try {for(int depth=1;depth<=6;depth++) {
                var a=base.search(root.board(),root.history(),depth,()->base.visitedNodes()>=2000000);
                var b=observed.search(root.board(),root.history(),depth,()->observed.visitedNodes()>=2000000);
                var c=observed.constructionCounters();
                assertTrue(a.completed());assertTrue(b.completed());
                assertEquals(a.score(),b.score(),root.id()+" d"+depth);
                assertArrayEquals(a.principalVariation(),b.principalVariation());
                assertEquals(a.nodes()+c.get("tacticalProbeNodes"),b.nodes());
                assertArrayEquals((int[])P4TtFoundationStudy.field(base,"quietHistory"),(int[])P4TtFoundationStudy.field(observed,"quietHistory"));
                var sa=(FlatSearchState)P4TtFoundationStudy.field(base,"constructionState");
                var sb=(FlatSearchState)P4TtFoundationStudy.field(observed,"constructionState");
                assertArrayEquals(sa.captures,sb.captures);assertArrayEquals(sa.continuation,sb.continuation);
                assertArrayEquals(saved,root.board());Phase2Oracle.legalPv(root.board(),b.principalVariation());
                candidates+=c.get("tacticalCandidates");verified+=c.get("tacticalVerifications");cutoffs+=c.get("tacticalCutoffs");
            }} finally {base.endRequest();observed.endRequest();}
        }
        assertTrue(candidates>0,"No tactical candidate reached");
        assertTrue(verified>0,"No reduced verification reached");
        assertTrue(cutoffs>0,"No selective cutoff witnessed");
    }

    @Test void incrementalEvaluatorMatchesFullRebuildAndLimitsDrainAtW1() throws Exception {
        var model=ConventionalNnueEngine.load();var board=Board.startingPosition();
        var root=new P3HistoryStudy.Root("start",board,GameHistory.initial(board));
        var cfg=new ConstructionSearch(config(),1,8,AspirationSearch.Policy.ADAPTIVE16);
        try(var rolling=new SearchDriver(cfg.create(model.evaluation()));
            var rebuilt=new SearchDriver(cfg.create(SearchEvaluation.conventionalFullRebuild(model.model())))) {
            ConstructionTtStudy.same((SearchDriverOutcome)ConstructionTtAdaptiveStudy.request(rolling,root,9,100000).get("outcome"),
                    (SearchDriverOutcome)ConstructionTtAdaptiveStudy.request(rebuilt,root,9,100000).get("outcome"));
            assertEquals(0,rolling.activeSearchThreads());
            rolling.newGame();assertTrue(rolling.search(new SearchRequest(board,2)).targetDepthCompleted());
        }
        try(var driver=new SearchDriver(cfg.create(model.evaluation()))) {
            var control=SearchControl.controlled(20000,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
            var result=driver.search(new SearchRequest(board,root.history(),40,control));
            assertEquals(20000,result.nodes());assertEquals(0,driver.activeSearchThreads());
            if(result.lastCompletedResult()!=null)Phase2Oracle.legalPv(board,result.lastCompletedResult().principalVariation());
        }
    }

    @Test void cancellationAndEvaluatorFailureInsideRealProbeRestoreHistoryAndReuse() throws Exception {
        var model=ConventionalNnueEngine.load();var board=Board.startingPosition();var saved=board.clone();
        for(boolean throwing:new boolean[]{false,true}) {
            var inject=new AtomicBoolean(true);var stop=new AtomicBoolean();var holder=new ExactSearch[1];
            var heldHistory=new SearchLineHistory[1];var delegate=ExactEvaluator.from(model.evaluation());
            var evaluator=new ExactEvaluator() {
                public void initialize(long[] b){delegate.initialize(b);}
                public void child(long[] p,long[] c,int ply){delegate.child(p,c,ply);}
                public void child(long[] p,long[] c,int ply,int qply){delegate.child(p,c,ply,qply);}
                public void reenter(int ply){delegate.reenter(ply);}
                public int evaluate(long[] b,int ply) {
                    try {if(inject.get() && flag(holder[0],"tacticalProbeActive")) {
                        assertFalse(flag(holder[0],"nullProbeActive"));
                        heldHistory[0]=(SearchLineHistory)P4TtFoundationStudy.field(holder[0],"history");
                        stop.set(true);if(throwing)throw new IllegalArgumentException("Injected tactical evaluator failure");
                    }} catch(ReflectiveOperationException e){throw new AssertionError(e);}
                    catch(Exception e){if(e instanceof RuntimeException r)throw r;throw new AssertionError(e);}
                    return delegate.evaluate(b,ply);
                }
            };
            var owner=ExactSearch.constructionResearch(evaluator,guarded(holder),config());holder[0]=owner;
            if(throwing)assertThrows(IllegalArgumentException.class,()->owner.search(board,GameHistory.initial(board),8,stop::get));
            else assertFalse(owner.search(board,GameHistory.initial(board),8,stop::get).completed());
            assertTrue(stop.get());assertNotNull(heldHistory[0]);
            assertEquals(1,heldHistory[0].size());assertEquals(1,heldHistory[0].currentOccurrences(board));
            assertFalse(flag(owner,"tacticalProbeActive"));assertFalse(flag(owner,"nullVerification"));
            assertNull(P4TtFoundationStudy.field(owner,"history"));assertArrayEquals(saved,board);
            inject.set(false);stop.set(false);assertTrue(owner.search(board,2).completed());
        }
    }

    @Test void adoptedProfileMatchesDefaultAndPreservesHistoricalFlags() {
        assertEquals(1177812991L,config().flags());
        assertEquals(104071167L,P4TacticalStudy.flags("guided",1).flags());
        assertEquals(1177812991L,BaselineEngine.configuration(1,64).mechanisms().flags());
        assertEquals(3407871L,BaselineEngine.legacyConfiguration(1,64).mechanisms().flags());
        assertThrows(IllegalArgumentException.class,()->FlatSearchConfig.exact().with(TACTICAL_VERIFICATION,true));
    }
}
