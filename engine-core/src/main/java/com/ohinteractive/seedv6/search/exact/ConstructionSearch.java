package com.ohinteractive.seedv6.search.exact;

import java.util.Objects;
import com.ohinteractive.seedv6.search.common.SingleDepthSearch;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;

/** Shared immutable Construction V1 composition and independent research controls. */
public record ConstructionSearch(FlatSearchConfig mechanisms,int workers,int hashMiB,
                                 AspirationSearch.Policy aspiration, RootScheduling rootScheduling) {
    public enum RootScheduling { STANDARD, EAGER_ROOT }
    /** Historical explicit controls retain their original scheduling. */
    public ConstructionSearch(FlatSearchConfig mechanisms, int workers, int hashMiB,
                              AspirationSearch.Policy aspiration) {
        this(mechanisms, workers, hashMiB, aspiration, RootScheduling.STANDARD);
    }
    public ConstructionSearch {
        Objects.requireNonNull(mechanisms,"mechanisms");
        Objects.requireNonNull(rootScheduling,"rootScheduling");
        if(workers<1 || workers>16) throw new IllegalArgumentException("Workers must be 1..16");
        if(hashMiB<1 || hashMiB>1024) throw new IllegalArgumentException("Hash must be 1..1024 MiB");
        if(rootScheduling==RootScheduling.EAGER_ROOT && workers==1)
            throw new IllegalArgumentException("Eager root scheduling requires multiple workers");
    }
    public static ConstructionSearch v1(int workers, int hashMiB, AspirationSearch.Policy aspiration) {
        return new ConstructionSearch(FlatSearchConfig.candidate(), workers, hashMiB, aspiration);
    }
    public SingleDepthSearch create(SearchEvaluation evaluation) {
        Objects.requireNonNull(evaluation,"evaluation");
        if((mechanisms.has(FlatSearchConfig.Feature.FUTILITY)||mechanisms.has(FlatSearchConfig.Feature.REVERSE_FUTILITY))
                && !(evaluation.isBrnPair2() || evaluation.isConventional() || evaluation.isResearchCentipawnsFullRecompute()
                    || evaluation.isResearchCentipawnsStateful() || evaluation == SearchEvaluation.handcrafted()))
            throw new IllegalArgumentException("Construction margins require HCE, Pair-2, conventional NNUE or explicit research centipawns");
        var table=new TTable(hashMiB,mechanisms.has(FlatSearchConfig.Feature.TT_INFORMATION));
        if(workers==1) {
            var owner=ExactSearchAdapter.constructionResearch(evaluation,table,mechanisms);
            return aspiration==null?owner:new AspirationSearch(owner,aspiration);
        }
        var owner=rootScheduling==RootScheduling.EAGER_ROOT
                ? ParallelSearch.constructionEagerRootResearch(workers,evaluation,table,mechanisms)
                : ParallelSearch.constructionResearch(workers,evaluation,table,mechanisms);
        return aspiration==null?owner:new AspirationSearch(owner,aspiration);
    }
    public String identity() {
        return mechanisms.identity()+";workers="+workers+";hashMiB="+hashMiB
                +";aspiration="+(aspiration==null?"OFF":aspiration)+";parallel="
                +parallelIdentity();
    }
    public String parallelIdentity() {
        return workers==1?"OWNER":rootScheduling==RootScheduling.EAGER_ROOT?"EAGER_ROOT"
                :mechanisms.has(FlatSearchConfig.Feature.EFFORT)?"ROOT_REUSE":"ROOT_SPLIT";
    }
}
