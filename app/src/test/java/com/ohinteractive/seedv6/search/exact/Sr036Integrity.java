package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.*;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;

/** Real frozen-model worker isolation and independently replayed ordinary TT proofs.
 * Every actual evaluator call checks private rolling state against fresh recomputation.
 * Full legal histories/root ply are retained in independent proof samples. */
public final class Sr036Integrity {
    record Proof(long[] board,GameHistory history,int depth,int type,int score){}
    static class Checked implements ExactEvaluator {
        final ExactEvaluator rolling;final SearchEvaluation.State fresh;
        final long[][] boards=new long[ExactSearch.MAX_DEPTH+1][Board.MAX_BITBOARDS];
        final long[] keys=new long[ExactSearch.MAX_DEPTH+1];
        final ThreadLocal<Checked> current;final AtomicLong evaluations;
        GameHistory game;
        Checked(SearchEvaluation e,ThreadLocal<Checked> current,AtomicLong evaluations){rolling=ExactEvaluator.from(e);fresh=e.newState(1);this.current=current;this.evaluations=evaluations;}
        public void initialize(long[] b){current.set(this);System.arraycopy(b,0,boards[0],0,b.length);keys[0]=SearchKey.rootHistory(b,game);rolling.initialize(b);}
        public void child(long[] b,long[] c,int p){
            System.arraycopy(c,0,boards[p+1],0,c.length);
            keys[p+1]=SearchKey.childHistory(keys[p],PositionIdentity.repetitionKey(c),(int)c[Board.STATUS]);rolling.child(b,c,p);
        }
        public int evaluate(long[] b,int p){int score=rolling.evaluate(b,p);fresh.initialize(b,0);if(score!=fresh.evaluate(b,0))throw new AssertionError("worker evaluator mismatch");evaluations.incrementAndGet();return score;}
    }
    public static void main(String[] args)throws Exception {
        var snapshot=CheckpointStore.readSnapshot(Path.of(args[0]),"g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304");
        if(!snapshot.manifest().networkSha256().equals("5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7"))throw new AssertionError("network");
        var eval=snapshot.model().evaluation(NnueScoreMapping.V1);var evaluations=new AtomicLong();var stores=new AtomicLong();
        var proofs=Collections.synchronizedList(new ArrayList<Proof>());var current=new ThreadLocal<Checked>();
        var gson=new GsonBuilder().create();int observations=0,oracleChecks=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            for(String line:Files.readAllLines(Path.of(args[1]))) {
                if(line.isBlank()||line.startsWith("#"))continue;
                var fields=line.split("\t",-1);var board=Board.fromFen(fields[1]);var builder=GameHistory.builder(board);
                if(fields.length>2&&!fields[2].isBlank())for(String uci:fields[2].split(" ")) {
                    long move=Arrays.stream(ExhaustiveOracle.legalMoves(board)).filter(m->Move.coordinate(m).equals(uci)).findFirst().orElseThrow();
                    board=ExhaustiveOracle.child(board,move);builder.appendPosition(board);
                }
                final var history=builder.snapshot();var original=board.clone();
                for(var policy:ConcurrencyPolicy.values())for(int workers:new int[]{2,4}) {
                    var table=new TTable(1){
                        @Override public void save(long key,int depth,int type,int score,long move){
                            long n=stores.incrementAndGet();
                            if(n%31==0 && depth<=4 && proofs.size()<2000) {
                                var w=current.get();if(w==null)throw new AssertionError("unowned TT write");
                                int ply=-1;
                                for(int i=0;i<w.keys.length;i++)if(SearchKey.key(w.boards[i],w.keys[i])==key){ply=i;break;}
                                if(ply<0)throw new AssertionError("unknown proof path");
                                var path=GameHistory.builder(history);for(int i=1;i<=ply;i++)path.appendPosition(w.boards[i]);
                                proofs.add(new Proof(w.boards[ply].clone(),path.snapshot(),depth,type,score));
                            }
                            super.save(key,depth,type,score,move);
                        }
                    };
                    try(var engine=new ParallelSearch(workers,eval,table,policy,i->{var w=new Checked(eval,current,evaluations);w.game=history;return w;});var driver=new SearchDriver(engine)) {
                        for(int repeat=0;repeat<2;repeat++) {
                            var control=SearchControl.controlled(100000,System.nanoTime(),5000000000L,TimeSource.SYSTEM);
                            var result=driver.search(new SearchRequest(board,history,5,SearchObserver.NONE,control,true));
                            if(result.nodes()!=control.nodes()||result.nodes()!=result.diagnostics().totalEnteredNodes())throw new AssertionError("accounting");
                            if(result.lastCompletedResult()!=null)Phase2QuiescenceTest.legalPv(board,result.lastCompletedResult().principalVariation());
                            if(!Arrays.equals(board,original)||engine.activeSearchThreads()!=0)throw new AssertionError("isolation/drain");
                            observations++;
                        }
                    }
                }
                out.write(gson.toJson(Map.of("id",fields[0],"status","passed","observations",observations,"evaluations",evaluations.get())));out.newLine();out.flush();
            }
            for(var p:proofs) {
                // Ordinary mathematical TT proof is independently nominal/static, never selective.
                var result=new ExactSearch(ExactEvaluator.from(eval)).search(p.board,p.history,p.depth,()->false);
                boolean valid=result.completed() && switch(p.type){case TTable.TYPE_EXACT->p.score==result.score();case TTable.TYPE_LOWER->p.score<=result.score();case TTable.TYPE_UPPER->p.score>=result.score();default->false;};
                if(!valid)throw new AssertionError("false ordinary proof "+p+" reference "+result.score());oracleChecks++;
            }
            out.write(gson.toJson(Map.of("status","passed","observations",observations,"evaluatorChecks",evaluations.get(),"storeAttempts",stores.get(),"independentProofChecks",oracleChecks)));out.newLine();
        }
    }
}
