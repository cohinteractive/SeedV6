package com.ohinteractive.seedv6.tools.perft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.BoardMoveType;

class PerftPositionLibraryTest {

    private static final List<ExpectedPosition> ORIGINAL_POSITIONS = List.of(
        expected(
            "initial-position",
            "1. Initial position",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            6,
            119060324L
        ),
        expected(
            "kiwipete",
            "2. Kiwipete",
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            6,
            8031647685L
        ),
        expected(
            "rook-and-pawn-endgame",
            "3.",
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
            7,
            178633661L
        ),
        expected(
            "promotion-and-castling-tactics",
            "4.",
            "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
            6,
            706045033L
        ),
        expected(
            "knight-check-and-castling",
            "5.",
            "rnbqkb1r/pp1p1ppp/2p5/4P3/2B5/8/PPP1NnPP/RNBQK2R w KQkq - 0 6",
            3,
            53392L
        ),
        expected(
            "complex-middlegame",
            "6.",
            "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10",
            6,
            6923051137L
        ),
        expected(
            "illegal-en-passant-discovered-check",
            "7.",
            "8/5bk1/8/2Pp4/8/1K6/8/8 w - d6 0 1",
            6,
            824064L
        ),
        expected(
            "en-passant-capture-gives-check",
            "8. En passant capture gives check",
            "8/8/1k6/2b5/2pP4/8/5K2/8 b - d3 0 1",
            6,
            1440467L
        ),
        expected(
            "short-castling-gives-check",
            "9. Short castling gives check",
            "5k2/8/8/8/8/8/8/4K2R w K - 0 1",
            6,
            661072L
        ),
        expected(
            "long-castling-gives-check",
            "10. Long castling gives check",
            "3k4/8/8/8/8/8/8/R3K3 w Q - 0 1",
            6,
            803711L
        ),
        expected(
            "castling",
            "11. Castling",
            "r3k2r/1b4bq/8/8/8/8/7B/R3K2R w KQkq - 0 1",
            4,
            1274206L
        ),
        expected(
            "castling-prevented",
            "12. Castling prevented",
            "r3k2r/8/3Q4/8/8/5q2/8/R3K2R b KQkq - 0 1",
            4,
            1720476L
        ),
        expected(
            "promote-out-of-check",
            "13. Promote out of check",
            "2K2r2/4P3/8/8/8/8/8/3k4 w - - 0 1",
            6,
            3821001L
        ),
        expected(
            "discovered-check",
            "14. Discovered check",
            "8/8/1P2K3/8/2n5/1q6/8/5k2 b - - 0 1",
            5,
            1004658L
        ),
        expected(
            "promotion-gives-check",
            "15. Promotion gives check",
            "4k3/1P6/8/8/8/8/K7/8 w - - 0 1",
            6,
            217342L
        ),
        expected(
            "underpromotion-gives-check",
            "16. Underpromotion gives check",
            "8/P1k5/K7/8/8/8/8/8 w - - 0 1",
            6,
            92683L
        ),
        expected(
            "self-stalemate",
            "17. Self stalemate",
            "K1k5/8/P7/8/8/8/8/8 w - - 0 1",
            6,
            2217L
        ),
        expected(
            "stalemate-checkmate",
            "18. Stalemate/Checkmate",
            "8/k1P5/8/1K6/8/8/8/8 w - - 0 1",
            7,
            567584L
        ),
        expected(
            "double-check",
            "19. Double check",
            "8/8/2k5/5q2/5n2/8/5K2/8 b - - 0 1",
            4,
            23527L
        ),
        expected(
            "new-position",
            "20. New position",
            "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
            5,
            89941194L
        )
    );

    @Test
    void defaultLibraryPreservesOriginalPositionDataAndOrder() {
        List<PerftPosition> positions = PerftPositions.DEFAULT.positions();

        assertEquals(ORIGINAL_POSITIONS.size(), positions.size());
        for(int i = 0; i < ORIGINAL_POSITIONS.size(); i ++) {
            ExpectedPosition expected = ORIGINAL_POSITIONS.get(i);
            PerftPosition actual = positions.get(i);
            assertEquals(expected.id(), actual.id());
            assertEquals(expected.name(), actual.name());
            assertEquals(expected.fen(), actual.fen());
            assertEquals(expected.depth(), actual.defaultDepth());
            assertEquals(
                List.of(new PerftExpectation(expected.depth(), expected.nodes())),
                actual.expectations()
            );
            assertEquals(actual.defaultExpectation(), actual.expectation(expected.depth()));
        }
    }

