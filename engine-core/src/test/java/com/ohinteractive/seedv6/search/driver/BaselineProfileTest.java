package com.ohinteractive.seedv6.search.driver;

import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.exact.FlatSearchConfig;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;
import static org.junit.jupiter.api.Assertions.*;

class BaselineProfileTest {
    @Test void rollbackPropertyIsSnapshottedAndExplicitSelectionWins() throws Exception {
        String property = BaselineEngine.SearchProfile.PROPERTY;
        String previous = System.getProperty(property);
        try {
            System.clearProperty(property);
            var promoted = BaselineEngine.load();
            System.setProperty(property, "legacy");
            var rollback = BaselineEngine.load();
            var explicit = BaselineEngine.load(BaselineEngine.Evaluator.CONVENTIONAL_CP64,
                    ClockPolicy.BOUNDARY2, BaselineEngine.SearchProfile.HL_G1);
            for (int workers = 1; workers <= 16; workers++) {
                assertTrue(promoted.identity(workers, 1).contains("searchProfile=TC_P1"));
                assertTrue(explicit.identity(workers, 1).contains("flags=104071167"));
                assertTrue(rollback.identity(workers, 1).contains("searchProfile=LEGACY"));
                assertTrue(rollback.withClockPolicy(ClockPolicy.FACTUAL).identity(workers, 1).contains("flags=3407871"));
                assertEquals(3407871, BaselineEngine.configuration(workers, 1).mechanisms().flags());
            }
            System.setProperty(property, "HL_G1");
            // Existing bindings retain their actual kernels after a property/clock-policy change.
            for (var binding : new BaselineEngine[]{promoted, rollback, explicit}) {
                var clock = binding.create(1, 1);
                try (var driver = new SearchDriver(clock)) {
                    Object aspiration = field(clock, "owner");
                    Object kernel = field(field(aspiration, "owner"), "exact");
                    assertEquals(binding == rollback ? 3407871 : binding == promoted ? 1177812991 : 104071167,
                            ((FlatSearchConfig) field(kernel, "construction")).flags());
                    assertTrue(driver.search(new SearchRequest(Board.startingPosition(), 4)).targetDepthCompleted());
                    assertEquals(0, driver.activeSearchThreads());
                }
            }
            System.setProperty(property, "typo");
            assertThrows(IllegalArgumentException.class, BaselineEngine::load);
            assertEquals(3407871, BaselineEngine.legacyConfiguration(4, 1).mechanisms().flags());
            assertTrue(BaselineEngine.load(BaselineEngine.Evaluator.HCE).identity(4, 1).contains("flags=262143"));
            assertNotNull(BaselineEngine.load(BaselineEngine.Evaluator.CONVENTIONAL_CP64,
                    ClockPolicy.FACTUAL, BaselineEngine.SearchProfile.LEGACY));
        } finally {
            if (previous == null) System.clearProperty(property); else System.setProperty(property, previous);
        }
    }
    private static Object field(Object value, String name) throws Exception {
        var field = value.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(value);
    }
}
