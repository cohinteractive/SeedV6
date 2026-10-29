package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ResearchMixedTiedEvaluationTest {
    @TempDir Path directory;
    @Test void loadedNeuralDefinitionMatchesFullScoresAcrossSpecialMovesParentsSiblingsAndQsearch()throws Exception {
        var model=new MixedTiedPieceNetwork(true,.01,71);var random=new Random(97);for(int i=MixedTiedPieceNetwork.E;i<model.weights.length;i++)model.weights[i]=.2*random.nextGaussian();
        Path file=directory.resolve("neural.state");try(var out=Files.newOutputStream(file)){model.write(out);}var definition=ResearchMixedTiedEvaluation.load(file);
        var configuration=com.ohinteractive.seedv6.search.exact.ConstructionSearch.v1(1,1,com.ohinteractive.seedv6.search.driver.AspirationSearch.Policy.ADAPTIVE16);
        try(var driver=new com.ohinteractive.seedv6.search.driver.SearchDriver(configuration.create(definition))){var result=driver.search(new com.ohinteractive.seedv6.search.common.SearchRequest(Board.startingPosition(),2));assertTrue(result.targetDepthCompleted());assertEquals(0,driver.activeSearchThreads());}
        var invalid=new MixedTiedPieceNetwork(false,.01,71);Path bad=directory.resolve("wrong-mode.state");try(var out=Files.newOutputStream(bad)){invalid.write(out);}assertThrows(java.io.IOException.class,()->ResearchMixedTiedEvaluation.load(bad));var old=new ContextualPieceNetwork(false,.01,71);Path oldFile=directory.resolve("wrong-format.state");try(var out=Files.newOutputStream(oldFile)){old.write(out);}assertThrows(java.io.IOException.class,()->ResearchMixedTiedEvaluation.load(oldFile));var main=definition.newState(257);var q=definition.newState(257);var other=definition.newState(257);var oracle=new MixedTiedPieceNetwork.Workspace();
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
