package com.ohinteractive.seedv6.search.driver;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.exact.Cp64SpendingPolicy;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import static org.junit.jupiter.api.Assertions.*;

class SpendingPolicyTest {
    static SearchResult result(int depth, long move, int score) {
        return new SearchResult(move, true, score, depth, 1, 20, true, new long[]{move}, null, true);
    }
    static SingleDepthSearch unused() { return new SingleDepthSearch() {
        @Override public int maxSupportedDepth() { return 64; }
        @Override public SearchResult search(SearchRequest request) { throw new AssertionError("Unexpected Search"); }
    }; }

    @Test void changedMoveBuysExactlyOneCompletedIterationComparedWithMatchedControl() {
        for(boolean enabled : new boolean[]{false, true}) {
            var policy = new Cp64SpendingPolicy(unused(), enabled);
            policy.beginRequest();
            for(int d=1; d<=5; d++) assertFalse(policy.clockAllocationComplete(result(d, 42, 12), 20, 200));
            assertEquals(!enabled, policy.clockAllocationComplete(result(6, 43, 12), 100, 200));
            // Even another move change does not buy a second extension.
            assertTrue(policy.clockAllocationComplete(result(7, 44, 12), 120, 200));
            policy.endRequest();
        }
    }

    @Test void stabilityAndDepthGuardRemainAndMateIsNeverOrdinaryEvidence() {
        var policy = new Cp64SpendingPolicy(unused(), true);
        for(int d=1; d<=5; d++) assertFalse(policy.clockAllocationComplete(result(d, 42, 12), 150, 200));
        assertFalse(policy.clockAllocationComplete(result(6, 42, 12), 49, 200));
        assertTrue(policy.clockAllocationComplete(result(7, 42, 12), 50, 200));
        assertFalse(policy.clockAllocationComplete(result(8, 43, TranspositionScores.MATE_SCORE - 5), 180, 200));
        // No prior ordinary adjacent evidence after mate; ordinary target is conservative.
        assertTrue(policy.clockAllocationComplete(result(9, 44, 12), 100, 200));
        policy.endRequest(); policy.beginRequest();
        assertTrue(policy.clockAllocationComplete(result(6, 45, 12), 100, 200));
        policy.newGame();
        assertTrue(policy.clockAllocationComplete(result(6, 46, 12), 100, 200));
    }

    @Test void aCompletedMateIterationAlsoExhaustsTheOneIterationExtension() {
        var policy = new Cp64SpendingPolicy(unused(), true);
        assertFalse(policy.clockAllocationComplete(result(5, 42, 12), 20, 200));
        assertFalse(policy.clockAllocationComplete(result(6, 43, 12), 100, 200));
        assertTrue(policy.clockAllocationComplete(result(7, 43, TranspositionScores.MATE_SCORE - 5), 120, 200));
    }

    @Test void allocationIsOverflowSafeAndPreservesReserveAndLowClockFloor() {
        assertEquals(796, Cp64SpendingPolicy.hardBudgetMillis(10000, 100, 30));
        for(long remaining : new long[]{0, 1, 49, 50, 51, 100, 3000, Long.MAX_VALUE}) {
            for(long increment : new long[]{0, 100, Long.MAX_VALUE}) {
                for(int moves : new int[]{1, 30, Integer.MAX_VALUE}) {
                    long hard = Cp64SpendingPolicy.hardBudgetMillis(remaining, increment, moves);
                    assertTrue(hard >= 0 && hard <= Math.max(0L, remaining - 50));
                    if(remaining > 50) assertTrue(hard >= 1); else assertEquals(0, hard);
                }
            }
        }
        var policy = new Cp64SpendingPolicy(unused(), false);
        assertFalse(policy.clockAllocationComplete(result(6, 42, 12), Long.MAX_VALUE/2, Long.MAX_VALUE));
        assertTrue(policy.clockAllocationComplete(result(7, 43, 12), Long.MAX_VALUE/2+1, Long.MAX_VALUE));
    }

    @Test void realDriverHardStopDuringExtraIterationPublishesOnlyPreviousCompletion() {
        var clock = new AtomicLong(); var publications = new ArrayList<Integer>();
        var control = SearchControl.controlled(-1, 0, 200, clock::get, true);
        SingleDepthSearch owner = new SingleDepthSearch() {
            @Override public int maxSupportedDepth() { return 64; }
            @Override public SearchResult search(SearchRequest request) {
                request.control().tryEnterNode();
                if(request.depth() == 7) {
                    clock.set(200); assertFalse(request.control().checkpoint());
                    return new SearchResult(0, false, 0, 7, 1, 20, false);
                }
                clock.set(request.depth() == 6 ? 110 : request.depth() * 5);
                return result(request.depth(), request.depth() == 6 ? 43 : 42, 12);
            }
        };
        var board = Board.startingPosition();
        try(var driver = new SearchDriver(new Cp64SpendingPolicy(owner, true))) {
            var outcome = driver.search(new SearchRequest(board, GameHistory.initial(board), 12,
                new SearchObserver() { @Override public void onIterationCompleted(IterationSnapshot snapshot) {
                    publications.add(snapshot.depth());
                } }, control, false));
            assertEquals(SearchTermination.TIME_LIMIT, control.termination());
            assertEquals(7, outcome.attemptedDepth()); assertEquals(6, outcome.lastCompletedResult().depth());
            assertTrue(outcome.iterationIncomplete()); assertEquals(0, driver.activeSearchThreads());
            assertEquals(java.util.List.of(1,2,3,4,5,6), publications);
        }
    }
}
