package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.nnue.*;
import com.ohinteractive.seedv6.core.nnue.NnueNetworkCodec;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import java.util.function.ToDoubleFunction;

/** Explicit, isolated and non-shipped BRN experiments. Test labels stay sealed during tuning. */
public final class BrnResearchMain {
    static Map<String,Object> metrics(List<BrnResearchData.Example> data, ToDoubleFunction<BrnResearchData.Example> predict, boolean cpOutput) {
        double loss = 0, balanced = 0, mae = 0, mse = 0, residual = 0; int balancedCount = 0;
        double[] errors = new double[data.size()]; int at = 0;
        for (var e : data) {
            double raw = predict.applyAsDouble(e);
            double outcome = cpOutput ? NnueCorpusTargets.wdl(Math.round(raw * 100), e.sfMaterial()).target() : raw;
            double difference = outcome - e.outcome(), squared = .5 * difference * difference; loss += squared;
            if (Math.abs(e.cp()) <= 200) { balanced += squared; balancedCount++; }
            if (cpOutput) {
                double error = raw - e.cp() / 100.0; errors[at++] = Math.abs(error); mae += Math.abs(error); mse += error * error;
                double r = raw - RelationalCandidate.material(e.board()); residual += r * r;
            }
        }
        var out = new LinkedHashMap<String,Object>(); int n = data.size();
        out.put("examples", n); out.put("outcomeHalfMse", loss / n); out.put("balancedOutcomeHalfMse", balanced / balancedCount); out.put("balancedExamples", balancedCount);
        if (cpOutput) { Arrays.sort(errors); out.put("maePawns", mae / n); out.put("rmsePawns", Math.sqrt(mse / n));
            out.put("p90AbsErrorPawns", errors[(n - 1) * 90 / 100]); out.put("p99AbsErrorPawns", errors[(n - 1) * 99 / 100]); out.put("residualRmsPawns", Math.sqrt(residual / n)); }
        return out;
    }
    static int[] order(int count, long seed) {
        int[] order = new int[count]; for (int i = 0; i < count; i++) order[i] = i;
        var rng = new SplittableRandom(seed); for (int i = count - 1; i > 0; i--) { int j = rng.nextInt(i + 1), t = order[i]; order[i] = order[j]; order[j] = t; } return order;
    }
    public static void main(String[] args) throws Exception {
        if (args.length < 3) throw new IllegalArgumentException("prepare SOURCE DIR [train held] | r0/nnue DATA OUT [count epochs seed rate normalized]");
        Path dataPath = Path.of(args[1]), out = Path.of(args[2]);
        if (args[0].equals("prepare") || args[0].equals("prepare-uniform")) { BrnResearchData.prepare(dataPath, out, Integer.parseInt(args[3]), Integer.parseInt(args[4]),args.length>5?Long.parseLong(args[5]):0,args.length>6?Path.of(args[6]):null,args[0].equals("prepare-uniform")); return; }
        if (Files.exists(out)) throw new IOException("Use new experiment output");
        var data = BrnResearchData.read(dataPath, false);
        int count = args.length > 3 ? Integer.parseInt(args[3]) : 8192, epochs = args.length > 4 ? Integer.parseInt(args[4]) : 1;
        long seed = args.length > 5 ? Long.parseLong(args[5]) : 71;
        double rate = args.length > 6 ? Double.parseDouble(args[6]) : .03;
        boolean normalized = args.length <= 7 || Boolean.parseBoolean(args[7]);
        double relativeScale = args.length > 8 ? Double.parseDouble(args[8]) : .25;
        double fineTuneRate = args.length > 9 ? Double.parseDouble(args[9]) : rate;
        int relationWidth = args.length > 10 ? Integer.parseInt(args[10]) : 8;
        double fineTuneAuxiliary = args.length > 11 ? Double.parseDouble(args[11]) : .05;
        long warmupExposure = args.length > 12 ? Long.parseLong(args[12]) : (long)count*2;
        double warmupAuxiliary = args.length > 13 ? Double.parseDouble(args[13]) : .05;
        boolean denseAdam = args.length>14 && Boolean.parseBoolean(args[14]);
        if (count < 2 || count > data.training().size() || epochs < 1 || epochs > 100) throw new IllegalArgumentException("Bad exposure");
        if (!Set.of("r0", "r0-wdl", "r1", "r1-wdl", "r1-stm-wdl", "r1-ce", "r2", "r2-wdl", "r3-wdl", "r3-pure-wdl", "r3-ce", "r4-wdl", "r4-ce", "r4-stm-ce", "r4-stm-wdl", "r4-stm-hybrid", "r4-relu-ce", "r4-relu-hybrid", "r4-relu-relative-ce", "r4-relu-relative-typed-ce", "r4-relu-relative-hybrid", "nnue").contains(args[0])) throw new IllegalArgumentException("Unknown candidate");
        Files.createDirectories(out);
        var training = data.training().subList(0, count);
        var events = new ArrayList<Map<String,Object>>();
        var report = new LinkedHashMap<String,Object>();
        report.put("candidate", args[0]); report.put("seed", seed); report.put("distinctTraining", count); report.put("epochs", epochs);
        report.put("relativeScale",relativeScale);
        report.put("fineTuneRate",fineTuneRate);report.put("relationWidth",relationWidth);
        report.put("fineTuneAuxiliary",fineTuneAuxiliary);
        report.put("warmupExposure",warmupExposure);
        report.put("warmupAuxiliary",warmupAuxiliary);
        report.put("denseAdam",denseAdam);
        report.put("data", dataPath.toAbsolutePath().toString()); report.put("datasetManifest", DataFiles.read(dataPath.resolve("manifest.json"), Map.class));
        report.put("materialValidation", metrics(data.validation(), e -> RelationalCandidate.material(e.board()), true));
        long begun = System.nanoTime(), trainingNanos = 0;
        var r0 = args[0].startsWith("r0") ? new RelationalCandidate(normalized) : null;
        var r1 = args[0].startsWith("r1") ? new PooledRelationCandidate(seed, !args[0].contains("-stm")) : null;
        var r2 = args[0].startsWith("r2") ? new MessageRelationCandidate(seed) : null;
        var r3 = args[0].startsWith("r3") ? new AnchoredRelationCandidate(seed) : null;
        var r4 = args[0].startsWith("r4") ? new AbsoluteRelationCandidate(seed,args[0].contains("-stm"),args[0].contains("-relu"),args[0].contains("-relative"),relativeScale,relationWidth,denseAdam,args[0].contains("-typed")) : null;
        var nnue = args[0].equals("nnue") ? new NnueTrainer(TrainableNnue.initialized(seed)) : null;
        report.put("parameterCount",r0!=null?r0.weights.length:r1!=null?r1.weights.length:r2!=null?r2.weights.length:r3!=null?r3.weights.length:r4!=null?r4.weights.length:3_149_953);
        var implementations=new LinkedHashMap<String,String>();
        for(var type:new Class<?>[]{BrnResearchMain.class,BrnResearchData.class,RelationalCandidate.class,NnueCorpusTargets.class,
                r0!=null?RelationalCandidate.class:r1!=null?PooledRelationCandidate.class:r2!=null?MessageRelationCandidate.class:r3!=null?AnchoredRelationCandidate.class:r4!=null?AbsoluteRelationCandidate.class:NnueTrainer.class}) {
            try(var input=new java.security.DigestInputStream(type.getResourceAsStream("/"+type.getName().replace('.','/')+".class"),DataFiles.digest())){
                input.transferTo(OutputStream.nullOutputStream());implementations.put(type.getName(),HexFormat.of().formatHex(input.getMessageDigest().digest()));
            }
        }
        report.put("implementationClassesSha256",implementations);
        ToDoubleFunction<BrnResearchData.Example> prediction = e -> r0 != null ? r0.predict(e.board()) : r1 != null ? r1.predict(e.board()) : r2 != null ? r2.predict(e.board()) : r3 != null ? r3.predict(e.board()) : r4 != null ? r4.predict(e.board()) : nnue.predict(e.board());
        report.put("rate", nnue != null ? nnue.optimizer().hyperparameters().learningRate() : rate);
        report.put("normalized", normalized); report.put("initialTraining", metrics(training, prediction, nnue == null));
        report.put("initialValidation", metrics(data.validation(), prediction, nnue == null));
        double bestLoss = Double.POSITIVE_INFINITY;
        for (int epoch = 0; epoch < epochs; epoch++) {
            int[] order = order(count, seed + epoch); long start = System.nanoTime();
            if (r0 != null) {
                for (int index : order) { var e = training.get(index); if (args[0].equals("r0-wdl")) r0.trainOutcome(e, rate); else r0.train(e.board(), e.cp() / 100.0, rate); }
            } else if (r1 != null) {
                for (int index : order) r1.train(training.get(index), rate, args[0].endsWith("-wdl"), args[0].endsWith("-ce"));
            } else if (r2 != null) {
                for (int index : order) r2.train(training.get(index), rate, args[0].equals("r2-wdl"));
            } else if (r3 != null) {
                for (int offset = 0; offset < count; offset += 128) r3.trainBatch(training, order, offset, Math.min(128, count - offset), rate, args[0].equals("r3-pure-wdl") ? 0 : .05, args[0].endsWith("-ce"));
            } else if (r4 != null) {
                for (int offset = 0; offset < count; offset += 128) {
                    boolean hybrid=args[0].endsWith("-hybrid"),fineTuning=hybrid && (long)epoch*count+offset>=warmupExposure;
                    r4.trainBatch(training, order, offset, Math.min(128, count-offset), fineTuning ? fineTuneRate : rate,
                            args[0].endsWith("-ce") || hybrid && !fineTuning, fineTuning ? fineTuneAuxiliary : warmupAuxiliary);
                }
            } else {
                int batchSize = 128; long[][] boards = new long[batchSize][]; double[] targets = new double[batchSize];
                for (int offset = 0; offset < count; offset += batchSize) {
                    int batch = Math.min(batchSize, count - offset);
                    for (int i = 0; i < batch; i++) { var e = training.get(order[offset + i]); boards[i] = e.board(); targets[i] = e.outcome(); }
                    nnue.trainBatch(boards, targets, batch);
                }
            }
            trainingNanos += System.nanoTime() - start;
            var validation = metrics(data.validation(), prediction, nnue == null);
            var event = new LinkedHashMap<String,Object>(); event.put("epoch", epoch + 1); event.put("exposure", (long) count * (epoch + 1));
            event.put("trainingSeconds", trainingNanos / 1e9); event.put("training", metrics(training, prediction, nnue == null)); event.put("validation", validation);
            events.add(event); System.out.println(DataFiles.JSON.toJson(event));
            double loss = (double) validation.get("outcomeHalfMse");
            if (loss < bestLoss) {
                bestLoss = loss; report.put("selectedEpoch", epoch + 1); report.put("selectedValidation", validation);
                try (var output = new BufferedOutputStream(Files.newOutputStream(out.resolve("selected.state")))) {
                    if (r0 != null) r0.write(output); else if (r1 != null) r1.write(output); else if (r2 != null) r2.write(output); else if (r3 != null) r3.write(output); else if (r4 != null) r4.write(output); else TrainingStateCodec.write(nnue, output);
                }
                if (nnue != null) try (var output = new BufferedOutputStream(Files.newOutputStream(out.resolve("selected.nnue")))) { NnueNetworkCodec.write(nnue.model().snapshot(), output); }
            }
            report.put("events", events); report.put("elapsedSeconds", (System.nanoTime() - begun) / 1e9);
            DataFiles.write(out.resolve("result.json"), report);
        }
        System.out.println("RESULT " + out.toAbsolutePath());
    }
    private BrnResearchMain() {}
}
