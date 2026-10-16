package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.corpus.CorpusPosition;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** One immutable, checksummed set of logical positions AND resolved targets, selected before either trainer runs. */
final class LearningArenaTranche {
    record Row(long ordinal, CorpusPosition position, int targetKind, int target, int perspective,
               double outcome) implements TrainingPosition {}
    record Manifest(String binding, String source, long start, long end, int count, String hash) {}
    final Manifest manifest;
    final List<Row> rows;
    private LearningArenaTranche(Manifest manifest, List<Row> rows) {
        this.manifest = manifest; this.rows = List.copyOf(rows);
    }
    static LearningArenaTranche obtain(Path round, Path seek, LearningArenaConfig config, long start,
                                      BooleanSupplier paused) throws IOException {
        Path destination = round.resolve("tranche");
        if (Files.exists(destination)) return read(destination, config, start);
        var source = config.source(); source.verify(); source.requireReady();
        var a = CorpusTraining.targetPolicy(config.a().architecture(), source.labelProfile());
        var b = CorpusTraining.targetPolicy(config.b().architecture(), source.labelProfile());
        Files.createDirectories(round);
        var rows = new ArrayList<Row>();
        long end;
        try (var reader = SourceReaders.open(source, seek, start)) {
            long budget = Math.addExact(1000, Math.multiplyExact(100L, config.positionsPerRound()));
            while (rows.size() < config.positionsPerRound()) {
                if (paused.getAsBoolean()) throw new InterruptedIOException("Paused before tranche publication");
                if (reader.nextPosition() - start >= budget) throw new IOException("Shared source skip budget exhausted; no tranche selected");
                var entry = reader.next();
                if (entry == null) throw new EOFException("Shared source exhausted at " + reader.nextPosition() + "; no wrapping or substitution");
                var p = entry.position();
                if (p == null || a.rejection(p) != null || b.rejection(p) != null) continue;
                long[] board = p.position().toBoard(0);
                double target = a.target(p, board);
                if (Double.doubleToLongBits(target) != Double.doubleToLongBits(b.target(p, board)))
                    throw new IOException("Competitor target policies disagree at source record " + entry.ordinal());
                rows.add(new Row(entry.ordinal(), p.position(), p.targetKind(), p.target(), p.perspective(), target));
            }
            end = reader.nextPosition();
        }
        source.verify();
        Path stage = Files.createTempDirectory(round, "tranche-pending-");
        LearningArenaFiles.write(stage.resolve("records.bin"), stream -> {
            var out = new DataOutputStream(stream); out.writeInt(1); out.writeInt(rows.size());
            for (var r : rows) {
                var p = r.position(); out.writeLong(r.ordinal());
                out.writeLong(p.plane0()); out.writeLong(p.plane1()); out.writeLong(p.plane2()); out.writeLong(p.plane3());
                out.writeInt(p.rules()); out.writeInt(p.halfmove());
                out.writeInt(r.targetKind()); out.writeInt(r.target()); out.writeInt(r.perspective()); out.writeDouble(r.outcome());
            }
        });
        var manifest = new Manifest(config.identity(), source.identity(), start, end, rows.size(),
                LearningArenaFiles.hash(stage.resolve("records.bin")));
        DataFiles.write(stage.resolve("manifest.json"), manifest);
        LearningArenaFiles.forceDirectory(stage);
        Files.move(stage, destination, StandardCopyOption.ATOMIC_MOVE);
        LearningArenaFiles.forceDirectory(round);
        return read(destination, config, start);
    }
    static LearningArenaTranche read(Path directory, LearningArenaConfig config, long start) throws IOException {
        var m = DataFiles.read(directory.resolve("manifest.json"), Manifest.class);
        if (!config.identity().equals(m.binding()) || !config.source().identity().equals(m.source())
                || start != m.start() || m.end() <= start || m.count() != config.positionsPerRound()
                || !LearningArenaFiles.hash(directory.resolve("records.bin")).equals(m.hash()))
            throw new IOException("Shared tranche identity/content mismatch");
        var rows = new ArrayList<Row>();
        try (var in = new DataInputStream(new BufferedInputStream(Files.newInputStream(directory.resolve("records.bin"))))) {
            if (in.readInt() != 1 || in.readInt() != m.count()) throw new IOException("Invalid shared tranche header");
            long previous = start - 1;
            for (int i = 0; i < m.count(); i++) {
                long ordinal = in.readLong();
                if (ordinal <= previous || ordinal >= m.end()) throw new IOException("Invalid tranche source ordinal");
                previous = ordinal;
                rows.add(new Row(ordinal, new CorpusPosition(in.readLong(), in.readLong(), in.readLong(), in.readLong(), in.readInt(), in.readInt()),
                        in.readInt(), in.readInt(), in.readInt(), in.readDouble()));
            }
            if (in.read() != -1) throw new IOException("Trailing tranche bytes");
        } catch (IllegalArgumentException invalid) { throw new IOException("Invalid tranche position", invalid); }
        var tranche = new LearningArenaTranche(m, rows);
        tranche.examples(config.a().architecture(), config.source().labelProfile());
        tranche.examples(config.b().architecture(), config.source().labelProfile());
        return tranche;
    }
    CorpusTraining.Examples examples(TrainingArchitecture architecture, LabelProfile profile) throws IOException {
        var adapter = CorpusTraining.targetPolicy(architecture, profile);
        var examples = new CorpusTraining.Examples(adapter);
        for (var row : rows) {
            if (adapter.rejection(row) != null || !Double.isFinite(row.outcome())
                    || Double.doubleToLongBits(adapter.target(row, row.position().toBoard(0))) != Double.doubleToLongBits(row.outcome()))
                throw new IOException("Frozen target no longer matches the architecture's existing target policy");
            examples.add(row, adapter);
        }
        return examples;
    }
}
