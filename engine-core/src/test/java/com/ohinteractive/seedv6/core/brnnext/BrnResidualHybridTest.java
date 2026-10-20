package com.ohinteractive.seedv6.core.brnnext;

import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.brnpair2.*;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.SeePolicy;
import com.ohinteractive.seedv6.search.tt.TTable;
import java.io.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class BrnResidualHybridTest {
    private static BrnPair2Model base(long seed) {
        var random=new SplittableRandom(seed);var weights=new float[BrnPair2Codec.INFERENCE_PARAMETERS];
        for(int i=0;i<weights.length;i++)weights[i]=(float)random.nextDouble(-.03,.03);
        return new BrnPair2Model(new BrnPair2Weights(2,weights));
    }
    private static List<long[]> boards() {
        var result=new ArrayList<long[]>();var random=new SplittableRandom(73);
        var board=Board.startingPosition();long[] moves=new long[256],scratch=new long[Board.MAX_BITBOARDS];
        for(int i=0;i<600;i++) {
            result.add(board);
            int n=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,scratch);
            if(n==0||i%100==99)board=Board.startingPosition();
            else {
                long[] child=new long[Board.MAX_BITBOARDS];
                Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[random.nextInt(n)],child);
                board=child;
            }
        }
        result.add(Board.fromFen("r3k2r/8/8/3pP3/8/8/8/R3K2R w KQkq d6 0 1"));
        result.add(Board.fromFen("4k3/P7/8/8/8/8/7p/4K3 w - - 0 1"));
        return result;
    }
    private static byte[] encoded(BrnNextModel model) throws IOException {
        var out=new ByteArrayOutputStream();BrnNextCodec.writeModel(model,out);return out.toByteArray();
    }
    private static byte[] encoded(BrnPair2Model model) throws IOException {
        var out=new ByteArrayOutputStream();BrnPair2Codec.writeModel(model,out);return out.toByteArray();
    }
    @ParameterizedTest @EnumSource(value=BrnNextArchitecture.class,names={"GEN32_GATED","GEN32_CUBIC"})
    void zeroCorrectionPreservesSuppliedBaseAndDoesNotClaimOriginalDispatch(BrnNextArchitecture kind) throws Exception {
        var base=base(17);var model=new BrnNextModel(kind,new double[kind.parameters()]);
        byte[] originalBase=encoded(base),originalModel=encoded(model);
        var expected=SearchEvaluation.brnPair2(base).newState(4);
        var full=SearchEvaluation.brnResidualHybridFullResearch(model,base);
        var fast=SearchEvaluation.brnResidualHybridResearch(model,base);
        var f=full.newState(4);var d=fast.newState(4);
        for(var board:boards()) {
            assertEquals(expected.evaluate(board,0),f.evaluate(board,0));
            assertEquals(expected.evaluate(board,0),d.evaluate(board,0));
        }
        assertFalse(fast.isBrnGen32Residual());assertFalse(full.isBrnGen32Residual());
        assertTrue(SearchEvaluation.brnGen32Residual(model).isBrnGen32Residual());
        assertArrayEquals(originalBase,encoded(base));assertArrayEquals(originalModel,encoded(model));
        assertThrows(IllegalArgumentException.class,()->d.initializeFrom(Board.startingPosition(),0,
                SearchEvaluation.brnResidualHybridResearch(model,base(18)).newState(4)));
        var w1=new BrnNextModel(BrnNextArchitecture.CUBIC,new double[BrnNextArchitecture.CUBIC.parameters()]);
        assertThrows(IllegalArgumentException.class,()->w1.newResearchWorkspace(base));
        assertThrows(IllegalArgumentException.class,()->SearchEvaluation.brnResidualHybridResearch(w1,base));
    }
    @ParameterizedTest @EnumSource(value=BrnNextArchitecture.class,names={"GEN32_GATED","GEN32_CUBIC"})
    void canonicalCompositionDeltaTransitionsAndSearchAgree(BrnNextArchitecture kind) throws Exception {
        var random=new SplittableRandom(91);double[] weights=new double[kind.parameters()];
        for(int i=0;i<weights.length;i++)weights[i]=random.nextDouble(-.2,.2);
        var model=new BrnNextModel(kind,weights);var base=base(43);
        var ordinary=model.newWorkspace();var composed=model.newResearchWorkspace(base);
        var oldBase=BrnNextBase.model().newWorkspace();var newBase=base.newWorkspace();
        var full=SearchEvaluation.brnResidualHybridFullResearch(model,base);
        var fast=SearchEvaluation.brnResidualHybridResearch(model,base);
        var f=full.newState(8);var d=fast.newState(8);var positions=boards();
        for(int pass=0;pass<3;pass++)for(int i=0;i<positions.size();i++) {
            var board=positions.get(pass==1?positions.size()-1-i:i);
            assertEquals(model.rawPawns(board,ordinary)-oldBase.evaluatePawns(board)+newBase.evaluatePawns(board),
                    model.rawPawns(board,composed),1e-11);
            assertEquals(f.evaluate(board,0),d.evaluate(board,0));
            var flipped=board.clone();flipped[4]^=Board.PLAYER_BIT;
            assertEquals(f.evaluate(flipped,0),d.evaluate(flipped,0));
        }
        for(var board:List.of(positions.get(30),positions.get(72))) {
            try(var a=new SearchDriver(ExactSearchAdapter.seeResearch(full,SeePolicy.QXRAY,null,new TTable(1)).withCompletedHints(true,null));
                var b=new SearchDriver(ExactSearchAdapter.seeResearch(fast,SeePolicy.QXRAY,null,new TTable(1)).withCompletedHints(true,null))) {
                var expected=a.search(new SearchRequest(board,3));var actual=b.search(new SearchRequest(board,3));
                assertTrue(expected.targetDepthCompleted());assertTrue(actual.targetDepthCompleted());
                assertEquals(expected.lastCompletedResult(),actual.lastCompletedResult());
            }
        }
    }
    @ParameterizedTest @EnumSource(value=BrnNextArchitecture.class,names={"GEN32_GATED","GEN32_CUBIC"})
    void hybridRoundingBoundariesRetainCanonicalScore(BrnNextArchitecture kind) {
        var positions=boards().subList(0,91);var last=positions.getLast();var base=base(41);
        double material=BrnPair2Features.material(last);
        for(double target:new double[]{-.5,.5,-37.5,37.5})for(double offset:new double[]{-1e-9,0,1e-9}) {
            var random=new SplittableRandom(13);double[] weights=new double[kind.parameters()];
            for(int i=1;i<weights.length;i++)weights[i]=random.nextDouble(-1,1);
            var initial=new BrnNextModel(kind,weights);
            double raw=initial.rawPawns(last,initial.newResearchWorkspace(base));
            weights[0]=material+((target+offset)/100-material)/.25-raw;
            var model=new BrnNextModel(kind,weights);
            var full=SearchEvaluation.brnResidualHybridFullResearch(model,base).newState(4);
            var fast=SearchEvaluation.brnResidualHybridResearch(model,base).newState(4);
            for(var board:positions)assertEquals(full.evaluate(board,0),fast.evaluate(board,0));
        }
    }
}
