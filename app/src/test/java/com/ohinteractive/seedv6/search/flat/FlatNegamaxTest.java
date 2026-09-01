package com.ohinteractive.seedv6.search.flat;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.LegalMoveResolver;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.move.MoveIntent;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlatNegamaxTest {

    @Test
    void searchRequestRejectsDepthZero() {
        assertThrows(IllegalArgumentException.class, () -> new SearchRequest(Board.startingPosition(), 0));
    }

    @Test
    void searchRequestValidatesRequiredInputsAndLeavesUpperBoundToSearch() {
        assertThrows(NullPointerException.class, () -> new SearchRequest(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new SearchRequest(new long[5], 1));
        assertThrows(
            NullPointerException.class,
            () -> new SearchRequest(Board.startingPosition(), 1, null)
        );

        final SearchRequest implementationLimited = new SearchRequest(Board.startingPosition(), 65);
        assertThrows(IllegalArgumentException.class, () -> new FlatNegamax().search(implementationLimited));
        assertThrows(NullPointerException.class, () -> new FlatNegamax().search(null));
    }

    @Test
    void searchRequestDefensivelySnapshotsFirstSixBoardLongs() {
        final long[] supplied = Arrays.copyOf(Board.startingPosition(), Board.MAX_BITBOARDS + 2);
        supplied[Board.MAX_BITBOARDS] = 91L;
        supplied[Board.MAX_BITBOARDS + 1] = 92L;
        final long[] expected = Arrays.copyOf(supplied, Board.MAX_BITBOARDS);

        final SearchRequest request = new SearchRequest(supplied, 1);
        Arrays.fill(supplied, 0L);
        final long[] copied = new long[Board.MAX_BITBOARDS + 2];
        request.copyBoardInto(copied);

        assertArrayEquals(expected, Arrays.copyOf(copied, Board.MAX_BITBOARDS));
        assertEquals(0L, copied[Board.MAX_BITBOARDS]);
        assertEquals(0L, copied[Board.MAX_BITBOARDS + 1]);
    }

    @Test
    void startingPositionDepthOneContract() {
        final SearchResult result = search(Board.startingPosition(), 1);

        assertEquals(1, result.depth());
        assertEquals(20L, result.nodes());
        assertEquals(20, result.legalRootMoves());
        assertTrue(result.hasMove());
        assertTrue(result.completed());
    }

    @Test
    void startingPositionDepthTwoCountsEveryEnteredChild() {
        assertEquals(420L, search(Board.startingPosition(), 2).nodes());
    }

    @Test
    void startingPositionDepthThreeCountsEveryEnteredChild() {
        assertEquals(9322L, search(Board.startingPosition(), 3).nodes());
    }

    @Test
    void searchPreservesCallerBoardAndIsSequentiallyDeterministic() {
        final long[] board = Board.startingPosition();
        final long[] original = board.clone();
        final FlatNegamax search = new FlatNegamax();

        final SearchResult first = search.search(new SearchRequest(board, 2));
        assertArrayEquals(original, board);
        final SearchResult second = search.search(new SearchRequest(board, 2));
        assertArrayEquals(original, board);

        assertNotSame(first, second);
        assertEquals(first.score(), second.score());
        assertEquals(first.bestMove(), second.bestMove());
        assertEquals(first.legalRootMoves(), second.legalRootMoves());
        assertEquals(first.nodes(), second.nodes());
    }

    @Test
    void rootCheckmateHasExplicitNoMoveResult() {
        final SearchResult result = search(
            Board.fromFen("7k/6Q1/5K2/8/8/8/8/8 b - - 0 1"),
            3
        );

        assertEquals(-32768, result.score());
        assertFalse(result.hasMove());
        assertEquals(0L, result.bestMove());
        assertEquals(0, result.legalRootMoves());
        assertEquals(0L, result.nodes());
        assertTrue(result.completed());
        assertEquals(3, result.depth());
    }

    @Test
    void rootStalemateHasExplicitNoMoveResult() {
        final SearchResult result = search(
            Board.fromFen("7k/5K2/6Q1/8/8/8/8/8 b - - 0 1"),
            2
        );

        assertEquals(0, result.score());
        assertFalse(result.hasMove());
        assertEquals(0L, result.bestMove());
        assertEquals(0, result.legalRootMoves());
        assertEquals(0L, result.nodes());
        assertTrue(result.completed());
        assertEquals(2, result.depth());
    }

    @Test
    void mateInOneUsesRootRelativeMateDistance() {
        final long[] board = Board.fromFen("7k/8/5KQ1/8/8/8/8/8 w - - 0 1");
        final SearchResult result = search(board, 1);

        assertEquals(32767, result.score());
        assertTrue(result.hasMove());
        assertEquals(result.legalRootMoves(), result.nodes());
        assertEquals(result.bestMove(), new LegalMoveResolver().resolve(board, intentOf(result.bestMove())));
    }

    @Test
    void forcedMateAtPlyTwoUsesRootRelativeMateDistance() {
        final long[] board = Board.fromFen("7k/p7/5KQ1/8/8/8/8/8 b - - 0 1");
        final SearchResult result = search(board, 2);

        assertEquals(-32766, result.score());
        assertTrue(result.hasMove());
    }

    @Test
    void searchResultEnforcesStructuralInvariantsButAllowsIncompleteResults() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SearchResult(0L, false, 0, 0, 0L, 0, true)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new SearchResult(0L, false, 0, 1, 0L, -1, true)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new SearchResult(1L, false, 0, 1, 0L, 0, true)
        );

        final SearchResult incomplete = new SearchResult(0L, false, 0, 1, 0L, 0, false);
        assertFalse(incomplete.completed());
    }

    @Test
    void finishedObserverReceivesReturnedImmutableResultInstance() {
        final AtomicReference<SearchResult> observed = new AtomicReference<>();
        final SearchObserver observer = new SearchObserver() {
            @Override
            public void onSearchFinished(SearchResult result, long elapsedNanos) {
                observed.set(result);
            }
        };

        final SearchResult returned = new FlatNegamax().search(
            new SearchRequest(Board.startingPosition(), 1, observer)
        );

        assertSame(returned, observed.get());
    }

    @Test
    void observerExceptionPropagatesAndSearchInstanceRemainsReusable() {
        final RuntimeException expected = new RuntimeException("observer failure");
        final SearchObserver failingObserver = new SearchObserver() {
            @Override
            public void onRootMoveStarted(int index, int total, long move) {
                throw expected;
            }
        };
        final FlatNegamax search = new FlatNegamax();

        final RuntimeException actual = assertThrows(
            RuntimeException.class,
            () -> search.search(new SearchRequest(Board.startingPosition(), 1, failingObserver))
        );
        assertSame(expected, actual);

        final SearchResult recovered = search.search(new SearchRequest(Board.startingPosition(), 1));
        assertTrue(recovered.completed());
        assertEquals(20L, recovered.nodes());
        assertEquals(20, recovered.legalRootMoves());
    }

    @Test
    void returnedBestMoveBelongsToAuthoritativeLegalRootSet() {
        final long[] board = Board.fromFen(
            "r3k2r/p1ppqpb1/bn2pnp1/2pP4/1p2P3/2N2N2/PPQBBPPP/R3K2R w KQkq - 0 1"
        );
        final SearchResult result = search(board, 1);

        assertTrue(result.hasMove());
        assertEquals(result.bestMove(), new LegalMoveResolver().resolve(board, intentOf(result.bestMove())));
    }

    @Test
    void flatSearchMatchesIndependentRecursiveOracle() {
        assertMatchesOracle(Board.startingPosition(), 1);
        assertMatchesOracle(Board.startingPosition(), 2);
        assertMatchesOracle(Board.fromFen("4k3/8/8/3p4/4P3/8/8/4K3 w - - 0 1"), 2);
        assertMatchesOracle(Board.fromFen("4k3/8/8/8/8/8/4r3/4K3 w - - 0 1"), 2);
        assertMatchesOracle(Board.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"), 2);
        assertMatchesOracle(Board.fromFen("7k/6Q1/5K2/8/8/8/8/8 b - - 0 1"), 2);
        assertMatchesOracle(Board.fromFen("7k/5K2/6Q1/8/8/8/8/8 b - - 0 1"), 2);
        assertMatchesOracle(Board.fromFen("7k/8/5KQ1/8/8/8/8/8 w - - 0 1"), 1);
        assertMatchesOracle(Board.fromFen("7k/p7/5KQ1/8/8/8/8/8 b - - 0 1"), 2);
    }

    private static SearchResult search(long[] board, int depth) {
        return new FlatNegamax().search(new SearchRequest(board, depth));
    }

    private static MoveIntent intentOf(long move) {
        return new MoveIntent(Move.fromSquare(move), Move.toSquare(move), Move.promotion(move));
    }

    private static void assertMatchesOracle(long[] board, int depth) {
        final SearchResult actual = search(board, depth);
        final ExactNegamaxOracle.Result expected = ExactNegamaxOracle.search(board, depth);
        assertEquals(expected.score(), actual.score());
        assertEquals(expected.hasMove(), actual.hasMove());
        assertEquals(expected.legalRootMoves(), actual.legalRootMoves());
    }

}
