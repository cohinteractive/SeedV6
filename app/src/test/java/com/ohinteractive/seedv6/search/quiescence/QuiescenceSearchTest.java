package com.ohinteractive.seedv6.search.quiescence;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuiescenceSearchTest {

    private static final int NEG_INF = -1_000_000_000;
    private static final int POS_INF = 1_000_000_000;

    @Test
    void windowMustBeOrderedAndSafelyNegatable() {
        final long[] board = Board.startingPosition();
        final SearchRequest request = new SearchRequest(board, 1);
        final QuiescenceSearch search = new QuiescenceSearch();
        assertThrows(IllegalArgumentException.class, () -> search.search(request, 0, 0));
        assertThrows(
            IllegalArgumentException.class,
            () -> search.search(request, Integer.MIN_VALUE, POS_INF)
        );
    }

    @Test
    void checkedNodesSearchQuietKingMovesInterpositionsAndOtherNonCaptures() {
        final List<String> fens = List.of(
            "4r1k1/8/8/8/8/8/8/4K3 w - - 0 1",
            "4r1k1/8/8/8/8/8/8/2B1K3 w - - 0 1",
            "4r1k1/8/8/8/8/2N5/8/4K3 w - - 0 1"
        );
        final List<String> mandatoryQuietMoves = List.of("e1d1", "c1e3", "c3e2");

        for(int fixture = 0; fixture < fens.size(); fixture ++) {
            final long[] board = Board.fromFen(fens.get(fixture));
            final long[] evasions = evasions(board);
            final long required = find(evasions, mandatoryQuietMoves.get(fixture));
            assertFalse(
                com.ohinteractive.seedv6.search.order.MoveOrdering.isTactical(board, required),
                fens.get(fixture)
            );

            final QuiescenceSearch search = new QuiescenceSearch();
            final QuiescenceSearch.Result result = searchAt(
                search, board, GameHistory.initial(board), 0,
                QuiescenceSearch.SOFT_QPLY_LIMIT, NEG_INF, POS_INF
            );
            assertTrue(result.completed());
            assertEquals(evasions.length, result.nodes(), fens.get(fixture));
            assertEquals(
                QuiescenceOracle.search(
                    board, GameHistory.initial(board), 0,
                    QuiescenceSearch.SOFT_QPLY_LIMIT, NEG_INF, POS_INF
                ),
                result.score(), fens.get(fixture)
            );
        }
    }

    @Test
    void mateAtEntryAndAfterTacticalQpliesUsesAbsolutePlyForBothColours() {
        final String blackMated = "7k/6Q1/5K2/8/8/8/8/8 b - - 0 1";
        final String whiteMated = "7K/6q1/5k2/8/8/8/8/8 w - - 0 1";
        for(String fen : List.of(blackMated, whiteMated)) {
            final long[] board = Board.fromFen(fen);
            assertEquals(-TranspositionScores.MATE_SCORE, score(board));
            final int atPlySeven = searchAt(
                new QuiescenceSearch(), board, GameHistory.initial(board), 7, 0,
                NEG_INF, POS_INF
            ).score();
            assertEquals(-TranspositionScores.MATE_SCORE + 7, atPlySeven);
            assertTrue(TranspositionScores.isMateScore(atPlySeven));
        }

        final List<String> matingCaptures = List.of(
            "7k/6p1/5KQ1/8/8/8/8/8 w - - 0 1",
            "8/8/8/8/8/5kq1/6P1/7K b - - 0 1"
        );
        for(String fen : matingCaptures) {
            final long[] board = Board.fromFen(fen);
            final int rootScore = score(board);
            assertEquals(TranspositionScores.MATE_SCORE - 1, rootScore, fen);
            final int atPlyFive = searchAt(
                new QuiescenceSearch(), board, GameHistory.initial(board), 5, 0,
                NEG_INF, POS_INF
            ).score();
            assertEquals(TranspositionScores.MATE_SCORE - 6, atPlyFive, fen);
        }
    }

    @Test
    void checkmatePrecedesFiftyMoveAndStaticEvaluation() {
        final long[] mateAtRuleBoundary = Board.fromFen(
            "7k/6Q1/5K2/8/8/8/8/8 b - - 100 1"
        );
        assertNotEquals(0, Eval.evaluate(mateAtRuleBoundary));
        assertEquals(-TranspositionScores.MATE_SCORE, score(mateAtRuleBoundary));
    }

    @Test
    void nonCheckStandPatIsFailSoftAndStalemateIsTerminal() {
        final long[] quiet = Board.startingPosition();
        final int standPat = Eval.evaluate(quiet);
        final QuiescenceSearch search = new QuiescenceSearch();

        QuiescenceSearch.Result result = search.search(
            new SearchRequest(quiet, 1), standPat - 100, standPat + 100
        );
        assertTrue(result.completed());
        assertEquals(standPat, result.score());
        assertEquals(0L, result.nodes());

        result = search.search(new SearchRequest(quiet, 1), standPat - 200, standPat - 1);
        assertEquals(standPat, result.score(), "fail-soft stand-pat cutoff");
        assertEquals(0L, result.nodes());

        final long[] poisoned = Board.fromFen(
            "3rk3/8/8/3p4/8/8/8/3QK3 w - - 0 1"
        );
        assertEquals(Eval.evaluate(poisoned), score(poisoned));

        final long[] stalemate = Board.fromFen(
            "7k/5K2/6Q1/8/8/8/8/8 b - - 0 1"
        );
        assertNotEquals(0, Eval.evaluate(stalemate));
        assertEquals(0, score(stalemate));
    }

    @Test
    void capturesXraysPinsAndKingRecapturesMatchTheIndependentOracle() {
        final List<String> positions = List.of(
            "4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1",
            "rq2k3/8/8/8/8/8/8/R3K3 w - - 0 1",
            "3rk3/8/8/3p4/8/8/8/3QK3 w - - 0 1",
            "4k3/6b1/8/8/3p4/8/3Q4/3RK3 w - - 0 1",
            "4k3/8/6b1/8/8/3r4/2Q5/1B2K3 w - - 0 1",
            "4r1k1/6b1/8/8/3p4/2P5/4N3/4K3 w - - 0 1",
            "8/8/4k3/3r4/8/8/8/3RK3 w - - 0 1",
            "8/8/4k3/3r4/8/1B6/8/3RK3 w - - 0 1"
        );
        for(String fen : positions) assertMatchesOracle(fen);

        final long[] winningCapture = Board.fromFen(positions.get(0));
        final int exact = score(winningCapture);
        final int beta = Eval.evaluate(winningCapture) + 50;
        final int cutoff = new QuiescenceSearch().search(
            new SearchRequest(winningCapture, 1), NEG_INF, beta
        ).score();
        assertTrue(cutoff >= beta);
        assertEquals(exact, cutoff, "tactical fail-high returns the actual fail-soft value");
    }

    @Test
    void enPassantLegalityOccupancyAndContinuationMatchOracle() {
        final List<String> positions = List.of(
            "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
            "4k3/8/8/3pP3/3r4/8/8/4K3 w - d6 0 1",
            "4k3/8/8/3R4/3Pp3/8/8/4K3 b - d3 0 1",
            "8/5bk1/8/2Pp4/8/1K6/8/8 w - d6 0 1"
        );
        for(String fen : positions) assertMatchesOracle(fen);

        final long[] continuation = Board.fromFen(positions.get(1));
        assertTrue(result(continuation).nodes() >= 2L, "EP must reach the rook continuation");
        final long[] kingSensitive = Board.fromFen(positions.get(3));
        assertEquals(0, tacticalMoves(kingSensitive).length, "illegal EP must not be exposed");
    }

    @Test
    void allPromotionTypesForBothColoursRemainTacticalAndSearchable() {
        final List<String> positions = List.of(
            "7k/P7/8/8/8/8/8/K7 w - - 0 1",
            "k7/8/8/8/8/8/p7/7K b - - 0 1",
            "4k2r/6P1/8/8/8/8/8/4K3 w - - 0 1",
            "4k3/8/8/8/8/8/6p1/4K2R b - - 0 1"
        );
        for(String fen : positions) {
            final long[] board = Board.fromFen(fen);
            final long[] tacticals = tacticalMoves(board);
            for(char suffix : new char[] {'q', 'r', 'b', 'n'}) {
                assertTrue(
                    Arrays.stream(tacticals).anyMatch(
                        move -> Move.coordinate(move).endsWith(Character.toString(suffix))
                    ),
                    fen + " suffix=" + suffix
                );
            }
            assertMatchesOracle(fen);
            assertTrue(result(board).nodes() >= 4L, fen);
        }

        final long[] quietPromotion = Board.fromFen(positions.get(0));
        final int queenChild = -childScore(quietPromotion, "a7a8q");
        final int knightChild = -childScore(quietPromotion, "a7a8n");
        assertNotEquals(queenChild, knightChild, "underpromotion must remain score-distinct");

        final long[] promotionEvasion = Board.fromFen(
            "k6r/6P1/8/8/8/8/8/7K w - - 0 1"
        );
        final long[] evasions = evasions(promotionEvasion);
        for(char suffix : new char[] {'q', 'r', 'b', 'n'}) {
            assertTrue(
                Arrays.stream(evasions).anyMatch(
                    move -> Move.coordinate(move).equals("g7h8" + suffix)
                ),
                "promotion evasion " + suffix
            );
        }
        assertMatchesOracle("k6r/6P1/8/8/8/8/8/7K w - - 0 1");
    }

    @Test
    void repetitionFiftyMoveMaterialDrawAndSiblingRestorationUseWs2Semantics() {
        final long[] checkedRoot = Board.fromFen(
            "4r1k1/8/8/8/8/8/8/4K3 w - - 20 1"
        );
        final long repeatedMove = find(evasions(checkedRoot), "e1d1");
        final long[] repeatedChild = apply(checkedRoot, repeatedMove);
        final GameHistory repeatedHistory = GameHistory.builder(repeatedChild)
            .appendPosition(repeatedChild)
            .appendPosition(checkedRoot)
            .snapshot();
        final SearchLineHistory line = new SearchLineHistory(repeatedHistory);
        final int initialSize = line.size();
        final long initialTop = line.currentKey();
        final QuiescenceSearch search = new QuiescenceSearch();
        final int repeated = search.searchLeaf(
            checkedRoot, line, SearchControl.unlimited(), 0, NEG_INF, POS_INF
        ).score();
        assertEquals(0, repeated);
        assertEquals(initialSize, line.size());
        assertEquals(initialTop, line.currentKey());
        assertEquals(repeated, search.searchLeaf(
            checkedRoot, line, SearchControl.unlimited(), 0, NEG_INF, POS_INF
        ).score());
        assertEquals(initialSize, line.size());

        assertEquals(0, score(Board.fromFen(
            "4k3/8/8/8/8/8/8/Q3K3 w - - 100 1"
        )));
        assertEquals(0, score(Board.fromFen(
            "4r1k1/8/8/8/8/8/8/4K3 w - - 99 1"
        )), "every quiet evasion reaches the halfmove boundary inside qsearch");
        assertEquals(0, score(Board.fromFen(
            "4k3/8/8/8/8/8/8/2B1K3 w - - 0 1"
        )));

        for(int index = 0; index < repeatedHistory.size(); index ++) {
            final long expected = index < 2
                ? repeatedHistory.keyAt(0) : repeatedHistory.currentKey();
            assertEquals(expected, repeatedHistory.keyAt(index));
        }
    }

    @Test
    void softDepthStopsOnlyNonCheckExpansionAndAbsoluteCapacityFailsCleanly() {
        final long[] tactic = Board.fromFen(
            "4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1"
        );
        final int standPat = Eval.evaluate(tactic);
        final QuiescenceSearch search = new QuiescenceSearch();
        final QuiescenceSearch.Result below = searchAt(
            search, tactic, GameHistory.initial(tactic), 0,
            QuiescenceSearch.SOFT_QPLY_LIMIT - 1, NEG_INF, POS_INF
        );
        assertTrue(below.nodes() > 0L);
        assertTrue(below.score() > standPat);

        final QuiescenceSearch.Result at = searchAt(
            search, tactic, GameHistory.initial(tactic), 0,
            QuiescenceSearch.SOFT_QPLY_LIMIT, NEG_INF, POS_INF
        );
        assertEquals(standPat, at.score());
        assertEquals(0L, at.nodes());
        final QuiescenceSearch.Result beyond = searchAt(
            search, tactic, GameHistory.initial(tactic), 0,
            QuiescenceSearch.SOFT_QPLY_LIMIT + 3, NEG_INF, POS_INF
        );
        assertEquals(standPat, beyond.score());
        assertEquals(0L, beyond.nodes());

        final long[] checked = Board.fromFen(
            "4r1k1/8/8/8/8/8/8/4K3 w - - 0 1"
        );
        for(int qPly : List.of(
            QuiescenceSearch.SOFT_QPLY_LIMIT,
            QuiescenceSearch.SOFT_QPLY_LIMIT + 2
        )) {
            final QuiescenceSearch.Result checkedResult = searchAt(
                search, checked, GameHistory.initial(checked), 20, qPly,
                NEG_INF, POS_INF
            );
            assertEquals(evasions(checked).length, checkedResult.nodes());
            assertEquals(
                QuiescenceOracle.search(
                    checked, GameHistory.initial(checked), 20, qPly,
                    NEG_INF, POS_INF
                ),
                checkedResult.score()
            );
        }

        final SearchLineHistory history = new SearchLineHistory(GameHistory.initial(checked));
        final int size = history.size();
        assertThrows(
            QuiescenceSearch.QuiescenceCapacityException.class,
            () -> search.searchAtQply(
                checked, history, SearchControl.unlimited(),
                QuiescenceSearch.MAX_ABSOLUTE_PLY,
                QuiescenceSearch.SOFT_QPLY_LIMIT, NEG_INF, POS_INF
            )
        );
        assertEquals(size, history.size());
        assertEquals(score(tactic), search.search(
            new SearchRequest(tactic, 1), NEG_INF, POS_INF
        ).score(), "worker must be reusable after capacity failure");
    }

    @Test
    void representativeCallsPreserveBoardKeyStatusHistoryAndDeterminism() {
        for(String fen : List.of(
            "4k3/8/8/3q4/4P3/8/8/4K3 w - - 7 19",
            "4r1k1/8/8/8/8/8/8/4K3 w - - 12 8",
            "4k3/8/8/3pP3/3r4/8/8/4K3 w - d6 4 9",
            "4k2r/6P1/8/8/8/8/8/4K3 w - - 2 11"
        )) {
            final long[] board = Board.fromFen(fen);
            final long[] before = board.clone();
            final GameHistory gameHistory = GameHistory.initial(board);
            final long historyKey = gameHistory.currentKey();
            final QuiescenceSearch search = new QuiescenceSearch();
            final int first = search.search(
                new SearchRequest(board, gameHistory, 1), NEG_INF, POS_INF
            ).score();
            final int second = search.search(
                new SearchRequest(board, gameHistory, 1), NEG_INF, POS_INF
            ).score();
            assertEquals(first, second, fen);
            assertArrayEquals(before, board, fen);
            assertEquals(before[Board.STATUS], board[Board.STATUS], fen);
            assertEquals(before[Board.KEY], board[Board.KEY], fen);
            assertEquals(historyKey, gameHistory.currentKey(), fen);
            assertEquals(1, gameHistory.size(), fen);
        }
    }

    private static QuiescenceSearch.Result result(long[] board) {
        return new QuiescenceSearch().search(new SearchRequest(board, 1), NEG_INF, POS_INF);
    }

    private static int score(long[] board) {
        return result(board).score();
    }

    private static int childScore(long[] board, String coordinate) {
        final long childMove = find(tacticalMoves(board), coordinate);
        return score(apply(board, childMove));
    }

    private static void assertMatchesOracle(String fen) {
        final long[] board = Board.fromFen(fen);
        final long[] before = board.clone();
        final int expected = QuiescenceOracle.search(board, NEG_INF, POS_INF);
        final int actual = score(board);
        assertEquals(expected, actual, fen);
        assertArrayEquals(before, board, fen);
    }

    private static QuiescenceSearch.Result searchAt(
        QuiescenceSearch search, long[] board, GameHistory gameHistory,
        int absolutePly, int qPly, int alpha, int beta
    ) {
        return search.searchAtQply(
            board, new SearchLineHistory(gameHistory), SearchControl.unlimited(),
            absolutePly, qPly, alpha, beta
        );
    }

    private static long[] tacticalMoves(long[] board) {
        final long[] moves = new long[256];
        final int count = Gen.genTactical(
            board[0], board[1], board[2], board[3],
            (int) board[Board.STATUS], board[Board.KEY], true,
            moves, new long[Board.MAX_BITBOARDS]
        );
        return Arrays.copyOf(moves, count);
    }

    private static long[] evasions(long[] board) {
        final long[] moves = new long[256];
        final int status = (int) board[Board.STATUS];
        final int player = status & Board.PLAYER_BIT;
        final long occupancy = board[0] | board[1] | board[2];
        final long colour = ~(-(long) player ^ board[3]);
        final long king = board[0] & ~board[1] & ~board[2] & colour;
        final long checkers = Board.getCheckersPext(
            board[0], board[1], board[2], board[3], colour, player,
            Long.numberOfTrailingZeros(king), occupancy
        );
        assertNotEquals(0L, checkers, "fixture must be checked");
        final int count = Gen.genEvasion(
            board[0], board[1], board[2], board[3], status, board[Board.KEY],
            true, checkers, moves, new long[Board.MAX_BITBOARDS]
        );
        return Arrays.copyOf(moves, count);
    }

    private static long find(long[] moves, String coordinate) {
        return Arrays.stream(moves)
            .filter(move -> Move.coordinate(move).equals(coordinate))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing legal move " + coordinate));
    }

    private static long[] apply(long[] board, long move) {
        final long[] child = new long[Board.MAX_BITBOARDS];
        Board.makeMoveInto(
            board[0], board[1], board[2], board[3],
            (int) board[Board.STATUS], board[Board.KEY], move, child
        );
        return child;
    }
}
