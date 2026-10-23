package com.ohinteractive.seedv6.search.driver;

import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnpair2.ProductionModel;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.exact.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(60)
class PinnedSearchTest {
    @TempDir Path temporary;
    @Test void actualCompositionIsRichFlatPvsForEverySupportedWorkerAndRetainsProduction() throws Exception {
        var binding = PinnedSearch.load(PinnedSearch.Policy.CONSTRUCTION_V1);
        assertTrue(binding.evaluation().isBrnPair2());
        for (int workers = 1; workers <= 16; workers++) {
            try (var search = binding.create(workers, 1)) {
                Object owner = workers == 1 ? field(search, "exact") : search;
                var config = (FlatSearchConfig) field(owner, "construction");
                assertEquals(262143L, config.flags());
                var original = java.util.EnumSet.range(FlatSearchConfig.Feature.QXRAY,
                        FlatSearchConfig.Feature.HISTORY_PROTECTION);
                for (var feature : FlatSearchConfig.Feature.values())
                    assertEquals(original.contains(feature), config.has(feature), feature.name());
                assertEquals(100, config.futilityMargin()); assertEquals(200, config.reverseMargin());
                assertTrue(config.identity().contains("extensionCap=1"));
                if (workers == 1) assertNotNull(field(owner, "flatMainFrames"));
                assertFalse(search instanceof AspirationSearch);
            }
        }
        try (var search = PinnedSearch.load(PinnedSearch.Policy.POLICY4).create(1, 1)) {
            assertNull(field(field(search, "exact"), "construction"));
        }
        assertTrue(binding.identity(4, 64).contains("aspiration=OFF;parallel=ROOT_REUSE"));
        assertTrue(binding.identity(1, 64).contains(ProductionModel.SHA256));
        assertThrows(IllegalArgumentException.class, () -> binding.create(17, 64));
        assertThrows(IllegalArgumentException.class, () -> binding.create(1, 0));
    }
    @Test void fullCandidateMatchesCanonicalFactoryAndSupportsBudgetResetReuseAndWiden16() throws Exception {
        var binding = PinnedSearch.load(PinnedSearch.Policy.CONSTRUCTION_V1);
        var board = Board.startingPosition();
        try (var actual = new SearchDriver(binding.create(1, 1));
             var expected = new SearchDriver(ConstructionSearch.v1(1, 1, null).create(binding.evaluation()))) {
            var a = actual.search(new SearchRequest(board, 4)); var b = expected.search(new SearchRequest(board, 4));
            assertTrue(a.targetDepthCompleted()); assertEquals(b.nodes(), a.nodes());
            assertEquals(b.lastCompletedResult().score(), a.lastCompletedResult().score());
            assertArrayEquals(b.lastCompletedResult().principalVariation(), a.lastCompletedResult().principalVariation());
        }
        for (int workers : new int[]{1, 4, 16}) {
            try (var driver = new SearchDriver(binding.create(workers, 1))) {
                var control = SearchControl.controlled(1234, System.nanoTime(), 30_000_000_000L, TimeSource.SYSTEM);
                var result = driver.search(new SearchRequest(board, GameHistory.initial(board), 20, SearchObserver.NONE, control, true));
                assertEquals(1234, result.nodes()); assertEquals(0, driver.activeSearchThreads());
                driver.newGame(); assertTrue(driver.search(new SearchRequest(board, 3)).targetDepthCompleted());
            }
        }
        try (var search = PinnedSearch.load(PinnedSearch.Policy.CONSTRUCTION_V1, null, AspirationSearch.Policy.WIDEN16).create(4, 1)) {
            assertInstanceOf(AspirationSearch.class, search);
        }
    }
    @Test void pinnedModelCannotBeReplacedByMissingCorruptOrOtherPayload() throws Exception {
        assertThrows(java.io.IOException.class, () -> PinnedSearch.load(PinnedSearch.Policy.CONSTRUCTION_V1, temporary.resolve("missing"), null));
        Path corrupt = temporary.resolve("model");
        byte[] bytes;
        try (var in = ProductionModel.class.getResourceAsStream("/models/bre-pair2-g32.brn-pair2")) { bytes = in.readAllBytes(); }
        assertEquals(ProductionModel.SHA256, java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)));
        bytes[bytes.length - 1] ^= 1; Files.write(corrupt, bytes);
        assertThrows(java.io.IOException.class, () -> PinnedSearch.load(PinnedSearch.Policy.CONSTRUCTION_V1, corrupt, null));
    }
    private static Object field(Object object, String name) throws Exception {
        var field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
}
