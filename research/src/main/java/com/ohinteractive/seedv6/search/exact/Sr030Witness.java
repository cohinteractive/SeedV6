package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;

/** Preserved adverse/positive Phase 1 roots on the current evaluator and leaf policy.
 * Historical repetition identities are restored verbatim and checked against the board.
 * Bounded deeper values are horizon evidence, not game-theoretic truth. */
public final class Sr030Witness {
    public static void main(String[] args) throws Exception {
        if(args.length<3||args.length>4)throw new IllegalArgumentException("store historical-witness.json new-output.jsonl [policies]");
        int[] policies=Arrays.stream((args.length==4?args[3]:"0,1,2,3,4").split(",")).mapToInt(Integer::parseInt).toArray();
        var model=CheckpointStore.readSnapshot(Path.of(args[0]),
                "g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304");
        if(!model.manifest().networkSha256().equals("5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7"))throw new AssertionError();
        var definition=model.model().evaluation(NnueScoreMapping.V1);var gson=new Gson();
        var historical=JsonParser.parseString(Files.readString(Path.of(args[1]))).getAsJsonObject();
        var oldBoard=gson.fromJson(historical.get("board"),long[].class);
        var oldHistory=gson.fromJson(historical.get("history"),GameHistory.class);oldHistory.requireCurrent(oldBoard);
        var promotion=Board.fromFen("8/k1P5/8/1K6/8/8/8/8 w - - 0 1");
        long[][] boards={oldBoard,promotion};GameHistory[] histories={oldHistory,GameHistory.initial(promotion)};
        int observations=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            for(int index=0;index<boards.length;index++)for(int depth=2;depth<=6;depth++) {
                var choices=new TreeSet<Long>();
                for(int policy:policies) {
                    var diag=new CheckExtensionDiagnostics();
                    var engine=ExactSearch.checkExtensionResearch(ExactEvaluator.from(definition),null,policy,
                            ExactSearch.LMR_QUIET,ExactSearch.LMP_WINDOW,diag);
                    var r=engine.search(boards[index],histories[index],depth,()->engine.visitedNodes()>2_000_000);
                    if(r.completed()) {Phase2Oracle.legalPv(boards[index],r.principalVariation());if(r.hasMove())choices.add(r.bestMove());}
                    out.write(gson.toJson(Map.of("id",index==0?"phase1-faster-mate":"promotion-draw","fen",Fen.fromBoard(boards[index]),
                            "history",histories[index],"depth",depth,"policy",policy,"result",r,"extensions",diag)));
                    out.newLine();out.flush();observations++;
                }
                for(long m:choices)for(int extra=1;extra<=2;extra++) {
                    var child=ExhaustiveOracle.child(boards[index],m);
                    var h=GameHistory.builder(histories[index]).appendPosition(child).snapshot();
                    var oracle=ExactSearch.quiescenceResearch(ExactEvaluator.from(definition));
                    var r=oracle.search(child,h,depth-1+extra,()->oracle.visitedNodes()>2_000_000);
                    int parentValue=-r.score();
                    if(r.completed()&&Math.abs(parentValue)>ExactSearch.MAX_STATIC_SCORE)parentValue+=parentValue>0?-1:1;
                    out.write(gson.toJson(Map.of("id",index==0?"phase1-faster-mate":"promotion-draw","parentDepth",depth+extra,
                            "move",Move.coordinate(m),"completed",r.completed(),"parentValue",parentValue,"result",r)));
                    out.newLine();out.flush();observations++;
                }
            }
            out.write(gson.toJson(Map.of("status","passed","observations",observations,"note","Incomplete bounded reference calls remain explicitly incomplete")));out.newLine();
        }
    }
}
