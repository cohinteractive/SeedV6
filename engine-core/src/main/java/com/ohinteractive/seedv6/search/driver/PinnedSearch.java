package com.ohinteractive.seedv6.search.driver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import com.ohinteractive.seedv6.core.brnpair2.ProductionModel;
import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model;
import com.ohinteractive.seedv6.search.common.SingleDepthSearch;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.ConstructionSearch;

/** Explicit gameplay bindings. Only verified model bytes can enter this composition.
 * Immutable model tables may be shared; every create call owns fresh Search/TT/worker state. */
public final class PinnedSearch {
    public enum Policy {
        POLICY4("Policy 4 / Pair-2 g32"),
        CONSTRUCTION_V1("construction-v1 / Pair-2 g32 (experimental)");
        private final String label;
        Policy(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    private final Policy policy;
    private final SearchEvaluation evaluation;
    private final AspirationSearch.Policy aspiration;

    private PinnedSearch(Policy policy, BrnPair2Model model, AspirationSearch.Policy aspiration) {
        this.policy = Objects.requireNonNull(policy);
        if (aspiration != null && (policy != Policy.CONSTRUCTION_V1 || aspiration != AspirationSearch.Policy.WIDEN16))
            throw new IllegalArgumentException("Pinned gameplay supports construction-v1 aspiration OFF or WIDEN16 only");
        this.evaluation = SearchEvaluation.brnPair2(model);
        this.aspiration = aspiration;
    }
    public static PinnedSearch load(Policy policy) throws IOException {
        return load(policy, null, null);
    }
    public static PinnedSearch load(Policy policy, Path model, AspirationSearch.Policy aspiration) throws IOException {
        return new PinnedSearch(policy, model == null ? ProductionModel.bundled() : ProductionModel.load(model), aspiration);
    }
    public Policy policy() { return policy; }
    public SearchEvaluation evaluation() { return evaluation; }
    public SingleDepthSearch create(int workers, int hashMiB) {
        // Validate the same supported resources for both explicitly pinned alternatives.
        var construction = ConstructionSearch.v1(workers, hashMiB, aspiration);
        return policy == Policy.CONSTRUCTION_V1 ? construction.create(evaluation)
                : ProductionSearch.create(workers, evaluation, hashMiB);
    }
    public String identity(int workers, int hashMiB) {
        String configuration = policy == Policy.CONSTRUCTION_V1
                ? "policy=construction-v1 experimental=true flags=" + ConstructionSearch.v1(workers, hashMiB, aspiration).mechanisms().flags()
                    + " configuration=" + ConstructionSearch.v1(workers, hashMiB, aspiration).identity()
                : "policy=production-policy4 experimental=false recursive-pvs workers=" + workers + " hashMiB=" + hashMiB;
        return configuration + " model.sha256=" + ProductionModel.SHA256 + " model.checkpoint=" + ProductionModel.CHECKPOINT
                + " model.lineage=" + ProductionModel.LINEAGE + " mapping=" + BrnPair2Model.CALIBRATION_ID;
    }
}
