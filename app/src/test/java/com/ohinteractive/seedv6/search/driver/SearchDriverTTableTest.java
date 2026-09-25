package com.ohinteractive.seedv6.search.driver;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

class SearchDriverTTableTest {
    @Test void oneGenerationPerRequestAllIterationsReuseOneTableWithoutNormalClears() {
        var table = new ObservedTable();
        var adapter = new ExactSearchAdapter(SearchEvaluation.handcrafted(), table);
        var driver = new SearchDriver(adapter);
        List<Integer> depths = new ArrayList<>();
        var observer = new SearchObserver() {
            public void onIterationCompleted(IterationSnapshot snapshot) {
                depths.add(snapshot.depth()); assertEquals(1, table.advances); assertEquals(0, table.clears);
                var entry = new TTable.TEntry(); assertTrue(table.probe(table.rootKey, entry));
                assertEquals(1, (entry.data >>> 10) & 255); assertEquals(snapshot.depth(), entry.data & 255);
            }
        };
        assertTrue(driver.search(new SearchRequest(Board.startingPosition(), 4, observer)).targetDepthCompleted());
        assertEquals(List.of(1, 2, 3, 4), depths); assertEquals(1, table.advances);
        assertTrue(driver.search(new SearchRequest(Board.startingPosition(), 2)).targetDepthCompleted());
        assertEquals(2, table.advances); assertEquals(0, table.clears);
        // Even a stopped top-level request establishes exactly one generation.
        var control = SearchControl.controlled(0, 0, -1, TimeSource.SYSTEM);
        assertNull(driver.search(new SearchRequest(Board.startingPosition(), GameHistory.initial(Board.startingPosition()), 2, control)).lastCompletedResult());
        assertEquals(3, table.advances); assertEquals(0, table.clears);
        driver.newGame(); assertEquals(1, table.clears);
        assertFalse(table.probe(table.rootKey, new TTable.TEntry()));
    }

    @Test void driverNodeBudgetDoesNotStoreIncompleteRoot() {
        var table = new ObservedTable(); var driver = new SearchDriver(new ExactSearchAdapter(SearchEvaluation.handcrafted(), table));
        var control = SearchControl.controlled(21, System.nanoTime(), -1, TimeSource.SYSTEM);
        var outcome = driver.search(new SearchRequest(Board.startingPosition(), GameHistory.initial(Board.startingPosition()), 3, control));
        assertTrue(outcome.iterationIncomplete()); assertEquals(1, outcome.lastCompletedResult().depth());
        var entry = new TTable.TEntry(); assertTrue(table.probe(table.rootKey, entry));
        assertEquals(1, entry.data & 255); // cancelled depth two did not overwrite completed depth one
        assertEquals(1, table.advances);
    }

    @Test void productionDefaultUsesAcceptedTableSizingAndReferenceAdapterCanDisableIt() throws Exception {
        var production = new ExactSearchAdapter();
        var exact = field(production, "exact"); var table = field(exact, "table");
        assertInstanceOf(TTable.class, table);
        assertEquals(Integer.highestOneBit(192 * 1024 * 1024 / 24), ((long[]) field(table, "data")).length);
        var reference = new ExactSearchAdapter(SearchEvaluation.handcrafted(), null);
        assertNull(field(field(reference, "exact"), "table"));
        var on = new SearchDriver(production).search(new SearchRequest(Board.startingPosition(), 3));
        var off = new SearchDriver(reference).search(new SearchRequest(Board.startingPosition(), 3));
        assertEquals(off.lastCompletedResult().score(), on.lastCompletedResult().score());
    }

    private static Object field(Object owner, String name) throws Exception {
        var field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner);
    }
    private static final class ObservedTable extends TTable {
        int advances, clears; long rootKey;
        ObservedTable() { super(4); }
        public void advanceGeneration() { advances++; super.advanceGeneration(); }
        public void clear() { clears++; super.clear(); }
        public void save(long key, int depth, int type, int score, long move) {
            if(advances == 1 && depth == 1 && rootKey == 0) rootKey = key;
            super.save(key, depth, type, score, move);
        }
    }
}
