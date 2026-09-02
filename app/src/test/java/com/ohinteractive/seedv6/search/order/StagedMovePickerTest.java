package com.ohinteractive.seedv6.search.order;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.move.LegalMoveResolver;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.move.MoveIntent;
import com.ohinteractive.seedv6.core.move.MoveIntent.Promotion;
import com.ohinteractive.seedv6.search.tt.TranspositionTable;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Bound;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Cacheability;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Probe;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.ProbeOutcome;

class StagedMovePickerTest {

    private static final String KIWIPETE =
        "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
    private static final String HIGH_MOBILITY_110 =
        "7k/6pp/Q1Q1Q3/1Q1Q1Q2/Q1Q1Q3/R1B1N1R1/2B1N3/K7 w - - 0 1";
    private static final String HIGH_MOBILITY_99 =
        "7k/6pp/Q1Q1Q3/1Q1Q1Q2/Q1Q1Q3/8/1RBNBRN1/K7 w - - 0 1";

    @Test
    void outputExactlyMatchesAuthoritativeLegalGenerationAcrossRepresentativePositions() {
        final String[] fens = {
            Board.FEN_STARTING_POSITION,
            KIWIPETE,
            "4k3/8/8/8/8/8/4r3/4K3 w - - 0 1",
            "4k3/8/8/8/4P3/8/8/4K3 w - - 0 1",
            "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
            "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
            "7k/P7/8/8/8/8/8/K7 w - - 0 1",
            "4k2r/6P1/8/8/8/8/8/4K3 w - - 0 1",
            "2K2r2/4P3/8/8/8/8/8/3k4 w - - 0 1",
            "8/8/2k5/5q2/5n2/8/5K2/8 b - - 0 1",
            HIGH_MOBILITY_110,
            HIGH_MOBILITY_99
        };

        for(String fen : fens) assertExactLegalSet(fen);
    }

    @Test
    void checkedNodesUseOneEvasionSetAndStillYieldEveryEvasion() {
        final String[] fens = {
            "4k3/8/8/8/8/8/4r3/4K3 w - - 0 1",
            "2K2r2/4P3/8/8/8/8/8/3k4 w - - 0 1",
            "4r1k1/8/8/8/1b6/8/8/4K3 w - - 0 1"
        };
        final MoveOrdering ordering = new MoveOrdering(2);
        for(String fen : fens) {
            final long[] board = Board.fromFen(fen);
            final StagedMovePicker picker = ordering.picker();
            final int count = picker.prepare(board, 0, StagedMovePicker.NO_MOVE);
            assertTrue(picker.inCheck(0), fen);
            assertEquals(authoritativeMoves(board).length, count, fen);
            assertEquals(count, drain(picker, 0).length, fen);
        }
    }

    @Test
    void highMobilityPositionsCrossDonorHazardAndRemainWithinFullV6Capacity() {
        assertEquals(256, StagedMovePicker.MAX_MOVES);
        final long[] first = authoritativeMoves(Board.fromFen(HIGH_MOBILITY_110));
        final long[] second = authoritativeMoves(Board.fromFen(HIGH_MOBILITY_99));
        assertEquals(110, first.length);
        assertEquals(99, second.length);
        assertTrue(first.length > 101, "fixture must cross donor Sort's final valid index");
        assertTrue(first.length <= StagedMovePicker.MAX_MOVES);
        assertTrue(second.length <= StagedMovePicker.MAX_MOVES);
        assertExactLegalSet(HIGH_MOBILITY_110);
        assertExactLegalSet(HIGH_MOBILITY_99);
    }

    @Test
    void legalHashTacticalQuietAndPromotionAreFirstAndUnique() {
        assertHashFirst(
            Board.fromFen("4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1"), "e4d5"
        );
        assertHashFirst(Board.startingPosition(), "e2e4");
        assertHashFirst(
            Board.fromFen("7k/P7/8/8/8/8/8/K7 w - - 0 1"), "a7a8n"
        );
    }

    @Test
    void depthInsufficientRealTtProbeStillSuppliesItsLegalOrderingMove() {
        final long[] board = Board.startingPosition();
        final long hashMove = move(board, "g1f3");
        final TranspositionTable table = new TranspositionTable(1);
        table.store(board[Board.KEY], 2, Bound.EXACT, 17, 0, hashMove,
            Cacheability.POSITION_ONLY);
        final Probe probe = new Probe();

        assertEquals(
            ProbeOutcome.DEPTH_INSUFFICIENT,
            table.probe(board[Board.KEY], 3, -100, 100, 0, probe)
        );
        assertTrue(probe.keyMatches());
        assertFalse(probe.scoreUsable());
        assertEquals(hashMove, probe.move());

        final MoveOrdering ordering = new MoveOrdering(2);
        final StagedMovePicker picker = ordering.picker();
        picker.prepare(board, 0, probe.move());
        assertEquals(hashMove, picker.next(0));
    }

