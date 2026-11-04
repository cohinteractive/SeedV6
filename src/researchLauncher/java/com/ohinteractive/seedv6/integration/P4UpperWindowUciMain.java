package com.ohinteractive.seedv6.integration;

import java.nio.file.Path;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.book.Book;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.exact.*;
import com.ohinteractive.seedv6.search.manage.*;
import com.ohinteractive.seedv6.search.tablebase.RootTablebase;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.uci.*;

/** IC-R001 explicit research composition. Neither selector enters production. */
public final class P4UpperWindowUciMain {
    public static void main(String[] args) throws Exception {
        if(args.length!=4 || !args[0].equals("--network") || !args[2].equals("--formulation")
                || !(args[3].equals("CONTROL") || args[3].equals("UPPER_CARRYOVER")))
            throw new IllegalArgumentException("--network <CP64> --formulation CONTROL|UPPER_CARRYOVER");
        boolean candidate=args[3].equals("UPPER_CARRYOVER");
        var model=ConventionalNnueEngine.load(Path.of(args[1]));
        var configuration=new UciConfiguration() {
            ConstructionSearch config(int workers,int hash) {
                return BaselineEngine.configuration(workers,hash,BaselineEngine.SearchProfile.MW_C4_W4);
            }
            public ClockPolicy clockPolicy(){return ClockPolicy.BOUNDARY2;}
            public SearchLifecycleService service(int workers,int hash) {
                return new SearchLifecycleService(TimeSource.SYSTEM,()->{
                    var c=config(workers,hash);
                    if(!candidate || workers!=4) return clockPolicy().apply(c.create(model.evaluation()));
                    var parallel=ParallelSearch.constructionEagerRootResearch(4,model.evaluation(),new TTable(hash),c.mechanisms());
                    return clockPolicy().apply(new P4UpperWindowSearch(parallel,parallel::searchWindow,null));
                },RootTablebase.NONE,Book.EMPTY);
            }
            public boolean standalone(){return true;}
            public boolean acceptsWorkers(int n){return n>=1 && n<=16;}
            public String name(){return "SeedV6 IC-R001 "+args[3];}
            public String identity(int workers){return identity(workers,64);}
            public String identity(int workers,int hash) {
                var version=ApplicationVersion.load();boolean active=candidate && workers==4;
                return "mode=research experimental=true formulation="+(active?"IC-C2_UPPER_CARRYOVER":"IC_CONTROL")
                        +" evaluator=CONVENTIONAL_NNUE checkpoint=64 networkSha256="+ConventionalNnueEngine.SHA256
                        +" schema="+model.model().schema()+" heads="+model.model().outputBuckets()+" head="+model.model().headBackend()
                        +" scoreMapping="+ConventionalNnueEngine.SCORE_MAPPING+" flags="+config(workers,hash).mechanisms().flags()
                        +" configuration=workers="+workers+";hashMiB="+hash+";aspiration="+(active?"UPPER_CARRYOVER":"ADAPTIVE16")
                        +";parallel="+(workers==1?"OWNER":workers==4?"EAGER_ROOT":"ROOT_REUSE")
                        +" clockPolicy="+clockPolicy().identity()+" book=OFF tablebase=OFF"
                        +" coreBuild="+version.build()+" coreSource="+version.sourceRevision()
                        +" researchIdentity=IC-R001-v1";
            }
        };
        try(var engine=new UciEngine(System.in,System.out,System.err,configuration)){engine.run();}
    }
    private P4UpperWindowUciMain(){}
}
