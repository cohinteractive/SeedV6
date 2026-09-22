package com.ohinteractive.seedv6.training.checkpoint;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.selfplay.*;

/** Durable partition, written before any update/publication; resumes even if the generator later changes. */
public record BootstrapData(String planHash, BootstrapPartition partition, SelfPlayBatch.Statistics statistics,
                            long generationNanos) {
    private static final int MAX_BYTES = 64 * 1024 * 1024;
    public BootstrapData {
        SmallRecord.requireHash(planHash);
        if (generationNanos < 0 || partition.training().size() < 2 || partition.heldOut().size() < 2
                || partition.training().size() + partition.heldOut().size() != statistics.sampledPositions()
                || (long) statistics.sampledPositions() * 56 + 4L * statistics.completedGames() + 256 > MAX_BYTES
                || partition.trainingGames().size() + partition.heldOutGames().size() > statistics.completedGames())
            throw new IllegalArgumentException("Invalid bootstrap samples/accounting.");
    }
    byte[] encode() throws IOException {
        return SmallRecord.encode("brn-bootstrap-data-v1", out -> {
            out.writeUTF(planHash); out.writeLong(generationNanos);
            writeSamples(out, partition.training()); writeSamples(out, partition.heldOut());
            writeIds(out, partition.trainingGames()); writeIds(out, partition.heldOutGames());
            var s = statistics;
            out.writeInt(s.requestedGames()); out.writeInt(s.completedGames()); out.writeInt(s.abortedGames());
            out.writeInt(s.whiteWins()); out.writeInt(s.draws()); out.writeInt(s.blackWins()); out.writeInt(s.cappedGames());
            out.writeLong(s.totalPlayedPlies()); out.writeInt(s.minimumCompletedPlies()); out.writeInt(s.maximumCompletedPlies());
            out.writeDouble(s.meanCompletedPlies()); out.writeLong(s.rawTrajectoryPositions()); out.writeInt(s.sampledPositions());
        }, MAX_BYTES);
    }
    public String hash() throws IOException { return SmallRecord.hash(encode()); }
    static BootstrapData read(Path path) throws IOException {
        return SmallRecord.read(path, "brn-bootstrap-data-v1", in -> {
            String hash = in.readUTF(); long nanos = in.readLong();
            var train = readSamples(in); var held = readSamples(in);
            var partition = new BootstrapPartition(train, held, readIds(in), readIds(in));
            var stats = new SelfPlayBatch.Statistics(in.readInt(), in.readInt(), in.readInt(), in.readInt(),
                    in.readInt(), in.readInt(), in.readInt(), in.readLong(), in.readInt(), in.readInt(),
                    in.readDouble(), in.readLong(), in.readInt());
            return new BootstrapData(hash, partition, stats, nanos);
        }, MAX_BYTES);
    }
    private static void writeSamples(DataOutputStream out, List<TrajectorySampler.Sample> samples) throws IOException {
        out.writeInt(samples.size());
        for (var sample : samples) { for (long value : sample.board()) out.writeLong(value); out.writeDouble(sample.target()); }
    }
    private static List<TrajectorySampler.Sample> readSamples(DataInputStream in) throws IOException {
        int count = in.readInt();
        if (count < 2 || count > in.available() / (8 * (Board.MAX_BITBOARDS + 1))) throw new IOException("Invalid sample count.");
        var samples = new ArrayList<TrajectorySampler.Sample>(count);
        for (int i = 0; i < count; i++) {
            long[] board = new long[Board.MAX_BITBOARDS];
            for (int n = 0; n < board.length; n++) board[n] = in.readLong();
            samples.add(new TrajectorySampler.Sample(board, in.readDouble()));
        }
        return samples;
    }
    private static void writeIds(DataOutputStream out, List<Integer> ids) throws IOException {
        out.writeInt(ids.size()); for (int id : ids) out.writeInt(id);
    }
    private static List<Integer> readIds(DataInputStream in) throws IOException {
        int count = in.readInt();
        if (count < 2 || count > in.available() / 4) throw new IOException("Invalid partition game count.");
        var ids = new ArrayList<Integer>(count); for (int i = 0; i < count; i++) ids.add(in.readInt()); return ids;
    }
}
