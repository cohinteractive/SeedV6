package com.ohinteractive.seedv6.uci;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.driver.ProductionSearch;
import com.ohinteractive.seedv6.search.driver.SearchDriver;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;

/** Legacy checkpoint/HCE product composition. Experimental selection belongs to research. */
public record LegacyUciEvaluation(SearchEvaluation evaluation, String identity) implements UciConfiguration {
    private static final String PREFIX = "seedv6.uci.";

    public static LegacyUciEvaluation load(Properties properties) {
        for (String key : properties.stringPropertyNames())
            if (key.startsWith(PREFIX) && key.endsWith("Research"))
                throw new IllegalArgumentException("Research policy is not a production option: " + key
                        + "; use the explicit research UCI launcher");
        String store = properties.getProperty(PREFIX + "pair2Store", "");
        String checkpoint = properties.getProperty(PREFIX + "checkpoint", "");
        String hash = properties.getProperty(PREFIX + "networkSha256", "");
        String warmup = properties.getProperty(PREFIX + "warmupDepth", "0");
        int workers = Integer.parseInt(properties.getProperty(PREFIX + "warmupWorkers", "1"));
        if (workers < 1 || workers > 16) throw new IllegalArgumentException("UCI warmupWorkers must be 1..16");
        if (store.isBlank()) {
            if (!checkpoint.isEmpty() || !hash.isEmpty() || !warmup.equals("0") || workers != 1)
                throw new IllegalArgumentException("Explicit UCI evaluator options require pair2Store");
            return new LegacyUciEvaluation(SearchEvaluation.handcrafted(), "");
        }
        if (!hash.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("Pair-2 UCI requires the expected networkSha256 (lowercase hex)");
        int depth = Integer.parseInt(warmup);
        if (depth < 0 || depth > 4) throw new IllegalArgumentException("UCI warmupDepth must be 0..4");
        try {
            var selected = checkpoint.isEmpty() ? CheckpointStore.readBestSnapshot(Path.of(store))
                    : CheckpointStore.readSnapshot(Path.of(store), checkpoint);
            if (selected.manifest().architecture() != TrainingArchitecture.BRN_PAIR2)
                throw new IllegalArgumentException("UCI pair2Store is not BRE-Pair 2");
            if (!hash.equals(selected.manifest().networkSha256()))
                throw new IllegalArgumentException("UCI networkSha256 mismatch");
            var definition = selected.model().evaluation(NnueScoreMapping.V1);
            if (depth > 0) try (var driver = new SearchDriver(ProductionSearch.create(workers, definition))) {
                var board = Board.startingPosition();
                for (int i = 0; i < 3; i++) {
                    driver.newGame();
                    if (!driver.search(new SearchRequest(board, GameHistory.initial(board), depth)).targetDepthCompleted())
                        throw new IllegalStateException("Incomplete UCI warm-up");
                }
            }
            var version = ApplicationVersion.load();
            return new LegacyUciEvaluation(definition, "evaluator=BRN_PAIR2 checkpoint=" + selected.manifest().id()
                    + " sha256=" + hash + " mapping=" + BrnPair2Model.CALIBRATION_ID
                    + " warmup=" + (depth == 0 ? "off" : "3x" + depth)
                    + " ttMiB=64 nmp=off reverseFutility=off"
                    + " build=" + version.build() + " source=" + version.sourceRevision());
        } catch (IOException invalid) {
            throw new IllegalArgumentException("Cannot load requested BRE-Pair 2 checkpoint", invalid);
        }
    }

    @Override public SearchLifecycleService service(int workers, int hashMiB) {
        if (hashMiB != 64) throw new IllegalArgumentException("Legacy profile uses Hash=64");
        return new SearchLifecycleService(workers, evaluation);
    }

    @Override public String identity(int workers) {
        if (identity.isEmpty()) return identity;
        return identity + " internal=off reuse=effort check=off lmp=window aspiration=off lmr=quiet workers=" + workers
                + " leaves=see_selective mode=production qTT=off nominalQProof=off nominalLmrProof=off nominalLmpProof=off"
                + " hints=completed_same_key history=" + (workers == 1 ? "invocation" : "root_seeded_private")
                + " learning=all_searched"
                + " see=qxray concurrency=" + (workers > 1 ? "root_reuse rootHistory=seeded_private" : "off");
    }
}
