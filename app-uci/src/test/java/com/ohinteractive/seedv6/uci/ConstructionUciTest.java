package com.ohinteractive.seedv6.uci;

import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.search.exact.FlatSearchConfig;
import static org.junit.jupiter.api.Assertions.*;

class ConstructionUciTest {
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
