package com.ohinteractive.seedv6.core.brnnext;

import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.brnpair2.ProductionModel;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.CRC32;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class BrnGen32ResidualTest {
    @ParameterizedTest @EnumSource(value=BrnNextArchitecture.class,names={"GEN32_GATED","GEN32_CUBIC"})
    void repeatedPlacementAndNullPerspectiveReuseCorrectMaterial(BrnNextArchitecture kind) {
        var random=new SplittableRandom(221);var weights=new double[kind.parameters()];
        for(int i=0;i<weights.length;i++)weights[i]=random.nextDouble(-.2,.2);
        var model=new BrnNextModel(kind,weights);var scalar=model.newWorkspace();
        var cached=SearchEvaluation.brnGen32Residual(model).newState(64);
        var board=Board.fromFen("4k3/8/8/8/8/8/P7/3QK3 w - - 0 1");
        var base=BrnNextBase.model().newWorkspace();
        assertThrows(IllegalStateException.class,()->base.materialPawns(0));
        base.evaluatePawns(board);
        assertEquals(10,base.materialPawns(0),1e-12);assertEquals(-10,base.materialPawns(1),1e-12);
        assertThrows(IllegalStateException.class,()->base.materialPawns(2));
        for(int i=0;i<80;i++) {
            assertEquals(model.score(board,scalar),cached.evaluate(board,0));
            assertEquals(model.score(board,scalar),cached.evaluate(board,0));
            board[4]^=Board.PLAYER_BIT;
            board[3]^=~(board[0]|board[1]|board[2]);
        }
    }
    @ParameterizedTest @EnumSource(value=BrnNextArchitecture.class,names={"GEN32_GATED","GEN32_CUBIC"})
    void zeroHeadPreservesActualReferenceAndProductionSearch(BrnNextArchitecture kind) throws Exception {
        var random=new SplittableRandom(71);var weights=new double[kind.parameters()];
        for(int i=BrnNextArchitecture.EMBEDDING;i<kind.head();i++)weights[i]=random.nextDouble(-.5,.5);
        var model=new BrnNextModel(kind,weights);var full=model.newWorkspace();
        var base=ProductionModel.bundled();var rawBase=base.newWorkspace();
        var reference=SearchEvaluation.brnPair2(base);var residual=SearchEvaluation.brnGen32Residual(model);
        assertFalse(residual.isBrnPair2());assertTrue(residual.isBrnGen32Residual());
        var a=reference.newState(64);var b=residual.newState(64);var other=residual.newState(64);
        long[] board=Board.startingPosition(),moves=new long[256],scratch=new long[Board.MAX_BITBOARDS];
        for(int i=0;i<2000;i++) {
            assertEquals(rawBase.evaluatePawns(board),model.rawPawns(board,full),1e-12);
            int expected=a.evaluate(board,0);assertEquals(expected,b.evaluate(board,0));
            other.initializeFrom(board,0,b);assertEquals(expected,other.evaluate(board,0));
            int n=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,scratch);
            if(n==0||i%80==79){board=Board.startingPosition();continue;}
            var child=new long[Board.MAX_BITBOARDS];
            Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[random.nextInt(n)],child);board=child;
        }
        for(String fen:List.of("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "r3k2r/8/8/3pP3/8/8/8/R3K2R w KQkq d6 0 1",
                "4k3/P7/8/8/8/8/7p/4K3 w - - 0 1")) {
            board=Board.fromFen(fen);
            try(var original=new SearchDriver(ProductionSearch.create(1,reference,1));
                var candidate=new SearchDriver(ProductionSearch.create(1,residual,1))) {
                var expected=original.search(new SearchRequest(board,4));var actual=candidate.search(new SearchRequest(board,4));
                assertTrue(expected.targetDepthCompleted());assertTrue(actual.targetDepthCompleted());
                assertEquals(expected.lastCompletedResult(),actual.lastCompletedResult());
            }
        }
    }
    @ParameterizedTest @EnumSource(value=BrnNextArchitecture.class,names={"GEN32_GATED","GEN32_CUBIC"})
    void nonzeroCorrectionMatchesScalarAndBaseBindingCannotBeReinterpreted(BrnNextArchitecture kind) throws Exception {
        var random=new SplittableRandom(97);var weights=new double[kind.parameters()];
        for(int i=0;i<weights.length;i++)weights[i]=random.nextDouble(-1,1);
        var model=new BrnNextModel(kind,weights);var scalar=model.newWorkspace();
        var cached=SearchEvaluation.brnGen32Residual(model).newState(64);
        for(String fen:List.of("r3k2r/8/8/3pP3/8/8/8/R3K2R w KQkq d6 0 1",
                "4k3/P7/8/8/8/8/7p/4K3 w - - 0 1")) {
            var board=Board.fromFen(fen);long[] moves=new long[256],scratch=new long[Board.MAX_BITBOARDS];
            int n=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,scratch);
            int original=model.score(board,scalar);
            for(int i=0;i<n;i++) {
                var child=new long[Board.MAX_BITBOARDS];
                Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[i],child);
                assertEquals(model.score(child,scalar),cached.evaluate(child,0));assertEquals(original,cached.evaluate(board,0));
            }
            board[4]^=Board.PLAYER_BIT;assertEquals(model.score(board,scalar),cached.evaluate(board,0));
        }
        var output=new ByteArrayOutputStream();BrnNextCodec.writeModel(model,output);byte[] bytes=output.toByteArray();
        assertEquals(BrnNextCodec.modelBytes(kind),bytes.length);
        assertArrayEquals(HexFormat.of().parseHex(BrnNextBase.SHA256),Arrays.copyOfRange(bytes,36,68));
        bytes[36]^=1;var crc=new CRC32();crc.update(bytes,0,bytes.length-4);
        ByteBuffer.wrap(bytes,bytes.length-4,4).putInt((int)crc.getValue());
        var failure=assertThrows(IOException.class,()->BrnNextCodec.readModel(kind,new ByteArrayInputStream(bytes)));
        assertTrue(failure.getMessage().contains("base identity"));
    }
}
