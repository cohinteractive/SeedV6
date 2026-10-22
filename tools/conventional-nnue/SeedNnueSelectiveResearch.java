import java.nio.file.Path;
import com.ohinteractive.seedv6.book.Book;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.conventional.ConventionalNetwork;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.ExactSearchAdapter;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.SeePolicy;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.search.tablebase.RootTablebase;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.uci.*;

/** Explicit local research composition. Never included in application distributions. */
public final class SeedNnueSelectiveResearch {
    static ExactSearchAdapter search(SearchEvaluation evaluation,int hash) {
        return ExactSearchAdapter.seeResearch(evaluation,SeePolicy.QXRAY,null,new TTable(hash))
                .withCompletedHints(true,null);
    }
    public static void main(String[] args) throws Exception {
        if(args.length<1||args.length>2)throw new IllegalArgumentException("NETWORK [--verify]");
        if(System.getenv("SEED_NNUE_OPERATION")==null)throw new IllegalStateException("Use nnue-control.cmd run");
        var net=ConventionalNetwork.load(Path.of(args[0]));var evaluation=SearchEvaluation.conventional(net);
        if(args.length==2) {
            if(!args[1].equals("--verify"))throw new IllegalArgumentException("Unknown argument");
            String[] fens={Board.FEN_STARTING_POSITION,
                "r3k2r/pppq1ppp/2n1bn2/3pp3/3PP3/2N1BN2/PPPQ1PPP/R3K2R w KQkq - 0 1",
                "4k3/8/8/3n4/4P3/8/8/4K3 w - - 0 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
                "7k/P7/8/8/8/8/8/7K w - - 0 1"};
            for(String fen:fens) {
                var board=Board.fromFen(fen);
                var request=new SearchRequest(board,GameHistory.initial(board),3,SearchObserver.NONE,
                        SearchControl.unlimited(),true);
                try(var fast=search(evaluation,4);var reference=search(SearchEvaluation.conventionalFullRebuild(net),4)) {
                    var a=fast.search(request);var b=reference.search(request);
                    if(!a.completed()||!b.completed()||a.score()!=b.score()||a.bestMove()!=b.bestMove()||a.nodes()!=b.nodes())
                        throw new AssertionError("Independent full-rebuild search mismatch: "+fen);
                    System.out.println("PASS score/move/nodes depth3 "+a.nodes()+" "+fen);
                }
            }
            return;
        }
        var config=new UciConfiguration() {
            public SearchLifecycleService service(int workers,int hash) {
                if(workers!=1)throw new IllegalArgumentException("Research supports one worker only");
                return new SearchLifecycleService(TimeSource.SYSTEM,()->search(evaluation,hash),RootTablebase.NONE,Book.bundled());
            }
            public boolean standalone(){return true;}
            public boolean acceptsWorkers(int workers){return workers==1;}
            public String name(){return "SeedV6 NNUE SELECTIVE RESEARCH unaccepted";}
            public String identity(int workers){return identity(workers,64);}
            public String identity(int workers,int hash){return "evaluator=CONVENTIONAL_NNUE payload="+net.payloadSha256()
                +" search=SELECTIVE_RESEARCH_QXRAY_LMR_WINDOW_EFFORT_HINTS workers=1 hashMiB="+hash+" unaccepted=true";}
        };
        try(var engine=new UciEngine(System.in,System.out,System.err,config)){engine.run();}
    }
}
