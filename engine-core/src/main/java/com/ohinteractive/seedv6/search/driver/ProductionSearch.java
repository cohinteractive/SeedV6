package com.ohinteractive.seedv6.search.driver;

import java.util.Objects;
import com.ohinteractive.seedv6.search.common.SingleDepthSearch;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.ParallelSearch;
import com.ohinteractive.seedv6.search.exact.IterationReuse;
import com.ohinteractive.seedv6.search.exact.ConcurrencyPolicy;
import com.ohinteractive.seedv6.search.exact.SeePolicy;
import com.ohinteractive.seedv6.search.tt.TTable;

/** Canonical search construction: the total worker limit includes the calling owner. */
public final class ProductionSearch {
    public static SingleDepthSearch create(int workers, SearchEvaluation evaluation) {
        return create(workers, evaluation, 64);
    }

    /** Same production policies with an explicitly bounded application Hash setting. */
    public static SingleDepthSearch create(int workers, SearchEvaluation evaluation, int hashMiB) {
        validate(workers, evaluation);
        if (hashMiB < 1 || hashMiB > 1024) throw new IllegalArgumentException("Hash must be 1..1024 MiB");
        var table = new TTable(hashMiB);
        if (evaluation.isBrnPair2())
            return workers == 1
                    ? ExactSearchAdapter.seeResearch(evaluation, SeePolicy.QXRAY, null, table)
                    : new ParallelSearch(workers, evaluation, table, ConcurrencyPolicy.ROOT_REUSE)
                            .withSeeResearch(SeePolicy.QXRAY, null);
        return workers == 1 ? new ExactSearchAdapter(evaluation, table)
                : new ParallelSearch(workers, evaluation, table, ConcurrencyPolicy.OFF);
    }

    /** Accepted pre-SR-024 cumulative control: native EFFORT or ROOT_REUSE, SEE off. */
    public static SingleDepthSearch createSeeControl(int workers, SearchEvaluation evaluation) {
        validate(workers, evaluation);
        if (evaluation.isBrnPair2())
            return workers == 1
                    ? ExactSearchAdapter.iterationReuseResearch(evaluation, IterationReuse.EFFORT, null)
                    : new ParallelSearch(workers, evaluation, ConcurrencyPolicy.ROOT_REUSE);
        return createStaticControl(workers, evaluation);
    }

    /** Accepted pre-SR-005 control: unpruned Pair-2 leaves, quiet LMR and WINDOW LMP. */
    public static SingleDepthSearch createIterationReuseControl(int workers, SearchEvaluation evaluation) {
        validate(workers, evaluation);
        if (workers == 1 && evaluation.isBrnPair2())
            return ExactSearchAdapter.quietMovePruning(evaluation);
        return createStaticControl(workers, evaluation);
    }

    /** Accepted SR-029 control: unpruned Pair-2 leaves and quiet LMR, with LMP off. */
    public static SingleDepthSearch createQuietLmrControl(int workers, SearchEvaluation evaluation) {
        validate(workers, evaluation);
        if (workers == 1 && evaluation.isBrnPair2())
            return ExactSearchAdapter.quietLateMoveReductions(evaluation);
        return createStaticControl(workers, evaluation);
    }

    /** Accepted SR-001 control: unpruned Pair-2 leaves without LMR; other policies unchanged. */
    public static SingleDepthSearch createUnprunedControl(int workers, SearchEvaluation evaluation) {
        validate(workers, evaluation);
        if (workers == 1 && evaluation.isBrnPair2())
            return ExactSearchAdapter.unprunedQuiescence(evaluation);
        return createStaticControl(workers, evaluation);
    }

    /** Preserved cumulative static-leaf control, including calibrated HCE pruning. */
    public static SingleDepthSearch createStaticControl(int workers, SearchEvaluation evaluation) {
        validate(workers, evaluation);
        return workers == 1 ? new ExactSearchAdapter(evaluation) : new ParallelSearch(workers, evaluation);
    }

    private static void validate(int workers, SearchEvaluation evaluation) {
        if (workers < ParallelSearch.MIN_WORKERS || workers > ParallelSearch.MAX_WORKERS)
            throw new IllegalArgumentException("Invalid search worker setting: " + workers);
        Objects.requireNonNull(evaluation, "evaluation");
    }

    private ProductionSearch() {}
}
