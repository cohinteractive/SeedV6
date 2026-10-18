package com.ohinteractive.seedv6.integration;

import java.util.Properties;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.driver.SearchDriver;
import com.ohinteractive.seedv6.search.exact.*;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.uci.*;

/** Explicit research launcher; not a production JAR or application entrypoint. */
public final class P3InformationUciMain {
    public static void main(String[] args) throws Exception {
        if(args.length!=0) throw new IllegalArgumentException("Use explicit research properties");
        int policy=Integer.parseInt(System.getProperty("seedv6.uci.informationResearch","0"));
        int workers=Integer.parseInt(System.getProperty("seedv6.uci.warmupWorkers","1"));
        int depth=Integer.parseInt(System.getProperty("seedv6.uci.warmupDepth","0"));
        if(!InformationResearch.accepts(workers,policy) || depth<0 || depth>8)
            throw new IllegalArgumentException("Invalid policy/worker/warmup combination");
        var properties=new Properties();properties.putAll(System.getProperties());
        properties.setProperty("seedv6.uci.warmupDepth","0");
        var loaded=UciEvaluation.load(properties);
        if(loaded.qpolicy()!=-1 || loaded.lmrPolicy()!=-1 || loaded.lmpPolicy()!=-1
                || loaded.reusePolicy()!=-1 || loaded.checkPolicy()!=0 || !loaded.aspirationPolicy().equals("off")
                || loaded.nullPolicy()!=NullMovePolicy.OFF || loaded.internalPolicy()!=InternalSearchPolicy.OFF
                || loaded.concurrencyPolicy()!=ConcurrencyPolicy.OFF || loaded.seePolicy()!=SeePolicy.OFF || loaded.seeExplicit()
                || loaded.futilityPolicy()!=FutilityPolicy.OFF || loaded.feedbackPolicy()!=DepthFeedback.OFF
                || loaded.tacticalPolicy()!=TacticalExtension.OFF || !loaded.evaluation().isBrnPair2())
            throw new IllegalArgumentException("Phase 3 requires the cumulative Pair-2 baseline; incompatible selector");
        if(depth>0) try(var driver=new SearchDriver(InformationResearch.create(loaded.evaluation(),workers,64,policy))) {
            for(int i=0;i<3;i++) {
                driver.newGame();
                if(!driver.search(new SearchRequest(Board.startingPosition(),depth)).targetDepthCompleted())
                    throw new IllegalStateException("Incomplete Phase 3 warmup");
            }
        }
        var configuration=new UciConfiguration() {
            @Override public SearchLifecycleService service(int count,int hashMiB) {
                if(!InformationResearch.accepts(count,policy) || hashMiB!=64)
                    throw new IllegalArgumentException("Unsupported Phase 3 worker/Hash setting");
                return new SearchLifecycleService(TimeSource.SYSTEM,()->InformationResearch.create(loaded.evaluation(),count,hashMiB,policy));
            }
            @Override public boolean acceptsWorkers(int count) {return InformationResearch.accepts(count,policy);}
            @Override public String identity(int count) {
                String id=loaded.identity(count).replace("warmup=off","warmup="+(depth==0?"off":"3x"+depth));
                if(policy!=0) id=id.replace("mode=production","mode=research")+" experimental=true";
                if((policy&1)!=0) id=id.replace("reuse=effort ","reuse=effort_history ");
                return id+" information="+policy+" hints="+((policy&4)!=0?"completed_same_key":"off")
                        +" history="+((policy&1)!=0?"completed_iterations":count>1?"root_seeded_private":"invocation")
                        +" learning="+((policy&2)!=0?"nominal_only":"all_searched");
            }
        };
        new UciEngine(System.in,System.out,System.err,configuration).run();
    }
    private P3InformationUciMain() {}
}
