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

/** Outcome-blind omission reservoirs and independent full-history full-window replays.
 * Public reroot results have real-ply mate normalization restored explicitly. */
public final class Sr021Audit {
    record Sample(String parentFen,String childFen,String move,List<String> path,long[] child,
            GameHistory history,int ply,int depth,int alpha,int beta,int eval,int ordinal,int quietOrdinal){}
    static class Bucket {long count;final ArrayList<Sample> values=new ArrayList<>();}
    static class Sink extends FutilityPolicy.Diagnostics {
        ExactSearch search; final GameHistory history;final Random random;
        final Map<Integer,Bucket> buckets=new TreeMap<>();
        Sink(GameHistory h,long seed){history=h;random=new Random(seed);}
        public void omitted(long[] b,long[] c,long move,int ply,int depth,int n,int q,int a,int z,int e){
            Bucket bucket=buckets.computeIfAbsent(depth,d->new Bucket());long slot=random.nextLong(++bucket.count);
            if(bucket.values.size()==8&&slot>=8)return;
            try{
                var boards=(long[][])Sr021Study.field(search,"boards");var builder=GameHistory.builder(history);var path=new ArrayList<String>();
                for(int p=1;p<=ply;p++){builder.appendPosition(boards[p]);path.add(Move.coordinate(Sr029Replay.moveBetween(boards[p-1],boards[p])));}
                builder.appendPosition(c);path.add(Move.coordinate(move));
                Sample s=new Sample(Fen.fromBoard(b),Fen.fromBoard(c),Move.coordinate(move),path,c.clone(),builder.snapshot(),ply+1,depth,a,z,e,n,q);
                if(bucket.values.size()<8)bucket.values.add(s);else bucket.values.set((int)slot,s);
            }catch(Exception error){throw new RuntimeException(error);}
        }
    }
    public static void main(String[] args)throws Exception {
        var snapshot=CheckpointStore.readSnapshot(Path.of(args[0]),"g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304");
        if(!snapshot.manifest().networkSha256().equals("5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7"))throw new AssertionError("model");
        var definition=snapshot.model().evaluation(NnueScoreMapping.V1);var gson=new Gson();int roots=0,samples=0,completed=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)){
            for(String name:args[3].split(","))for(String line:Files.readAllLines(Path.of(args[1]))){
                if(line.isBlank()||line.startsWith("#"))continue;
                var policy=FutilityPolicy.valueOf(name.toUpperCase(Locale.ROOT));var f=line.split("\t",-1);var board=Board.fromFen(f[1]);var h=GameHistory.builder(board);
                if(f.length>2&&!f[2].isBlank())for(String uci:f[2].split(" ")){
                    long m=Arrays.stream(ExhaustiveOracle.legalMoves(board)).filter(x->Move.coordinate(x).equals(uci)).findFirst().orElseThrow();board=ExhaustiveOracle.child(board,m);h.appendPosition(board);
                }
                var history=h.snapshot();var sink=new Sink(history,211015L+roots);
                var owner=ExactSearchAdapter.futilityResearch(definition,policy,sink);sink.search=(ExactSearch)Sr021Study.field(owner,"exact");
                try(var driver=new SearchDriver(owner)){
                    var result=driver.search(new SearchRequest(board,history,6,SearchControl.controlled(5_000_000,0,-1,TimeSource.SYSTEM)));
                    if(!result.targetDepthCompleted())throw new AssertionError("capture incomplete");
                    Phase2QuiescenceTest.legalPv(board,result.lastCompletedResult().principalVariation());
                    out.write(gson.toJson(Map.of("kind","capture","root",f[0],"policy",name,"omissions",sink.omissions,"result",result)));out.newLine();
                    for(var bucket:sink.buckets.values())for(var sample:bucket.values){
                        samples++;
                        for(boolean qxray:new boolean[]{false,true}){
                            var oracle=qxray ? ExactSearch.quiescencePvsResearch(ExactEvaluator.from(definition),null,ExactSearch.QSEARCH_BASELINE)
                                    : ExactSearch.quiescenceResearch(ExactEvaluator.from(definition));
                            if(qxray)oracle.withSeeResearch(SeePolicy.QXRAY,null);
                            var replay=oracle.search(sample.child,sample.history,sample.depth-1,()->oracle.visitedNodes()>=1_000_000);
                            var row=new LinkedHashMap<String,Object>();row.put("kind","replay");row.put("root",f[0]);row.put("initialFen",f[1]);row.put("gameMoves",f.length>2?f[2]:"");row.put("policy",name);row.put("sample",sample);row.put("qxray",qxray);row.put("reference",replay);
                            if(replay.completed()){
                                completed++;int value=replay.score();if(value>ExactSearch.MAX_STATIC_SCORE)value-=sample.ply;if(value< -ExactSearch.MAX_STATIC_SCORE)value+=sample.ply;
                                row.put("parentValue",-value);row.put("aboveAlpha",-value>sample.alpha);Phase2QuiescenceTest.legalPv(sample.child,replay.principalVariation());
                            }
                            out.write(gson.toJson(row));out.newLine();
                        }
                    }
                    out.flush();roots++;
                }
            }
            out.write(gson.toJson(Map.of("status","passed","roots",roots,"samples",samples,"completedReplays",completed)));out.newLine();
        }
    }
}
