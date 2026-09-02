package com.ohinteractive.seedv6.search.quiescence;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.order.MoveOrdering;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuiescenceCorpusTest {

    static final long CORPUS_SEED = 0x5eed_0009_cafeL;
    static final int RANDOM_POSITIONS = 384;
    static final int QPLIES_PER_POSITION = 2;

    private static final int NEG_INF = -1_000_000_000;
    private static final int POS_INF = 1_000_000_000;
    private static final int MAX_MOVES = 256;

    @Test
    void boundedDeterministicLegalCorpusMatchesIndependentUnprunedOracle() {
        final Random random = new Random(CORPUS_SEED);
        final QuiescenceSearch production = new QuiescenceSearch();
        int positions = 0;
        int calls = 0;
        int checkedPositions = 0;
        int tacticalPositions = 0;
        int enPassantPositions = 0;
        int promotionPositions = 0;

        while(positions < RANDOM_POSITIONS) {
            long[] board = Board.startingPosition();
            GameHistory.Builder history = GameHistory.builder(board);
            for(int gamePly = 0; gamePly < 64 && positions < RANDOM_POSITIONS; gamePly ++) {
                final GameHistory snapshot = history.snapshot();
                final CorpusCounts counts = classify(board);
                if(counts.checked) checkedPositions ++;
                if(counts.tactical != 0) tacticalPositions ++;
                if(counts.enPassant != 0) enPassantPositions ++;
                if(counts.promotions != 0) promotionPositions ++;

                calls += compareAtBoundary(
                    production, board, snapshot, gamePly,
                    QuiescenceSearch.SOFT_QPLY_LIMIT - 1
                );
                calls += compareAtBoundary(
                    production, board, snapshot, gamePly,
                    QuiescenceSearch.SOFT_QPLY_LIMIT
                );
                positions ++;

                final long[] moves = legalMoves(board);
                if(moves.length == 0) break;
                final long selected = selectBiased(board, moves, gamePly, random);
                board = apply(board, selected);
                history.appendPosition(board);
            }
        }

        final List<String> specialPositions = List.of(
            "4r1k1/8/8/8/8/8/8/4K3 w - - 0 1",
            "4r1k1/8/8/8/8/8/8/2B1K3 w - - 0 1",
            "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
            "4k3/8/8/3pP3/3r4/8/8/4K3 w - d6 0 1",
            "4k3/8/8/3R4/3Pp3/8/8/4K3 b - d3 0 1",
            "8/5bk1/8/2Pp4/8/1K6/8/8 w - d6 0 1",
            "7k/P7/8/8/8/8/8/K7 w - - 0 1",
            "k7/8/8/8/8/8/p7/7K b - - 0 1",
            "4k2r/6P1/8/8/8/8/8/4K3 w - - 0 1",
            "4k3/8/8/8/8/8/6p1/4K2R b - - 0 1",
            "k6r/6P1/8/8/8/8/8/7K w - - 0 1",
            "7k/6p1/5KQ1/8/8/8/8/8 w - - 0 1"
        );
        for(int index = 0; index < specialPositions.size(); index ++) {
            final long[] board = Board.fromFen(specialPositions.get(index));
            final GameHistory history = GameHistory.initial(board);
            final CorpusCounts counts = classify(board);
            if(counts.checked) checkedPositions ++;
            if(counts.tactical != 0) tacticalPositions ++;
            if(counts.enPassant != 0) enPassantPositions ++;
            if(counts.promotions != 0) promotionPositions ++;
            calls += compareAtBoundary(
                production, board, history, index,
                QuiescenceSearch.SOFT_QPLY_LIMIT - 1
            );
            calls += compareAtBoundary(
                production, board, history, index,
                QuiescenceSearch.SOFT_QPLY_LIMIT
            );
            positions ++;
        }

        assertEquals(RANDOM_POSITIONS + specialPositions.size(), positions);
        assertEquals(positions * QPLIES_PER_POSITION, calls);
        assertTrue(checkedPositions >= 4, "checked=" + checkedPositions);
        assertTrue(tacticalPositions >= 40, "tactical=" + tacticalPositions);
        assertTrue(enPassantPositions >= 3, "enPassant=" + enPassantPositions);
        assertTrue(promotionPositions >= 5, "promotions=" + promotionPositions);
    }

    private static int compareAtBoundary(
        QuiescenceSearch production, long[] board, GameHistory history,
        int absolutePly, int qPly
    ) {
        final long[] before = board.clone();
        final int expected = QuiescenceOracle.search(
            board, history, absolutePly, qPly, NEG_INF, POS_INF
        );
        final QuiescenceSearch.Result result = production.searchAtQply(
            board, new SearchLineHistory(history), SearchControl.unlimited(),
            absolutePly, qPly, NEG_INF, POS_INF
        );
        assertTrue(result.completed());
        assertEquals(expected, result.score(),
            "absolutePly=" + absolutePly + " qPly=" + qPly);
        assertArrayEquals(before, board);
        return 1;
    }

    private static CorpusCounts classify(long[] board) {
        final long[] moves = legalMoves(board);
        int tactical = 0;
        int enPassant = 0;
        int promotions = 0;
        for(long move : moves) {
            if(MoveOrdering.isTactical(board, move)) tactical ++;
            if(isEnPassant(board, move)) enPassant ++;
            if(promotion(move) != Value.NONE) promotions ++;
        }
        final int player = (int) board[Board.STATUS] & Board.PLAYER_BIT;
        final boolean checked = Board.isPlayerInCheckPext(
            board[0], board[1], board[2], board[3], player
        );
        return new CorpusCounts(checked, tactical, enPassant, promotions);
    }

    private static long selectBiased(
        long[] board, long[] moves, int gamePly, Random random
    ) {
        final long[] preferred = new long[moves.length];
        int preferredCount = 0;

        for(long move : moves) {
            if(promotion(move) != Value.NONE || isEnPassant(board, move)) {
                preferred[preferredCount ++] = move;
            }
        }
        if(preferredCount != 0) return preferred[random.nextInt(preferredCount)];

        if(gamePly % 3 == 0) {
            for(long move : moves) {
                final long[] child = apply(board, move);
                final int childPlayer = (int) child[Board.STATUS] & Board.PLAYER_BIT;
                if(Board.isPlayerInCheckPext(
                    child[0], child[1], child[2], child[3], childPlayer
                )) preferred[preferredCount ++] = move;
            }
            if(preferredCount != 0) return preferred[random.nextInt(preferredCount)];
        }

        if(gamePly % 2 == 0) {
            preferredCount = 0;
            for(long move : moves) {
                if(MoveOrdering.isTactical(board, move)) {
                    preferred[preferredCount ++] = move;
                }
            }
            if(preferredCount != 0) return preferred[random.nextInt(preferredCount)];
        }
        return moves[random.nextInt(moves.length)];
    }

    private static long[] legalMoves(long[] board) {
        final long[] moves = new long[MAX_MOVES];
        final int count = Gen.genAll(
            board[0], board[1], board[2], board[3],
            (int) board[Board.STATUS], board[Board.KEY], true,
            moves, new long[Board.MAX_BITBOARDS]
        );
        return Arrays.copyOf(moves, count);
    }

    private static int promotion(long move) {
        return (int) (move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS;
    }

    private static boolean isEnPassant(long[] board, long move) {
        final int startPiece = (int) (move >>> Board.START_PIECE_SHIFT) & Board.PIECE_BITS;
        final int targetPiece = (int) (move >>> Board.TARGET_PIECE_SHIFT) & Board.PIECE_BITS;
        final int from = (int) move & Board.SQUARE_BITS;
        final int target = (int) (move >>> Board.TARGET_SQUARE_SHIFT) & Board.SQUARE_BITS;
        return (startPiece & Piece.TYPE) == Piece.PAWN
            && targetPiece == Value.NONE
            && (from & Value.FILE) != (target & Value.FILE)
            && target == Board.enPassantSquare((int) board[Board.STATUS]);
    }

    private static long[] apply(long[] board, long move) {
        final long[] child = new long[Board.MAX_BITBOARDS];
        Board.makeMoveInto(
            board[0], board[1], board[2], board[3],
            (int) board[Board.STATUS], board[Board.KEY], move, child
        );
        return child;
    }

    private record CorpusCounts(
        boolean checked, int tactical, int enPassant, int promotions
    ) {}
}
