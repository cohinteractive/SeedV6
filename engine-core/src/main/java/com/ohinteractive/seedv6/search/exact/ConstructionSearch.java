package com.ohinteractive.seedv6.search.exact;

import java.util.Objects;
import com.ohinteractive.seedv6.search.common.SingleDepthSearch;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;

/** Shared immutable composition for explicit experimental activation and research controls.
 * This factory does not change the production default. */
public record ConstructionSearch(FlatSearchConfig mechanisms,int workers,int hashMiB,
                                 AspirationSearch.Policy aspiration) {
    public ConstructionSearch {
        Objects.requireNonNull(mechanisms,"mechanisms");
        if(workers<1 || workers>16) throw new IllegalArgumentException("Workers must be 1..16");
        if(hashMiB<1 || hashMiB>1024) throw new IllegalArgumentException("Hash must be 1..1024 MiB");
    }
    public static ConstructionSearch v1(int workers, int hashMiB, AspirationSearch.Policy aspiration) {
        return new ConstructionSearch(FlatSearchConfig.candidate(), workers, hashMiB, aspiration);
    }
    public SingleDepthSearch create(SearchEvaluation evaluation) {
        Objects.requireNonNull(evaluation,"evaluation");
        if((mechanisms.has(FlatSearchConfig.Feature.FUTILITY)||mechanisms.has(FlatSearchConfig.Feature.REVERSE_FUTILITY))
                && !evaluation.isBrnPair2()) throw new IllegalArgumentException("Construction margins require fixed Pair-2 evaluation");
        var table=new TTable(hashMiB);
        if(workers==1) {
            var owner=ExactSearchAdapter.constructionResearch(evaluation,table,mechanisms);
            return aspiration==null?owner:new AspirationSearch(owner,aspiration);
        }
        var owner=ParallelSearch.constructionResearch(workers,evaluation,table,mechanisms);
        return aspiration==null?owner:new AspirationSearch(owner,aspiration);
    }
    public String identity() {
        return mechanisms.identity()+";workers="+workers+";hashMiB="+hashMiB
                +";aspiration="+(aspiration==null?"OFF":aspiration)+";parallel="
                +(workers==1?"OWNER":mechanisms.has(FlatSearchConfig.Feature.EFFORT)?"ROOT_REUSE":"ROOT_SPLIT");
    }
}
