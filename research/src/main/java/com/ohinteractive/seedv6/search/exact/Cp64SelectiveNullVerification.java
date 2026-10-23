package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import com.ohinteractive.seedv6.search.driver.ConventionalNnueEngine;
import com.ohinteractive.seedv6.search.tt.TTable;

/** Economic/reliability sensitivity only; no null integration or proof authority. */
public final class Cp64SelectiveNullVerification {
    public static void main(String[] args) throws Exception {
        if(args.length!=4)throw new IllegalArgumentException("model corpus original-probes NEW-output");
        var model=ConventionalNnueEngine.load(Path.of(args[0]));var roots=P3HistoryStudy.roots(Path.of(args[1]));
        var gson=new GsonBuilder().create();int count=0;
        try(var out=Files.newBufferedWriter(Path.of(args[3]),StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type","identity","arguments",args,"networkSha256",ConventionalNnueEngine.SHA256,
                    "policy","Full Construction V1 legalD-2 verification; fresh private Hash64, restricted legal parent, ADAPTIVE16 not used for this unit window",
                    "limits","New selective verifier hypothesis, not mathematical proof; inherited ordering/extension/TT context is not reconstructed")));out.newLine();
            for(String line:Files.readAllLines(Path.of(args[2]))) {
                var row=JsonParser.parseString(line).getAsJsonObject();
                if(!row.has("type") || !row.get("type").getAsString().equals("probe"))continue;
                var s=gson.fromJson(row.get("sample"),Cp64SelectivityLab.Sample.class);
                if(!s.kind().equals("null-opportunity"))continue;
                String id=row.get("id").getAsString();var root=roots.stream().filter(r->r.id().equals(id)).findFirst().orElseThrow();
                var c=Cp64SelectivityProbe.context(root,s);
                var owner=ExactSearch.constructionResearch(ExactEvaluator.from(model.evaluation()),new TTable(64),FlatSearchConfig.candidate());
                long start=System.nanoTime();
                var result=owner.searchRootMove(c.parent(),c.parentHistory(),s.depth()-1,c.lastMove(),-s.beta(),1-s.beta(),true,
                        ()->owner.visitedNodes()>=200000);
                long elapsed=System.nanoTime()-start;Phase2Oracle.legalPv(c.parent(),result.principalVariation());
                out.write(gson.toJson(Map.of("type","selective-verification","id",id,"sampleIndex",row.get("sampleIndex"),
                        "sample",s,"result",result,"parentRelativeScore",-result.score(),"elapsedNanos",elapsed)));
                out.newLine();out.flush();
                if(!owner.search(c.parent(),c.parentHistory(),1,()->false).completed())throw new AssertionError("Owner reuse");count++;
            }
            if(count!=48)throw new AssertionError("Frozen48-target selection");
            out.write(gson.toJson(Map.of("status","passed","samples",count)));out.newLine();
        }
    }
    private Cp64SelectiveNullVerification() {}
}
