package com.ohinteractive.seedv6.uci;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Codec;
import com.ohinteractive.seedv6.search.driver.ProductionSearch;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class StandaloneBoundaryTest {
    @TempDir Path temporary;

    @Test void runtimeContainsOnlyUciAndCore() {
        for (String type : new String[] {"com.ohinteractive.seedv6.Main",
                "com.ohinteractive.seedv6.gui.SwingLauncher", "com.ohinteractive.seedv6.uci.UciEvaluation",
                "com.ohinteractive.seedv6.training.checkpoint.CheckpointStore",
                "com.ohinteractive.seedv6.search.flat.FlatNegamax"})
            assertThrows(ClassNotFoundException.class, () -> Class.forName(type, false, getClass().getClassLoader()));
        assertNotEquals(UciEngine.class.getProtectionDomain().getCodeSource().getLocation(),
                Board.class.getProtectionDomain().getCodeSource().getLocation());
    }

    @Test void missingTruncatedOversizedAndDifferentModelNeverFallBack() throws Exception {
        assertThrows(java.io.IOException.class, () -> ProductionModel.load(temporary.resolve("missing")));
        for (int size : new int[] {0, BrnPair2Codec.MODEL_BYTES - 1, BrnPair2Codec.MODEL_BYTES,
                BrnPair2Codec.MODEL_BYTES + 1}) {
            Path file = temporary.resolve("bad-" + size);
            Files.write(file, new byte[size]);
            assertThrows(java.io.IOException.class, () -> ProductionModel.load(file));
        }
    }

    @Test void invalidPositionIsAtomicBeforeOptimizedMoveGeneration() {
        var session = new UciSession();
        session.setPosition("position startpos moves e2e4".split(" "));
        var before = session.boardSnapshot();
        int history = session.historySnapshot().size();
        for (String fen : new String[] {
                "8/8/8/8/8/8/8/8 w - - 0 1", // no kings
                "4k3/8/8/8/8/8/4K3/4K3 w - - 0 1", // two white kings
                "4k3/8/8/8/8/8/8/P3K3 w - - 0 1", // pawn on back rank
                "4k3/8/8/8/8/8/4Q3/4K3 w - - 0 1", // previous mover in check
                "4k3/8/8/8/8/8/8/4K3 w K - 0 1", // absent rook
                "4k3/8/8/4P3/8/8/8/4K3 w - d6 0 1", // absent double push
                "4k3/8/8/8/8/8/4K3/8 w - - -1 1"}) {
            assertThrows(IllegalArgumentException.class, () -> session.setPosition(("position fen " + fen).split(" ")), fen);
            assertArrayEquals(before, session.boardSnapshot());
            assertEquals(history, session.historySnapshot().size());
        }
    }

    @Test void applicationHashBoundsAreCheckedBeforeAllocation() {
        for (int hash : new int[] {-1, 0, 1025, Integer.MAX_VALUE})
            assertThrows(IllegalArgumentException.class,
                    () -> ProductionSearch.create(1, SearchEvaluation.handcrafted(), hash));
    }

    @Test void resourceResolutionIgnoresClientWorkingDirectory() throws Exception {
        String original = System.getProperty("user.dir");
        Path selected = ProductionModel.resolve(null);
        try {
            System.setProperty("user.dir", temporary.toString());
            assertEquals(selected, ProductionModel.resolve(null));
            assertEquals(temporary.resolve("explicit"), ProductionModel.resolve(temporary.resolve("explicit").toString()));
        } finally { System.setProperty("user.dir", original); }
    }

    @Test void pinnedProductionProfileReportsPolicy4AndCompletesAfterWorkerAndHashSelection() throws Exception {
        var profile = ProductionUciConfiguration.load(Path.of("../build-assets/production/bre-pair2-g32.brn-pair2"));
        for(int workers : new int[]{1,4,16}) {
            String identity = profile.identity(workers);
            assertTrue(identity.contains("sha256=" + ProductionModel.SHA256));
            assertTrue(identity.contains("hints=completed_same_key"));
            assertTrue(identity.contains("history=" + (workers == 1 ? "invocation" : "root_seeded_private")));
            assertTrue(identity.contains("learning=all_searched"));
            try(var service = profile.service(workers, 1)) {
                service.setBookEnabled(false);
                var done = new java.util.concurrent.CompletableFuture<com.ohinteractive.seedv6.search.manage.ManagedSearchResult>();
                var board = Board.startingPosition();
                service.start(board, com.ohinteractive.seedv6.rules.GameHistory.initial(board),
                        new com.ohinteractive.seedv6.search.manage.SearchLimits(3,-1,-1,false), done::complete);
                var result = done.get(20, java.util.concurrent.TimeUnit.SECONDS);
                assertNull(result.failure());
                assertEquals(com.ohinteractive.seedv6.search.common.SearchTermination.COMPLETED, result.termination());
                assertEquals(3, result.lastCompletedResult().depth());
                assertTrue(result.lastCompletedResult().hasMove());
            }
        }
    }
}
