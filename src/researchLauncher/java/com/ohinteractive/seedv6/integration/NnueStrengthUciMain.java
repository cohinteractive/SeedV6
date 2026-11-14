package com.ohinteractive.seedv6.integration;

import java.nio.file.Path;
import com.ohinteractive.seedv6.book.Book;
import com.ohinteractive.seedv6.core.nnue.conventional.*;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.driver.BaselineEngine;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.ConstructionSearch;
import com.ohinteractive.seedv6.search.manage.*;
import com.ohinteractive.seedv6.search.tablebase.RootTablebase;
import com.ohinteractive.seedv6.uci.*;

/** Evaluator-only experiment. Both sides receive explicit, unchanged CLV_C1.
 * Never calls the evaluator-identity-dependent automatic search selector. */
public final class NnueStrengthUciMain {
    public static void main(String[] args) throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("network.cnnue");
        var model=StrengthNetwork.load(Path.of(args[0]));
        // Every 256-unit model uses the existing production kernel, isolating training changes.
        SearchEvaluation evaluation=model.hidden()==256
                ? SearchEvaluation.conventional(ConventionalNetwork.load(Path.of(args[0]))) : model.evaluation();
        var configuration=new UciConfiguration() {
            ConstructionSearch config(int workers,int hash) {
                return BaselineEngine.configuration(workers,hash,BaselineEngine.SearchProfile.CLV_C1);
            }
            public ClockPolicy clockPolicy(){return ClockPolicy.BOUNDARY2;}
            public SearchLifecycleService service(int workers,int hash) {
                return new SearchLifecycleService(TimeSource.SYSTEM,()->clockPolicy().apply(config(workers,hash).create(evaluation)),
                        RootTablebase.NONE,Book.EMPTY);
            }
            public boolean standalone(){return true;}
            public boolean acceptsWorkers(int n){return n==1;}
            public String name(){return "SeedV6 NNUE strength research";}
            public String identity(int workers){return identity(workers,32);}
            public String identity(int workers,int hash) {
                return "mode=research formulation=CLV_C1 schema="+model.schema()+" hidden="+model.hidden()
                        +" heads="+model.outputBuckets()+" payload="+model.payloadSha256()+" head="+model.headBackend()
                        +" scoreMapping=BT4_Q_LOGIT400_V1 flags="+config(workers,hash).mechanisms().flags()
                        +" configuration="+config(workers,hash).identity()+" clockPolicy="+clockPolicy().identity()
                        +" book=OFF tablebase=OFF";
            }
        };
        try(var engine=new UciEngine(System.in,System.out,System.err,configuration)){engine.run();}
    }
}
