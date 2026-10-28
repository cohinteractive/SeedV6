package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import com.ohinteractive.seedv6.core.Board;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Same aligned Pair-2 objective/optimizer with bounded primitive-array data storage. */
public final class PairCoverageDiagnostic {
    static final class Data {
        static final int STRIDE=Board.MAX_BITBOARDS;
        final int rows;
        final long[] boards;
        final double[] targets;
        Data(int rows) {
            if(rows<1||rows>4194304)throw new IllegalArgumentException("Bounded data rows");
            this.rows=rows;boards=new long[Math.multiplyExact(rows,STRIDE)];targets=new double[rows];
        }
        void board(int row,long[] destination){System.arraycopy(boards,row*STRIDE,destination,0,STRIDE);}
        static Data read(Path path,int rows)throws IOException {
            var data=new Data(rows);int row=0;
            try(var reader=Files.newBufferedReader(path)) {
                for(String line;(line=reader.readLine())!=null;row++) {
                    if(row>=rows)throw new IOException("Too many data rows");int split=line.indexOf('\t');if(split<1)throw new IOException("Missing target");
                    long[] board=Board.fromFen(line.substring(0,split));double target=Double.parseDouble(line.substring(split+1));
                    if(!Double.isFinite(target)||target<0||target>1||board.length!=STRIDE)throw new IOException("Data value/board");
                    System.arraycopy(board,0,data.boards,row*STRIDE,STRIDE);data.targets[row]=target;
                }
            }
            if(row!=rows)throw new IOException("Missing data rows");return data;
        }
    }
    static double mse(BrnPair2Trainer trainer,Data data) {
        var workspace=new BrnPair2Model(trainer.snapshot()).newWorkspace();long[] board=new long[Data.STRIDE];double loss=0;
        for(int i=0;i<data.rows;i++){data.board(i,board);double d=PairObjectiveDiagnostic.sigmoid(workspace.evaluatePawns(board,.25)/4)-data.targets[i];loss+=d*d;}
        return loss/data.rows;
    }
    static void epoch(BrnPair2Trainer trainer,Data data,long seed,int epoch) {
        int[] order=new int[data.rows];for(int i=0;i<order.length;i++)order[i]=i;var random=new Random(seed+epoch);
        for(int i=order.length-1;i>0;i--){int j=random.nextInt(i+1),v=order[i];order[i]=order[j];order[j]=v;}
        var aligned=new PairObjectiveDiagnostic.Aligned(trainer);
        for(int offset=0;offset<order.length;offset+=128) {
            int count=Math.min(128,order.length-offset);long[][] boards=new long[count][Data.STRIDE];double[] targets=new double[count];
            for(int j=0;j<count;j++){int i=order[offset+j];data.board(i,boards[j]);targets[j]=data.targets[i];}
            aligned.batch(boards,targets);
        }
    }
    /** Streaming hash avoids a second data-sized heap allocation. */
    static String sha(Path path)throws Exception {
        var digest=java.security.MessageDigest.getInstance("SHA-256");
        try(var input=new java.security.DigestInputStream(Files.newInputStream(path),digest)){input.transferTo(OutputStream.nullOutputStream());}
        return HexFormat.of().formatHex(digest.digest());
    }
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();Path configPath=Path.of(args[0]);var config=JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
        if(!config.get("mode").getAsString().equals("aligned"))throw new IllegalArgumentException("Only the matched aligned objective");
        Path trainPath=Path.of(config.get("train").getAsString()),valPath=Path.of(config.get("validation").getAsString()),initial=Path.of(config.get("initial_state").getAsString());
        if(!sha(trainPath).equals(config.get("train_sha256").getAsString())||!sha(valPath).equals(config.get("validation_sha256").getAsString())||!sha(initial).equals(config.get("initial_state_sha256").getAsString()))throw new IOException("Input identity");
        int initialEpoch=config.get("initial_epoch").getAsInt(),epochs=config.get("epochs").getAsInt();long seed=config.get("seed").getAsLong();
        var prior=JsonParser.parseString(Files.readString(initial.resolveSibling(initial.getFileName().toString().replace(".state",".json")))).getAsJsonObject();
        if(!prior.get("objective").getAsString().equals("aligned")||prior.get("epoch").getAsInt()!=initialEpoch)throw new IOException("Initial semantics");
        BrnPair2Trainer trainer;try(var input=Files.newInputStream(initial)){trainer=BrnPair2Trainer.read(input);}
        if(trainer.rate()!=config.get("rate").getAsDouble())throw new IOException("Initial rate");
        Path out=Path.of(config.get("output").getAsString());Files.createDirectories(out);String configSha=sha(configPath);Path bound=out.resolve("config.sha256");
        if(Files.exists(bound)){if(!Files.readString(bound).equals(configSha))throw new IOException("Config changed");}else Files.writeString(bound,configSha,StandardOpenOption.CREATE_NEW);
        long loading=System.nanoTime();var train=Data.read(trainPath,config.get("train_positions").getAsInt());var val=Data.read(valPath,4096);double loadSeconds=(System.nanoTime()-loading)/1e9;
        var gson=new GsonBuilder().setPrettyPrinting().create();var metrics=new ArrayList<Object>();metrics.add(Map.of("epoch",initialEpoch,"validation_mse",mse(trainer,val)));
        long started=System.nanoTime();
        for(int epoch=initialEpoch+1;epoch<=epochs;epoch++) {
            PairObjectiveDiagnostic.permission();Path receipt=out.resolve("epoch-"+epoch+".json"),state=out.resolve("epoch-"+epoch+".state"),model=out.resolve("epoch-"+epoch+".brn-pair2");
            if(Files.exists(receipt)) {
                var r=JsonParser.parseString(Files.readString(receipt)).getAsJsonObject();
                if(!sha(state).equals(r.get("state_sha256").getAsString())||!sha(model).equals(r.get("model_sha256").getAsString()))throw new IOException("Checkpoint identity");
                try(var input=Files.newInputStream(state)){trainer=BrnPair2Trainer.read(input);}metrics.add(r);continue;
            }
            if(Files.exists(state)||Files.exists(model))throw new IOException("Partial checkpoint preserved; reconcile before retry");
            long began=System.nanoTime();epoch(trainer,train,seed,epoch);
            double validation=mse(trainer,val),training=mse(trainer,train);
            try(var stream=Files.newOutputStream(state,StandardOpenOption.CREATE_NEW)){trainer.write(stream);}
            try(var stream=Files.newOutputStream(model,StandardOpenOption.CREATE_NEW)){BrnPair2Codec.writeModel(new BrnPair2Model(trainer.snapshot()),stream);}
            var r=Map.of("epoch",epoch,"train_mse",training,"validation_mse",validation,"updates",trainer.step(),"seconds",(System.nanoTime()-began)/1e9,
                "state_sha256",sha(state),"model_sha256",sha(model),"objective","aligned");
            Files.writeString(receipt,gson.toJson(r)+"\n",StandardOpenOption.CREATE_NEW);metrics.add(r);
        }
        var result=Map.of("mode","aligned","seed",seed,"metrics",metrics,"load_seconds",loadSeconds,"train_positions",train.rows,"validation_positions",val.rows,
            "seconds",(System.nanoTime()-started)/1e9,"scope","Matched exposure coverage diagnostic; unchanged Pair2 computation; no strength claim");
        Files.writeString(out.resolve("result.json"),gson.toJson(result)+"\n",StandardOpenOption.CREATE_NEW);System.out.println(gson.toJson(result));
    }
    private PairCoverageDiagnostic(){}
}
