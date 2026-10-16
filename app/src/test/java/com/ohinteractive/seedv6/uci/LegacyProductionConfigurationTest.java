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