    @Test
    void invalidStaleMalformedAndCollisionHintsLeaveTheLegalSetUnchanged() {
        final long[] board = Board.startingPosition();
        final long[] expected = authoritativeMoves(board);
        final long stale = move(
            Board.fromFen("4k3/8/8/8/4P3/8/8/4K3 w - - 0 1"), "e4e5"
        );
        final long malformed = move(board, "e2e4") | Move.UNUSED_MOVE_BIT_MASK;
        final long illegal = 1L;

        assertInvalidHint(board, expected, stale);
        assertInvalidHint(board, expected, malformed);
        assertInvalidHint(board, expected, illegal);

        final long[] collisionBoard = Board.fromFen(
            "6k1/8/8/8/8/8/8/R6K w - - 0 1"
        );
        final long collisionMove = move(collisionBoard, "a1a2");
        final TranspositionTable table = new TranspositionTable(1);
        table.store(board[Board.KEY] ^ 0x4000L, 4, Bound.EXACT, 9, 0, collisionMove,
            Cacheability.POSITION_ONLY);
        final Probe mismatch = new Probe();
        assertEquals(
            ProbeOutcome.KEY_MISMATCH,
            table.probe(board[Board.KEY], 1, -100, 100, 0, mismatch)
        );
        assertFalse(mismatch.keyMatches());
        assertEquals(StagedMovePicker.NO_MOVE, mismatch.move());
        assertInvalidHint(board, expected, collisionMove);
    }

    @Test
    void hashMoveSuppressesItsKillerOrHistoryOccurrence() {
        final long[] board = Board.startingPosition();
        final long killerHash = move(board, "e2e4");
        final MoveOrdering killerOrdering = new MoveOrdering(3);
        assertTrue(killerOrdering.recordQuietCutoff(board, 0, killerHash, 4));
        final long[] killerOutput = output(killerOrdering, board, 0, killerHash);
        assertEquals(killerHash, killerOutput[0]);
        assertEquals(1, occurrences(killerOutput, killerHash));

        final long historyHash = move(board, "g1f3");
        final MoveOrdering historyOrdering = new MoveOrdering(3);
        assertTrue(historyOrdering.recordQuietCutoff(board, 1, historyHash, 8));
        final long[] historyOutput = output(historyOrdering, board, 0, historyHash);
        assertEquals(historyHash, historyOutput[0]);
        assertEquals(1, occurrences(historyOutput, historyHash));
    }

    @Test
    void nonLosingTacticalsPrecedeKillersAndQuietsWhileLosingTacticalsFollow() {
        final long[] winningBoard = Board.fromFen(
            "4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1"
        );
        final long winning = move(winningBoard, "e4d5");
        final long winningQuietKiller = move(winningBoard, "e1f1");
        final MoveOrdering winningOrdering = new MoveOrdering(2);
        winningOrdering.recordQuietCutoff(winningBoard, 0, winningQuietKiller, 4);
        final long[] winningOutput = output(
            winningOrdering, winningBoard, 0, StagedMovePicker.NO_MOVE
        );
        assertTrue(indexOf(winningOutput, winning) < indexOf(winningOutput, winningQuietKiller));

        final long[] equalBoard = Board.fromFen(
            "rq2k3/8/8/8/8/8/8/R3K3 w - - 0 1"
        );
        final long equal = move(equalBoard, "a1a8");
        final long equalQuiet = move(equalBoard, "e1f1");
        final long[] equalOutput = output(new MoveOrdering(2), equalBoard, 0,
            StagedMovePicker.NO_MOVE);
        assertTrue(indexOf(equalOutput, equal) < indexOf(equalOutput, equalQuiet));

        final long[] losingBoard = Board.fromFen(
            "3rk3/8/8/3p4/8/8/8/3QK3 w - - 0 1"
        );
        final long losing = move(losingBoard, "d1d5");
        final long losingQuietKiller = move(losingBoard, "e1f1");
        final MoveOrdering losingOrdering = new MoveOrdering(2);
        losingOrdering.recordQuietCutoff(losingBoard, 0, losingQuietKiller, 4);
        final long[] losingOutput = output(
            losingOrdering, losingBoard, 0, StagedMovePicker.NO_MOVE
        );
        assertEquals(0, indexOf(losingOutput, losingQuietKiller));
        assertEquals(losingOutput.length - 1, indexOf(losingOutput, losing));
    }

