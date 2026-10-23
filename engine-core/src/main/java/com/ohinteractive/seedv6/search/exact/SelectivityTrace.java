package com.ohinteractive.seedv6.search.exact;

/** Opt-in synchronous research observation. Callers must not mutate borrowed boards/path.
 * These are selective invocation facts, never score/proof or learning authority. */
public interface SelectivityTrace {
    /** A main node surviving legal/terminal/TT/domain and existing reverse-futility resolution.
     * raw can be MIN_VALUE when not already evaluated; tracing never requests evaluation. */
    default void survivor(long[] board,long[] path,int ply,int depth,int alpha,int beta,
            boolean scout,boolean checked,int raw) {}
    /** Actually searched later move after existing omissions; reductions are before/after relief.
     * ordinal/searchedQuiets exclude omitted moves and count the current begun child once. */
    default void reduction(long[] board,long move,long[] path,int ply,int depth,int ordinal,int searchedQuiets,
            int alpha,int beta,int history,int continuation,int raw,int previousRaw,
            int baseReduction,int assignedReduction,boolean caution) {}
}
