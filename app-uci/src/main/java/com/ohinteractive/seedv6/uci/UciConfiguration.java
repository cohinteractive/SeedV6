package com.ohinteractive.seedv6.uci;

import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.search.manage.TimeManager;

/** Application composition, not an evaluator registry. Legacy stores stay in app; explicit research composition stays outside production. */
public interface UciConfiguration {
    SearchLifecycleService service(int workers, int hashMiB);
    String identity(int workers);
    default String identity(int workers, int hashMiB) { return identity(workers); }
    default String name() { return "SeedV6"; }
    default boolean standalone() { return false; }
    default boolean acceptsWorkers(int workers) { return true; }
    default int maximumWorkers() { return 16; }
    default boolean announcesResourceChanges() { return true; }
    /** Ordinary clocks only. The parser independently enforces the clock reserve.
     * Explicit movetime, depth, nodes and infinite requests keep their own contracts. */
    default long clockBudgetMillis(long remainingMillis, long incrementMillis, int movesToGo) {
        return TimeManager.allocateClockMillis(remainingMillis, incrementMillis, movesToGo);
    }
}
