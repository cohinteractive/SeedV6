package com.ohinteractive.seedv6.uci;

import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;

/** Application composition, not an evaluator registry. Legacy store/research selection stays in app. */
public interface UciConfiguration {
    SearchLifecycleService service(int workers, int hashMiB);
    String identity(int workers);
    default String name() { return "SeedV6"; }
    default boolean standalone() { return false; }
    default boolean acceptsWorkers(int workers) { return true; }
}
