package com.ohinteractive.seedv6.uci;

import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.book.Book;
import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.driver.ProductionSearch;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.search.tablebase.RootTablebase;
import java.io.IOException;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;
import java.nio.file.Path;

/** Standalone product profile: fixed verified Pair-2, optional bundled book, no tablebase. */
final class ProductionUciConfiguration implements UciConfiguration {
    static ProductionUciConfiguration load(Path model) throws IOException {
        return new ProductionUciConfiguration(SearchEvaluation.brnPair2(ProductionModel.load(model)), ClockPolicy.configured());
    }
    private ProductionUciConfiguration(SearchEvaluation evaluation, ClockPolicy clocks) {
        this.evaluation = evaluation; this.clocks = clocks;
    }
    @Override public ClockPolicy clockPolicy() { return clocks; }
    @Override public UciConfiguration withClockPolicy(ClockPolicy policy) {
        return new ProductionUciConfiguration(evaluation, policy);
    }
    @Override public SearchLifecycleService service(int workers, int hashMiB) {
        return new SearchLifecycleService(TimeSource.SYSTEM, () -> clocks.apply(ProductionSearch.create(workers, evaluation, hashMiB)),
                RootTablebase.NONE, OpeningBook.INSTANCE);
    }
    @Override public String name() { return "SeedV6 UCI " + version.displayVersion(); }
    @Override public boolean standalone() { return true; }
    @Override public String identity(int workers) {
        return "evaluator=BRN_PAIR2 checkpoint=" + ProductionModel.CHECKPOINT
                + " lineage=" + ProductionModel.LINEAGE + " sha256=" + ProductionModel.SHA256
                + " mapping=" + BrnPair2Model.CALIBRATION_ID + " engine=" + ProductionModel.ENGINE_SOURCE
                + " mode=production leaves=see_selective see=qxray lmr=quiet lmp=window"
                + " reuse=effort concurrency=" + (workers == 1 ? "off" : "root_reuse")
                + " hints=completed_same_key history=" + (workers == 1 ? "invocation" : "root_seeded_private")
                + " learning=all_searched"
                + " clockPolicy=" + clocks.identity() + " workers=" + workers + " futility=off build=" + version.build() + " source=" + version.sourceRevision();
    }
    private final ClockPolicy clocks;
    private final SearchEvaluation evaluation;
    private final ApplicationVersion version = ApplicationVersion.load();

    // Load once independently of GUI state or client cwd, including after Hash/Threads changes.
    private static final class OpeningBook {
        static final Book INSTANCE = load();
        private static Book load() {
            try { return Book.bundled(); }
            catch (ExceptionInInitializerError unavailable) {
                // Book's lazy holder wraps missing/invalid resource errors. Other initialization
                // failures remain fatal; an optional repertoire must not hide engine defects.
                if (!(unavailable.getCause() instanceof IllegalStateException)) throw unavailable;
                System.err.println("SeedV6 UCI opening book unavailable; using search: "
                        + unavailable.getCause().getMessage());
                return Book.EMPTY;
            }
        }
    }
}
