package com.ohinteractive.seedv6.search.alphabeta;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot.MoveOrderMetrics;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot.NodeMetrics;
import com.ohinteractive.seedv6.search.diagnostics.SearchDiagnosticsSnapshot.TtMetrics;
import com.ohinteractive.seedv6.search.order.MoveOrdering;
import com.ohinteractive.seedv6.search.order.StagedMovePicker;
import com.ohinteractive.seedv6.search.tt.TranspositionTable;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Bound;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Cacheability;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchDiagnosticsTest {

    @Test
    void exactEnteredNodeSplitAndDepthMaximaMatchSearchControl() {
        final long[] board = Board.fromFen(
            "4k3/8/2p1p3/3q4/2P1P3/3Q4/8/4K3 w - - 0 1"
        );
        final SearchControl control = controlled(-1L);
        final SearchResult result = new AlphaBetaPvsSearch(new TranspositionTable(1 << 14)).search(
            request(board, 1, control, true)
        );
        final NodeMetrics nodes = result.diagnostics().worker().nodes();

        assertEquals(43L, result.nodes());
        assertEquals(43L, control.nodes());
        assertEquals(24L, nodes.mainNodes());
        assertEquals(19L, nodes.qNodes());
        assertEquals(result.nodes(), nodes.totalEnteredNodes());
        assertEquals(result.nodes(), result.diagnostics().totalEnteredNodes());
        assertEquals(5, nodes.maximumAbsolutePly());
        assertEquals(4, nodes.maximumQply());
        assertEquals(19L, result.diagnostics().worker().qsearch().tacticalMovesSearched());
        assertEquals(0L, result.diagnostics().worker().qsearch().evasionMovesSearched());
    }

    @Test
    void ttMissInsufficientDepthAndUsableBoundsRemainDistinct() {
        final long[] terminal = Board.fromFen("7k/6Q1/6K1/8/8/8/8/8 b - - 0 1");
        final SearchResult miss = new AlphaBetaPvsSearch(new TranspositionTable(1 << 14)).search(
            request(terminal, 1, controlled(-1L), true)
        );
        assertEquals(new TtMetrics(1L, 0L, 0L, 0L, 0L, 0L, 0L, 1L),
            miss.diagnostics().worker().transpositionTable());

        final long[] root = Board.startingPosition();
        final long pawnMove = move(root, "e2e4");
        final TranspositionTable insufficientTable = new TranspositionTable(1 << 16);
        insufficientTable.store(
            root[Board.KEY], 0, Bound.EXACT, 0, 0, pawnMove, Cacheability.POSITION_ONLY
        );
        final SearchResult insufficient = new AlphaBetaPvsSearch(insufficientTable).search(
            request(root, 1, controlled(-1L), true)
        );
        final TtMetrics insufficientMetrics = insufficient.diagnostics().worker().transpositionTable();
        assertEquals(1L, insufficientMetrics.probes());
        assertEquals(1L, insufficientMetrics.keyMatches());
        assertEquals(1L, insufficientMetrics.insufficientDepthMatches());
        assertEquals(1L, insufficientMetrics.hashMovesAvailable());
        assertEquals(0L, insufficientMetrics.usableBoundCutoffs());

        assertBoundCutoff(Bound.EXACT, -200, 1L, 0L, 0L);
        assertBoundCutoff(Bound.LOWER, 100, 0L, 1L, 0L);
        assertBoundCutoff(Bound.UPPER, -100, 0L, 0L, 1L);
    }

    @Test
    void cutoffRanksAndHashKillerHistorySourcesUseActualDistinctMoveOrder() {
        final long[] board = Board.startingPosition();

        final MoveOrderMetrics first = window(board, -32_769, 0).diagnostics().worker().moveOrder();
        assertEquals(1L, first.betaCutoffs());
        assertEquals(1L, first.firstMoveBetaCutoffs());
        assertEquals(1L, first.cutoffRankSum());
        assertEquals(1, first.maximumCutoffRank());
        assertEquals(1L, first.cutoffRank1());

        final MoveOrderMetrics second = window(board, -32_769, 25).diagnostics().worker().moveOrder();
        assertEquals(2L, second.legalMovesSearched());
        assertEquals(1L, second.betaCutoffs());
        assertEquals(0L, second.firstMoveBetaCutoffs());
        assertEquals(2L, second.cutoffRankSum());
        assertEquals(2, second.maximumCutoffRank());
        assertEquals(0L, second.cutoffRank1());
        assertEquals(1L, second.cutoffRank2());

        final long firstOrdered = firstOrderedMove(board);
        final TranspositionTable hashTable = new TranspositionTable(1 << 14);
        hashTable.store(
            board[Board.KEY], 0, Bound.EXACT, 0, 0, firstOrdered,
            Cacheability.POSITION_ONLY
        );
        final AlphaBetaPvsSearch hashSearch = new AlphaBetaPvsSearch(hashTable);
        final MoveOrderMetrics hash = window(hashSearch, board, -32_769, 0)
            .diagnostics().worker().moveOrder();
        assertEquals(1L, hash.hashMoveCutoffs());
        assertEquals(1L, hash.quietCutoffs());
        assertEquals(0L, hash.tacticalCutoffs());

        final AlphaBetaPvsSearch killerSearch = new AlphaBetaPvsSearch(
            new TranspositionTable(1 << 14)
        );
        assertTrue(killerSearch.ordering().recordQuietCutoff(board, 0, firstOrdered, 1));
        final MoveOrderMetrics killer = window(killerSearch, board, -32_769, 0)
            .diagnostics().worker().moveOrder();
        assertEquals(1L, killer.killerCutoffs());
        assertEquals(0L, killer.historyCutoffs());

        final AlphaBetaPvsSearch historySearch = new AlphaBetaPvsSearch(
            new TranspositionTable(1 << 14)
        );
        assertTrue(historySearch.ordering().recordQuietCutoff(board, 1, firstOrdered, 1));
        final MoveOrderMetrics history = window(historySearch, board, -32_769, 0)
            .diagnostics().worker().moveOrder();
        assertEquals(0L, history.killerCutoffs());
        assertEquals(1L, history.historyCutoffs());
    }

    @Test
    void enabledAndDisabledStandaloneSearchesAreResultAndNodeIdentical() {
        final List<SearchCase> cases = List.of(
            new SearchCase(Board.startingPosition(), 2),
            new SearchCase(Board.fromFen(
                "4k3/8/2p1p3/3q4/2P1P3/3Q4/8/4K3 w - - 0 1"
            ), 1),
            new SearchCase(Board.fromFen("4k3/P7/8/8/8/8/7p/4K3 w - - 0 1"), 2),
            new SearchCase(Board.fromFen("7k/6Q1/6K1/8/8/8/8/8 b - - 0 1"), 1),
            new SearchCase(Board.fromFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1"), 1),
            new SearchCase(Board.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1"), 1)
        );

        for(SearchCase searchCase : cases) {
            final SearchResult disabled = new AlphaBetaPvsSearch(
                new TranspositionTable(1 << 16)
            ).search(request(searchCase.board, searchCase.depth, controlled(-1L), false));
            final SearchResult enabled = new AlphaBetaPvsSearch(
                new TranspositionTable(1 << 16)
            ).search(request(searchCase.board, searchCase.depth, controlled(-1L), true));
            assertSameSearchSemantics(disabled, enabled);
            assertFalse(disabled.diagnostics().enabled());
            assertTrue(enabled.diagnostics().enabled());
        }
    }

    @Test
    void warmReuseIsNeutralAndNewStandaloneScopeResetsWithoutMutatingPriorSnapshot() {
        final long[] board = Board.startingPosition();
        final AlphaBetaPvsSearch disabledSearch = new AlphaBetaPvsSearch(
            new TranspositionTable(1 << 16)
        );
        final AlphaBetaPvsSearch enabledSearch = new AlphaBetaPvsSearch(
            new TranspositionTable(1 << 16)
        );
        disabledSearch.search(request(board, 2, controlled(-1L), false));
        enabledSearch.search(request(board, 2, controlled(-1L), true));
        final SearchResult disabledWarm = disabledSearch.search(
            request(board, 2, controlled(-1L), false)
        );
        final SearchResult enabledWarm = enabledSearch.search(
            request(board, 2, controlled(-1L), true)
        );
        assertSameSearchSemantics(disabledWarm, enabledWarm);
        assertEquals(21L, enabledWarm.diagnostics().worker().transpositionTable().keyMatches());
        assertEquals(16L, enabledWarm.diagnostics().worker().transpositionTable().lowerBoundCutoffs());

        final SearchDiagnosticsSnapshot retained = enabledWarm.diagnostics();
        final long retainedNodes = retained.totalEnteredNodes();
        final long[] terminal = Board.fromFen("7k/6Q1/6K1/8/8/8/8/8 b - - 0 1");
        final SearchResult next = enabledSearch.search(
            request(terminal, 1, controlled(-1L), true)
        );
        assertEquals(0L, next.diagnostics().totalEnteredNodes());
        assertEquals(1L, next.diagnostics().worker().transpositionTable().probes());
        assertEquals(retainedNodes, retained.totalEnteredNodes());
        assertEquals(44L, retainedNodes);
    }

    private static void assertBoundCutoff(
        Bound bound, int storedScore, long exact, long lower, long upper
    ) {
        final long[] root = Board.startingPosition();
        final long pawnMove = move(root, "e2e4");
        final long[] child = new long[Board.MAX_BITBOARDS];
        Board.makeMoveInto(
            root[0], root[1], root[2], root[3], Math.toIntExact(root[Board.STATUS]),
            root[Board.KEY], pawnMove, child
        );
        final TranspositionTable table = new TranspositionTable(1 << 18);
        table.store(
            root[Board.KEY], 0, Bound.EXACT, 0, 0, pawnMove, Cacheability.POSITION_ONLY
        );
        table.store(
            child[Board.KEY], 1, bound, storedScore, 1, StagedMovePicker.NO_MOVE,
            Cacheability.POSITION_ONLY
        );
        final SearchResult result = window(
            new AlphaBetaPvsSearch(table), root, -100, 100, 2
        );
        final TtMetrics metrics = result.diagnostics().worker().transpositionTable();
        assertEquals(exact, metrics.exactCutoffs(), bound.name());
        assertEquals(lower, metrics.lowerBoundCutoffs(), bound.name());
        assertEquals(upper, metrics.upperBoundCutoffs(), bound.name());
        assertEquals(1L, metrics.insufficientDepthMatches(), bound.name());
    }

    private static SearchResult window(long[] board, int alpha, int beta) {
        return window(new AlphaBetaPvsSearch(new TranspositionTable(1 << 14)), board, alpha, beta);
    }

    private static SearchResult window(
        AlphaBetaPvsSearch search, long[] board, int alpha, int beta
    ) {
        return window(search, board, alpha, beta, 1);
    }

    private static SearchResult window(
        AlphaBetaPvsSearch search, long[] board, int alpha, int beta, int depth
    ) {
        search.beginTopLevelSearch();
        try {
            return search.searchWindow(request(board, depth, controlled(-1L), true), alpha, beta);
        } finally {
            search.endTopLevelSearch();
        }
    }

    private static long firstOrderedMove(long[] board) {
        final MoveOrdering ordering = new MoveOrdering(AlphaBetaPvsSearch.MAX_SUPPORTED_DEPTH + 1);
        final StagedMovePicker picker = ordering.picker();
        picker.prepare(board, 0, StagedMovePicker.NO_MOVE);
        try {
            return picker.next(0);
        } finally {
            picker.clearPly(0);
        }
    }

    private static long move(long[] board, String coordinate) {
        final long[] moves = new long[StagedMovePicker.MAX_MOVES];
        final int count = Gen.genAll(
            board[0], board[1], board[2], board[3], Math.toIntExact(board[Board.STATUS]),
            board[Board.KEY], true, moves, new long[Board.MAX_BITBOARDS]
        );
        for(int index = 0; index < count; index ++) {
            if(Move.coordinate(moves[index]).equals(coordinate)) return moves[index];
        }
        throw new AssertionError("Missing legal move " + coordinate);
    }

    private static SearchRequest request(
        long[] board, int depth, SearchControl control, boolean diagnostics
    ) {
        return new SearchRequest(
            board, GameHistory.initial(board), depth, SearchObserver.NONE, control, diagnostics
        );
    }

    private static SearchControl controlled(long nodeLimit) {
        return SearchControl.controlled(nodeLimit, 0L, -1L, () -> 0L);
    }

    private static void assertSameSearchSemantics(SearchResult expected, SearchResult actual) {
        assertEquals(expected.bestMove(), actual.bestMove());
        assertEquals(expected.hasMove(), actual.hasMove());
        assertEquals(expected.score(), actual.score());
        assertEquals(expected.depth(), actual.depth());
        assertEquals(expected.nodes(), actual.nodes());
        assertEquals(expected.legalRootMoves(), actual.legalRootMoves());
        assertEquals(expected.completed(), actual.completed());
        assertArrayEquals(expected.principalVariation(), actual.principalVariation());
    }

    private record SearchCase(long[] board, int depth) {
        @Override
        public boolean equals(Object other) {
            return other instanceof SearchCase candidate
                && depth == candidate.depth && Arrays.equals(board, candidate.board);
        }

        @Override
        public int hashCode() {
            return 31 * Arrays.hashCode(board) + depth;
        }
    }
}
