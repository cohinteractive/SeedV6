package com.ohinteractive.seedv6.integration;

import java.nio.file.Path;
import java.util.Set;
import com.ohinteractive.seedv6.book.Book;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.exact.*;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.search.tablebase.RootTablebase;
import com.ohinteractive.seedv6.uci.*;

/** Explicit selectivity experiments; ordinary UCI/defaults remain owned by BaselineEngine. */
public final class Cp64SelectivityUciMain {
    public static void main(String[] args) throws Exception {
        if(args.length!=4 || !args[0].equals("--network") || !args[2].equals("--profile")
                || !Set.of("baseline","no-improving","lazy-improving","nominal-futility","reduction-futility","reduction-no-improving").contains(args[3]))
            throw new IllegalArgumentException("--network <frozen CP64> --profile <explicit selectivity research profile>");
        var model=ConventionalNnueEngine.load(Path.of(args[1]));var evaluation=model.evaluation();String profile=args[3];
        int warmupWorkers=Integer.parseInt(System.getProperty("seedv6.selectivity.warmupWorkers","1"));
        try(var warmup=new SearchDriver(Cp64SelectivityLab.configuration(profile,warmupWorkers,64).create(evaluation))) {
            for(int i=0;i<2;i++) {
                var b=Board.startingPosition();var c=SearchControl.controlled(100000,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
                var r=warmup.search(new SearchRequest(b,GameHistory.initial(b),40,c));
                if(r.nodes()!=100000 || warmup.activeSearchThreads()!=0) throw new IllegalStateException("Warmup accounting/drain");
                warmup.newGame();
            }
        }
        var configuration=new UciConfiguration() {
            ConstructionSearch config(int workers,int hash){return Cp64SelectivityLab.configuration(profile,workers,hash);}
            @Override public SearchLifecycleService service(int workers,int hash) {
                return new SearchLifecycleService(TimeSource.SYSTEM,()->config(workers,hash).create(evaluation),RootTablebase.NONE,Book.EMPTY);
            }
            @Override public boolean standalone(){return true;}
            @Override public boolean acceptsWorkers(int count){return count>=1 && count<=16;}
            @Override public String name(){return "SeedV6 CP64 selectivity research "+profile;}
            @Override public String identity(int workers){return identity(workers,64);}
            @Override public String identity(int workers,int hash) {
                return "mode=research experimental=true profile="+profile+" evaluator=CONVENTIONAL_NNUE"
                        +" checkpoint=64 networkSha256="+ConventionalNnueEngine.SHA256
                        +" schema="+model.model().schema()+" heads="+model.model().outputBuckets()
                        +" head="+model.model().headBackend()+" scoreMapping="+ConventionalNnueEngine.SCORE_MAPPING
                        +" flags="+config(workers,hash).mechanisms().flags()+" configuration="+config(workers,hash).identity()
                        +" book=OFF tablebase=OFF warmupNodes=200000 warmupWorkers="+warmupWorkers;
            }
        };
        try(var engine=new UciEngine(System.in,System.out,System.err,configuration)){engine.run();}
    }
    private Cp64SelectivityUciMain() {}
}
