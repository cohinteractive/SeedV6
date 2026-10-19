package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.brnpair2.ProductionModel;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;
import static com.ohinteractive.seedv6.search.exact.FlatSearchConfig.Feature.*;

/** Diagnostic removals to explain an observed finite-horizon disagreement; no strength ablation. */
public final class ConstructionWitness {
    public static void main(String[] args) throws Exception {
        if(args.length!=3) throw new IllegalArgumentException("model corpus new-output");
        var evaluation=SearchEvaluation.brnPair2(ProductionModel.load(Path.of(args[0])));
        var root=P3HistoryStudy.roots(Path.of(args[1])).stream().filter(r->r.id().equals("opening-03")).findFirst().orElseThrow();
        var configs=new LinkedHashMap<String,FlatSearchConfig>();
        configs.put("candidate",FlatSearchConfig.candidate());configs.put("flat-policy4",FlatSearchConfig.policy4());configs.put("exact",FlatSearchConfig.exact());
        for(var f:FlatSearchConfig.Feature.values()) {
            var config=FlatSearchConfig.candidate();
            if(f==LMR) config=config.with(IMPROVING,false).with(CAUTION,false);
            configs.put("without-"+f,config.with(f,false));
        }
        configs.put("unpruned-extended",FlatSearchConfig.exact().with(CHECK_EXTENSION,true).with(RECAPTURE_EXTENSION,true));
        configs.put("unpruned-ordered",FlatSearchConfig.ordered().with(QXRAY,false).with(LMR,false).with(LMP,false));
        var gson=new GsonBuilder().create();
        try(var out=Files.newBufferedWriter(Path.of(args[2]),StandardOpenOption.CREATE_NEW)) {
            for(int depth:new int[]{2,3,4}) {
                var oracle=ExactSearch.quiescenceResearch(ExactEvaluator.from(evaluation));
                var reference=oracle.search(root.board(),root.history(),depth,()->oracle.visitedNodes()>=500000);
                out.write(gson.toJson(Map.of("type","reference","depth",depth,"outcome",reference)));out.newLine();out.flush();
                for(var variant:configs.entrySet()) {
                    var search=ExactSearch.constructionResearch(ExactEvaluator.from(evaluation),new TTable(1),variant.getValue());
                    var r=search.search(root.board(),root.history(),depth,()->search.visitedNodes()>=500000);
                    Phase2Oracle.legalPv(root.board(),r.principalVariation());
                    out.write(gson.toJson(Map.of("type","variant","depth",depth,"name",variant.getKey(),"identity",variant.getValue().identity(),
                            "outcome",r,"counters",search.constructionCounters())));out.newLine();out.flush();
                }
            }
            out.write("{\"status\":\"passed\"}\n");
        }
    }
    private ConstructionWitness() {}
}