    @Test
    void selectorsProduceOrderedDefaultCasesUsingOneBasedNumbersAndStableIds() {
        PerftPositionLibrary library = PerftPositions.DEFAULT;
        PerftSuite all = library.all();
        PerftSuite numeric = library.select(5);
        PerftSuite byId = library.select("knight-check-and-castling");
        PerftSuite range = library.range(1, 5);

        assertEquals(20, all.cases().size());
        assertEquals(library.positions(), all.cases().stream().map(PerftCase::position).toList());
        assertEquals(List.of(numeric.cases().get(0)), byId.cases());
        assertEquals("knight-check-and-castling", numeric.cases().get(0).position().id());
        assertEquals(library.positions().subList(0, 5), range.cases().stream().map(PerftCase::position).toList());
        assertEquals(53392L, numeric.cases().get(0).expectedNodes());
    }

    @Test
    void selectorsRejectInvalidNumbersIdsRangesAndDuplicateLibraryIds() {
        PerftPositionLibrary library = PerftPositions.DEFAULT;

        assertThrows(IllegalArgumentException.class, () -> library.select(0));
        assertThrows(IllegalArgumentException.class, () -> library.select(21));
        assertThrows(IllegalArgumentException.class, () -> library.select("unknown-position"));
        assertThrows(IllegalArgumentException.class, () -> library.select(" "));
        assertThrows(IllegalArgumentException.class, () -> library.range(0, 1));
        assertThrows(IllegalArgumentException.class, () -> library.range(1, 21));
        assertThrows(IllegalArgumentException.class, () -> library.range(5, 4));

        PerftPosition duplicate = library.positions().get(0);
        PerftPositionLibrary duplicateIds = new PerftPositionLibrary() {
            @Override
            public String id() {
                return "duplicates";
            }

            @Override
            public String name() {
                return "Duplicate positions";
            }

            @Override
            public List<PerftPosition> positions() {
                return List.of(duplicate, duplicate);
            }
        };
        assertThrows(IllegalStateException.class, duplicateIds::all);
    }

    @Test
    void valuesValidateExpectationsCasesSuitesAndImmutableCollections() {
        PerftPosition position = PerftPositions.DEFAULT.positions().get(0);
        PerftCase perftCase = PerftPositions.DEFAULT.select(1).cases().get(0);

        assertThrows(UnsupportedOperationException.class, () -> PerftPositions.DEFAULT.positions().add(position));
        assertThrows(UnsupportedOperationException.class, () -> position.expectations().add(position.defaultExpectation()));
        assertThrows(UnsupportedOperationException.class, () -> PerftPositions.DEFAULT.all().cases().add(perftCase));
        assertThrows(IllegalArgumentException.class, () -> new PerftExpectation(0, 1L));
        assertThrows(IllegalArgumentException.class, () -> new PerftExpectation(1, -1L));
        assertThrows(
            IllegalArgumentException.class,
            () -> new PerftPosition(
                "duplicate-depths",
                "Duplicate depths",
                position.fen(),
                List.of(new PerftExpectation(1, 1L), new PerftExpectation(1, 1L)),
                1
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new PerftPosition(
                "missing-default",
                "Missing default",
                position.fen(),
                List.of(new PerftExpectation(2, 1L)),
                1
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new PerftCase(position, position.defaultDepth(), position.defaultExpectation().expectedNodes() + 1L)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new PerftSuite("duplicates", "Duplicates", List.of(perftCase, perftCase))
        );

        assertSame(
            PerftPositions.DEFAULT.positions(),
            new DefaultPerftPositionLibrary().positions()
        );
    }

    @Test
    void allTraversalImplementationsMatchAConfiguredExpectationSeriallyAndConcurrently() {
        PerftCase perftCase = PerftPositions.DEFAULT.select("knight-check-and-castling").cases().get(0);

        for(Perft.Implementation implementation : Perft.Implementation.values()) {
            for(Perft.Concurrency concurrency : Perft.Concurrency.values()) {
                long[] board = implementation == Perft.MOVE_TYPE_RECURSIVE || implementation == Perft.MOVE_TYPE_FLAT
                    ? BoardMoveType.fromFen(perftCase.position().fen())
                    : Board.fromFen(perftCase.position().fen());
                assertEquals(
                    perftCase.expectedNodes(),
                    Perft.run(board, perftCase.depth(), implementation, concurrency, 2),
                    implementation + " " + concurrency
                );
            }
        }
    }

    private static ExpectedPosition expected(
        String id,
        String name,
        String fen,
        int depth,
        long nodes
    ) {
        return new ExpectedPosition(id, name, fen, depth, nodes);
    }

    private record ExpectedPosition(String id, String name, String fen, int depth, long nodes) {}
}
