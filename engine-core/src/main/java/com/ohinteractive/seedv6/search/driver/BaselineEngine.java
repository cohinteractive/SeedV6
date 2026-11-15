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
    public enum Evaluator {
        HCE, BREP2_32, CONVENTIONAL_CP64, CONVENTIONAL_WDL25, CONVENTIONAL_DIRECT128;
        public boolean conventional() { return this == CONVENTIONAL_CP64 || this == CONVENTIONAL_WDL25 || this == CONVENTIONAL_DIRECT128; }
    }
    /** Adopted conventional production policy; explicit research controls bypass payload selection. */
    public enum SearchProfile {
        TC_P1, HL_G1, LEGACY, MW_C4, MW_C4_W4, CLV_C1;
        public static final SearchProfile PRODUCTION_DEFAULT = CLV_C1;
        public static final String PROPERTY = "seedv6.searchProfile";
        public static SearchProfile configured() {
            try { return valueOf(System.getProperty(PROPERTY, PRODUCTION_DEFAULT.name()).trim().toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException invalid) {
                throw new IllegalArgumentException("seedv6.searchProfile must be CLV_C1, MW_C4, TC_P1, HL_G1, LEGACY or historical MW_C4_W4", invalid);
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
    public static BaselineEngine load() throws IOException { return load(Evaluator.CONVENTIONAL_DIRECT128); }
    public static BaselineEngine load(Evaluator evaluator) throws IOException {
        return load(evaluator, ClockPolicy.configured());
    }
    public static BaselineEngine load(Evaluator evaluator, ClockPolicy clockPolicy) throws IOException {
        return load(evaluator, clockPolicy, evaluator.conventional()
                ? SearchProfile.configured() : SearchProfile.LEGACY);
    }
    /** Explicit API selection precedes the JVM property and is fixed for this binding's lifetime. */
    public static BaselineEngine load(Evaluator evaluator, ClockPolicy clockPolicy, SearchProfile profile) throws IOException {
        return switch (evaluator) {
            case HCE -> new BaselineEngine(evaluator, SearchEvaluation.handcrafted(), null, clockPolicy, profile);
            case BREP2_32 -> new BaselineEngine(evaluator, SearchEvaluation.brnPair2(ProductionModel.bundled()), null, clockPolicy, profile);
            case CONVENTIONAL_CP64, CONVENTIONAL_WDL25, CONVENTIONAL_DIRECT128 -> {
                var network = ConventionalNnueEngine.loadBundled(evaluator == Evaluator.CONVENTIONAL_CP64
                        ? ConventionalNnueEngine.Model.CP64 : evaluator == Evaluator.CONVENTIONAL_WDL25
                        ? ConventionalNnueEngine.Model.WDL25 : ConventionalNnueEngine.Model.DIRECT128);
                yield new BaselineEngine(evaluator, network.evaluation(), network, clockPolicy, profile);
            }
        };
    }
    /** Selected production search; evaluator-aware callers use the overload below. */
    public static ConstructionSearch configuration(int workers, int hashMiB) {
        return configuration(workers, hashMiB, SearchProfile.configured());
    }
    public static ConstructionSearch configuration(int workers, int hashMiB, SearchProfile profile) {
        Objects.requireNonNull(profile);
        var legacy = legacyConfiguration(workers, hashMiB);
        // One adopted feature set at every worker count. W1 owns its search directly;
        // multiple workers cooperate eagerly. MW_C4_W4 preserves the build96 selector
        // solely as an explicit historical control. MW_C4 retains the prior unified
        // formulation; CLV_C1 adds frozen C1 relief without changing worker scheduling.
        boolean memory = profile == SearchProfile.CLV_C1 || profile == SearchProfile.MW_C4
                || profile == SearchProfile.MW_C4_W4 && workers == 4;
        return new ConstructionSearch(legacy.mechanisms()
                .with(FlatSearchConfig.Feature.REQUEST_LEARNING, profile != SearchProfile.LEGACY)
                .with(FlatSearchConfig.Feature.HISTORY_EFFORT, profile != SearchProfile.LEGACY)
                // Exact TC-P1/build89 formulation; HL_G1 and LEGACY retain their prior meanings.
                .with(FlatSearchConfig.Feature.TACTICAL_VERIFICATION,
                        profile == SearchProfile.TC_P1 || profile == SearchProfile.CLV_C1 || profile == SearchProfile.MW_C4
                                || profile == SearchProfile.MW_C4_W4)
                .with(FlatSearchConfig.Feature.HEURISTIC_TT_MEMORY, memory)
                .with(FlatSearchConfig.Feature.TT_CRITICAL_RELIEF, profile == SearchProfile.CLV_C1),
                workers, hashMiB, legacy.aspiration(), memory && workers > 1
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
        return adoptedConventional(evaluation)
                ? configuration(workers, hashMiB)
                : ConstructionSearch.v1(workers, hashMiB, AspirationSearch.Policy.ADAPTIVE16);
    }
    public static ConstructionSearch legacyConfiguration(SearchEvaluation evaluation, int workers, int hashMiB) {
        return adoptedConventional(evaluation)
                ? legacyConfiguration(workers, hashMiB)
                : ConstructionSearch.v1(workers, hashMiB, AspirationSearch.Policy.ADAPTIVE16);
    }
    private ConstructionSearch selectedConfiguration(int workers, int hashMiB) {
        return evaluator.conventional()
                ? configuration(workers, hashMiB, searchProfile)
                : ConstructionSearch.v1(workers, hashMiB, AspirationSearch.Policy.ADAPTIVE16);
    }
    public static SingleDepthSearch create(SearchEvaluation evaluation, int workers, int hashMiB) {
        return create(evaluation, workers, hashMiB, ClockPolicy.configured());
    }
    public static SingleDepthSearch create(SearchEvaluation evaluation, int workers, int hashMiB, ClockPolicy clocks) {
        return Objects.requireNonNull(clocks).apply(configuration(evaluation, workers, hashMiB).create(evaluation));
    }
    /** Explicit research policy never falls back because a candidate has an unknown payload. */
    public static SingleDepthSearch create(SearchEvaluation evaluation, int workers, int hashMiB,
                                          ClockPolicy clocks, SearchProfile profile) {
        return Objects.requireNonNull(clocks).apply(configuration(workers, hashMiB, profile).create(evaluation));
    }
    private static boolean adoptedConventional(SearchEvaluation evaluation) {
        return evaluation.matchesConventionalPayload(ConventionalNnueEngine.Model.CP64.payloadSha256())
                || evaluation.matchesConventionalPayload(ConventionalNnueEngine.Model.WDL25.payloadSha256())
                || evaluation.matchesConventionalPayload(ConventionalNnueEngine.Model.DIRECT128.payloadSha256());
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
            case CONVENTIONAL_CP64, CONVENTIONAL_WDL25, CONVENTIONAL_DIRECT128 -> "evaluator=CONVENTIONAL_NNUE model=" + conventional.selection()
                    + " checkpoint=64 schema=" + conventional.model().schema()
                    + " hidden=" + conventional.model().hidden() + " heads=" + conventional.model().outputBuckets() + " networkSha256=" + conventional.selection().sha256()
                    + " payload=" + conventional.model().payloadSha256() + " head=" + conventional.model().headBackend()
                    + " scoreMapping=" + ConventionalNnueEngine.SCORE_MAPPING;
        };
    }
    public EngineFormulation formulation(int workers, int hashMiB) {
        String label = switch (evaluator) {
            case HCE -> "HCE (handcrafted)";
            case BREP2_32 -> "BREP2-32 · checkpoint " + ProductionModel.CHECKPOINT
                    + " · SHA-256 " + ProductionModel.SHA256.substring(0, 12);
            case CONVENTIONAL_CP64, CONVENTIONAL_WDL25, CONVENTIONAL_DIRECT128 -> conventional.selection().description() + " checkpoint 64" + " · " + conventional.model().schema()
                    + " · " + conventional.model().outputBuckets() + " heads · SHA-256 "
                    + conventional.selection().sha256().substring(0, 12);
        };
        return new EngineFormulation(selectedConfiguration(workers, hashMiB), label, evaluatorIdentity(), clockPolicy);
    }
    public static String productionFormulation() {
        var owner = configuration(1, 64, SearchProfile.PRODUCTION_DEFAULT);
        var parallel = configuration(16, 64, SearchProfile.PRODUCTION_DEFAULT);
        return "W1-W16: " + EngineFormulation.searchName(owner)
                + "\nProfile " + SearchProfile.PRODUCTION_DEFAULT + "; flags" + owner.mechanisms().flags()
                + "; W1 " + owner.parallelIdentity() + " / W2-W16 " + parallel.parallelIdentity();
    }
    public String identity(int workers, int hashMiB) {
        var version = ApplicationVersion.load();
        var configuration = selectedConfiguration(workers, hashMiB);
        return "baseline=construction-v1-" + (evaluator.conventional() ? conventional.selection().name().toLowerCase(Locale.ROOT) : evaluator.name())
                + " searchProfile=" + (evaluator.conventional() ? searchProfile : "CONSTRUCTION_V1")
                + " search=construction-v1 flags=" + configuration.mechanisms().flags()
                + " configuration=" + configuration.identity() + " " + evaluatorIdentity()
                + " clockPolicy=" + clockPolicy.identity()
                + " score=side-to-move-cp build=" + version.build() + " source=" + version.sourceRevision();
    }
}
