package com.ohinteractive.seedv6.search.driver;

import java.io.IOException;
import java.util.Objects;
import java.util.Locale;
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
    /** CP64 production policy only; explicit ConstructionSearch controls bypass this selector. */
    public enum SearchProfile {
        TC_P1, HL_G1, LEGACY, MW_C4_W4;
        public static final SearchProfile PRODUCTION_DEFAULT = MW_C4_W4;
        public static final String PROPERTY = "seedv6.searchProfile";
        public static SearchProfile configured() {
            try { return valueOf(System.getProperty(PROPERTY, PRODUCTION_DEFAULT.name()).trim().toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException invalid) {
                throw new IllegalArgumentException("seedv6.searchProfile must be MW_C4_W4, TC_P1, HL_G1 or LEGACY", invalid);
            }
        }
    }
    private final ClockPolicy clockPolicy;
    private final SearchProfile searchProfile;
    private final Evaluator evaluator;
    private final SearchEvaluation evaluation;
    private final ConventionalNnueEngine conventional;

    private BaselineEngine(Evaluator evaluator, SearchEvaluation evaluation, ConventionalNnueEngine conventional,
                           ClockPolicy clockPolicy, SearchProfile searchProfile) {
        this.clockPolicy = Objects.requireNonNull(clockPolicy);
        this.searchProfile = Objects.requireNonNull(searchProfile);
        this.evaluator = evaluator;
        this.evaluation = evaluation;
        this.conventional = conventional;
    }
    public static BaselineEngine load() throws IOException { return load(Evaluator.CONVENTIONAL_CP64); }
    public static BaselineEngine load(Evaluator evaluator) throws IOException {
        return load(evaluator, ClockPolicy.configured());
    }
    public static BaselineEngine load(Evaluator evaluator, ClockPolicy clockPolicy) throws IOException {
        return load(evaluator, clockPolicy, evaluator == Evaluator.CONVENTIONAL_CP64
                ? SearchProfile.configured() : SearchProfile.LEGACY);
    }
    /** Explicit API selection precedes the JVM property and is fixed for this binding's lifetime. */
    public static BaselineEngine load(Evaluator evaluator, ClockPolicy clockPolicy, SearchProfile profile) throws IOException {
        return switch (evaluator) {
            case HCE -> new BaselineEngine(evaluator, SearchEvaluation.handcrafted(), null, clockPolicy, profile);
            case BREP2_32 -> new BaselineEngine(evaluator, SearchEvaluation.brnPair2(ProductionModel.bundled()), null, clockPolicy, profile);
            case CONVENTIONAL_CP64 -> {
                var network = ConventionalNnueEngine.load();
                yield new BaselineEngine(evaluator, network.evaluation(), network, clockPolicy, profile);
            }
        };
    }
    /** Selected frozen-CP64 default; evaluator-aware callers use the overload below. */
    public static ConstructionSearch configuration(int workers, int hashMiB) {
        return configuration(workers, hashMiB, SearchProfile.configured());
    }
    public static ConstructionSearch configuration(int workers, int hashMiB, SearchProfile profile) {
        Objects.requireNonNull(profile);
        var legacy = legacyConfiguration(workers, hashMiB);
        // The adopted policy resolves afresh for each resource reconstruction. Only W4
        // selects confirmed C4; explicit TC_P1 remains the exact ROOT_REUSE rollback.
        boolean memory = profile == SearchProfile.MW_C4_W4 && workers == 4;
        return new ConstructionSearch(legacy.mechanisms()
                .with(FlatSearchConfig.Feature.REQUEST_LEARNING, profile != SearchProfile.LEGACY)
                .with(FlatSearchConfig.Feature.HISTORY_EFFORT, profile != SearchProfile.LEGACY)
                // Exact TC-P1/build89 formulation; HL_G1 and LEGACY retain their prior meanings.
                .with(FlatSearchConfig.Feature.TACTICAL_VERIFICATION,
                        profile == SearchProfile.TC_P1 || profile == SearchProfile.MW_C4_W4)
                .with(FlatSearchConfig.Feature.HEURISTIC_TT_MEMORY, memory),
                workers, hashMiB, legacy.aspiration(), memory
                        ? ConstructionSearch.RootScheduling.EAGER_ROOT : ConstructionSearch.RootScheduling.STANDARD);
    }
    /** Pre-HL production policy, independent of defaults/properties. Retains current correctness fixes. */
    public static ConstructionSearch legacyConfiguration(int workers, int hashMiB) {
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
    public static ConstructionSearch legacyConfiguration(SearchEvaluation evaluation, int workers, int hashMiB) {
        return evaluation.matchesConventionalPayload(CP64_PAYLOAD_SHA256)
                ? legacyConfiguration(workers, hashMiB)
                : ConstructionSearch.v1(workers, hashMiB, AspirationSearch.Policy.ADAPTIVE16);
    }
    private ConstructionSearch selectedConfiguration(int workers, int hashMiB) {
        return evaluation.matchesConventionalPayload(CP64_PAYLOAD_SHA256)
                ? configuration(workers, hashMiB, searchProfile)
                : ConstructionSearch.v1(workers, hashMiB, AspirationSearch.Policy.ADAPTIVE16);
    }
    public static SingleDepthSearch create(SearchEvaluation evaluation, int workers, int hashMiB) {
        return create(evaluation, workers, hashMiB, ClockPolicy.configured());
    }
    public static SingleDepthSearch create(SearchEvaluation evaluation, int workers, int hashMiB, ClockPolicy clocks) {
        return Objects.requireNonNull(clocks).apply(configuration(evaluation, workers, hashMiB).create(evaluation));
    }
    public SingleDepthSearch create(int workers, int hashMiB) {
        return clockPolicy.apply(selectedConfiguration(workers, hashMiB).create(evaluation));
    }
    public SearchLifecycleService service(int workers, int hashMiB) {
        return new SearchLifecycleService(TimeSource.SYSTEM, () -> create(workers, hashMiB),
                RootTablebase.NONE, Book.bundled());
    }
    public ClockPolicy clockPolicy() { return clockPolicy; }
    public long clockBudgetMillis(long remaining, long increment, int movesToGo) {
        return clockPolicy.allocateClockMillis(remaining, increment, movesToGo);
    }
    public BaselineEngine withClockPolicy(ClockPolicy clocks) {
        return new BaselineEngine(evaluator, evaluation, conventional, clocks, searchProfile);
    }
    public SearchEvaluation evaluation() { return evaluation; }
    public ConventionalNnueEngine conventional() { return conventional; }
    public Evaluator evaluator() { return evaluator; }
    private String evaluatorIdentity() {
        return switch (evaluator) {
            case HCE -> "evaluator=HCE";
            case BREP2_32 -> "evaluator=BREP2_32 checkpoint=" + ProductionModel.CHECKPOINT
                    + " networkSha256=" + ProductionModel.SHA256 + " mapping=BRN_PAIR2_MATERIAL_RESIDUAL_QUARTER_V1";
            case CONVENTIONAL_CP64 -> "evaluator=CONVENTIONAL_NNUE checkpoint=64 schema=" + conventional.model().schema()
                    + " heads=" + conventional.model().outputBuckets() + " networkSha256=" + ConventionalNnueEngine.SHA256
                    + " payload=" + conventional.model().payloadSha256() + " head=" + conventional.model().headBackend()
                    + " scoreMapping=" + ConventionalNnueEngine.SCORE_MAPPING;
        };
    }
    public EngineFormulation formulation(int workers, int hashMiB) {
        String label = switch (evaluator) {
            case HCE -> "HCE (handcrafted)";
            case BREP2_32 -> "BREP2-32 · checkpoint " + ProductionModel.CHECKPOINT
                    + " · SHA-256 " + ProductionModel.SHA256.substring(0, 12);
            case CONVENTIONAL_CP64 -> "Conventional NNUE CP64 · " + conventional.model().schema()
                    + " · " + conventional.model().outputBuckets() + " heads · SHA-256 "
                    + ConventionalNnueEngine.SHA256.substring(0, 12);
        };
        return new EngineFormulation(selectedConfiguration(workers, hashMiB), label, evaluatorIdentity(), clockPolicy);
    }
    public static String productionFormulation() {
        return "W4: " + EngineFormulation.searchName(configuration(4, 64, SearchProfile.PRODUCTION_DEFAULT))
                + "\nW1-W3 / W5-W16: " + EngineFormulation.searchName(configuration(1, 64, SearchProfile.PRODUCTION_DEFAULT));
    }
    public String identity(int workers, int hashMiB) {
        var version = ApplicationVersion.load();
        var configuration = selectedConfiguration(workers, hashMiB);
        return "baseline=construction-v1-" + (evaluator == Evaluator.CONVENTIONAL_CP64 ? "cp64" : evaluator.name())
                + " searchProfile=" + (evaluator == Evaluator.CONVENTIONAL_CP64 ? searchProfile : "CONSTRUCTION_V1")
                + " search=construction-v1 flags=" + configuration.mechanisms().flags()
                + " configuration=" + configuration.identity() + " " + evaluatorIdentity()
                + " clockPolicy=" + clockPolicy.identity()
                + " score=side-to-move-cp build=" + version.build() + " source=" + version.sourceRevision();
    }
}