    @Test
    void promotionsCapturePromotionAndEnPassantFollowNumericSeePriority() {
        final long[] promotionBoard = Board.fromFen(
            "7k/P7/8/8/8/8/8/K7 w - - 0 1"
        );
        final long[] promotionOutput = output(
            new MoveOrdering(2), promotionBoard, 0, StagedMovePicker.NO_MOVE
        );
        assertStrictOrder(promotionOutput,
            move(promotionBoard, "a7a8q"), move(promotionBoard, "a7a8r"),
            move(promotionBoard, "a7a8b"), move(promotionBoard, "a7a8n"));

        final long[] capturePromotionBoard = Board.fromFen(
            "4k2r/6P1/8/8/8/8/8/4K3 w - - 0 1"
        );
        final long[] capturePromotionOutput = output(
            new MoveOrdering(2), capturePromotionBoard, 0, StagedMovePicker.NO_MOVE
        );
        assertStrictOrder(capturePromotionOutput,
            move(capturePromotionBoard, "g7h8q"), move(capturePromotionBoard, "g7h8r"),
            move(capturePromotionBoard, "g7h8b"), move(capturePromotionBoard, "g7h8n"));

        final long[] enPassantBoard = Board.fromFen(
            "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"
        );
        final long enPassant = move(enPassantBoard, "e5d6");
        final long quiet = move(enPassantBoard, "e1f1");
        final long[] enPassantOutput = output(
            new MoveOrdering(2), enPassantBoard, 0, StagedMovePicker.NO_MOVE
        );
        assertTrue(indexOf(enPassantOutput, enPassant) < indexOf(enPassantOutput, quiet));
    }

    @Test
    void legalKillersAreRecentFirstAndStaleKillersAreIgnored() {
        final long[] board = Board.startingPosition();
        final long older = move(board, "e2e4");
        final long recent = move(board, "d2d4");
        final MoveOrdering ordering = new MoveOrdering(2);
        ordering.recordQuietCutoff(board, 0, older, 2);
        ordering.recordQuietCutoff(board, 0, recent, 2);

        final long[] legalOutput = output(ordering, board, 0, StagedMovePicker.NO_MOVE);
        assertEquals(recent, legalOutput[0]);
        assertEquals(older, legalOutput[1]);

        final long[] rookBoard = Board.fromFen("6k1/8/8/8/8/8/8/R6K w - - 0 1");
        final MoveOrdering staleOrdering = new MoveOrdering(2);
        staleOrdering.recordQuietCutoff(rookBoard, 0, move(rookBoard, "a1a2"), 8);
        final long[] staleOutput = output(
            staleOrdering, board, 0, StagedMovePicker.NO_MOVE
        );
        assertSameSet(authoritativeMoves(board), staleOutput);
        assertNotEquals("a1a2", Move.coordinate(staleOutput[0]));
    }

    @Test
    void boundedHistoryDeterministicallyOrdersQuietsWithoutCrossPlyKillerEffect() {
        final long[] board = Board.startingPosition();
        final long preferred = move(board, "g1f3");
        final MoveOrdering ordering = new MoveOrdering(3);
        ordering.recordQuietCutoff(board, 1, preferred, 8);

        final long[] first = output(ordering, board, 0, StagedMovePicker.NO_MOVE);
        final long[] second = output(ordering, board, 0, StagedMovePicker.NO_MOVE);

        assertEquals(preferred, first[0]);
        assertArrayEquals(first, second);
    }

    @Test
    void equalScoreTiesUseFullMoveIdentityAndRemainDeterministic() {
        final long[] board = Board.startingPosition();
        final long[] first = output(new MoveOrdering(2), board, 0,
            StagedMovePicker.NO_MOVE);
        final long[] second = output(new MoveOrdering(2), board, 0,
            StagedMovePicker.NO_MOVE);
        assertArrayEquals(first, second);
        for(int index = 1; index < first.length; index ++) {
            assertTrue(Long.compareUnsigned(first[index - 1], first[index]) < 0);
        }
    }

