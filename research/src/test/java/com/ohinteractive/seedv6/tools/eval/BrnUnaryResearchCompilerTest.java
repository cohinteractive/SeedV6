package com.ohinteractive.seedv6.tools.eval;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnnext.*;
import com.ohinteractive.seedv6.core.brnpair2.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrnUnaryResearchCompilerTest {
    private static byte[] encode(BrnPair2Model model)throws IOException {
        var bytes=new ByteArrayOutputStream();BrnPair2Codec.writeModel(model,bytes);return bytes.toByteArray();
    }
    @Test void allSquaresCategoriesAndPerspectivesMatchUnaryAlgebraAndPreserveBase()throws Exception {
        var kind=BrnNextArchitecture.GEN32_CUBIC;var random=new SplittableRandom(732);
        double[] weights=new double[kind.parameters()];for(int i=0;i<BrnNextArchitecture.EMBEDDING;i++)weights[i]=random.nextDouble(-.4,.4);
        var unary=new BrnNextModel(kind,weights);var base=BrnNextBase.model();byte[] before=encode(base);
        var folded=BrnUnaryResearchCompiler.compile(base,unary);var actual=folded.newWorkspace();var expected=unary.newWorkspace();
        // Explicit placements cover all64 squares,12 occupied categories,both STM orientations.
        for(int square=0;square<64;square++)for(int code=1;code<16;code++)if((code&7)>=1&&(code&7)<=6)for(int side=0;side<2;side++) {
            long[] board=new long[Board.MAX_BITBOARDS];long bit=1L<<square;
            for(int plane=0;plane<4;plane++)if((code&(1<<plane))!=0)board[plane]|=bit;
            board[4]=side==0?0:Board.PLAYER_BIT;
            assertEquals(unary.rawPawns(board,expected),actual.evaluatePawns(board),1e-10);
        }
        assertArrayEquals(before,encode(base));assertEquals(base.immutablePrimitiveBytes(),folded.immutablePrimitiveBytes());
        assertEquals(base.newWorkspace().primitiveBytes(),actual.primitiveBytes());
        assertEquals(BrnPair2Codec.MODEL_BYTES,encode(folded).length);
    }
    @Test void canonicalBoardsSpecialMovesAndSourceReloadRetainScores()throws Exception {
        var kind=BrnNextArchitecture.GEN32_GATED;double[] w=new double[kind.parameters()];var random=new SplittableRandom(72);
        for(int i=0;i<BrnNextArchitecture.EMBEDDING;i++)w[i]=random.nextDouble(-1,1);
        var unary=new BrnNextModel(kind,w);var base=BrnSuccessorVariants.additiveChange(BrnNextBase.model(),.5);
        var folded=BrnUnaryResearchCompiler.compile(base,unary);var compiled=SearchEvaluation.brnPair2(folded).newState(4);
        var reference=unary.newResearchWorkspace(base);
        for(int pass=0;pass<5;pass++)for(var board:BrnSuccessorAudit.boards()) {
            assertEquals(unary.score(board,reference),compiled.evaluate(board,0));
            var emptyOwner=board.clone();emptyOwner[3]^=~(board[0]|board[1]|board[2]);
            assertEquals(unary.score(board,reference),compiled.evaluate(emptyOwner,0));
        }
        assertArrayEquals(encode(folded),encode(BrnPair2Codec.readModel(new ByteArrayInputStream(encode(folded)))));
    }
    @Test void rejectsNonUnaryOrUnboundCorrectionsAndNonfiniteCompilation()throws Exception {
        var kind=BrnNextArchitecture.GEN32_CUBIC;double[] w=new double[kind.parameters()];w[kind.head()]=1;
        assertThrows(IllegalArgumentException.class,()->BrnUnaryResearchCompiler.compile(BrnNextBase.model(),new BrnNextModel(kind,w)));
        assertThrows(IllegalArgumentException.class,()->BrnUnaryResearchCompiler.compile(BrnNextBase.model(),new BrnNextTrainer(BrnNextArchitecture.CUBIC,1,.001).snapshot()));
        double[] huge=new double[kind.parameters()];Arrays.fill(huge,1,BrnNextArchitecture.EMBEDDING,Double.MAX_VALUE);
        assertThrows(IOException.class,()->BrnUnaryResearchCompiler.compile(BrnNextBase.model(),new BrnNextModel(kind,huge)));
    }
}
