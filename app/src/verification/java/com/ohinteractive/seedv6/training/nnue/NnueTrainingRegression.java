package com.ohinteractive.seedv6.training.nnue;

import com.google.gson.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** E013: isolated, explicit-path forensics. Never opens an existing store for writing. */
public final class NnueTrainingRegression {
    static final Gson JSON = new GsonBuilder().registerTypeAdapter(java.time.Instant.class,
            (JsonSerializer<java.time.Instant>)(value,type,context)->new JsonPrimitive(value.toString())).setPrettyPrinting().serializeNulls().create();
    public record Sample(long[] board, double target, String source, long ordinal) {}
    static String key(long[] b) { return b[0]+":"+b[1]+":"+b[2]+":"+b[3]+":"+Board.player((int)b[4]); }
    public static String hash(Path p) throws Exception {
        var d=MessageDigest.getInstance("SHA-256");
        try(var in=new DigestInputStream(Files.newInputStream(p),d)){in.transferTo(OutputStream.nullOutputStream());}
        return HexFormat.of().formatHex(d.digest());
    }
    public static void json(Path p,Object value)throws IOException {Files.writeString(p,JSON.toJson(value),StandardOpenOption.CREATE_NEW);}
    public static NnueTrainer checkpoint(Path directory)throws Exception {
        var m=CheckpointInspection.manifest(directory);
        if(m.architecture()!=TrainingArchitecture.NNUE_MATERIAL)throw new IOException("Expected material checkpoint");
        Path state=directory.resolve(CheckpointManifest.TRAINING_FILE), model=directory.resolve(m.networkFile());
        if(!hash(state).equals(m.trainingSha256())||!hash(model).equals(m.networkSha256()))throw new IOException("Checkpoint SHA mismatch");
        NnueTrainer trainer;
        try(var in=new BufferedInputStream(Files.newInputStream(state))){trainer=TrainingStateCodec.readMaterial(in);}
        var out=new ByteArrayOutputStream();new NetworkTrainingState.NnueMaterial(trainer).snapshot().write(out);
        if(!Arrays.equals(out.toByteArray(),Files.readAllBytes(model))||trainer.optimizer().step()!=m.optimizerStep())throw new IOException("Model/optimizer mismatch");
        return trainer;
    }
    public static List<Sample> read(Path file)throws IOException {
        var result=new ArrayList<Sample>();
        try(var in=new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            if(in.readLong()!=0x5336453031334431L)throw new IOException("Diagnostic format");
            int n=in.readInt();if(n<1||n>1000000)throw new IOException("Diagnostic count");
            for(int i=0;i<n;i++){long[] b=new long[Board.MAX_BITBOARDS];for(int j=0;j<b.length;j++)b[j]=in.readLong();result.add(new Sample(b,in.readDouble(),in.readUTF(),in.readLong()));}
            if(in.read()!=-1)throw new IOException("Trailing data");
        }
        return result;
    }
    static void write(Path file,List<Sample> samples)throws IOException {
        try(var out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file,StandardOpenOption.CREATE_NEW)))) {
            out.writeLong(0x5336453031334431L);out.writeInt(samples.size());
            for(var s:samples){for(long v:s.board())out.writeLong(v);out.writeDouble(s.target());out.writeUTF(s.source());out.writeLong(s.ordinal());}
        }
    }
    static boolean usable(TrainingPosition p,DataSource source) {
        return p!=null && (source.labelProfile()==LabelProfile.BT4_Q_V1
            ? p.targetKind()==BinpackDecoder.ENCODED_SCORE && p.perspective()==com.ohinteractive.seedv6.corpus.CorpusRecord.SIDE_TO_MOVE && Math.abs((long)p.target())<=32000
            : NnueCorpusTargets.rejection(p)==null);
    }
    static double target(TrainingPosition p,long[] b,DataSource s) {return s.labelProfile()==LabelProfile.BT4_Q_V1?Bt4Targets.q(p.target()):NnueCorpusTargets.target(p,b);}
    static void freeze(Path lineage,Path out,long generation)throws Exception {
        var sources=DataSources.read(DataSources.directory(lineage));
        var reservation=new SourceLedger(lineage).generation(generation).orElseThrow();
        Files.createDirectory(out.resolve("seek"));
        try(var paths=Files.list(DataSources.directory(lineage).resolve("seek"))) {
            for(var p:paths.toList())if(Files.isRegularFile(p))Files.copy(p,out.resolve("seek").resolve(p.getFileName()));
        }
        var digest=MessageDigest.getInstance("SHA-256");var bytes=ByteBuffer.allocate(60);
        var labels=new TreeMap<String,String>();boolean bt4=false;
        for(var s:sources.sources()){String label=s.labelProfile().name();if(s.format()==DataSource.Format.STOCKFISH_BINPACK_ZSTD){label+=";prepared="+PreparedBinpack.ready(s).identity();bt4=true;}labels.put(s.identity(),label);}
        if(bt4)digest.update(labels.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var all=new ArrayList<Sample>();var held=new ArrayList<Sample>();var seen=new HashSet<String>();
        var audit=new ArrayList<Object>();
        for(var s:sources.sources()) {
            var range=reservation.ranges().stream().filter(r->r.source().equals(s.identity())).findFirst().orElseThrow();
            if(range.validation()!=0)throw new IOException("This reproduction expects game-pair source reservation");
            int count=0,mates=0;double sum=0,sum2=0;int negative=0,positive=0,zero=0;
            var identity=s.identity().getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            try(var reader=SourceReaders.open(s,out.resolve("seek"),range.start())) {
                while(reader.nextPosition()<range.end()) {
                    var e=reader.next();if(e==null)throw new EOFException();var p=e.position();if(!usable(p,s))continue;
                    long[] b=p.position().toBoard(0);double y=target(p,b,s);
                    var q=p.position();digest.update(identity);bytes.clear().putLong(e.ordinal()).putLong(q.plane0()).putLong(q.plane1()).putLong(q.plane2()).putLong(q.plane3()).putInt(q.rules()).putInt(q.halfmove()).putInt(p.targetKind()).putInt(p.target()).putInt(p.perspective());digest.update(bytes.array());
                    all.add(new Sample(b,y,s.identity(),e.ordinal()));seen.add(key(b));count++;sum+=y;sum2+=y*y;
                    if(p.targetKind()==com.ohinteractive.seedv6.corpus.CorpusRecord.MATE)mates++;
                    if(y<0)negative++;else if(y>0)positive++;else zero++;
                }
                if(count!=range.training())throw new IOException("Source range count mismatch");
                int added=0,examined=0;
                while(added<1024 && examined++<32768) {
                    var e=reader.next();if(e==null)throw new EOFException();var p=e.position();if(!usable(p,s))continue;
                    long[] b=p.position().toBoard(0);if(!seen.add(key(b)))continue;
                    held.add(new Sample(b,target(p,b,s),s.identity(),e.ordinal()));added++;
                }
                if(added!=1024)throw new IOException("Insufficient independent holdout");
            }
            audit.add(Map.of("source",s,"range",range,"count",count,"mate",mates,"negative",negative,"zero",zero,"positive",positive,"mean",sum/count,"rms",Math.sqrt(sum2/count)));
        }
        String actual=HexFormat.of().formatHex(digest.digest());if(!actual.equals(reservation.trainingHash()))throw new IOException("Actual generation training hash mismatch: "+actual);
        var trainingKeys=new HashSet<String>();for(var s:all)trainingKeys.add(key(s.board()));held.removeIf(s->trainingKeys.contains(key(s.board())));
        var order=new ArrayList<>(all);Collections.shuffle(order,new Random(202610051301L));
        var train=List.copyOf(order.subList(0,8192));
        write(out.resolve("train.bin"),train);write(out.resolve("held.bin"),held);write(out.resolve("full.bin"),all);
        json(out.resolve("data.json"),Map.of("generation",generation,"sources",audit,"verifiedFullGenerationTrainingHash",actual,"fullGenerationCount",all.size(),"pilotTrain",train.size(),"pilotHeld",held.size(),"heldPolicy","Next distinct placement+STM positions after each selected generation range, absent from the entire selected training generation; may be consumed by the historical next generation","trainSha256",hash(out.resolve("train.bin")),"heldSha256",hash(out.resolve("held.bin"))));
    }
    static List<Object> norms(NnueTrainer t,float[][] before) {
        var result=new ArrayList<Object>();
        for(int group=0;group<6;group++) {
            double p2=0,g2=0,u2=0;var p=t.model().parameters.groups[group];var g=t.gradients.values.groups[group];
            for(int i=0;i<p.length;i++){p2+=(double)p[i]*p[i];g2+=(double)g[i]*g[i];double d=p[i]-before[group][i];u2+=d*d;}
            result.add(Map.of("group",group,"parameters",p.length,"parameterL2",Math.sqrt(p2),"gradientL2",Math.sqrt(g2),"updateL2",Math.sqrt(u2)));
        }
        return result;
    }
    static double correlation(double[] a,double[] b) {
        double sa=0,sb=0,saa=0,sbb=0,sab=0;int n=a.length;
        for(int i=0;i<n;i++){sa+=a[i];sb+=b[i];saa+=a[i]*a[i];sbb+=b[i]*b[i];sab+=a[i]*b[i];}
        double d=Math.sqrt(Math.max(0,n*saa-sa*sa)*Math.max(0,n*sbb-sb*sb));return d==0?0:(n*sab-sa*sb)/d;
    }
    static Map<String,Object> moments(double[] a) {
        double sum=0,sq=0,abs=0;for(double v:a){sum+=v;sq+=v*v;abs+=Math.abs(v);}double[] q=a.clone();Arrays.sort(q);int n=a.length;
        return Map.of("mean",sum/n,"sd",Math.sqrt(Math.max(0,sq/n-sum*sum/n/n)),"rms",Math.sqrt(sq/n),"meanAbs",abs/n,"min",q[0],"p05",q[n/20],"median",q[n/2],"p95",q[n*19/20],"max",q[n-1]);
    }
    public static Map<String,Object> metrics(NnueTrainer t,List<Sample> data) {
        int n=data.size();double[] pred=new double[n],residual=new double[n],material=new double[n],target=new double[n],calibrated=new double[n];
        double loss=0,calibratedLoss=0,ce=0,maxDifference=0,nullAbs=0;long az=0,as=0,hz=0,hs=0;int overwhelm=0,imbalanced=0,reversed=0;
        var snapshot=t.model().snapshot();var eval=new NnueEvaluator(snapshot);var scalar=NnueEvaluator.scalarOracle(snapshot);var search=SearchEvaluation.incrementalWithMaterial(snapshot,NnueScoreMapping.V1).newState(1);
        for(int i=0;i<n;i++) {
            var s=data.get(i);var b=s.board();double outcome=t.predict(b);target[i]=s.target();material[i]=NnueMaterialBootstrap.forSideToMove(b,NnueMaterialBootstrap.whiteScore(b));residual[i]=32511*t.scratch.value;
            pred[i]=NnueMaterialBootstrap.combinedOutcome(t.scratch.value,(int)material[i]);
            double error=pred[i]-target[i];loss+=.5*error*error;
            calibrated[i]=com.ohinteractive.seedv6.core.brn3.Brn3Objective.smoothOutcome(pred[i]*325.11,NnueCorpusTargets.material(b))[0];
            if(outcome!=(t.calibratedOutcome()?calibrated[i]:pred[i]))throw new AssertionError("Outcome link mismatch");
            double d=calibrated[i]-target[i];calibratedLoss+=.5*d*d;
            ce+=com.ohinteractive.seedv6.core.brn3.Brn3Objective.crossEntropy(pred[i]*325.11,NnueCorpusTargets.material(b),target[i])[0];
            double raw=t.scratch.raw;if(raw!=eval.evaluate(b)||raw!=scalar.evaluate(b))throw new AssertionError("Forward path mismatch");
            search.initialize(b,0);maxDifference=Math.max(maxDifference,Math.abs(search.evaluate(b,0)-32511*pred[i]));
            if(maxDifference>1.00001)throw new AssertionError("Training/search mismatch");
            for(float x:t.scratch.input){if(x==0)az++;if(x==1)as++;}for(float x:t.scratch.hidden){if(x==0)hz++;if(x==1)hs++;}
            if(material[i]!=0){imbalanced++;if(Math.abs(residual[i])>Math.abs(material[i]))overwhelm++;if((material[i]+residual[i])*material[i]<0)reversed++;}
            long[] flipped=new long[Board.MAX_BITBOARDS];Board.nullMoveInto(b[0],b[1],b[2],b[3],(int)b[4],b[5],flipped);
            t.predict(flipped);double flippedScore=NnueMaterialBootstrap.combinedOutcome(t.scratch.value,NnueMaterialBootstrap.forSideToMove(flipped,NnueMaterialBootstrap.whiteScore(flipped)));
            nullAbs+=Math.abs(32511*(pred[i]+flippedScore));
        }
        var m=new LinkedHashMap<String,Object>();m.put("n",n);m.put("halfMse",loss/n);m.put("calibratedOutcomeHalfMse",calibratedLoss/n);m.put("calibratedCE",ce/n);m.put("calibratedTargetCorrelation",correlation(calibrated,target));m.put("prediction",moments(pred));m.put("target",moments(target));m.put("residualUnits",moments(residual));m.put("materialUnits",moments(material));m.put("predictionTargetCorrelation",correlation(pred,target));m.put("residualMaterialCorrelation",correlation(residual,material));m.put("materialNonzero",imbalanced);m.put("residualOverwhelmsMaterial",overwhelm);m.put("materialSignReversed",reversed);m.put("accumulatorZeroFraction",az/(n*128.0));m.put("accumulatorSaturatedFraction",as/(n*128.0));m.put("hiddenZeroFraction",hz/(n*32.0));m.put("hiddenSaturatedFraction",hs/(n*32.0));m.put("maxTrainingSearchQuantizationError",maxDifference);m.put("meanAbsNullAntisymmetryErrorUnits",nullAbs/n);return m;
    }
    public static void main(String[] args)throws Exception {
        String mode=args[0];Path root=Path.of(args[1]),out=Path.of(args[2]);Files.createDirectory(out);
        if(mode.equals("freeze")){freeze(root,out,args.length>3?Long.parseLong(args[3]):1);return;}
        String trainingFile=mode.equals("full")?"full.bin":"train.bin";
        var train=read(root.resolve(trainingFile));var held=read(root.resolve("held.bin"));
        if(mode.equals("probe")) {
            var excluded=new HashSet<String>();for(var s:train)excluded.add(key(s.board()));
            if(args.length>4)for(var s:read(Path.of(args[4])))excluded.add(key(s.board()));
            int original=held.size();held.removeIf(s->excluded.contains(key(s.board())));
            Path state=Path.of(args[3]);NnueTrainer t;
            try(var in=new BufferedInputStream(Files.newInputStream(state))){t=TrainingStateCodec.readMaterial(in);}
            json(out.resolve("probe.json"),Map.of("stateSha256",hash(state),"excludedAliases",original-held.size(),
                    "originalProbeCount",original,"optimizerStep",t.optimizer().step(),"probe",metrics(t,held)));
            return;
        }
        if(mode.equals("checkpoint")) {
            Path path=Path.of(args[3]);var t=checkpoint(path);
            json(out.resolve("checkpoint.json"),Map.of("manifest",CheckpointInspection.manifest(path),"optimizer",t.optimizer().hyperparameters(),
                    "probe",metrics(t,held),"modelOptimizerExact",true,"calibratedOutcome",t.calibratedOutcome(),
                    "probeNote","Diagnostic positions only: independent of the ordinary Gen1 tranche, but may have been consumed by a different Arena/campaign"));
            return;
        }
        if(mode.equals("audit")) {
            Path lineage=Path.of(args[3]);json(out.resolve("lineage.json"),TrainingLineage.read(lineage).orElseThrow());json(out.resolve("validations.json"),CheckpointInspection.validations(lineage));
            var partial=PartialGeneration.inspect(lineage);
            if(partial.isPresent())json(out.resolve("partial-games.json"),Map.of("candidate",partial.get().candidate(),"pairs",partial.get().pairs(),"training",partial.get().training()));
            if(args.length>4) {
                var campaign=com.ohinteractive.seedv6.training.service.LearningArenaState.read(Path.of(args[4]));
                json(out.resolve("arena.json"),campaign);
            }
            try(var dirs=Files.list(lineage.resolve("checkpoints"))) {
                for(var p:dirs.sorted().toList()) {var t=checkpoint(p);json(out.resolve(p.getFileName()+".json"),Map.of("manifest",CheckpointInspection.manifest(p),"optimizer",t.optimizer().hyperparameters(),"training",metrics(t,train),"probe",metrics(t,held),"modelOptimizerExact",true));System.out.println("audited "+p.getFileName());}
            }
        } else if(mode.equals("train")||mode.equals("calibrated")||mode.equals("production")||mode.equals("full")) {
            long seed=Long.parseLong(args[3]);int epochs=Integer.parseInt(args[4]);double lr=Double.parseDouble(args[5]);
            NnueTrainer t;
            if(args.length>6)try(var in=new BufferedInputStream(Files.newInputStream(Path.of(args[6])))){t=TrainingStateCodec.readMaterial(in);}
            else t=mode.equals("train")?NnueTrainer.materialParity(TrainableNnue.initialized(seed)):NnueTrainer.calibratedMaterialParity(TrainableNnue.initialized(seed));
            if(t.calibratedOutcome()==mode.equals("train"))throw new IllegalArgumentException("Parent objective differs from requested experiment");
            t.optimizer().setLearningRate(lr);
            long shuffleSeed=mode.equals("full")?new SplittableRandom(seed ^ 0xBB67AE8584CAA73BL
                    ^ (JSON.fromJson(Files.readString(root.resolve("data.json")),JsonObject.class).get("generation").getAsLong()*0x9E3779B97F4A7C15L)).nextLong():202610051302L;
            json(out.resolve("config.json"),Map.of("mode",mode,"seed",seed,"epochs",epochs,"batch",128,"learningRate",lr,"trainingSha256",hash(root.resolve(trainingFile)),"heldSha256",hash(root.resolve("held.bin")),"initialStep",t.optimizer().step(),"parent",args.length>6?args[6]:"fresh","shuffleSeed",shuffleSeed));
            var events=new ArrayList<Object>();events.add(Map.of("updates",t.optimizer().step(),"training",metrics(t,train),"held",metrics(t,held)));
            var norms=new ArrayList<Object>();
            var random=new SplittableRandom(shuffleSeed);int[] order=new int[train.size()];long[][] boards=new long[128][];double[] targets=new double[128];
            for(int epoch=0;epoch<epochs;epoch++) {
                for(int i=0;i<order.length;i++)order[i]=i;
                for(int i=order.length-1;i>0;i--){int j=random.nextInt(i+1),v=order[i];order[i]=order[j];order[j]=v;}
                for(int start=0;start<order.length;start+=128) {
                    int count=Math.min(128,order.length-start);for(int i=0;i<count;i++){var s=train.get(order[start+i]);boards[i]=s.board();targets[i]=s.target();}
                    long next=t.optimizer().step()+1;boolean measure=next==1||next==4||next==16||start+count==order.length;
                    float[][] before=measure?Arrays.stream(t.model().parameters.groups).map(float[]::clone).toArray(float[][]::new):null;
                    t.trainBatch(boards,targets,count);
                    if(measure)norms.add(Map.of("step",t.optimizer().step(),"norms",norms(t,before)));
                    long step=t.optimizer().step();if(step==1||step==4||step==16)events.add(Map.of("updates",step,"training",metrics(t,train),"held",metrics(t,held)));
                }
                var event=Map.of("epoch",epoch+1,"updates",t.optimizer().step(),"training",metrics(t,train),"held",metrics(t,held));events.add(event);json(out.resolve("epoch-"+(epoch+1)+".json"),event);
                System.out.println("epoch "+(epoch+1)+" updates "+t.optimizer().step()+" held loss "+((Map<?,?>)event.get("held")).get("halfMse"));
            }
            try(var stream=new BufferedOutputStream(Files.newOutputStream(out.resolve("training.state"),StandardOpenOption.CREATE_NEW))){TrainingStateCodec.writeMaterial(t,stream);}
            try(var stream=new BufferedOutputStream(Files.newOutputStream(out.resolve("network.nnuem"),StandardOpenOption.CREATE_NEW))){new NetworkTrainingState.NnueMaterial(t).snapshot().write(stream);}
            json(out.resolve("events.json"),events);
            json(out.resolve("norms.json"),norms);
            try(var in=new BufferedInputStream(Files.newInputStream(out.resolve("training.state")))) {
                var restored=TrainingStateCodec.readMaterial(in);
                if(!Arrays.equals(TrainingStateCodec.encodeMaterial(t),TrainingStateCodec.encodeMaterial(restored)))throw new AssertionError("Resume bytes");
                for(var s:held)if(t.predict(s.board())!=restored.predict(s.board()))throw new AssertionError("Resume prediction");
                var s=train.getFirst();for(var copy:List.of(t,restored))copy.trainBatch(new long[][]{s.board()},new double[]{s.target()},1);
                if(!Arrays.equals(TrainingStateCodec.encodeMaterial(t),TrainingStateCodec.encodeMaterial(restored)))throw new AssertionError("Resume next step");
                json(out.resolve("persistence.json"),Map.of("optimizerRoundTripExact",true,"nextStepExact",true,"predictionsExact",held.size(),"modelSha256",hash(out.resolve("network.nnuem")),"optimizerSha256",hash(out.resolve("training.state"))));
            }
        } else throw new IllegalArgumentException("freeze|audit|train (historical v1)|production|full");
    }
}
