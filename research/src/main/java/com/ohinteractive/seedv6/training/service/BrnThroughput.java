package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.brn3.*;
import com.ohinteractive.seedv6.corpus.CorpusPreparation;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import jdk.jfr.Recording;

/** Explicit bounded throughput experiment; never opens an existing writable lineage. */
public final class BrnThroughput {
    public static void main(String[] args) throws Exception {
        if(args.length==1&&args[0].equals("smoke")) {
            for(String module:List.of("java.desktop","java.sql","java.logging"))
                if(ModuleLayer.boot().findModule(module).isEmpty())throw new AssertionError("Missing runtime module "+module);
            var trainer=new Brn3Trainer(71);
            long[][] boards={com.ohinteractive.seedv6.core.Board.startingPosition(),
                    com.ohinteractive.seedv6.core.Board.fromFen("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1")};
            for(int step=0;step<6;step++)trainer.trainBatch(boards,new double[]{.5,-.2},2);
            System.out.println("BRN_RUNTIME vector="+ModuleLayer.boot().findModule("jdk.incubator.vector").isPresent()
                    +" state="+stateHash(trainer));return;
        }
        if(args.length!=6)throw new IllegalArgumentException("train|load|pipeline INPUT NEW_OUTPUT POSITIONS EPOCHS REPEATS");
        String mode=args[0]; Path input=Path.of(args[1]),output=Path.of(args[2]);
        int count=Integer.parseInt(args[3]),epochs=Integer.parseInt(args[4]),repeats=Integer.parseInt(args[5]);
        if(!Set.of("train","load","pipeline").contains(mode)||count<128||count>262144||epochs<1||epochs>8||repeats<1||repeats>7)
            throw new IllegalArgumentException("Bounded experiment limits");
        Files.createDirectory(output);
        var report=new LinkedHashMap<String,Object>();
        report.put("mode",mode);report.put("input",input.toAbsolutePath().toString());report.put("positions",count);
        report.put("epochs",epochs);report.put("batch",128);report.put("seed",71);
        report.put("java",System.getProperty("java.runtime.version"));report.put("vmArgs",ManagementFactory.getRuntimeMXBean().getInputArguments());
        report.put("processors",Runtime.getRuntime().availableProcessors());
        var trials=new ArrayList<Map<String,Object>>();report.put("trials",trials);
        long startup=System.nanoTime();
        if(mode.equals("train")) {
            var data=BrnResearchData.read(input,false);
            if(data.training().size()<count)throw new IllegalArgumentException("Insufficient examples");
            var samples=new ArrayList<TrajectorySampler.Sample>();var targets=new IdentityHashMap<TrajectorySampler.Sample,Double>();
            for(var example:data.training().subList(0,count)) {
                var sample=new TrajectorySampler.Sample(example.board(),0);samples.add(sample);targets.put(sample,example.outcome());
            }
            report.put("datasetReadPrepareSeconds",seconds(startup));
            for(int trial=-1;trial<repeats;trial++) {
                var trainer=new Brn3Trainer(71);
                var selected=trial<0?samples.subList(0,Math.min(8192,count)):samples;
                System.gc();long memory=used(),gc=gcMillis(),allocated=allocated();
                try(var recording=recording(trial==0&&Boolean.getBoolean("brn.profile"))) {
                    long begin=System.nanoTime();
                    var statistics=Brn3CorpusOptimization.trainSamples(trainer,selected,
                            new SelfPlayTraining.Config(trial<0?2:epochs,128,true,73103),new SelfPlayControl(),p->{},targets::get).orElseThrow();
                    double elapsed=seconds(begin);
                    if(recording!=null){recording.stop();recording.dump(output.resolve("training.jfr"));}
                    var result=new LinkedHashMap<String,Object>();result.put("trial",trial);result.put("seconds",elapsed);
                    result.put("examplesPerSecond",statistics.samplesTrained()/elapsed);result.put("statistics",statistics);
                    result.put("heapBefore",memory);result.put("heapAfter",used());result.put("gcMillis",gcMillis()-gc);
                    result.put("ownerAllocatedBytes",allocated()-allocated);result.put("stateSha256",stateHash(trainer));
                    trials.add(result);System.out.println(DataFiles.JSON.toJson(result));
                }
            }
        } else {
            var source=DataSource.register("Throughput source",input,1);report.put("source",source);
            for(int trial=-1;trial<repeats;trial++) {
                Path lineage=output.resolve("load-"+trial);Files.createDirectory(lineage);
                new DataSources(1,List.of(source),false).save(DataSources.directory(lineage));
                var config=config(lineage,trial<0?Math.min(4096,count):count,epochs);
                System.gc();long memory=used(),gc=gcMillis(),allocated=allocated();
                try(var recording=recording(trial==0&&Boolean.getBoolean("brn.profile"))) {
                    long begin=System.nanoTime();
                    try(var loader=new SequentialTraining(config,config.source(),CorpusPreparation.NONE,2_000_000)) {
                        var batch=loader.batch(1);double loadSeconds=seconds(begin);
                        Brn3Trainer trainer=null;SelfPlayTraining.Statistics statistics=null;double trainingSeconds=0;
                        if(mode.equals("pipeline")) {
                            long trainStart=System.nanoTime();trainer=new Brn3Trainer(71);
                            statistics=Brn3CorpusOptimization.trainSamples(trainer,batch.training().samples(),
                                    new SelfPlayTraining.Config(trial<0?2:epochs,128,true,73103),new SelfPlayControl(),p->{},batch.training().targets()).orElseThrow();
                            trainingSeconds=seconds(trainStart);
                        }
                        double elapsed=seconds(begin);
                        if(recording!=null){recording.stop();recording.dump(output.resolve("loading.jfr"));}
                        var result=new LinkedHashMap<String,Object>();result.put("trial",trial);result.put("seconds",elapsed);
                        result.put("loadSeconds",loadSeconds);result.put("trainingSeconds",trainingSeconds);
                        result.put("evidence",batch.evidence());result.put("metrics",loader.metrics());
                        result.put("heapBefore",memory);result.put("heapAfter",used());result.put("gcMillis",gcMillis()-gc);
                        result.put("ownerAllocatedBytes",allocated()-allocated);
                        if(trainer!=null){result.put("statistics",statistics);result.put("stateSha256",stateHash(trainer));}
                        trials.add(result);System.out.println(DataFiles.JSON.toJson(result));
                    }
                }
            }
        }
        var times=trials.stream().filter(t->((Integer)t.get("trial"))>=0).mapToDouble(t->(Double)t.get("seconds")).sorted().toArray();
        report.put("medianSeconds",times[times.length/2]);report.put("totalSeconds",seconds(startup));
        DataFiles.write(output.resolve("report.json"),report);
    }
    static TrainerConfig config(Path output,int count,int epochs) {
        return new TrainerConfig(output,71,
                new TrainerConfig.SelfPlay(1,1,4,0,0,1,1,NnueScoreMapping.V1),
                new TrainerConfig.Training(epochs,128,true),
                new TrainerConfig.Validation(2,0,0,1,1,1,NnueScoreMapping.V1,new PromotionPolicy(1,.9,0)),
                1,TrainerConfig.DepthChange.REQUIRE_SAME,TrainerConfig.STANDARD_START,TrainingArchitecture.BRN3,.003)
                .withSource(TrainingSource.dataSources(DataSources.directory(output)))
                .withCorpusTraining(new CorpusTrainingConfig(count)).withValidationMethod(ValidationMethod.HELD_OUT);
    }
    private static Recording recording(boolean enabled) {
        if(!enabled)return null;
        var recording=new Recording();recording.enable("jdk.ExecutionSample").withPeriod(Duration.ofMillis(2));
        recording.enable("jdk.ObjectAllocationSample");recording.enable("jdk.GarbageCollection");
        recording.enable("jdk.FileRead").withThreshold(Duration.ofMillis(1));recording.start();return recording;
    }
    private static long allocated(){return ((com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean()).getThreadAllocatedBytes(Thread.currentThread().threadId());}
    private static long gcMillis(){return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b->Math.max(0,b.getCollectionTime())).sum();}
    private static long used(){return Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();}
    private static double seconds(long begin){return (System.nanoTime()-begin)/1e9;}
    static String stateHash(Brn3Trainer trainer) {
        var hash=DataFiles.digest();var buffer=ByteBuffer.allocate(24);
        for(int i=0;i<Brn3Layout.TRAINING_PARAMETERS;i++) {
            buffer.clear().putDouble(trainer.weight(i)).putDouble(trainer.firstMoment(i)).putDouble(trainer.secondMoment(i));hash.update(buffer.array());
        }
        buffer.clear().putLong(trainer.step());hash.update(buffer.array(),0,8);return HexFormat.of().formatHex(hash.digest());
    }
    private BrnThroughput(){}
}
