package com.ohinteractive.seedv6.search.tablebase;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SyzygyResourcesTest {
    @Test void tableIntegrityManifestIsAvailableWithoutApplicationResources() throws Exception {
        var hashes = new Properties();
        try (var input = SyzygyNative.class.getResourceAsStream("small-tables.sha256")) {
            assertNotNull(input, "Core must own its runtime tablebase integrity manifest");
            hashes.load(input);
        }
        assertEquals(70, hashes.size());
        for (String name : hashes.stringPropertyNames()) {
            assertTrue(name.matches("[KQRBNPv]+\\.rtb[zw]"), name);
            assertTrue(hashes.getProperty(name).matches("[0-9a-f]{64}"), name);
        }
    }
}
