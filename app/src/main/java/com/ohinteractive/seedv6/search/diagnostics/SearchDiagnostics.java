package com.ohinteractive.seedv6.search.diagnostics;

import java.util.Arrays;

import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot.MoveOrderMetrics;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot.NodeMetrics;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot.QsearchMetrics;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot.TtMetrics;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot.WorkerMetrics;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.ProbeOutcome;

/**
 * Worker-confined mutable primitive accumulator. It is reused and reset once
 * per top-level search scope; it performs no formatting, synchronization, or
 * hot-path allocation. Later heuristic counters extend the fixed index list.
 */
public final class SearchDiagnostics {

    public void reset() {
        Arrays.fill(values, 0L);
    }

    public void recordRoot() {
        maximum(MAXIMUM_ABSOLUTE_PLY, 0);
    }

    /** Called beside one successful main-search SearchControl entry. */
    public void recordMainNode(int absolutePly) {
        values[MAIN_NODES] ++;
        maximum(MAXIMUM_ABSOLUTE_PLY, absolutePly);
    }

    /** Called beside one successful qsearch SearchControl entry. */
    public void recordQNode(int absolutePly, int qPly) {
        values[Q_NODES] ++;
        maximum(MAXIMUM_ABSOLUTE_PLY, absolutePly);
        maximum(MAXIMUM_QPLY, qPly);
    }

    /** Records reached depth for a qsearch leaf root, which is already a main node. */
    public void recordQPosition(int absolutePly, int qPly) {
        maximum(MAXIMUM_ABSOLUTE_PLY, absolutePly);
        maximum(MAXIMUM_QPLY, qPly);
    }

    public void recordCheckedQNode() {
        values[CHECKED_Q_NODES] ++;
    }

    public void recordTtProbe(ProbeOutcome outcome) {
        values[TT_PROBES] ++;
        switch(outcome) {
            case EMPTY, KEY_MISMATCH -> { }
            case DEPTH_INSUFFICIENT -> {
                values[TT_KEY_MATCHES] ++;
                values[TT_DEPTH_INSUFFICIENT] ++;
            }
            default -> values[TT_KEY_MATCHES] ++;
        }
    }

    public void recordTtCutoff(ProbeOutcome outcome) {
        switch(outcome) {
            case EXACT_HIT -> values[TT_EXACT_CUTOFFS] ++;
            case LOWER_HIT -> values[TT_LOWER_CUTOFFS] ++;
            case UPPER_HIT -> values[TT_UPPER_CUTOFFS] ++;
            default -> throw new IllegalArgumentException("Not a usable TT cutoff outcome: " + outcome);
        }
    }

    public void recordHashMoveAvailable() {
        values[HASH_MOVES_AVAILABLE] ++;
    }

    public void recordTtStore() {
        values[TT_STORES] ++;
    }

    public void recordLegalMoveSearched() {
        values[LEGAL_MOVES_SEARCHED] ++;
    }

    public void recordBetaCutoff(
        int rank, boolean hashMove, boolean tactical,
        boolean killer, boolean history
    ) {
        if(rank < 1) throw new IllegalArgumentException("Cutoff rank must be positive: " + rank);
        values[BETA_CUTOFFS] ++;
        values[CUTOFF_RANK_SUM] += rank;
        maximum(MAXIMUM_CUTOFF_RANK, rank);
        if(rank == 1) {
            values[FIRST_MOVE_BETA_CUTOFFS] ++;
            values[CUTOFF_RANK_1] ++;
        } else if(rank == 2) {
            values[CUTOFF_RANK_2] ++;
        } else if(rank == 3) {
            values[CUTOFF_RANK_3] ++;
        } else if(rank == 4) {
            values[CUTOFF_RANK_4] ++;
        } else if(rank <= 8) {
            values[CUTOFF_RANK_5_TO_8] ++;
        } else {
            values[CUTOFF_RANK_9_PLUS] ++;
        }
        if(hashMove) values[HASH_MOVE_CUTOFFS] ++;
        if(tactical) values[TACTICAL_CUTOFFS] ++;
        else values[QUIET_CUTOFFS] ++;
        if(killer) values[KILLER_CUTOFFS] ++;
        if(history) values[HISTORY_CUTOFFS] ++;
    }

    public void recordStandPatCutoff() {
        values[STAND_PAT_CUTOFFS] ++;
    }

    public void recordQMoveSearched(boolean evasion) {
        values[evasion ? Q_EVASIONS_SEARCHED : Q_TACTICAL_MOVES_SEARCHED] ++;
    }

