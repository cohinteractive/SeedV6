package com.ohinteractive.seedv6.training.selfplay;

import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.common.TimeSource;

/** One generation/training caller plus a cancelling thread. Sticky cancellation, no owned threads. */
public final class SelfPlayControl {
    private volatile boolean cancelled;
    private SearchControl activeSearch;

    public synchronized void cancel() {
        cancelled = true;
        if (activeSearch != null) activeSearch.request(SearchTermination.STOPPED);
    }

    public boolean cancelled() { return cancelled; }

    synchronized SearchControl beginSearch(SelfPlayConfig config) {
        if (activeSearch != null) throw new IllegalStateException("Control already owns a search.");
        activeSearch = SearchControl.controlled(config.nodesPerMove(), System.nanoTime(),
                config.millisPerMove() < 0 ? -1 : config.millisPerMove() * 1_000_000, TimeSource.SYSTEM);
        if (cancelled) activeSearch.request(SearchTermination.STOPPED);
        return activeSearch;
    }

    synchronized void endSearch() { activeSearch = null; }
}
