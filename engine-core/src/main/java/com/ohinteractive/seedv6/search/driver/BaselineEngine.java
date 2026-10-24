package com.ohinteractive.seedv6.search.driver;

import java.io.IOException;
import java.util.Objects;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.book.Book;
import com.ohinteractive.seedv6.core.brnpair2.ProductionModel;
import com.ohinteractive.seedv6.search.common.SingleDepthSearch;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.ConstructionSearch;
import com.ohinteractive.seedv6.search.exact.FlatSearchConfig;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.search.tablebase.RootTablebase;

/** Authoritative competitive engine composition. Mutable search state is per owner/worker.
 * Historical ProductionSearch, PinnedSearch and ConventionalNnueEngine retain their identities. */
public final class BaselineEngine {
    // Frozen S6CNU002 CP64 payload; unrelated conventional checkpoints retain original V1.
    private static final String CP64_PAYLOAD_SHA256 = "5da0253642ab33b8e446b5b1c5764750be008cd3dbacc7cd1992a95bf4a881f6";
    public enum Evaluator { HCE, BREP2_32, CONVENTIONAL_CP64 }
    private final ClockPolicy clockPolicy;
    private final Evaluator evaluator;
    private final SearchEvaluation evaluation;
    private final ConventionalNnueEngine conventional;

    private BaselineEngine(Evaluator evaluator, SearchEvaluation evaluation, ConventionalNnueEngine conventional, ClockPolicy clockPolicy) {
        this.clockPolicy = Objects.requireNonNull(clockPolicy);
        this.evaluator = evaluator;
        this.evaluation = evaluation;
        this.conventional = conventional;
    }
    public static BaselineEngine load() throws IOException { return load(Evaluator.CONVENTIONAL_CP64); }
    public static BaselineEngine load(Evaluator evaluator) throws IOException {
        return load(evaluator, ClockPolicy.configured());
    }
    public static BaselineEngine load(Evaluator evaluator, ClockPolicy clockPolicy) throws IOException {
        return switch (evaluator) {
            case HCE -> new BaselineEngine(evaluator, SearchEvaluation.handcrafted(), null, clockPolicy);
            case BREP2_32 -> new BaselineEngine(evaluator, SearchEvaluation.brnPair2(ProductionModel.bundled()), null, clockPolicy);
            case CONVENTIONAL_CP64 -> {
                var network = ConventionalNnueEngine.load();
                yield new BaselineEngine(evaluator, network.evaluation(), network, clockPolicy);
            }
        };
    }
    /** Selected frozen-CP64 default; evaluator-aware callers use the overload below. */
    public static ConstructionSearch configuration(int workers, int hashMiB) {
        return new ConstructionSearch(FlatSearchConfig.candidate()
                .with(FlatSearchConfig.Feature.NOMINAL_FUTILITY, true)
                .with(FlatSearchConfig.Feature.VERIFIED_NULL, true),
                workers, hashMiB, AspirationSearch.Policy.ADAPTIVE16);
    }
    /** Calibrated production selection; explicit ConstructionSearch controls remain independent. */
    public static ConstructionSearch configuration(SearchEvaluation evaluation, int workers, int hashMiB) {
        return evaluation.matchesConventionalPayload(CP64_PAYLOAD_SHA256)
                ? configuration(workers, hashMiB)
                : ConstructionSearch.v1(workers, hashMiB, AspirationSearch.Policy.ADAPTIVE16);
    }
    public static SingleDepthSearch create(SearchEvaluation evaluation, int workers, int hashMiB) {
        return create(evaluation, workers, hashMiB, ClockPolicy.configured());
    }
    public static SingleDepthSearch create(SearchEvaluation evaluation, int workers, int hashMiB, ClockPolicy clocks) {
        return Objects.requireNonNull(clocks).apply(configuration(evaluation, workers, hashMiB).create(evaluation));
    }
    public SingleDepthSearch create(int workers, int hashMiB) { return create(evaluation, workers, hashMiB, clockPolicy); }
    public SearchLifecycleService service(int workers, int hashMiB) {
        return new SearchLifecycleService(TimeSource.SYSTEM, () -> create(workers, hashMiB),
                RootTablebase.NONE, Book.bundled());
    }
    public ClockPolicy clockPolicy() { return clockPolicy; }
    public long clockBudgetMillis(long remaining, long increment, int movesToGo) {
        return clockPolicy.allocateClockMillis(remaining, increment, movesToGo);
    }
    public BaselineEngine withClockPolicy(ClockPolicy clocks) {
        return new BaselineEngine(evaluator, evaluation, conventional, clocks);
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
        var configuration = configuration(evaluation, workers, hashMiB);
        return "baseline=construction-v1-" + (evaluator == Evaluator.CONVENTIONAL_CP64 ? "cp64" : evaluator.name())
                + " search=construction-v1 flags=" + configuration.mechanisms().flags()
                + " configuration=" + configuration.identity() + " " + model
                + " clockPolicy=" + clockPolicy.identity()
                + " score=side-to-move-cp build=" + version.build() + " source=" + version.sourceRevision();
    }
}
