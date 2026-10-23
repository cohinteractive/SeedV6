package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.driver.ConventionalNnueEngine;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

/** Independent TT-off unpruned nominal-qtree replay of frozen real-path predictions.
 * Deliberate horizon disagreement is an observation, not a correctness verdict. */
public final class Cp64SelectivityWitness {
    public static void main(String[] args) throws Exception {
        if(args.length!=5) throw new IllegalArgumentException("model corpus observations NEW-output budget");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));var roots=P3HistoryStudy.roots(Path.of(args[1]));
        long budget=Long.parseLong(args[4]);if(budget<1) throw new IllegalArgumentException("budget");
        var gson=new GsonBuilder().create();
        try(var out=Files.newBufferedWriter(Path.of(args[3]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","arguments",args,"networkSha256",ConventionalNnueEngine.SHA256,
                    "reference","independent recursive TT-off unpruned SR001A qsearch, sampled nominal remaining depth",
                    "limits","No extension-credit equivalence, current-policy recurrence or population error-rate claim")));
            out.newLine();out.flush();
            int rows=0;
            for(String line:Files.readAllLines(Path.of(args[2]))) {
                var observation=JsonParser.parseString(line).getAsJsonObject();
                if(!observation.has("type") || !observation.get("type").getAsString().equals("observation")) continue;
                String id=observation.get("id").getAsString();
                var root=roots.stream().filter(r->r.id().equals(id)).findFirst().orElseThrow();
                int sampleIndex=0;
                for(var element:observation.getAsJsonArray("samples")) {
                    var s=gson.fromJson(element,Cp64SelectivityStudy.Sample.class);
                    long[] board=root.board();var builder=GameHistory.builder(root.history());
                    if(s.path().length!=s.ply() || s.depth()<1 || s.depth()>2) throw new AssertionError("Sample provenance");
                    for(long move:s.path()) {board=ExhaustiveOracle.child(board,move);builder.appendPosition(board);}
                    if(!Arrays.equals(board,s.board())) throw new AssertionError("Real path replay mismatch");
                    int nominal=s.depth(),realPly=s.ply();
                    if(!s.reverse()) {board=ExhaustiveOracle.child(board,s.move());builder.appendPosition(board);nominal--;realPly++;}
                    var oracle=ExactSearch.quiescenceResearch(ExactEvaluator.from(model.evaluation()));
                    long start=System.nanoTime();
                    var result=oracle.search(board,builder.snapshot(),nominal,()->oracle.visitedNodes()>=budget);
                    Phase2Oracle.legalPv(board,result.principalVariation());
                    int score=result.score();
                    if(result.completed()) {
                        if(TranspositionScores.isMateScore(score)) score+=score>0?-realPly:realPly;
                        if(!s.reverse()) score=-score;
                    }
                    var row=new LinkedHashMap<String,Object>();row.put("type","predictionAudit");row.put("id",id);
                    row.put("sampleIndex",sampleIndex++);row.put("sample",s);row.put("reference",result);
                    row.put("elapsedNanos",System.nanoTime()-start);row.put("parentRelativeScore",score);
                    row.put("falseThreshold",result.completed() && (s.reverse()?score<s.beta():score>s.alpha()));
                    out.write(gson.toJson(row));out.newLine();out.flush();rows++;
                }
            }
            if(rows==0) throw new AssertionError("No samples");
            out.write(gson.toJson(Map.of("status","passed","samples",rows)));out.newLine();
        }
    }
    private Cp64SelectivityWitness() {}
}
