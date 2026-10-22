package com.ohinteractive.seedv6.core.move;

import com.ohinteractive.seedv6.core.Board;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MoveNotationTest {
    @Test void sanCoversLegalDisambiguationCapturesCastleChecksMateAndPromotions() {
        san("4k3/8/8/8/8/2N1N3/8/4K3 w - - 0 1", "c3d5", "Ncd5");
        san("4r1k1/8/8/8/8/2N1N3/8/4K3 w - - 0 1", "c3d5", "Nd5");
        san("4k3/8/8/8/R7/8/8/R3K3 w - - 0 1", "a1a2", "R1a2");
        san("7k/8/8/8/8/Q7/8/Q1Q4K w - - 0 1", "a1c3", "Qa1c3+");
        san("5k2/8/8/8/8/8/8/4K2R w K - 0 1", "e1g1", "O-O+");
        san("4k3/8/8/8/8/8/8/R3K3 w Q - 0 1", "e1c1", "O-O-O");
        san("7k/5Q2/6K1/8/8/8/8/8 w - - 0 1", "f7g7", "Qg7#");
        san("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1", "e5d6", "exd6");
        san("1r5k/P7/8/8/8/8/8/7K w - - 0 1", "a7b8q", "axb8=Q+");
        san("7k/8/8/8/8/8/p7/7K b - - 0 17", "a2a1n", "a1=N");
        san("4k3/8/8/3p4/4P3/8/8/4K3 w - - 0 1", "e4d5", "exd5");
    }
    private static void san(String fen, String coordinate, String expected) {
        long[] board = Board.fromFen(fen);
        int from = square(coordinate.substring(0, 2)), to = square(coordinate.substring(2, 4));
        MoveIntent.Promotion promotion = coordinate.length() == 4 ? MoveIntent.Promotion.NONE
                : coordinate.charAt(4) == 'q' ? MoveIntent.Promotion.QUEEN : MoveIntent.Promotion.KNIGHT;
        long move = new LegalMoveResolver().resolve(board, new MoveIntent(from, to, promotion));
        assertEquals(expected, Move.notation(board, move), coordinate);
    }
    private static int square(String s) { return (s.charAt(1) - '1') * 8 + s.charAt(0) - 'a'; }
}
