package com.ohinteractive.seedv6.training.validation;

import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.common.TimeSource;

/** One arena caller and an optional cancelling thread; cancellation also interrupts active search. */
public final class ValidationControl {
    private volatile boolean cancelled;
    private SearchControl active;

    public synchronized void cancel() {
        cancelled = true;
        if (active != null) active.request(SearchTermination.STOPPED);
    }
    public boolean cancelled() { return cancelled; }

    /** Enter one synchronous fixed-depth search; shared by validation and the read-only strength arena. */
    public synchronized SearchControl beginSearch() {
        if (active != null) throw new IllegalStateException("Control is already in use.");
        active = SearchControl.controlled(-1, System.nanoTime(), -1, TimeSource.SYSTEM);
        if (cancelled) active.request(SearchTermination.STOPPED);
        return active;
    }
    public synchronized void endSearch() { active = null; }
}
