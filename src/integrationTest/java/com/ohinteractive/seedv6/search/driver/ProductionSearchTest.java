package com.ohinteractive.seedv6.search.driver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.search.driver.SearchWorkerAssertions.assertCapacity;

class ProductionSearchTest {
    @ParameterizedTest @ValueSource(ints = {1, 2, 4, 16})
    void canonicalConstructionCountsTheOwnerWithinTheLimit(int workers) {
        try (var driver = new SearchDriver(ProductionSearch.create(workers, SearchEvaluation.handcrafted()))) {
            assertCapacity(driver, workers);
        }
    }

    @Test void invalidLimitsAndMissingEvaluatorAreRejected() {
        for (int workers : new int[] {0, 17})
            assertThrows(IllegalArgumentException.class, () -> ProductionSearch.create(workers, SearchEvaluation.handcrafted()));
        assertThrows(NullPointerException.class, () -> ProductionSearch.create(1, null));
        assertThrows(NullPointerException.class, () -> ProductionSearch.create(4, null));
    }
}
