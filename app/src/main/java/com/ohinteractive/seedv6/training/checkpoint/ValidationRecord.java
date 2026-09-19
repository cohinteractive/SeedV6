package com.ohinteractive.seedv6.training.checkpoint;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.selfplay.GameTermination;
import com.ohinteractive.seedv6.training.validation.PromotionPolicy;
import com.ohinteractive.seedv6.training.validation.ValidationConfig;
import com.ohinteractive.seedv6.training.validation.ValidationResult;

/** Immutable V1 aggregate evidence; starting board/history are bound by SHA-256, raw games are omitted. */
public record ValidationRecord(String id, String candidateId, String incumbentId, ValidationConfig config,
                               String startingStateHash, ValidationResult.Statistics statistics,
                               PromotionPolicy policy, PromotionPolicy.Assessment assessment) {
    public ValidationRecord {
        requireId(id);
        CheckpointManifest.requireId(candidateId);
        CheckpointManifest.requireId(incumbentId);
        SmallRecord.requireHash(startingStateHash);
        if ((long) statistics.validPairs() + statistics.incompletePairs() != config.openingPairs()
                || statistics.totalPlies() > 2L * config.openingPairs() * config.maximumPlies()
                || !assessment.equals(policy.assess(statistics.validPairs(), statistics.pairScoreSum()))) {
            throw new IllegalArgumentException("Validation aggregates/policy disagree.");
        }
    }
    static String requireId(String id) {
        if (id == null || !id.matches("v-[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid validation ID.");
        return id;
    }
    static ValidationRecord create(String candidate, String incumbent, ValidationResult result, PromotionPolicy policy)
            throws IOException {
        var provisional = new ValidationRecord("v-" + "0".repeat(64), candidate, incumbent, result.config(),
                result.startingStateHash(), result.statistics(), policy, result.assess(policy));
        return new ValidationRecord("v-" + SmallRecord.hash(provisional.encode()), candidate, incumbent, result.config(),
                result.startingStateHash(), result.statistics(), policy, result.assess(policy));
    }
    // ID is content-addressed by these bytes; it is the filename and is not self-embedded.
    byte[] encode() throws IOException { return SmallRecord.encode("validation", this::write); }
    private void write(DataOutputStream out) throws IOException {
        out.writeUTF(candidateId);
        out.writeUTF(incumbentId);
        out.writeUTF(ValidationConfig.SEARCH_POLICY);
        out.writeInt(config.openingPairs());
        out.writeLong(config.seed());
        out.writeInt(config.minimumOpeningPlies());
        out.writeInt(config.maximumOpeningPlies());
        out.writeInt(config.depth());
        out.writeInt(config.threads());
        out.writeDouble(config.scoreMapping().scale());
        out.writeInt(config.maximumPlies());
        out.writeUTF(startingStateHash);
        out.writeInt(statistics.validPairs());
        out.writeInt(statistics.incompletePairs());
        writeColour(out, statistics.white());
        writeColour(out, statistics.black());
        out.writeLong(statistics.totalPlies());
        out.writeInt(GameTermination.values().length - 1);
        for (GameTermination reason : GameTermination.values()) if (reason != GameTermination.ACTIVE) {
            out.writeUTF(reason.name());
            out.writeInt(statistics.terminations().getOrDefault(reason, 0));
        }
        out.writeInt(policy.minimumPairs());
        out.writeDouble(policy.alpha());
        out.writeDouble(policy.requiredMargin());
        out.writeDouble(assessment.mean());
        out.writeDouble(assessment.radius());
        out.writeDouble(assessment.lowerBound());
        out.writeDouble(assessment.threshold());
        out.writeUTF(assessment.decision().name());
    }
    static ValidationRecord read(String id, DataInputStream in) throws IOException {
        String candidate = in.readUTF(), incumbent = in.readUTF();
        if (!in.readUTF().equals(ValidationConfig.SEARCH_POLICY)) throw new IOException("Unknown search policy.");
        var config = new ValidationConfig(in.readInt(), in.readLong(), in.readInt(), in.readInt(),
                in.readInt(), in.readInt(), new NnueScoreMapping(in.readDouble()), in.readInt());
        String startHash = in.readUTF();
        int valid = in.readInt(), incomplete = in.readInt();
        var white = readColour(in);
        var black = readColour(in);
        long plies = in.readLong();
        if (in.readInt() != GameTermination.values().length - 1) throw new IOException("Unknown termination format.");
        var reasons = new EnumMap<GameTermination, Integer>(GameTermination.class);
        for (GameTermination reason : GameTermination.values()) if (reason != GameTermination.ACTIVE) {
            if (!in.readUTF().equals(reason.name())) throw new IOException("Unknown termination reason/order.");
            int count = in.readInt();
            if (count != 0) reasons.put(reason, count);
        }
        var stats = new ValidationResult.Statistics(valid, incomplete, white, black, plies, reasons);
        var policy = new PromotionPolicy(in.readInt(), in.readDouble(), in.readDouble());
        var assessment = new PromotionPolicy.Assessment(valid, in.readDouble(), in.readDouble(),
                in.readDouble(), in.readDouble(), PromotionPolicy.Decision.valueOf(in.readUTF()));
        return new ValidationRecord(id, candidate, incumbent, config, startHash, stats, policy, assessment);
    }
    private static void writeColour(DataOutputStream out, ValidationResult.ColourRecord colour) throws IOException {
        out.writeInt(colour.wins()); out.writeInt(colour.draws()); out.writeInt(colour.losses());
    }
    private static ValidationResult.ColourRecord readColour(DataInputStream in) throws IOException {
        return new ValidationResult.ColourRecord(in.readInt(), in.readInt(), in.readInt());
    }
}

