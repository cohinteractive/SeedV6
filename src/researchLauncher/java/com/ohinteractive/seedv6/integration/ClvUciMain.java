package com.ohinteractive.seedv6.integration;

import java.nio.file.Path;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.book.Book;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.exact.*;
import com.ohinteractive.seedv6.search.manage.*;
import com.ohinteractive.seedv6.search.tablebase.RootTablebase;
import com.ohinteractive.seedv6.uci.*;

/** CLV explicit binding; never included in production distributions. */
public final class ClvUciMain {
    public static void main(String[] args) throws Exception {
        if(args.length!=4 || !args[0].equals("--network") || !args[2].equals("--formulation")
                || !(args[3].equals("CONTROL") || args[3].equals("C1")))
            throw new IllegalArgumentException("--network <CP64> --formulation CONTROL|C1");
        boolean candidate=args[3].equals("C1");var model=ConventionalNnueEngine.load(Path.of(args[1]));
        var configuration=new UciConfiguration() {
            ConstructionSearch config(int workers,int hash) {
                var c=BaselineEngine.configuration(workers,hash,BaselineEngine.SearchProfile.MW_C4);
                return new ConstructionSearch(c.mechanisms().with(FlatSearchConfig.Feature.TT_CRITICAL_RELIEF,candidate),
                        workers,hash,c.aspiration(),c.rootScheduling());
            }
            public ClockPolicy clockPolicy(){return ClockPolicy.BOUNDARY2;}
            public SearchLifecycleService service(int workers,int hash) {
                return new SearchLifecycleService(TimeSource.SYSTEM,()->clockPolicy().apply(config(workers,hash).create(model.evaluation())),
                        RootTablebase.NONE,Book.EMPTY);
            }
            public boolean standalone(){return true;}
            public boolean acceptsWorkers(int n){return n>=1 && n<=16;}
            public String name(){return "SeedV6 CLV-R001 "+args[3];}
            public String identity(int workers){return identity(workers,64);}
            public String identity(int workers,int hash) {
                var version=ApplicationVersion.load();
                return "mode=research experimental=true formulation=CLV-"+args[3]
                        +" evaluator=CONVENTIONAL_NNUE checkpoint=64 networkSha256="+ConventionalNnueEngine.SHA256
                        +" schema="+model.model().schema()+" heads="+model.model().outputBuckets()+" head="+model.model().headBackend()
                        +" scoreMapping="+ConventionalNnueEngine.SCORE_MAPPING+" flags="+config(workers,hash).mechanisms().flags()
                        +" configuration=workers="+workers+";hashMiB="+hash+";aspiration=ADAPTIVE16;parallel="+(workers==1?"OWNER":"EAGER_ROOT")
                        +" clockPolicy="+clockPolicy().identity()+" book=OFF tablebase=OFF coreBuild="+version.build()
                        +" coreSource="+version.sourceRevision()+" researchIdentity=CLV-R001-C1-v1";
            }
        };
        try(var engine=new UciEngine(System.in,System.out,System.err,configuration)){engine.run();}
    }
    private ClvUciMain(){}
}
