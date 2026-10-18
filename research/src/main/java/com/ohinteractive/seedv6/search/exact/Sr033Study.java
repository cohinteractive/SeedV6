package com.ohinteractive.seedv6.search.exact;

import java.lang.reflect.Field;
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

/** Observation only, on actual production. Callbacks never change a search decision.
 * Samples are conditional/correlated; a missing final receipt means incomplete. */
public final class Sr033Study {
    static Object field(Object owner, String name) throws Exception {
        Field f=owner.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(owner);
    }
    static class Sample {
        String fen, move, stratum; List<String> path;
        int ply, depth, reduction, ordinal, alpha, beta, reduced, history;
        Integer verified; boolean verifyNext, fullNext;
    }
    static class Bucket { long population; List<Sample> samples=new ArrayList<>(); }
    static class Capture extends LmrDiagnostics {
        final long[][] boards; final int[] quietHistory;
        final int[] confirmed=new int[65536], refuted=new int[65536];
        final int[] indices=new int[257]; final Sample[] pending=new Sample[257];
        final Random rng; final TreeMap<String,Bucket> buckets=new TreeMap<>();
        final TreeMap<String,long[]> observations=new TreeMap<>();
        Capture(ExactSearch s, long seed) throws Exception {
            boards=(long[][])field(s,"boards"); quietHistory=(int[])field(s,"quietHistory"); rng=new Random(seed);
            Field f=ExactSearch.class.getDeclaredField("lmrDiagnostics"); f.setAccessible(true); f.set(s,this);
        }
        @Override void reset() { /* Aggregate all iterations and root jobs in this request. */ }
        @Override public void probe(long[] child,int ply,int depth,int reduction,int ordinal,
                int alpha,int beta,int score,boolean verify) {
            long move=Sr029Replay.moveBetween(boards[ply-1],child);
            int index=QuietHistory.index(move), h=quietHistory[index]; indices[ply]=index;
            String key=depth+":"+reduction+":"+(h>0?"positive":h<0?"negative":"zero")+":"+(ordinal<8?"4-7":"8+");
            long[] counts=observations.computeIfAbsent(key,k->new long[6]); counts[0]++;
            if(verify)counts[1]++;
            if(confirmed[index]>0)counts[2]++;
            if(refuted[index]>0)counts[3]++;
            if(confirmed[index]>0&&h<=0)counts[4]++;
            if(refuted[index]>0&&h>=0)counts[5]++;
            String stratum=depth+":"+reduction+":"+verify;
            Bucket bucket=buckets.computeIfAbsent(stratum,k->new Bucket());
            long slot=rng.nextLong(++bucket.population); pending[ply]=null;
            if(bucket.samples.size()==4&&slot>=4)return;
            Sample s=new Sample();s.fen=Fen.fromBoard(child);s.move=Move.coordinate(move);s.stratum=stratum;
            s.ply=ply;s.depth=depth;s.reduction=reduction;s.ordinal=ordinal;s.alpha=alpha;s.beta=beta;
            s.reduced=score;s.history=h;s.verifyNext=verify;s.path=new ArrayList<>();
            for(int p=1;p<=ply;p++)s.path.add(Move.coordinate(Sr029Replay.moveBetween(boards[p-1],boards[p])));
            if(bucket.samples.size()<4)bucket.samples.add(s);else bucket.samples.set((int)slot,s);
            pending[ply]=s;
        }
        @Override public void verified(int ply,int depth,int alpha,int beta,int score,boolean full) {
            if(score>alpha)confirmed[indices[ply]]++;else refuted[indices[ply]]++;
            Sample s=pending[ply];if(s!=null){s.verified=score;s.fullNext=full;}
            pending[ply]=null;
        }
    }
    public static void main(String[] args) throws Exception {
        // store corpus.tsv new-output.jsonl workers depth
        int workers=Integer.parseInt(args[3]),depth=Integer.parseInt(args[4]);
        var snapshot=CheckpointStore.readSnapshot(Path.of(args[0]),"g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304");
        if(!snapshot.manifest().networkSha256().equals("5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7"))throw new AssertionError("network");
        var definition=snapshot.model().evaluation(NnueScoreMapping.V1);var gson=new Gson();int roots=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            for(String line:Files.readAllLines(Path.of(args[1]))) {
                if(line.isBlank()||line.startsWith("#"))continue;
                String[] f=line.split("\t",-1);var board=Board.fromFen(f[1]);var builder=GameHistory.builder(board);
                if(f.length>2&&!f[2].isBlank())for(String uci:f[2].split(" ")) {
                    long move=Arrays.stream(ExhaustiveOracle.legalMoves(board)).filter(m->Move.coordinate(m).equals(uci)).findFirst().orElseThrow();
                    board=ExhaustiveOracle.child(board,move);builder.appendPosition(board);
                }
                var history=builder.snapshot();var owner=ProductionSearch.createInformationControl(workers,definition);var captures=new ArrayList<Capture>();
                if(workers==1)captures.add(new Capture((ExactSearch)field(owner,"exact"),330017L+roots));
                else for(Object w:(Object[])field(owner,"workers"))captures.add(new Capture((ExactSearch)field(w,"search"),330017L+roots*17+captures.size()));
                try(var driver=new SearchDriver(owner)) {
                    var control=SearchControl.controlled(2_000_000,0,-1,TimeSource.SYSTEM);
                    var result=driver.search(new SearchRequest(board,history,depth,SearchObserver.NONE,control,true));
                    if(result.nodes()!=control.nodes()||result.nodes()!=result.diagnostics().totalEnteredNodes()||driver.activeSearchThreads()!=0)throw new AssertionError("accounting/drain");
                    if(result.lastCompletedResult()!=null)Phase2Oracle.legalPv(board,result.lastCompletedResult().principalVariation());
                    boolean parity=false;
                    if(workers==1)try(var plain=new SearchDriver(ProductionSearch.createInformationControl(1,definition))) {
                        var other=plain.search(new SearchRequest(board,history,depth,SearchObserver.NONE,SearchControl.controlled(2_000_000,0,-1,TimeSource.SYSTEM),true));
                        var a=result.lastCompletedResult();var b=other.lastCompletedResult();
                        if(result.nodes()!=other.nodes()||a.score()!=b.score()||a.bestMove()!=b.bestMove()||!Arrays.equals(a.principalVariation(),b.principalVariation()))throw new AssertionError("observer tree changed");parity=true;
                    }
                    out.write(gson.toJson(Map.of("kind","root","root",f[0],"workers",workers,"outcome",result,"nativeParity",parity)));out.newLine();
                    for(int w=0;w<captures.size();w++) {
                        Capture c=captures.get(w);
                        var counters=Map.of("attempts",c.attempts,"verifications",c.verifications,"omissions",c.omissions,"fullResearches",c.fullResearches,
                                "reducedNodes",c.reducedNodes,"reducedQnodes",c.reducedQnodes,"verificationNodes",c.verificationNodes,"verificationQnodes",c.verificationQnodes,
                                "fullResearchNodes",c.fullResearchNodes,"fullResearchQnodes",c.fullResearchQnodes);
                        out.write(gson.toJson(Map.of("kind","worker","root",f[0],"worker",w,"counts",counters,"strata",c.observations)));out.newLine();
                        for(var entry:c.buckets.entrySet())for(Sample s:entry.getValue().samples) {
                            out.write(gson.toJson(Map.of("kind","sample","root",f[0],"worker",w,"population",entry.getValue().population,"initialFen",f[1],"gameMoves",f.length>2?f[2]:"","sample",s)));out.newLine();
                        }
                    }
                    out.flush();roots++;
                }
            }
            out.write(gson.toJson(Map.of("status","passed","roots",roots,"workers",workers,"depth",depth)));out.newLine();
        }
    }
}
