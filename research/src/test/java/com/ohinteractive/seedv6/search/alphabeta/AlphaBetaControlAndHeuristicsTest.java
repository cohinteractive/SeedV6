package com.ohinteractive.seedv6.search.alphabeta;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.LegalMoveResolver;
import com.ohinteractive.seedv6.core.move.MoveIntent;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch.Configuration;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.order.MoveOrdering;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import com.ohinteractive.seedv6.search.tt.TranspositionTable;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Bound;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Cacheability;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Probe;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlphaBetaControlAndHeuristicsTest {

    private static final int NEGATIVE_INFINITY = -TranspositionScores.MATE_SCORE - 1;

    @Test
    void qualifyingQuietCutoffUpdatesHistoryAndKillerOnlyAfterCompletion() {
        final long[] board = Board.startingPosition();
        final AlphaBetaPvsSearch search = search(false, false);
        final SearchResult result = search.search(
            new SearchRequest(board, 1), NEGATIVE_INFINITY, -TranspositionScores.MATE_SCORE
        );
        assertTrue(result.completed());
        assertFalse(MoveOrdering.isTactical(board, result.bestMove()));
        assertEquals(result.bestMove(), search.ordering().killer(0, 0));
        assertEquals(1, search.ordering().historyScore(result.bestMove()));
        assertEquals(1L, search.statistics().quietCutoffUpdates());
    }

    @Test
    void tacticalCutoffAndFailLowDoNotUpdateQuietHeuristics() {
        final long[] tactical = Board.fromFen(
            "4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1"
        );
        final AlphaBetaPvsSearch tacticalSearch = search(false, false);
        final SearchResult tacticalResult = tacticalSearch.search(
            new SearchRequest(tactical, 1), NEGATIVE_INFINITY,
            -TranspositionScores.MATE_SCORE
        );
        assertTrue(MoveOrdering.isTactical(tactical, tacticalResult.bestMove()));
        assertEquals(0L, tacticalSearch.ordering().killer(0, 0));
        assertEquals(0L, tacticalSearch.statistics().quietCutoffUpdates());

        final long[] quiet = Board.startingPosition();
        final int exact = search(false, false).search(new SearchRequest(quiet, 1)).score();
        final AlphaBetaPvsSearch failLow = search(false, false);
        final SearchResult upper = failLow.search(
            new SearchRequest(quiet, 1), exact, exact + 1
        );
        assertEquals(exact, upper.score());
        assertEquals(0L, failLow.ordering().killer(0, 0));
        assertEquals(0L, failLow.statistics().quietCutoffUpdates());
    }

    @Test
    void pvsResearchDoesNotByItselfCreateARootQuietCutoff() {
        final AlphaBetaPvsSearch search = search(true, false);
        final SearchResult result = search.search(new SearchRequest(Board.startingPosition(), 3));
        assertTrue(result.completed());
        assertTrue(search.statistics().pvsResearches() > 0L);
        assertEquals(0L, search.ordering().killer(0, 0));
    }

    @Test
    void interiorTtCutoffDoesNotFabricateAQuietUpdate() {
        final long[] board = Board.fromFen(
            "4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1"
        );
        final long tactical = resolve(board, "e4d5");
        final long[] child = apply(board, tactical);
        final int childScore = AlphaBetaReference.search(
            child, GameHistory.initial(child), 1
        ).score();
        final int parentScore = -childScore;

        final TranspositionTable table = new TranspositionTable(1 << 12);
        table.store(
            child[Board.KEY], 1, Bound.EXACT, childScore, 1, 0L,
            Cacheability.POSITION_ONLY
        );
        table.store(
            board[Board.KEY], 0, Bound.EXACT, 0, 0, tactical,
            Cacheability.POSITION_ONLY
        );
        final AlphaBetaPvsSearch search = new AlphaBetaPvsSearch(
            table, new Configuration(true, true, true)
        );
        final SearchResult result = search.search(
            new SearchRequest(board, 2), parentScore - 1, parentScore
        );
        assertEquals(parentScore, result.score());
        assertEquals(tactical, result.bestMove());
        assertTrue(search.statistics().ttCutoffs() > 0L);
        assertEquals(0L, search.statistics().quietCutoffUpdates());
        assertEquals(0L, search.ordering().killer(0, 0));
    }

    @Test
    void cancellationBeforeRootInsideMainAndInsideQsearchRestoresAllOwnedState() {
        final long[] start = Board.startingPosition();
        final long[] before = start.clone();
        final GameHistory gameHistory = GameHistory.initial(start);

        final SearchControl preCancelled = controlled(-1L);
        preCancelled.request(SearchTermination.STOPPED);
        final AlphaBetaPvsSearch beforeRoot = search(true, true);
        final SearchResult stopped = beforeRoot.search(
            new SearchRequest(start, gameHistory, 3, preCancelled)
        );
        assertFalse(stopped.completed());
        assertEquals(0L, stopped.nodes());
        assertBalanced(beforeRoot, gameHistory);

        final SearchControl mainLimit = controlled(1L);
        final AlphaBetaPvsSearch inMain = search(true, true);
        final SearchResult mainStopped = inMain.search(
            new SearchRequest(start, gameHistory, 3, mainLimit)
        );
        assertFalse(mainStopped.completed());
        assertEquals(1L, mainStopped.nodes());
        assertEquals(1L, mainLimit.nodes());
        assertBalanced(inMain, gameHistory);

        final long[] tactical = Board.fromFen(
            "4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1"
        );
        final SearchControl qLimit = controlled(0L);
        final AlphaBetaPvsSearch inQsearch = search(true, true);
        final SearchResult qStopped = inQsearch.search(
            new SearchRequest(tactical, GameHistory.initial(tactical), 0, qLimit)
        );
        assertFalse(qStopped.completed());
        assertEquals(0L, qStopped.nodes());
        assertEquals(SearchTermination.NODE_LIMIT, qLimit.termination());
        assertBalanced(inQsearch, GameHistory.initial(tactical));

        assertArrayEquals(before, start);
        assertEquals(1, gameHistory.size());
        gameHistory.requireCurrent(start);
    }

    @Test
    void nodeLimitsAreCumulativeAcrossMainAndQsearchChildEntries() {
        final long[] board = Board.startingPosition();
        for(long limit : new long[] {0L, 1L, 149L}) {
            final SearchControl control = controlled(limit);
            final AlphaBetaPvsSearch search = search(true, false);
            final SearchResult result = search.search(
                new SearchRequest(board, GameHistory.initial(board), 2, control)
            );
            assertFalse(result.completed(), "limit=" + limit);
            assertEquals(limit, result.nodes(), "limit=" + limit);
            assertEquals(limit, control.nodes(), "limit=" + limit);
            assertEquals(
                result.nodes(),
                search.statistics().mainChildEntries()
                    + search.statistics().qsearchChildEntries()
            );
            assertBalanced(search, GameHistory.initial(board));
        }

        final SearchControl exact = controlled(150L);
        final AlphaBetaPvsSearch search = search(true, false);
        final SearchResult complete = search.search(
            new SearchRequest(board, GameHistory.initial(board), 2, exact)
        );
        assertTrue(complete.completed());
        assertEquals(150L, complete.nodes());
        assertEquals(150L, exact.nodes());
    }

    @Test
    void cancellationDuringPvsResearchLeavesNoRootCompletionStore() {
        final long[] board = Board.startingPosition();
        boolean exercised = false;
        for(long limit = 1L; limit < 150L; limit ++) {
            final TranspositionTable table = new TranspositionTable(1 << 14);
            final AlphaBetaPvsSearch search = new AlphaBetaPvsSearch(
                table, new Configuration(true, true, true)
            );
            final SearchControl control = controlled(limit);
            final SearchResult result = search.search(
                new SearchRequest(board, GameHistory.initial(board), 3, control)
            );
            if(!result.completed() && search.statistics().pvsResearches() > 0L) {
                exercised = true;
                assertBalanced(search, GameHistory.initial(board));
                AlphaBetaPvsSearchTest.assertLegalPv(board, result);
                final Probe probe = new Probe();
                table.probe(
                    board[Board.KEY], 3, NEGATIVE_INFINITY,
                    TranspositionScores.MATE_SCORE + 1, 0, probe
                );
                assertFalse(probe.keyMatches(), "Incomplete root was published to TT.");
                break;
            }
        }
        assertTrue(exercised, "No deterministic node limit interrupted a PVS re-search.");
    }

    @Test
    void generationAdvancesOncePerTopLevelSearchAndNewGameResetsBeforeNextSearch() {
        final TranspositionTable table = new TranspositionTable(1024);
        final AlphaBetaPvsSearch search = new AlphaBetaPvsSearch(table);
        final long[] board = Board.startingPosition();
        search.search(new SearchRequest(board, 1));
        assertEquals(1, table.generation());
        search.search(new SearchRequest(board, 1));
        assertEquals(2, table.generation());

        search.newGame();
        search.search(new SearchRequest(board, 1));
        assertEquals(1, table.generation());
    }

    private static void assertBalanced(
        AlphaBetaPvsSearch search, GameHistory gameHistory
    ) {
        assertEquals(gameHistory.size(), search.statistics().historyStartSize());
        assertEquals(gameHistory.size(), search.statistics().historyEndSize());
    }

    private static AlphaBetaPvsSearch search(boolean pvs, boolean tt) {
        return new AlphaBetaPvsSearch(
            new TranspositionTable(1 << 12), new Configuration(pvs, tt, true)
        );
    }

    private static SearchControl controlled(long nodeLimit) {
        return SearchControl.controlled(nodeLimit, 0L, -1L, TimeSource.SYSTEM);
    }

    private static long resolve(long[] board, String coordinate) {
        return new LegalMoveResolver().resolve(
            board, new MoveIntent(square(coordinate.substring(0, 2)),
                square(coordinate.substring(2, 4)))
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
