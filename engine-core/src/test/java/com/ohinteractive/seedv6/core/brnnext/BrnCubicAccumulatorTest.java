package com.ohinteractive.seedv6.core.brnnext;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.SeePolicy;
import com.ohinteractive.seedv6.search.tt.TTable;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrnCubicAccumulatorTest {
    private static BrnNextModel model(long seed) {
        var random=new SplittableRandom(seed);var weights=new double[BrnNextArchitecture.CUBIC.parameters()];
        for(int i=0;i<weights.length;i++)weights[i]=random.nextDouble(-2,2);
        return new BrnNextModel(BrnNextArchitecture.CUBIC,weights);
    }
    @Test void arbitraryTransitionsHaveNoHistoryDriftAndMatchOriginalForward() {
        double maximum=0;
        for(long seed:new long[]{13,71,242}) {
            var model=model(seed);var cache=new BrnCubicAccumulator(model);var workspace=model.newWorkspace();
            var random=new SplittableRandom(seed);var positions=new ArrayList<long[]>();
            long[] board=Board.startingPosition(),moves=new long[256],scratch=new long[Board.MAX_BITBOARDS];
            for(int i=0;i<3000;i++) {
                int count=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,scratch);
                if(count==0||i%100==99)board=Board.startingPosition();
                else {
                    long[] child=new long[Board.MAX_BITBOARDS];
                    Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[random.nextInt(count)],child);
                    board=child;
                }
                positions.add(board);
                long[] selected=i%7==0?positions.get(random.nextInt(positions.size())):board;
                double actual=cache.rawPawns(selected),expected=model.rawPawns(selected,workspace);
                maximum=Math.max(maximum,Math.abs(actual-expected));
                assertEquals(expected,actual,2e-12,"Original full recompute");
                assertEquals(Double.doubleToLongBits(new BrnCubicAccumulator(model).rawPawns(selected)),
                        Double.doubleToLongBits(actual),"Rebuilt and transitioned caches must be history independent");
                assertEquals(model.score(selected,workspace),cache.score(selected));
            }
            assertTrue(cache.primitiveBytes()<8192);
        }
        System.out.println("CUBIC block cache maximum raw error over 9000 transitions: "+maximum);
    }
    @Test void specialMovesUndoNullPerspectiveAndEmptyOwnershipAreSafe() {
        var model=model(97);var cache=new BrnCubicAccumulator(model);var workspace=model.newWorkspace();
        for(String fen:List.of("r3k2r/8/8/3pP3/8/8/8/R3K2R w KQkq d6 0 1",
                "4k3/P7/8/8/8/8/7p/4K3 w - - 0 1")) {
            var board=Board.fromFen(fen);double original=cache.rawPawns(board);
            long[] moves=new long[256],scratch=new long[Board.MAX_BITBOARDS];
            int count=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,scratch);
            for(int i=0;i<count;i++) {
                long[] child=new long[Board.MAX_BITBOARDS];
                Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[i],child);
                assertEquals(model.rawPawns(child,workspace),cache.rawPawns(child),2e-12);
                assertEquals(original,cache.rawPawns(board));
            }
            var status=board.clone();status[4]^=Board.PLAYER_BIT;
            assertEquals(model.rawPawns(status,workspace),cache.rawPawns(status),2e-12);
            assertEquals(original,cache.rawPawns(board));
            long ranks=cache.recomputedRanks;
            var emptyOwner=board.clone();emptyOwner[3]^=~(board[0]|board[1]|board[2]);
            assertEquals(original,cache.rawPawns(emptyOwner));
            assertEquals(ranks,cache.recomputedRanks);
        }
    }
    @Test void independentSearchOwnersAndCommonPolicyAgreeWithFullRecompute() {
        var model=model(42);
        var full=SearchEvaluation.brnNext(model);var cached=SearchEvaluation.brnCubicIncrementalResearch(model);
        var a=cached.newState(64);var b=cached.newState(64);
        for(String fen:List.of("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "r3k2r/8/8/3pP3/8/8/8/R3K2R w KQkq d6 0 1",
                "4k3/P7/8/8/8/8/7p/4K3 w - - 0 1")) {
            var board=Board.fromFen(fen);
            a.initialize(board,0);b.initializeFrom(board,0,a);
            assertEquals(a.evaluate(board,0),b.evaluate(board,0));
            try(var scalar=new SearchDriver(ExactSearchAdapter.seeResearch(full,SeePolicy.QXRAY,null,new TTable(1)).withCompletedHints(true,null));
                var rolling=new SearchDriver(ExactSearchAdapter.seeResearch(cached,SeePolicy.QXRAY,null,new TTable(1)).withCompletedHints(true,null))) {
                var expected=scalar.search(new SearchRequest(board,4));
                var actual=rolling.search(new SearchRequest(board,4));
                assertTrue(expected.targetDepthCompleted());assertTrue(actual.targetDepthCompleted());
                assertEquals(expected.lastCompletedResult(),actual.lastCompletedResult());
            }
        }
        assertThrows(IllegalArgumentException.class,()->a.initializeFrom(Board.startingPosition(),0,full.newState(2)));
        assertThrows(IllegalArgumentException.class,()->SearchEvaluation.brnCubicIncrementalResearch(
                new BrnNextModel(BrnNextArchitecture.GATED_PAIRS,new double[BrnNextArchitecture.GATED_PAIRS.parameters()])));
    }
}
