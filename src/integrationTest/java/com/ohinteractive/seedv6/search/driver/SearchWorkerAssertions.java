package com.ohinteractive.seedv6.search.driver;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import com.ohinteractive.seedv6.search.exact.ExactSearch;
import com.ohinteractive.seedv6.search.exact.ParallelSearch;
import static org.junit.jupiter.api.Assertions.*;

/** Test-only inspection of actual worker capacity, without production instrumentation. */
public final class SearchWorkerAssertions {
    public static void assertCapacity(Object owner, int count) {
        SearchDriver driver = owner instanceof SearchDriver d ? d : capturedDriver(owner);
        Object facility = field(driver, "exact");
        if (count == 1) {
            assertInstanceOf(ExactSearchAdapter.class, facility);
            assertInstanceOf(ExactSearch.class, field(facility, "exact"));
        } else {
            assertInstanceOf(ParallelSearch.class, facility);
            Object[] workers = (Object[]) field(facility, "workers");
            assertEquals(count, workers.length, "Total capacity includes the owner");
            assertEquals(count - 1, ((Object[]) field(facility, "futures")).length);
            assertEquals(count - 1, ((ThreadPoolExecutor) field(facility, "pool")).getMaximumPoolSize());
            var searches = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
            for (Object worker : workers) {
                Object search = field(worker, "search");
                assertInstanceOf(ExactSearch.class, search);
                assertTrue(searches.add(search), "Workers must own separate recursive search state");
            }
        }
    }

    private static SearchDriver capturedDriver(Object owner) {
        for (Field field : owner.getClass().getDeclaredFields()) {
            if (field.getType() == SearchDriver.class) return (SearchDriver) field(owner, field.getName());
        }
        throw new AssertionError("No production driver captured by " + owner.getClass());
    }

    private static Object field(Object owner, String name) {
        try {
            Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }

    private SearchWorkerAssertions() {}
}
