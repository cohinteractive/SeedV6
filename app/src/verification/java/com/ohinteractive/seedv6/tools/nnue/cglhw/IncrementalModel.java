package com.ohinteractive.seedv6.tools.nnue.cglhw;

import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.search.exact.ExactEvaluator;

/** Verification-only experiment seam; production definitions and codecs stay unchanged. */
public interface IncrementalModel {
    int parameters();
    Worker worker(int capacity);

    abstract class Worker implements ExactEvaluator {
        private final NnueScoreMapping mapping;
        protected Worker(double scale) { mapping=new NnueScoreMapping(scale); }
        public abstract void refresh(long[] board,int slot);
        public abstract void transition(long[] parent,long[] child,int source,int destination);
        public abstract double raw(long[] board,int slot);
        @Override public final void initialize(long[] board) { refresh(board,0); }
        @Override public final void child(long[] parent,long[] child,int ply) { transition(parent,child,ply,ply+1); }
        @Override public int evaluate(long[] board,int ply) { return mapping.map(StrictMath.tanh(raw(board,ply))); }
    }
}
