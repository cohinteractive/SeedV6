package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;

/** Independent unpruned-qsearch PVS comparison of the changed native depth-six
 * choices. Full history retained; bounded incomplete references remain censored. */
public final class Sr032Decisions {
    public static void main(String[] args)throws Exception {
        var snapshot=CheckpointStore.readSnapshot(Path.of(args[0]),"g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304");
        if(!snapshot.manifest().networkSha256().equals("5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7"))throw new AssertionError("network");
        var evaluation=snapshot.model().evaluation(NnueScoreMapping.V1);
        var chosen=new TreeMap<String,Map<String,Long>>();
        for(String line:Files.readAllLines(Path.of(args[2]))) {
            var row=JsonParser.parseString(line).getAsJsonObject();if(!row.has("config"))continue;
            if(row.get("round").getAsInt()!=0||!row.get("budget").getAsString().equals("depth6")||!row.get("config").getAsString().endsWith(":1"))continue;
            chosen.computeIfAbsent(row.get("id").getAsString(),k->new TreeMap<>()).put(row.get("config").getAsString(),row.getAsJsonObject("outcome").getAsJsonObject("lastCompletedResult").get("bestMove").getAsLong());
        }
        int count=0;var gson=new Gson();
        try(var out=Files.newBufferedWriter(Path.of(args[3]),StandardOpenOption.CREATE_NEW)) {
            for(String line:Files.readAllLines(Path.of(args[1]))) {
                if(line.isBlank()||line.startsWith("#"))continue;
                var f=line.split("\t",-1);var choices=chosen.get(f[0]);
                if(choices==null||new HashSet<>(choices.values()).size()==1)continue;
                var board=Board.fromFen(f[1]);var h=GameHistory.builder(board);
                if(f.length>2&&!f[2].isBlank())for(String uci:f[2].split(" ")) {
                    long move=Arrays.stream(ExhaustiveOracle.legalMoves(board)).filter(m->Move.coordinate(m).equals(uci)).findFirst().orElseThrow();
                    board=ExhaustiveOracle.child(board,move);h.appendPosition(board);
                }
                for(long move:new TreeSet<>(choices.values()))for(int depth:new int[]{5,6,7}) {
                    var child=ExhaustiveOracle.child(board,move);var history=GameHistory.builder(h.snapshot());history.appendPosition(child);
                    var search=ExactSearch.quiescencePvsResearch(ExactEvaluator.from(evaluation),new TTable(4),ExactSearch.QSEARCH_BASELINE);
                    var result=search.search(child,history.snapshot(),depth-1,()->search.visitedNodes()>=2_000_000);
                    int score=-result.score();if(Math.abs(score)>ExactSearch.MAX_STATIC_SCORE)score-=Integer.signum(score);
                    if(result.completed())Phase2Oracle.legalPv(child,result.principalVariation());
                    out.write(gson.toJson(Map.of("root",f[0],"choices",choices,"move",Move.coordinate(move),"depth",depth,"completed",result.completed(),"rootScore",score,"nodes",result.nodes(),"elapsedNanos",result.elapsedNanos()))+"\n");out.flush();count++;
                }
            }
            out.write(gson.toJson(Map.of("status","passed","comparisons",count))+"\n");
        }
    }
}
