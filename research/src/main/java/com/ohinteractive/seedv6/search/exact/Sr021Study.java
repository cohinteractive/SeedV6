package com.ohinteractive.seedv6.search.exact;

import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import com.google.gson.Gson;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;

/** Outcome-blind conditional headroom study on actual QXRAY cumulative production.
 * Test-only reflection installs existing observation sinks; no shipped code changes.
 * Samples are correlated diagnostics, not population error rates or strength evidence. */
public final class Sr021Study {
    static Object field(Object owner,String name)throws Exception {
        Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);
    }
    static class Sample {
        String stratum,fen,childFen,move; List<String> path;
        long[] parent,child; GameHistory history;
        int depth,ply,quietOrdinal,ordinal,alpha,beta,originalAlpha,originalBeta,ordering,live;
        boolean omitted,scout; Integer observedScore;
    }
    static class Bucket {long population; final ArrayList<Sample> samples=new ArrayList<>();}
    static class Capture extends LmpDiagnostics {
        final ExactSearch search; final GameHistory rootHistory;
        final long[][] boards; final Random random;
        final TreeMap<String,Bucket> buckets=new TreeMap<>();
        final Sample[] pending=new Sample[257];
        final int[] oa=new int[257],ob=new int[257];final boolean[] scout=new boolean[257];
        Capture(ExactSearch s,GameHistory h,long seed)throws Exception {
            search=s;rootHistory=h;boards=(long[][])field(s,"boards");random=new Random(seed);
            Field f=ExactSearch.class.getDeclaredField("lmpDiagnostics");f.setAccessible(true);f.set(s,this);
        }
        public void node(long[] b,int p,int d,int a,int z,boolean sc){oa[p]=a;ob[p]=z;scout[p]=sc;pending[p]=null;}
        public void historyCandidate(long[] b,long[] c,long m,int p,int d,int n,int q,int a,int z,int frozen,int live,boolean omitted) {
            pending[p]=null;
            int type=(int)(m>>>Board.START_PIECE_SHIFT)&Piece.TYPE;
            String band=q<3?"early":q<10?"middle":"late";
            String k=d+":"+band+":"+(type==Piece.PAWN?"pawn":type==Piece.KING?"king":"piece")+":"+omitted+":"+(Long.bitCount(b[0]|b[1]|b[2])<=6?"sparse":"ordinary");
            Bucket bucket=buckets.computeIfAbsent(k,x->new Bucket());long slot=random.nextLong(++bucket.population);
            if(bucket.samples.size()==3&&slot>=3)return;
            Sample s=new Sample();s.stratum=k;s.parent=b.clone();s.child=c.clone();s.fen=Fen.fromBoard(b);s.childFen=Fen.fromBoard(c);s.move=Move.coordinate(m);
            s.depth=d;s.ply=p;s.ordinal=n+1;s.quietOrdinal=q+1;s.alpha=a;s.beta=z;s.originalAlpha=oa[p];s.originalBeta=ob[p];s.scout=scout[p];s.ordering=frozen;s.live=live;s.omitted=omitted;
            var h=GameHistory.builder(rootHistory);s.path=new ArrayList<>();
            for(int i=1;i<=p;i++){h.appendPosition(boards[i]);s.path.add(Move.coordinate(Sr029Replay.moveBetween(boards[i-1],boards[i])));}
            h.appendPosition(c);s.history=h.snapshot();
            if(bucket.samples.size()<3)bucket.samples.add(s);else bucket.samples.set((int)slot,s);
            if(!omitted)pending[p]=s;
        }
        public void searched(long[] b,long[] c,long m,int p,int d,int n,int q,int a,int z,int score) {
            Sample s=pending[p];if(s!=null){
                if(!s.move.equals(Move.coordinate(m))||s.alpha!=a)throw new AssertionError("sample association");
                s.observedScore=score;pending[p]=null;
            }
        }
    }
    public static void main(String[] args)throws Exception {
        // store corpus output workers; no time budget in this intrusive diagnostic.
        var snapshot=CheckpointStore.readSnapshot(Path.of(args[0]),"g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304");
        if(!snapshot.manifest().networkSha256().equals("5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7"))throw new AssertionError("model");
        var definition=snapshot.model().evaluation(NnueScoreMapping.V1);int workers=Integer.parseInt(args[3]);
        var fresh=ExactEvaluator.from(definition);var gson=new Gson();int roots=0,rows=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            for(String line:Files.readAllLines(Path.of(args[1]))) {
                if(line.isBlank()||line.startsWith("#"))continue;
                var f=line.split("\t",-1);var board=Board.fromFen(f[1]);var h=GameHistory.builder(board);
                if(f.length>2&&!f[2].isBlank())for(String uci:f[2].split(" ")){
                    long m=Arrays.stream(ExhaustiveOracle.legalMoves(board)).filter(x->Move.coordinate(x).equals(uci)).findFirst().orElseThrow();
                    board=ExhaustiveOracle.child(board,m);h.appendPosition(board);
                }
                var history=h.snapshot();var captures=new ArrayList<Capture>();
                var owner=ProductionSearch.create(workers,definition);
                if(owner instanceof ExactSearchAdapter)captures.add(new Capture((ExactSearch)field(owner,"exact"),history,210015L+roots));
                else for(Object w:(Object[])field(owner,"workers"))captures.add(new Capture((ExactSearch)field(w,"search"),history,210015L+roots*17+captures.size()));
                try(var driver=new SearchDriver(owner)) {
                    var control=SearchControl.controlled(5_000_000,0,-1,TimeSource.SYSTEM);
                    var result=driver.search(new SearchRequest(board,history,6,SearchObserver.NONE,control,true));
                    if(result.nodes()!=control.nodes()||result.nodes()!=result.diagnostics().totalEnteredNodes())throw new AssertionError("accounting");
                    if(result.lastCompletedResult()!=null)Phase2Oracle.legalPv(board,result.lastCompletedResult().principalVariation());
                    boolean parity=false;
                    if(workers==1)try(var plain=new SearchDriver(ProductionSearch.create(1,definition))){
                        var other=plain.search(new SearchRequest(board,history,6,SearchObserver.NONE,SearchControl.controlled(5_000_000,0,-1,TimeSource.SYSTEM),true));
                        var a=result.lastCompletedResult();var b=other.lastCompletedResult();
                        if(result.nodes()!=other.nodes()||a.score()!=b.score()||a.bestMove()!=b.bestMove()||!Arrays.equals(a.principalVariation(),b.principalVariation()))throw new AssertionError("observer tree changed");
                        parity=true;
                    }
                    out.write(gson.toJson(Map.of("kind","root","root",f[0],"workers",workers,"outcome",result,"nativeParity",parity)));out.newLine();
                    for(int w=0;w<captures.size();w++)for(var e:captures.get(w).buckets.entrySet()){
                        out.write(gson.toJson(Map.of("kind","population","root",f[0],"worker",w,"stratum",e.getKey(),"count",e.getValue().population)));out.newLine();
                        for(Sample s:e.getValue().samples){
                            fresh.initialize(s.parent);int parentEval=fresh.evaluate(s.parent,0);
                            fresh.initialize(s.child);int childValue=-fresh.evaluate(s.child,0);
                            var row=new LinkedHashMap<String,Object>();row.put("kind","sample");row.put("root",f[0]);row.put("worker",w);row.put("initialFen",f[1]);row.put("gameMoves",f.length>2?f[2]:"");row.put("sample",s);
                            row.put("parentEval",parentEval);row.put("childStaticValue",childValue);row.put("parentHeadroom",s.alpha-parentEval);row.put("childHeadroom",s.alpha-childValue);
                            out.write(gson.toJson(row));out.newLine();rows++;
                        }
                    }
                    out.flush();roots++;System.out.println("captured "+f[0]+" workers="+workers);
                }
            }
            out.write(gson.toJson(Map.of("status","passed","roots",roots,"samples",rows,"workers",workers)));out.newLine();
        }
    }
}
