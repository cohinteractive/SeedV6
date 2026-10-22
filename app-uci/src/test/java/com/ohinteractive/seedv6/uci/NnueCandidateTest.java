package com.ohinteractive.seedv6.uci;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NnueCandidateTest {
    @Test void packagedWeightsMatchIndependentPythonAcrossAllEightHeads() throws Exception {
        var model = NnueCandidateUciMain.load(null);
        assertEquals("5da0253642ab33b8e446b5b1c5764750be008cd3dbacc7cd1992a95bf4a881f6", model.payloadSha256());
        assertEquals(8, model.outputBuckets());
        var heads = new HashSet<Integer>();
        int count = 0;
        try (var input = getClass().getResourceAsStream("/conventional-cp64-parity.tsv")) {
            for (String line : new String(input.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (line.startsWith("#") || line.isBlank()) continue;
                var fields = line.split("\t");
                int expected = Integer.parseInt(fields[0]);
                var board = Board.fromFen(fields[1]);
                heads.add((Long.bitCount(board[0] | board[1] | board[2]) - 2) / 4);
                var accumulator = model.newAccumulator();
                accumulator.rebuild(board);
                assertEquals(expected, accumulator.evaluate(board), fields[1]);
                assertEquals(expected, model.fullRebuild(board), fields[1]);
                count++;
            }
        }
        assertEquals(259, count);
        assertEquals(8, heads.size());
    }

    @Test void actualServicePreservesE008CompositionAfterResourceReconstruction() throws Exception {
        var config = NnueCandidateUciMain.configuration(new String[0]);
        assertEquals(1, config.maximumWorkers());
        assertFalse(config.announcesResourceChanges());
        assertFalse(config.acceptsWorkers(2));
        assertThrows(IllegalArgumentException.class, () -> config.service(2, 1));
        assertThrows(IllegalArgumentException.class, () -> config.service(1, 0));
        for (int hash : new int[]{1, 32, 256}) try (var service = config.service(1, hash)) {
            var exact = field(field(field(service, "search"), "exact"), "exact");
            assertEquals(SeePolicy.QXRAY, field(exact, "seePolicy"));
            assertEquals(ExactSearch.LMR_QUIET, field(exact, "lmrPolicy"));
            assertEquals(ExactSearch.LMP_WINDOW, field(exact, "lmpPolicy"));
            assertEquals(IterationReuse.EFFORT, field(field(exact, "iterationReuse"), "policy"));
            assertEquals(true, field(exact, "completedHints"));
            assertEquals(false, field(exact, "reverseFutility"));
            assertEquals(false, field(exact, "nullMovePruning"));
            assertEquals(FutilityPolicy.OFF, field(exact, "futilityPolicy"));
            assertEquals(NullMovePolicy.OFF, field(exact, "pairNull"));
            assertEquals(ExactSearch.CHECK_OFF, field(exact, "checkPolicy"));
            assertNull(field(exact, "construction"));
            assertNull(field(exact, "flatMainFrames"));
            assertNull(field(exact, "flatQFrames"));
            assertNull(field(exact, "historyLearning"));
            assertTrue(config.identity(1, hash).contains("hashMiB=" + hash));
        }
    }

    @Test void candidateSearchMatchesIndependentRebuildAtSelectiveDepth() throws Exception {
        var model = NnueCandidateUciMain.load(null);
        String[] fens = {Board.FEN_STARTING_POSITION,
            "r1bqkbnr/1ppp1ppp/p1n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 0 4",
            "r3k2r/pppq1ppp/2n1bn2/3pp3/3PP3/2N1BN2/PPPQ1PPP/R3K2R w KQkq - 0 1",
            "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
            "7k/P7/8/8/8/8/8/7K w - - 0 1"};
        boolean selective = false;
        for (String fen : fens) {
            var board = Board.fromFen(fen);
            var request = new SearchRequest(board, GameHistory.initial(board), 5,
                    SearchObserver.NONE, SearchControl.unlimited(), true);
            try (var fast = NnueCandidateUciMain.search(SearchEvaluation.conventional(model), 4);
                 var oracle = NnueCandidateUciMain.search(SearchEvaluation.conventionalFullRebuild(model), 4)) {
                var a = fast.search(request); var b = oracle.search(request);
                assertTrue(a.completed()); assertTrue(b.completed());
                assertEquals(a.score(), b.score(), fen);
                assertEquals(a.bestMove(), b.bestMove(), fen);
                assertEquals(a.nodes(), b.nodes(), fen);
                assertEquals(a.diagnostics().worker().nodes().qNodes(), b.diagnostics().worker().nodes().qNodes(), fen);
                assertArrayEquals(a.principalVariation(), b.principalVariation(), fen);
                assertEquals(a.selective(), b.selective());
                selective |= a.selective();
            }
        }
        assertTrue(selective, "The oracle comparisons must exercise selective search");
    }

    @Test void missingCorruptTruncatedExtraAndDifferentAssetsCannotFallBack() throws Exception {
        assertThrows(java.io.IOException.class, () -> NnueCandidateUciMain.load(Path.of("missing-candidate-network")));
        assertThrows(java.io.IOException.class, () -> NnueCandidateUciMain.readPinned(null));
        byte[] bytes;
        try (var input = getClass().getResourceAsStream(NnueCandidateUciMain.NETWORK)) { bytes = input.readAllBytes(); }
        for (int size : new int[]{0, bytes.length - 1, bytes.length + 1}) {
            var changed = java.util.Arrays.copyOf(bytes, size);
            assertThrows(java.io.IOException.class, () -> NnueCandidateUciMain.readPinned(new ByteArrayInputStream(changed)));
        }
        for (int index : new int[]{7, 72, bytes.length - 1}) {
            var changed = bytes.clone(); changed[index] ^= 1;
            assertThrows(java.io.IOException.class, () -> NnueCandidateUciMain.readPinned(new ByteArrayInputStream(changed)));
        }
        assertThrows(IllegalArgumentException.class, () -> NnueCandidateUciMain.configuration(new String[]{"--nnue", "anything"}));
    }

    private static Object field(Object object, String name) throws Exception {
        var field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
}
