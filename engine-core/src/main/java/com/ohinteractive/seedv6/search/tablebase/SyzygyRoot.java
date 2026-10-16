package com.ohinteractive.seedv6.search.tablebase;

import java.util.Objects;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.DrawAdjudicator;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;
import com.ohinteractive.seedv6.search.common.SearchControl;

/** Conservative root WIN policy; never called from recursive Search or its TT. */
final class SyzygyRoot implements RootTablebase {
    @FunctionalInterface
    interface Probe {
        /** Fathom packed root result, or -1 when unavailable. Input is not modified. */
        int root(long[] board);
    }

    private final Probe probe;

    SyzygyRoot(Probe probe) { this.probe = Objects.requireNonNull(probe); }

    @Override public TablebaseWin probeWin(long[] board, GameHistory history, SearchControl control) {
        if(Thread.currentThread().isInterrupted() || !control.checkpoint() || !control.checkpointNodeBudget()) return null;
        int status = (int) board[Board.STATUS];
        if(Long.bitCount(board[0] | board[1] | board[2]) > 4 || ((status >>> 1) & 15) != 0) return null;
        history.requireCurrent(board);
        int clock = Board.halfMoveClock(status);
        if(clock >= 99) return null;
        // All reachable prior identities matter, not just the current root's count.
        int first = Math.max(0, history.size() - 1 - clock);
        for(int i = first; i < history.size(); i++)
            for(int j = first; j < i; j++)
                if(history.keyAt(i) == history.keyAt(j)) return null;

        long[] moves = new long[512];
        long[] child = new long[Board.MAX_BITBOARDS];
        int count = Gen.genAll(board[0], board[1], board[2], board[3], status, board[Board.KEY], true, moves, child);
        if(count == 0 || DrawAdjudicator.adjudicateNonTerminal(board, new SearchLineHistory(history))
                != DrawAdjudicator.RuleDraw.NONE) return null;
        int result = probe.root(board);
        if(Thread.currentThread().isInterrupted() || !control.checkpoint() || !control.checkpointNodeBudget()) return null;
        // Checkmate/stalemate are sentinels, not ordinary packed WDL results.
        if(result == -1 || result == 2 || result == 4 || (result & 15) != 4) return null;
        int dtz = (result >>> 20) & 4095;
        if(dtz == 0 || dtz + clock > 98) return null;
        int from = (result >>> 10) & 63, to = (result >>> 4) & 63, promotion = (result >>> 16) & 7;
        for(int i = 0; i < count; i++) {
            long move = moves[i];
            int p = switch(Move.promotion(move)) {
                case NONE -> 0; case QUEEN -> 1; case ROOK -> 2; case BISHOP -> 3; case KNIGHT -> 4;
            };
            if(Move.fromSquare(move) != from || Move.toSquare(move) != to || p != promotion) continue;
            Board.makeMoveInto(board[0], board[1], board[2], board[3], status, board[Board.KEY], move, child);
            int replies = Gen.genAll(child[0], child[1], child[2], child[3], (int)child[4], child[5],
                    true, moves, new long[Board.MAX_BITBOARDS]);
            if(replies == 0) {
                if(!Board.isPlayerInCheck(child[0], child[1], child[2], child[3], Board.player((int)child[4]))) return null;
            } else {
                var next = GameHistory.builder(history).appendPosition(child).snapshot();
                if(DrawAdjudicator.adjudicateNonTerminal(child, new SearchLineHistory(next)) != DrawAdjudicator.RuleDraw.NONE)
                    return null;
            }
            return new TablebaseWin(move, dtz);
        }
        return null;
    }
}
