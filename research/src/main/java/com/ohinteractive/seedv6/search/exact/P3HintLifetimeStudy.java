package com.ohinteractive.seedv6.search.exact;

import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnpair2.ProductionModel;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;

/** Single-owner observation of existing hint aging/pressure across actual game
 * prefixes. Reflection reads only; plain/observed results must match throughout.
 * Observer overhead and small-table changes are not playing-strength estimates. */
public final class P3HintLifetimeStudy {
    static final class Observed extends TTable {
        final long[] keys,data;
        int generation;
        long probes,hits,currentHits,olderHits,proofBlocked,deeperBlocked,emptyWrites,sameKeyWrites,collisionWrites;
        long proofAttempts;
        final long[] hitAge=new long[256];
        Observed(int size) throws Exception {
            super(size);keys=array(this,"key");data=array(this,"data");
        }
        static long[] array(TTable table,String name)throws Exception {
            Field f=TTable.class.getDeclaredField(name);f.setAccessible(true);return (long[])f.get(table);
        }
        @Override public void advanceGeneration() {super.advanceGeneration();generation=(generation+1)&255;}
        @Override public long probeHint(long key) {
            probes++;long move=super.probeHint(key);
            if(move!=0) {
                hits++;int age=(generation-(int)(data[(int)key&(data.length-1)]>>>10&255))&255;
                hitAge[age]++;if(age==0)currentHits++;else olderHits++;
            }
            return move;
        }
        @Override public boolean saveHint(long key,int depth,long move) {
            int index=(int)key&(data.length-1);long old=data[index],oldKey=keys[index];
            boolean saved=super.saveHint(key,depth,move);
            if(saved) {
                if((old&(1L<<18))==0)emptyWrites++;
                else if(oldKey==key)sameKeyWrites++;
                else collisionWrites++;
            } else if((old&(1L<<18))!=0 && (old&(1L<<19))==0)proofBlocked++;
            else deeperBlocked++;
            return saved;
        }
        @Override public void save(long key,int depth,int type,int score,long move) {
            proofAttempts++;super.save(key,depth,type,score,move);
        }
        void resetCounters() {
            probes=hits=currentHits=olderHits=proofBlocked=deeperBlocked=emptyWrites=sameKeyWrites=collisionWrites=proofAttempts=0;
            Arrays.fill(hitAge,0);
        }
        Map<String,Object> snapshot() {
            long proof=0,hints=0,current=0;
            for(long value:data) if((value&(1L<<18))!=0) {
                if((value&(1L<<19))==0)proof++;
                else {hints++;if((value>>>10&255)==generation)current++;}
            }
            var result=new LinkedHashMap<String,Object>();
            result.put("entries",data.length);result.put("occupiedProof",proof);result.put("occupiedHints",hints);
            result.put("currentGenerationHints",current);result.put("probes",probes);result.put("hits",hits);
            result.put("currentHits",currentHits);result.put("olderHits",olderHits);result.put("hitAge",hitAge.clone());
            result.put("proofBlocked",proofBlocked);result.put("deeperBlocked",deeperBlocked);
            result.put("emptyWrites",emptyWrites);result.put("sameKeyWrites",sameKeyWrites);result.put("collisionWrites",collisionWrites);
            result.put("proofSaveAttempts",proofAttempts);return result;
        }
    }

    static ExactSearch search(SearchEvaluation e,TTable table,int policy) {
        return P3InformationBaseline.engine(e,table,null).withHistoryResearch((policy&1)!=0,(policy&2)!=0,null)
                .withCompletedHints(true,null);
    }

    /** model lines.tsv new-output.jsonl; lines: id<TAB>initial FEN<TAB>legal moves.
     * Each color owns its private engine; starts at ply16, ends before ply60.
     * Two representations, policies4/7, D1..5, 250k recursive entries/invocation. */
    public static void main(String[] args)throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("model lines.tsv new-output.jsonl");
        var e=SearchEvaluation.brnPair2(ProductionModel.load(Path.of(args[0])));var gson=new GsonBuilder().create();
        int count=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            for(String line:Files.readAllLines(Path.of(args[1]))) {
                if(line.isBlank()||line.startsWith("#"))continue;
                var fields=line.split("\t",-1);
                for(int size:new int[]{64,1})for(int policy:new int[]{4,7}) {
                    long[] board=Board.fromFen(fields[1]);var history=GameHistory.builder(board);
                    var tables=new Observed[]{new Observed(size),new Observed(size)};
                    var observed=new ExactSearch[]{search(e,tables[0],policy),search(e,tables[1],policy)};
                    var plain=new ExactSearch[]{search(e,new TTable(size),policy),search(e,new TTable(size),policy)};
                    String[] moves=fields[2].split(" ");
                    for(int ply=0;ply<Math.min(60,moves.length);ply++) {
                        int side=Board.player((int)board[Board.STATUS]);
                        if(ply>=16) {
                            var a=plain[side];var b=observed[side];var t=tables[side];
                            a.beginRequest();b.beginRequest();t.resetCounters();long nodes=0;int completedDepth=0;
                            try {for(int depth=1;depth<=5;depth++) {
                                var x=a.search(board,history.snapshot(),depth,()->a.visitedNodes()>=250000);
                                var y=b.search(board,history.snapshot(),depth,()->b.visitedNodes()>=250000);
                                P3InformationBaseline.same(x,y);Phase2Oracle.legalPv(board,y.principalVariation());nodes+=y.nodes();
                                if(!y.completed())break;completedDepth=depth;
                            }}finally {a.endRequest();b.endRequest();}
                            var row=new LinkedHashMap<String,Object>();row.put("game",fields[0]);row.put("ply",ply);
                            row.put("side",side);row.put("requestedMiB",size);row.put("policy",policy);
                            row.put("completedDepth",completedDepth);row.put("recursiveNodes",nodes);row.put("tt",t.snapshot());
                            out.write(gson.toJson(row));out.newLine();out.flush();count++;
                        }
                        final String uci=moves[ply];
                        long move=Arrays.stream(ExhaustiveOracle.legalMoves(board)).filter(m->Move.coordinate(m).equals(uci)).findFirst().orElseThrow();
                        board=ExhaustiveOracle.child(board,move);history.appendPosition(board);
                    }
                    System.out.println("completed "+fields[0]+" MiB="+size+" policy="+policy);
                }
            }
            out.write(gson.toJson(Map.of("status","passed","observations",count)));out.newLine();
        }
    }
    private P3HintLifetimeStudy() {}
}
