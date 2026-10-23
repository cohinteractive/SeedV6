package com.ohinteractive.seedv6.integration;

import java.nio.file.Path;
import com.ohinteractive.seedv6.book.Book;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.exact.ConstructionSearch;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.search.tablebase.RootTablebase;
import com.ohinteractive.seedv6.uci.*;

/** TT-effectiveness research activation only; production factories are unchanged. */
public final class P3TtUciMain {
    public static void main(String[] args) throws Exception {
        if(args.length!=4 || !args[0].equals("--network") || !args[2].equals("--profile")
                || !(args[3].equals("baseline") || args[3].equals("full") || args[3].equals("adaptive")))
            throw new IllegalArgumentException("--network <frozen CP64> --profile baseline|full|adaptive");
        var model=ConventionalNnueEngine.load(Path.of(args[1]));var evaluation=model.evaluation();
        String profile=args[3];
        var aspiration=profile.equals("baseline")?AspirationSearch.Policy.WIDEN16
                :profile.equals("adaptive")?AspirationSearch.Policy.ADAPTIVE16:null;
        int warmupWorkers=Integer.parseInt(System.getProperty("seedv6.tt.warmupWorkers","1"));
        var warmupConfig=ConstructionSearch.v1(warmupWorkers,64,
                aspiration);
        try(var warmup=new SearchDriver(warmupConfig.create(evaluation))) {
            for(int i=0;i<2;i++) {
                var board=Board.startingPosition();
                var control=SearchControl.controlled(100000,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
                var result=warmup.search(new SearchRequest(board,GameHistory.initial(board),40,control));
                if(result.nodes()!=100000 || warmup.activeSearchThreads()!=0)
                    throw new IllegalStateException("Warmup accounting/drain failed");
                warmup.newGame();
            }
        }
        var configuration=new UciConfiguration() {
            ConstructionSearch config(int workers,int hash) {
                return ConstructionSearch.v1(workers,hash,aspiration);
            }
            @Override public SearchLifecycleService service(int workers,int hash) {
                return new SearchLifecycleService(TimeSource.SYSTEM,
                        ()->profile.equals("baseline")?BaselineEngine.create(evaluation,workers,hash):config(workers,hash).create(evaluation),
                        RootTablebase.NONE,Book.EMPTY);
            }
            @Override public boolean standalone(){return true;}
            @Override public boolean acceptsWorkers(int count){return count>=1 && count<=16;}
            @Override public String name(){return "SeedV6 TT effectiveness research "+profile;}
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
        try(var engine=new UciEngine(System.in,System.out,System.err,configuration)) {engine.run();}
    }
    private P3TtUciMain() {}
}
