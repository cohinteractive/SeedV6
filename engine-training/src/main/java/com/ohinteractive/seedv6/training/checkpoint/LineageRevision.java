package com.ohinteractive.seedv6.training.checkpoint;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** An observed prior configuration, archived before replacement. Not an invented generation recipe. */
public record LineageRevision(Instant archived, TrainingLineage lineage) {
    public static final String DIRECTORY = "lineage-revisions";
    public LineageRevision { Objects.requireNonNull(archived); Objects.requireNonNull(lineage); }
    String key() throws IOException { return SmallRecord.hash(lineage.encode()); }
    byte[] encode() throws IOException {
        return SmallRecord.encode("lineage-revision-v1", out -> { out.writeUTF(archived.toString()); lineage.write(out); });
    }
    public static List<LineageRevision> read(Path root) throws IOException {
        Path directory = root.resolve(DIRECTORY);
        if (Files.notExists(directory)) return List.of();
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Invalid lineage revision directory");
        var revisions = new ArrayList<LineageRevision>();
        try (var files = Files.list(directory)) {
            for (Path file : files.toList()) {
                CheckpointPayload.regular(file);
                var revision = SmallRecord.read(file, "lineage-revision-v1", in -> new LineageRevision(Instant.parse(in.readUTF()), TrainingLineage.read(in)));
                if (!file.getFileName().toString().equals(revision.key() + ".bin")) throw new IOException("Lineage revision identity mismatch");
                revisions.add(revision);
            }
        }
        revisions.sort(Comparator.comparing(LineageRevision::archived)); return List.copyOf(revisions);
    }
}
