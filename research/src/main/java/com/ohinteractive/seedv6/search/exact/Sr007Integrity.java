package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;

/** Real-model rolling/fresh evaluator and independent static-horizon IID checks.
 * This deliberately does not require selective production to equal an exact oracle. */
public final class Sr007Integrity {
    public static void main(String[] args) throws Exception {
        var snapshot=CheckpointStore.readSnapshot(Path.of(args[0]),
                "g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304");
        if(!snapshot.manifest().networkSha256().equals("5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7"))throw new AssertionError("Wrong network");
        var definition=snapshot.model().evaluation(NnueScoreMapping.V1);var gson=new GsonBuilder().create();
        long[] evaluations={0};int observations=0,oracleChecks=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            for(String line:Files.readAllLines(Path.of(args[1]))) {
                if(line.isBlank()||line.startsWith("#"))continue;
                var f=line.split("\t",-1);var board=Board.fromFen(f[1]);var builder=GameHistory.builder(board);
                if(f.length>2&&!f[2].isBlank())for(String uci:f[2].split(" ")) {
                    long move=Arrays.stream(ExhaustiveOracle.legalMoves(board)).filter(m->Move.coordinate(m).equals(uci)).findFirst().orElseThrow();
                    board=ExhaustiveOracle.child(board,move);builder.appendPosition(board);
                }
                var history=builder.snapshot();var original=board.clone();
                // Exact finite static depth, independent TT-off ordered alpha-beta.
                var reference=new ExactSearch(ExactEvaluator.from(definition)).search(board,history,5,()->false);
                for(var policy:InternalSearchPolicy.values()) {
                    var rolling=ExactEvaluator.from(definition);var fresh=definition.newState(1);
                    var checked=new ExactEvaluator(){
                        public void initialize(long[] b){rolling.initialize(b);}
                        public void child(long[] b,long[] c,int p){rolling.child(b,c,p);}
                        public int evaluate(long[] b,int p){
                            int score=rolling.evaluate(b,p);fresh.initialize(b,0);int expected=fresh.evaluate(b,0);
                            if(score!=expected)throw new AssertionError("Rolling/fresh mismatch at ply "+p);
                            evaluations[0]++;return score;
                        }
                    };
                    var engine=ExactSearch.internalSearchResearch(checked,new TTable(1),policy,null);
                    engine.beginRequest();
                    for(int d=1;d<=5;d++) {
                        var r=engine.search(board,history,d,()->false);
                        if(!r.completed())throw new AssertionError("Incomplete integrity request");
                        Phase2Oracle.legalPv(board,r.principalVariation());observations++;
                    }
                    engine.endRequest();
                    if(policy.iid() && !policy.iir()) {
                        var exact=ExactSearch.internalSearchResearch(checked,new TTable(1),policy,null,false,0)
                                .search(board,history,5,()->false);
                        if(!exact.completed()||exact.selective()||exact.score()!=reference.score())throw new AssertionError("Exact IID mismatch "+f[0]+policy);
                        Phase2Oracle.legalPv(board,exact.principalVariation());oracleChecks++;
                    }
                    if(!Arrays.equals(original,board))throw new AssertionError("Input mutated");
                    out.write(gson.toJson(Map.of("id",f[0],"policy",policy,"status","passed","evaluations",evaluations[0])));out.newLine();out.flush();
                }
            }
            out.write(gson.toJson(Map.of("status","passed","observations",observations,"exactOracleChecks",oracleChecks,"rollingFreshEvaluations",evaluations[0])));out.newLine();
        }
    }
}
