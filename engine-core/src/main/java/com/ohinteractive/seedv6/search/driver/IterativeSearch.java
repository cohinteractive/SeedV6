package com.ohinteractive.seedv6.search.driver;

import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;

/** Managed request owner. Return/failure must follow complete helper draining;
 * completed depth/PV and all admitted request work remain separate facts. */
public interface IterativeSearch extends AutoCloseable {
    SearchDriverOutcome search(SearchRequest request);
    int maxSupportedDepth();
    int activeSearchThreads();
    SearchResult lastCompletedResult();
    SearchDiagnosticsSnapshot lastDiagnostics();
    void newGame();
    @Override void close();
}
