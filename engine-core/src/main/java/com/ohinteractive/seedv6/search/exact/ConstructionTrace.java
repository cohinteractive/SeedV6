package com.ohinteractive.seedv6.search.exact;

/** Optional synchronous research observation of a new threshold prediction.
 * Arrays are borrowed read-only until return. Off the ordinary production path. */
@FunctionalInterface
public interface ConstructionTrace {
    void prediction(boolean reverse,long[] board,long move,int ply,int depth,int alpha,int beta,
                    int rawEvaluation,long[] realPathMoves);
}
