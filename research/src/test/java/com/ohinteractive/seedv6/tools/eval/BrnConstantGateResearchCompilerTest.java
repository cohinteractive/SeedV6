package com.ohinteractive.seedv6.tools.eval;

import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.brnnext.*;
import com.ohinteractive.seedv6.core.brnpair2.*;
import com.ohinteractive.seedv6.search.common.SearchRequest;
import com.ohinteractive.seedv6.search.driver.SearchDriver;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrnConstantGateResearchCompilerTest {
    private static byte[] encode(BrnPair2Model model)throws IOException {
        var b=new ByteArrayOutputStream();BrnPair2Codec.writeModel(model,b);return b.toByteArray();
    }
    @Test void constantGatesIncludeAllRelationsAndMatchRandomLegalTransitions()throws Exception {
        var kind=BrnNextArchitecture.GEN32_GATED;var base=BrnNextBase.model();byte[] before=encode(base);
        for(long seed:new long[]{912,714}) {
            var random=new SplittableRandom(seed);var weights=new double[kind.parameters()];
            for(int i=0;i<weights.length;i++)if(i<BrnNextArchitecture.EMBEDDING||i>=kind.head())weights[i]=random.nextDouble(-.4,.4);
            var source=new BrnNextModel(kind,weights);var compiled=BrnConstantGateResearchCompiler.compile(base,source);
            assertEquals(298,compiled.model().pairs());assertArrayEquals(new int[]{1,6,48,55},compiled.extraFirst());
            assertArrayEquals(new int[]{8,15,57,62},compiled.extraSecond());
            var reference=source.newWorkspace();var actual=compiled.model().newWorkspace();
            var state=SearchEvaluation.brnPair2(compiled.model()).newState(2);var board=Board.startingPosition();
            long[] moves=new long[256],scratch=new long[Board.MAX_BITBOARDS];
            for(int i=0;i<2000;i++) {
                assertEquals(source.rawPawns(board,reference),actual.evaluatePawns(board),1e-10);
                assertEquals(source.score(board,reference),state.evaluate(board,0));
                int n=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,scratch);
                if(n==0||i%200==199)board=Board.startingPosition();
                else {long[] next=new long[Board.MAX_BITBOARDS];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[random.nextInt(n)],next);board=next;}
            }
            for(var fixture:BrnSuccessorAudit.boards())assertEquals(source.score(fixture,reference),state.evaluate(fixture,0));
            assertEquals(compiled.functionSha256(),BrnConstantGateResearchCompiler.compile(base,source).functionSha256());
            assertTrue(compiled.model().immutablePrimitiveBytes()<base.immutablePrimitiveBytes()+12000);
            var bytes=new ByteArrayOutputStream();assertThrows(IOException.class,()->BrnPair2Codec.writeModel(compiled.model(),bytes));assertEquals(0,bytes.size());
        }
        assertArrayEquals(before,encode(base));
    }
    @Test void separateOwnersAndCommonSearchRetainCanonicalOutcome()throws Exception {
        var kind=BrnNextArchitecture.GEN32_GATED;var w=new double[kind.parameters()];var random=new SplittableRandom(42);
        for(int i=0;i<w.length;i++)if(i<BrnNextArchitecture.EMBEDDING||i>=kind.head())w[i]=random.nextDouble(-.5,.5);
        var source=new BrnNextModel(kind,w);var compiled=BrnConstantGateResearchCompiler.compile(BrnNextBase.model(),source).model();
        var canonical=SearchEvaluation.brnNext(source);var folded=SearchEvaluation.brnPair2(compiled);
        var positions=BrnSuccessorAudit.boards();var a=folded.newState(2);var b=folded.newState(2);
        a.evaluate(positions.getFirst(),0);b.initializeFrom(positions.getLast(),0,a);
        assertEquals(canonical.newState(2).evaluate(positions.getLast(),0),b.evaluate(positions.getLast(),0));
        assertEquals(canonical.newState(2).evaluate(positions.getFirst(),0),a.evaluate(positions.getFirst(),0));
        for(var board:positions.subList(0,2))try(var full=new SearchDriver(BrnSuccessorAudit.common(canonical));var fast=new SearchDriver(BrnSuccessorAudit.common(folded))) {
            var expected=full.search(new SearchRequest(board,4));var actual=fast.search(new SearchRequest(board,4));
            assertTrue(expected.targetDepthCompleted());assertTrue(actual.targetDepthCompleted());assertEquals(expected.lastCompletedResult(),actual.lastCompletedResult());
        }
    }
    @Test void additionalPairsAreBoundedCopiedAndCannotEnterOrdinaryCodec()throws Exception {
        var base=BrnNextBase.model();int[] first={1},second={8};double[] tables=new double[338];tables[0]=1;
        var extended=BrnPair2Model.withResearchPairs(base,first,second,tables);double value=extended.newWorkspace().evaluatePawns(Board.startingPosition());
        first[0]=6;second[0]=15;Arrays.fill(tables,42);
        assertEquals(value,extended.newWorkspace().evaluatePawns(Board.startingPosition()));
        assertThrows(IllegalArgumentException.class,()->BrnPair2Model.withResearchPairs(base,new int[]{0},new int[]{1},new double[338]));
        assertThrows(IllegalArgumentException.class,()->BrnPair2Model.withResearchPairs(base,new int[]{1,1},new int[]{8,8},new double[676]));
        assertThrows(IllegalArgumentException.class,()->BrnPair2Model.withResearchPairs(base,new int[]{8},new int[]{1},new double[338]));
        assertThrows(IllegalArgumentException.class,()->BrnPair2Model.withResearchPairs(extended,new int[]{6},new int[]{15},new double[338]));
        double[] invalid=new double[338];invalid[1]=Double.NaN;
        assertThrows(IllegalArgumentException.class,()->BrnPair2Model.withResearchPairs(base,new int[]{1},new int[]{8},invalid));
        assertThrows(IOException.class,()->encode(extended));
        assertArrayEquals(encode(base),encode(BrnPair2Codec.readModel(new ByteArrayInputStream(encode(base)))));
        assertThrows(IllegalArgumentException.class,()->BrnConstantGateResearchCompiler.compile(base,new BrnNextTrainer(BrnNextArchitecture.GEN32_GATED,1,.001).snapshot()));
    }
}
