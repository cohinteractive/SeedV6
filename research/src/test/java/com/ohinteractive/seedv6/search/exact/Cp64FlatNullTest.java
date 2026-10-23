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

@Timeout(240)
class Cp64FlatNullTest {
    static FlatSearchConfig config(){return FlatSearchConfig.candidate().with(VERIFIED_NULL,true);}
    static boolean flag(ExactSearch s,String name) {
        try{return (boolean)Policy4ProductionTest.field(s,name);}catch(Exception e){throw new AssertionError(e);}
    }
    static TTable guarded(ExactSearch[] owner) {
        return new TTable(8){
            @Override public boolean probe(long k,TEntry e){assertFalse(flag(owner[0],"nullProbeActive"));return super.probe(k,e);}
            @Override public long probeHint(long k){assertFalse(flag(owner[0],"nullProbeActive"));assertFalse(flag(owner[0],"nullVerification"));return super.probeHint(k);}
            @Override public boolean saveHint(long k,int d,long m){assertFalse(flag(owner[0],"nullProbeActive"));assertFalse(flag(owner[0],"nullVerification"));return super.saveHint(k,d,m);}
            @Override public void save(long k,int d,int t,int s,long m){fail("Selective ordinary proof store");}
        };
    }
    @Test void observeOnlyDiscardsEveryProbeAndVerifierEffectExceptActualWork() throws Exception {
        var model=ConventionalNnueEngine.load();
        var roots=P3HistoryStudy.roots(Path.of("../docs/research/search/p3-construction-2026-10-20/diagnostic-corpus.tsv"));
        long total=0;
        for(var root:roots.subList(0,4)) {
            var base=ExactSearch.constructionResearch(ExactEvaluator.from(model.evaluation()),new TTable(8),FlatSearchConfig.candidate());
            var diag=new NullMovePolicy.Diagnostics();var holder=new ExactSearch[1];
            var observed=ExactSearch.constructionResearch(ExactEvaluator.from(model.evaluation()),guarded(holder),config()).withFlatNullObservation(true,diag);holder[0]=observed;
            base.beginRequest();observed.beginRequest();
            try {for(int depth=1;depth<=6;depth++) {
                long before=diag.probeNodes+diag.verificationNodes;
                var a=base.search(root.board(),root.history(),depth,()->base.visitedNodes()>=2000000);
                var b=observed.search(root.board(),root.history(),depth,()->observed.visitedNodes()>=2000000);
                assertTrue(a.completed());assertTrue(b.completed());assertEquals(a.score(),b.score(),root.id()+" d"+depth);
                assertArrayEquals(a.principalVariation(),b.principalVariation());
                assertEquals(a.nodes()+diag.probeNodes+diag.verificationNodes-before,b.nodes(),root.id()+" d"+depth);
                assertArrayEquals((int[])Policy4ProductionTest.field(base,"quietHistory"),(int[])Policy4ProductionTest.field(observed,"quietHistory"));
                var x=ConstructionDepthTest.state(base);var y=ConstructionDepthTest.state(observed);
                assertArrayEquals(x.captures,y.captures);assertArrayEquals(x.continuation,y.continuation);
                assertArrayEquals(x.killers,y.killers);assertArrayEquals(x.counters,y.counters);
            }} finally {base.endRequest();observed.endRequest();}
            assertEquals(0,diag.cutoffs);total+=diag.attempts;
        }
        assertTrue(total>0);System.out.println("flat null observe-only isolated probes="+total);
    }
    @Test void acceptedTargetReturnsOnlyThresholdAndIndependentSyntheticThresholdsAgree() throws Exception {
        var model=ConventionalNnueEngine.load();var root=P3HistoryStudy.roots(Path.of("../docs/research/search/p3-construction-2026-10-20/diagnostic-corpus.tsv")).get(0);
        var samples=new ArrayList<Cp64SelectivityLab.Sample>();var predictions=new ArrayList<Integer>();var holder=new ExactSearch[1];
        var diag=new NullMovePolicy.Diagnostics(){
            @Override public void probe(long[] b,int ply,int d,int beta,int raw,int reduction,int prediction,boolean accepted){
                if(samples.size()<12)try {
                    var state=ConstructionDepthTest.state(holder[0]);
                    samples.add(new Cp64SelectivityLab.Sample("null-opportunity",b.clone(),0,Arrays.copyOfRange(state.pathMoves,1,ply+1),ply,d,beta-1,beta,raw,0,0,0));
                    predictions.add(prediction);
                }catch(Exception e){throw new AssertionError(e);}
            }
        };
        var owner=ExactSearch.constructionResearch(ExactEvaluator.from(model.evaluation()),guarded(holder),config()).withFlatNullObservation(false,diag);holder[0]=owner;
        var result=owner.search(root.board(),root.history(),7,()->owner.visitedNodes()>=500000);
        assertTrue(diag.attempts>0);assertTrue(diag.cutoffs>0);Phase2Oracle.legalPv(root.board(),result.principalVariation());
        for(int i=0;i<samples.size();i++) {
            var s=samples.get(i);var c=Cp64SelectivityProbe.context(root,s);var ref=Cp64SelectivityProbe.probe(model,c,s,200000);
            assertEquals(true,ref.get("captured"));
            assertEquals((int)ref.get("prediction")>=s.beta(),predictions.get(i)>=s.beta(),"Independent synthetic threshold");
        }
        var board=Board.startingPosition();long move=ExhaustiveOracle.legalMoves(board)[0];
        var simple=new NullMovePolicy.Diagnostics();owner.withFlatNullObservation(false,simple);
        var cut=owner.searchRootMove(board,GameHistory.initial(board),4,move,1000,1001,true,()->false);
        assertTrue(cut.completed());assertEquals(1000,cut.score());assertEquals(1,cut.principalVariation().length);
        assertEquals(move,cut.principalVariation()[0]);assertEquals(1,simple.cutoffs);
    }
    @Test void accumulatorRebuildAndCancellationWithinBothIsolatedDomainsRestoreOwnership() throws Exception {
        var model=ConventionalNnueEngine.load();var board=Board.startingPosition();var root=new P3HistoryStudy.Root("start",board,GameHistory.initial(board));
        for(boolean composed:new boolean[]{false,true}) {
          var cfg=new ConstructionSearch(config().with(NOMINAL_FUTILITY,composed),1,8,AspirationSearch.Policy.ADAPTIVE16);
          try(var rolling=new SearchDriver(cfg.create(model.evaluation()));var rebuilt=new SearchDriver(cfg.create(SearchEvaluation.conventionalFullRebuild(model.model())))) {
            ConstructionTtStudy.same((SearchDriverOutcome)ConstructionTtAdaptiveStudy.request(rolling,root,8,100000).get("outcome"),
                    (SearchDriverOutcome)ConstructionTtAdaptiveStudy.request(rebuilt,root,8,100000).get("outcome"));
          }
        }
        for(boolean composed:new boolean[]{false,true}) for(String domain:List.of("nullProbeActive","nullVerification")) {
            var stop=new AtomicBoolean();var holder=new ExactSearch[1];var heldHistory=new SearchLineHistory[1];var delegate=ExactEvaluator.from(model.evaluation());
            var e=new ExactEvaluator(){
                public void initialize(long[] b){delegate.initialize(b);}
                public void child(long[] p,long[] c,int ply){delegate.child(p,c,ply);}
                public int evaluate(long[] b,int ply){if(flag(holder[0],domain)) {
                    try{heldHistory[0]=(SearchLineHistory)Policy4ProductionTest.field(holder[0],"history");}catch(Exception e){throw new AssertionError(e);}
                    stop.set(true);
                }return delegate.evaluate(b,ply);}
            };
            var owner=ExactSearch.constructionResearch(e,guarded(holder),config().with(NOMINAL_FUTILITY,composed));holder[0]=owner;
            var r=owner.search(board,GameHistory.initial(board),8,stop::get);assertTrue(stop.get(),domain);assertFalse(r.completed());
            for(String f:List.of("nullProbeActive","nullVerification","constructionNullProbe","constructionNullVerify"))assertFalse(flag(owner,f));
            assertNull(Policy4ProductionTest.field(owner,"history"));assertEquals(1,heldHistory[0].currentOccurrences(board));
            stop.set(false);assertTrue(owner.search(board,2).completed());
        }
    }
    @Test void allWorkerDomainsAndInjectedSyntheticFailureDrainAndReuse() throws Exception {
        var model=ConventionalNnueEngine.load();var board=Board.startingPosition();
        for(int workers=1;workers<=16;workers++)try(var driver=new SearchDriver(new ConstructionSearch(config(),workers,1,AspirationSearch.Policy.ADAPTIVE16).create(model.evaluation()))) {
            var control=SearchControl.controlled(5000,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
            var r=driver.search(new SearchRequest(board,GameHistory.initial(board),40,control));assertEquals(5000,r.nodes());assertEquals(0,driver.activeSearchThreads());
            if(r.lastCompletedResult()!=null)Phase2Oracle.legalPv(board,r.lastCompletedResult().principalVariation());
            driver.newGame();assertTrue(driver.search(new SearchRequest(board,2)).targetDepthCompleted());
        }
        for(boolean composed:new boolean[]{false,true}) {
        var fail=new AtomicBoolean(true);
        try(var owner=ParallelSearch.constructionResearch(4,model.evaluation(),new TTable(1),config().with(NOMINAL_FUTILITY,composed),i->{
            var delegate=ExactEvaluator.from(model.evaluation());return new ExactEvaluator(){
                public void initialize(long[] b){delegate.initialize(b);}
                public void child(long[] p,long[] c,int ply){
                    if(fail.get() && Arrays.equals(Arrays.copyOf(p,4),Arrays.copyOf(c,4)))throw new IllegalStateException("Injected synthetic transition failure");
                    delegate.child(p,c,ply);
                }
                public int evaluate(long[] b,int ply){return delegate.evaluate(b,ply);}
            };
        });var driver=new SearchDriver(new AspirationSearch(owner,AspirationSearch.Policy.ADAPTIVE16))) {
            var control=SearchControl.controlled(1000000,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
            assertThrows(IllegalStateException.class,()->driver.search(new SearchRequest(board,GameHistory.initial(board),9,control)));
            assertEquals(SearchTermination.FAILURE,control.termination());assertEquals(0,driver.activeSearchThreads());
            fail.set(false);driver.newGame();assertTrue(driver.search(new SearchRequest(board,3)).targetDepthCompleted());
        }
        }
        assertEquals(3407871,BaselineEngine.configuration(1,64).mechanisms().flags());
        assertEquals(262143,Cp64SelectivityLab.configuration("baseline",1,64).mechanisms().flags());
        assertThrows(IllegalArgumentException.class,()->FlatSearchConfig.exact().with(VERIFIED_NULL,true));
    }
}
