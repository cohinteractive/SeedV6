package com.ohinteractive.seedv6.tools.eval;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnnext.*;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.model.NetworkModel;
import java.nio.file.*;
import java.util.*;
import java.util.function.ToIntFunction;

/** Bounded isolated quadratic cost; no model writes, search integration or training. */
public final class BrnQuadraticCostProbe {
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
            var scalar=new BrnCubicDeltaAccumulator(model);var quadratic=new BrnQuadraticDeltaPrototype(model);
            var canonical=model.newWorkspace();double maximum=0;int bits=0,score=0;
            for(int pass=0;pass<4;pass++)for(var board:boards) {
                double a=scalar.rawPawns(board),b=quadratic.rawPawns(board);
                if(Double.doubleToLongBits(a)!=Double.doubleToLongBits(b))bits++;
                maximum=Math.max(maximum,Math.abs(model.rawPawns(board,canonical)-b));
                int expected=model.score(board,canonical);
                if(expected!=scalar.score(board)||expected!=quadratic.score(board))score++;
            }
            if(bits!=0||score!=0)throw new IllegalStateException("Prototype differs: bits="+bits+" scores="+score);
            List<ToIntFunction<long[]>> functions=List.of(scalar::score,quadratic::score);
            for(var function:functions)run(function,boards,100000);
            long[][] nanos=new long[2][9];
            for(int pass=0;pass<9;pass++)for(int offset=0;offset<2;offset++) {
                int index=(pass+offset)%2;long start=System.nanoTime();
                run(functions.get(index),boards,100000);nanos[index][pass]=System.nanoTime()-start;
            }
            var medians=new ArrayList<Double>();
            for(var values:nanos) {var sorted=values.clone();Arrays.sort(sorted);medians.add(sorted[4]/100000.0);}
            reports.add(Map.of("candidate",spec.name(),"manifest",CheckpointStore.readSnapshot(Path.of(spec.root()),spec.checkpoint()).manifest(),
                    "positionsPerPass",boards.size(),"parityPasses",4,"scalarPrototypeBitDifferences",bits,"integerDifferences",score,
                    "maximumCanonicalRawError",maximum,"memory",Map.of("scalar",scalar.primitiveBytes(),"prototype",quadratic.primitiveBytes()),
                    "timing",Map.of("evaluationsPerPass",100000,"implementations",List.of("core-cubic-zero-heads","quadratic-only-arithmetic"),
                            "passesNanos",nanos,"medianNanos",medians,"prototypeOverScalar",medians.get(1)/medians.get(0))));
        }
        DataFiles.write(output,Map.of("id","wave5-quadratic-cost-v1","modelPlanHash",DataFiles.hash(Files.readString(Path.of(args[0]))),
                "inputs",inputs,"models",reports,"activeProcessors",Runtime.getRuntime().availableProcessors(),
                "java",System.getProperty("java.runtime.version"),"limitations","Isolated evaluation only. No search integration or runtime adoption. No trained quadratic model or end-to-end search benefit inferred."));
        System.out.println("Quadratic cost prototype complete: "+output);
    }
    private static void run(ToIntFunction<long[]> function,List<long[]> boards,int count) {
        long total=0;for(int i=0;i<count;i++)total+=function.applyAsInt(boards.get(i%boards.size()));sink=total;
    }
}
