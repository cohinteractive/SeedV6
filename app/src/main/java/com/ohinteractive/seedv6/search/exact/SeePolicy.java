package com.ohinteractive.seedv6.search.exact;

/** Explicit SR-024 experiments over cumulative Search; OFF preserves production. */
public enum SeePolicy {
    OFF,
    /** Omit negative nonchecking captures in qsearch; retain promotions and EP. */
    QSAFE,
    /** As QSAFE, but retain every move at the first qsearch layer. */
    QDEEP,
    /** Preserve original full-window qsearch calls; prune only original scouts. */
    QSCOUT,
    /** Retain exchanges losing at most one material pawn, including minor trades. */
    QPAWN,
    /** Protected later negative captures at nominal scout depths one through three. */
    MAIN,
    /** Exchange-negative later non-pawn/non-king quiets at scout depths two to four. */
    QUIET,
    /** Retain bad captures, ordering them after quiets in nominal Search. */
    ORDER;

    public boolean prunesQuiescence(int qply) {
        return this == QSAFE || this == QSCOUT || this == QPAWN || this == QDEEP && qply > 0;
    }

    /** Optional worker-private diagnostic sink; never shared across workers. */
    public static class Diagnostics {
        public long qCandidates, qOmitted, mainCandidates, mainOmitted, checksRetained, demoted;
        public final long[] omittedAtDepth = new long[ExactSearch.MAX_DEPTH + 1];
        public void omitted(long[] board, long[] child, long move, int ply, int depth,
                int qply, int searched, int searchedQuiets, int alpha, int beta) {}
    }
}
