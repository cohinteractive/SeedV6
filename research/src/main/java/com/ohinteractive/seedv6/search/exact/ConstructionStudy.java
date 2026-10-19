package com.ohinteractive.seedv6.search.exact;

import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnpair2.ProductionModel;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.*;

/** Frozen construction diagnostics and whole-request cost, never an Elo experiment. */
public final class ConstructionStudy {
    record Sample(boolean reverse,long[] board,long move,int ply,int depth,int alpha,int beta,int raw,long[] path) {}
    public static void main(String[] args) throws Exception {
        if(args.length!=10) throw new IllegalArgumentException("model corpus output workers warmups rounds depth budget roots observe|cost");
        var evaluation=SearchEvaluation.brnPair2(ProductionModel.load(Path.of(args[0])));
        var all=P3HistoryStudy.roots(Path.of(args[1]));
        int workers=Integer.parseInt(args[3]),warmups=Integer.parseInt(args[4]),rounds=Integer.parseInt(args[5]);
        int depth=Integer.parseInt(args[6]),count=Integer.parseInt(args[8]);long budget=Long.parseLong(args[7]);
        if(warmups<0 || rounds<1 || count<1 || count>all.size() || budget<1 || depth<1 || depth>ExactSearch.MAX_DEPTH)
            throw new IllegalArgumentException("Invalid experiment bounds");
        var roots=all.subList(0,count);var gson=new GsonBuilder().create();
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","java",System.getProperty("java.runtime.version"),
                    "jvm",ManagementFactory.getRuntimeMXBean().getInputArguments(),"arguments",args,
                    "candidate",new ConstructionSearch(FlatSearchConfig.candidate(),workers,64,AspirationSearch.Policy.WIDEN16).identity())));
            out.newLine();out.flush();
            if(args[9].equals("observe")) {
                if(workers!=1 || warmups!=0 || rounds!=1) throw new IllegalArgumentException("Observe is one deterministic single-worker pass");
                for(var root:roots) {
                    var samples=new ArrayList<Sample>();long[] events={0,0};
                    var table=new P3InformationBaseline.ObservedTable(64);
                    var search=ExactSearch.constructionResearch(ExactEvaluator.from(evaluation),table,FlatSearchConfig.candidate())
                            .withConstructionTrace((reverse,b,move,ply,d,a,z,e,path)->{
                                long n=++events[reverse?0:1];
                                if(n==1 || n==2 || n==32 || n==128)
                                    samples.add(new Sample(reverse,b.clone(),move,ply,d,a,z,e,Arrays.copyOfRange(path,1,ply+1)));
                            });
                    long start=System.nanoTime();
                    var result=search.search(root.board(),root.history(),depth,()->search.visitedNodes()>=budget);
                    Phase2Oracle.legalPv(root.board(),result.principalVariation());
                    var row=new LinkedHashMap<String,Object>();row.put("type","mechanisms");row.put("id",root.id());
                    row.put("outcome",result);row.put("counters",search.constructionCounters());row.put("predictionEvents",events);
                    row.put("elapsedNanos",System.nanoTime()-start);
                    row.put("tt",Map.of("probes",table.probes,"ordinaryHits",table.hits,"proofStores",table.saves));
                    out.write(gson.toJson(row));out.newLine();out.flush();
                    for(var sample:samples) {
                        long[] board=root.board();var builder=GameHistory.builder(root.history());
                        for(long move:sample.path()) {board=ExhaustiveOracle.child(board,move);builder.appendPosition(board);}
                        if(!Arrays.equals(board,sample.board())) throw new AssertionError("Prediction path mismatch");
                        int nominal=sample.depth(),realPly=sample.ply();
                        if(!sample.reverse()) {board=ExhaustiveOracle.child(board,sample.move());builder.appendPosition(board);nominal--;realPly++;}
                        var oracle=ExactSearch.quiescenceResearch(ExactEvaluator.from(evaluation));
                        var reference=oracle.search(board,builder.snapshot(),nominal,()->oracle.visitedNodes()>=100000);
                        int score=reference.score();
                        if(reference.completed()) {
                            if(TranspositionScores.isMateScore(score)) score+=score>0?-realPly:realPly;
                            if(!sample.reverse()) score=-score;
                        }
                        var audit=new LinkedHashMap<String,Object>();audit.put("type","predictionAudit");audit.put("id",root.id());
                        audit.put("sample",sample);audit.put("reference",reference);audit.put("parentRelativeScore",score);
                        audit.put("falseThreshold",reference.completed() && (sample.reverse()?score<sample.beta():score>sample.alpha()));
                        out.write(gson.toJson(audit));out.newLine();out.flush();
                    }
                    // Shallow independent recursive unpruned value: selective disagreement is retained, never forced to agree.
                    var reference=ExactSearch.quiescenceResearch(ExactEvaluator.from(evaluation));
                    var expected=reference.search(root.board(),root.history(),2,()->reference.visitedNodes()>=100000);
                    var shallow=ExactSearch.constructionResearch(ExactEvaluator.from(evaluation),new TTable(1),FlatSearchConfig.candidate())
                            .search(root.board(),root.history(),2,()->false);
                    out.write(gson.toJson(Map.of("type","shallowReference","id",root.id(),"reference",expected,"candidate",shallow)));
                    out.newLine();out.flush();
                }
            } else if(args[9].equals("cost")) {
                var mx=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
                var os=(com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
                if(!mx.isThreadAllocatedMemorySupported() || !mx.isThreadCpuTimeSupported()) throw new IllegalStateException("Cost counters unavailable");
                mx.setThreadAllocatedMemoryEnabled(true);mx.setThreadCpuTimeEnabled(true);
                var configs=List.of(new ConstructionSearch(FlatSearchConfig.policy4(),workers,64,null),
                        new ConstructionSearch(FlatSearchConfig.candidate(),workers,64,null),
                        new ConstructionSearch(FlatSearchConfig.candidate(),workers,64,AspirationSearch.Policy.WIDEN16));
                String[] names={"flat-policy4","rich-full","rich-aspiration"};SearchDriver[] drivers=new SearchDriver[3];
                try {
                    for(int i=0;i<3;i++) {
                        long begin=System.nanoTime();drivers[i]=new SearchDriver(configs.get(i).create(evaluation));
                        out.write(gson.toJson(Map.of("type","construction","profile",names[i],"config",configs.get(i).identity(),
                                "elapsedNanos",System.nanoTime()-begin,"heapUsed",ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed())));
                        out.newLine();
                    }
                    for(int round=-warmups;round<rounds;round++) for(int rootIndex=0;rootIndex<roots.size();rootIndex++) {
                        var root=roots.get(rootIndex);
                        for(int offset=0;offset<3;offset++) {
                            int index=Math.floorMod(round+rootIndex+offset,3);var driver=drivers[index];
                            long reset=System.nanoTime();driver.newGame();reset=System.nanoTime()-reset;
                            long[] ids=mx.getAllThreadIds();long allocated=allocation(mx,ids),cpu=os.getProcessCpuTime(),gc=P3HistoryStudy.gcCount();
                            long jit=ManagementFactory.getCompilationMXBean().getTotalCompilationTime(),begin=System.nanoTime();
                            var control=SearchControl.controlled(budget,begin,30_000_000_000L,TimeSource.SYSTEM);
                            var result=driver.search(new SearchRequest(root.board(),root.history(),depth,SearchObserver.NONE,control,true));
                            long elapsed=System.nanoTime()-begin,usedCpu=os.getProcessCpuTime()-cpu,bytes=allocation(mx,ids)-allocated;
                            if(result.nodes()!=result.diagnostics().totalEnteredNodes() || result.nodes()>budget) throw new AssertionError("All-work accounting");
                            if(control.termination()==SearchTermination.TIME_LIMIT) throw new AssertionError("Safety deadline");
                            if(driver.activeSearchThreads()!=0) throw new AssertionError("Undrained helpers");
                            if(result.lastCompletedResult()!=null) Phase2Oracle.legalPv(root.board(),result.lastCompletedResult().principalVariation());
                            var row=new LinkedHashMap<String,Object>();row.put("type","request");row.put("round",round);row.put("id",root.id());row.put("profile",names[index]);
                            row.put("elapsedNanos",elapsed);row.put("processCpuNanos",usedCpu);row.put("liveThreadAllocatedBytes",bytes);
                            row.put("resetNanos",reset);row.put("gcCollections",P3HistoryStudy.gcCount()-gc);
                            row.put("compilationMillis",ManagementFactory.getCompilationMXBean().getTotalCompilationTime()-jit);
                            row.put("heapUsed",ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
                            row.put("outcome",result);row.put("termination",control.termination());
                            out.write(gson.toJson(row));out.newLine();out.flush();
                        }
                    }
                } finally {for(var driver:drivers) if(driver!=null) driver.close();}
            } else throw new IllegalArgumentException("Unknown study mode");
            out.write(gson.toJson(Map.of("status","passed")));out.newLine();
        }
    }
    private static long allocation(com.sun.management.ThreadMXBean mx,long[] ids) {
        return Arrays.stream(mx.getThreadAllocatedBytes(ids)).filter(n->n>=0).sum();
    }
    private ConstructionStudy() {}
}
