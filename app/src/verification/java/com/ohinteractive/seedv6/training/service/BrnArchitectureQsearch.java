package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.exact.*;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;

/** Common opt-in qsearch diagnostic; does not enable production qsearch. */
public final class BrnArchitectureQsearch {
    static Map<String,Object> probe(long[] board,Supplier<ExactEvaluator> factory,int index,int depth,long nodeLimit,long millis) {
        if(index<0||depth<1||depth>4||nodeLimit<1||nodeLimit>2_000_000||millis<1||millis>10_000)
            throw new IllegalArgumentException("Qsearch diagnostic bounds");
        long[] before=board.clone();var evaluator=factory.get();long start=System.nanoTime();
        var search=ExactSearch.quiescenceResearch(evaluator);
        boolean[] stoppedBy=new boolean[2];
        var result=search.search(board,GameHistory.initial(board),depth,()->{
            boolean nodes=search.visitedNodes()>=nodeLimit,time=System.nanoTime()-start>=millis*1_000_000L;
            stoppedBy[0]|=nodes;stoppedBy[1]|=time;return nodes||time;
        });
        long elapsed=System.nanoTime()-start;
        if(!Arrays.equals(board,before))throw new AssertionError("Qsearch modified input");
        var row=new LinkedHashMap<String,Object>();row.put("index",index);row.put("fen",Fen.fromBoard(board));
        row.put("completed",result.completed());row.put("nodes",result.nodes());row.put("qnodes",result.qnodes());
        row.put("maximumQply",result.maximumQply());row.put("seconds",elapsed/1e9);
        row.put("nodeGuardReached",stoppedBy[0]);row.put("timeGuardReached",stoppedBy[1]);
        row.put("otherAbort",!result.completed()&&!stoppedBy[0]&&!stoppedBy[1]);
        if(result.completed()){row.put("score",result.score());row.put("move",Long.toUnsignedString(result.bestMove()));}
        return row;
    }
    public static void main(String[] a)throws Exception {
        if(a.length!=9)throw new IllegalArgumentException("DATA MODEL NEW_OUT GAIN COUNT DEPTH MILLIS NODE_LIMIT FIRST_INDEX");
        Path data=Path.of(a[0]),model=Path.of(a[1]),out=Path.of(a[2]);double gain=Double.parseDouble(a[3]);
        int count=Integer.parseInt(a[4]),depth=Integer.parseInt(a[5]),first=Integer.parseInt(a[8]);
        long millis=Long.parseLong(a[6]),nodeLimit=Long.parseLong(a[7]);BrnArchitectureControls.requireGain(gain);
        if(count<1||count>128||first<0||depth<1||depth>4||millis<1||millis>10_000||nodeLimit<1||nodeLimit>2_000_000)
            throw new IllegalArgumentException("Diagnostic bounds");
        if(Files.exists(out))throw new IOException("Use a new diagnostic output");
        var held=BrnResearchData.read(data,false).validation();
        if(first>held.size()-count)throw new IllegalArgumentException("Insufficient validation roots");
        String modelIdentity=BrnArchitectureControls.modelIdentity(model),metadataIdentity=BrnArchitectureControls.metadataIdentity(model);
        var view=BrnArchitectureControls.loadView(model,gain);var rows=new ArrayList<Map<String,Object>>();
        var report=new LinkedHashMap<String,Object>();report.put("schema","brn-architecture-qsearch-v1");
        report.put("arguments",a);report.put("modelSha256",modelIdentity);report.put("modelMetadataIdentity",metadataIdentity);report.put("gain",gain);
        report.put("datasetManifest",DataFiles.read(data.resolve("manifest.json"),Map.class));
        report.put("method","Existing ExactSearch.quiescenceResearch baseline: TT-free ordered alpha-beta with unpruned tactical leaves; one worker; native evaluator/gain. Diagnostic only, not the production match search.");
        report.put("nodeLimit",nodeLimit);report.put("millis",millis);report.put("depth",depth);
        report.put("complete",false);report.put("roots",rows);Files.createDirectory(out);DataFiles.write(out.resolve("result.json"),report);
        var warm=new ArrayList<Map<String,Object>>();
        for(int i=0;i<Math.min(8,held.size());i++)warm.add(probe(held.get(i).board(),view.evaluator(),i,2,10000,250));
        report.put("warmup",warm);report.put("warmupScope","First8 validation roots, depth2,10000 nodes/250ms each; retained separately from measured roots; full process cost includes warmup.");
        for(int i=first;i<first+count;i++) {
            try {rows.add(probe(held.get(i).board(),view.evaluator(),i,depth,nodeLimit,millis));}
            catch(RuntimeException|AssertionError failure) {
                report.put("failedIndex",i);report.put("failure",failure.toString());DataFiles.write(out.resolve("result.json"),report);throw failure;
            }
            DataFiles.write(out.resolve("result.json"),report);System.out.println("qroot="+i+" completed="+rows.get(rows.size()-1).get("completed"));
        }
        BrnArchitectureControls.requireIdentity(model,modelIdentity,metadataIdentity);
        long completed=rows.stream().filter(r->Boolean.TRUE.equals(r.get("completed"))).count();
        report.put("completedRoots",completed);report.put("allRootsCompleted",completed==count);
        report.put("complete",true);report.put("completionScope","All requested roots attempted; individual deadline/node failures remain explicit and are not successful searches.");
        DataFiles.write(out.resolve("result.json"),report);
    }
    private BrnArchitectureQsearch(){}
}
