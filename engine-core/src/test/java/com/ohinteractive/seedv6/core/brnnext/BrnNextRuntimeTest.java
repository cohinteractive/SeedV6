package com.ohinteractive.seedv6.core.brnnext;

import java.io.*;
import java.util.*;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.tt.TTable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class BrnNextRuntimeTest {
    @ParameterizedTest @EnumSource(BrnNextArchitecture.class)
    void runtimeOnlyCodecWorkerIsolationSpecialMovesAndSearch(BrnNextArchitecture kind) throws Exception {
        var values = new double[kind.parameters()]; var random = new SplittableRandom(242);
        for (int i = 0; i < values.length; i++) values[i] = random.nextDouble(-.1, .1);
        var model = new BrnNextModel(kind, values); double owned = model.weight(0); values[0] = 99;
        assertEquals(owned, model.weight(0));
        var encoded = new ByteArrayOutputStream(); BrnNextCodec.writeModel(model, encoded);
        var decoded = BrnNextCodec.readModel(kind, new ByteArrayInputStream(encoded.toByteArray()));
        var definition = SearchEvaluation.brnNext(decoded);
        var state = definition.newState(64); var other = definition.newState(64);
        for (String fen : new String[]{
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "r3k2r/8/8/3pP3/8/8/8/R3K2R w KQkq d6 0 1",
                "4k3/P7/8/8/8/8/7p/4K3 w - - 0 1"}) {
            long[] board = Board.fromFen(fen); state.initialize(board, 0);
            int score = state.evaluate(board, 0);
            var moves = new long[512]; var scratch = new long[Board.MAX_BITBOARDS];
            int n = Gen.genAll(board[0], board[1], board[2], board[3], (int) board[4], board[5], true, moves, scratch);
            for (int i = 0; i < n; i++) {
                var child = new long[Board.MAX_BITBOARDS];
                Board.makeMoveInto(board[0], board[1], board[2], board[3], (int) board[4], board[5], moves[i], child);
                state.child(board, child, 0); other.initializeFrom(child, 1, state);
                int cold = model.score(child, model.newWorkspace());
                assertEquals(cold, state.evaluate(child, 1)); assertEquals(cold, other.evaluate(child, 1));
                assertEquals(score, state.evaluate(board, 0), "undo/revisit must not drift");
            }
            // Explicit common static engine: no evaluator-dependent production policies.
            try (var search = new SearchDriver(new ExactSearchAdapter(definition, new TTable(1)))) {
                var result = search.search(new SearchRequest(board, 2));
                assertTrue(result.targetDepthCompleted());
                long chosen = result.lastCompletedResult().principalVariation()[0];
                assertTrue(Arrays.stream(moves, 0, n).anyMatch(m -> m == chosen));
                search.newGame(); var repeated = search.search(new SearchRequest(board, 2));
                assertEquals(result.lastCompletedResult().score(), repeated.lastCompletedResult().score());
                assertArrayEquals(result.lastCompletedResult().principalVariation(), repeated.lastCompletedResult().principalVariation());
            }
        }
        var different = SearchEvaluation.brnNext(model).newState(64);
        assertThrows(IllegalArgumentException.class, () -> different.initializeFrom(Board.startingPosition(), 0, state));
    }
}
