package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.conventional.ConventionalNetwork;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import static com.ohinteractive.seedv6.core.brnpair2.BrnPair2Geometry.*;

/** Research-only objective intervention on the unchanged Pair-2 representation.
 * Uses existing codec in an explicitly labelled research envelope, never a user lineage. */
public final class PairObjectiveDiagnostic {
    static double sigmoid(double x) { return 1/(1+Math.exp(-x)); }
    static double[] objective(double rawPawns,double material,double target) {
        double p=sigmoid((material+.25*(rawPawns-material))/4);
        return new double[]{(p-target)*(p-target),2*(p-target)*p*(1-p)/16};
    }
    static final class Aligned {
        final BrnPair2Trainer trainer;
        final double[] gradient=new double[BrnPair2Codec.TRAINING_PARAMETERS];
        final boolean[] touched=new boolean[gradient.length];
        final int[][] codes=new int[2][64];
        Aligned(BrnPair2Trainer trainer) { this.trainer=trainer; }
        double gradients(long[][] boards,double[] targets) {
            Arrays.fill(gradient,0);Arrays.fill(touched,false);double loss=0;
            for(int n=0;n<boards.length;n++) {
                long[] board=boards[n];double[] value=objective(trainer.predictPawns(board),BrnPair2Features.material(board),targets[n]);
                loss+=value[0];add(trainer.bias,value[1]);
                for(int square=0;square<64;square++) {
                    int code=Board.getSquare(board[0],board[1],board[2],board[3],square);
                    for(int side=0;side<2;side++) codes[side][square^(side*56)]=category(code,side);
                }
                int stm=Board.player((int)board[4]);
                for(int role=0;role<2;role++)for(int t=0;t<TEMPLATES;t++) {
                    int address=state(codes[stm^role],t,2),absolute=(role*TEMPLATES+t)*169+address;
                    add(absolute,NORM*value[1]);
                    add(trainer.modelParameters+(role*shapeCount()+shape(t))*169+address,NORM*value[1]);
                    trainer.seen.set(absolute);
                }
            }
            for(int i=0;i<gradient.length;i++)gradient[i]/=boards.length;
            return loss/boards.length;
        }
        void add(int index,double value){gradient[index]+=value;touched[index]=true;}
        double batch(long[][] boards,double[] targets) {
            double loss=gradients(boards,targets);trainer.updates++;
            double c1=1-Math.pow(.9,trainer.updates),c2=1-Math.pow(.999,trainer.updates);
            for(int i=0;i<gradient.length;i++)if(touched[i]) {
                double g=gradient[i];trainer.first[i]=.9*trainer.first[i]+.1*g;
                trainer.second[i]=.999*trainer.second[i]+.001*g*g;
                trainer.weights[i]-=trainer.rate()*(trainer.first[i]/c1)/(Math.sqrt(trainer.second[i]/c2)+1e-8);
                if(!Double.isFinite(trainer.weights[i]))throw new ArithmeticException("Nonfinite aligned weights");
            }
            return loss;
        }
    }
    record Data(long[][] boards,double[] targets) {}
    static Data read(Path path)throws IOException {
        var lines=Files.readAllLines(path);long[][] boards=new long[lines.size()][];double[] targets=new double[lines.size()];
        for(int i=0;i<lines.size();i++) {
            String[] fields=lines.get(i).split("\\t");boards[i]=Board.fromFen(fields[0]);targets[i]=Double.parseDouble(fields[1]);
            if(!Double.isFinite(targets[i])||targets[i]<0||targets[i]>1)throw new IOException("Target");
        }
        return new Data(boards,targets);
    }
    static String sha(Path path)throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
    static void permission()throws IOException {
        var state=JsonParser.parseString(Files.readString(Path.of(System.getenv("SEED_NNUE_CONTROL")))).getAsJsonObject();
        if(!state.get("mode").getAsString().equals("NORMAL")||state.get("active").isJsonNull()
                ||!state.getAsJsonObject("active").get("id").getAsString().equals(System.getenv("SEED_NNUE_OPERATION"))
                ||!state.getAsJsonObject("active").get("stop_requested_at").isJsonNull()
                ||(!state.get("expires_at").isJsonNull()&&System.currentTimeMillis()/1000.0>=state.get("expires_at").getAsDouble()))
            throw new IOException("Compute hold; last completed epoch is resume point");
    }
    static double mse(BrnPair2Trainer trainer,Data data) {
        var workspace=new BrnPair2Model(trainer.snapshot()).newWorkspace();double loss=0;
        for(int i=0;i<data.boards.length;i++) {
            double p=sigmoid(workspace.evaluatePawns(data.boards[i],.25)/4),d=p-data.targets[i];loss+=d*d;
        }
        return loss/data.boards.length;
    }
    static void epoch(BrnPair2Trainer trainer,Data data,String mode,long seed,int epoch) {
        int[] order=new int[data.boards.length];for(int i=0;i<order.length;i++)order[i]=i;
        var rng=new Random(seed+epoch);for(int i=order.length-1;i>0;i--){int j=rng.nextInt(i+1),v=order[i];order[i]=order[j];order[j]=v;}
        var aligned=new Aligned(trainer);
        for(int start=0;start<order.length;start+=128) {
            int count=Math.min(128,order.length-start);long[][] boards=new long[count][];double[] targets=new double[count];
            for(int i=0;i<count;i++){int j=order[start+i];boards[i]=data.boards[j];targets[i]=mode.equals("legacy")?2*data.targets[j]-1:data.targets[j];}
            if(mode.equals("legacy"))trainer.trainBatch(boards,targets,count);else aligned.batch(boards,targets);
        }
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("Immutable run config JSON");
        permission();var config=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();
        String mode=config.get("mode").getAsString();if(!Set.of("legacy","aligned").contains(mode))throw new IllegalArgumentException("Mode");
        Path trainPath=Path.of(config.get("train").getAsString()),valPath=Path.of(config.get("validation").getAsString());
        if(!sha(trainPath).equals(config.get("train_sha256").getAsString())||!sha(valPath).equals(config.get("validation_sha256").getAsString()))throw new IOException("Data identity");
        Path out=Path.of(config.get("output").getAsString());Files.createDirectories(out);
        String configSha=sha(Path.of(args[0]));Path binding=out.resolve("config.sha256");
        if(Files.exists(binding)){if(!Files.readString(binding).equals(configSha))throw new IOException("Config changed");}
        else Files.writeString(binding,configSha,StandardOpenOption.CREATE_NEW);
        var train=read(trainPath);var val=read(valPath);long seed=config.get("seed").getAsLong();
        int epochs=config.get("epochs").getAsInt(),initialEpoch=0;var trainer=new BrnPair2Trainer(config.get("rate").getAsDouble());
        if(config.has("initial_state")) {
            Path initial=Path.of(config.get("initial_state").getAsString());
            if(!sha(initial).equals(config.get("initial_state_sha256").getAsString()))throw new IOException("Initial state identity");
            Path initialReceipt=initial.resolveSibling(initial.getFileName().toString().replace(".state",".json"));
            var prior=JsonParser.parseString(Files.readString(initialReceipt)).getAsJsonObject();
            initialEpoch=config.get("initial_epoch").getAsInt();
            if(!prior.get("objective").getAsString().equals(mode)||prior.get("epoch").getAsInt()!=initialEpoch)throw new IOException("Initial objective/epoch");
            try(var in=Files.newInputStream(initial)){trainer=BrnPair2Trainer.read(in);}
            if(trainer.rate()!=config.get("rate").getAsDouble())throw new IOException("Initial optimizer rate");
        }
        var gson=new GsonBuilder().setPrettyPrinting().create();var metrics=new ArrayList<Object>();
        metrics.add(Map.of("epoch",initialEpoch,"validation_mse",mse(trainer,val)));
        var nnue=ConventionalNetwork.load(Path.of(config.get("cp64").getAsString()));double baseline=0;
        for(int i=0;i<val.boards.length;i++){double d=sigmoid(nnue.fullRebuild(val.boards[i])/400.0)-val.targets[i];baseline+=d*d;}
        long start=System.nanoTime();
        for(int epoch=initialEpoch+1;epoch<=epochs;epoch++) {
            permission();Path receipt=out.resolve("epoch-"+epoch+".json"),state=out.resolve("epoch-"+epoch+".state"),model=out.resolve("epoch-"+epoch+".brn-pair2");
            if(Files.exists(receipt)) {
                var r=JsonParser.parseString(Files.readString(receipt)).getAsJsonObject();
                if(!sha(state).equals(r.get("state_sha256").getAsString())||!sha(model).equals(r.get("model_sha256").getAsString()))throw new IOException("Checkpoint identity");
                try(var in=Files.newInputStream(state)){trainer=BrnPair2Trainer.read(in);}metrics.add(r);continue;
            }
            if(Files.exists(state)||Files.exists(model))throw new IOException("Partial epoch preserved; reconcile before retry");
            long began=System.nanoTime();epoch(trainer,train,mode,seed,epoch);
            double valLoss=mse(trainer,val),trainLoss=mse(trainer,train);
            try(var stream=Files.newOutputStream(state,StandardOpenOption.CREATE_NEW)){trainer.write(stream);}
            try(var stream=Files.newOutputStream(model,StandardOpenOption.CREATE_NEW)){BrnPair2Codec.writeModel(new BrnPair2Model(trainer.snapshot()),stream);}
            var r=Map.of("epoch",epoch,"train_mse",trainLoss,"validation_mse",valLoss,"updates",trainer.step(),
                "seconds",(System.nanoTime()-began)/1e9,"state_sha256",sha(state),"model_sha256",sha(model),"objective",mode);
            Files.writeString(receipt,gson.toJson(r)+"\n",StandardOpenOption.CREATE_NEW);metrics.add(r);
        }
        var result=Map.of("mode",mode,"seed",seed,"metrics",metrics,"cp64_validation_mse",baseline/val.boards.length,
            "train_positions",train.boards.length,"validation_positions",val.boards.length,"seconds",(System.nanoTime()-start)/1e9,
            "scope","development objective diagnostic; no strength claim; research checkpoint envelope required");
        Files.writeString(out.resolve("result.json"),gson.toJson(result)+"\n",StandardOpenOption.CREATE_NEW);
        System.out.println(gson.toJson(result));
    }
}
