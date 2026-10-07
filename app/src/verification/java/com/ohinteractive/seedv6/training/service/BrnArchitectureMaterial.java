package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.nnue.NnueMaterialBootstrap;
import com.ohinteractive.seedv6.core.brn3.Brn3Features;
import com.ohinteractive.seedv6.core.brn3.Brn3Objective;
import com.ohinteractive.seedv6.search.exact.ExactEvaluator;
import com.ohinteractive.seedv6.search.exact.ExactSearch;

/** I01 cheap prior control; distinct from running the same prior through a whole BRN Gen0. */
final class BrnArchitectureMaterial {
    static final String ID="incremental-fixed-material-v1";
    static ExactEvaluator evaluator() {
        return new ExactEvaluator() {
            final int[] material=new int[ExactSearch.MAX_DEPTH+1];
            @Override public void initialize(long[] b){material[0]=NnueMaterialBootstrap.whiteScore(b);}
            @Override public void child(long[] parent,long[] child,int ply){material[ply+1]=NnueMaterialBootstrap.update(parent,child,material[ply]);}
            @Override public int evaluate(long[] b,int ply){return NnueMaterialBootstrap.combine(NnueMaterialBootstrap.forSideToMove(b,material[ply]),0);}
        };
    }
    static BrnArchitectureControls.View view() {
        return new BrnArchitectureControls.View(b->Brn3Objective.outcome(Brn3Features.material(b),b),
                Brn3Features::material,BrnArchitectureMaterial::evaluator);
    }
    private BrnArchitectureMaterial(){}
}
