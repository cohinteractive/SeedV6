package com.ohinteractive.seedv6.uci;

import java.io.IOException;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.search.driver.BaselineEngine;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;

/** Thin protocol adapter; GUI, Arena and UCI share the engine-core baseline. */
final class BaselineUciConfiguration {
    static UciConfiguration load() throws IOException {
        return bind(BaselineEngine.load());
    }
    private static UciConfiguration bind(BaselineEngine binding) {
        return new UciConfiguration() {
            public com.ohinteractive.seedv6.search.manage.ClockPolicy clockPolicy() { return binding.clockPolicy(); }
            public UciConfiguration withClockPolicy(com.ohinteractive.seedv6.search.manage.ClockPolicy policy) {
                return bind(binding.withClockPolicy(policy));
            }
            public SearchLifecycleService service(int workers, int hashMiB) { return binding.service(workers, hashMiB); }
            public boolean standalone() { return true; }
            public boolean acceptsWorkers(int workers) { return workers >= 1 && workers <= 16; }
            public boolean announcesResourceChanges() { return false; }
            public String name() { return "SeedV6 Construction V1 NNUE CP64 " + ApplicationVersion.load().displayVersion(); }
            public String identity(int workers) { return identity(workers, 64); }
            public String identity(int workers, int hashMiB) { return binding.identity(workers, hashMiB); }
        };
    }
    private BaselineUciConfiguration() {}
}
