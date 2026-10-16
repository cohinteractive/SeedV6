package com.ohinteractive.seedv6.integration;

import com.ohinteractive.seedv6.uci.UciConfiguration;
import com.ohinteractive.seedv6.uci.UciEngine;
import com.ohinteractive.seedv6.uci.UciEvaluation;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;

/** Explicit research composition only; never on a production runtime classpath. */
public final class ResearchUciMain {
    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("Historical research options use -Dseedv6.uci.* properties");
        var research = UciEvaluation.load(System.getProperties());
        var configuration = new UciConfiguration() {
            @Override public SearchLifecycleService service(int workers, int hashMiB) {
                return research.service(workers, hashMiB);
            }
            @Override public String identity(int workers) { return research.identity(workers); }
            @Override public boolean acceptsWorkers(int workers) { return research.acceptsWorkers(workers); }
        };
        new UciEngine(System.in, System.out, System.err, configuration).run();
    }
    private ResearchUciMain() {}
}
