package com.ohinteractive.seedv6.corpus;

import java.util.List;

/** Read from the transactional catalog. manifest.json is a refreshed human-readable snapshot. */
public record CorpusManifest(int schemaVersion, int recordBytes, long positions, long storedRecords,
                             long completedShards, List<Source> sources) {
    public record Source(int id, String adapter, String identifier, String metadata, String policy,
                         long recordsRead, long accepted, long rejected, long duplicates,
                         long added, long upgraded, long passes) {}
    public CorpusManifest { sources = List.copyOf(sources); }
}
