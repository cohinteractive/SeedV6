package com.ohinteractive.seedv6.tools.perft;

import java.util.List;

public final class DefaultPerftPositionLibrary implements PerftPositionLibrary {

    private static final List<PerftPosition> POSITIONS = List.of(
        position(
            "initial-position",
            "1. Initial position",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            6,
            119060324L
        ),
        position(
            "kiwipete",
            "2. Kiwipete",
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            6,
            8031647685L
        ),
        position(
            "rook-and-pawn-endgame",
            "3.",
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
            7,
            178633661L
        ),
        position(
            "promotion-and-castling-tactics",
            "4.",
            "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
            6,
            706045033L
        ),
        position(
            "knight-check-and-castling",
            "5.",
            "rnbqkb1r/pp1p1ppp/2p5/4P3/2B5/8/PPP1NnPP/RNBQK2R w KQkq - 0 6",
            3,
            53392L
        ),
        position(
            "complex-middlegame",
            "6.",
            "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10",
            6,
            6923051137L
        ),
        position(
            "illegal-en-passant-discovered-check",
            "7.",
            "8/5bk1/8/2Pp4/8/1K6/8/8 w - d6 0 1",
            6,
            824064L
        ),
        position(
            "en-passant-capture-gives-check",
            "8. En passant capture gives check",
            "8/8/1k6/2b5/2pP4/8/5K2/8 b - d3 0 1",
            6,
            1440467L
        ),
        position(
            "short-castling-gives-check",
            "9. Short castling gives check",
            "5k2/8/8/8/8/8/8/4K2R w K - 0 1",
            6,
            661072L
        ),
        position(
            "long-castling-gives-check",
            "10. Long castling gives check",
            "3k4/8/8/8/8/8/8/R3K3 w Q - 0 1",
            6,
            803711L
        ),
        position(
            "castling",
            "11. Castling",
            "r3k2r/1b4bq/8/8/8/8/7B/R3K2R w KQkq - 0 1",
            4,
            1274206L
        ),
        position(
            "castling-prevented",
            "12. Castling prevented",
            "r3k2r/8/3Q4/8/8/5q2/8/R3K2R b KQkq - 0 1",
            4,
            1720476L
        ),
        position(
            "promote-out-of-check",
            "13. Promote out of check",
            "2K2r2/4P3/8/8/8/8/8/3k4 w - - 0 1",
            6,
            3821001L
        ),
        position(
            "discovered-check",
            "14. Discovered check",
            "8/8/1P2K3/8/2n5/1q6/8/5k2 b - - 0 1",
            5,
            1004658L
        ),
        position(
            "promotion-gives-check",
            "15. Promotion gives check",
            "4k3/1P6/8/8/8/8/K7/8 w - - 0 1",
            6,
            217342L
        ),
        position(
            "underpromotion-gives-check",
            "16. Underpromotion gives check",
            "8/P1k5/K7/8/8/8/8/8 w - - 0 1",
            6,
            92683L
        ),
        position(
            "self-stalemate",
            "17. Self stalemate",
            "K1k5/8/P7/8/8/8/8/8 w - - 0 1",
            6,
            2217L
        ),
        position(
            "stalemate-checkmate",
            "18. Stalemate/Checkmate",
            "8/k1P5/8/1K6/8/8/8/8 w - - 0 1",
            7,
            567584L
        ),
        position(
            "double-check",
            "19. Double check",
            "8/8/2k5/5q2/5n2/8/5K2/8 b - - 0 1",
            4,
            23527L
        ),
        position(
            "new-position",
            "20. New position",
            "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
            5,
            89941194L
        )
    );

    @Override
    public String id() {
        return "seedv6-default";
    }

    @Override
    public String name() {
        return "SeedV6 default perft positions";
    }

    @Override
    public List<PerftPosition> positions() {
        return POSITIONS;
    }

    private static PerftPosition position(
        String id,
        String name,
        String fen,
        int depth,
        long expectedNodes
    ) {
        return new PerftPosition(
            id,
            name,
            fen,
            List.of(new PerftExpectation(depth, expectedNodes)),
            depth
        );
    }
}
