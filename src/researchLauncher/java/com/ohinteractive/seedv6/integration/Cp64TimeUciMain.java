package com.ohinteractive.seedv6.integration;

import java.nio.file.Path;
import java.util.Set;
import com.ohinteractive.seedv6.book.Book;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.exact.Cp64ClockPolicy;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.search.tablebase.RootTablebase;
import com.ohinteractive.seedv6.uci.*;

/** Explicit SR-034 research launcher. Production UCI and BaselineEngine defaults remain unchanged. */
public final class Cp64TimeUciMain {
    static SingleDepthSearch engine(ConventionalNnueEngine model,String profile,int workers,int hash) {
        var owner=BaselineEngine.create(model.evaluation(),workers,hash);
        return switch(profile) {
            case "baseline" -> owner;
            case "half" -> new Cp64ClockPolicy(owner,Cp64ClockPolicy.Policy.HALF);
            case "stable3-half" -> new Cp64ClockPolicy(owner,Cp64ClockPolicy.Policy.STABLE3_HALF);
            default -> throw new IllegalArgumentException("Unknown clock research policy");
        };
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=4 || !args[0].equals("--network") || !args[2].equals("--profile")
                || !Set.of("baseline","half","stable3-half").contains(args[3]))
            throw new IllegalArgumentException("--network <frozen CP64> --profile baseline|half|stable3-half");
        var model=ConventionalNnueEngine.load(Path.of(args[1]));String profile=args[3];
        int workers=Integer.parseInt(System.getProperty("seedv6.time.warmupWorkers","1"));
        try(var warmup=new SearchDriver(engine(model,profile,workers,64))) {
            for(int i=0;i<2;i++) {
                var board=Board.startingPosition();var c=SearchControl.controlled(100000,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
                var r=warmup.search(new SearchRequest(board,GameHistory.initial(board),40,c));
                if(r.nodes()!=100000 || warmup.activeSearchThreads()!=0)throw new IllegalStateException("Warmup accounting/drain");
                warmup.newGame();
            }
        }
        var configuration=new UciConfiguration() {
            @Override public SearchLifecycleService service(int w,int hash) {
                return new SearchLifecycleService(TimeSource.SYSTEM,()->engine(model,profile,w,hash),RootTablebase.NONE,Book.EMPTY);
            }
            @Override public boolean standalone(){return true;}
            @Override public boolean acceptsWorkers(int count){return count>=1 && count<=16;}
            @Override public String name(){return "SeedV6 CP64 time research "+profile;}
            @Override public String identity(int w){return identity(w,64);}
            @Override public String identity(int w,int hash) {
                var config=BaselineEngine.configuration(model.evaluation(),w,hash);
                return "mode=research experimental=true profile="+profile+" evaluator=CONVENTIONAL_NNUE"
                        +" checkpoint=64 networkSha256="+ConventionalNnueEngine.SHA256
                        +" payload="+model.model().payloadSha256()+" schema="+model.model().schema()
                        +" heads="+model.model().outputBuckets()+" head="+model.model().headBackend()
                        +" scoreMapping="+ConventionalNnueEngine.SCORE_MAPPING+" flags="+config.mechanisms().flags()
                        +" configuration="+config.identity()+" clockPolicy="+(profile.equals("baseline")?"factual":profile+"-d6-v1")
                        +" hardBudget=baseline-unchanged book=OFF tablebase=OFF warmupNodes=200000 warmupWorkers="+workers;
            }
        };
        try(var engine=new UciEngine(System.in,System.out,System.err,configuration)){engine.run();}
    }
    private Cp64TimeUciMain() {}
}
