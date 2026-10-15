package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;

/** Independent SR-001A replay of every distinct native D6 decision difference
 * in the completed clean diagnostic, not a new tuning corpus or strength test. */
public final class Sr021Decisions {
    public static void main(String[] args)throws Exception {
        var snapshot=CheckpointStore.readSnapshot(Path.of(args[0]),"g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304");
        if(!snapshot.manifest().networkSha256().equals("5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7"))throw new AssertionError("network");
        var evaluation=snapshot.model().evaluation(NnueScoreMapping.V1);
        var decisions=JsonParser.parseString(Files.readString(Path.of(args[2]))).getAsJsonObject()
                .getAsJsonArray(args.length > 4 ? args[4] : "nativeShallowDepth6Differences");
        var selected=new LinkedHashMap<String,JsonObject>();
        for(var row:decisions)selected.putIfAbsent(row.getAsJsonObject().get("id").getAsString(),row.getAsJsonObject());
        var gson=new GsonBuilder().create();int roots=0;
        try(var out=Files.newBufferedWriter(Path.of(args[3]),StandardOpenOption.CREATE_NEW)) {
            for(String line:Files.readAllLines(Path.of(args[1]))) {
                var fields=line.split("\t",-1);if(!selected.containsKey(fields[0]))continue;
                var board=Board.fromFen(fields[1]);var history=GameHistory.builder(board);
                if(fields.length>2&&!fields[2].isBlank())for(String uci:fields[2].split(" ")) {
                    long move=Arrays.stream(ExhaustiveOracle.legalMoves(board)).filter(m->Move.coordinate(m).equals(uci)).findFirst().orElseThrow();
                    board=ExhaustiveOracle.child(board,move);history.appendPosition(board);
                }
                var realHistory=history.snapshot();var row=selected.get(fields[0]);
                var oracle=ExactSearch.quiescenceResearch(ExactEvaluator.from(evaluation));
                var result=oracle.search(board,realHistory,6,()->oracle.visitedNodes()>=20_000_000);
                if(result.completed())Phase2QuiescenceTest.legalPv(board,result.principalVariation());
                out.write(gson.toJson(Map.of("kind","root","id",fields[0],"reference","independent_SR001A_unpruned",
                        "result",result,"observedBaseline",row.get("baseline"),"observedCandidate",row.get("candidate"))));
                out.newLine();out.flush();
                var moves=new LinkedHashSet<Long>();
                for(String side:new String[]{"baseline","candidate"})moves.add(row.getAsJsonObject(side).get("bestMove").getAsLong());
                for(long move:moves) {
                    if(Arrays.stream(ExhaustiveOracle.legalMoves(board)).noneMatch(m->m==move))throw new AssertionError("observed illegal root move");
                    var child=ExhaustiveOracle.child(board,move);var childHistory=GameHistory.builder(realHistory).appendPosition(child).snapshot();
                    var replay=ExactSearch.quiescenceResearch(ExactEvaluator.from(evaluation));
                    var childResult=replay.search(child,childHistory,5,()->replay.visitedNodes()>=20_000_000);
                    var record=new LinkedHashMap<String,Object>();record.put("kind","root_move");record.put("id",fields[0]);
                    record.put("move",Move.coordinate(move));record.put("reference",childResult);
                    if(childResult.completed()) {
                        Phase2QuiescenceTest.legalPv(child,childResult.principalVariation());
                        int value=childResult.score();if(value>ExactSearch.MAX_STATIC_SCORE)value--;if(value< -ExactSearch.MAX_STATIC_SCORE)value++;
                        if(result.completed()&&(-value>result.score()
                                ||(move==result.bestMove()&&-value!=result.score())))
                            throw new AssertionError("inconsistent independent root/child value");
                        record.put("parentScore",-value);
                    }
                    out.write(gson.toJson(record));out.newLine();out.flush();
                }
                roots++;
            }
            if(roots!=selected.size())throw new AssertionError("missing source roots");
        }
    }
}
