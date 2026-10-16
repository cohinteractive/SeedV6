package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.*;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;

/** Conditional mechanism audit, not strength or an unbiased error-rate estimate.
 * Retains original full real history and ply; independent unpruned qsearch oracle
 * reroots the same legal state, then converts mate distance to the captured ply. */
public final class Sr018Audit {
    static Object get(Object object,String name) throws Exception {
        var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);
    }
    record Sample(long[] board,long[] history,int ply,int depth,int beta,int evaluation,int reduction,int prediction,boolean accepted) {}
    public static void main(String[] args) throws Exception {
        var snapshot=CheckpointStore.readSnapshot(Path.of(args[0]),"g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304");
        if(!snapshot.manifest().networkSha256().equals("5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7"))throw new AssertionError("Wrong network");
        var definition=snapshot.model().evaluation(NnueScoreMapping.V1);var gson=new GsonBuilder().create();
        var constructor=GameHistory.class.getDeclaredConstructor(long[].class,long.class,int.class);constructor.setAccessible(true);
        long[] histogram=new long[11];int audited=0,errors=0,incomplete=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            for(String line:Files.readAllLines(Path.of(args[1]))) {
                if(line.isBlank()||line.startsWith("#"))continue;
                var f=line.split("\t",-1);var board=Board.fromFen(f[1]);var builder=GameHistory.builder(board);
                if(f.length>2&&!f[2].isBlank())for(String uci:f[2].split(" ")) {
                    long move=Arrays.stream(ExhaustiveOracle.legalMoves(board)).filter(m->Move.coordinate(m).equals(uci)).findFirst().orElseThrow();
                    board=ExhaustiveOracle.child(board,move);builder.appendPosition(board);
                }
                final ExactSearch[] owner={null};var samples=new ArrayList<Sample>();
                var diagnostic=new NullMovePolicy.Diagnostics(){
                    int candidate;
                    @Override public void probe(long[] b,int p,int d,int beta,int eval,int r,int score,boolean accepted){
                        int surplus=eval-beta;int bin=surplus<0?0:surplus<8?1:surplus<16?2:surplus<32?3:surplus<64?4:surplus<100?5:surplus<200?6:surplus<400?7:surplus<800?8:surplus<1600?9:10;
                        histogram[bin]++;
                        if(score<beta||++candidate%7!=1||samples.size()>=12)return;
                        try {
                            var h=(SearchLineHistory)get(owner[0],"history");
                            samples.add(new Sample(b.clone(),Arrays.copyOf((long[])get(h,"keys"),h.size()),p,d,beta,eval,r,score,accepted));
                        } catch(Exception e){throw new AssertionError(e);}
                    }
                };
                var adapter=ExactSearchAdapter.nullMoveResearch(definition,NullMovePolicy.valueOf(args[3]),diagnostic);
                owner[0]=(ExactSearch)get(adapter,"exact");
                try(var driver=new SearchDriver(adapter)) {
                    var control=SearchControl.controlled(2_000_000,System.nanoTime(),30_000_000_000L,TimeSource.SYSTEM);
                    var result=driver.search(new SearchRequest(board,builder.snapshot(),7,SearchObserver.NONE,control,true));
                    out.write(gson.toJson(Map.of("id",f[0],"capture",result,"diagnostics",diagnostic)));out.newLine();
                }
                var oracle=ExactSearch.quiescenceResearch(ExactEvaluator.from(definition));
                for(var sample:samples) {
                    var history=constructor.newInstance(sample.history,sample.board[Board.KEY],(int)sample.board[Board.STATUS]);history.requireCurrent(sample.board);
                    long start=System.nanoTime();
                    var result=oracle.searchWindow(sample.board,history,sample.depth,sample.beta-1,sample.beta,()->System.nanoTime()-start>5_000_000_000L);
                    int value=result.score();if(result.completed()&&value>ExactSearch.MAX_STATIC_SCORE)value-=sample.ply;
                    if(result.completed()&&value < -ExactSearch.MAX_STATIC_SCORE)value+=sample.ply;
                    boolean falseCut=result.completed()&&value<sample.beta;
                    if(result.completed()){audited++;if(falseCut)errors++;}else incomplete++;
                    out.write(gson.toJson(Map.of("id",f[0],"sample",sample,"reference",result,"originalPlyScore",value,"falseCutoff",falseCut)));out.newLine();out.flush();
                }
                System.out.println("audited "+f[0]+" completed="+audited+" differences="+errors+" incomplete="+incomplete);
            }
            out.write(gson.toJson(Map.of("status","passed","audited",audited,"falseCutoffs",errors,"incomplete",incomplete,"surplusHistogram",histogram)));out.newLine();
        }
    }
}
