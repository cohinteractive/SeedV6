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

/** Explicit TC-R001 research binding, excluded from production distributions. */
public final class P4TacticalUciMain {
    public static void main(String[] args) throws Exception {
        if(args.length!=4 || !args[0].equals("--network") || !args[2].equals("--profile"))
            throw new IllegalArgumentException("--network <CP64> --profile guided|probcut");
        String profile=args[3];P4TacticalStudy.flags(profile,1);
        String formulation=profile.equals("probcut")?"TC-P1":"HL-G1";
        var model=ConventionalNnueEngine.load(Path.of(args[1]));var clocks=ClockPolicy.configured();
        var configuration=new UciConfiguration() {
            ConstructionSearch config(int workers,int hash) {
                var base=BaselineEngine.legacyConfiguration(workers,hash);
                return new ConstructionSearch(P4TacticalStudy.flags(profile,workers),workers,hash,base.aspiration());
            }
            public ClockPolicy clockPolicy() {return clocks;}
            public SearchLifecycleService service(int workers,int hash) {
                return new SearchLifecycleService(TimeSource.SYSTEM,
                        ()->clocks.apply(config(workers,hash).create(model.evaluation())),RootTablebase.NONE,Book.EMPTY);
            }
            public boolean standalone() {return true;}
            public boolean acceptsWorkers(int n) {return n>=1 && n<=16;}
            public String name() {return "SeedV6 "+formulation+" tactical verification research";}
            public String identity(int workers) {return identity(workers,64);}
            public String identity(int workers,int hash) {
                var version=ApplicationVersion.load();
                return "mode=research experimental=true formulation="+formulation+" profile="+profile+" evaluator=CONVENTIONAL_NNUE"
                        +" checkpoint=64 networkSha256="+ConventionalNnueEngine.SHA256
                        +" schema="+model.model().schema()+" heads="+model.model().outputBuckets()
                        +" head="+model.model().headBackend()+" scoreMapping="+ConventionalNnueEngine.SCORE_MAPPING
                        +" flags="+config(workers,hash).mechanisms().flags()+" configuration="+config(workers,hash).identity()
                        +" learning=request-worker-private-no-seed effort=history64-bounded2"
                        +" tactical="+(profile.equals("probcut")?"qverify-d4to8-margin150-max3-childdminus3":"OFF")
                        +" ttPolicy=production-hints slotBytes=24 clockPolicy="+clocks.identity()+" book=OFF tablebase=OFF"
                        +" build="+version.build()+" source="+version.sourceRevision();
            }
        };
        try(var engine=new UciEngine(System.in,System.out,System.err,configuration)) {engine.run();}
    }
    private P4TacticalUciMain() {}
}
