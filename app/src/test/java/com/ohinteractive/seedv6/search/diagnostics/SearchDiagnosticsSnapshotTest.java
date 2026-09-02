package com.ohinteractive.seedv6.search.diagnostics;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot.WorkerMetrics;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.ProbeOutcome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchDiagnosticsSnapshotTest {

    @Test
    void snapshotsAreDetachedAndWorkerMergeUsesSumsAndMaximaOnly() {
        final SearchDiagnostics left = new SearchDiagnostics();
        left.recordMainNode(2);
        left.recordQNode(3, 1);
        left.recordTtProbe(ProbeOutcome.DEPTH_INSUFFICIENT);
        left.recordBetaCutoff(2, false, false, false, true);
        left.recordMateDistanceCutoff();
        left.recordRazorAttempt();
        left.recordRazorAccepted();
        left.recordFutilityEligibleNode();
        left.recordFutilityQuietMovePruned();
        final SearchDiagnosticsSnapshot retained = left.snapshot();

        left.reset();
        left.recordMainNode(1);
        assertEquals(1L, retained.worker().nodes().mainNodes());
        assertEquals(1L, retained.worker().nodes().qNodes());
        assertEquals(3, retained.worker().nodes().maximumAbsolutePly());
        assertEquals(2L, retained.worker().moveOrder().cutoffRankSum());

        final SearchDiagnostics right = new SearchDiagnostics();
        right.recordMainNode(5);
        right.recordTtProbe(ProbeOutcome.EXACT_HIT);
        right.recordTtCutoff(ProbeOutcome.EXACT_HIT);
        right.recordBetaCutoff(9, true, true, false, false);
        right.recordRazorAttempt();
        right.recordFutilityQuietMovePruned();
        final WorkerMetrics merged = retained.worker().merge(right.snapshot().worker());
        assertEquals(2L, merged.nodes().mainNodes());
        assertEquals(1L, merged.nodes().qNodes());
        assertEquals(5, merged.nodes().maximumAbsolutePly());
        assertEquals(2L, merged.transpositionTable().probes());
        assertEquals(2L, merged.transpositionTable().keyMatches());
        assertEquals(1L, merged.transpositionTable().insufficientDepthMatches());
        assertEquals(1L, merged.transpositionTable().exactCutoffs());
        assertEquals(2L, merged.moveOrder().betaCutoffs());
        assertEquals(11L, merged.moveOrder().cutoffRankSum());
        assertEquals(9, merged.moveOrder().maximumCutoffRank());
        assertEquals(1L, merged.moveOrder().cutoffRank2());
        assertEquals(1L, merged.moveOrder().cutoffRank9Plus());
        assertEquals(1L, merged.selective().mateDistanceCutoffs());
        assertEquals(2L, merged.selective().razorAttempts());
        assertEquals(2L, merged.selective().razorQsearchProbes());
        assertEquals(1L, merged.selective().razorAcceptedResults());
        assertEquals(1L, merged.selective().futilityEligibleNodes());
        assertEquals(2L, merged.selective().futilityQuietMovesPruned());

        final SearchDiagnosticsSnapshot mergedSnapshot = retained.mergeWorkers(right.snapshot());
        assertEquals(merged, mergedSnapshot.worker());
        assertEquals(SearchDiagnosticsSnapshot.IterationMetrics.empty(), mergedSnapshot.iteration());
        assertThrows(IllegalArgumentException.class, () -> retained.withIteration(
            new SearchDiagnosticsSnapshot.IterationMetrics(1, 0, 0, 0, 0, 1)
        ).mergeWorkers(right.snapshot()));
    }

    @Test
    void disabledSnapshotIsAllocationFreeSingletonAndEnabledEmptyIsExplicit() {
        assertSame(SearchDiagnosticsSnapshot.disabled(), SearchDiagnosticsSnapshot.disabled());
        assertSame(SearchDiagnosticsSnapshot.enabledEmpty(), SearchDiagnosticsSnapshot.enabledEmpty());
        assertTrue(SearchDiagnosticsSnapshot.enabledEmpty().enabled());
        assertEquals(0L, SearchDiagnosticsSnapshot.enabledEmpty().totalEnteredNodes());
        assertEquals(SearchDiagnosticsSnapshot.IterationMetrics.empty(),
            SearchDiagnosticsSnapshot.enabledEmpty().iteration());
    }
}
