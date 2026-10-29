package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ResearchOneEvaluationTest {
    @TempDir Path directory;
    @Test void loadedStatePreservesSpecialChildrenQsearchCopiesAndOwnership()throws Exception{
        var model=new TrainableOne(16,4,.01,71);Path file=directory.resolve("one.state");try(var out=Files.newOutputStream(file)){model.write(out);}
        var definition=ResearchOneEvaluation.load(file);var main=definition.newState(257);var q=definition.newState(257);
        for(String fen:IncrementalOneTest.SPECIAL){var board=Board.fromFen(fen);main.initialize(board,0);int parent=main.evaluate(board,0);long[] moves=new long[256],scratch=new long[6];int count=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,scratch);
            for(int i=0;i<count;i++){long[] child=new long[6];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[i],child);main.child(board,child,0);q.initializeFrom(child,1,main);double cp=model.scoreCp(child);int expected=(cp<0?-1:1)*(int)Math.round(Math.min(Math.abs(cp),32511));assertEquals(expected,main.evaluate(child,1));assertEquals(expected,q.evaluate(child,1));assertEquals(parent,main.evaluate(board,0));}
        }
        var configuration=com.ohinteractive.seedv6.search.exact.ConstructionSearch.v1(1,1,com.ohinteractive.seedv6.search.driver.AspirationSearch.Policy.ADAPTIVE16);
        try(var driver=new com.ohinteractive.seedv6.search.driver.SearchDriver(configuration.create(definition))){var result=driver.search(new com.ohinteractive.seedv6.search.common.SearchRequest(Board.startingPosition(),2));assertTrue(result.targetDepthCompleted());assertEquals(0,driver.activeSearchThreads());}
        Path wrong=directory.resolve("reference.state");try(var out=Files.newOutputStream(wrong)){new LowRankRelationNetwork(false,.01,71).write(out);}assertThrows(java.io.IOException.class,()->ResearchOneEvaluation.load(wrong));
    }
}
