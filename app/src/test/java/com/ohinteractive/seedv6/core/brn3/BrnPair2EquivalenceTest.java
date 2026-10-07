package com.ohinteractive.seedv6.core.brn3;

import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.brnpair2.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.search.exact.*;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.training.model.*;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Uses the unchanged d23a05f research implementation as an independent integration oracle. */
class BrnPair2EquivalenceTest {
    static BrnPair2Model compile(BrnTupleTrainer.Model original) {
        return new BrnPair2Model(new BrnPair2Trainer.Model(2,original.weights));
    }
    static byte[] modelBytes(BrnPair2Model model)throws Exception {
        var bytes=new ByteArrayOutputStream();BrnPair2Codec.writeModel(model,bytes);return bytes.toByteArray();
    }
    @Test void trainerWeightsMomentsFoldingAndContinuationMatchResearchExactly()throws Exception {
        var reference=new BrnTupleTrainer(2,.01);var supported=new BrnPair2Trainer(.01);
        var boards=BrnTupleTrainerTest.examples();double[] targets={.25,-.5,.1};
        assertEquals(3,boards.length);
        for(int step=0;step<40;step++) {
            reference.trainBatch(boards,targets,boards.length);supported.trainBatch(boards,targets,boards.length);
            for(var board:boards)assertEquals(reference.predictPawns(board),supported.predictPawns(board),0);
        }
        byte[] saved=new NetworkTrainingState.BrnPair2(supported).encode();
        assertEquals(BrnPair2Codec.TRAINING_BYTES,saved.length);
        var in=new DataInputStream(new ByteArrayInputStream(saved));in.skipNBytes(44);
        assertEquals(reference.step(),in.readLong());assertEquals(reference.rate(),in.readDouble());
        for(var array:new double[][]{reference.weights,reference.first,reference.second})
            for(double value:array)assertEquals(Double.doubleToRawLongBits(value),Double.doubleToRawLongBits(in.readDouble()));
        var resumed=BrnPair2Codec.readTraining(new ByteArrayInputStream(saved));
        for(int step=0;step<8;step++) {
            supported.trainBatch(boards,targets,boards.length);resumed.trainBatch(boards,targets,boards.length);
            reference.trainBatch(boards,targets,boards.length);
        }
        assertArrayEquals(new NetworkTrainingState.BrnPair2(supported).encode(),new NetworkTrainingState.BrnPair2(resumed).encode());
        assertArrayEquals(modelBytes(compile(reference.snapshot())),modelBytes(new BrnPair2Model(supported.snapshot())));
        long before=resumed.step();resumed.setLearningRate(.005);assertEquals(before,resumed.step());
        assertEquals(.005,BrnPair2Codec.readTraining(new ByteArrayInputStream(new NetworkTrainingState.BrnPair2(resumed).encode())).rate());
    }
    @Test void legalSpecialUndoStmInformationAndSearchMatchCompiledC02()throws Exception {
        var source=BrnTupleCompiledTest.randomTrainer().snapshot();var model=compile(source);
        assertEquals(294,model.pairs());assertEquals(799688,model.immutablePrimitiveBytes());
        var oracle=new BrnTupleCompiled(source).newWorkspace();var cache=model.newWorkspace();
        assertEquals(2920,cache.primitiveBytes());
        var definition=SearchEvaluation.brnPair2(model);var state=definition.newState(32);
        var other=definition.newState(32);state.initialize(Board.startingPosition(),0);
        var random=new Random(8731);long[] moves=new long[512];
        var positions=new ArrayList<long[]>();
        for(String fen:new String[]{"r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 w - - 0 1"}) {
            var parent=Board.fromFen(fen);int count=Gen.genAll(parent[0],parent[1],parent[2],parent[3],(int)parent[4],parent[5],true,moves,new long[6]);
            for(int i=0;i<count;i++) {
                var child=new long[6];Board.makeMoveInto(parent[0],parent[1],parent[2],parent[3],(int)parent[4],parent[5],moves[i],child);
                positions.add(parent);positions.add(child);positions.add(parent);
            }
        }
        var board=Board.startingPosition();
        for(int ply=0;ply<2400;ply++) {
            positions.add(board);var altered=board.clone();altered[4]^=~Board.PLAYER_BIT;altered[5]^=76543;
            altered[3]^=~(altered[0]|altered[1]|altered[2]);positions.add(altered);
            var flip=board.clone();flip[4]^=Board.PLAYER_BIT;positions.add(flip);
            int count=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,new long[6]);
            if(count==0||ply%99==98){board=Board.startingPosition();continue;}
            var child=new long[6];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[random.nextInt(count)],child);board=child;
        }
        for(var position:positions) {
            assertEquals(oracle.evaluatePawns(position),cache.evaluatePawns(position),0);
            int expected=Brn3Model.score(oracle.evaluatePawns(position,.25));
            state.child(position,position,0);assertEquals(expected,state.evaluate(position,1));
            other.initializeFrom(position,1,state);assertEquals(expected,other.evaluate(position,1));
        }
        assertTrue(cache.updates>100);assertTrue(cache.rebuilds>1);
        assertThrows(IllegalArgumentException.class,()->state.initializeFrom(boardForMismatch(),0,SearchEvaluation.handcrafted().newState(1)));
        var referenceWorkspace=new BrnTupleCompiled(source).newWorkspace();
        var research=new ExactSearch((b,ply)->Brn3Model.score(referenceWorkspace.evaluatePawns(b,.25)),new TTable(4));
        var integrated=new ExactSearch(definition,new TTable(4));
        for(var position:List.of(Board.startingPosition(),BrnTupleTrainerTest.examples()[0])) {
            var history=GameHistory.initial(position);
            var expected=research.search(position,history,3,ExactSearch.NEVER_CANCELLED);
            var actual=integrated.search(position,history,3,ExactSearch.NEVER_CANCELLED);
            assertEquals(expected.score(),actual.score());assertEquals(expected.bestMove(),actual.bestMove());assertEquals(expected.nodes(),actual.nodes());
        }
        System.out.println("Pair-2/C02 rolling comparisons="+positions.size()+"; depth-3 score/move/nodes identical");
    }
    private static long[] boardForMismatch(){return Board.startingPosition();}
    @Test void loadedModelUsesNormalSingleAndParallelSearchWithPrivateCaches()throws Exception {
        var model=BrnPair2Codec.readModel(new ByteArrayInputStream(modelBytes(compile(BrnTupleCompiledTest.randomTrainer().snapshot()))));
        var definition=SearchEvaluation.brnPair2(model);var oracle=new ExactSearch(definition);
        for(int workers:new int[]{1,2})try(var search=com.ohinteractive.seedv6.search.driver.ProductionSearch.create(workers,definition)) {
            for(var board:BrnTupleTrainerTest.examples()) {
                int expected=oracle.search(board,3).score();
                var actual=search.search(new com.ohinteractive.seedv6.search.common.SearchRequest(board,3));
                assertTrue(actual.completed());assertFalse(actual.selective());assertEquals(expected,actual.score());
            }
        }
    }
    @Test void deterministicCompiledCodecRejectsFamiliesCorruptionVersionsAndResumeSubstitution()throws Exception {
        var model=compile(BrnTupleCompiledTest.randomTrainer().snapshot());byte[] bytes=modelBytes(model);
        assertEquals(BrnPair2Codec.MODEL_BYTES,bytes.length);
        assertArrayEquals(bytes,modelBytes(BrnPair2Codec.readModel(new ByteArrayInputStream(bytes))));
        assertThrows(IOException.class,()->BrnPair2Codec.readTraining(new ByteArrayInputStream(bytes)));
        for(var family:TrainingArchitecture.values())if(family!=TrainingArchitecture.BRN_PAIR2)
            assertThrows(IOException.class,()->NetworkModel.read(family,new ByteArrayInputStream(bytes)),family.name());
        var original=new ByteArrayOutputStream();BrnTupleCompiledTest.randomTrainer().snapshot().write(original);
        assertThrows(IOException.class,()->BrnPair2Codec.readModel(new ByteArrayInputStream(original.toByteArray())));
        for(int offset:new int[]{0,8,12,16,20,24,28,32,36,40}) {
            byte[] invalid=bytes.clone();invalid[offset]^=1;
            var crc=new java.util.zip.CRC32();crc.update(invalid,0,invalid.length-4);
            java.nio.ByteBuffer.wrap(invalid,invalid.length-4,4).putInt((int)crc.getValue());
            assertThrows(IOException.class,()->BrnPair2Codec.readModel(new ByteArrayInputStream(invalid)));
        }
        bytes[100]^=1;assertThrows(IOException.class,()->BrnPair2Codec.readModel(new ByteArrayInputStream(bytes)));
        assertThrows(IOException.class,()->BrnPair2Codec.readModel(new ByteArrayInputStream(new byte[100])));
        assertThrows(IllegalArgumentException.class,()->new BrnPair2Trainer.Model(3,new float[1]));
    }
}
