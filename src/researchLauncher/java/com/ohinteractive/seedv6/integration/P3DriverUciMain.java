package com.ohinteractive.seedv6.integration;

import java.util.Properties;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.exact.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.uci.*;

/** Explicit Workstream 2 research executable; never packaged in a production application. */
public final class P3DriverUciMain {
    static SingleDepthSearch create(SearchEvaluation e,int workers,MtdDriverResearch.Policy policy) {
        var engine=ProductionSearch.create(workers,e,64);
        return engine instanceof ExactSearchAdapter a
                ?new MtdDriverResearch(a.withFlatSearchResearch(true),policy,null)
                :new MtdDriverResearch(((ParallelSearch)engine).withFlatSearchResearch(true),policy,null);
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=0)throw new IllegalArgumentException("Use explicit research properties");
        var policy=MtdDriverResearch.Policy.valueOf(System.getProperty("seedv6.uci.rootDriver","PVS"));
        int workers=Integer.parseInt(System.getProperty("seedv6.uci.warmupWorkers","1"));
        var properties=new Properties();properties.putAll(System.getProperties());
        properties.setProperty("seedv6.uci.warmupDepth","0");
        var loaded=UciEvaluation.load(properties);
        if(!loaded.evaluation().isBrnPair2() || loaded.qpolicy()!=-1 || loaded.lmrPolicy()!=-1 || loaded.lmpPolicy()!=-1
                || loaded.reusePolicy()!=-1 || loaded.checkPolicy()!=0 || !loaded.aspirationPolicy().equals("off")
                || loaded.nullPolicy()!=NullMovePolicy.OFF || loaded.internalPolicy()!=InternalSearchPolicy.OFF
                || loaded.concurrencyPolicy()!=ConcurrencyPolicy.OFF || loaded.seePolicy()!=SeePolicy.OFF || loaded.seeExplicit()
                || loaded.futilityPolicy()!=FutilityPolicy.OFF || loaded.feedbackPolicy()!=DepthFeedback.OFF
                || loaded.tacticalPolicy()!=TacticalExtension.OFF)
            throw new IllegalArgumentException("Root driver requires unchanged pinned Pair-2 baseline");
        try(var driver=new SearchDriver(create(loaded.evaluation(),workers,policy))) {
            var b=Board.startingPosition();var history=GameHistory.initial(b);
            for(int i=0;i<16;i++) {
                driver.newGame();var control=SearchControl.controlled(1_000_000,System.nanoTime(),60_000_000_000L,TimeSource.SYSTEM);
                var r=driver.search(new SearchRequest(b,history,20,control));
                if(r.nodes()!=1_000_000 || r.lastCompletedResult()==null)throw new IllegalStateException("Incomplete warmup");
            }
        }
        var configuration=new UciConfiguration() {
            public SearchLifecycleService service(int count,int hashMiB) {
                if(hashMiB!=64)throw new IllegalArgumentException("Frozen 64 MiB research Hash");
                return new SearchLifecycleService(TimeSource.SYSTEM,()->create(loaded.evaluation(),count,policy));
            }
            public boolean acceptsWorkers(int count){return count>=1 && count<=16;}
            public String identity(int count) {
                return loaded.identity(count).replace("mode=production","mode=research").replace("warmup=off","warmup=16x1000000nodes")
                        +" kernel=flat rootDriver="+policy+" hints=completed_same_key experimental=true";
            }
        };
        new UciEngine(System.in,System.out,System.err,configuration).run();
    }
    private P3DriverUciMain(){}
}