    @Test
    void orderingPreservesBoardsMoveBitsSpecialMoveIdentityAndTtContents() {
        final long[] board = Board.fromFen(
            "4k2r/6P1/8/8/8/8/8/4K3 w - - 0 1"
        );
        final long[] boardBefore = board.clone();
        final long[] generated = authoritativeMoves(board);
        final long[] generatedBefore = generated.clone();
        final long promotion = move(board, "g7h8n");
        final TranspositionTable table = new TranspositionTable(1);
        table.store(board[Board.KEY], 4, Bound.EXACT, 11, 0, promotion,
            Cacheability.POSITION_ONLY);
        final Probe before = new Probe();
        table.probe(board[Board.KEY], 4, -100, 100, 0, before);

        final long[] ordered = output(new MoveOrdering(2), board, 0, promotion);

        assertArrayEquals(boardBefore, board);
        assertArrayEquals(generatedBefore, generated);
        assertEquals(promotion, ordered[0]);
        assertEquals(Promotion.KNIGHT, Move.promotion(ordered[0]));

        final long[] selectedChild = apply(board, ordered[0]);
        final long[] resolvedChild = apply(board, promotion);
        assertArrayEquals(resolvedChild, selectedChild);

        final Probe after = new Probe();
        table.probe(board[Board.KEY], 4, -100, 100, 0, after);
        assertEquals(before.outcome(), after.outcome());
        assertEquals(before.key(), after.key());
        assertEquals(before.depth(), after.depth());
        assertEquals(before.bound(), after.bound());
        assertEquals(before.score(), after.score());
        assertEquals(before.move(), after.move());
        assertEquals(before.generation(), after.generation());
    }

