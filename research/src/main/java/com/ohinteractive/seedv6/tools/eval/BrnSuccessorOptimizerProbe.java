package com.ohinteractive.seedv6.tools.eval;

import com.google.gson.JsonParser;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Objective;
import com.ohinteractive.seedv6.core.brnnext.*;
import com.ohinteractive.seedv6.core.brnpair2.*;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets;
import java.nio.file.*;
import java.util.*;

/** Fixed small early-optimization diagnostic; no checkpoints are written. */
public final class BrnSuccessorOptimizerProbe {
    private record Row(long[] board,double target) {}
    private interface Trainer {
        double predict(long[] board);
        double train(long[][] boards,double[] targets);
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("BT4_JSONL CP_JSONL NEW_OUTPUT");
        Path output=Path.of(args[2]);
        if(Files.exists(output))throw new IllegalArgumentException("Preserve prior evidence");
        long deadline=System.nanoTime()+300_000_000_000L;
        var results=new ArrayList<Object>();var inputs=new TreeMap<String,String>();
        for(int source=0;source<2;source++) {
            String text=Files.readString(Path.of(args[source]));inputs.put(args[source],DataFiles.hash(text));
            var rows=new ArrayList<Row>();
            for(String line:text.lines().toList()) {
                var json=JsonParser.parseString(line).getAsJsonObject();
                rows.add(new Row(Board.fromFen(json.get("fen").getAsString()),json.get("target").getAsDouble()));
            }
            if(rows.size()!=2048)throw new IllegalArgumentException("Expected frozen2048-position diagnostic source");
            List<Row> train=rows.subList(0,1024),heldout=rows.subList(1024,2048);
            var orders=new ArrayList<List<Integer>>();var random=new Random(71);
            for(int epoch=0;epoch<8;epoch++) {
                var order=new ArrayList<Integer>();for(int i=0;i<1024;i++)order.add(i);
                Collections.shuffle(order,random);orders.add(order);
            }
            for(String kind:List.of("PAIR2","GATED_PAIRS","CUBIC","POOLED"))for(double rate:new double[]{.003,.01}) {
                Trainer trainer=create(kind,rate);var curve=new ArrayList<Object>();long nanos=0;
                curve.add(metrics(trainer,train,heldout,0,0));
                long[][] boards=new long[128][];double[] targets=new double[128];int step=0;
                for(var order:orders)for(int batch=0;batch<8;batch++) {
                    if(System.nanoTime()>deadline)throw new IllegalStateException("Bounded diagnostic deadline exceeded; preserve completed arms");
                    for(int i=0;i<128;i++){var row=train.get(order.get(batch*128+i));boards[i]=row.board();targets[i]=row.target();}
                    long start=System.nanoTime();trainer.train(boards,targets);nanos+=System.nanoTime()-start;step++;
                    if(Set.of(1,4,16,64).contains(step))curve.add(metrics(trainer,train,heldout,step,nanos));
                }
                results.add(Map.of("source",source==0?"BT4":"CP","architecture",kind,"rate",rate,"seed",71,
                        "visits",8192,"batch",128,"curve",curve));
                DataFiles.write(output,Map.of("plan","brn-successor-wave2-optimizer-smoke-v1","inputs",inputs,
                        "arms",results,"completed",false));
            }
        }
        DataFiles.write(output,Map.of("plan","brn-successor-wave2-optimizer-smoke-v1","inputs",inputs,
                "arms",results,"completed",true,"java",System.getProperty("java.runtime.version"),
                "activeProcessors",Runtime.getRuntime().availableProcessors()));
        System.out.println("Completed16 bounded optimizer arms; no models saved: "+output);
    }
    private static Object metrics(Trainer trainer,List<Row> train,List<Row> heldout,int step,long elapsed) {
        return Map.of("step",step,"trainingNanos",elapsed,"train",measure(trainer,train),"heldout",measure(trainer,heldout));
    }
    private static Object measure(Trainer trainer,List<Row> rows) {
        double ce=0,mse=0,residual=0,max=0;
        for(var row:rows) {
            double raw=trainer.predict(row.board()),material=BrnPair2Features.material(row.board());
            double error=Brn3Objective.smoothOutcome(raw,NnueCorpusTargets.material(row.board()))[0]-row.target();
            ce+=Brn3Objective.crossEntropy(raw,NnueCorpusTargets.material(row.board()),row.target())[0];
            mse+=error*error;residual+=(raw-material)*(raw-material);max=Math.max(max,Math.abs(raw-material));
        }
        return Map.of("crossEntropy",ce/rows.size(),"outcomeMse",mse/rows.size(),
                "residualRmsPawns",Math.sqrt(residual/rows.size()),"maxResidualPawns",max);
    }
    private static Trainer create(String kind,double rate) {
        if(kind.equals("PAIR2")) {
            var trainer=new BrnPair2Trainer(rate);
            return new Trainer() {
                public double predict(long[] board){return trainer.predictPawns(board);}
                public double train(long[][] boards,double[] targets){return trainer.trainBatch(boards,targets,128);}
            };
        }
        var trainer=new BrnNextTrainer(BrnNextArchitecture.valueOf(kind),71,rate);
        return new Trainer() {
            public double predict(long[] board){return trainer.predictPawns(board);}
            public double train(long[][] boards,double[] targets){return trainer.trainBatch(boards,targets,128);}
        };
    }
    private BrnSuccessorOptimizerProbe() {}
}
