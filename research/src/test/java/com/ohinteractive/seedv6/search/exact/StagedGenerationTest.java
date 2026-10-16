package com.ohinteractive.seedv6.search.exact;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.tools.search.ExactSearchHarness;

class StagedGenerationTest {
    static List<String> fens() {
        var positions = new ArrayList<>(ExactSearchMechanicsTest.fens());
        for(var p : ExactSearchHarness.positions()) positions.add(p.fen());
        positions.addAll(List.of("4k3/8/8/8/8/8/4r3/4K3 w - - 0 1",
                "4r1k1/8/8/8/1b6/8/8/4K3 w - - 0 1",
                "4k3/8/8/3pP3/8/8/8/4R1K1 b - e3 0 1"));
        return positions;
    }

    @Test void existingSubsetsAreAnExactDisjointStablePartitionOfGenAll() {
        int checked = 0, positions = 0;
        Random random = new Random(17018);
        for(String fen : fens()) {
            long[] board = Board.fromFen(fen);
            for(int ply = 0; ply < 48; ply++) {
                long[] all = ExhaustiveOracle.legalMoves(board);
                long[] tactical = subset(board, 1), quiet = subset(board, 2);
                int ep = Board.enPassantSquare((int) board[Board.STATUS]);
                assertArrayEquals(Arrays.stream(all).filter(m -> CaptureHistory.isTactical(m, ep)).toArray(), tactical);
                assertArrayEquals(Arrays.stream(all).filter(m -> !CaptureHistory.isTactical(m, ep)).toArray(), quiet);
                long[] union = Arrays.copyOf(tactical, tactical.length + quiet.length);
                System.arraycopy(quiet, 0, union, tactical.length, quiet.length);
                assertEquals(union.length, new HashSet<>(Arrays.stream(union).boxed().toList()).size());
                long[] sorted = all.clone(); Arrays.sort(sorted); Arrays.sort(union); assertArrayEquals(sorted, union);
                if(checkers(board) != 0) {
                    checked++;
                    // Evasions must retain class-relative generation ties as well as exact identity.
                    long[] evasion = subset(board, 3);
                    assertArrayEquals(tactical, Arrays.stream(evasion).filter(m -> CaptureHistory.isTactical(m, ep)).toArray());
                    assertArrayEquals(quiet, Arrays.stream(evasion).filter(m -> !CaptureHistory.isTactical(m, ep)).toArray());
                }
                positions++;
                if(all.length == 0) break;
                board = ExhaustiveOracle.child(board, all[random.nextInt(all.length)]);
            }
        }
        assertTrue(checked > 20); assertTrue(positions > 500);
        System.out.println("staged generator partition positions=" + positions + " checked=" + checked);
    }

    static long[] subset(long[] b, int mode) {
        long[] moves = new long[512], scratch = new long[Board.MAX_BITBOARDS];
        int n = switch(mode) {
            case 1 -> Gen.genTactical(b[0], b[1], b[2], b[3], (int) b[Board.STATUS], b[Board.KEY], true, moves, scratch);
            case 2 -> Gen.genQuiet(b[0], b[1], b[2], b[3], (int) b[Board.STATUS], b[Board.KEY], true, moves, scratch);
            case 3 -> Gen.genEvasion(b[0], b[1], b[2], b[3], (int) b[Board.STATUS], b[Board.KEY], true, checkers(b), moves, scratch);
            default -> throw new AssertionError();
        };
        return Arrays.copyOf(moves, n);
    }

    static long checkers(long[] b) {
        int player = Board.player((int) b[Board.STATUS]);
        long color = ~(-(long) player ^ b[3]);
        return Board.getCheckersPext(b[0], b[1], b[2], b[3], color, player,
                Long.numberOfTrailingZeros(b[0] & ~b[1] & ~b[2] & color), b[0] | b[1] | b[2]);
    }
}
