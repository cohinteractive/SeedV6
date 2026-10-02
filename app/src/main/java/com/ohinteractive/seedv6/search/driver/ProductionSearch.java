package com.ohinteractive.seedv6.search.driver;

import java.util.Objects;
import com.ohinteractive.seedv6.search.common.SingleDepthSearch;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.ParallelSearch;

/** Canonical search construction: the total worker limit includes the calling owner. */
public final class ProductionSearch {
    public static SingleDepthSearch create(int workers, SearchEvaluation evaluation) {
        if (workers < ParallelSearch.MIN_WORKERS || workers > ParallelSearch.MAX_WORKERS)
            throw new IllegalArgumentException("Invalid search worker setting: " + workers);
        Objects.requireNonNull(evaluation, "evaluation");
        return workers == 1 ? new ExactSearchAdapter(evaluation) : new ParallelSearch(workers, evaluation);
    }

    private ProductionSearch() {}
}
