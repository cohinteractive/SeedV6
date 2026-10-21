package com.ohinteractive.seedv6.tools.eval;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnnext.*;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.model.NetworkModel;
import java.nio.file.*;
import java.util.*;
import java.util.function.ToIntFunction;

/** Bounded isolated vector feasibility; no model writes, search integration or training. */
public final class BrnCubicVectorProbe {
    private static volatile long sink;
    public static void main(String[] args) throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("MODEL_PLAN GAME_RECEIPTS NEW_OUTPUT");
        Path output=Path.of(args[2]);if(Files.exists(output))throw new IllegalArgumentException("Preserve prior evidence");
        var plan=DataFiles.read(Path.of(args[0]),BrnGatedCacheProbe.ModelPlan.class);
        var boards=new ArrayList<long[]>();var inputs=new TreeMap<String,String>();
        try(var files=Files.list(Path.of(args[1]))) {
            for(var file:files.filter(f->f.getFileName().toString().matches("p\\d+-m\\d+-c\\d+\\.json")).sorted().toList()) {
                String text=Files.readString(file);inputs.put(file.toString(),DataFiles.hash(text));
                var game=DataFiles.JSON.fromJson(text,BrnSuccessorMatch.Receipt.class);
                boards.add(Board.fromFen(game.openingFen()));
                for(var move:game.moves())boards.add(Board.fromFen(move.fenAfter()));
            }
        }
        if(boards.isEmpty()||boards.size()>10000)throw new IllegalArgumentException("Bounded game trace required");
        var reports=new ArrayList<Object>();
        for(var spec:plan.models()) {
            var loaded=BrnSuccessorMatch.loadModel(spec);
            if(!(loaded instanceof NetworkModel.BrnNext next))continue;
            var model=BrnSuccessorVariants.correction(next.model(),spec.correctionScale(),spec.correctionAblation(),null);
            if(!model.architecture().frozenCubic())throw new IllegalArgumentException("Cubic only");
            var scalar=new BrnCubicDeltaAccumulator(model);var vector=new BrnCubicVectorPrototype(model);
            var copiedScalar=new BrnCubicVectorPrototype(model,false);
            var canonical=model.newWorkspace();double maximum=0;int bits=0,score=0;
            for(int pass=0;pass<4;pass++)for(var board:boards) {
                double a=scalar.rawPawns(board),b=vector.rawPawns(board);
                if(Double.doubleToLongBits(a)!=Double.doubleToLongBits(b)
                        ||Double.doubleToLongBits(a)!=Double.doubleToLongBits(copiedScalar.rawPawns(board)))bits++;
                maximum=Math.max(maximum,Math.abs(model.rawPawns(board,canonical)-b));
                int expected=model.score(board,canonical);
                if(expected!=scalar.score(board)||expected!=vector.score(board)||expected!=copiedScalar.score(board))score++;
            }
            if(bits!=0||score!=0)throw new IllegalStateException("Prototype differs: bits="+bits+" scores="+score);
            List<ToIntFunction<long[]>> functions=List.of(scalar::score,copiedScalar::score,vector::score);
            for(var function:functions)run(function,boards,100000);
            long[][] nanos=new long[3][9];
            for(int pass=0;pass<9;pass++)for(int offset=0;offset<3;offset++) {
                int index=(pass+offset)%3;long start=System.nanoTime();
                run(functions.get(index),boards,100000);nanos[index][pass]=System.nanoTime()-start;
            }
            var medians=new ArrayList<Double>();
            for(var values:nanos) {var sorted=values.clone();Arrays.sort(sorted);medians.add(sorted[4]/100000.0);}
            reports.add(Map.of("candidate",spec.name(),"manifest",CheckpointStore.readSnapshot(Path.of(spec.root()),spec.checkpoint()).manifest(),
                    "positionsPerPass",boards.size(),"parityPasses",4,"scalarVectorBitDifferences",bits,"integerDifferences",score,
                    "maximumCanonicalRawError",maximum,"memory",Map.of("scalar",scalar.primitiveBytes(),"prototype",vector.primitiveBytes()),
                    "timing",Map.of("evaluationsPerPass",100000,"implementations",List.of("core-scalar","copied-scalar","vector"),
                            "passesNanos",nanos,"medianNanos",medians,"prototypeOverScalar",medians.get(2)/medians.get(0),"vectorOverCopiedScalar",medians.get(2)/medians.get(1))));
        }
        DataFiles.write(output,Map.of("id","wave5-vector-feasibility-v1","modelPlanHash",DataFiles.hash(Files.readString(Path.of(args[0]))),
                "inputs",inputs,"models",reports,"vectorLanes",BrnCubicVectorPrototype.lanes(),"activeProcessors",Runtime.getRuntime().availableProcessors(),
                "java",System.getProperty("java.runtime.version"),"limitations","Isolated evaluation only. No search integration or runtime adoption. Private coefficient copy counted in workspace."));
        System.out.println("Vector prototype feasibility complete: "+output);
    }
    private static void run(ToIntFunction<long[]> function,List<long[]> boards,int count) {
        long total=0;for(int i=0;i<count;i++)total+=function.applyAsInt(boards.get(i%boards.size()));sink=total;
    }
}