    public void recordSoftQdepthLimitEncounter() {
        values[SOFT_QDEPTH_LIMIT_ENCOUNTERS] ++;
    }

    public void recordQmate() {
        values[QMATE_TERMINALS] ++;
    }

    public SearchDiagnosticsSnapshot snapshot() {
        return new SearchDiagnosticsSnapshot(
            true,
            new WorkerMetrics(
                new NodeMetrics(
                    value(MAIN_NODES), value(Q_NODES), intValue(MAXIMUM_ABSOLUTE_PLY),
                    intValue(MAXIMUM_QPLY)
                ),
                new TtMetrics(
                    value(TT_PROBES), value(TT_KEY_MATCHES), value(TT_DEPTH_INSUFFICIENT),
                    value(TT_EXACT_CUTOFFS), value(TT_LOWER_CUTOFFS),
                    value(TT_UPPER_CUTOFFS), value(HASH_MOVES_AVAILABLE), value(TT_STORES)
                ),
                new MoveOrderMetrics(
                    value(LEGAL_MOVES_SEARCHED), value(BETA_CUTOFFS),
                    value(FIRST_MOVE_BETA_CUTOFFS), value(CUTOFF_RANK_SUM),
                    intValue(MAXIMUM_CUTOFF_RANK), value(CUTOFF_RANK_1),
                    value(CUTOFF_RANK_2), value(CUTOFF_RANK_3), value(CUTOFF_RANK_4),
                    value(CUTOFF_RANK_5_TO_8), value(CUTOFF_RANK_9_PLUS),
                    value(HASH_MOVE_CUTOFFS), value(TACTICAL_CUTOFFS), value(QUIET_CUTOFFS),
                    value(KILLER_CUTOFFS), value(HISTORY_CUTOFFS)
                ),
                new QsearchMetrics(
                    value(CHECKED_Q_NODES), value(STAND_PAT_CUTOFFS),
                    value(Q_TACTICAL_MOVES_SEARCHED), value(Q_EVASIONS_SEARCHED),
                    value(SOFT_QDEPTH_LIMIT_ENCOUNTERS), value(QMATE_TERMINALS)
                )
            ),
            SearchDiagnosticsSnapshot.IterationMetrics.empty()
        );
    }

    private static final int MAIN_NODES = 0;
    private static final int Q_NODES = 1;
    private static final int MAXIMUM_ABSOLUTE_PLY = 2;
    private static final int MAXIMUM_QPLY = 3;
    private static final int TT_PROBES = 4;
    private static final int TT_KEY_MATCHES = 5;
    private static final int TT_DEPTH_INSUFFICIENT = 6;
    private static final int TT_EXACT_CUTOFFS = 7;
    private static final int TT_LOWER_CUTOFFS = 8;
    private static final int TT_UPPER_CUTOFFS = 9;
    private static final int HASH_MOVES_AVAILABLE = 10;
    private static final int TT_STORES = 11;
    private static final int LEGAL_MOVES_SEARCHED = 12;
    private static final int BETA_CUTOFFS = 13;
    private static final int FIRST_MOVE_BETA_CUTOFFS = 14;
    private static final int CUTOFF_RANK_SUM = 15;
    private static final int MAXIMUM_CUTOFF_RANK = 16;
    private static final int CUTOFF_RANK_1 = 17;
    private static final int CUTOFF_RANK_2 = 18;
    private static final int CUTOFF_RANK_3 = 19;
    private static final int CUTOFF_RANK_4 = 20;
    private static final int CUTOFF_RANK_5_TO_8 = 21;
    private static final int CUTOFF_RANK_9_PLUS = 22;
    private static final int HASH_MOVE_CUTOFFS = 23;
    private static final int TACTICAL_CUTOFFS = 24;
    private static final int QUIET_CUTOFFS = 25;
    private static final int KILLER_CUTOFFS = 26;
    private static final int HISTORY_CUTOFFS = 27;
    private static final int CHECKED_Q_NODES = 28;
    private static final int STAND_PAT_CUTOFFS = 29;
    private static final int Q_TACTICAL_MOVES_SEARCHED = 30;
    private static final int Q_EVASIONS_SEARCHED = 31;
    private static final int SOFT_QDEPTH_LIMIT_ENCOUNTERS = 32;
    private static final int QMATE_TERMINALS = 33;
    private static final int COUNTER_COUNT = 34;

    private final long[] values = new long[COUNTER_COUNT];

    private long value(int index) {
        return values[index];
    }

    private int intValue(int index) {
        return Math.toIntExact(values[index]);
    }

    private void maximum(int index, int candidate) {
        if(candidate > values[index]) values[index] = candidate;
    }
}
