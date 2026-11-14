package com.ohinteractive.seedv6.tools.eval;

import com.google.gson.Gson;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.conventional.*;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.BaselineEngine;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.rules.GameHistory;
import java.nio.file.*;
import java.util.*;

/** Warmed fixed-root production search cost, with independently selected evaluators.
 * Counts are decision-dependent; NPS is not an evaluator speedup or strength proof. */
public final class Phase2SearchProbe {
    public static void main(String[] args) throws Exception {
        if(args.length<2 || args.length>4)throw new IllegalArgumentException("roots.fens network [network ...]");
        var roots=Files.readAllLines(Path.of(args[0]));if(roots.size()>16 || roots.isEmpty())throw new IllegalArgumentException("bounded roots");
        var nets=new Phase2Network[args.length-1];var evals=new SearchEvaluation[nets.length];
        for(int i=0;i<nets.length;i++) {
            nets[i]=Phase2Network.load(Path.of(args[i+1]));
            evals[i]=nets[i].evaluation();
        }
        var config=BaselineEngine.configuration(1,32,BaselineEngine.SearchProfile.CLV_C1);var gson=new Gson();
        for(int round=-1;round<2;round++)for(int root=0;root<roots.size();root++)for(int offset=0;offset<nets.length;offset++) {
            int index=Math.floorMod(offset+root+round,nets.length);long[] b=Board.fromFen(roots.get(root));
            try(var search=config.create(evals[index])) {
                Phase2Network.resetProbeCounters();
                long start=System.nanoTime();var control=SearchControl.controlled(200000,start,10_000_000_000L,TimeSource.SYSTEM);
                long nodes=0;SearchResult result=null;
                for(int depth=1;depth<=(round<0?4:7);depth++) {
                    result=search.search(new SearchRequest(b,GameHistory.initial(b),depth,SearchObserver.NONE,control,true));
                    nodes+=result.nodes();if(!result.completed())break;
                }
                long nanos=System.nanoTime()-start;
                if(round>=0) {
                    var row=new LinkedHashMap<String,Object>();row.put("round",round);row.put("root",root);row.put("fen",roots.get(root));
                    row.put("payload",nets[index].payloadSha256());row.put("hidden",nets[index].hidden());row.put("backend",nets[index].headBackend());
                    row.put("configuration",config.identity());row.put("nodes",nodes);row.put("elapsed_ns",nanos);
                    row.put("depth",result.depth());row.put("completed",result.completed());row.put("score",result.score());
                    row.put("diagnostic_counters",Phase2Network.probeCounters());
                    System.out.println(gson.toJson(row));System.out.flush();
                }
            }
        }
    }
}
