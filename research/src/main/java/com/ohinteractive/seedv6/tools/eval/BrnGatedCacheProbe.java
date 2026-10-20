package com.ohinteractive.seedv6.tools.eval;

import com.google.gson.JsonParser;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnnext.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.SearchDriver;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.model.NetworkModel;
import java.nio.file.*;
import java.util.*;

/** Actual frozen GATE/CUBIC weights, preserved game transitions and same-node search oracle. */
public final class BrnGatedCacheProbe {
    private static volatile long sink;
    public static void main(String[] args) throws Exception {
        if(args.length<4||args.length>5)throw new IllegalArgumentException("MODEL_PLAN NODE_RECEIPTS CORPUS_JSONL NEW_OUTPUT [MODEL_NAME]");
        String candidate=args.length==5?args[4]:"W1-GATE";
        Path output=Path.of(args[3]);if(Files.exists(output))throw new IllegalArgumentException("Preserve prior evidence");
        var plan=DataFiles.read(Path.of(args[0]),BrnSuccessorAudit.Plan.class);
        var spec=plan.models().stream().filter(m->m.name().equals(candidate)).findFirst().orElseThrow();
        var snapshot=CheckpointStore.readSnapshot(Path.of(spec.root()),spec.checkpoint());
        var model=((NetworkModel.BrnNext)snapshot.model()).model();
        boolean cubic=model.architecture()==BrnNextArchitecture.CUBIC;
        if(!cubic&&model.architecture()!=BrnNextArchitecture.GATED_PAIRS)throw new IllegalArgumentException("Unsupported cache");
        var full=SearchEvaluation.brnNext(model);
        var cached=cubic?SearchEvaluation.brnCubicIncrementalResearch(model):SearchEvaluation.brnGatedIncrementalResearch(model);
        var base=plan.models().getFirst();
        var reference=CheckpointStore.readSnapshot(Path.of(base.root()),base.checkpoint()).model().evaluation(NnueScoreMapping.V1);
        var boards=new ArrayList<long[]>();var inputs=new TreeMap<String,String>();
        try(var files=Files.list(Path.of(args[1]))) {
            for(var file:files.filter(f->f.getFileName().toString().matches("p\\d+-m\\d+-c\\d+\\.json")).sorted().toList()) {
                String json=Files.readString(file);inputs.put(file.toString(),DataFiles.hash(json));
                var receipt=DataFiles.JSON.fromJson(json,BrnSuccessorMatch.Receipt.class);
                boards.add(Board.fromFen(receipt.openingFen()));
                for(var move:receipt.moves())boards.add(Board.fromFen(move.fenAfter()));
            }
        }
        var accumulator=cache(model);var workspace=model.newWorkspace();
        double maximum=0;int differences=0;
        for(int i=0;i<boards.size();i++) {
            var board=boards.get(i);double expected=model.rawPawns(board,workspace),actual=accumulator.rawPawns(board);
            maximum=Math.max(maximum,Math.abs(expected-actual));
            if(model.score(board,workspace)!=accumulator.score(board))differences++;
            if(i%97==0&&Double.doubleToLongBits(actual)!=Double.doubleToLongBits(cache(model).rawPawns(board)))
                throw new IllegalStateException("History-dependent cached value");
        }
        var result=new LinkedHashMap<String,Object>();
        result.put("plan","brn-successor-wave2-"+(cubic?"cubic":"gate")+"-cache-v1");result.put("manifest",snapshot.manifest());
        result.put("modelPlanHash",DataFiles.hash(Files.readString(Path.of(args[0]))));result.put("candidateName",candidate);
        result.put("inputs",inputs);result.put("positions",boards.size());result.put("maximumRawError",maximum);
        result.put("integerDifferences",differences);result.put("accumulatorPrimitiveBytes",accumulator.primitiveBytes());
        DataFiles.write(output,result);
        if(maximum>1e-9||differences!=0)throw new IllegalStateException("Trained oracle mismatch; evidence preserved");
        String label=cubic?"CUBIC":"GATE";
        var names=List.of(label+"-full",label+"-cache","Gen32");
        var states=List.of(full.newState(2),cached.newState(2),reference.newState(2));
        for(var state:states)run(state,boards,50000);
        long[][] timings=new long[3][7];
        for(int pass=0;pass<7;pass++)for(int offset=0;offset<3;offset++) {
            int i=(pass+offset)%3;long start=System.nanoTime();run(states.get(i),boards,50000);timings[i][pass]=System.nanoTime()-start;
        }
        var latency=new ArrayList<Object>();
        for(int i=0;i<3;i++) {
            long[] sorted=timings[i].clone();Arrays.sort(sorted);
            latency.add(Map.of("name",names.get(i),"passNanos",timings[i],"evaluationsPerPass",50000,"medianNanos",sorted[3]/50000.0));
        }
        result.put("transitionLatency",latency);DataFiles.write(output,result);
        var roots=new ArrayList<long[]>();
        try(var lines=Files.lines(Path.of(args[2]))) {
            var list=lines.toList();
            for(int i=0;i<16;i++)roots.add(Board.fromFen(JsonParser.parseString(list.get(i*128)).getAsJsonObject().get("fen").getAsString()));
        }
        var searches=new ArrayList<Object>();
        for(int pass=0;pass<3;pass++)for(int i=0;i<roots.size();i++) {
            var board=roots.get(i);SearchResult expected=null;
            for(int offset=0;offset<2;offset++) {
                int implementation=(offset+pass+i)%2;
                try(var driver=new SearchDriver(BrnSuccessorAudit.common(implementation==0?full:cached))) {
                    var control=SearchControl.controlled(32768,System.nanoTime(),2_000_000_000L,TimeSource.SYSTEM);
                    var outcome=driver.search(new SearchRequest(board,GameHistory.initial(board),64,control));
                    if(outcome.lastCompletedResult()==null||control.termination()!=SearchTermination.NODE_LIMIT)
                        throw new IllegalStateException("Search probe failed");
                    if(expected!=null&&!expected.equals(outcome.lastCompletedResult()))throw new IllegalStateException("Search parity failed");
                    expected=outcome.lastCompletedResult();
                    searches.add(Map.of("pass",pass,"root",i,"implementation",names.get(implementation),
                            "elapsedNanos",control.elapsedNanos(),"nodes",control.nodes(),"result",expected));
                }
            }
        }
        result.put("searches",searches);result.put("completed",true);
        result.put("java",System.getProperty("java.runtime.version"));result.put("activeProcessors",Runtime.getRuntime().availableProcessors());
        DataFiles.write(output,result);System.out.println("Trained "+label+" cache oracle and performance complete: "+output);
    }
    private interface Cache {
        double rawPawns(long[] board);
        int score(long[] board);
        long primitiveBytes();
    }
    private static Cache cache(BrnNextModel model) {
        if(model.architecture()==BrnNextArchitecture.CUBIC) {
            var value=new BrnCubicAccumulator(model);
            return new Cache() {
                public double rawPawns(long[] board){return value.rawPawns(board);}
                public int score(long[] board){return value.score(board);}
                public long primitiveBytes(){return value.primitiveBytes();}
            };
        }
        var value=new BrnGatedAccumulator(model);
        return new Cache() {
            public double rawPawns(long[] board){return value.rawPawns(board);}
            public int score(long[] board){return value.score(board);}
            public long primitiveBytes(){return value.primitiveBytes();}
        };
    }
    private static void run(SearchEvaluation.State state,List<long[]>boards,int count) {
        long sum=0;for(int i=0;i<count;i++)sum+=state.evaluate(boards.get(i%boards.size()),0);sink=sum;
    }
    private BrnGatedCacheProbe() {}
}
