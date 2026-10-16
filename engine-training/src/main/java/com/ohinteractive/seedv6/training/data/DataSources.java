package com.ohinteractive.seedv6.training.data;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Lineage setup. Weighted largest-remainder allocation, ties in configured source order; no fallback. */
public record DataSources(int version, List<DataSource> sources, boolean legacyProgressAcknowledged) {
    public static final String DIRECTORY = "training-data";
    public DataSources {
        if (version != 1 || sources == null || sources.isEmpty()) throw new IllegalArgumentException("Add at least one Training Data source");
        sources = List.copyOf(sources);
        if (sources.stream().map(DataSource::identity).distinct().count() != sources.size())
            throw new IllegalArgumentException("The same source version cannot appear twice in a mix");
    }
    public static Path directory(Path lineage) { return lineage.resolve(DIRECTORY); }
    public static DataSources read(Path directory) throws IOException { return DataFiles.read(directory.resolve("sources.json"), DataSources.class); }
    public void save(Path directory) throws IOException { DataFiles.write(directory.resolve("sources.json"), this); }
    public void archiveSelection(Path directory) throws IOException {
        Path file = directory.resolve("selections").resolve(identity() + ".json");
        if (!Files.exists(file)) DataFiles.write(file, this);
        else if (!DataFiles.read(file, DataSources.class).identity().equals(identity())) throw new IOException("Saved Training Data selection changed");
    }
    public void verify() throws IOException { for (var source : sources) source.verify(); }
    public String identity() {
        return DataFiles.hash(sources.stream().map(s -> s.identity() + ":" + s.weight()).toList().toString() + ";sequential-v1;strict-exhaustion");
    }
    public int[] allocate(int positions) {
        if (positions < 0) throw new IllegalArgumentException("Negative count");
        long weight = sources.stream().mapToLong(DataSource::weight).sum();
        int[] result = new int[sources.size()]; long[] remainders = new long[result.length]; int assigned = 0;
        for (int i = 0; i < result.length; i++) {
            long scaled = (long) positions * sources.get(i).weight();
            result[i] = (int) (scaled / weight); remainders[i] = scaled % weight; assigned += result[i];
        }
        for (int extra = positions - assigned; extra > 0; extra--) {
            int best = 0;
            for (int i = 1; i < result.length; i++) if (remainders[i] > remainders[best]) best = i;
            result[best]++; remainders[best] = -1;
        }
        return result;
    }
}
