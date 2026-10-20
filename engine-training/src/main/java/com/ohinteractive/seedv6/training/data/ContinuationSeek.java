package com.ohinteractive.seedv6.training.data;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Navigation is part of cursor inheritance. Callers hold both lineage writer locks.
 * Copies only absent indexes, never cursors, settings, reservations or optimizer state. */
public final class ContinuationSeek {
    public static void verify(DataSources selection, Map<String, Long> cursors, Path seek) throws IOException {
        for (var source : selection.sources()) {
            source.requireReady();
            if (source.format() == DataSource.Format.STOCKFISH_BINPACK_ZSTD) continue; // Shared prepared index.
            Path file = seek.resolve(source.identity() + ".json");
            if (Files.isSymbolicLink(seek) || Files.isSymbolicLink(file))
                throw new IOException("Linked continuation seek metadata refused");
            var first = new SourceReaders.Point(0, 0, source.format() == DataSource.Format.SEED_LEGACY ? -1 : 0);
            try (var navigation = new SourceReaders.Navigation(source, seek, first)) {
                navigation.floor(cursors.getOrDefault(source.identity(), 0L));
            }
        }
    }

    public static List<String> inherit(DataSources selection, Map<String, Long> cursors,
                                       Path sourceSeek, Path destinationSeek) throws IOException {
        verify(selection, cursors, sourceSeek);
        if (Files.isSymbolicLink(destinationSeek)) throw new IOException("Linked continuation seek directory refused");
        // Validate every existing destination before adding anything. A differing valid index
        // can include later visits and must never be replaced by the parent's older index.
        for (var source : selection.sources()) {
            Path destination = destinationSeek.resolve(source.identity() + ".json");
            if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS))
                verify(new DataSources(1, List.of(source), false), cursors, destinationSeek);
        }
        var copied = new ArrayList<String>();
        for (var source : selection.sources()) {
            if (source.format() == DataSource.Format.STOCKFISH_BINPACK_ZSTD) continue;
            String name = source.identity() + ".json";
            Path from = sourceSeek.resolve(name), to = destinationSeek.resolve(name);
            if (Files.exists(from) && Files.notExists(to)) {
                Files.createDirectories(destinationSeek);
                Path pending=Files.createTempFile(destinationSeek,"inherit-",".pending");
                try {
                    Files.copy(from,pending,StandardCopyOption.REPLACE_EXISTING);
                    if (Files.mismatch(from,pending)!=-1)throw new IOException("Continuation source index changed during copy");
                    try(var channel=java.nio.channels.FileChannel.open(pending,StandardOpenOption.WRITE)){channel.force(true);}
                    // Same-directory publication, no replacement of an existing destination.
                    Files.move(pending,to);
                } finally {Files.deleteIfExists(pending);}
                copied.add(name);
            }
        }
        verify(selection, cursors, destinationSeek);
        return List.copyOf(copied);
    }
    private ContinuationSeek() {}
}
