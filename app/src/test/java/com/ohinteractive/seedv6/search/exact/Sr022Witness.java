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

/** Full-history local alternatives at nominal and two deeper horizons for
 * the largest sparse, largest ordinary, and changed-root omission witnesses. */
public final class Sr022Witness {
    public static void main(String[] args) throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("store replay.jsonl new-output.jsonl");
        var model=CheckpointStore.readSnapshot(Path.of(args[0]),
                "g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304");
        if(!model.manifest().networkSha256().equals("5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7"))throw new AssertionError();
        var definition=model.model().evaluation(NnueScoreMapping.V1);var gson=new Gson();int selected=0,observations=0;
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)){
            for(String line:Files.readAllLines(Path.of(args[1]))){
                var r=JsonParser.parseString(line).getAsJsonObject();
                if(!r.has("aboveAlpha")||!r.get("aboveAlpha").getAsBoolean()||r.get("policy").getAsInt()!=3)continue;
                var s=gson.fromJson(r.get("sample"),Sr022Replay.Sample.class);
                String root=r.get("root").getAsString(),omitted=s.path().getLast();
                if(!(root.equals("count-pawn")&&omitted.equals("g3f4"))&&!(root.equals("opening-05")&&omitted.equals("f7f5"))
                        &&!(root.equals("opening-14")&&omitted.equals("c3a4")))continue;
                selected++;
                var b=Board.fromFen(r.get("initialFen").getAsString());var history=GameHistory.builder(b);
                var prefix=new ArrayList<String>();String game=r.get("gameMoves").getAsString();
                if(!game.isBlank())prefix.addAll(List.of(game.split(" ")));
                prefix.addAll(s.path().subList(0,s.path().size()-1));
                for(String uci:prefix){
                    long move=Arrays.stream(ExhaustiveOracle.legalMoves(b)).filter(m->Move.coordinate(m).equals(uci)).findFirst().orElseThrow();
                    b=ExhaustiveOracle.child(b,move);history.appendPosition(b);
                }
                for(int d=s.depth();d<=s.depth()+2;d++)for(long move:ExhaustiveOracle.legalMoves(b)){
                    var child=ExhaustiveOracle.child(b,move);var h=GameHistory.builder(history.snapshot()).appendPosition(child).snapshot();
                    if(Move.coordinate(move).equals(omitted)){
                        if(!Arrays.equals(child,s.child())||h.size()!=s.history().size())throw new AssertionError("Replay identity drift");
                        for(int i=0;i<h.size();i++)if(h.keyAt(i)!=s.history().keyAt(i))throw new AssertionError("History drift");
                    }
                    var search=ExactSearch.quiescenceResearch(ExactEvaluator.from(definition));
                    var result=search.search(child,h,d,()->search.visitedNodes()>=5_000_000);
                    int value=result.score();if(value>ExactSearch.MAX_STATIC_SCORE)value-=s.ply();
                    if(value< -ExactSearch.MAX_STATIC_SCORE)value+=s.ply();
                    out.write(gson.toJson(Map.of("root",root,"parentFen",Fen.fromBoard(b),"parentDepth",d+1,"omitted",omitted,
                            "move",Move.coordinate(move),"parentValue",-value,"result",result,"originalAlpha",s.alpha())));
                    out.newLine();out.flush();observations++;
                }
            }
            if(selected!=3)throw new AssertionError("Expected three selected witnesses");
            out.write(gson.toJson(Map.of("status","passed","witnesses",selected,"observations",observations)));out.newLine();
        }
    }
}
