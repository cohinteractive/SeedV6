package com.ohinteractive.seedv6.search.exact;

import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnpair2.ProductionModel;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;

/** Bounded O2 mechanism and whole-request economics; no games or strength inference.
 * 0 champion, 1 retention, 2 nominal labels, 3 both, 4 observation-only champion. */
public final class P3HistoryStudy {
    record Root(String id,long[] board,GameHistory history) {}
    static List<Root> roots(Path corpus) throws Exception {
        var roots=new ArrayList<Root>();
        for(String line:Files.readAllLines(corpus)) {
            if(line.isBlank() || line.startsWith("#")) continue;
            var f=line.split("\t",-1);long[] board=Board.fromFen(f[1]);
            var builder=GameHistory.builder(board);
            if(f.length>2 && !f[2].isBlank()) for(String uci:f[2].split(" ")) {
                long move=Arrays.stream(ExhaustiveOracle.legalMoves(board))
                        .filter(m->Move.coordinate(m).equals(uci)).findFirst().orElseThrow();
                board=ExhaustiveOracle.child(board,move);builder.appendPosition(board);
            }
            roots.add(new Root(f[0],board,builder.snapshot()));
        }
        return roots;
    }
    static boolean retain(int p) {return p==1 || p==3;}
    static boolean nominal(int p) {return p==2 || p==3;}
    static SingleDepthSearch engine(SearchEvaluation e,int p) {
        return p==0?ProductionSearch.create(1,e):ExactSearchAdapter.historyResearch(e,retain(p),nominal(p),null);
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=4) throw new IllegalArgumentException("model corpus.tsv new-output.jsonl observe|cost");
        var e=SearchEvaluation.brnPair2(ProductionModel.load(Path.of(args[0])));
        var roots=roots(Path.of(args[1]));var gson=new GsonBuilder().create();
        boolean observe=args[3].equals("observe");
        if(!observe && !args[3].equals("cost")) throw new IllegalArgumentException("Unknown mode");
        int observations=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            if(observe) {
                for(var root:roots) for(int p=0;p<=4;p++) {
                    var d=new HistoryLearning.Diagnostics();var t=new P3InformationBaseline.ObservedTable(64);
                    var a=new P3InformationBaseline.Admissions(e);
                    var search=P3InformationBaseline.engine(a,t,null);
                    if(p!=0) search.withHistoryResearch(retain(p),nominal(p),d);
                    search.beginRequest();
                    try { for(int depth=1;depth<=6;depth++) {
                        t.resetCounters();
                        var result=search.search(root.board,root.history,depth,()->search.visitedNodes()>=250000);
                        Phase2Oracle.legalPv(root.board,result.principalVariation());
                        var row=new LinkedHashMap<String,Object>();
                        row.put("id",root.id);row.put("policy",p);row.put("depth",depth);row.put("outcome",result);
                        row.put("preparedAdmissions",a.nodes);row.put("learning",d);
                        row.put("history",P3InformationBaseline.historySummary(P3InformationBaseline.history(search),null));
                        row.put("tt",Map.of("probes",t.probes,"hits",t.hits,"saveAttempts",t.saves));
                        out.write(gson.toJson(row));out.newLine();out.flush();observations++;
                        if(!result.completed()) break;
                    }} finally {search.endRequest();}
                    System.out.println("completed "+root.id+" policy "+p);
                }
            } else {
                var mx=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
                if(!mx.isThreadAllocatedMemorySupported() || !mx.isCurrentThreadCpuTimeSupported())
                    throw new IllegalStateException("Cost counters unavailable");
                mx.setThreadAllocatedMemoryEnabled(true);mx.setThreadCpuTimeEnabled(true);
                long thread=Thread.currentThread().threadId();
                SearchDriver[] drivers=new SearchDriver[4];
                try {
                    for(int p=0;p<4;p++) {
                        drivers[p]=new SearchDriver(engine(e,p));
                        for(int w=0;w<4;w++) {
                            drivers[p].newGame();long start=System.nanoTime();
                            drivers[p].search(new SearchRequest(Board.startingPosition(),GameHistory.initial(Board.startingPosition()),20,
                                    SearchObserver.NONE,SearchControl.controlled(100000,start,30_000_000_000L,TimeSource.SYSTEM),true));
                        }
                    }
                    for(int round=0;round<2;round++) for(int i=0;i<roots.size();i++) {
                        var root=roots.get(i);
                        for(long budget:new long[]{0,100000}) for(int offset=0;offset<4;offset++) {
                            int p=(i+round+offset)%4;var driver=drivers[p];
                            long reset=System.nanoTime();driver.newGame();reset=System.nanoTime()-reset;
                            long allocated=mx.getThreadAllocatedBytes(thread),cpu=mx.getCurrentThreadCpuTime();
                            long gc=gcCount(),compiled=ManagementFactory.getCompilationMXBean().getTotalCompilationTime();
                            long start=System.nanoTime();
                            var control=SearchControl.controlled(budget==0?2000000:budget,start,30_000_000_000L,TimeSource.SYSTEM);
                            var result=driver.search(new SearchRequest(root.board,root.history,budget==0?6:20,SearchObserver.NONE,control,true));
                            long elapsed=System.nanoTime()-start,usedCpu=mx.getCurrentThreadCpuTime()-cpu;
                            long bytes=mx.getThreadAllocatedBytes(thread)-allocated;
                            if(result.nodes()!=result.diagnostics().totalEnteredNodes()) throw new AssertionError("Accounting mismatch");
                            if(control.termination()==SearchTermination.TIME_LIMIT || (budget!=0 && result.nodes()>budget))
                                throw new AssertionError("Time or node bound exceeded");
                            if(result.lastCompletedResult()!=null) Phase2Oracle.legalPv(root.board,result.lastCompletedResult().principalVariation());
                            var row=new LinkedHashMap<String,Object>();
                            row.put("id",root.id);row.put("policy",p);row.put("round",round);row.put("budget",budget);
                            row.put("elapsedNanos",elapsed);row.put("threadCpuNanos",usedCpu);row.put("threadAllocatedBytes",bytes);
                            row.put("newGameNanos",reset);row.put("processGcCollections",gcCount()-gc);
                            row.put("processCompilationMillis",ManagementFactory.getCompilationMXBean().getTotalCompilationTime()-compiled);
                            row.put("outcome",result);row.put("termination",control.termination());
                            out.write(gson.toJson(row));out.newLine();out.flush();observations++;
                        }
                        System.out.println("completed cost round "+round+" root "+root.id);
                    }
                } finally {for(var driver:drivers) if(driver!=null) driver.close();}
            }
            out.write(gson.toJson(Map.of("status","passed","mode",args[3],"observations",observations)));out.newLine();
        }
    }
    static long gcCount() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b->Math.max(0,b.getCollectionCount())).sum();
    }
}
