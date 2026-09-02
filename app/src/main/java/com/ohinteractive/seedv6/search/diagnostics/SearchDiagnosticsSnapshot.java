package com.ohinteractive.seedv6.search.diagnostics;

import java.util.Objects;

/**
 * Immutable diagnostics publication for one search scope.
 *
 * <p>Worker metrics are cumulative within the scope. Their counters are
 * additive and their reached-depth fields are maxima, so
 * {@link WorkerMetrics#merge(WorkerMetrics)} is the future WS14 merge seam.
 * Iteration metrics are controller-owned final state and must not be merged as
 * worker data.</p>
 */
public record SearchDiagnosticsSnapshot(
    boolean enabled,
    WorkerMetrics worker,
    IterationMetrics iteration
) {

    public SearchDiagnosticsSnapshot {
        Objects.requireNonNull(worker, "worker");
        Objects.requireNonNull(iteration, "iteration");
    }

    public static SearchDiagnosticsSnapshot disabled() {
        return DISABLED;
    }

    public static SearchDiagnosticsSnapshot enabledEmpty() {
        return ENABLED_EMPTY;
    }

    public long totalEnteredNodes() {
        return worker.nodes().mainNodes() + worker.nodes().qNodes();
    }

    /** Reuses the immutable worker groups and replaces controller final state. */
    public SearchDiagnosticsSnapshot withIteration(IterationMetrics metrics) {
        Objects.requireNonNull(metrics, "metrics");
        if(!enabled && !metrics.isEmpty()) {
            throw new IllegalStateException("Disabled diagnostics cannot publish iteration metrics.");
        }
        if(!enabled) return DISABLED;
        return new SearchDiagnosticsSnapshot(true, worker, metrics);
    }

    /**
     * Merge two worker-only snapshots. Iteration fields remain controller-owned
     * and therefore must be empty on both inputs.
     */
    public SearchDiagnosticsSnapshot mergeWorkers(SearchDiagnosticsSnapshot other) {
        Objects.requireNonNull(other, "other");
        if(enabled != other.enabled) {
            throw new IllegalArgumentException("Worker diagnostics modes must match.");
        }
        if(!iteration.isEmpty() || !other.iteration.isEmpty()) {
            throw new IllegalArgumentException("Controller iteration metrics are not mergeable.");
        }
        if(!enabled) return DISABLED;
        return new SearchDiagnosticsSnapshot(
            true, worker.merge(other.worker), IterationMetrics.empty()
        );
    }

    public record WorkerMetrics(
        NodeMetrics nodes,
        TtMetrics transpositionTable,
        MoveOrderMetrics moveOrder,
        QsearchMetrics qsearch,
        SelectiveMetrics selective
    ) {
        public WorkerMetrics {
            Objects.requireNonNull(nodes, "nodes");
            Objects.requireNonNull(transpositionTable, "transpositionTable");
            Objects.requireNonNull(moveOrder, "moveOrder");
            Objects.requireNonNull(qsearch, "qsearch");
            Objects.requireNonNull(selective, "selective");
        }

        /** Additive sums plus maximum reached depths; no controller state. */
        public WorkerMetrics merge(WorkerMetrics other) {
            Objects.requireNonNull(other, "other");
            return new WorkerMetrics(
                nodes.merge(other.nodes),
                transpositionTable.merge(other.transpositionTable),
                moveOrder.merge(other.moveOrder),
                qsearch.merge(other.qsearch),
                selective.merge(other.selective)
            );
        }
    }

    /** Main/q nodes are successful authoritative child entries; depths are maxima. */
    public record NodeMetrics(
        long mainNodes,
        long qNodes,
        int maximumAbsolutePly,
        int maximumQply
    ) {
        public long totalEnteredNodes() {
            return mainNodes + qNodes;
        }

        NodeMetrics merge(NodeMetrics other) {
            return new NodeMetrics(
                mainNodes + other.mainNodes,
                qNodes + other.qNodes,
                Math.max(maximumAbsolutePly, other.maximumAbsolutePly),
                Math.max(maximumQply, other.maximumQply)
            );
        }
    }

    /** Probe outcomes remain semantically distinct; stores are successful writes. */
    public record TtMetrics(
        long probes,
        long keyMatches,
        long insufficientDepthMatches,
        long exactCutoffs,
        long lowerBoundCutoffs,
        long upperBoundCutoffs,
        long hashMovesAvailable,
        long stores
    ) {
        public long usableBoundCutoffs() {
            return exactCutoffs + lowerBoundCutoffs + upperBoundCutoffs;
        }

        TtMetrics merge(TtMetrics other) {
            return new TtMetrics(
                probes + other.probes,
                keyMatches + other.keyMatches,
                insufficientDepthMatches + other.insufficientDepthMatches,
                exactCutoffs + other.exactCutoffs,
                lowerBoundCutoffs + other.lowerBoundCutoffs,
                upperBoundCutoffs + other.upperBoundCutoffs,
                hashMovesAvailable + other.hashMovesAvailable,
                stores + other.stores
            );
        }
    }

    /**
     * A cutoff rank is the one-based position of its distinct move in actual
     * legal search order. PVS re-search of that move does not add a rank.
     */
    public record MoveOrderMetrics(
        long legalMovesSearched,
        long betaCutoffs,
        long firstMoveBetaCutoffs,
        long cutoffRankSum,
        int maximumCutoffRank,
        long cutoffRank1,
        long cutoffRank2,
        long cutoffRank3,
        long cutoffRank4,
        long cutoffRank5To8,
        long cutoffRank9Plus,
        long hashMoveCutoffs,
        long tacticalCutoffs,
        long quietCutoffs,
        long killerCutoffs,
        long historyCutoffs
    ) {
        MoveOrderMetrics merge(MoveOrderMetrics other) {
            return new MoveOrderMetrics(
                legalMovesSearched + other.legalMovesSearched,
                betaCutoffs + other.betaCutoffs,
                firstMoveBetaCutoffs + other.firstMoveBetaCutoffs,
                cutoffRankSum + other.cutoffRankSum,
                Math.max(maximumCutoffRank, other.maximumCutoffRank),
                cutoffRank1 + other.cutoffRank1,
                cutoffRank2 + other.cutoffRank2,
                cutoffRank3 + other.cutoffRank3,
                cutoffRank4 + other.cutoffRank4,
                cutoffRank5To8 + other.cutoffRank5To8,
                cutoffRank9Plus + other.cutoffRank9Plus,
                hashMoveCutoffs + other.hashMoveCutoffs,
                tacticalCutoffs + other.tacticalCutoffs,
                quietCutoffs + other.quietCutoffs,
                killerCutoffs + other.killerCutoffs,
                historyCutoffs + other.historyCutoffs
            );
        }
    }

    /** Qsearch event counters; checked nodes are entered qnodes, not leaf roots. */
    public record QsearchMetrics(
        long checkedQNodes,
        long standPatCutoffs,
        long tacticalMovesSearched,
        long evasionMovesSearched,
        long softDepthLimitEncounters,
        long qmateTerminals
    ) {
        QsearchMetrics merge(QsearchMetrics other) {
            return new QsearchMetrics(
                checkedQNodes + other.checkedQNodes,
                standPatCutoffs + other.standPatCutoffs,
                tacticalMovesSearched + other.tacticalMovesSearched,
                evasionMovesSearched + other.evasionMovesSearched,
                softDepthLimitEncounters + other.softDepthLimitEncounters,
                qmateTerminals + other.qmateTerminals
            );
        }
    }

    /**
     * WS13 event counters. Attempts mean an actual comparison or probe, while
     * cutoff and prune fields describe the resulting decision.
     */
    public record SelectiveMetrics(
        long mateDistanceCutoffs,
        long razorAttempts,
        long razorQsearchProbes,
        long razorAcceptedResults,
        long futilityEligibleNodes,
        long futilityQuietMovesPruned
    ) {
        SelectiveMetrics merge(SelectiveMetrics other) {
            return new SelectiveMetrics(
                mateDistanceCutoffs + other.mateDistanceCutoffs,
                razorAttempts + other.razorAttempts,
                razorQsearchProbes + other.razorQsearchProbes,
                razorAcceptedResults + other.razorAcceptedResults,
                futilityEligibleNodes + other.futilityEligibleNodes,
                futilityQuietMovesPruned + other.futilityQuietMovesPruned
            );
        }
    }

    /** Controller-owned cumulative counters plus deepest completed final state. */
    public record IterationMetrics(
        long completedIterations,
        long aspirationAttempts,
        long failLowResearches,
        long failHighResearches,
        long fullWindowFallbacks,
        int deepestCompletedDepth
    ) {
        public static IterationMetrics empty() {
            return EMPTY_ITERATION;
        }

        public boolean isEmpty() {
            return completedIterations == 0L
                && aspirationAttempts == 0L
                && failLowResearches == 0L
                && failHighResearches == 0L
                && fullWindowFallbacks == 0L
                && deepestCompletedDepth == 0;
        }
    }

    private static final NodeMetrics EMPTY_NODES = new NodeMetrics(0L, 0L, 0, 0);
    private static final TtMetrics EMPTY_TT = new TtMetrics(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
    private static final MoveOrderMetrics EMPTY_MOVE_ORDER = new MoveOrderMetrics(
        0L, 0L, 0L, 0L, 0, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L
    );
    private static final QsearchMetrics EMPTY_QSEARCH = new QsearchMetrics(0L, 0L, 0L, 0L, 0L, 0L);
    private static final SelectiveMetrics EMPTY_SELECTIVE = new SelectiveMetrics(
        0L, 0L, 0L, 0L, 0L, 0L
    );
    private static final IterationMetrics EMPTY_ITERATION = new IterationMetrics(0L, 0L, 0L, 0L, 0L, 0);
    private static final WorkerMetrics EMPTY_WORKER = new WorkerMetrics(
        EMPTY_NODES, EMPTY_TT, EMPTY_MOVE_ORDER, EMPTY_QSEARCH, EMPTY_SELECTIVE
    );
    private static final SearchDiagnosticsSnapshot DISABLED = new SearchDiagnosticsSnapshot(
        false, EMPTY_WORKER, EMPTY_ITERATION
    );
    private static final SearchDiagnosticsSnapshot ENABLED_EMPTY = new SearchDiagnosticsSnapshot(
        true, EMPTY_WORKER, EMPTY_ITERATION
    );
}
