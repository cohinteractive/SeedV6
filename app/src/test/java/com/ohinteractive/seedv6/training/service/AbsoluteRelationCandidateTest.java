package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import java.io.*;
import java.util.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AbsoluteRelationCandidateTest {
    @TempDir Path temporary;
    @Test void typedPoolsPreserveGradientsMaterialAndExactContinuation() throws Exception {
        var model=new AbsoluteRelationCandidate(71,false,true,true,1,8,false,true);
        var board=Board.fromFen(RelationalCandidateTest.FEN);
        assertEquals(RelationalCandidate.material(board),model.predict(board),0);
        for(int i=model.HEAD;i<model.PARAMETERS;i++)model.weights[i]=.1;
        double value=model.predict(board);model.resetGradient();model.backward(1);
        var indices=Arrays.copyOf(model.touched,model.size);var expected=model.gradient.clone();
        for(int n=0;n<indices.length;n+=97){int i=indices[n];double old=model.weights[i];model.weights[i]=old+1e-6;double plus=model.predict(board);
            model.weights[i]=old-1e-6;double minus=model.predict(board);model.weights[i]=old;assertEquals(expected[i],(plus-minus)/2e-6,1e-7,"typed parameter "+i);}
        var reversed=Board.fromFen("rnbqkb1r/pppp1ppp/5n2/4p3/4P3/2N5/PPPP1PPP/R1BQKBNR b KQkq - 2 3");
        assertEquals(value,model.predict(reversed),1e-12);
        var examples=List.of(new BrnResearchData.Example(board,80,78,0));int[] order={0};
        for(int step=0;step<5;step++)model.trainBatch(examples,order,0,1,.003,true,0);
        var bytes=new ByteArrayOutputStream();model.write(bytes);var restored=AbsoluteRelationCandidate.read(new ByteArrayInputStream(bytes.toByteArray()));
        assertTrue(restored.typedPooling);assertEquals(96,restored.POOL_WIDTH);
        model.trainBatch(examples,order,0,1,.003,true,0);restored.trainBatch(examples,order,0,1,.003,true,0);
        assertArrayEquals(model.weights,restored.weights);assertArrayEquals(model.first,restored.first);assertArrayEquals(model.second,restored.second);
        var folded=model.foldedInference();var cache=new AbsoluteRelationInference(folded);
        assertEquals(model.predict(board),folded.predict(board),1e-10);AbsoluteRelationInferenceTest.check(folded,cache,board);
        var moves=new long[512];int count=BrnResearchDiagnostics.legal(board,moves);
        for(int i=0;i<count;i++)AbsoluteRelationInferenceTest.check(folded,cache,BrnResearchDiagnostics.child(board,moves[i]));
    }
    @Test void typedPoolingOverfitsDistinctOutcomeLabelsFromMaterialStart() {
        var model=new AbsoluteRelationCandidate(71,false,true,true,1,8,false,true);
        var examples=List.of(new BrnResearchData.Example(Board.fromFen(RelationalCandidateTest.FEN),80,78,0),
                new BrnResearchData.Example(Board.fromFen("4k3/8/8/3p4/3P4/8/4N3/4K3 w - - 0 1"),250,5,0),
                new BrnResearchData.Example(Board.fromFen("4k3/8/8/4q3/8/8/4R3/4K3 b - - 0 1"),-300,14,0));
        int[] order={0,1,2};for(int step=0;step<1800;step++)model.trainBatch(examples,order,0,3,.003,true,0);
        for(var example:examples)assertEquals(example.outcome(),RelationalCandidate.outcome(model.predict(example.board()),example.sfMaterial())[0],.002);
    }
    @Test void exactMaterialFiniteDifferenceAndPermittedSymmetry() {
        var model = new AbsoluteRelationCandidate(71); var board=Board.fromFen(RelationalCandidateTest.FEN);
        assertEquals(RelationalCandidate.material(board),model.predict(board),0);
        assertEquals(21.5,model.predict(Board.fromFen("4k3/8/8/8/8/8/PNBRQ3/4K3 w - - 0 1")),1e-12);
        for(int i=model.HEAD;i<model.weights.length;i++)model.weights[i]=.1;
        double value=model.predict(board);model.resetGradient();model.backward(1);
        var indices=Arrays.copyOf(model.touched,model.size);var expected=model.gradient.clone();
        for(int n=0;n<indices.length;n+=41){int i=indices[n];double old=model.weights[i];model.weights[i]=old+1e-5;double plus=model.predict(board);
            model.weights[i]=old-1e-5;double minus=model.predict(board);model.weights[i]=old;assertEquals(expected[i],(plus-minus)/2e-5,1e-8,"parameter "+i);}
        var changed=Board.fromFen(RelationalCandidateTest.FEN.replace("KQkq - 2 3","- - 75 89"));changed[5]^=888;
        assertEquals(value,model.predict(changed),0);
        var reversed=Board.fromFen("rnbqkb1r/pppp1ppp/5n2/4p3/4P3/2N5/PPPP1PPP/R1BQKBNR b KQkq - 2 3");
        assertEquals(value,model.predict(reversed),1e-12);
        assertEquals(model.edge(0,767),model.edge(767,0));
    }
    @Test void tinyDistinctSetAndExactResume() throws Exception {
        var model=new AbsoluteRelationCandidate(71);
        var examples=List.of(new BrnResearchData.Example(Board.fromFen(RelationalCandidateTest.FEN),80,78,0),
                new BrnResearchData.Example(Board.fromFen("4k3/8/8/3p4/3P4/8/4N3/4K3 w - - 0 1"),250,5,0),
                new BrnResearchData.Example(Board.fromFen("4k3/8/8/4q3/8/8/4R3/4K3 b - - 0 1"),300,14,0));
        int[] order={0,1,2};for(int n=0;n<1600;n++)model.trainBatch(examples,order,0,3,.003,false);
        for(var e:examples)assertEquals(e.cp()/100.0,model.predict(e.board()),.02);
        var path=temporary.resolve("state");try(var out=new BufferedOutputStream(Files.newOutputStream(path))){model.write(out);}
        AbsoluteRelationCandidate restored;try(var in=new BufferedInputStream(Files.newInputStream(path))){restored=AbsoluteRelationCandidate.read(in);}
        for(int n=0;n<5;n++){model.trainBatch(examples,order,0,3,.001,true);restored.trainBatch(examples,order,0,3,.001,true);}
        assertArrayEquals(model.weights,restored.weights);assertArrayEquals(model.first,restored.first);assertArrayEquals(model.second,restored.second);
    }
    @Test void singlePerspectiveHasCorrectGradientAndColorEquivalence() {
        var model=new AbsoluteRelationCandidate(71,true);var board=Board.fromFen(RelationalCandidateTest.FEN);
        for(int i=model.HEAD;i<model.weights.length;i++)model.weights[i]=.1;
        double value=model.predict(board);model.resetGradient();model.backward(1);
        var indices=Arrays.copyOf(model.touched,model.size);var expected=model.gradient.clone();
        for(int n=0;n<indices.length;n+=97){int i=indices[n];double old=model.weights[i];model.weights[i]=old+1e-5;double plus=model.predict(board);
            model.weights[i]=old-1e-5;double minus=model.predict(board);model.weights[i]=old;assertEquals(expected[i],(plus-minus)/2e-5,1e-8);}
        var reversed=Board.fromFen("rnbqkb1r/pppp1ppp/5n2/4p3/4P3/2N5/PPPP1PPP/R1BQKBNR b KQkq - 2 3");assertEquals(value,model.predict(reversed),1e-12);
    }
    @Test void rectifiedActivationGradientAndCheckpointFlags() throws Exception {
        var model=new AbsoluteRelationCandidate(71,false,true);var board=Board.fromFen(RelationalCandidateTest.FEN);
        for(int i=model.HEAD;i<model.weights.length;i++)model.weights[i]=.1;
        model.predict(board);model.resetGradient();model.backward(1);var indices=Arrays.copyOf(model.touched,model.size);var expected=model.gradient.clone();
        for(int n=0;n<indices.length;n+=67){int i=indices[n];double old=model.weights[i];model.weights[i]=old+1e-6;double plus=model.predict(board);
            model.weights[i]=old-1e-6;double minus=model.predict(board);model.weights[i]=old;assertEquals(expected[i],(plus-minus)/2e-6,1e-7);}
        var path=temporary.resolve("rectified");try(var out=new BufferedOutputStream(Files.newOutputStream(path))){model.write(out);}
        try(var in=new BufferedInputStream(Files.newInputStream(path))){var restored=AbsoluteRelationCandidate.read(in);assertTrue(restored.relu);assertFalse(restored.singlePerspective);assertEquals(model.predict(board),restored.predict(board),0);}
    }
    @Test void sharedRelativeBranchGradientIsCorrect() {
        var model=new AbsoluteRelationCandidate(71,false,true,true);var board=Board.fromFen(RelationalCandidateTest.FEN);
        for(int i=model.HEAD;i<=model.OUTPUT_BIAS;i++)model.weights[i]=.1;
        model.predict(board);model.resetGradient();model.backward(1);var indices=Arrays.copyOf(model.touched,model.size);var expected=model.gradient.clone();int checked=0;
        for(int n=0;n<indices.length;n+=37){int i=indices[n];double old=model.weights[i];model.weights[i]=old+1e-6;double plus=model.predict(board);
            model.weights[i]=old-1e-6;double minus=model.predict(board);model.weights[i]=old;assertEquals(expected[i],(plus-minus)/2e-6,1e-7);if(i>=model.PARAMETERS)checked++;}
        assertTrue(checked>10);
        var rng=new Random(99);for(int i=model.PARAMETERS;i<model.weights.length;i++)model.weights[i]=(rng.nextDouble()-.5)*.3;
        var folded=model.foldedInference();assertFalse(folded.relative);
        assertEquals(model.predict(board),folded.predict(board),1e-12);
        assertEquals(model.predict(Board.fromFen("4k3/8/8/3p4/3P4/8/4N3/4K3 b - - 0 1")),folded.predict(Board.fromFen("4k3/8/8/3p4/3P4/8/4N3/4K3 b - - 0 1")),1e-12);
    }
    @Test void widerRelationsRetainGradientAndSerialization() throws Exception {
        var model=new AbsoluteRelationCandidate(71,false,true,true,.25,16);var board=Board.fromFen(RelationalCandidateTest.FEN);
        assertEquals(RelationalCandidate.material(board),model.predict(board),0);
        for(int i=model.HEAD;i<=model.OUTPUT_BIAS;i++)model.weights[i]=.1;
        model.predict(board);model.resetGradient();model.backward(1);int[] indices=Arrays.copyOf(model.touched,model.size);
        for(int n=0;n<indices.length;n+=113){int i=indices[n];double expected=model.gradient[i],old=model.weights[i];model.weights[i]=old+1e-6;double plus=model.predict(board);
            model.weights[i]=old-1e-6;double minus=model.predict(board);model.weights[i]=old;assertEquals(expected,(plus-minus)/2e-6,1e-7);}
        var path=temporary.resolve("wide");try(var out=new BufferedOutputStream(Files.newOutputStream(path))){model.write(out);}
        try(var in=new BufferedInputStream(Files.newInputStream(path))){var restored=AbsoluteRelationCandidate.read(in);assertEquals(16,restored.WIDTH);assertTrue(restored.relative);assertEquals(model.predict(board),restored.predict(board),0);}
    }
    @Test void conventionalAdamUpdatesAbsentMomentsAndResumes() throws Exception {
        var model=new AbsoluteRelationCandidate(71,false,true,true,.25,8,true);int parameter=13;
        model.resetGradient();model.add(parameter,2);model.update(1,.01);double before=model.weights[parameter];
        model.resetGradient();model.add(model.OUTPUT_BIAS,1);model.update(1,.01);
        double m=.9*.2,v=.999*.004,expected=before-.01*(m/(1-.9*.9))/(Math.sqrt(v/(1-.999*.999))+1e-8);
        assertEquals(expected,model.weights[parameter],1e-12);assertEquals(m,model.first[parameter],1e-12);assertEquals(v,model.second[parameter],1e-12);
        var path=temporary.resolve("dense-adam");try(var out=new BufferedOutputStream(Files.newOutputStream(path))){model.write(out);}
        try(var in=new BufferedInputStream(Files.newInputStream(path))){var restored=AbsoluteRelationCandidate.read(in);assertTrue(restored.denseAdam);
            var e=new BrnResearchData.Example(Board.fromFen(RelationalCandidateTest.FEN),80,78,0);var samples=List.of(e);int[] order={0};
            for(int n=0;n<3;n++){model.trainBatch(samples,order,0,1,.003,true,0);restored.trainBatch(samples,order,0,1,.003,true,0);}
            assertArrayEquals(model.weights,restored.weights);assertArrayEquals(model.first,restored.first);assertArrayEquals(model.second,restored.second);}
    }
}
