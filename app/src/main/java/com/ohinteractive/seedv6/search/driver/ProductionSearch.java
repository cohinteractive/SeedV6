package com.ohinteractive.seedv6.search.driver;

import java.util.Objects;
import com.ohinteractive.seedv6.search.common.SingleDepthSearch;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.ParallelSearch;
import com.ohinteractive.seedv6.search.exact.IterationReuse;

/** Canonical search construction: the total worker limit includes the calling owner. */
public final class ProductionSearch {
    public static SingleDepthSearch create(int workers, SearchEvaluation evaluation) {
        validate(workers, evaluation);
        if (workers == 1 && evaluation.isBrnPair2())
            return ExactSearchAdapter.iterationReuseResearch(evaluation, IterationReuse.EFFORT, null);
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
