package com.ohinteractive.seedv6.tools.eval;

import com.google.gson.JsonParser;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.SearchDriver;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.rules.GameHistory;
import java.nio.file.*;
import java.util.*;

/** Trained inference timing plus explicit first-iteration-budget reproduction. No training. */
public final class BrnSuccessorCostProbe {
    private static volatile long sink;
    public static void main(String[] args) throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("MODEL_PLAN POSITION_JSONL NEW_RESULT");
        var plan=DataFiles.read(Path.of(args[0]),BrnSuccessorAudit.Plan.class);
        Path output=Path.of(args[2]);if(Files.exists(output))throw new IllegalArgumentException("Preserve prior result");
        var boards=new ArrayList<long[]>();
        try(var lines=Files.lines(Path.of(args[1]))) {
            for(var line:lines.limit(256).toList())boards.add(Board.fromFen(JsonParser.parseString(line).getAsJsonObject().get("fen").getAsString()));
        }
        if(boards.size()!=256)throw new IllegalStateException("Missing fixed timing positions");
        var states=new ArrayList<SearchEvaluation.State>();var definitions=new ArrayList<SearchEvaluation>();
        for(var spec:plan.models()) {
            var evaluation=CheckpointStore.readSnapshot(Path.of(spec.root()),spec.checkpoint()).model().evaluation(NnueScoreMapping.V1);
            definitions.add(evaluation);states.add(evaluation.newState(2));
        }
        // Fixed unrelated-position transitions; retain each production workspace's cache behavior.
        for(var state:states)run(state,boards,50000);
        long[][] nanos=new long[states.size()][7];
        for(int pass=0;pass<7;pass++)for(int offset=0;offset<states.size();offset++) {
            int i=(offset+pass)%states.size();long start=System.nanoTime();
            run(states.get(i),boards,50000);nanos[i][pass]=System.nanoTime()-start;
        }
        var rows=new ArrayList<Object>();
        for(int i=0;i<states.size();i++) {
            long[] sorted=nanos[i].clone();Arrays.sort(sorted);
            rows.add(Map.of("name",plan.models().get(i).name(),"passesNanos",nanos[i],"evaluationsPerPass",50000,"medianNanos",sorted[3]/50000.0));
        }
        String failureFen="n1kr1b1r/1bp2p2/4p1p1/RP2P2q/3p1P2/1PPBP2P/2Q3K1/1NB4R w - - 0 22";
        var board=Board.fromFen(failureFen);var repro=new ArrayList<Object>();
        for(int i=0;i<definitions.size();i++)for(long nodes:new long[]{8192,32768,131072}) {
            try(var search=new SearchDriver(BrnSuccessorAudit.common(definitions.get(i)))) {
                var control=SearchControl.controlled(nodes,System.nanoTime(),2_000_000_000L,TimeSource.SYSTEM);
                var result=search.search(new SearchRequest(board,GameHistory.initial(board),64,control));
                var row=new LinkedHashMap<String,Object>();row.put("model",plan.models().get(i).name());row.put("limit",nodes);
                row.put("termination",control.termination());row.put("nodes",control.nodes());
                row.put("elapsedNanos",control.elapsedNanos());row.put("result",result.lastCompletedResult());repro.add(row);
            }
        }
        DataFiles.write(output,Map.of("timings",rows,"failureFen",failureFen,"reproduction",repro,"checksum",sink,
                "java",System.getProperty("java.runtime.version"),"activeProcessors",Runtime.getRuntime().availableProcessors(),
                "limits","256-position BT4 transitions with production workspace cache behavior; no search-throughput equivalence. Failure replay has a fresh TT and initial FEN history."));
    }
    private static void run(SearchEvaluation.State state,List<long[]>boards,int count) {
        long sum=0;for(int i=0;i<count;i++) {var board=boards.get(i%boards.size());state.initialize(board,0);sum+=state.evaluate(board,0);}sink=sum;
    }
    private BrnSuccessorCostProbe() {}
}
