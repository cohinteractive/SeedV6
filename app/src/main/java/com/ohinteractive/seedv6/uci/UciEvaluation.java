package com.ohinteractive.seedv6.uci;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.driver.ProductionSearch;
import com.ohinteractive.seedv6.search.driver.SearchDriver;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.model.TrainingArchitecture;

/** Explicit, immutable process configuration. No preferences, promotion, or fallback. */
record UciEvaluation(SearchEvaluation evaluation, String identity) {
    static final String PREFIX = "seedv6.uci.";

    static UciEvaluation load(Properties properties) {
        String store = properties.getProperty(PREFIX + "pair2Store", "");
        String checkpoint = properties.getProperty(PREFIX + "checkpoint", "");
        String hash = properties.getProperty(PREFIX + "networkSha256", "");
        String warmup = properties.getProperty(PREFIX + "warmupDepth", "0");
        if (store.isBlank()) {
            if (!checkpoint.isEmpty() || !hash.isEmpty() || !warmup.equals("0"))
                throw new IllegalArgumentException("Explicit UCI evaluator options require pair2Store");
            return new UciEvaluation(SearchEvaluation.handcrafted(), "");
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
            // Untimed, bounded JVM warm-up; private scratch/TT discarded before play.
            // Three complete depth-limited production requests, outside game clocks.
            if (depth > 0) try (var driver = new SearchDriver(ProductionSearch.create(1, definition))) {
                var board = Board.startingPosition();
                for (int i = 0; i < 3; i++) {
                    driver.newGame();
                    if (!driver.search(new SearchRequest(board, GameHistory.initial(board), depth)).targetDepthCompleted())
                        throw new IllegalStateException("Incomplete UCI warm-up");
                }
            }
            var version = com.ohinteractive.seedv6.ApplicationVersion.load();
            return new UciEvaluation(definition, "evaluator=BRN_PAIR2 checkpoint=" + selected.manifest().id()
                    + " sha256=" + hash + " mapping=" + BrnPair2Model.CALIBRATION_ID
                    + " warmup=" + (depth == 0 ? "off" : "3x" + depth)
                    + " ttMiB=64 leaves=static nmp=off reverseFutility=off"
                    + " build=" + version.build() + " source=" + version.sourceRevision());
        } catch (IOException invalid) {
            throw new IllegalArgumentException("Cannot load requested BRE-Pair 2 checkpoint", invalid);
        }
    }
}
