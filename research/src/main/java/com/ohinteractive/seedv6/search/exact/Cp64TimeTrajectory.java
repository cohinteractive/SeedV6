package com.ohinteractive.seedv6.search.exact;

import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;

/** SR-034 observation through existing completed-iteration callbacks only.
 * No clock policy, TT change, root instrumentation or production default change. */
public final class Cp64TimeTrajectory {
    record Sample(IterationSnapshot iteration, long observedNanos) {}
    record Run(SearchDriverOutcome outcome, SearchTermination termination, long elapsedNanos,
               long processCpuNanos, long heapUsed, long gcCollections, long compilationMillis,
               List<Sample> iterations) {}

    static Run run(SearchDriver driver, P3HistoryStudy.Root root, int depth, long nodes, boolean observe) {
        driver.newGame();
        long[] before = root.board().clone();
        var samples = new ArrayList<Sample>();
        var os = (com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
        long cpu = os.getProcessCpuTime(), gc = P3HistoryStudy.gcCount();
        long jit = ManagementFactory.getCompilationMXBean().getTotalCompilationTime();
        long start = System.nanoTime();
        var control = SearchControl.controlled(nodes, start, 60_000_000_000L, TimeSource.SYSTEM);
        SearchObserver observer = observe ? new SearchObserver() {
            @Override public void onIterationCompleted(IterationSnapshot snapshot) {
                samples.add(new Sample(snapshot, System.nanoTime() - start));
            }
        } : SearchObserver.NONE;
        var outcome = driver.search(new SearchRequest(root.board(), root.history(), depth, observer, control, observe));
        long elapsed = System.nanoTime() - start, usedCpu = os.getProcessCpuTime() - cpu;
        if(control.termination() == SearchTermination.TIME_LIMIT) throw new AssertionError("Operational deadline");
        if(driver.activeSearchThreads() != 0 || !Arrays.equals(root.board(), before))
            throw new AssertionError("Worker drain/root restoration");
        if(outcome.nodes() > nodes || outcome.nodes() != control.nodes()) throw new AssertionError("Node accounting");
        if(observe && outcome.nodes() != outcome.diagnostics().totalEnteredNodes())
            throw new AssertionError("Diagnostics accounting");
        if(outcome.lastCompletedResult() != null) Phase2Oracle.legalPv(root.board(), outcome.lastCompletedResult().principalVariation());
        for(int i=0; i<samples.size(); i++) {
            var sample = samples.get(i).iteration();
            if(!sample.result().completed() || sample.depth() != i+1) throw new AssertionError("False completion");
            Phase2Oracle.legalPv(root.board(), sample.principalVariation());
        }
        return new Run(outcome, control.termination(), elapsed, usedCpu,
                ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed(), P3HistoryStudy.gcCount()-gc,
                ManagementFactory.getCompilationMXBean().getTotalCompilationTime()-jit, samples);
    }

    static void same(Run a, Run b) {
        var x = a.outcome(); var y = b.outcome();
        if(x.nodes()!=y.nodes() || x.attemptedDepth()!=y.attemptedDepth()
                || x.iterationIncomplete()!=y.iterationIncomplete() || x.targetDepthCompleted()!=y.targetDepthCompleted()
                || x.terminalRoot()!=y.terminalRoot() || a.termination()!=b.termination())
            throw new AssertionError("Observation changed search scope");
        var p=x.lastCompletedResult();var q=y.lastCompletedResult();
        if(p==null || q==null) { if(p!=q) throw new AssertionError("Observation changed completion");return; }
        if(p.bestMove()!=q.bestMove() || p.hasMove()!=q.hasMove() || p.score()!=q.score() || p.depth()!=q.depth()
                || p.nodes()!=q.nodes() || p.legalRootMoves()!=q.legalRootMoves() || p.selective()!=q.selective()
                || !Arrays.equals(p.principalVariation(),q.principalVariation()))
            throw new AssertionError("Observation changed resolved result");
    }

    /** model corpus NEW-output depth nodeBudget rounds roots; one worker/Hash64. */
    public static void main(String[] args) throws Exception {
        if(args.length!=7) throw new IllegalArgumentException("model corpus NEW-output depth budget rounds roots");
        var model = ConventionalNnueEngine.load(Path.of(args[0]));
        var all = P3HistoryStudy.roots(Path.of(args[1]));
        int depth=Integer.parseInt(args[3]), rounds=Integer.parseInt(args[5]), count=Integer.parseInt(args[6]);
        long budget=Long.parseLong(args[4]);
        if(depth<2 || depth>64 || budget<1 || rounds<1 || count<1 || count>all.size()) throw new IllegalArgumentException("Bounds");
        var config=BaselineEngine.configuration(model.evaluation(),1,64);
        if(config.mechanisms().flags()!=3407871 || config.aspiration()!=AspirationSearch.Policy.ADAPTIVE16)
            throw new AssertionError("Accepted baseline identity");
        var gson=new GsonBuilder().create();
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW);
            var plain=new SearchDriver(config.create(model.evaluation()));
            var observed=new SearchDriver(config.create(model.evaluation()))) {
            out.write(gson.toJson(Map.of("type","identity","arguments",args,"configuration",config.identity(),
                    "networkSha256",ConventionalNnueEngine.SHA256,"payload",model.model().payloadSha256(),
                    "scoreMapping",ConventionalNnueEngine.SCORE_MAPPING,"java",System.getProperty("java.runtime.version"),
                    "jvm",ManagementFactory.getRuntimeMXBean().getInputArguments())));out.newLine();out.flush();
            for(int w=0; w<4; w++) {run(plain,all.get(w%count),40,100000,false);run(observed,all.get(w%count),40,100000,true);}
            for(int round=0;round<rounds;round++) for(int i=0;i<count;i++) {
                var root=all.get(i); Run a,b;
                if((round+i)%2==0) {a=run(plain,root,depth,budget,false);b=run(observed,root,depth,budget,true);}
                else {b=run(observed,root,depth,budget,true);a=run(plain,root,depth,budget,false);}
                same(a,b);
                out.write(gson.toJson(Map.of("type","trajectory","round",round,"id",root.id(),"historySize",root.history().size(),
                        "neutral",true,"plain",a,"observed",b)));out.newLine();out.flush();
            }
            out.write(gson.toJson(Map.of("status","passed","trajectories",rounds*count,"neutralComparisons",rounds*count)));
            out.newLine();
        }
    }
    private Cp64TimeTrajectory() {}
}
