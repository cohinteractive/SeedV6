package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.*;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;

/** Conditional horizon sensitivity at actual IIR eligibility events. Two independent
 * unpruned qsearch oracles isolate horizon loss; neither is the selective prediction.
 * Captured histories and real ply are preserved. Counts are not calibrated error rates. */
public final class Sr007HorizonAudit {
    record Sample(long[] board,long[] history,int ply,int depth,int actualDepth,int alpha,int beta) {}
    static int originalPlyScore(ExactSearchResult result,int ply) {
        int score=result.score();return score>ExactSearch.MAX_STATIC_SCORE?score-ply:
                score < -ExactSearch.MAX_STATIC_SCORE?score+ply:score;
    }
    public static void main(String[] args) throws Exception {
        var snapshot=CheckpointStore.readSnapshot(Path.of(args[0]),
                "g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304");
        if(!snapshot.manifest().networkSha256().equals("5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7"))throw new AssertionError("Wrong network");
        var definition=snapshot.model().evaluation(NnueScoreMapping.V1);var gson=new GsonBuilder().create();
        var constructor=GameHistory.class.getDeclaredConstructor(long[].class,long.class,int.class);constructor.setAccessible(true);
        int complete=0,incomplete=0,crossings=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            for(String line:Files.readAllLines(Path.of(args[1]))) {
                if(line.isBlank()||line.startsWith("#"))continue;
                var f=line.split("\t",-1);var board=Board.fromFen(f[1]);var builder=GameHistory.builder(board);
                if(f.length>2&&!f[2].isBlank())for(String uci:f[2].split(" ")) {
                    long move=Arrays.stream(ExhaustiveOracle.legalMoves(board)).filter(m->Move.coordinate(m).equals(uci)).findFirst().orElseThrow();
                    board=ExhaustiveOracle.child(board,move);builder.appendPosition(board);
                }
                final ExactSearch[] owner={null};var samples=new ArrayList<Sample>();
                var diagnostic=new InternalSearchPolicy.Diagnostics(){
                    int candidate;
                    @Override public void reduced(long[] b,int p,int d,int actual,int a,int beta){
                        if(d>5||++candidate%13!=1||samples.size()>=8)return;
                        try {
                            var h=(SearchLineHistory)Sr018Audit.get(owner[0],"history");
                            samples.add(new Sample(b.clone(),Arrays.copyOf((long[])Sr018Audit.get(h,"keys"),h.size()),p,d,actual,a,beta));
                        }catch(Exception e){throw new AssertionError(e);}
                    }
                };
                var adapter=ExactSearchAdapter.internalSearchResearch(definition,InternalSearchPolicy.IIR_ALL,diagnostic);
                owner[0]=(ExactSearch)Sr018Audit.get(adapter,"exact");
                try(var driver=new SearchDriver(adapter)) {
                    var control=SearchControl.controlled(500_000,System.nanoTime(),15_000_000_000L,TimeSource.SYSTEM);
                    var result=driver.search(new SearchRequest(board,builder.snapshot(),7,SearchObserver.NONE,control,true));
                    out.write(gson.toJson(Map.of("id",f[0],"capture",result,"diagnostics",diagnostic)));out.newLine();
                }
                for(var sample:samples) {
                    var history=constructor.newInstance(sample.history,sample.board[Board.KEY],(int)sample.board[Board.STATUS]);history.requireCurrent(sample.board);
                    var oracle=ExactSearch.quiescenceResearch(ExactEvaluator.from(definition));
                    long start=System.nanoTime();
                    var reduced=oracle.search(sample.board,history,sample.actualDepth,()->System.nanoTime()-start>3_000_000_000L);
                    long next=System.nanoTime();
                    var nominal=oracle.search(sample.board,history,sample.depth,()->System.nanoTime()-next>3_000_000_000L);
                    boolean done=reduced.completed()&&nominal.completed();
                    int rv=originalPlyScore(reduced,sample.ply),nv=originalPlyScore(nominal,sample.ply);
                    boolean crossing=done&&((rv>=sample.beta)!=(nv>=sample.beta)||(rv<=sample.alpha)!=(nv<=sample.alpha));
                    if(done) {complete++;if(crossing)crossings++;}else incomplete++;
                    out.write(gson.toJson(Map.of("id",f[0],"sample",sample,"reduced",reduced,"nominal",nominal,
                            "reducedOriginalPly",rv,"nominalOriginalPly",nv,"thresholdCrossing",crossing)));out.newLine();out.flush();
                }
            }
            out.write(gson.toJson(Map.of("status","passed","completedPairs",complete,"incompletePairs",incomplete,"thresholdCrossings",crossings)));out.newLine();
        }
    }
}
