package com.ohinteractive.seedv6.search.exact;

import java.lang.reflect.Field;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnpair2.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;

/** Inspect the real production factory before attaching observation-only sinks.
 * No policy builder is used to make the implementation pass its own routing test. */
@Timeout(180)
class ProductionSeeTest {
    static SearchEvaluation evaluation() {
        return SearchEvaluation.brnPair2(new BrnPair2Model(new BrnPair2Trainer(.01).snapshot()));
    }

    @Test void everySupportedProductionWorkerKeepsCumulativePoliciesAndPrivateState() {
        var evaluation = evaluation();
        var board = Board.fromFen(SeePruningTest.MIDDLE);
        for(int count = 1; count <= 16; count++) {
            try(var facility = ProductionSearch.create(count, evaluation);
                var driver = new SearchDriver(facility)) {
                var searches = searches(facility);
                assertEquals(count, searches.length);
                var histories = Collections.newSetFromMap(new IdentityHashMap<>());
                var sinks = new SeePolicy.Diagnostics[count];
                for(int i = 0; i < count; i++) {
                    var search = searches[i];
                    assertComposition(search, SeePolicy.QXRAY);
                    assertTrue(histories.add(field(search, "quietHistory")));
                    assertNotNull(field(search, "iterationReuse"));
                    sinks[i] = new SeePolicy.Diagnostics();
                    set(search, "seeDiagnostics", sinks[i]);
                }
                var control = SearchControl.controlled(500_000, 0, -1, TimeSource.SYSTEM);
                var result = driver.search(new SearchRequest(board, GameHistory.initial(board), 6,
                        SearchObserver.NONE, control, true));
                assertEquals(control.nodes(), result.nodes());
                assertEquals(result.nodes(), result.diagnostics().totalEnteredNodes());
                assertNotNull(result.lastCompletedResult());
                Phase2Oracle.legalPv(board, result.lastCompletedResult().principalVariation());
                assertEquals(0, driver.activeSearchThreads());
                assertTrue(sinks[0].qOmitted > 0, "Owner " + count);
                if(count > 1) assertTrue(Arrays.stream(sinks).skip(1).mapToLong(d -> d.qOmitted).sum() > 0,
                        "Helpers " + count);
                System.out.println("SR024 production workers=" + count + " qOmitted="
                        + Arrays.toString(Arrays.stream(sinks).mapToLong(d -> d.qOmitted).toArray()));
                driver.newGame();
                for(var search : searches) {
                    assertComposition(search, SeePolicy.QXRAY);
                }
                var limited = SearchControl.controlled(1000, 0, -1, TimeSource.SYSTEM);
                assertEquals(1000, driver.search(new SearchRequest(board, GameHistory.initial(board), 20, limited)).nodes());
                assertEquals(0, driver.activeSearchThreads());
                driver.newGame();
                var again = driver.search(new SearchRequest(board, 2));
                assertTrue(again.targetDepthCompleted());
                // History is cleared on the next request's first search, not eagerly
                // by newGame. Check observable fresh-owner parity in the native lane.
                if(count == 1) try(var fresh = new SearchDriver(ProductionSearch.create(1, evaluation))) {
                    var expected = fresh.search(new SearchRequest(board, 2));
                    assertEquals(expected.nodes(), again.nodes());
                    assertEquals(expected.lastCompletedResult(), again.lastCompletedResult());
                }
            }
        }
    }

    @Test void deeperFourAndSixteenWorkerProductionExecuteSeeLmrAndLmpInEveryWorker() {
        var board = Board.fromFen(SeePruningTest.MIDDLE);
        for(int count : new int[] {4, 16})
        try(var facility = ProductionSearch.create(count, evaluation());
            var driver = new SearchDriver(facility)) {
            var searches = searches(facility);
            var see = new SeePolicy.Diagnostics[count];
            var reductions = new long[count]; var omissions = new long[count];
            for(int i = 0; i < count; i++) {
                final int worker = i;
                assertComposition(searches[i], SeePolicy.QXRAY);
                set(searches[i], "seeDiagnostics", see[i] = new SeePolicy.Diagnostics());
                set(searches[i], "lmrDiagnostics", new LmrDiagnostics() {
                    @Override public void probe(long[] child, int ply, int depth, int reduction, int ordinal,
                            int alpha, int beta, int score, boolean verifiedNext) { reductions[worker]++; }
                });
                set(searches[i], "lmpDiagnostics", new LmpDiagnostics() {
                    @Override public void omitted(long[] parent, long[] child, long move, int ply, int depth,
                            int searched, int quiets, int alpha, int beta) { omissions[worker]++; }
                });
            }
            var control = SearchControl.controlled(2_000_000, 0, -1, TimeSource.SYSTEM);
            var result = driver.search(new SearchRequest(board, GameHistory.initial(board), 8, control));
            assertNotNull(result.lastCompletedResult());
            assertEquals(0, driver.activeSearchThreads());
            Phase2Oracle.legalPv(board, result.lastCompletedResult().principalVariation());
            for(int i = 0; i < count; i++) {
                assertTrue(see[i].qOmitted > 0, "SEE worker " + i + "/" + count);
                assertTrue(reductions[i] > 0, "LMR worker " + i + "/" + count);
                assertTrue(omissions[i] > 0, "LMP worker " + i + "/" + count);
            }
            System.out.println("SR024 deeper workers=" + count + " SEE="
                    + Arrays.toString(Arrays.stream(see).mapToLong(d -> d.qOmitted).toArray())
                    + " LMR=" + Arrays.toString(reductions) + " LMP=" + Arrays.toString(omissions));
        }
    }

    @Test void explicitOffRetainsPriorCompositionAndOtherEvaluatorsStayOff() {
        for(int count : new int[] {1, 4, 16}) {
            try(var control = ProductionSearch.createSeeControl(count, evaluation())) {
                for(var search : searches(control)) assertComposition(search, SeePolicy.OFF);
            }
            try(var hce = ProductionSearch.create(count, SearchEvaluation.handcrafted())) {
                for(var search : searches(hce)) assertEquals(SeePolicy.OFF, field(search, "seePolicy"));
            }
        }
    }

    private static void assertComposition(Object search, SeePolicy policy) {
        assertEquals(policy, field(search, "seePolicy"));
        assertEquals(ExactSearch.LMR_QUIET, field(search, "lmrPolicy"));
        assertEquals(ExactSearch.LMP_WINDOW, field(search, "lmpPolicy"));
        assertEquals(true, field(search, "quiescence"));
        assertEquals(true, field(search, "nominalQsearch"));
        assertEquals(false, field(search, "nullMovePruning"));
        assertNull(field(search, "qtable"));
    }

    private static Object[] searches(Object facility) {
        if(facility instanceof ExactSearchAdapter) return new Object[] {field(facility, "exact")};
        assertInstanceOf(ParallelSearch.class, facility);
        return Arrays.stream((Object[])field(facility, "workers")).map(w -> field(w, "search")).toArray();
    }
    private static Object field(Object owner, String name) {
        try { Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner); }
        catch(ReflectiveOperationException e) {throw new AssertionError(e);}
    }
    private static void set(Object owner, String name, Object value) {
        try { Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);f.set(owner,value); }
        catch(ReflectiveOperationException e) {throw new AssertionError(e);}
    }
}
