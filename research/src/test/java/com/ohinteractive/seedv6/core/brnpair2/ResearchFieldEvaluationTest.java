package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ResearchFieldEvaluationTest {
    @TempDir Path directory;
    @Test void researchCentipawnScaleBoundsFiniteChecksAndStateOwnership() {
        var definition=SearchEvaluation.researchCentipawnsFullRecompute(()->board->Double.longBitsToDouble(board[5]));var state=definition.newState(257);long[] board=Board.startingPosition();
        for(double cp:new double[]{0,.49,.5,1.5,400,32511,1e20})for(int sign:new int[]{-1,1}) {
            board[5]=Double.doubleToLongBits(cp*sign);int expected=sign*(int)Math.round(Math.min(cp,32511));assertEquals(expected,state.evaluate(board,256));assertFalse(TranspositionScores.isMateScore(state.evaluate(board,256)));
        }
        for(double cp:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}){board[5]=Double.doubleToLongBits(cp);assertThrows(ArithmeticException.class,()->state.evaluate(board,0));}
        assertThrows(IndexOutOfBoundsException.class,()->state.evaluate(board,257));assertThrows(IndexOutOfBoundsException.class,()->state.child(board,board,256));assertThrows(IllegalArgumentException.class,()->definition.newState(0));
        var counter=SearchEvaluation.researchCentipawnsFullRecompute(()->{int[] n={0};return ignored->n[0]++;});var a=counter.newState(2);var b=counter.newState(2);assertEquals(0,a.evaluate(board,0));assertEquals(1,a.evaluate(board,0));assertEquals(0,b.evaluate(board,0));
        assertThrows(IllegalArgumentException.class,()->state.initializeFrom(board,0,a));assertFalse(definition.isBrnPair2());assertFalse(definition.isConventional());
        var fresh=Board.startingPosition();assertEquals(Eval.evaluate(fresh),SearchEvaluation.handcrafted().newState(2).evaluate(fresh,0));
    }
    @Test void constructionAdmitsExplicitResearchCentipawnsWithoutBroadeningOtherMappings()throws Exception {
        var definition=SearchEvaluation.researchCentipawnsFullRecompute(()->board->0);
        var configuration=com.ohinteractive.seedv6.search.exact.ConstructionSearch.v1(1,1,com.ohinteractive.seedv6.search.driver.AspirationSearch.Policy.ADAPTIVE16);
        try(var driver=new com.ohinteractive.seedv6.search.driver.SearchDriver(configuration.create(definition))) {
            var result=driver.search(new com.ohinteractive.seedv6.search.common.SearchRequest(Board.startingPosition(),2));assertTrue(result.targetDepthCompleted());assertEquals(0,driver.activeSearchThreads());
        }
        var unsupported=SearchEvaluation.blendResearch(SearchEvaluation.handcrafted(),SearchEvaluation.handcrafted(),.5);
        assertThrows(IllegalArgumentException.class,()->configuration.create(unsupported));
    }
    @Test void loadedNeuralDefinitionMatchesFullScoresAcrossSpecialMovesParentsSiblingsAndQsearch()throws Exception {
        var model=new ComposedAttackFieldNetwork(true,.01,71);var random=new Random(97);for(int i=ComposedAttackFieldNetwork.G;i<model.weights.length;i++)model.weights[i]=.2*random.nextGaussian();
        Path file=directory.resolve("neural.state");try(var out=Files.newOutputStream(file)){model.write(out);}var definition=ResearchFieldEvaluation.load(file);var main=definition.newState(257);var q=definition.newState(257);var other=definition.newState(257);var oracle=new ComposedAttackFieldNetwork.Workspace();
        long[] moves=new long[256],scratch=new long[6];
        for(String fen:List.of("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 b - - 0 1")) {
            long[] board=Board.fromFen(fen),saved=board.clone();main.initialize(board,0);int parent=main.evaluate(board,0);int count=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,scratch);
            for(int i=count-1;i>=0;i--) {
                var child=new long[6];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[i],child);main.child(board,child,0);q.initializeFrom(child,1,main);other.initialize(child,256);
                double cp=model.scoreCp(child,oracle);int expected=(cp<0?-1:1)*(int)Math.round(Math.min(Math.abs(cp),32511));assertEquals(expected,main.evaluate(child,1));assertEquals(expected,q.evaluate(child,1));assertEquals(expected,other.evaluate(child,256));assertEquals(parent,main.evaluate(board,0));assertArrayEquals(saved,board);
            }
        }
    }
}
