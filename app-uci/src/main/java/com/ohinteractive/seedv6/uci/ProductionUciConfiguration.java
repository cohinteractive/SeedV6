package com.ohinteractive.seedv6.uci;

import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.driver.ProductionSearch;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import java.io.IOException;
import java.nio.file.Path;

/** Standalone product profile: fixed verified Pair-2, canonical production search, no book/tablebase. */
final class ProductionUciConfiguration implements UciConfiguration {
    static ProductionUciConfiguration load(Path model) throws IOException {
        return new ProductionUciConfiguration(SearchEvaluation.brnPair2(ProductionModel.load(model)));
    }
    private ProductionUciConfiguration(SearchEvaluation evaluation) { this.evaluation = evaluation; }
    @Override public SearchLifecycleService service(int workers, int hashMiB) {
        return new SearchLifecycleService(TimeSource.SYSTEM, () -> ProductionSearch.create(workers, evaluation, hashMiB));
    }
    @Override public String name() { return "SeedV6 UCI " + version.displayVersion(); }
    @Override public boolean standalone() { return true; }
    @Override public String identity(int workers) {
        return "evaluator=BRN_PAIR2 checkpoint=" + ProductionModel.CHECKPOINT
                + " lineage=" + ProductionModel.LINEAGE + " sha256=" + ProductionModel.SHA256
                + " mapping=" + BrnPair2Model.CALIBRATION_ID + " engine=" + ProductionModel.ENGINE_SOURCE
                + " mode=production leaves=see_selective see=qxray lmr=quiet lmp=window"
                + " reuse=effort concurrency=" + (workers == 1 ? "off" : "root_reuse")
                + " workers=" + workers + " futility=off build=" + version.build() + " source=" + version.sourceRevision();
    }
    private final SearchEvaluation evaluation;
    private final ApplicationVersion version = ApplicationVersion.load();
}
