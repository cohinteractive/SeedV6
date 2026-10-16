package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MessageRelationCandidateTest {
    @Test void initialScaleTrainedSymmetryForbiddenInputsAndGradients() {
        var model=new MessageRelationCandidate(71);var board=Board.fromFen(RelationalCandidateTest.FEN);
        assertEquals(RelationalCandidate.material(board),model.predict(board),0);
        var rng=new Random(17);for(int i=MessageRelationCandidate.HEAD;i<model.weights.length;i++)model.weights[i]=rng.nextDouble()-.5;
        double prediction=model.predict(board);model.backward(board,1);var gradients=model.gradient.clone();var indices=Arrays.copyOf(model.touched,model.size);
        for(int n=0;n<indices.length;n+=71){int i=indices[n];double old=model.weights[i];model.weights[i]=old+1e-5;double plus=model.predict(board);
            model.weights[i]=old-1e-5;double minus=model.predict(board);model.weights[i]=old;assertEquals(gradients[i],(plus-minus)/2e-5,1e-8,"parameter "+i);}
        var reverse=board.clone();reverse[4]^=Board.PLAYER_BIT;assertEquals(-prediction,model.predict(reverse),1e-12);
        var ignored=Board.fromFen(RelationalCandidateTest.FEN.replace("KQkq - 2 3","- - 75 89"));ignored[5]^=1234;assertEquals(prediction,model.predict(ignored),0);
        var color=Board.fromFen("rnbqkb1r/pppp1ppp/5n2/4p3/4P3/2N5/PPPP1PPP/R1BQKBNR b KQkq - 2 3");assertEquals(prediction,model.predict(color),1e-12);
    }
    @Test void tinyOverfitAndPersistence()throws Exception{
        var model=new MessageRelationCandidate(71);var e=new BrnResearchData.Example(Board.fromFen(RelationalCandidateTest.FEN),80,78,0);
        for(int i=0;i<1000;i++)model.train(e,.001,false);assertEquals(.8,model.predict(e.board()),1e-5);
        var bytes=new ByteArrayOutputStream();model.write(bytes);var restored=MessageRelationCandidate.read(new ByteArrayInputStream(bytes.toByteArray()));
        for(int i=0;i<10;i++){model.train(e,.001,true);restored.train(e,.001,true);}
        assertArrayEquals(model.weights,restored.weights);assertArrayEquals(model.first,restored.first);assertArrayEquals(model.second,restored.second);
    }
}
