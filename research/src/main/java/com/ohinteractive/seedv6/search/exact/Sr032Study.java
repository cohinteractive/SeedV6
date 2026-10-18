package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.Gson;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;

/** Outcome-blind opportunity counts and bounded full-history witnesses. Instrumented
 * cost is not clean timing. Completed native trees must match the unobserved control. */
public final class Sr032Study {
    static final class Capture extends TacticalExtensionDiagnostics {
        final TreeMap<String,Long> counts=new TreeMap<>();
        final TreeMap<String,List<Object>> samples=new TreeMap<>();
        final long[][] boards;
        Capture(ExactSearch search)throws Exception {
            boards=(long[][])Sr033Study.field(search,"boards");
            var field=ExactSearch.class.getDeclaredField("tacticalDiagnostics");
            field.setAccessible(true);field.set(search,this);
        }
        void count(String key){counts.merge(key,1L,Long::sum);}
        void sample(String key,long[] board,int ply) {
            var list=samples.computeIfAbsent(key,k->new ArrayList<>());
            if(list.size()>=3)return;
            var path=new ArrayList<String>();
            for(int p=1;p<=ply;p++)path.add(Move.coordinate(Sr029Replay.moveBetween(boards[p-1],boards[p])));
            list.add(Map.of("fen",Fen.fromBoard(board),"path",path,"ply",ply));
        }
        @Override public void edge(long[] parent,long[] child,long move,long previous,
                int ply,int depth,boolean checked,boolean complete,int legalCount) {
            if(depth>3)return;
            String d="d"+depth;count(d+"/edges");
            boolean promotion=((move>>>Board.PROMOTE_PIECE_SHIFT)&Board.PIECE_BITS)!=0;
            boolean capture=((move>>>Board.TARGET_PIECE_SHIFT)&Board.PIECE_BITS)!=0;
            boolean priorCapture=previous!=0&&((previous>>>Board.TARGET_PIECE_SHIFT)&Board.PIECE_BITS)!=0;
            if(promotion){count(d+"/promotion");sample("promotion",parent,ply);}
            if(capture&&priorCapture&&Move.toSquare(move)==Move.toSquare(previous)) {
                count(d+"/recapture");if(!checked){count(d+"/uncheckedRecapture");sample("recapture",parent,ply);}
            }
            if(complete&&legalCount==1) {
                count(d+(checked?"/checkedUnique":"/uncheckedUnique"));
                if(!checked)sample("unique",parent,ply);
            }
        }
        @Override public void quietBoundary(long[] board,int ply,int qply) {
            count("quietBoundary");
            // Independent full generation adds diagnostic work only, never changes production scratch.
            int count=ExhaustiveOracle.legalMoves(board).length;
            if(count==1){count("quietBoundaryUnique");sample("quietBoundaryUnique",board,ply);}
        }
    }
    public static void main(String[] args)throws Exception {
        // frozen-store corpus output workers nominal-depth
        int workers=Integer.parseInt(args[3]),depth=Integer.parseInt(args[4]);
        var snapshot=CheckpointStore.readSnapshot(Path.of(args[0]),"g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304");
        if(!snapshot.manifest().networkSha256().equals("5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7"))throw new AssertionError("network");
        var eval=snapshot.model().evaluation(NnueScoreMapping.V1);var gson=new Gson();int roots=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            for(String line:Files.readAllLines(Path.of(args[1]))) {
                if(line.isBlank()||line.startsWith("#"))continue;
                String[] f=line.split("\t",-1);var board=Board.fromFen(f[1]);var builder=GameHistory.builder(board);
                if(f.length>2&&!f[2].isBlank())for(String uci:f[2].split(" ")) {
                    long move=Arrays.stream(ExhaustiveOracle.legalMoves(board)).filter(m->Move.coordinate(m).equals(uci)).findFirst().orElseThrow();
                    board=ExhaustiveOracle.child(board,move);builder.appendPosition(board);
                }
                var history=builder.snapshot();var owner=ProductionSearch.createInformationControl(workers,eval);var sinks=new ArrayList<Capture>();
                if(workers==1)sinks.add(new Capture((ExactSearch)Sr033Study.field(owner,"exact")));
                else for(Object w:(Object[])Sr033Study.field(owner,"workers"))sinks.add(new Capture((ExactSearch)Sr033Study.field(w,"search")));
                try(var driver=new SearchDriver(owner)) {
                    var control=SearchControl.controlled(2_000_000,0,-1,TimeSource.SYSTEM);
                    var result=driver.search(new SearchRequest(board,history,depth,SearchObserver.NONE,control,true));
                    if(result.nodes()!=control.nodes()||result.nodes()!=result.diagnostics().totalEnteredNodes()||driver.activeSearchThreads()!=0)throw new AssertionError("accounting/drain");
                    Phase2Oracle.legalPv(board,result.lastCompletedResult().principalVariation());
                    if(workers==1)try(var plain=new SearchDriver(ProductionSearch.createInformationControl(1,eval))) {
                        var other=plain.search(new SearchRequest(board,history,depth,SearchObserver.NONE,SearchControl.controlled(2_000_000,0,-1,TimeSource.SYSTEM),true));
                        var a=result.lastCompletedResult();var b=other.lastCompletedResult();
                        if(result.nodes()!=other.nodes()||a.score()!=b.score()||a.bestMove()!=b.bestMove()||!Arrays.equals(a.principalVariation(),b.principalVariation()))throw new AssertionError("observer parity");
                    }
                    for(int w=0;w<sinks.size();w++)out.write(gson.toJson(Map.of("root",f[0],"initialFen",f[1],"gameMoves",f.length>2?f[2]:"","worker",w,"counts",sinks.get(w).counts,"samples",sinks.get(w).samples))+"\n");
                    out.write(gson.toJson(Map.of("root",f[0],"outcome",result,"nativeParity",workers==1))+"\n");out.flush();roots++;
                }
            }
            out.write(gson.toJson(Map.of("status","passed","workers",workers,"roots",roots,"depth",depth))+"\n");
        }
    }
}
