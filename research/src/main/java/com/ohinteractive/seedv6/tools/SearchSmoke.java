package com.ohinteractive.seedv6.tools;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.search.common.SearchObserver;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.flat.FlatNegamax;

public class SearchSmoke {
    
    public static void main(String[] args) {
        final int depth = args.length > 0 ? Integer.parseInt(args[0]) : 7;
        final long[] board = Board.startingPosition();
        final SearchRequest request = new SearchRequest(board, depth, new ClassicConsoleObserver());
        final FlatNegamax search = new FlatNegamax();
        final long start = System.nanoTime();
        final SearchResult result = search.search(request);
        final long elapsed = System.nanoTime() - start;
        final double seconds = elapsed / 1_000_000_000.0;
        final long nps = seconds > 0.0 ? (long) (result.nodes() / seconds) : 0L;
        System.out.println("Search smoke test");
        System.out.println("-----------------");
        System.out.println("Depth:          " + result.depth());
        System.out.println("Has move:       " + result.hasMove());
        System.out.println("Best move raw:  " + result.bestMove());
        System.out.println("Best move:      " + (result.hasMove() ? Move.string(result.bestMove()) : "(none)"));
        System.out.println("Score:          " + result.score());
        System.out.println("Nodes:          " + result.nodes());
        System.out.println("Root moves:     " + result.legalRootMoves());
        System.out.println("Completed:      " + result.completed());
        System.out.println("Time:           " + seconds + "s");
        System.out.println("NPS:            " + nps);
    }

    private static class ClassicConsoleObserver implements SearchObserver {

        private long bestMove;
        private int bestScore;
        private boolean hasBest;

        @Override
        public void onSearchStarted(int depth, int rootEval, int rootMoveCount) {
            bestMove = 0L;
            bestScore = 0;
            hasBest = false;
            System.out.println("----------------------------------------");
            System.out.println("Evaluation: " + rootEval + " Root moves: " + rootMoveCount);
            System.out.println("----------------------------------------");
            System.out.println("Depth searched:");
            System.out.println(depth + "(Max)");
        }

        @Override
        public void onRootMoveFinished(int index, int total, long move, int score, boolean best, long nodes, long elapsedNanos) {
            if(best) {
                bestMove = move;
                bestScore = score;
                hasBest = true;
            }
            final double seconds = elapsedNanos / 1_000_000_000.0;
            System.out.printf(
                "  %2d/%-2d %-5s: %6s  %-7s %7.2fs %s%n",
                index,
                total,
                Move.string(move),
                formatScore(score),
                best ? "(Best)" : "",
                seconds,
                formatNodes(nodes)
            );
        }

        @Override
        public void onSearchFinished(SearchResult result, long elapsedNanos) {
            final double seconds = elapsedNanos / 1_000_000_000.0;
            System.out.println("Elapsed: " + formatSeconds(seconds));
            System.out.println("Total Nodes: " + formatNodes(result.nodes()));
            if(hasBest) {
                System.out.println("Best evaluated move: [" + Move.string(bestMove) + "] (" + formatScore(bestScore) + ")");
            } else {
                System.out.println("Best evaluated move: (none)");
            }
        }

        private static String formatScore(int score) {
            return Integer.toString(score);
        }

        private static String formatSeconds(double seconds) {
            return String.format("%.2fs", seconds);
        }

        private static String formatNodes(long nodes) {
            if(nodes >= 1_000_000L) return String.format("%.3fm", nodes / 1_000_000.0);
            if(nodes >= 1_000L) return String.format("%.3fk", nodes / 1_000.0);
            return Long.toString(nodes);
        }

    }

}
