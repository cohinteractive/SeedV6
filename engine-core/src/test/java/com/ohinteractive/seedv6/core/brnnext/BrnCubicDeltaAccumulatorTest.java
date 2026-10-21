package com.ohinteractive.seedv6.core.brnnext;

import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Features;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.SeePolicy;
import com.ohinteractive.seedv6.search.tt.TTable;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class BrnCubicDeltaAccumulatorTest {
    private static double[] weights(BrnNextArchitecture kind,long seed) {
        var random=new SplittableRandom(seed);var w=new double[kind.parameters()];
        for(int i=0;i<w.length;i++)w[i]=random.nextDouble(-2,2);
        return w;
    }
    private static List<long[]> walk(long seed,int count) {
        var random=new SplittableRandom(seed);var result=new ArrayList<long[]>();
        var board=Board.startingPosition();long[] moves=new long[256],scratch=new long[Board.MAX_BITBOARDS];
        for(int i=0;i<count;i++) {
            result.add(board);
            int n=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,scratch);
            if(n==0||i%200==199)board=Board.startingPosition();
            else {
                long[] child=new long[Board.MAX_BITBOARDS];
                Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[random.nextInt(n)],child);
                board=child;
            }
        }
        return result;
    }
    @ParameterizedTest @EnumSource(value=BrnNextArchitecture.class,names={"GEN32_CUBIC","GEN32_CUBIC_R4","GEN32_FIXED_CUBIC_R4"})
    void arbitraryTransitionsAndRebuildsRetainRawBoundsAndIntegerScores(BrnNextArchitecture kind) {
        double maximum=0;
        for(long seed:new long[]{13,71,242}) {
            var model=new BrnNextModel(kind,weights(kind,seed));
            var delta=new BrnCubicDeltaAccumulator(model);var full=model.newWorkspace();var random=new SplittableRandom(seed);
            var positions=walk(seed,3000);
            for(int i=0;i<positions.size();i++) {
                var board=positions.get(i%7==0?random.nextInt(i+1):i);
                double expected=model.rawPawns(board,full),actual=delta.rawPawns(board);
                maximum=Math.max(maximum,Math.abs(expected-actual));
                assertEquals(expected,actual,1e-10);
                assertEquals(model.score(board,full),delta.score(board));
            }
            assertTrue(delta.rebuilds>10);assertTrue(delta.updates>50);assertTrue(delta.primitiveBytes()<8192);
        }
        System.out.println("Delta maximum raw error over9000 transitions: "+maximum);
    }
    @ParameterizedTest @EnumSource(value=BrnNextArchitecture.class,names={"GEN32_CUBIC","GEN32_CUBIC_R4","GEN32_FIXED_CUBIC_R4"})
    void roundingBoundariesUseCanonicalFallbackOnBothScoreSigns(BrnNextArchitecture kind) {
        var positions=walk(197,91);var last=positions.getLast();double material=BrnPair2Features.material(last);
        for(double target:new double[]{-.5,.5,-37.5,37.5})for(double offset:new double[]{-1e-9,0,1e-9}) {
            var w=weights(kind,51);w[0]=0;
            var initial=new BrnNextModel(kind,w);
            double raw=initial.rawPawns(last,initial.newWorkspace());
            w[0]=material+((target+offset)/100-material)/.25-raw;
            var model=new BrnNextModel(kind,w);
            var delta=new BrnCubicDeltaAccumulator(model);var full=model.newWorkspace();
            for(var board:positions) {
                model.rawPawns(board,full);delta.rawPawns(board);
            }
            assertEquals(model.score(last,full),delta.score(last),"target="+target+", offset="+offset);
            assertTrue(delta.roundingFallbacks>0,"Boundary must exercise canonical fallback");
        }
    }
    @ParameterizedTest @EnumSource(value=BrnNextArchitecture.class,names={"GEN32_CUBIC","GEN32_CUBIC_R4","GEN32_FIXED_CUBIC_R4"})
    void specialMovesUndoNullAndEmptyOwnershipMatchFull(BrnNextArchitecture kind) {
        var model=new BrnNextModel(kind,weights(kind,97));
        var delta=new BrnCubicDeltaAccumulator(model);var full=model.newWorkspace();
        for(String fen:List.of("r3k2r/8/8/3pP3/8/8/8/R3K2R w KQkq d6 0 1","4k3/P7/8/8/8/8/7p/4K3 w - - 0 1")) {
            var board=Board.fromFen(fen);long[] moves=new long[256],scratch=new long[Board.MAX_BITBOARDS];
            int n=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,scratch);
            for(int i=0;i<n;i++) {
                long[] child=new long[Board.MAX_BITBOARDS];
                Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[i],child);
                for(var selected:List.of(child,board))assertEquals(model.score(selected,full),delta.score(selected));
            }
            var flip=board.clone();flip[4]^=Board.PLAYER_BIT;
            assertEquals(model.score(flip,full),delta.score(flip));
            var empty=board.clone();empty[3]^=~(board[0]|board[1]|board[2]);
            assertEquals(model.score(empty,full),delta.score(empty));
        }
    }
    @ParameterizedTest @EnumSource(value=BrnNextArchitecture.class,names={"GEN32_CUBIC","GEN32_CUBIC_R4","GEN32_FIXED_CUBIC_R4"})
    void separateOwnersAndSearchMatchOriginal(BrnNextArchitecture kind) {
        var model=new BrnNextModel(kind,weights(kind,42));
        var full=SearchEvaluation.brnNext(model);var fast=SearchEvaluation.brnCubicDeltaResearch(model);
        var a=fast.newState(64);var b=fast.newState(64);var positions=walk(17,30);
        a.initialize(positions.getFirst(),0);b.initializeFrom(positions.getLast(),0,a);
        assertEquals(full.newState(64).evaluate(positions.getLast(),0),b.evaluate(positions.getLast(),0));
        assertEquals(full.newState(64).evaluate(positions.getFirst(),0),a.evaluate(positions.getFirst(),0));
        for(var board:List.of(positions.getFirst(),positions.getLast())) {
            try(var original=new SearchDriver(ExactSearchAdapter.seeResearch(full,SeePolicy.QXRAY,null,new TTable(1)).withCompletedHints(true,null));
                var optimized=new SearchDriver(ExactSearchAdapter.seeResearch(fast,SeePolicy.QXRAY,null,new TTable(1)).withCompletedHints(true,null))) {
                var expected=original.search(new SearchRequest(board,4));var actual=optimized.search(new SearchRequest(board,4));
                assertTrue(expected.targetDepthCompleted());assertTrue(actual.targetDepthCompleted());
                assertEquals(expected.lastCompletedResult(),actual.lastCompletedResult());
            }
        }
        assertThrows(IllegalArgumentException.class,()->a.initializeFrom(positions.getFirst(),0,full.newState(2)));
        assertThrows(IllegalArgumentException.class,()->SearchEvaluation.brnCubicDeltaResearch(
                new BrnNextModel(BrnNextArchitecture.CUBIC,new double[BrnNextArchitecture.CUBIC.parameters()])));
    }
}
