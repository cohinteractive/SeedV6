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

/** Frozen-model rolling/fresh evaluator parity and sampled independent ordinary
 * proofs under every tactical extension policy. Instrumented correctness, never timing evidence. */
public final class Sr032Integrity {
    record Proof(long[] board,GameHistory history,int depth,int type,int score,TacticalExtension policy) {}
    public static void main(String[] args)throws Exception {
        var snapshot=CheckpointStore.readSnapshot(Path.of(args[0]),"g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304");
        if(!snapshot.manifest().networkSha256().equals("5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7"))throw new AssertionError("network");
        var eval=snapshot.model().evaluation(NnueScoreMapping.V1);var evaluations=new AtomicLong();
        var current=new ThreadLocal<Sr036Integrity.Checked>();
        var proofs=Collections.synchronizedList(new ArrayList<Proof>());
        var stores=new EnumMap<TacticalExtension,AtomicLong>(TacticalExtension.class);
        for(var p:TacticalExtension.values())stores.put(p,new AtomicLong());
        var gson=new GsonBuilder().create();int observations=0,proofChecks=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            for(String line:Files.readAllLines(Path.of(args[1]))) {
                if(line.isBlank()||line.startsWith("#"))continue;
                var fields=line.split("\t",-1);var board=Board.fromFen(fields[1]);var builder=GameHistory.builder(board);
                if(fields.length>2&&!fields[2].isBlank())for(String uci:fields[2].split(" ")) {
                    long move=Arrays.stream(ExhaustiveOracle.legalMoves(board)).filter(m->Move.coordinate(m).equals(uci)).findFirst().orElseThrow();
                    board=ExhaustiveOracle.child(board,move);builder.appendPosition(board);
                }
                final var history=builder.snapshot();var original=board.clone();
                for(var policy:TacticalExtension.values())for(int workers:new int[]{1,4}) {
                    var table=new TTable(1) {
                        @Override public void save(long key,int depth,int type,int score,long move) {
                            long n=stores.get(policy).incrementAndGet();
                            if((n<=30 || n%7==0) && depth<=4 && n<1400) {
                                var w=current.get();if(w==null)throw new AssertionError("unowned TT write");
                                int ply=-1;
                                for(int i=0;i<w.keys.length;i++)if(SearchKey.key(w.boards[i],w.keys[i])==key){ply=i;break;}
                                if(ply<0)throw new AssertionError("unknown proof path");
                                var path=GameHistory.builder(history);for(int i=1;i<=ply;i++)path.appendPosition(w.boards[i]);
                                proofs.add(new Proof(w.boards[ply].clone(),path.snapshot(),depth,type,score,policy));
                            }
                            super.save(key,depth,type,score,move);
                        }
                    };
                    var sinks=new TacticalExtensionDiagnostics[workers];for(int w=0;w<workers;w++)sinks[w]=new TacticalExtensionDiagnostics();
                    if(workers==1) {
                        var checked=new Sr036Integrity.Checked(eval,current,evaluations);checked.game=history;
                        var exact=ExactSearch.iterationReuseResearch(checked,table,IterationReuse.EFFORT,true,null).withSeeResearch(SeePolicy.QXRAY,null).withTacticalExtension(policy,sinks[0]);
                        exact.beginRequest();
                        try {
                            for(int depth=1;depth<=5;depth++) {
                                var result=exact.search(board,history,depth,()->exact.visitedNodes()>=20000);
                                if(result.completed())Phase2Oracle.legalPv(board,result.principalVariation());
                                observations++;if(!result.completed())break;
                            }
                        } finally {exact.endRequest();}
                        exact.newGame();
                        var reused=exact.search(board,history,2,()->exact.visitedNodes()>=20000);
                        if(reused.completed())Phase2Oracle.legalPv(board,reused.principalVariation());observations++;
                    } else {
                        try(var engine=new ParallelSearch(workers,eval,table,ConcurrencyPolicy.ROOT_REUSE,
                                i->{var checked=new Sr036Integrity.Checked(eval,current,evaluations);checked.game=history;return checked;})
                                .withSeeResearch(SeePolicy.QXRAY,null).withTacticalExtension(policy,i->sinks[i]);var driver=new SearchDriver(engine)) {
                            for(int repeat=0;repeat<2;repeat++) {
                                var control=SearchControl.controlled(50000,System.nanoTime(),10000000000L,TimeSource.SYSTEM);
                                var result=driver.search(new SearchRequest(board,history,5,SearchObserver.NONE,control,true));
                                if(result.nodes()!=control.nodes()||result.nodes()!=result.diagnostics().totalEnteredNodes())throw new AssertionError("accounting");
                                if(result.lastCompletedResult()!=null)Phase2Oracle.legalPv(board,result.lastCompletedResult().principalVariation());
                                if(engine.activeSearchThreads()!=0)throw new AssertionError("drain");observations++;driver.newGame();
                            }
                        }
                    }
                    if(!Arrays.equals(board,original))throw new AssertionError("root isolation");
                    out.write(gson.toJson(Map.of("id",fields[0],"policy",policy,"workers",workers,"status","passed",
                            "observations",observations,"evaluatorChecks",evaluations.get(),"extensions",Arrays.stream(sinks).mapToLong(d->d.extensions).toArray(),
                            "retries",Arrays.stream(sinks).mapToLong(d->d.fullResearches).toArray())));out.newLine();out.flush();
                }
            }
            for(var p:proofs) {
                var result=new ExactSearch(ExactEvaluator.from(eval)).search(p.board,p.history,p.depth,()->false);
                boolean valid=result.completed()&&switch(p.type){case TTable.TYPE_EXACT->p.score==result.score();case TTable.TYPE_LOWER->p.score<=result.score();case TTable.TYPE_UPPER->p.score>=result.score();default->false;};
                if(!valid)throw new AssertionError("false ordinary proof "+p+" reference "+result.score());proofChecks++;
            }
            var counts=new LinkedHashMap<String,Long>();for(var p:TacticalExtension.values())counts.put(p.name(),stores.get(p).get());
            out.write(gson.toJson(Map.of("status","passed","observations",observations,"evaluatorChecks",evaluations.get(),
                    "storeAttempts",counts,"independentProofChecks",proofChecks)));out.newLine();
        }
    }
}