    @Test
    void pickerPlyBoundariesAndPreparationContractAreExplicit() {
        final MoveOrdering ordering = new MoveOrdering(2);
        final StagedMovePicker picker = ordering.picker();
        final long[] board = Board.startingPosition();

        assertThrows(IllegalStateException.class, () -> picker.next(0));
        assertTrue(picker.prepare(board, 0, StagedMovePicker.NO_MOVE) > 0);
        assertTrue(picker.prepare(board, 1, StagedMovePicker.NO_MOVE) > 0);
        assertThrows(
            IndexOutOfBoundsException.class,
            () -> picker.prepare(board, -1, StagedMovePicker.NO_MOVE)
        );
        assertThrows(
            IndexOutOfBoundsException.class,
            () -> picker.prepare(board, 2, StagedMovePicker.NO_MOVE)
        );
        assertThrows(IndexOutOfBoundsException.class, () -> picker.next(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> picker.next(2));
    }

    @Test
    void quiescencePreparationIsTacticalOnlyOutsideCheckAndCompleteInCheck() {
        final MoveOrdering ordering = new MoveOrdering(2);
        final StagedMovePicker picker = ordering.picker();
        final long[] ordinary = Board.fromFen(
            "4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1"
        );
        final long[] expectedTactical = new long[StagedMovePicker.MAX_MOVES];
        final int expectedTacticalCount = Gen.genTactical(
            ordinary[0], ordinary[1], ordinary[2], ordinary[3],
            (int) ordinary[Board.STATUS], ordinary[Board.KEY], true,
            expectedTactical, new long[Board.MAX_BITBOARDS]
        );
        assertEquals(
            expectedTacticalCount,
            picker.prepareQuiescence(ordinary, 0, StagedMovePicker.NO_MOVE, 0L)
        );
        assertFalse(picker.inCheck(0));
        assertSameSet(
            Arrays.copyOf(expectedTactical, expectedTacticalCount), drain(picker, 0)
        );
        picker.clearPly(0);
        assertThrows(IllegalStateException.class, () -> picker.next(0));

        final long[] checked = Board.fromFen(
            "4r1k1/8/8/8/8/8/8/2B1K3 w - - 0 1"
        );
        final long checkers = checkers(checked);
        final long[] expectedEvasions = authoritativeMoves(checked);
        assertEquals(
            expectedEvasions.length,
            picker.prepareQuiescence(
                checked, 0, StagedMovePicker.NO_MOVE, checkers
            )
        );
        assertTrue(picker.inCheck(0));
        assertSameSet(expectedEvasions, drain(picker, 0));
    }

    private static void assertExactLegalSet(String fen) {
        final long[] board = Board.fromFen(fen);
        final long[] boardBefore = board.clone();
        final long[] expected = authoritativeMoves(board);
        final MoveOrdering ordering = new MoveOrdering(3);
        final StagedMovePicker picker = ordering.picker();
        final int count = picker.prepare(board, 0, StagedMovePicker.NO_MOVE);
        final long[] actual = drain(picker, 0);

        assertEquals(expected.length, count, fen);
        assertEquals(expected.length, actual.length, fen);
        assertSameSet(expected, actual);
        assertArrayEquals(boardBefore, board, fen);
        int tacticalCount = 0;
        for(long move : expected) {
            if(MoveOrdering.isTactical(board, move)) tacticalCount ++;
        }
        assertEquals(tacticalCount, picker.seeEvaluationCount(0), fen);
        assertEquals(StagedMovePicker.NO_MOVE, picker.next(0));
    }

    private static void assertHashFirst(long[] board, String coordinate) {
        final long hash = move(board, coordinate);
        final long[] actual = output(new MoveOrdering(2), board, 0, hash);
        assertEquals(hash, actual[0]);
        assertEquals(1, occurrences(actual, hash));
        assertSameSet(authoritativeMoves(board), actual);
    }

    private static void assertInvalidHint(long[] board, long[] expected, long hint) {
        final long[] actual = output(new MoveOrdering(2), board, 0, hint);
        assertSameSet(expected, actual);
        assertEquals(0, occurrences(actual, hint));
    }

    private static long[] output(
        MoveOrdering ordering, long[] board, int ply, long hashMove
    ) {
        final StagedMovePicker picker = ordering.picker();
        picker.prepare(board, ply, hashMove);
        return drain(picker, ply);
    }

    private static long[] drain(StagedMovePicker picker, int ply) {
        final long[] output = new long[picker.moveCount(ply)];
        int count = 0;
        long move;
        while((move = picker.next(ply)) != StagedMovePicker.NO_MOVE) {
            output[count ++] = move;
        }
        return count == output.length ? output : Arrays.copyOf(output, count);
    }

    private static long[] authoritativeMoves(long[] board) {
        final long[] moves = new long[StagedMovePicker.MAX_MOVES];
        final long[] scratch = new long[Board.MAX_BITBOARDS];
        final int count = Gen.genAll(
            board[0], board[1], board[2], board[3],
            Math.toIntExact(board[Board.STATUS]), board[Board.KEY], true, moves, scratch
        );
        return Arrays.copyOf(moves, count);
    }

    private static long checkers(long[] board) {
        final int status = (int) board[Board.STATUS];
        final int player = status & Board.PLAYER_BIT;
        final long occupancy = board[0] | board[1] | board[2];
        final long colour = ~(-(long) player ^ board[3]);
        final long king = board[0] & ~board[1] & ~board[2] & colour;
        return Board.getCheckersPext(
            board[0], board[1], board[2], board[3], colour, player,
            Long.numberOfTrailingZeros(king), occupancy
        );
    }

    private static void assertSameSet(long[] expected, long[] actual) {
        final Set<Long> expectedSet = new HashSet<>();
        final Set<Long> actualSet = new HashSet<>();
        for(long move : expected) assertTrue(expectedSet.add(move), "duplicate authoritative move");
        for(long move : actual) assertTrue(actualSet.add(move), "duplicate picker move");
        assertEquals(expectedSet, actualSet);
        assertEquals(expected.length, actual.length);
    }

    private static int occurrences(long[] moves, long target) {
        int count = 0;
        for(long move : moves) if(move == target) count ++;
        return count;
    }

    private static int indexOf(long[] moves, long target) {
        for(int index = 0; index < moves.length; index ++) {
            if(moves[index] == target) return index;
        }
        return -1;
    }

    private static void assertStrictOrder(long[] moves, long... expectedOrder) {
        int prior = -1;
        for(long move : expectedOrder) {
            final int current = indexOf(moves, move);
            assertTrue(current > prior, Move.coordinate(move));
            prior = current;
        }
    }

    private static long move(long[] board, String coordinate) {
        final Promotion promotion = coordinate.length() == 4 ? Promotion.NONE
            : switch(coordinate.charAt(4)) {
                case 'q' -> Promotion.QUEEN;
                case 'r' -> Promotion.ROOK;
                case 'b' -> Promotion.BISHOP;
                case 'n' -> Promotion.KNIGHT;
                default -> throw new IllegalArgumentException("Unknown promotion: " + coordinate);
            };
        return new LegalMoveResolver().resolve(
            board,
            new MoveIntent(square(coordinate.substring(0, 2)),
                square(coordinate.substring(2, 4)), promotion)
        );
    }

    private static int square(String coordinate) {
        return (coordinate.charAt(1) - '1') * 8 + coordinate.charAt(0) - 'a';
    }

    private static long[] apply(long[] board, long move) {
        final long[] child = new long[Board.MAX_BITBOARDS];
        Board.makeMoveInto(
            board[0], board[1], board[2], board[3],
            Math.toIntExact(board[Board.STATUS]), board[Board.KEY], move, child
        );
        return child;
    }
}
