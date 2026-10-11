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

/** Outcome-blind SR-023 history strata and independent unpruned nominal/qsearch replays.
 * This diagnostic allocates freely outside timed runs; samples are correlated, not calibrated rates. */
public final class Sr023Replay {
    record Sample(long[] child, GameHistory history, String childFen, List<String> path,
            int ply, int depth, int ordinal, int quietOrdinal, int alpha, int beta,
            int frozen, int live, boolean omitted, int originalAlpha, int originalBeta, boolean scout) {}
    static final class Bucket { long population; final ArrayList<Sample> samples = new ArrayList<>(); }

    public static void main(String[] args) throws Exception {
        if(args.length != 3) throw new IllegalArgumentException("store corpus.tsv new-output.jsonl");
        var snapshot = CheckpointStore.readSnapshot(Path.of(args[0]),
                "g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304");
        if(!snapshot.manifest().networkSha256().equals("5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7"))
            throw new AssertionError("Wrong model");
        var definition = snapshot.model().evaluation(NnueScoreMapping.V1);
        var gson = new Gson(); int roots = 0, samples = 0, completed = 0;
        try(var out = Files.newBufferedWriter(Path.of(args[2]), StandardOpenOption.CREATE_NEW)) {
            for(String line : Files.readAllLines(Path.of(args[1]))) {
                if(line.isBlank() || line.startsWith("#")) continue;
                var f = line.split("\t", -1); var board = Board.fromFen(f[1]); var builder = GameHistory.builder(board);
                if(f.length > 2 && !f[2].isBlank()) for(String uci : f[2].split(" ")) {
                    long m = Arrays.stream(ExhaustiveOracle.legalMoves(board))
                            .filter(x -> Move.coordinate(x).equals(uci)).findFirst().orElseThrow();
                    board = ExhaustiveOracle.child(board, m); builder.appendPosition(board);
                }
                final var history = builder.snapshot();
                var positions = new long[257][Board.MAX_BITBOARDS]; var delegate = ExactEvaluator.from(definition);
                var eval = new ExactEvaluator() {
                    public void initialize(long[] b) { System.arraycopy(b,0,positions[0],0,b.length); delegate.initialize(b); }
                    public void child(long[] b,long[] c,int p) { System.arraycopy(c,0,positions[p+1],0,c.length); delegate.child(b,c,p); }
                    public int evaluate(long[] b,int p) { return delegate.evaluate(b,p); }
                };
                var buckets = new TreeMap<String,Bucket>(); var rng = new Random(230012L + roots);
                var magnitudes = new TreeMap<Integer,Long>(); long[] totals = new long[4];
                var diag = new LmpDiagnostics() {
                    final int[] oa = new int[257], ob = new int[257]; final boolean[] scouts = new boolean[257];
                    public void node(long[] b,int p,int d,int a,int z,boolean scout) { oa[p]=a; ob[p]=z; scouts[p]=scout; }
                    public void historyCandidate(long[] b,long[] c,long m,int p,int d,int n,int q,
                            int a,int z,int frozen,int live,boolean omitted) {
                        if(Math.abs(frozen)>QuietHistory.LIMIT || Math.abs(live)>QuietHistory.LIMIT)
                            throw new AssertionError("Not a quiet snapshot");
                        totals[0]++; if(frozen!=live) totals[1]++; if(Integer.signum(frozen)!=Integer.signum(live)) totals[2]++;
                        if(omitted) totals[3]++;
                        magnitudes.merge(frozen,1L,Long::sum);
                        int ordinal=q+1;
                        String band=ordinal<=3?"1-3":ordinal<=6?"4-6":ordinal<=10?"7-10":ordinal<=16?"11-16":"17+";
                        String key=d+":"+band+":"+Integer.signum(frozen)+":"+(omitted?"omitted":"searched")
                                +":"+(Long.bitCount(b[0]|b[1]|b[2])<=6?"sparse":"ordinary");
                        var bucket=buckets.computeIfAbsent(key,k->new Bucket()); long slot=rng.nextLong(++bucket.population);
                        if(bucket.samples.size()==4 && slot>=4) return;
                        var h=GameHistory.builder(history); var path=new ArrayList<String>();
                        for(int i=1;i<=p;i++) { h.appendPosition(positions[i]); path.add(Move.coordinate(Sr029Replay.moveBetween(positions[i-1],positions[i]))); }
                        h.appendPosition(c); path.add(Move.coordinate(m));
                        var s=new Sample(c.clone(),h.snapshot(),Fen.fromBoard(c),path,p+1,d-1,n+1,q+1,a,z,
                                frozen,live,omitted,oa[p],ob[p],scouts[p]);
                        if(bucket.samples.size()<4) bucket.samples.add(s); else bucket.samples.set((int)slot,s);
                    }
                };
                var search=ExactSearch.lateMovePruningResearch(eval,new TTable(),ExactSearch.LMP_WINDOW,diag);
                var observed=search.search(board,history,6,()->search.visitedNodes()>=5_000_000);
                var plain=ExactSearch.lateMovePruningResearch(ExactEvaluator.from(definition),new TTable(),ExactSearch.LMP_WINDOW,null);
                var control=plain.search(board,history,6,()->plain.visitedNodes()>=5_000_000);
                if(!observed.completed() || !control.completed() || observed.score()!=control.score() || observed.nodes()!=control.nodes()
                        || observed.qnodes()!=control.qnodes() || !Arrays.equals(observed.principalVariation(),control.principalVariation()))
                    throw new AssertionError("Instrumentation changed tree or incomplete: "+f[0]);
                out.write(gson.toJson(Map.of("kind","capture","root",f[0],"outcome",observed,"opportunities",totals,
                        "historyHistogram",magnitudes,"population",buckets.entrySet().stream().collect(
                                java.util.stream.Collectors.toMap(Map.Entry::getKey,e->e.getValue().population))))); out.newLine(); out.flush();
                for(var entry:buckets.entrySet()) for(var sample:entry.getValue().samples) {
                    samples++; var ref=ExactSearch.quiescenceResearch(ExactEvaluator.from(definition));
                    var result=ref.search(sample.child(),sample.history(),sample.depth(),()->ref.visitedNodes()>=2_000_000);
                    var row=new LinkedHashMap<String,Object>(); row.put("kind","replay"); row.put("root",f[0]);
                    row.put("initialFen",f[1]); row.put("gameMoves",f.length>2?f[2]:""); row.put("stratum",entry.getKey());
                    row.put("sample",sample); row.put("reference",result);
                    if(result.completed()) {
                        completed++; int v=result.score(); if(v>ExactSearch.MAX_STATIC_SCORE)v-=sample.ply();
                        if(v< -ExactSearch.MAX_STATIC_SCORE)v+=sample.ply();
                        row.put("nominalParentValue",-v); row.put("aboveAlpha",-v>sample.alpha());
                        Phase2QuiescenceTest.legalPv(sample.child(),result.principalVariation());
                    }
                    out.write(gson.toJson(row)); out.newLine(); out.flush();
                }
                roots++; System.out.println("replayed "+f[0]);
            }
            out.write(gson.toJson(Map.of("status","passed","roots",roots,"samples",samples,"completedReplays",completed))); out.newLine();
        }
    }
}
