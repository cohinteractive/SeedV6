package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.Gson;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;

/** Outcome-blind depth/quiet-ordinal reservoirs from control and actual omission
 * trees. Replays preserve full legal prefixes and correct real-ply mate scores. */
public final class Sr022Replay {
    record Sample(long[] child,GameHistory history,String childFen,List<String> path,
            int ply,int depth,int ordinal,int quietOrdinal,int alpha,int beta,Integer observed,
            int originalAlpha,int originalBeta,boolean explicitScout) {}
    static final class Bucket { int population; final ArrayList<Sample> samples=new ArrayList<>(); }
    public static void main(String[] args) throws Exception {
        if(args.length<3||args.length>4) throw new IllegalArgumentException("store corpus.tsv new-output.jsonl [policies]");
        int[] policies=Arrays.stream((args.length==4?args[3]:"0,3").split(",")).mapToInt(Integer::parseInt).toArray();
        var snapshot=CheckpointStore.readSnapshot(Path.of(args[0]),
                "g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304");
        if(!snapshot.manifest().networkSha256().equals("5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7"))
            throw new AssertionError("Wrong model");
        var definition=snapshot.model().evaluation(NnueScoreMapping.V1);
        var gson=new Gson();int rootIndex=0,samples=0,completed=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)){
            for(String line:Files.readAllLines(Path.of(args[1]))){
                if(line.isBlank()||line.startsWith("#")) continue;
                var f=line.split("\t",-1);var board=Board.fromFen(f[1]);var builder=GameHistory.builder(board);
                if(f.length>2&&!f[2].isBlank())for(String uci:f[2].split(" ")){
                    long m=Arrays.stream(ExhaustiveOracle.legalMoves(board)).filter(x->Move.coordinate(x).equals(uci)).findFirst().orElseThrow();
                    board=ExhaustiveOracle.child(board,m);builder.appendPosition(board);
                }
                final var history=builder.snapshot();
                for(int policy:policies){
                    var positions=new long[257][Board.MAX_BITBOARDS];var delegate=ExactEvaluator.from(definition);
                    var eval=new ExactEvaluator(){
                        public void initialize(long[] b){System.arraycopy(b,0,positions[0],0,b.length);delegate.initialize(b);}
                        public void child(long[] b,long[] c,int p){System.arraycopy(c,0,positions[p+1],0,c.length);delegate.child(b,c,p);}
                        public int evaluate(long[] b,int p){return delegate.evaluate(b,p);}
                    };
                    var buckets=new TreeMap<String,Bucket>();var rng=new Random(220022L+rootIndex*17L+policy);
                    var diag=new LmpDiagnostics(){
                        final int[] originalAlpha=new int[257],originalBeta=new int[257];
                        final boolean[] explicitScout=new boolean[257];
                        public void node(long[] b,int p,int d,int a,int beta,boolean scout){
                            originalAlpha[p]=a;originalBeta[p]=beta;explicitScout[p]=scout;
                        }
                        void capture(long[] child,int p,int d,int n,int q,int a,int beta,Integer score){
                            String key=d+":"+(q<=3?"1-3":q<=6?"4-6":q<=10?"7-10":q<=16?"11-16":"17+");
                            var bucket=buckets.computeIfAbsent(key,k->new Bucket());int slot=rng.nextInt(++bucket.population);
                            if(bucket.samples.size()==6&&slot>=6)return;
                            var h=GameHistory.builder(history);var path=new ArrayList<String>();
                            for(int i=1;i<=p;i++){
                                h.appendPosition(positions[i]);path.add(Move.coordinate(Sr029Replay.moveBetween(positions[i-1],positions[i])));
                            }
                            h.appendPosition(child);path.add(Move.coordinate(Sr029Replay.moveBetween(positions[p],child)));
                            var s=new Sample(child.clone(),h.snapshot(),Fen.fromBoard(child),path,p+1,d-1,n,q,a,beta,score,
                                    originalAlpha[p],originalBeta[p],explicitScout[p]);
                            if(bucket.samples.size()<6)bucket.samples.add(s);else bucket.samples.set(slot,s);
                        }
                        public void omitted(long[] b,long[] c,long m,int p,int d,int n,int q,int a,int beta){capture(c,p,d,n+1,q+1,a,beta,null);}
                        public void searched(long[] b,long[] c,long m,int p,int d,int n,int q,int a,int beta,int score){if(policy==0)capture(c,p,d,n,q,a,beta,score);}
                    };
                    var search=ExactSearch.lateMovePruningResearch(eval,new TTable(),policy,diag);
                    var observed=search.search(board,history,6,()->search.visitedNodes()>=5_000_000);
                    var shadow=ExactSearch.lateMovePruningResearch(ExactEvaluator.from(definition),new TTable(),policy,null);
                    var control=shadow.search(board,history,6,()->shadow.visitedNodes()>=5_000_000);
                    if(!observed.completed()||!control.completed()||observed.score()!=control.score()||observed.nodes()!=control.nodes()
                            ||observed.qnodes()!=control.qnodes()||!Arrays.equals(observed.principalVariation(),control.principalVariation()))
                        throw new AssertionError("Capture changed tree or incomplete: "+f[0]);
                    out.write(gson.toJson(Map.of("kind","capture","root",f[0],"policy",policy,"outcome",observed,"lmp",diag,
                            "population",buckets.entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,e->e.getValue().population)))));
                    out.newLine();out.flush();
                    for(var entry:buckets.entrySet())for(var sample:entry.getValue().samples){
                        samples++;var ref=ExactSearch.quiescenceResearch(ExactEvaluator.from(definition));
                        var result=ref.search(sample.child(),sample.history(),sample.depth(),()->ref.visitedNodes()>=2_000_000);
                        var row=new LinkedHashMap<String,Object>();row.put("kind","replay");row.put("root",f[0]);row.put("initialFen",f[1]);
                        row.put("gameMoves",f.length>2?f[2]:"");row.put("policy",policy);row.put("stratum",entry.getKey());
                        row.put("sample",sample);row.put("reference",result);
                        if(result.completed()){
                            completed++;int value=result.score();
                            if(value>ExactSearch.MAX_STATIC_SCORE)value-=sample.ply();
                            if(value< -ExactSearch.MAX_STATIC_SCORE)value+=sample.ply();
                            row.put("nominalParentValue",-value);row.put("aboveAlpha",-value>sample.alpha());
                            Phase2QuiescenceTest.legalPv(sample.child(),result.principalVariation());
                        }
                        out.write(gson.toJson(row));out.newLine();out.flush();
                    }
                }
                rootIndex++;System.out.println("replayed "+f[0]);
            }
            out.write(gson.toJson(Map.of("status","passed","roots",rootIndex,"samples",samples,"completedReplays",completed)));out.newLine();
        }
    }
}
