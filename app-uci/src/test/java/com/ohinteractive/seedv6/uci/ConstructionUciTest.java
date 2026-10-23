package com.ohinteractive.seedv6.uci;

import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.search.exact.FlatSearchConfig;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionUciTest {
    @Test void standardBaselineInstantiatesAdaptiveAtEverySupportedWorkerCount() throws Exception {
        var configuration = BaselineUciConfiguration.load();
        assertTrue(configuration.identity(1).contains("hashMiB=64;aspiration=ADAPTIVE16"));
        for (int workers = 1; workers <= 16; workers++) try (var service = configuration.service(workers, 1)) {
            assertTrue(configuration.acceptsWorkers(workers));
            var aspiration = field(field(service, "search"), "exact");
            assertEquals(com.ohinteractive.seedv6.search.driver.AspirationSearch.Policy.ADAPTIVE16,
                    field(aspiration, "policy"));
            var owner = field(aspiration, "owner");
            var kernel = workers == 1 ? field(owner, "exact") : owner;
            assertEquals(3407871L, ((FlatSearchConfig) field(kernel, "construction")).flags());
            assertTrue(configuration.identity(workers, 1).contains("flags=3407871"));
            assertTrue(configuration.identity(workers, 1).contains("workers=" + workers + ";hashMiB=1;aspiration=ADAPTIVE16"));
        }
    }
    @Test void actualUciServiceUsesRichCompositionAfterHashAndThreadsReconstruction() throws Exception {
        var configuration = ConstructionUciMain.configuration(new String[0]);
        for (int workers : new int[]{1, 4, 16}) try (var service = configuration.service(workers, 1)) {
            var search = field(field(service, "search"), "exact");
            Object owner = workers == 1 ? field(search, "exact") : search;
            assertEquals(262143L, ((FlatSearchConfig) field(owner, "construction")).flags());
            assertTrue(configuration.identity(workers, 1).contains("hashMiB=1;aspiration=OFF"));
        }
        assertTrue(configuration.name().contains("construction-v1 experimental"));
        assertThrows(IllegalArgumentException.class, () -> ConstructionUciMain.configuration(new String[]{"--aspiration", "typo"}));
        assertThrows(java.io.IOException.class, () -> ConstructionUciMain.configuration(new String[]{"--model", "missing-model"}));
    }
    private static Object field(Object object, String name) throws Exception {
        var field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
}
