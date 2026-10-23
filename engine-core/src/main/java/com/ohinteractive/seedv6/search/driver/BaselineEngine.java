package com.ohinteractive.seedv6.search.driver;

import java.io.IOException;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.book.Book;
import com.ohinteractive.seedv6.core.brnpair2.ProductionModel;
import com.ohinteractive.seedv6.search.common.SingleDepthSearch;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.ConstructionSearch;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.search.tablebase.RootTablebase;

/** Authoritative competitive engine composition. Mutable search state is per owner/worker.
 * Historical ProductionSearch, PinnedSearch and ConventionalNnueEngine retain their identities. */
public final class BaselineEngine {
    public enum Evaluator { HCE, BREP2_32, CONVENTIONAL_CP64 }
    private final Evaluator evaluator;
    private final SearchEvaluation evaluation;
    private final ConventionalNnueEngine conventional;

    private BaselineEngine(Evaluator evaluator, SearchEvaluation evaluation, ConventionalNnueEngine conventional) {
        this.evaluator = evaluator;
        this.evaluation = evaluation;
        this.conventional = conventional;
    }
    public static BaselineEngine load() throws IOException { return load(Evaluator.CONVENTIONAL_CP64); }
    public static BaselineEngine load(Evaluator evaluator) throws IOException {
        return switch (evaluator) {
            case HCE -> new BaselineEngine(evaluator, SearchEvaluation.handcrafted(), null);
            case BREP2_32 -> new BaselineEngine(evaluator, SearchEvaluation.brnPair2(ProductionModel.bundled()), null);
            case CONVENTIONAL_CP64 -> {
                var network = ConventionalNnueEngine.load();
                yield new BaselineEngine(evaluator, network.evaluation(), network);
            }
        };
    }
    public static ConstructionSearch configuration(int workers, int hashMiB) {
        return ConstructionSearch.v1(workers, hashMiB, AspirationSearch.Policy.ADAPTIVE16);
    }
    public static SingleDepthSearch create(SearchEvaluation evaluation, int workers, int hashMiB) {
        return configuration(workers, hashMiB).create(evaluation);
    }
    public SingleDepthSearch create(int workers, int hashMiB) { return create(evaluation, workers, hashMiB); }
    public SearchLifecycleService service(int workers, int hashMiB) {
        return new SearchLifecycleService(TimeSource.SYSTEM, () -> create(workers, hashMiB),
                RootTablebase.NONE, Book.bundled());
    }
    public SearchEvaluation evaluation() { return evaluation; }
    public ConventionalNnueEngine conventional() { return conventional; }
    public Evaluator evaluator() { return evaluator; }
    public String identity(int workers, int hashMiB) {
        var version = ApplicationVersion.load();
        String model = switch (evaluator) {
            case HCE -> "evaluator=HCE";
            case BREP2_32 -> "evaluator=BREP2_32 checkpoint=" + ProductionModel.CHECKPOINT
                    + " networkSha256=" + ProductionModel.SHA256 + " mapping=BRN_PAIR2_MATERIAL_RESIDUAL_QUARTER_V1";
            case CONVENTIONAL_CP64 -> "evaluator=CONVENTIONAL_NNUE checkpoint=64 schema=" + conventional.model().schema()
                    + " heads=" + conventional.model().outputBuckets() + " networkSha256=" + ConventionalNnueEngine.SHA256
                    + " payload=" + conventional.model().payloadSha256() + " head=" + conventional.model().headBackend()
                    + " scoreMapping=" + ConventionalNnueEngine.SCORE_MAPPING;
        };
        return "baseline=construction-v1-" + (evaluator == Evaluator.CONVENTIONAL_CP64 ? "cp64" : evaluator.name())
                + " search=construction-v1 flags=" + configuration(workers, hashMiB).mechanisms().flags()
                + " configuration=" + configuration(workers, hashMiB).identity() + " " + model
                + " score=side-to-move-cp build=" + version.build() + " source=" + version.sourceRevision();
    }
}
