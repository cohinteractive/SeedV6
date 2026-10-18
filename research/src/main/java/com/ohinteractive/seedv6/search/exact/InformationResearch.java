package com.ohinteractive.seedv6.search.exact;

import com.ohinteractive.seedv6.search.common.SingleDepthSearch;
import com.ohinteractive.seedv6.search.driver.ExactSearchAdapter;
import com.ohinteractive.seedv6.search.driver.ProductionSearch;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;

/** Concrete Phase 3 configurations, never a production selection route.
 * Bits: 1 completed-iteration history, 2 nominal maluses, 4 completed hints. */
public final class InformationResearch {
    public static boolean accepts(int workers,int policy) {
        return policy>=0 && policy<=7 && workers>=1 && workers<=16 && (workers==1 || policy==0 || policy==4);
    }
    public static SingleDepthSearch create(SearchEvaluation evaluation,int workers,int hashMiB,int policy) {
        if(!evaluation.isBrnPair2() || !accepts(workers,policy) || hashMiB!=64)
            throw new IllegalArgumentException("Unsupported Phase 3 information configuration");
        if(policy==0) return ProductionSearch.createInformationControl(workers,evaluation,hashMiB);
        if(workers==1) return ExactSearchAdapter.informationResearch(evaluation,(policy&4)!=0,(policy&1)!=0,(policy&2)!=0,null);
        return new ParallelSearch(workers,evaluation,new TTable(hashMiB),ConcurrencyPolicy.ROOT_REUSE)
                .withSeeResearch(SeePolicy.QXRAY,null).withCompletedHints(true,null);
    }
    private InformationResearch() {}
}
