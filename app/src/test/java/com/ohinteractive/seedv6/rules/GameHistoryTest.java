package com.ohinteractive.seedv6.rules;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.LegalMoveResolver;
import com.ohinteractive.seedv6.core.move.MoveIntent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameHistoryTest {

    @Test
    void initialAndCurrentRootAreOrdinaryCountedEntries() {
        long[] board = Board.startingPosition();
        final GameHistory.Builder builder = GameHistory.builder(board);

        assertEquals(1, builder.snapshot().size());
        assertEquals(1, builder.snapshot().currentOccurrences(board));

        board = playAndAppend(builder, board, "g1f3");
        board = playAndAppend(builder, board, "g8f6");
        board = playAndAppend(builder, board, "f3g1");
        board = playAndAppend(builder, board, "f6g8");

        final GameHistory history = builder.snapshot();
        assertEquals(5, history.size());
        assertEquals(2, history.currentOccurrences(board));
        assertFalse(history.isFormalThreefold(board));
    }

    @Test
    void formalThreefoldConcernsOnlyTheCurrentPosition() {
        long[] board = Board.startingPosition();
        final GameHistory.Builder builder = GameHistory.builder(board);
        for(int cycle = 0; cycle < 2; cycle ++) {
            board = playAndAppend(builder, board, "g1f3");
            board = playAndAppend(builder, board, "g8f6");
            board = playAndAppend(builder, board, "f3g1");
            board = playAndAppend(builder, board, "f6g8");
        }

        final GameHistory thirdInitial = builder.snapshot();
        assertEquals(3, thirdInitial.currentOccurrences(board));
        assertTrue(thirdInitial.isFormalThreefold(board));

        board = playAndAppend(builder, board, "b1c3");
        final GameHistory unrelatedCurrent = builder.snapshot();
        assertEquals(1, unrelatedCurrent.currentOccurrences(board));
        assertFalse(unrelatedCurrent.isFormalThreefold(board));
    }

    @Test
    void reversibleWindowCannotReachPastTheHalfmoveClock() {
        final long[] first = Board.fromFen("4k3/8/8/8/8/8/8/R3K3 w - - 0 1");
        final long[] second = Board.fromFen("4k3/8/8/8/8/8/8/R3K3 w - - 0 1");
        final long[] current = Board.fromFen("4k3/8/8/8/8/8/8/R3K3 w - - 1 1");
        final GameHistory history = GameHistory.builder(first)
            .appendPosition(second)
            .appendPosition(current)
            .snapshot();

        assertEquals(3, history.occurrences(history.currentKey()));
        assertEquals(2, history.currentOccurrences(current));
        assertFalse(history.isFormalThreefold(current));
    }

    @Test
    void saturatedHalfmoveClockScansAllAvailableReversibleHistory() {
        final long[] initial = Board.fromFen("4k3/8/8/8/8/8/8/R3K3 w - - 0 1");
        final long[] saturated = Board.fromFen("4k3/8/8/8/8/8/8/R3K3 w - - 127 1");
        final GameHistory.Builder builder = GameHistory.builder(initial);
        for(int i = 0; i < 130; i ++) builder.appendPosition(initial);
        builder.appendPosition(saturated);
        final GameHistory history = builder.snapshot();

        assertEquals(132, history.currentOccurrences(saturated));
        assertTrue(history.isFormalThreefold(saturated));
    }

    @Test
    void searchLinePushPopRestoresNestedAndSiblingBranches() {
        final long[] root = Board.startingPosition();
        final GameHistory game = GameHistory.initial(root);
        final SearchLineHistory line = new SearchLineHistory(game);
        final long[] first = play(root, "g1f3");
        final long[] nested = play(first, "g8f6");
        final long[] sibling = play(first, "b8c6");

        line.pushRealPosition(first);
        line.pushRealPosition(nested);
        assertEquals(game.size() + 2, line.size());
        assertEquals(1, line.currentOccurrences(nested));

        line.popRealPosition();
        assertEquals(PositionIdentity.repetitionKey(first), line.currentKey());
        line.pushRealPosition(sibling);
        assertEquals(1, line.currentOccurrences(sibling));
        line.popRealPosition();
        line.popRealPosition();

        assertEquals(line.rootSize(), line.size());
        assertEquals(PositionIdentity.repetitionKey(root), line.currentKey());
        assertThrows(IllegalStateException.class, line::popRealPosition);
    }

    @Test
    void gamePrefixAndSearchLineCombineForFormalThreefoldButNotTwofold() {
        long[] board = Board.startingPosition();
        final GameHistory.Builder builder = GameHistory.builder(board);
        board = playAndAppend(builder, board, "g1f3");
        board = playAndAppend(builder, board, "g8f6");
        board = playAndAppend(builder, board, "f3g1");
        board = playAndAppend(builder, board, "f6g8");
        final SearchLineHistory line = new SearchLineHistory(builder.snapshot());

        board = play(board, "g1f3");
        line.pushRealPosition(board);
        assertEquals(2, line.currentOccurrences(board));
        assertFalse(line.isFormalThreefold(board));

        board = play(board, "g8f6");
        line.pushRealPosition(board);
        assertFalse(line.isFormalThreefold(board));
        board = play(board, "f3g1");
        line.pushRealPosition(board);
        assertFalse(line.isFormalThreefold(board));
        board = play(board, "f6g8");
        line.pushRealPosition(board);

        assertEquals(3, line.currentOccurrences(board));
        assertTrue(line.isFormalThreefold(board));
        line.restoreRoot();
        assertEquals(line.rootSize(), line.size());
    }

    @Test
    void snapshotsAreIndependentAndStorageGrowsWithoutFixedLimit() {
        final long[] board = Board.startingPosition();
        final GameHistory.Builder builder = GameHistory.builder(board);
        for(int i = 0; i < 700; i ++) builder.appendPosition(board);
        final GameHistory first = builder.snapshot();
        builder.appendPosition(board);
        final GameHistory second = builder.snapshot();

        assertEquals(701, first.size());
        assertEquals(702, second.size());

        final SearchLineHistory line = new SearchLineHistory(first);
        for(int i = 0; i < 700; i ++) line.pushRealPosition(board);
        assertEquals(1401, line.size());
        line.restoreRoot();
        assertEquals(first.size(), line.size());
    }

    @Test
    void nullTransitionIsNotPushedAsAnActualPosition() {
        final long[] root = Board.fromFen("4k3/8/8/8/3pP3/8/8/4K3 b - e3 12 1");
        final SearchLineHistory line = new SearchLineHistory(GameHistory.initial(root));
        final long[] nullBoard = new long[Board.MAX_BITBOARDS];

        Board.nullMoveInto(
            root[0], root[1], root[2], root[3],
            (int) root[Board.STATUS], root[Board.KEY], nullBoard
        );

        assertEquals(line.rootSize(), line.size());
        assertThrows(IllegalArgumentException.class, () -> line.currentOccurrences(nullBoard));
    }

    @Test
    void currentBoardMismatchIsRejected() {
        final GameHistory history = GameHistory.initial(Board.startingPosition());
        final long[] other = Board.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1");

        assertFalse(history.matchesCurrent(other));
        assertThrows(IllegalArgumentException.class, () -> history.requireCurrent(other));

        final long[] noEp = Board.fromFen("4k3/8/8/8/4P3/8/8/4K3 b - - 0 1");
        final long[] unusableEp = Board.fromFen("4k3/8/8/8/4P3/8/8/4K3 b - e3 0 1");
        final GameHistory concrete = GameHistory.initial(noEp);
        assertEquals(
            PositionIdentity.repetitionKey(noEp),
            PositionIdentity.repetitionKey(unusableEp)
        );
        assertFalse(concrete.matchesCurrent(unusableEp));
    }

    private static long[] playAndAppend(
        GameHistory.Builder builder, long[] board, String coordinate
    ) {
        final long[] child = play(board, coordinate);
        builder.appendPosition(child);
        return child;
    }

    private static long[] play(long[] board, String coordinate) {
        final int from = square(coordinate.substring(0, 2));
        final int to = square(coordinate.substring(2, 4));
        final long move = new LegalMoveResolver().resolve(
            board, new MoveIntent(from, to, MoveIntent.Promotion.NONE)
        );
        final long[] child = new long[Board.MAX_BITBOARDS];
        Board.makeMoveInto(
            board[0], board[1], board[2], board[3],
            (int) board[Board.STATUS], board[Board.KEY], move, child
        );
        return child;
    }

    private static int square(String coordinate) {
        return (coordinate.charAt(1) - '1') * 8 + coordinate.charAt(0) - 'a';
    }
}
