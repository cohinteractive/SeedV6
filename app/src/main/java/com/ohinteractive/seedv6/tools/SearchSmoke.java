package com.ohinteractive.seedv6.tools;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.flat.FlatNegamax;

public class SearchSmoke {
    
    public static void main(String[] args) {
        final int depth = args.length > 0 ? Integer.parseInt(args[0]) : 5;
        final long[] board = Board.startingPosition();
        final SearchRequest request = new SearchRequest();
        request.board = board;
        request.depth = depth;
        final FlatNegamax search = new FlatNegamax();
        final long start = System.nanoTime();
        final SearchResult result = search.search(request);
        final long elapsed = System.nanoTime() - start;
        final double seconds = elapsed / 1_000_000_000.0;
        final long nps = seconds > 0.0 ? (long) (result.nodes / seconds) : 0L;
        System.out.println("Search smoke test");
        System.out.println("-----------------");
        System.out.println("Depth:          " + result.depth);
        System.out.println("Has move:       " + result.hasMove);
        System.out.println("Best move raw:  " + result.bestMove);
        System.out.println("Best move:      " + (result.hasMove ? Move.string(result.bestMove) : "(none)"));
        System.out.println("Score:          " + result.score);
        System.out.println("Nodes:          " + result.nodes);
        System.out.println("Root moves:     " + result.legalRootMoves);
        System.out.println("Time:           " + seconds + "s");
        System.out.println("NPS:            " + nps);
    }

}
