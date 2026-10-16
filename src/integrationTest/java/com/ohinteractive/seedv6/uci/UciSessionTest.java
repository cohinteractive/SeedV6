package com.ohinteractive.seedv6.uci;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.LegalMoveResolver;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.flat.FlatNegamax;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UciSessionTest {

    @Test
    void startPositionSeedsHistoryAndAppendsEachResolvedRealMove() {
        final UciSession session = new UciSession();
        session.setPosition(tokens("position startpos moves e2e4 e7e5 g1f3 b8c6 f1b5"));

        assertMatchesReplay(session, Board.FEN_STARTING_POSITION, "e2e4", "e7e5", "g1f3", "b8c6", "f1b5");
        assertEquals(6, session.historySnapshot().size());
    }

    @Test
    void fullFenIsTheFirstHistoryEntryBeforeOptionalMoves() {
        final String fen = "4k3/8/8/8/8/8/4P3/4K3 w - - 37 9";
        final UciSession session = new UciSession();
        session.setPosition(tokens("position fen " + fen));

        assertArrayEquals(Board.fromFen(fen), session.boardSnapshot());
        assertEquals(1, session.historySnapshot().size());
        session.historySnapshot().requireCurrent(session.boardSnapshot());

        session.setPosition(tokens("position fen " + fen + " moves e2e3 e8e7"));
        assertMatchesReplay(session, fen, "e2e3", "e8e7");
        assertEquals(3, session.historySnapshot().size());
    }

    @Test
    void replaysQuietCaptureDoublePushCastlingAndEnPassantThroughGeneratedMoves() {
        assertReplay(Board.FEN_STARTING_POSITION, "e2e4");
        assertReplay("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1", "e2e3");
        assertReplay("4k3/8/8/3p4/4P3/8/8/4K3 w - - 0 1", "e4d5");

        final UciSession kingSide = assertReplay("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "e1g1");
        assertEquals(Piece.KING, pieceAt(kingSide.boardSnapshot(), "g1"));
        assertEquals(Piece.ROOK, pieceAt(kingSide.boardSnapshot(), "f1"));

        final UciSession queenSide = assertReplay("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "e1c1");
        assertEquals(Piece.KING, pieceAt(queenSide.boardSnapshot(), "c1"));
        assertEquals(Piece.ROOK, pieceAt(queenSide.boardSnapshot(), "d1"));

        final UciSession enPassant = assertReplay("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1", "e5d6");
        assertEquals(Piece.PAWN, pieceAt(enPassant.boardSnapshot(), "d6"));
        assertEquals(0, pieceAt(enPassant.boardSnapshot(), "d5"));
    }

    @Test
    void replaysEveryPromotionKindForBothColoursIncludingCapturesAndUnderpromotion() {
        for(char suffix : new char[] {'q', 'r', 'b', 'n'}) {
            assertPromotion("7k/P7/8/8/8/8/8/7K w - - 0 1", "a7a8" + suffix, "a8", false, suffix);
            assertPromotion("7k/8/8/8/8/8/p7/7K b - - 0 1", "a2a1" + suffix, "a1", true, suffix);
        }
        assertPromotion("1r5k/P7/8/8/8/8/8/7K w - - 0 1", "a7b8n", "b8", false, 'n');
        assertPromotion("7k/8/8/8/8/8/p7/1R5K b - - 0 1", "a2b1r", "b1", true, 'r');
    }

    @Test
    void everyFailedCandidateLeavesPreviouslyPublishedBoardAndHistoryUntouched() {
        final UciSession session = new UciSession();
        session.setPosition(tokens("position startpos moves e2e4 e7e5"));
        final long[] acceptedBoard = session.boardSnapshot();
        final GameHistory acceptedHistory = session.historySnapshot();

        assertRejectedWithoutChange(session, acceptedBoard, acceptedHistory, "position fen broken w - - 0 1");
        assertRejectedWithoutChange(session, acceptedBoard, acceptedHistory, "position startpos moves e2e5");
        assertRejectedWithoutChange(session, acceptedBoard, acceptedHistory, "position startpos moves e2e4 e7e5 e1e5");
        assertRejectedWithoutChange(session, acceptedBoard, acceptedHistory, "position fen 7k/P7/8/8/8/8/8/7K w - - 0 1 moves a7a8k");
        assertRejectedWithoutChange(session, acceptedBoard, acceptedHistory, "position startpos moves i2e4");
        assertRejectedWithoutChange(session, acceptedBoard, acceptedHistory, "position startpos moves e2i4");
        assertRejectedWithoutChange(session, acceptedBoard, acceptedHistory, "position fen 4k3/8/8/8/8/8/4P3/4K3 w - - 0");
    }

    @Test
    void uciBuiltRepetitionHistoryReachesTheAcceptedRuleAwareSearch() {
        final UciSession session = new UciSession();
        session.setPosition(tokens(
            "position startpos moves g1f3 g8f6 f3g1 f6g8 g1f3 g8f6 f3g1 f6g8"
        ));
        final long[] board = session.boardSnapshot();
        final GameHistory history = session.historySnapshot();

        assertEquals(9, history.size());
        assertTrue(history.isFormalThreefold(board));
        final SearchResult result = search(session, 1);
        assertTrue(result.hasMove());
        assertEquals(0, result.score());
        assertEquals(0L, result.nodes());
        assertArrayEquals(board, session.boardSnapshot());
        history.requireCurrent(session.boardSnapshot());
    }

    @Test
    void resetClearsPositionHistoryAndReusableSearchState() {
        final UciSession session = new UciSession();
        session.setPosition(tokens("position fen 7k/6Q1/5K2/8/8/8/8/8 b - - 0 1"));
        assertTrue(!search(session, 1).hasMove());

        session.reset();

        assertArrayEquals(Board.startingPosition(), session.boardSnapshot());
        assertEquals(1, session.historySnapshot().size());
        assertTrue(search(session, 1).hasMove());
    }

    private static UciSession assertReplay(String fen, String... moves) {
        final UciSession session = new UciSession();
        session.setPosition(tokens("position fen " + fen + " moves " + String.join(" ", moves)));
        assertMatchesReplay(session, fen, moves);
        return session;
    }

    private static void assertMatchesReplay(UciSession session, String fen, String... moves) {
        long[] expected = Board.fromFen(fen);
        final LegalMoveResolver resolver = new LegalMoveResolver();
        for(String coordinate : moves) {
            final long move = resolver.resolve(expected, UciMoveParser.parse(coordinate));
            final long[] child = new long[Board.MAX_BITBOARDS];
            Board.makeMoveInto(
                expected[0], expected[1], expected[2], expected[3],
                (int) expected[Board.STATUS], expected[Board.KEY], move, child
            );
            expected = child;
        }
        assertArrayEquals(expected, session.boardSnapshot());
        assertEquals(moves.length + 1, session.historySnapshot().size());
        session.historySnapshot().requireCurrent(session.boardSnapshot());
    }

    private static void assertPromotion(
        String fen, String move, String target, boolean black, char suffix
    ) {
        final UciSession session = assertReplay(fen, move);
        final int type = switch(suffix) {
            case 'q' -> Piece.QUEEN;
            case 'r' -> Piece.ROOK;
            case 'b' -> Piece.BISHOP;
            case 'n' -> Piece.KNIGHT;
            default -> throw new IllegalArgumentException();
        };
        assertEquals(type | (black ? 8 : 0), pieceAt(session.boardSnapshot(), target));
    }

    private static void assertRejectedWithoutChange(
        UciSession session, long[] acceptedBoard, GameHistory acceptedHistory, String command
    ) {
        assertThrows(IllegalArgumentException.class, () -> session.setPosition(tokens(command)));
        assertArrayEquals(acceptedBoard, session.boardSnapshot());
        assertHistoryEquals(acceptedHistory, session.historySnapshot());
        session.historySnapshot().requireCurrent(session.boardSnapshot());
    }

    private static void assertHistoryEquals(GameHistory expected, GameHistory actual) {
        assertEquals(expected.size(), actual.size());
        for(int i = 0; i < expected.size(); i ++) assertEquals(expected.keyAt(i), actual.keyAt(i));
    }

    private static int pieceAt(long[] board, String coordinate) {
        return Board.getSquare(board[0], board[1], board[2], board[3], square(coordinate));
    }

    private static int square(String coordinate) {
        return (coordinate.charAt(1) - '1') * 8 + coordinate.charAt(0) - 'a';
    }

    private static String[] tokens(String command) {
        return command.trim().split("\\s+");
    }

    private static SearchResult search(UciSession session, int depth) {
        final UciSession.PositionState position = session.snapshot();
        return new FlatNegamax().search(
            new SearchRequest(position.board, position.history, depth)
        );
    }
}
