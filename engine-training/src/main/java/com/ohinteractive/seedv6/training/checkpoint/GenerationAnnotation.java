package com.ohinteractive.seedv6.training.checkpoint;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** User metadata, separate from immutable model, optimizer, validation and history evidence. */
public record GenerationAnnotation(String checkpointId, String notes, List<String> tags, Instant modified) {
    public GenerationAnnotation {
        CheckpointManifest.requireId(checkpointId);
        Objects.requireNonNull(notes); Objects.requireNonNull(tags); Objects.requireNonNull(modified);
        if (notes.length() > 8000 || tags.size() > 32)
            throw new IllegalArgumentException("Use at most 8000 note characters and 32 tags.");
        var normalized = new LinkedHashSet<String>();
        for (String tag : tags) {
            tag = Objects.requireNonNull(tag).strip();
            if (tag.isEmpty() || tag.length() > 80 || tag.chars().anyMatch(Character::isISOControl))
                throw new IllegalArgumentException("Tags must contain 1 to 80 printable characters.");
            normalized.add(tag);
        }
        tags = List.copyOf(normalized);
    }

    static Path file(Path root, String checkpointId) {
        CheckpointManifest.requireId(checkpointId);
        return root.resolve("annotations").resolve(checkpointId + ".bin");
    }

    public static Optional<GenerationAnnotation> read(Path root, String checkpointId) throws IOException {
        Path file = file(root, checkpointId);
        if (Files.notExists(file, LinkOption.NOFOLLOW_LINKS)) return Optional.empty();
        CheckpointPayload.regular(file);
        var value = SmallRecord.read(file, "generation-annotation-v1", in -> {
            String id = in.readUTF(), notes = in.readUTF();
            int count = in.readInt();
            if (count < 0 || count > 32) throw new IOException("Invalid annotation tag count.");
            var tags = new ArrayList<String>();
            for (int i = 0; i < count; i++) tags.add(in.readUTF());
            return new GenerationAnnotation(id, notes, tags, Instant.parse(in.readUTF()));
        });
        if (!value.checkpointId().equals(checkpointId)) throw new IOException("Annotation/checkpoint mismatch.");
        return Optional.of(value);
    }

    byte[] encode() throws IOException {
        return SmallRecord.encode("generation-annotation-v1", out -> {
            out.writeUTF(checkpointId); out.writeUTF(notes); out.writeInt(tags.size());
            for (String tag : tags) out.writeUTF(tag);
            out.writeUTF(modified.toString());
        });
    }
}
