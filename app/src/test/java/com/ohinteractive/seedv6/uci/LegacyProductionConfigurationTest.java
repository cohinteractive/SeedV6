package com.ohinteractive.seedv6.uci;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;

class LegacyProductionConfigurationTest {
    @Test void defaultEvaluatorRemainsHce() {
        var selected = LegacyUciEvaluation.load(new Properties());
        assertSame(SearchEvaluation.handcrafted(), selected.evaluation());
        assertEquals("", selected.identity(1));
    }

    @Test void clockDefaultAndFallbackPreserveEveryExistingEvaluatorAndInstanceIsolation() throws Exception {
        var properties = new Properties();
        var defaultProfile = LegacyUciEvaluation.load(properties);
        assertEquals(com.ohinteractive.seedv6.search.manage.ClockPolicy.BOUNDARY2, defaultProfile.clockPolicy());
        properties.setProperty(com.ohinteractive.seedv6.search.manage.ClockPolicy.PROPERTY, "FACTUAL");
        var fallback = LegacyUciEvaluation.load(properties);
        assertEquals(330, fallback.clockBudgetMillis(7959, 100, 30));
        assertEquals(660, defaultProfile.clockBudgetMillis(7959, 100, 30));
        for (var evaluator : com.ohinteractive.seedv6.search.driver.BaselineEngine.Evaluator.values()) {
            var evaluation = com.ohinteractive.seedv6.search.driver.BaselineEngine.load(evaluator).evaluation();
            for (var clocks : com.ohinteractive.seedv6.search.manage.ClockPolicy.values()) {
                var profile = new LegacyUciEvaluation(evaluation, "evaluator-fixture", clocks);
                assertSame(evaluation, profile.evaluation());
                for (int workers : new int[]{1,4,16}) try (var service = profile.service(workers,64)) {
                    var searchField = service.getClass().getDeclaredField("search"); searchField.setAccessible(true);
                    var driver = searchField.get(service); var exact = driver.getClass().getDeclaredField("exact"); exact.setAccessible(true);
                    assertEquals(clocks == com.ohinteractive.seedv6.search.manage.ClockPolicy.BOUNDARY2,
                            exact.get(driver) instanceof com.ohinteractive.seedv6.search.manage.Boundary2ClockPolicy);
                    assertTrue(profile.identity(workers).contains("clockPolicy=" + clocks.identity()));
                }
            }
        }
    }

    @Test void everyAmbientResearchSelectorIsRejectedEvenWhenItNamesAControl() {
        for (String option : new String[] {"qsearch", "lmr", "aspiration", "lmp", "check", "reuse",
                "nmp", "internal", "concurrency", "see", "futility", "futurePolicy"}) {
            for (String value : new String[] {"off", "production", "shallow", "qxray"}) {
                var properties = new Properties();
                properties.setProperty("seedv6.uci." + option + "Research", value);
                assertTrue(assertThrows(IllegalArgumentException.class,
                        () -> LegacyUciEvaluation.load(properties)).getMessage().contains("not a production option"));
            }
        }
    }

    @Test void invalidExplicitModelCannotFallBackToHce() {
        var properties = new Properties();
        properties.setProperty("seedv6.uci.pair2Store", "missing");
        assertThrows(IllegalArgumentException.class, () -> LegacyUciEvaluation.load(properties));
    }
}
