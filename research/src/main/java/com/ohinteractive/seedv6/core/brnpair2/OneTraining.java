package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Checkpointed CPU ONE training. Immutable segment directories commit only by receipt. */
public final class OneTraining {
    static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    static Map<String,Object> metrics(TrainableOne model,OneTrainingCorpus corpus,int samples,long dataSeed){
        var f=new TrainableOne.Features();double dev=0,ablated=0,train=0;var order=new OneTrainingCorpus.Order(samples,0,dataSeed);
        for(int i=0;i<8192;i++){
            double t=model.decode(corpus.development,i*32,f),e=PairObjectiveDiagnostic.sigmoid(model.logit(f,model.workspace,true))-t;dev+=e*e;
            e=PairObjectiveDiagnostic.sigmoid(model.logit(f,model.workspace,false))-t;ablated+=e*e;
            t=corpus.read(order.at((int)((long)i*samples/8192)),model,f);e=PairObjectiveDiagnostic.sigmoid(model.logit(f,model.workspace,true))-t;train+=e*e;
        }
        return Map.of("development_mse",dev/8192,"development_unary_only_mse",ablated/8192,"fixed_training_panel_mse",train/8192,"panel_rows",8192);
    }
    public static void main(String[] args)throws Exception{
        PairObjectiveDiagnostic.permission();Path configPath=Path.of(args[0]);String configHash=PairCoverageDiagnostic.sha(configPath);var config=JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
        int width=config.get("width").getAsInt(),rank=config.get("rank").getAsInt(),samples=config.get("samples").getAsInt(),epochs=config.get("epochs").getAsInt(),checkpointRows=config.get("checkpoint_rows").getAsInt();
        long seed=config.get("seed").getAsLong(),dataSeed=config.get("data_seed").getAsLong();long target=(long)samples*epochs;
        if(epochs<1||epochs>8||checkpointRows<128||checkpointRows>262144||checkpointRows%128!=0)throw new IllegalArgumentException("Bounded allocation");
        if(config.getAsJsonArray("rates").size()!=epochs)throw new IllegalArgumentException("Rate schedule");
        for(var rate:config.getAsJsonArray("rates"))if(!Double.isFinite(rate.getAsDouble())||rate.getAsDouble()<=0||rate.getAsDouble()>.1)throw new IllegalArgumentException("Rate bounds");
        int segmentLimit=args.length>1?Integer.parseInt(args[1]):Integer.MAX_VALUE,completedSegments=0;if(segmentLimit<1)throw new IllegalArgumentException("Segment limit");
        Path corpusPath=Path.of(config.get("corpus").getAsString());if(!PairCoverageDiagnostic.sha(corpusPath).equals(config.get("corpus_sha256").getAsString()))throw new IOException("Corpus manifest changed");
        Path out=Path.of(config.get("output").getAsString()),segments=out.resolve("segments");Files.createDirectories(segments);
        if(Files.exists(out.resolve("result.json")))throw new IllegalStateException("Completed run must not repeat");
        TrainableOne model=new TrainableOne(width,rank,config.getAsJsonArray("rates").get(0).getAsDouble(),seed);long exposure=0;JsonObject previous=null;Path previousPath=null;
        try(var paths=Files.walk(segments,2)){
            for(Path p:paths.filter(p->p.getFileName().toString().equals("receipt.json")).toList()){
                var r=JsonParser.parseString(Files.readString(p)).getAsJsonObject();if(!r.get("config_sha256").getAsString().equals(configHash))throw new IOException("Checkpoint config changed");
                long e=r.get("exposures").getAsLong();if(e>target||e%128!=0)throw new IOException("Checkpoint exposure");if(previous==null||e>exposure){previous=r;previousPath=p;exposure=e;}
            }
        }
        if(previous!=null){Path state=previousPath.resolveSibling("model.state");if(!PairCoverageDiagnostic.sha(state).equals(previous.get("state_sha256").getAsString()))throw new IOException("Checkpoint hash");
            try(var stream=Files.newInputStream(state)){model=TrainableOne.read(stream);}if(model.width!=width||model.rank!=rank||model.step!=exposure/128)throw new IOException("Checkpoint cursor/optimizer");
            var cursor=previous;long expected=exposure;
            while(true){if(cursor.get("exposures").getAsLong()!=expected||!cursor.get("config_sha256").getAsString().equals(configHash))throw new IOException("Checkpoint chain");expected=cursor.get("begin_exposures").getAsLong();
                if(!cursor.has("previous_receipt")||cursor.get("previous_receipt").isJsonNull()){if(expected!=0)throw new IOException("Checkpoint missing baseline");break;}
                Path prior=Path.of(cursor.get("previous_receipt").getAsString());if(!PairCoverageDiagnostic.sha(prior).equals(cursor.get("previous_receipt_sha256").getAsString()))throw new IOException("Checkpoint prior identity");cursor=JsonParser.parseString(Files.readString(prior)).getAsJsonObject();
            }
        }
        final long resumed=exposure;long started=System.nanoTime();var batch=new TrainableOne.Features[128];for(int i=0;i<128;i++)batch[i]=new TrainableOne.Features();double[] targets=new double[128];
        try(var corpus=new OneTrainingCorpus(corpusPath)){
            if(Files.exists(out.resolve("initial.json"))){var initial=JsonParser.parseString(Files.readString(out.resolve("initial.json"))).getAsJsonObject();if(!initial.get("config_sha256").getAsString().equals(configHash))throw new IOException("Initial config identity");}
            else if(exposure==0)Files.writeString(out.resolve("initial.json"),JSON.toJson(Map.of("config_sha256",configHash,"metrics",metrics(model,corpus,samples,dataSeed)))+"\n",StandardOpenOption.CREATE_NEW);
            int activeEpoch=-1;OneTrainingCorpus.Order order=null;
            while(exposure<target){PairObjectiveDiagnostic.permission();long begin=exposure,end=Math.min(target,Math.min(exposure+checkpointRows,(exposure/samples+1)*samples));double loss=0;int batches=0;long segmentStart=System.nanoTime();
                while(exposure<end){int epoch=(int)(exposure/samples);if(epoch!=activeEpoch){activeEpoch=epoch;order=new OneTrainingCorpus.Order(samples,epoch,dataSeed);model.rate=config.getAsJsonArray("rates").get(epoch).getAsDouble();}
                    int cursor=(int)(exposure%samples);for(int i=0;i<128;i++)targets[i]=corpus.read(order.at(cursor+i),model,batch[i]);loss+=model.train(batch,targets);batches++;exposure+=128;
                    if((exposure&4095)==0)PairObjectiveDiagnostic.permission();
                }
                Path directory=segments.resolve(String.format(Locale.ROOT,"%09d-%s",exposure,UUID.randomUUID().toString().substring(0,12)));Files.createDirectory(directory);
                Path state=directory.resolve("model.state");try(var stream=Files.newOutputStream(state,StandardOpenOption.CREATE_NEW)){model.write(stream);}
                // Independent deserialization plus exact byte comparison before a completed receipt is published.
                TrainableOne reread;try(var stream=Files.newInputStream(state)){reread=TrainableOne.read(stream);}var bytes=new ByteArrayOutputStream();reread.write(bytes);if(!Arrays.equals(Files.readAllBytes(state),bytes.toByteArray()))throw new IOException("Checkpoint verification");
                var receipt=new LinkedHashMap<String,Object>();receipt.put("config_sha256",configHash);receipt.put("state_sha256",PairCoverageDiagnostic.sha(state));receipt.put("previous_receipt",previousPath==null?null:previousPath.toString());receipt.put("previous_receipt_sha256",previousPath==null?null:PairCoverageDiagnostic.sha(previousPath));
                receipt.put("begin_exposures",begin);receipt.put("exposures",exposure);receipt.put("next_epoch_zero_based",exposure/samples);receipt.put("next_row_in_epoch",exposure%samples);receipt.put("optimizer_steps",model.step);receipt.put("mean_training_batch_mse",loss/batches);receipt.put("seconds",(System.nanoTime()-segmentStart)/1e9);receipt.put("rate",model.rate);
                if(exposure%samples==0)receipt.put("metrics",metrics(model,corpus,samples,dataSeed));
                Path temp=directory.resolve("receipt.tmp");Files.writeString(temp,JSON.toJson(receipt)+"\n",StandardOpenOption.CREATE_NEW);previousPath=directory.resolve("receipt.json");Files.move(temp,previousPath,StandardCopyOption.ATOMIC_MOVE);
                System.out.println("checkpoint exposures="+exposure+" target="+target+" seconds="+receipt.get("seconds")+(receipt.containsKey("metrics")?" metrics="+JSON.toJson(receipt.get("metrics")):""));System.out.flush();
                if(++completedSegments>=segmentLimit&&exposure<target){System.out.println("Safe segment boundary; resume unchanged config.");return;}
            }
            var result=new LinkedHashMap<String,Object>();result.put("config_sha256",configHash);result.put("samples",samples);result.put("exposures",exposure);result.put("epochs",epochs);result.put("parameters",model.parameters);result.put("latest_receipt",previousPath.toString());result.put("latest_receipt_sha256",PairCoverageDiagnostic.sha(previousPath));result.put("metrics",metrics(model,corpus,samples,dataSeed));result.put("invocation_resumed_exposures",resumed);result.put("invocation_seconds",(System.nanoTime()-started)/1e9);
            Files.writeString(out.resolve("result.json"),JSON.toJson(result)+"\n",StandardOpenOption.CREATE_NEW);System.out.println(JSON.toJson(result));
        }
    }
}
