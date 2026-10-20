package com.ohinteractive.seedv6.tools.eval;

import com.google.gson.JsonParser;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnnext.BrnCubicDeltaAccumulator;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.model.NetworkModel;
import java.nio.file.*;
import java.util.*;

/** Correctness and balanced transition cost only; sparse prototype has no search integration. */
public final class BrnCubicSparseProbe {
    private static volatile double sink;
    public static void main(String[] args) throws Exception {
        if(args.length!=6)throw new IllegalArgumentException("MODEL_PLAN MODEL_NAME RANK_REPORT NODE_RECEIPTS CP_JSONL NEW_OUTPUT");
        Path output=Path.of(args[5]);if(Files.exists(output))throw new IllegalArgumentException("Preserve evidence");
        var plan=DataFiles.read(Path.of(args[0]),BrnGatedCacheProbe.ModelPlan.class);
        var spec=plan.models().stream().filter(m->m.name().equals(args[1])).findFirst().orElseThrow();
        var source=((NetworkModel.BrnNext)BrnSuccessorMatch.loadModel(spec)).model();
        var rankReport=JsonParser.parseString(Files.readString(Path.of(args[2]))).getAsJsonObject();
        if(!rankReport.getAsJsonObject("model").equals(DataFiles.JSON.toJsonTree(spec).getAsJsonObject()))
            throw new IllegalArgumentException("Rank report/model mismatch");
        var inputs=new TreeMap<String,String>();
        for(int i:new int[]{0,2,4})inputs.put(args[i],DataFiles.hash(Files.readString(Path.of(args[i]))));
        var boards=new ArrayList<long[]>();
        try(var files=Files.list(Path.of(args[3]))) {
            for(var path:files.filter(p->p.getFileName().toString().matches("p\\d+-m\\d+-c\\d+\\.json")).sorted().toList()) {
                String text=Files.readString(path);inputs.put(path.toString(),DataFiles.hash(text));
                var receipt=DataFiles.JSON.fromJson(text,BrnSuccessorMatch.Receipt.class);
                boards.add(Board.fromFen(receipt.openingFen()));for(var move:receipt.moves())boards.add(Board.fromFen(move.fenAfter()));
            }
        }
        int transitions=boards.size();if(transitions==0)throw new IllegalArgumentException("Game transitions required");
        for(String line:Files.readString(Path.of(args[4])).lines().toList())boards.add(Board.fromFen(JsonParser.parseString(line).getAsJsonObject().get("fen").getAsString()));
        var results=new ArrayList<Object>();
        for(var variant:rankReport.getAsJsonArray("variants")) {
            var ranks=new ArrayList<Integer>();for(var r:variant.getAsJsonObject().getAsJsonArray("ranks"))ranks.add(r.getAsInt());
            var model=BrnCubicRankProbe.keep(source,ranks);
            var encoded=new java.io.ByteArrayOutputStream();new NetworkModel.BrnNext(model).write(encoded);
            String payloadHash=HexFormat.of().formatHex(DataFiles.digest().digest(encoded.toByteArray()));
            if(!payloadHash.equals(variant.getAsJsonObject().get("networkSha256").getAsString()))
                throw new IllegalArgumentException("Masked payload differs from rank diagnostic");
            var full=model.newWorkspace();
            var dense=new BrnCubicDeltaAccumulator(model);var sparse=new BrnCubicSparsePrototype(model);
            double maximum=0;int integerDifferences=0;
            for(int pass=0;pass<3;pass++)for(int i=0;i<boards.size();i++) {
                var board=boards.get(pass==1?boards.size()-1-i:i);
                double expected=model.rawPawns(board,full);
                maximum=Math.max(maximum,Math.max(Math.abs(expected-dense.rawPawns(board)),Math.abs(expected-sparse.rawPawns(board))));
                int score=model.score(board,full);
                if(score!=dense.score(board)||score!=sparse.score(board))integerDifferences++;
            }
            var result=new LinkedHashMap<String,Object>();result.put("ranks",ranks);result.put("positionsCompared",boards.size()*3);
            result.put("maximumRawError",maximum);result.put("integerDifferences",integerDifferences);
            result.put("sparsePrimitiveBytes",sparse.primitiveBytes());result.put("densePrimitiveBytes",dense.primitiveBytes());
            results.add(result);
            if(maximum>1e-9||integerDifferences!=0) {
                DataFiles.write(output,Map.of("inputs",inputs,"results",results,"completed",false));
                throw new IllegalStateException("Sparse prototype parity failed");
            }
            double sum=0;
            for(int i=0;i<100000;i++){var board=boards.get(i%transitions);sum+=dense.rawPawns(board)+sparse.rawPawns(board);}sink=sum;
            var timings=new ArrayList<Object>();
            for(boolean useSparse:new boolean[]{false,true,true,false,true,false,false,true}) {
                dense=new BrnCubicDeltaAccumulator(model);sparse=new BrnCubicSparsePrototype(model);sum=0;long started=System.nanoTime();
                for(int i=0;i<200000;i++){var board=boards.get(i%transitions);sum+=useSparse?sparse.rawPawns(board):dense.rawPawns(board);}
                long elapsed=System.nanoTime()-started;sink=sum;
                timings.add(Map.of("sparse",useSparse,"nanosPerTransition",elapsed/200000.0));
            }
            result.put("timings",timings);
        }
        DataFiles.write(output,Map.of("id","wave4-sparse-cubic-prototype-v1","model",spec,"inputs",inputs,"results",results,"completed",true,
                "scope","No search routing/training. Same bound and period64 rebuild; nonfinite bound retains all ranks. Microcost only, no chess claim."));
        System.out.println("Completed sparse cubic prototype: "+output);
    }
    private BrnCubicSparseProbe() {}
}
