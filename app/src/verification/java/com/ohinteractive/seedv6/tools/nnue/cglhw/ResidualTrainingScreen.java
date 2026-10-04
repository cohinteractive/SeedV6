package com.ohinteractive.seedv6.tools.nnue.cglhw;

import com.google.gson.Gson;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import com.ohinteractive.seedv6.core.brn3.*;
import com.ohinteractive.seedv6.corpus.CorpusPreparation;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.training.service.*;
import com.ohinteractive.seedv6.training.validation.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.function.ToDoubleFunction;

/** Frozen equal-data comparison using production source selection/adapters and BRN3 trainer.
 * Separate output roots; no existing checkpoint/source cursor or application selector is changed.
 */
public final class ResidualTrainingScreen {
    private static final Gson JSON=new com.google.gson.GsonBuilder().serializeNulls().create();
    private static final long DATA_SEED=2026100510L,SHUFFLE_SEED=2026100511L,OPENING_SEED=2026100512L;
    record Data(long[][] boards,double[] targets) {}
    record Frozen(Data train,Data held,Map<String,Object> evidence) {}
    record PositionKey(long a,long b,long c,long d,int stm) {
        static PositionKey of(long[] b){return new PositionKey(b[0],b[1],b[2],b[3],Board.player((int)b[4]));}
    }
    private static Data unpack(CorpusTraining.Examples examples) {
        var samples=examples.samples();var boards=new long[samples.size()][];var targets=new double[boards.length];
        for(int i=0;i<boards.length;i++){boards[i]=samples.get(i).board();targets[i]=examples.targets().applyAsDouble(samples.get(i));}
        return new Data(boards,targets);
    }
    static Data disjointHeld(Data train,Data held) {
        var seen=new HashSet<PositionKey>();for(var b:train.boards)seen.add(PositionKey.of(b));
        var boards=new ArrayList<long[]>();var targets=new ArrayList<Double>();
        for(int i=0;i<held.boards.length;i++)if(seen.add(PositionKey.of(held.boards[i]))) {
            boards.add(held.boards[i]);targets.add(held.targets[i]);
        }
        return new Data(boards.toArray(long[][]::new),targets.stream().mapToDouble(Double::doubleValue).toArray());
    }
    private static Frozen freeze(Path root,Path registered,int count,int epochs,int batch)throws Exception {
        var candidates=DataSources.read(registered).sources().stream().filter(s->s.labelProfile()==LabelProfile.BT4_Q_V1).toList();
        if(candidates.size()!=1)throw new IllegalArgumentException("Expected exactly one explicitly BT4-labelled registered source");
        var source=candidates.getFirst();
        if(CorpusTraining.targetPolicy(TrainingArchitecture.NNUE,source.labelProfile())!=
                CorpusTraining.targetPolicy(TrainingArchitecture.BRN3,source.labelProfile()))throw new AssertionError("Unequal target adapters");
        new DataSources(1,List.of(source.withDisplay(source.name(),1)),false).save(DataSources.directory(root));
        var config=new TrainerConfig(root,DATA_SEED,
                new TrainerConfig.SelfPlay(1,1,4,0,0,1,1,NnueScoreMapping.V1),
                new TrainerConfig.Training(epochs,batch,true),
                new TrainerConfig.Validation(2,0,0,1,1,1,NnueScoreMapping.V1,new PromotionPolicy(1,.9,0)),
                1,TrainerConfig.DepthChange.REQUIRE_SAME,TrainerConfig.STANDARD_START,TrainingArchitecture.NNUE,.003)
                .withSource(TrainingSource.dataSources(DataSources.directory(root)))
                .withCorpusTraining(new CorpusTrainingConfig(count)).withValidationMethod(ValidationMethod.HELD_OUT);
        try(var selection=new SequentialTraining(config,config.source(),CorpusPreparation.NONE,100L*count+10000)) {
            var selected=selection.batch(1);var train=unpack(selected.training());var originalHeld=unpack(selected.validation());
            var held=disjointHeld(train,originalHeld);
            if(held.boards.length<100)throw new IllegalStateException("Insufficient disjoint held-out positions");
            var evidence=new LinkedHashMap<String,Object>();evidence.put("productionSelection",selected.evidence());
            evidence.put("sourceMetrics",selection.metrics());evidence.put("trainingRecords",train.boards.length);
            evidence.put("originalHeldOut",originalHeld.boards.length);evidence.put("disjointHeldOut",held.boards.length);
            evidence.put("heldOutPolicy","Drop placement+STM identities seen in training or earlier held-out rows; target-independent, common to both architectures; training exposures unchanged");
            evidence.put("trainingFileSha256",writeData(root.resolve("training.bin"),train));
            evidence.put("heldOutFileSha256",writeData(root.resolve("heldout.bin"),held));
            selection.complete(1);return new Frozen(train,held,evidence);
        }
    }
    private static String writeData(Path path,Data data)throws Exception {
        try(var out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path,StandardOpenOption.CREATE_NEW)))) {
            out.writeLong(0x5336434744415431L);out.writeInt(data.boards.length);
            for(int i=0;i<data.boards.length;i++){for(long v:data.boards[i])out.writeLong(v);out.writeDouble(data.targets[i]);}
        }
        return hash(path);
    }
    private static String hash(Path path)throws Exception {
        var digest=MessageDigest.getInstance("SHA-256");
        try(var in=new DigestInputStream(Files.newInputStream(path),digest)){in.transferTo(OutputStream.nullOutputStream());}
        return HexFormat.of().formatHex(digest.digest());
    }
    private static Map<String,Object> evaluation(Data data,ToDoubleFunction<long[]> prediction) {
        double ce=0,mse=0,searchMse=0,residual2=0;
        for(int i=0;i<data.boards.length;i++) {
            var board=data.boards[i];double total=prediction.applyAsDouble(board),target=data.targets[i];
            double material=Brn3Features.material(board),residual=total-material;
            ce+=Brn3Objective.crossEntropy(total,NnueCorpusTargets.material(board),target)[0];
            double difference=Brn3Objective.outcome(total,board)-target;mse+=.5*difference*difference;
            double calibrated=Brn3Objective.outcome(material+Brn3SearchCalibration.RESIDUAL_GAIN*residual,board)-target;
            searchMse+=.5*calibrated*calibrated;residual2+=residual*residual;
        }
        int n=data.boards.length;
        return Map.of("positions",n,"rawTotalCe",ce/n,"rawTotalHalfMse",mse/n,
                "calibratedTotalHalfMse",searchMse/n,"neuralResidualRmsPawns",Math.sqrt(residual2/n));
    }
    private static int[] permutation(int n,int epoch) {
        int[] order=new int[n];for(int i=0;i<n;i++)order[i]=i;
        var random=new SplittableRandom(SHUFFLE_SEED+epoch);
        for(int i=n-1;i>0;i--){int j=random.nextInt(i+1),temp=order[i];order[i]=order[j];order[j]=temp;}
        return order;
    }
    private static void write(BufferedWriter out,Object row)throws IOException {
        String text=JSON.toJson(row);out.write(text);out.newLine();out.flush();System.out.println(text);
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=10)throw new IllegalArgumentException("NEW_REPORT_DIR NEW_STATE_DIR REGISTERED_SOURCE_DIR POSITIONS EPOCHS BATCH SEEDS PAIRS DEPTH CAP");
        Path report=Path.of(args[0]),root=Path.of(args[1]),source=Path.of(args[2]);
        int count=Integer.parseInt(args[3]),epochs=Integer.parseInt(args[4]),batch=Integer.parseInt(args[5]);
        long[] seeds=Arrays.stream(args[6].split(",")).mapToLong(Long::parseLong).toArray();
        int pairs=Integer.parseInt(args[7]),depth=Integer.parseInt(args[8]),cap=Integer.parseInt(args[9]);
        if(count<128||count>1048576||epochs<1||epochs>16||batch<1||batch>1024||pairs<1||depth<1||depth>6||cap<1)throw new IllegalArgumentException("Research bounds");
        Files.createDirectory(report);Files.createDirectory(root);
        var meta=new LinkedHashMap<String,Object>();meta.put("args",args);meta.put("seeds",seeds);meta.put("recipe",NnueMaterialResidualTrainer.ID);
        meta.put("track","B bootstrap-parity strict-control pilot");meta.put("fixedMaterial","BRN3 V1 both architectures, retained after training");
        meta.put("adam",new BrnAdamConfig(.003));meta.put("shuffleSeed",SHUFFLE_SEED);meta.put("openingSeed",OPENING_SEED);
        meta.put("precision","Same masked Adam equations/loss/LR/batch/order/updates; NNUE float32 weights+gradients and binary64 moments; BRN3 binary64 training state, folded float32 inference");
        meta.put("search","Both M+.25R in pawn units, symmetric100-units/pawn mapping; depth controlled, private4MiBTT, paired colours, no qsearch/book/tablebase");
        var hashes=new TreeMap<String,String>();
        for(String directory:List.of("app/src/verification/java/com/ohinteractive/seedv6/tools/nnue/cglhw","app/src/verification/java/com/ohinteractive/seedv6/training/nnue",
                "app/src/main/java/com/ohinteractive/seedv6/training/nnue","app/src/main/java/com/ohinteractive/seedv6/core/brn3"))
            try(var files=Files.list(Path.of(directory))){for(var f:files.filter(f->f.toString().endsWith(".java")).toList())hashes.put(f.toString(),hash(f));}
        meta.put("sourceHashes",hashes);Files.writeString(report.resolve("metadata.json"),JSON.toJson(meta),StandardOpenOption.CREATE_NEW);
        Frozen frozen=freeze(root,source,count,epochs,batch);
        Files.writeString(report.resolve("data.json"),JSON.toJson(frozen.evidence),StandardOpenOption.CREATE_NEW);
        try(var log=Files.newBufferedWriter(report.resolve("training.jsonl"),StandardOpenOption.CREATE_NEW);
            var matches=Files.newBufferedWriter(report.resolve("results.jsonl"),StandardOpenOption.CREATE_NEW)) {
            for(int ordinal=0;ordinal<seeds.length;ordinal++) {
                long seed=seeds[ordinal];var config=new BrnAdamConfig(.003);
                var nnue=new NnueMaterialResidualTrainer(seed,config);var brn=new Brn3Trainer(seed,config);
                for(int epoch=0;epoch<=epochs;epoch++) {
                    if(epoch>0) {
                        int[] order=permutation(count,epoch);var boards=new long[batch][];var targets=new double[batch];
                        for(int offset=0;offset<count;offset+=batch) {
                            int size=Math.min(batch,count-offset);
                            for(int i=0;i<size;i++){int idx=order[offset+i];boards[i]=frozen.train.boards[idx];targets[i]=frozen.train.targets[idx];}
                            // Alternate order only to avoid systematic wall-time scheduling bias; same frozen inputs.
                            if((epoch+ordinal)%2==0){nnue.trainBatch(boards,targets,size);brn.trainBatch(boards,targets,size);}
                            else {brn.trainBatch(boards,targets,size);nnue.trainBatch(boards,targets,size);}
                        }
                    }
                    long expected=(long)epoch*((count+(long)batch-1)/batch);
                    if(nnue.step()!=expected||brn.step()!=expected)throw new AssertionError("Unequal update exposure");
                    write(log,Map.of("seed",seed,"epoch",epoch,"updatesEach",expected,"samplesEach",(long)count*epoch,
                            "nnue",evaluation(frozen.held,nnue::predictPawns),"brn3",evaluation(frozen.held,brn::predictPawns)));
                }
                Path n=root.resolve(seed+".nnue-residual-state"),b=root.resolve(seed+".brn3-state");
                try(var out=new BufferedOutputStream(Files.newOutputStream(n,StandardOpenOption.CREATE_NEW))){nnue.write(out);}
                try(var out=new BufferedOutputStream(Files.newOutputStream(b,StandardOpenOption.CREATE_NEW))){Brn3Codec.writeTraining(brn,out);}
                write(log,Map.of("type","checkpoint","seed",seed,"nnueState",n.toString(),"brn3State",b.toString(),"nnueSha256",hash(n),"brn3Sha256",hash(b)));
                var candidate=new HalfKpResidualModel(nnue.snapshot());var reference=brn.snapshot();
                var openings=new ValidationConfig(pairs,OPENING_SEED,6,10,depth,1,NnueScoreMapping.V1,cap);var start=Board.startingPosition();
                for(int i=0;i<pairs;i++) {
                    int index=ordinal*pairs+i;var opening=ValidationArena.opening(start,GameHistory.initial(start),openings,index);
                    var first=BootstrapAudit.game(opening,()->new SearchDriver(new ExactSearchAdapter(candidate.worker(257),new TTable(4))),
                            ()->new SearchDriver(new ExactSearchAdapter(SearchEvaluation.brn3(reference),new TTable(4))),i%2,depth,cap);
                    var second=BootstrapAudit.game(opening,()->new SearchDriver(new ExactSearchAdapter(candidate.worker(257),new TTable(4))),
                            ()->new SearchDriver(new ExactSearchAdapter(SearchEvaluation.brn3(reference),new TTable(4))),1-i%2,depth,cap);
                    write(matches,Map.of("type","pair","variant","halfkp-pawn-residual","seed",seed,"index",index,"openingHash",opening.identity(),"first",first,"second",second));
                }
            }
        }
        Files.writeString(report.resolve("complete.json"),JSON.toJson(Map.of("seeds",seeds.length,"games",2*pairs*seeds.length)),StandardOpenOption.CREATE_NEW);
    }
}
