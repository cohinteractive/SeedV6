// Same evaluation interposition and worker witnesses as Phase 2 qualification.
import com.google.gson.Gson;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.nnue.conventional.ConventionalNetwork;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.BaselineEngine;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/** Test-only interposition: every real parallel evaluation must equal full rebuild. */
public final class ParallelProductionCheck {
    static final ConcurrentHashMap<String,LongAdder> calls = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<String,LongAdder> rootCalls = new ConcurrentHashMap<>();
    static final LongAdder transfers = new LongAdder();
    static final LongAdder states = new LongAdder();
    static final class Checked implements SearchEvaluation.ResearchCpLifecycle {
        final ConventionalNetwork model;
        final SearchEvaluation.State delegate;
        Checked(ConventionalNetwork model, SearchEvaluation definition, int capacity) {
            this.model=model; delegate=definition.newState(capacity); states.increment();
        }
        public void initialize(long[] board,int ply){delegate.initialize(board,ply);}
        public void child(long[] parent,long[] child,int ply){delegate.child(parent,child,ply);}
        public void initializeFrom(long[] board,int ply,SearchEvaluation.ResearchCpLifecycle source) {
            delegate.initializeFrom(board,ply,((Checked)source).delegate); transfers.increment();
        }
        public double evaluate(long[] board,int ply) {
            int incremental=delegate.evaluate(board,ply), oracle=model.fullRebuild(board);
            if(incremental!=oracle)throw new AssertionError("Parallel incremental mismatch at ply "+ply);
            calls.computeIfAbsent(Thread.currentThread().getName(),k->new LongAdder()).increment();
            rootCalls.computeIfAbsent(Thread.currentThread().getName(),k->new LongAdder()).increment();
            return incremental;
        }
    }
    static void legalPv(long[] root,long[] pv) {
        long[] board=root.clone();
        for(long move:pv) {
            long[] moves=new long[256];
            int count=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,new long[6]);
            boolean found=false;for(int i=0;i<count;i++)found|=moves[i]==move;
            if(!found)throw new AssertionError("Illegal PV move");
            long[] next=new long[6];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],move,next);board=next;
        }
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("network roots.fens workers");
        var model=ConventionalNetwork.load(Path.of(args[0]));var original=SearchEvaluation.conventional(model);
        var checked=SearchEvaluation.researchCentipawnsStateful(capacity->new Checked(model,original,capacity));
        var configuration=BaselineEngine.configuration(Integer.parseInt(args[2]),32,BaselineEngine.SearchProfile.CLV_C1);
        if(configuration.mechanisms().flags()!=3329490943L)throw new AssertionError("Search flags changed");
        var observations=new ArrayList<Map<String,Object>>();
        var roots=Files.readAllLines(Path.of(args[1]));
        if(roots.size()<4)throw new IllegalArgumentException("Four roots required");
        for(int index=0;index<4;index++) {
            rootCalls.clear();
            long[] board=Board.fromFen(roots.get(index)), saved=board.clone();
            try(var search=configuration.create(checked)) {
                long start=System.nanoTime();var control=SearchControl.controlled(20000,start,60_000_000_000L,TimeSource.SYSTEM);
                int completed=0;long qnodes=0;
                for(int depth=1;depth<=40;depth++) {
                    var result=search.search(new SearchRequest(board,GameHistory.initial(board),depth,SearchObserver.NONE,control,true));
                    qnodes+=result.diagnostics().worker().nodes().qNodes();
                    if(!result.completed())break;
                    legalPv(board,result.principalVariation());completed=depth;
                }
                if(control.termination()!=SearchTermination.NODE_LIMIT||control.nodes()!=20000||completed<1)
                    throw new AssertionError("Did not reach bounded real search endpoint: "+control.termination());
                if(search.activeSearchThreads()!=0||!Arrays.equals(board,saved))throw new AssertionError("Search did not drain/preserve root");
                search.newGame();var restart=search.search(new SearchRequest(board,3));
                if(!restart.completed()||search.activeSearchThreads()!=0)throw new AssertionError("Restart failed");
                legalPv(board,restart.principalVariation());
                if(rootCalls.size()<2||qnodes==0)throw new AssertionError("No helper/qsearch activity on root "+index);
                var observedThreads=new TreeMap<String,Long>();rootCalls.forEach((name,n)->observedThreads.put(name,n.sum()));
                observations.add(Map.of("root",index,"nodes",control.nodes(),"qnodes",qnodes,"completedDepth",completed,"restartDepth",restart.depth(),"evaluationsByThread",observedThreads));
            }
        }
        var byThread=new TreeMap<String,Long>();calls.forEach((name,n)->byThread.put(name,n.sum()));
        if(byThread.size()<2)throw new AssertionError("No observed parallel activity");
        System.out.println(new Gson().toJson(Map.of("status","PASS","payload",model.payloadSha256(),
            "configuration",configuration.identity(),"evaluationsByThread",byThread,"states",states.sum(),
            "lifecycleCopies",transfers.sum(),"observations",observations,
            "limits","Bounded actual parallel search with full-rebuild checks. ExactEvaluator uses one absolute-ply stack for main/qsearch; lifecycle copies are not expected. Test wrapper is not timed production inference.")));
    }
}
