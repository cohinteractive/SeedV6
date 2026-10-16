package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;

/** Explicit real-model independent TT-off qsearch comparisons; outside routine tests. */
public final class Sr006ModelOracle {
    public static void main(String[] args) throws Exception {
        var snapshot=CheckpointStore.readSnapshot(Path.of(args[0]),
                "g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304");
        if(!snapshot.manifest().networkSha256().equals("5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7"))
            throw new AssertionError("Wrong network");
        var evaluation=snapshot.model().evaluation(NnueScoreMapping.V1);
        var gson=new GsonBuilder().create(); int comparisons=0;
        try(var output=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            for(String line:Files.readAllLines(Path.of(args[1]))) {
                if(line.isBlank()||line.startsWith("#")) continue;
                var fields=line.split("\t",-1); var board=Board.fromFen(fields[1]); var history=GameHistory.builder(board);
                if(fields.length>2&&!fields[2].isBlank()) for(String uci:fields[2].split(" ")) {
                    long move=Arrays.stream(ExhaustiveOracle.legalMoves(board))
                            .filter(m->Move.coordinate(m).equals(uci)).findFirst().orElseThrow();
                    board=ExhaustiveOracle.child(board,move); history.appendPosition(board);
                }
                final var root=board; final var prefix=history.snapshot();
                int[] scores=new int[4];
                for(int d=1;d<=3;d++) {
                    var expected=ExactSearch.quiescenceResearch(ExactEvaluator.from(evaluation),0)
                            .search(root,prefix,d,ExactSearch.NEVER_CANCELLED);
                    if(!expected.completed()) throw new AssertionError("Incomplete oracle");
                    scores[d]=expected.score();
                }
                for(var policy:AspirationSearch.Policy.values()) {
                    int[] checked={0};
                    try(var driver=new SearchDriver(new AspirationSearch(ExactSearchAdapter.unprunedQuiescence(evaluation),policy))) {
                        var r=driver.search(new SearchRequest(root,prefix,3,new SearchObserver() {
                            public void onIterationCompleted(IterationSnapshot iteration) {
                                if(iteration.score()!=scores[iteration.depth()])
                                    throw new AssertionError(fields[0]+policy+" depth "+iteration.depth());
                                Phase2Oracle.legalPv(root,iteration.result().principalVariation()); checked[0]++;
                            }
                        }));
                        if(!r.targetDepthCompleted()) throw new AssertionError("Incomplete aspiration");
                        output.write(gson.toJson(Map.of("id",fields[0],"policy",policy,"comparisons",checked[0],
                                "nodes",r.nodes(),"score",r.lastCompletedResult().score())));output.newLine();output.flush();
                        comparisons+=checked[0];
                    }
                }
            }
            output.write(gson.toJson(Map.of("status","passed","comparisons",comparisons)));output.newLine();
        }
    }
}
