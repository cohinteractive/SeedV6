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

/** Objective-specific opt-in launcher; never an application distribution input. */
public final class P4TtUciMain {
    public static void main(String[] args) throws Exception {
        if(args.length!=4 || !args[0].equals("--network") || !args[2].equals("--profile")
                || !(args[3].equals("h1") || args[3].equals("control")))
            throw new IllegalArgumentException("--network <CP64> --profile <control|h1>");
        var model=ConventionalNnueEngine.load(Path.of(args[1]));
        String profile=args[3];
        var clocks=ClockPolicy.configured();
        var configuration=new UciConfiguration() {
            ConstructionSearch config(int workers,int hash) {
                var base=BaselineEngine.configuration(workers,hash);
                return new ConstructionSearch(base.mechanisms().with(FlatSearchConfig.Feature.HEURISTIC_TT_MEMORY,
                        profile.equals("h1")),workers,hash,base.aspiration());
            }
            public ClockPolicy clockPolicy() { return clocks; }
            public SearchLifecycleService service(int workers,int hash) {
                return new SearchLifecycleService(TimeSource.SYSTEM,
                        ()->clocks.apply(config(workers,hash).create(model.evaluation())),RootTablebase.NONE,Book.EMPTY);
            }
            public boolean standalone() { return true; }
            public boolean acceptsWorkers(int n) { return n>=1 && n<=16; }
            public String name() { return "SeedV6 Phase4 TT research "+profile; }
            public String identity(int workers) { return identity(workers,64); }
            public String identity(int workers,int hash) {
                var version=ApplicationVersion.load();
                return "mode=research experimental=true profile="+profile+" evaluator=CONVENTIONAL_NNUE"
                        +" checkpoint=64 networkSha256="+ConventionalNnueEngine.SHA256
                        +" schema="+model.model().schema()+" heads="+model.model().outputBuckets()
                        +" head="+model.model().headBackend()+" scoreMapping="+ConventionalNnueEngine.SCORE_MAPPING
                        +" flags="+config(workers,hash).mechanisms().flags()+" configuration="+config(workers,hash).identity()
                        +" ttPolicy="+(profile.equals("h1")?"H1-current-sufficient-nonpv-normal":"production-hints")
                        +" clockPolicy="+clocks.identity()+" book=OFF tablebase=OFF"
                        +" build="+version.build()+" source="+version.sourceRevision();
            }
        };
        try(var engine=new UciEngine(System.in,System.out,System.err,configuration)) { engine.run(); }
    }
    private P4TtUciMain() {}
}
