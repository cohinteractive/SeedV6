package com.ohinteractive.seedv6.search.exact;

/** Optional request-worker-private O1 counters; no score or result authority. */
public final class MoveHintDiagnostics {
    public long probes,hits,illegal,completedProducers,stored;
    void reset() {probes=hits=illegal=completedProducers=stored=0;}
}
