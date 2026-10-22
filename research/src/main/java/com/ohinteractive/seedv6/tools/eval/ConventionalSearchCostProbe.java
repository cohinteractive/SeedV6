package com.ohinteractive.seedv6.tools.eval;

import com.google.gson.Gson;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnpair2.ProductionModel;
import com.ohinteractive.seedv6.core.nnue.conventional.ConventionalNetwork;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.ExactSearchAdapter;
import com.ohinteractive.seedv6.search.driver.ProductionSearch;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

/** Bounded matched-root cost diagnostic; no policy adoption or strength estimate.
 * Run only under nnue-control.cmd run. One worker; fresh 32MiB TT per root. */
public final class ConventionalSearchCostProbe {
    public static void main(String[] args) throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("network.cnnue frozen.fens");
        var network=ConventionalNetwork.load(Path.of(args[0]));
        var definitions=new SearchEvaluation[]{SearchEvaluation.conventional(network),
                SearchEvaluation.brnPair2(ProductionModel.bundled())};
        var fens=Files.readAllLines(Path.of(args[1]));
        if(fens.isEmpty()||fens.size()>16)throw new IllegalArgumentException("Use 1..16 fixed roots");
        var gson=new Gson();String[] names={"NNUE_UNPRUNED","GEN32_UNPRUNED","GEN32_POLICY4"};
        // One unreported warm-up round, then two measured rounds with rotated order.
        for(int round=-1;round<2;round++)for(int root=0;root<fens.size();root++)
            for(int offset=0;offset<3;offset++) {
                int mode=(offset+root+round+3)%3;
                var evaluation=definitions[mode==0?0:1];
                long[] board=Board.fromFen(fens.get(root));
                try(var search=mode==2?ProductionSearch.create(1,evaluation,32):
                        ExactSearchAdapter.unprunedQuiescence(evaluation,new TTable(32))) {
                    long start=System.nanoTime(),nodes=0,qnodes=0;
                    var control=SearchControl.controlled(250000,start,1_000_000_000L,TimeSource.SYSTEM);
                    SearchResult result=null;
                    for(int depth=1;depth<=(round<0?3:4);depth++) {
                        result=search.search(new SearchRequest(board,GameHistory.initial(board),depth,
                                SearchObserver.NONE,control,true));
                        nodes+=result.nodes();qnodes+=result.diagnostics().worker().nodes().qNodes();
                        if(!result.completed())break;
                    }
                    long elapsed=System.nanoTime()-start;
                    if(round>=0) {
                        var row=new LinkedHashMap<String,Object>();
                        row.put("mode",names[mode]);row.put("round",round);row.put("root",root);
                        row.put("fen",fens.get(root));row.put("completed",result.completed());
                        row.put("depth",result.depth());row.put("score",result.score());
                        row.put("nodes",nodes);row.put("qnodes",qnodes);row.put("elapsed_ns",elapsed);
                        row.put("node_limit",250000);row.put("time_limit_ms",1000);
                        row.put("network_payload",network.payloadSha256());
                        System.out.println(gson.toJson(row));System.out.flush();
                    }
                }
            }
    }
    private ConventionalSearchCostProbe() {}
}
