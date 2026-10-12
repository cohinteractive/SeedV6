package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.Gson;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;

/** Bounded real-model cold-workspace, transition and LMR/LMP interaction audit. */
public final class Sr030Integrity {
    public static void main(String[] args) throws Exception {
        if(args.length!=3&&args.length!=5)throw new IllegalArgumentException("store corpus.tsv new-output.jsonl [first-policy last-policy]");
        var model=CheckpointStore.readSnapshot(Path.of(args[0]),
                "g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304");
        if(!model.manifest().networkSha256().equals("5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7"))throw new AssertionError();
        var definition=model.model().evaluation(NnueScoreMapping.V1);var gson=new Gson();
        long[] calls={0,0,0};int observations=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            for(String line:Files.readAllLines(Path.of(args[1]))) {
                if(line.isBlank()||line.startsWith("#"))continue;
                var fields=line.split("\t",-1);var b=Board.fromFen(fields[1]);var h=GameHistory.builder(b);
                if(fields.length>2&&!fields[2].isBlank())for(String uci:fields[2].split(" ")) {
                    long m=Arrays.stream(ExhaustiveOracle.legalMoves(b)).filter(x->Move.coordinate(x).equals(uci)).findFirst().orElseThrow();
                    b=ExhaustiveOracle.child(b,m);h.appendPosition(b);
                }
                for(int policy=args.length==5?Integer.parseInt(args[3]):1;policy<=(args.length==5?Integer.parseInt(args[4]):4);policy++)for(int mask=0;mask<3;mask++) {
                    var state=ExactEvaluator.from(definition);var prepared=new long[257][6];
                    var eval=new ExactEvaluator() {
                        public void initialize(long[] board){System.arraycopy(board,0,prepared[0],0,6);state.initialize(board);}
                        public void child(long[] parent,long[] child,int ply,int qply){
                            assertBoard(parent,prepared[ply]);System.arraycopy(child,0,prepared[ply+1],0,6);
                            state.child(parent,child,ply,qply);calls[2]++;
                        }
                        public int evaluate(long[] board,int ply){
                            assertBoard(board,prepared[ply]);int score=state.evaluate(board,ply);calls[0]++;
                            if(calls[0]%127==0){
                                var cold=definition.newState(1);cold.initialize(board,0);
                                if(cold.evaluate(board,0)!=score)throw new AssertionError("Cold evaluator disagreement");calls[1]++;
                            }
                            return score;
                        }
                    };
                    var engine=ExactSearch.checkExtensionResearch(eval,new TTable(1),policy,mask==0?0:ExactSearch.LMR_QUIET,
                            mask==2?ExactSearch.LMP_WINDOW:0,null);
                    var r=engine.search(b,h.snapshot(),4,()->engine.visitedNodes()>=500_000);
                    if(r.completed())Phase2QuiescenceTest.legalPv(b,r.principalVariation());
                    if(mask==0&&r.completed()){
                        var control=ExactSearch.checkExtensionResearch(ExactEvaluator.from(definition),null,policy,0,0,null);
                        var oracle=control.search(b,h.snapshot(),4,()->control.visitedNodes()>=500_000);
                        if(!oracle.completed()||r.score()!=oracle.score())throw new AssertionError("Unpruned TT differential");
                    }
                    out.write(gson.toJson(Map.of("id",fields[0],"policy",policy,"interaction",mask,
                            "result",r,"coldChecks",calls[1])));out.newLine();out.flush();observations++;
                }
            }
            out.write(gson.toJson(Map.of("status","passed","observations",observations,
                    "evaluations",calls[0],"coldChecks",calls[1],"preparedTransitions",calls[2])));out.newLine();
        }
    }
    static void assertBoard(long[] a,long[] b){if(!Arrays.equals(a,b))throw new AssertionError("Unprepared evaluator slot");}
}
