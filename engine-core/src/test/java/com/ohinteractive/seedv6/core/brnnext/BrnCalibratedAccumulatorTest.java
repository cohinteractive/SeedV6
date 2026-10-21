package com.ohinteractive.seedv6.core.brnnext;

import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.SeePolicy;
import com.ohinteractive.seedv6.search.tt.TTable;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.*;

class BrnCalibratedAccumulatorTest {
    static double[] weights(BrnNextArchitecture kind,long seed) {
        var rng=new SplittableRandom(seed);var w=new double[kind.parameters()];
        for(int i=0;i<w.length;i++)w[i]=rng.nextDouble(-1.5,1.5);return w;
    }
    static List<long[]> walk(long seed,int count) {
        var rng=new SplittableRandom(seed);var result=new ArrayList<long[]>();var board=Board.startingPosition();
        long[] moves=new long[256],scratch=new long[Board.MAX_BITBOARDS];
        for(int i=0;i<count;i++) {
            result.add(board);int n=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,scratch);
            if(n==0||i%200==199)board=Board.startingPosition();
            else {var child=new long[Board.MAX_BITBOARDS];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[rng.nextInt(n)],child);board=child;}
        }
        return result;
    }
    @ParameterizedTest @EnumSource(value=BrnNextArchitecture.class,names={"GEN32_CAL_AFFINE_R4","GEN32_CAL_QUADRATIC_R4","GEN32_CAL_CUBIC_R4"})
    void arbitraryTransitionsSpecialMovesAndSearchMatchCanonical(BrnNextArchitecture kind) {
        var model=new BrnNextModel(kind,weights(kind,93));var fast=new BrnCalibratedAccumulator(model);var full=model.newWorkspace();
        var positions=walk(73,3000);var rng=new SplittableRandom(72);
        for(int i=0;i<positions.size();i++) {
            var board=positions.get(i%7==0?rng.nextInt(i+1):i);
            assertEquals(model.rawPawns(board,full),fast.rawPawns(board),1e-10);
            assertEquals(model.score(board,full),fast.score(board));
        }
        assertTrue(fast.primitiveBytes()<16384);
        for(String fen:List.of("r3k2r/8/8/3pP3/8/8/8/R3K2R w KQkq d6 0 1","4k3/P7/8/8/8/8/7p/4K3 w - - 0 1")) {
            var board=Board.fromFen(fen);long[] moves=new long[256],scratch=new long[Board.MAX_BITBOARDS];
            int n=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,scratch);
            for(int i=0;i<n;i++) {
                var child=new long[Board.MAX_BITBOARDS];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[i],child);
                assertEquals(model.score(child,full),fast.score(child));assertEquals(model.score(board,full),fast.score(board));
            }
            var flip=board.clone();flip[4]^=Board.PLAYER_BIT;assertEquals(model.score(flip,full),fast.score(flip));
            var empty=board.clone();empty[3]^=~(board[0]|board[1]|board[2]);assertEquals(model.score(empty,full),fast.score(empty));
        }
        for(var board:List.of(positions.getFirst(),positions.get(100))) {
            try(var original=new SearchDriver(ExactSearchAdapter.seeResearch(SearchEvaluation.brnNext(model),SeePolicy.QXRAY,null,new TTable(1)).withCompletedHints(true,null));
                var cached=new SearchDriver(ExactSearchAdapter.seeResearch(SearchEvaluation.brnGen32Residual(model),SeePolicy.QXRAY,null,new TTable(1)).withCompletedHints(true,null))) {
                var a=original.search(new SearchRequest(board,4));var b=cached.search(new SearchRequest(board,4));
                assertTrue(a.targetDepthCompleted());assertTrue(b.targetDepthCompleted());
                assertEquals(a.lastCompletedResult(),b.lastCompletedResult());
            }
        }
    }
    @ParameterizedTest @EnumSource(value=BrnNextArchitecture.class,names={"GEN32_CAL_AFFINE_R4","GEN32_CAL_QUADRATIC_R4","GEN32_CAL_CUBIC_R4"})
    void roundingBoundariesFallBackToUnfoldedOracle(BrnNextArchitecture kind) {
        var positions=walk(197,91);var last=positions.getLast();
        for(double target:new double[]{-.5,.5,-37.5,37.5})for(double offset:new double[]{-1e-9,0,1e-9}) {
            var w=weights(kind,51);w[kind.affineHead()]=0;var original=new BrnNextModel(kind,w);
            double m=com.ohinteractive.seedv6.core.brnpair2.BrnPair2Features.material(last),raw=original.rawPawns(last,original.newWorkspace());
            w[kind.affineHead()]=((target+offset)/100-(m+.25*(raw-m)))/.125;
            var model=new BrnNextModel(kind,w);var fast=new BrnCalibratedAccumulator(model);
            for(var board:positions)fast.rawPawns(board);
            assertEquals(model.score(last,model.newWorkspace()),fast.score(last));assertTrue(fast.roundingFallbacks>0);
        }
    }
    @ParameterizedTest @EnumSource(value=BrnNextArchitecture.class,names={"GEN32_CAL_AFFINE_R4","GEN32_CAL_QUADRATIC_R4","GEN32_CAL_CUBIC_R4"})
    void thirdMixedDifferenceSeparatesCubicFromAdditiveControls(BrnNextArchitecture kind) {
        var w=new double[kind.parameters()];
        // Three optional friendly pawns; all other embeddings are zero.
        for(int square:new int[]{8,9,10})w[EMBEDDING+((com.ohinteractive.seedv6.core.util.Piece.PAWN-1)*64+square)*4]=1;
        if(kind!=GEN32_CAL_AFFINE_R4)w[kind.extraHead()]=2;
        var model=new BrnNextModel(kind,w);double third=0;
        for(int mask=0;mask<8;mask++) {
            StringBuilder rank=new StringBuilder();int empty=0;
            for(int file=0;file<8;file++) {
                if(file<3&&(mask&(1<<file))!=0){if(empty>0){rank.append(empty);empty=0;}rank.append('P');}else empty++;
            }
            if(empty>0)rank.append(empty);
            var board=Board.fromFen("4k3/8/8/8/8/8/"+rank+"/4K3 w - - 0 1");
            third+=(Integer.bitCount(mask)%2==1?1:-1)*model.rawPawns(board,model.newWorkspace());
        }
        assertEquals(kind==GEN32_CAL_CUBIC_R4?DEGREE3_NORM:0,third,1e-10);
    }
}
