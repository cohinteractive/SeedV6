package com.ohinteractive.seedv6.tools.search;

import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.GarbageCollectorMXBean;
import java.util.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.*;
import com.ohinteractive.seedv6.search.tt.TTable;

/**
 * SR-002 paired same-JVM experiment. No production selection changes.
 * Oracle and PV checks precede all timings. Both workers have independent equal
 * tables/evaluators, clear TT before every invocation and reset quiet history within
 * each invocation. Construction, clearing, diagnostics and checking are outside
 * reported Search time. Allocation/GC/compiler counters surround the invocation.
 */
public final class FlatSearchBenchmark {
    private record Case(String name, long[] board, GameHistory history, int depth, ExactSearchResult expected) {}

    public static void main(String[] args) { run(args, System.out); }

    static void run(String[] args, PrintStream out) {
        int[] depths = {6,7};
        int warmups=12, repetitions=15, mib=64;
        String names="ordering", fen=null;
        boolean named=false;
        boolean localLeaves=true;
        for(String arg : args) {
            if(arg.equals("--frames=both") || arg.equals("--tt=on")) continue;
            if(arg.equals("--flat=frames")) localLeaves=false;
            else if(arg.equals("--flat=local-leaves")) localLeaves=true;
            else if(arg.startsWith("--depths=")) depths=parseDepths(arg.substring(9));
            else if(arg.startsWith("--depth=")) depths=parseDepths(arg.substring(8));
            else if(arg.startsWith("--warmups=")) warmups=Integer.parseInt(arg.substring(10));
            else if(arg.startsWith("--repetitions=")) repetitions=Integer.parseInt(arg.substring(14));
            else if(arg.startsWith("--tt-mib=")) mib=Integer.parseInt(arg.substring(9));
            else if(arg.startsWith("--position=")) { names=arg.substring(11); named=true; }
            else if(arg.startsWith("--fen=")) fen=arg.substring(6);
            else throw new IllegalArgumentException("Unsupported SR-002 argument: "+arg);
        }
        if(warmups<0 || repetitions<1 || mib<1 || mib>256 || (fen!=null && named))
            throw new IllegalArgumentException("Invalid SR-002 configuration.");
        var selected=new ArrayList<ExactSearchHarness.Position>();
        if(fen!=null) selected.add(new ExactSearchHarness.Position("fen",fen));
        else if(names.equals("ordering")) selected.addAll(ExactSearchHarness.orderingPositions());
        else {
            var available=new ArrayList<>(ExactSearchHarness.positions());
            available.addAll(ExactSearchHarness.orderingPositions());
            for(String name : names.split(",",-1)) selected.add(available.stream().filter(p->p.name().equals(name))
                    .findFirst().orElseThrow(()->new IllegalArgumentException("Unknown position: "+name)));
        }
        var recursiveTable=new TTable(mib);
        var flatTable=new TTable(mib);
        var recursive=new ExactSearch(ExactEvaluator.from(SearchEvaluation.handcrafted()),recursiveTable);
        var flat=new FlatExactSearch(ExactEvaluator.from(SearchEvaluation.handcrafted()),flatTable,localLeaves);
        var oracle=new ExactSearch();
        var cases=new ArrayList<Case>();
        out.printf(Locale.ROOT,"SR-002 evaluator=HCE threads=1 java=%s vm=%s os=%s/%s jvm=%s tt_mib=%d warmups=%d repetitions=%d%n",
                System.getProperty("java.version"),System.getProperty("java.vm.name"),System.getProperty("os.name"),
                System.getProperty("os.arch"),ManagementFactory.getRuntimeMXBean().getInputArguments(),mib,warmups,repetitions);
        out.println("policy=production-staged-lazy-PVS control=normal-ExactSearch-constructor candidate=FlatExactSearch");
        out.println("flat_layout="+(localLeaves ? "local-leaves" : "frames"));
        out.println("state=independent-workers/cold-cleared-TT/equal-generations/invocation-reset-history order=alternating");
        // Finish semantic preflight for the ENTIRE requested suite before reporting performance.
        for(int depth : depths) for(var position : selected) {
            long[] b=Board.fromFen(position.fen()); var game=GameHistory.initial(b);
            recursiveTable.clear(); flatTable.clear();
            var r=recursive.search(b,game,depth,ExactSearch.NEVER_CANCELLED);
            var f=flat.search(b,game,depth,ExactSearch.NEVER_CANCELLED);
            ExactSearchHarness.requireRepeatable(r,f);
            var reference=oracle.search(b,game,depth,ExactSearch.NEVER_CANCELLED);
            if(!reference.completed() || reference.score()!=r.score())
                throw new IllegalStateException("Oracle mismatch at "+position.name()+" depth="+depth);
            ExactSearchHarness.verifyBestAndPv(b,game,depth,r,oracle,true);
            cases.add(new Case(position.name(),b,game,depth,r));
            out.printf("equivalence position=%s depth=%d nodes=%d score=%d same_best=true same_pv=true oracle=true every_pv_prefix=true%n",
                    position.name(),depth,r.nodes(),r.score());
        }
        var allocation=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        if(allocation.isThreadAllocatedMemorySupported() && !allocation.isThreadAllocatedMemoryEnabled())
            allocation.setThreadAllocatedMemoryEnabled(true);
        boolean hasAllocation=allocation.isThreadAllocatedMemorySupported();
        long thread=Thread.currentThread().threadId();
        var collectors=ManagementFactory.getGarbageCollectorMXBeans();
        var compiler=ManagementFactory.getCompilationMXBean();
        long totalRecursive=0,totalFlat=0,totalNodes=0;
        double logRatio=0;
        for(var c : cases) {
            long[][] nanos=new long[2][repetitions], bytes=new long[2][repetitions];
            long[] gc=new long[2], compilation=new long[2];
            double[] paired=new double[repetitions];
            for(int round=0;round<warmups+repetitions;round++) {
                for(int turn=0;turn<2;turn++) {
                    int mode=(round+turn)&1;
                    (mode==0 ? recursiveTable : flatTable).clear();
                    boolean measured=round>=warmups;
                    long gcBefore=measured ? collections(collectors) : 0;
                    long compileBefore=measured ? compiler.getTotalCompilationTime() : 0;
                    long allocatedBefore=measured && hasAllocation ? allocation.getThreadAllocatedBytes(thread) : 0;
                    var result=mode==0 ? recursive.search(c.board,c.history,c.depth,ExactSearch.NEVER_CANCELLED)
                            : flat.search(c.board,c.history,c.depth,ExactSearch.NEVER_CANCELLED);
                    long allocatedAfter=measured && hasAllocation ? allocation.getThreadAllocatedBytes(thread) : 0;
                    if(measured) {
                        int sample=round-warmups;
                        nanos[mode][sample]=result.elapsedNanos();
                        bytes[mode][sample]=hasAllocation ? allocatedAfter-allocatedBefore : -1;
                        gc[mode]+=collections(collectors)-gcBefore;
                        compilation[mode]+=compiler.getTotalCompilationTime()-compileBefore;
                    }
                    ExactSearchHarness.requireRepeatable(c.expected,result);
                }
                if(round>=warmups) {
                    int sample=round-warmups;
                    paired[sample]=nanos[1][sample]/(double)nanos[0][sample];
                }
            }
            Arrays.sort(paired);
            for(int mode=0;mode<2;mode++) {
                Arrays.sort(nanos[mode]); Arrays.sort(bytes[mode]);
                long median=nanos[mode][repetitions/2];
                out.printf(Locale.ROOT,"sample position=%s depth=%d mode=%s score=%d best=%s nodes=%d median_ms=%.3f nps=%.0f min_ms=%.3f q1_ms=%.3f q3_ms=%.3f max_ms=%.3f allocated_bytes_median=%d gc=%d compilation_ms=%d%n",
                        c.name,c.depth,mode==0 ? "recursive" : "flat",c.expected.score(),
                        c.expected.hasMove()?Move.coordinate(c.expected.bestMove()):"none",c.expected.nodes(),
                        median/1e6,c.expected.nodes()*1e9/median,nanos[mode][0]/1e6,
                        nanos[mode][repetitions/4]/1e6,nanos[mode][3*repetitions/4]/1e6,nanos[mode][repetitions-1]/1e6,
                        bytes[mode][repetitions/2],gc[mode],compilation[mode]);
            }
            long r=nanos[0][repetitions/2],f=nanos[1][repetitions/2];
            double ratio=f/(double)r;
            totalRecursive+=r; totalFlat+=f; totalNodes+=c.expected.nodes(); logRatio+=Math.log(ratio);
            out.printf(Locale.ROOT,"comparison position=%s depth=%d identical_nodes=%d wall_ratio=%.5f throughput_ratio=%.5f paired_median=%.5f paired_q1=%.5f paired_q3=%.5f%n",
                    c.name,c.depth,c.expected.nodes(),ratio,1/ratio,paired[repetitions/2],
                    paired[repetitions/4],paired[3*repetitions/4]);
        }
        out.printf(Locale.ROOT,"aggregate cases=%d nodes=%d recursive_ms=%.3f flat_ms=%.3f sum_medians_wall_ratio=%.5f throughput_ratio=%.5f geometric_wall_ratio=%.5f%n",
                cases.size(),totalNodes,totalRecursive/1e6,totalFlat/1e6,totalFlat/(double)totalRecursive,
                totalRecursive/(double)totalFlat,Math.exp(logRatio/cases.size()));
    }

    private static int[] parseDepths(String value) {
        int[] depths=Arrays.stream(value.split(",",-1)).mapToInt(Integer::parseInt).toArray();
        for(int depth : depths) if(depth<0 || depth>ExactSearch.MAX_DEPTH) throw new IllegalArgumentException("Invalid depth.");
        return depths;
    }

    private static long collections(List<GarbageCollectorMXBean> collectors) {
        long count=0;
        for(var gc : collectors) count+=Math.max(0,gc.getCollectionCount());
        return count;
    }

    private FlatSearchBenchmark() {}
}
