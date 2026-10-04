package com.ohinteractive.seedv6.tools.nnue.cglhw;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArchitectureScreenTest {
    @Test void independentConfirmationBlocksNeverReuseOpeningIndicesAcrossSeeds() {
        var seen=new java.util.HashSet<Integer>();
        for(int seed=0;seed<32;seed++)for(int pair=0;pair<2;pair++) {
            assertTrue(seen.add(ArchitectureScreen.openingIndex(true,seed,2,pair)));
            assertEquals(pair,ArchitectureScreen.openingIndex(false,seed,2,pair));
        }
        assertEquals(64,seen.size());
    }
}
