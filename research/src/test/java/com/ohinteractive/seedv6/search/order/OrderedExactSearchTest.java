package com.ohinteractive.seedv6.search.order;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.order.OrderedExactSearchHarness.Result;

class OrderedExactSearchTest {

    @Test
    void orderingOnOffPreservesExactScoresTerminalMeaningAndTraversalSemantics() {
        final Fixture[] fixtures = {
            new Fixture(Board.FEN_STARTING_POSITION, 2),
            new Fixture(
                "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
                2
            ),
            new Fixture("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1", 3),
            new Fixture("4k2r/6P1/8/8/8/8/8/4K3 w - - 0 1", 2),
            new Fixture("4r1k1/8/8/8/1b6/8/8/4K3 w - - 0 1", 2)
        };

        boolean observedDifferentTraversalOrder = false;
        for(Fixture fixture : fixtures) {
            final long[] board = Board.fromFen(fixture.fen);
            final long[] before = board.clone();
            final Result baseline = OrderedExactSearchHarness.search(
                board, fixture.depth, false
            );
            final Result ordered = OrderedExactSearchHarness.search(
                board, fixture.depth, true
            );

            assertArrayEquals(before, board, fixture.fen);
            assertEquals(baseline.score(), ordered.score(), fixture.fen);
            assertEquals(baseline.hasMove(), ordered.hasMove(), fixture.fen);
            assertEquals(baseline.legalRootMoves(), ordered.legalRootMoves(), fixture.fen);
            assertEquals(baseline.edges(), ordered.edges(), fixture.fen);
            assertEquals(baseline.checkmates(), ordered.checkmates(), fixture.fen);
            assertEquals(baseline.stalemates(), ordered.stalemates(), fixture.fen);
            assertEquals(baseline.ruleDraws(), ordered.ruleDraws(), fixture.fen);
            assertEquals(baseline.frontiers(), ordered.frontiers(), fixture.fen);
            assertEquals(
                baseline.semanticChecksum(), ordered.semanticChecksum(), fixture.fen
            );
            observedDifferentTraversalOrder |= baseline.orderFingerprint()
                != ordered.orderFingerprint();

            if(baseline.bestMove() != ordered.bestMove()) {
                assertEquals(
                    baseline.score(),
                    OrderedExactSearchHarness.rootMoveScore(
                        board, fixture.depth, baseline.bestMove()
                    ),
                    "baseline root tie " + fixture.fen
                );
                assertEquals(
                    ordered.score(),
                    OrderedExactSearchHarness.rootMoveScore(
                        board, fixture.depth, ordered.bestMove()
                    ),
                    "ordered root tie " + fixture.fen
                );
            }
        }
        assertNotEquals(false, observedDifferentTraversalOrder);
    }

    @Test
    void orderingEnabledExactTraversalIsSequentiallyDeterministic() {
        final long[] board = Board.fromFen(
            "4k2r/6P1/8/8/8/8/8/4K3 w - - 0 1"
        );
        final Result first = OrderedExactSearchHarness.search(board, 2, true);
        final Result second = OrderedExactSearchHarness.search(board, 2, true);
        assertEquals(first, second);
    }

    private record Fixture(String fen, int depth) {}
}
