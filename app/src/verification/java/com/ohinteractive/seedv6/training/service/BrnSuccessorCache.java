package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.brn3.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.exact.ExactEvaluator;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.nio.file.*;
import java.util.*;

/** Search-local topology experiment. No production switches or shared engine edits. */
public final class BrnSuccessorCache {
    static final class Evaluation implements ExactEvaluator {
        final Brn3Model model;final String mode;final Brn3Workspace[] work;final long[][] previous;
        long calls,children;int initialized;
        Evaluation(Brn3Model model,String mode) {
            this.model=model;this.mode=mode;int size=mode.equals("rolling")?1:mode.equals("ply")?128:4;
            work=new Brn3Workspace[size];previous=new long[size][4];
        }
        @Override public void child(long[] parent,long[] child,int parentPly){children++;}
        @Override public int evaluate(long[] board,int ply) {
            calls++;int selected=0;
            if(mode.equals("ply"))selected=ply;
            if(mode.equals("nearest4")) {
                int minimum=65;
                for(int i=0;i<work.length;i++) {
                    if(work[i]==null){if(minimum>8)selected=i;break;}
                    long changed=0;for(int p=0;p<4;p++)changed|=previous[i][p]^board[p];
                    int distance=Long.bitCount(changed);if(distance<minimum){minimum=distance;selected=i;}
                }
            }
            if(work[selected]==null){work[selected]=model.newWorkspace();initialized++;}
            int value=Brn3Model.score(work[selected].evaluatePawns(board,.25));
            System.arraycopy(board,0,previous[selected],0,4);return value;
        }
        Map<String,Object> counters() {
            long rebuilds=0,updates=0,repeats=0;for(var w:work)if(w!=null){rebuilds+=w.rebuilds;updates+=w.updates;repeats+=w.repeats;}
            return Map.of("calls",calls,"preparedChildren",children,"workspaces",initialized,"rebuilds",rebuilds,"updates",updates,"repeats",repeats);
        }
    }
    static List<Map<String,Object>> run(Brn3Model model,List<BrnResearchData.Example> data,String mode,int depth) {
        var results=new ArrayList<Map<String,Object>>();
        for(int i=0;i<data.size();i++) {
            var board=data.get(i).board();var evaluation=new Evaluation(model,mode);
            try(var driver=new SearchDriver(new ExactSearchAdapter(evaluation,new TTable(4)))) {
                long start=System.nanoTime();var control=SearchControl.controlled(5_000_000,start,5_000_000_000L,TimeSource.SYSTEM);
                var result=driver.search(new SearchRequest(board,GameHistory.initial(board),depth,SearchObserver.NONE,control,true));
                double seconds=(System.nanoTime()-start)/1e9;var last=result.lastCompletedResult();
                var r=new LinkedHashMap<String,Object>();r.put("index",i);r.put("seconds",seconds);r.put("complete",result.targetDepthCompleted());
                r.put("nodes",control.nodes());r.put("score",last.score());r.put("move",Long.toUnsignedString(last.bestMove()));r.put("cache",evaluation.counters());results.add(r);
            }
        }
        return results;
    }
    public static void main(String[] args)throws Exception {
        Path dataPath=Path.of(args[0]),modelPath=Path.of(args[1]),out=Path.of(args[2]);
        if(Files.exists(out))throw new IllegalArgumentException("Use new output");
        var data=BrnResearchData.read(dataPath,false);var model=BrnLearningProbe.model(modelPath);
        var trials=new ArrayList<Map<String,Object>>();
        for(String mode:List.of("rolling","ply","nearest4"))run(model,data.validation().subList(0,8),mode,4);
        for(int round=0;round<3;round++)for(int n=0;n<3;n++) {
            String mode=List.of("rolling","ply","nearest4").get((n+round)%3);
            var roots=run(model,data.validation().subList(0,64),mode,5);
            trials.add(Map.of("round",round,"mode",mode,"seconds",roots.stream().mapToDouble(r->(double)r.get("seconds")).sum(),"roots",roots));
            System.out.println(mode+" "+trials.getLast().get("seconds"));
        }
        var reference=(List<?>)trials.getFirst().get("roots");
        for(var trial:trials) {
            var roots=(List<?>)trial.get("roots");
            for(int i=0;i<reference.size();i++)for(String key:List.of("complete","nodes","score","move"))
                if(!Objects.equals(((Map<?,?>)reference.get(i)).get(key),((Map<?,?>)roots.get(i)).get(key)))throw new AssertionError("search mismatch "+trial.get("mode")+" root"+i+" "+key);
        }
        DataFiles.write(out,Map.of("trials",trials,"modelSha256",BrnResearchComparison.digest(modelPath),
                "datasetManifest",DataFiles.read(dataPath.resolve("manifest.json"),Map.class),"method","3 rotated rounds,64 roots,depth5,8 depth4 warmups per mode;5s/5M per root; exact production search via explicit evaluator seam"));
    }
    private BrnSuccessorCache(){}
}
