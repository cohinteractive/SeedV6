package com.ohinteractive.seedv6.search.exact;

import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;

/** Sequential, rotated complete-request diagnostics. Never a strength experiment.
 * Construction/newGame excluded; every search iteration and losing helper included. */
public final class Sr036Cost {
    static class Tracked implements SingleDepthSearch {
        final SingleDepthSearch delegate;
        long[] nodes,qnodes,aborted;
        Tracked(SingleDepthSearch delegate,int workers) {this.delegate=delegate;nodes=new long[workers];qnodes=new long[workers];aborted=new long[workers];}
        public int maxSupportedDepth(){return delegate.maxSupportedDepth();}
        public void beginRequest(){Arrays.fill(nodes,0);Arrays.fill(qnodes,0);Arrays.fill(aborted,0);delegate.beginRequest();}
        public void endRequest(){delegate.endRequest();}
        public void newGame(){delegate.newGame();}
        public void close(){delegate.close();}
        public SearchResult search(SearchRequest request){
            var r=delegate.search(request);
            if(delegate instanceof ParallelSearch p)for(var w:p.workerWork()) {
                nodes[w.worker()]+=w.nodes();qnodes[w.worker()]+=w.qnodes();
                aborted[w.worker()]+=w.abortedNodes();
            }else{nodes[0]+=r.nodes();qnodes[0]+=r.diagnostics().worker().nodes().qNodes();if(!r.completed())aborted[0]+=r.nodes();}
            return r;
        }
    }
    record Position(String id,long[] board,GameHistory history){}
    public static void main(String[] args)throws Exception {
        // store corpus out configurations (e.g. production:1,off:4,cooperative:4)
        var snapshot=CheckpointStore.readSnapshot(Path.of(args[0]),"g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304");
        if(!snapshot.manifest().networkSha256().equals("5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7"))throw new AssertionError("network");
        var eval=snapshot.model().evaluation(NnueScoreMapping.V1);var gson=new GsonBuilder().create();
        var configs=args[3].split(",");var tracked=new Tracked[configs.length];var drivers=new SearchDriver[configs.length];
        var positions=new ArrayList<Position>();
        for(String line:Files.readAllLines(Path.of(args[1]))) {
            if(line.isBlank()||line.startsWith("#"))continue;
            var fields=line.split("\t",-1);var board=Board.fromFen(fields[1]);var builder=GameHistory.builder(board);
            if(fields.length>2&&!fields[2].isBlank())for(String uci:fields[2].split(" ")) {
                long move=Arrays.stream(ExhaustiveOracle.legalMoves(board)).filter(m->Move.coordinate(m).equals(uci)).findFirst().orElseThrow();
                board=ExhaustiveOracle.child(board,move);builder.appendPosition(board);
            }
            positions.add(new Position(fields[0],board,builder.snapshot()));
        }
        var os=(com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
        var allocation=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            for(int i=0;i<configs.length;i++) {
                var fields=configs[i].split(":");int workers=Integer.parseInt(fields[1]);
                SingleDepthSearch search=fields[0].equals("production")?ProductionSearch.create(workers,eval)
                        :new ParallelSearch(workers,eval,ConcurrencyPolicy.valueOf(fields[0].toUpperCase(Locale.ROOT)));
                tracked[i]=new Tracked(search,workers);drivers[i]=new SearchDriver(tracked[i]);
                for(int warm=0;warm<3;warm++) {
                    var b=Board.startingPosition();var c=SearchControl.controlled(500000,0,-1,TimeSource.SYSTEM);
                    drivers[i].newGame();drivers[i].search(new SearchRequest(b,GameHistory.initial(b),10,c));
                    if(c.nodes()!=500000)throw new AssertionError("warmup admissions");
                }
            }
            int observations=0;
            for(int round=0;round<2;round++)for(int root=0;root<positions.size();root++) {
                var position=positions.get(root);
                for(int step=0;step<configs.length;step++) {
                    int index=(step+root+round)%configs.length;
                    for(String budget:new String[]{"depth6","nodes100000","time100"}) {
                        var driver=drivers[index];driver.newGame();
                        long cpu=os.getProcessCpuTime(),gc=gcCount(),jit=ManagementFactory.getCompilationMXBean().getTotalCompilationTime();
                        long allocated=allocated(allocation);long start=System.nanoTime();
                        var control=SearchControl.controlled(budget.equals("nodes100000")?100000:-1,start,
                                budget.equals("time100")?100000000L:30000000000L,TimeSource.SYSTEM);
                        var result=driver.search(new SearchRequest(position.board,position.history,budget.equals("depth6")?6:64,SearchObserver.NONE,control,true));
                        long elapsed=System.nanoTime()-start,usedCpu=os.getProcessCpuTime()-cpu;
                        if(result.nodes()!=control.nodes()||result.nodes()!=result.diagnostics().totalEnteredNodes()
                                ||result.nodes()!=Arrays.stream(tracked[index].nodes).sum())throw new AssertionError("accounting");
                        if(result.lastCompletedResult()==null)throw new AssertionError("no completed depth");
                        Phase2Oracle.legalPv(position.board,result.lastCompletedResult().principalVariation());
                        var row=new LinkedHashMap<String,Object>();
                        row.put("round",round);row.put("id",position.id);row.put("config",configs[index]);row.put("budget",budget);
                        row.put("elapsedNanos",elapsed);row.put("cpuNanos",usedCpu);row.put("allocatedBytes",allocated(allocation)-allocated);
                        row.put("gcCollections",gcCount()-gc);row.put("compilationMillis",ManagementFactory.getCompilationMXBean().getTotalCompilationTime()-jit);
                        row.put("workerNodes",tracked[index].nodes);row.put("workerQnodes",tracked[index].qnodes);row.put("abortedWorkerNodes",tracked[index].aborted);
                        row.put("termination",control.termination());row.put("outcome",result);
                        out.write(gson.toJson(row));out.newLine();out.flush();observations++;
                    }
                }
            }
            out.write(gson.toJson(Map.of("status","passed","observations",observations,"jvm",System.getProperty("java.vm.version"))));out.newLine();
        }finally{for(var d:drivers)if(d!=null)d.close();}
    }
    static long gcCount(){return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b->Math.max(0,b.getCollectionCount())).sum();}
    static long allocated(com.sun.management.ThreadMXBean b){return Arrays.stream(b.getThreadAllocatedBytes(b.getAllThreadIds())).filter(v->v>=0).sum();}
}
