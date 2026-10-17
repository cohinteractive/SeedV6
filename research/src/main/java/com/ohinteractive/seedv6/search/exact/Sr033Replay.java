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

/** Fixed outcome-blind per-depth reservoir subsample, independent unpruned qsearch
 * at reduced/nominal horizons. Root prefixes are reconstructed, never inferred. */
public final class Sr033Replay {
    public static void main(String[] args)throws Exception {
        var snapshot=CheckpointStore.readSnapshot(Path.of(args[0]),"g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304");
        if(!snapshot.manifest().networkSha256().equals("5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7"))throw new AssertionError("network");
        var evaluation=snapshot.model().evaluation(NnueScoreMapping.V1);var groups=new TreeMap<String,List<JsonObject>>();
        for(String line:Files.readAllLines(Path.of(args[1]))) {
            var row=JsonParser.parseString(line).getAsJsonObject();if(!row.has("kind")||!row.get("kind").getAsString().equals("sample"))continue;
            var sample=row.getAsJsonObject("sample");int depth=sample.get("depth").getAsInt();if(depth>4)continue;
            groups.computeIfAbsent(depth+":"+sample.get("verifyNext"),k->new ArrayList<>()).add(row);
        }
        var selected=new ArrayList<JsonObject>();var random=new Random(330033);
        for(var group:groups.values()){Collections.shuffle(group,random);selected.addAll(group.subList(0,Math.min(12,group.size())));}
        var gson=new Gson();int completed=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            for(var row:selected) {
                var s=row.getAsJsonObject("sample");var board=Board.fromFen(row.get("initialFen").getAsString());var h=GameHistory.builder(board);
                var moves=new ArrayList<String>();String game=row.get("gameMoves").getAsString();if(!game.isBlank())moves.addAll(List.of(game.split(" ")));
                for(var p:s.getAsJsonArray("path"))moves.add(p.getAsString());
                for(String uci:moves){long move=Arrays.stream(ExhaustiveOracle.legalMoves(board)).filter(m->Move.coordinate(m).equals(uci)).findFirst().orElseThrow();board=ExhaustiveOracle.child(board,move);h.appendPosition(board);}
                if(!Fen.fromBoard(board).equals(s.get("fen").getAsString()))throw new AssertionError("path identity");
                int depth=s.get("depth").getAsInt(),r=s.get("reduction").getAsInt(),ply=s.get("ply").getAsInt();
                var result=new LinkedHashMap<String,Object>();result.put("input",row);
                for(int d:new int[]{depth-r,depth}) {
                    var oracle=ExactSearch.quiescenceResearch(ExactEvaluator.from(evaluation));
                    var reference=oracle.search(board,h.snapshot(),d,()->oracle.visitedNodes()>=2_000_000);
                    result.put("depth"+d,reference);
                    if(reference.completed()) {
                        Phase2Oracle.legalPv(board,reference.principalVariation());int value=reference.score();
                        if(value>ExactSearch.MAX_STATIC_SCORE)value-=ply;if(value< -ExactSearch.MAX_STATIC_SCORE)value+=ply;
                        result.put("parentScoreDepth"+d,-value);completed++;
                    }
                }
                out.write(gson.toJson(result));out.newLine();out.flush();
            }
            out.write(gson.toJson(Map.of("status","passed","samples",selected.size(),"completedReferences",completed,"attemptedReferences",selected.size()*2)));out.newLine();
        }
    }
}
