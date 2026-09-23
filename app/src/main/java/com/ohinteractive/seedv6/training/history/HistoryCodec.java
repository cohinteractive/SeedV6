package com.ohinteractive.seedv6.training.history;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;

/** Versioned UTF-8 tab-separated key=value fields; '-' is an explicitly absent measurement. */
final class HistoryCodec {
    static String encode(GenerationRecord r) {
        String[] names = {"schema", "generation", "candidate", "incumbent", "best", "outcome", "decision", "wins", "draws", "losses", "validPairs", "incompletePairs", "score", "lower", "threshold", "depth", "games", "pairs", "threads", "completedGames", "abortedGames", "samples", "loss", "started", "completed", "selfPlayNs", "trainingNs", "validationNs", "totalNs"};
        Object[] values = {r.bootstrap() == null ? GenerationRecord.SCHEMA : r.bootstrap().separated() ? 4 : r.bootstrap().supervision().blended() ? 3 : 2, r.generation(), r.candidate(), r.incumbent(), r.resultingBest(), r.outcome(), r.decision(), r.wins(), r.draws(), r.losses(), r.validPairs(), r.incompletePairs(), r.score(), r.lowerBound(), r.threshold(), r.regime().depth(), r.regime().games(), r.regime().pairs(), r.regime().threads(), r.completedGames(), r.abortedGames(), r.samples(), r.loss(), r.started(), r.completed(), r.selfPlayNanos(), r.trainingNanos(), r.validationNanos(), r.totalNanos()};
        StringJoiner line = new StringJoiner("\t");
        for (int i = 0; i < names.length; i++) line.add(names[i] + "=" + (values[i] == null ? "-" : values[i]));
        if (r.bootstrap() != null) {
            var b = r.bootstrap();
            line.add("validationKind=" + (b.supervision().blended() ? "BOOTSTRAP_NNUE_BLENDED_LOSS" : "BOOTSTRAP_WDL_LOSS"));
            line.add("generatorStore=" + Base64.getUrlEncoder().encodeToString(b.generatorStore().getBytes(StandardCharsets.UTF_8)));
            line.add("generatorId=" + b.generatorId()); line.add("generatorHash=" + b.generatorHash()); line.add("dataHash=" + b.dataHash());
            line.add("splitSeed=" + b.splitSeed()); line.add("trainingSamples=" + b.trainingSamples());
            line.add("trainingGames=" + b.trainingGames()); line.add("heldOutGames=" + b.heldOutGames());
            line.add("heldOutSamples=" + b.comparison().samples()); line.add("candidateLoss=" + b.comparison().candidateLoss());
            line.add("bestLoss=" + b.comparison().bestLoss());
            if (b.separated()) {
                line.add("generatorMode=" + b.generatorMode().name());
                line.add("teacherStore=" + Base64.getUrlEncoder().encodeToString(b.teacherStore().getBytes(StandardCharsets.UTF_8)));
                line.add("teacherId=" + b.teacherId()); line.add("teacherHash=" + b.teacherHash());
            }
            if (b.supervision().blended()) {
                line.add("supervision=" + b.supervision().mode().name()); line.add("teacherWeight=" + b.supervision().teacherWeight());
                line.add("candidateWdlLoss=" + b.wdlLoss().candidateLoss()); line.add("bestWdlLoss=" + b.wdlLoss().bestLoss());
                line.add("candidateTeacherLoss=" + b.teacherLoss().candidateLoss()); line.add("bestTeacherLoss=" + b.teacherLoss().bestLoss());
            }
        }
        String payload = line.toString();
        return payload + "\tsha256=" + hash(payload);
    }
    static GenerationRecord decode(String line) {
        int checksum = line.lastIndexOf("\tsha256=");
        if (checksum < 0 || !hash(line.substring(0, checksum)).equals(line.substring(checksum + 8)))
            throw new IllegalArgumentException("Checksum mismatch / interrupted record");
        Map<String, String> f = new HashMap<>();
        for (String field : line.substring(0, checksum).split("\t")) {
            int equals = field.indexOf('=');
            if (equals < 1 || f.put(field.substring(0, equals), field.substring(equals + 1)) != null)
                throw new IllegalArgumentException("Invalid / duplicate field");
        }
        if (!"1".equals(f.get("schema")) && !"2".equals(f.get("schema")) && !"3".equals(f.get("schema")) && !"4".equals(f.get("schema"))) throw new IllegalArgumentException("Unsupported history schema " + f.get("schema"));
        return new GenerationRecord(l(f,"generation"), s(f,"candidate"), s(f,"incumbent"), s(f,"best"),
                GenerationRecord.Outcome.valueOf(s(f,"outcome")), PromotionPolicy.Decision.valueOf(s(f,"decision")),
                i(f,"wins"), i(f,"draws"), i(f,"losses"), i(f,"validPairs"), i(f,"incompletePairs"),
                d(f,"score"), d(f,"lower"), d(f,"threshold"),
                new GenerationRecord.Regime(i(f,"depth"), i(f,"games"), i(f,"pairs"), i(f,"threads")),
                i(f,"completedGames"), i(f,"abortedGames"), l(f,"samples"), d(f,"loss"),
                instant(f,"started"), instant(f,"completed"), l(f,"selfPlayNs"), l(f,"trainingNs"), l(f,"validationNs"), l(f,"totalNs"), bootstrap(f));
    }
    private static com.ohinteractive.seedv6.training.checkpoint.BootstrapEvidence bootstrap(Map<String,String> f) {
        if ("1".equals(f.get("schema"))) return null;
        boolean separated = "4".equals(f.get("schema"));
        boolean blended = "3".equals(f.get("schema")) || separated && "NNUE_BLENDED".equals(f.get("supervision"));
        if (!(blended ? "BOOTSTRAP_NNUE_BLENDED_LOSS" : "BOOTSTRAP_WDL_LOSS").equals(s(f,"validationKind"))) throw new IllegalArgumentException("Unknown validation kind");
        String store = new String(Base64.getUrlDecoder().decode(s(f,"generatorStore")), StandardCharsets.UTF_8);
        String id = s(f,"generatorId"), hash = s(f,"generatorHash");
        var objective = blended ? new com.ohinteractive.seedv6.training.service.BrnSupervision(
                com.ohinteractive.seedv6.training.service.BrnSupervision.Mode.valueOf(s(f,"supervision")), d(f,"teacherWeight"))
                : com.ohinteractive.seedv6.training.service.BrnSupervision.WDL;
        var comparison = new com.ohinteractive.seedv6.training.validation.HeldOutLoss.Comparison(i(f,"heldOutSamples"), d(f,"candidateLoss"), d(f,"bestLoss"));
        var wdl = blended ? new com.ohinteractive.seedv6.training.validation.HeldOutLoss.Comparison(i(f,"heldOutSamples"), d(f,"candidateWdlLoss"), d(f,"bestWdlLoss")) : null;
        var teacher = blended ? new com.ohinteractive.seedv6.training.validation.HeldOutLoss.Comparison(i(f,"heldOutSamples"), d(f,"candidateTeacherLoss"), d(f,"bestTeacherLoss")) : null;
        return new com.ohinteractive.seedv6.training.checkpoint.BootstrapEvidence(store, id, hash, s(f,"dataHash"), l(f,"splitSeed"),
                i(f,"trainingSamples"), i(f,"trainingGames"), i(f,"heldOutGames"), comparison, objective, wdl, teacher,
                separated ? com.ohinteractive.seedv6.training.service.TrainingSource.Mode.valueOf(s(f,"generatorMode"))
                        : com.ohinteractive.seedv6.training.service.TrainingSource.Mode.NNUE_BOOTSTRAP,
                separated ? new String(Base64.getUrlDecoder().decode(s(f,"teacherStore")), StandardCharsets.UTF_8) : blended ? store : "",
                separated ? s(f,"teacherId") : blended ? id : "", separated ? s(f,"teacherHash") : blended ? hash : "", separated);
    }
    private static String s(Map<String,String> f, String k) {
        String v = Objects.requireNonNull(f.get(k), "Missing " + k); return v.equals("-") ? null : v;
    }
    private static Long l(Map<String,String> f, String k) { String v=s(f,k); return v==null?null:Long.valueOf(v); }
    private static Integer i(Map<String,String> f, String k) { String v=s(f,k); return v==null?null:Integer.valueOf(v); }
    private static Double d(Map<String,String> f, String k) { String v=s(f,k); return v==null?null:Double.valueOf(v); }
    private static Instant instant(Map<String,String> f, String k) { String v=s(f,k); return v==null?null:Instant.parse(v); }
    private static String hash(String s) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private HistoryCodec() {}
}
