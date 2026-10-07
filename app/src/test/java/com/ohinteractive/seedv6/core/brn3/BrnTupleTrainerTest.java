package com.ohinteractive.seedv6.core.brn3;

import com.ohinteractive.seedv6.core.*;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrnTupleTrainerTest {
    static long[][] examples(){return BrnEnergyTrainerTest.examples();}
    /** Independent direct addressing from raw squares, no cached codes/state helper. */
    static double direct(BrnTupleTrainer.Model model,long[] board) {
        int stm=Board.player((int)board[4]);double sum=0;
        for(int role=0;role<2;role++)for(int t=0;t<327;t++) {
            int perspective=stm^role,state=0,multiplier=1;
            for(int j=0;j<model.order;j++) {
                int sq=BrnTupleTrainer.CELLS[t][j]^(perspective*56),code=Board.getSquare(board[0],board[1],board[2],board[3],sq);
                int category=code==0?0:(code&7)+(((code>>>3)^perspective)&1)*6;
                state+=category*multiplier;multiplier*=13;
            }
            sum+=model.weights[(role*327+t)*model.states+state];
        }
        return Brn3Features.material(board)+model.weights[model.weights.length-1]+sum/Math.sqrt(654);
    }
    @Test void geometryIndependentForwardAndGradients() {
        assertEquals(327,BrnTupleTrainer.CELLS.length);var unique=new HashSet<String>();int[] shapes=new int[8];
        for(int t=0;t<327;t++){assertTrue(unique.add(Arrays.toString(BrnTupleTrainer.CELLS[t])));shapes[BrnTupleTrainer.SHAPE[t]]++;}
        assertArrayEquals(new int[]{48,48,36,36,49,49,36,25},shapes);
        for(int order:new int[]{2,3}) {
            var model=new BrnTupleTrainer(order,.003);var b=examples()[0];assertEquals(Brn3Features.material(b),model.predictPawns(b),0);
            var random=new Random(71);for(int i=0;i<model.weights.length;i++)model.weights[i]=random.nextDouble()*.2-.1;
            assertEquals(direct(model.snapshot(),b),model.predictPawns(b),1e-7);
            model.predictPawns(b);model.resetGradient();model.backward(1);double[] g=model.gradient.clone();
            for(int lo:new int[]{0,model.bias,model.modelParameters}) {
                int hi=lo==0?model.bias:lo==model.bias?model.bias+1:g.length,chosen=0;
                for(int i=lo;i<hi&&chosen<16;i++)if(Math.abs(g[i])>1e-12) {
                    double prior=model.weights[i];model.weights[i]=prior+1e-6;double plus=model.predictPawns(b);
                    model.weights[i]=prior-1e-6;double minus=model.predictPawns(b);model.weights[i]=prior;
                    assertEquals(g[i],(plus-minus)/2e-6,2e-8);chosen++;
                }
                assertTrue(chosen>0);
            }
        }
    }
    @Test void tinyOverfitInformationBoundaryAndCoverage() {
        var boards=examples();double[] targets={.2,-.3,-.4};
        for(int order:new int[]{2,3}) {
            var model=new BrnTupleTrainer(order,.003);assertEquals(1,model.unseenActiveFraction(boards),0);
            for(int i=0;i<1000;i++)model.trainBatch(boards,targets,3);
            assertEquals(0,model.unseenActiveFraction(boards),0);assertTrue(model.seenAbsoluteStates()>0);
            for(int i=0;i<boards.length;i++)assertEquals(targets[i],Brn3Objective.smoothOutcome(model.predictPawns(boards[i]),com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets.material(boards[i]))[0],.015);
            var altered=boards[0].clone();altered[4]=(altered[4]&Board.PLAYER_BIT)|(~altered[4]&~Board.PLAYER_BIT);altered[5]^=76543;
            assertEquals(model.predictPawns(boards[0]),model.predictPawns(altered),0);
            var reversed=Board.fromFen("rnbqkb1r/pppp1ppp/5n2/4p3/4P3/2N5/PPPP1PPP/R1BQKBNR b KQkq - 2 3");
            assertEquals(model.predictPawns(boards[0]),model.predictPawns(reversed),1e-12);
        }
    }
    @Test void exactContinuationCodecAndImmutableModel()throws Exception {
        for(int order:new int[]{2,3}) {
            var model=new BrnTupleTrainer(order,.003);var boards=examples();double[] targets={.2,-.3,-.4};
            for(int i=0;i<6;i++)model.trainBatch(boards,targets,3);
            var bytes=new ByteArrayOutputStream();model.write(bytes);byte[] state=bytes.toByteArray();
            var restored=BrnTupleTrainer.read(new ByteArrayInputStream(state));model.trainBatch(boards,targets,3);restored.trainBatch(boards,targets,3);
            assertArrayEquals(model.weights,restored.weights);assertArrayEquals(model.first,restored.first);assertArrayEquals(model.second,restored.second);assertEquals(model.seen,restored.seen);assertEquals(model.step(),restored.step());
            state[state.length/2]^=1;assertThrows(IOException.class,()->BrnTupleTrainer.read(new ByteArrayInputStream(state)));
            bytes.reset();model.snapshot().write(bytes);byte[] payload=bytes.toByteArray();var snapshot=BrnTupleTrainer.Model.read(new ByteArrayInputStream(payload));
            var work=snapshot.newWorkspace();double frozen=work.evaluatePawns(boards[0]);assertEquals(model.predictPawns(boards[0]),frozen,1e-6);
            Arrays.fill(model.weights,123);assertEquals(frozen,work.evaluatePawns(boards[0]),0);
            payload[payload.length/2]^=1;assertThrows(IOException.class,()->BrnTupleTrainer.Model.read(new ByteArrayInputStream(payload)));
            assertThrows(IOException.class,()->BrnTupleTrainer.Model.read(new ByteArrayInputStream(Arrays.copyOf(payload,24))));
        }
    }
    @Test void cachedParityAcrossLegalSpecialUndoAndStmTransitions() {
        String[] special={"r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 w - - 0 1"};
        for(int order:new int[]{2,3}) {
            var trainer=new BrnTupleTrainer(order,.003);var random=new Random(97);
            for(int i=0;i<trainer.weights.length;i++)trainer.weights[i]=random.nextDouble()-.5;
            var model=trainer.snapshot();var work=model.newWorkspace();long[] moves=new long[512];
            for(String fen:special) {
                long[] parent=Board.fromFen(fen);int n=Gen.genAll(parent[0],parent[1],parent[2],parent[3],(int)parent[4],parent[5],true,moves,new long[6]);
                for(int i=0;i<n;i++){var child=new long[6];Board.makeMoveInto(parent[0],parent[1],parent[2],parent[3],(int)parent[4],parent[5],moves[i],child);
                    assertEquals(direct(model,parent),work.evaluatePawns(parent),1e-10);assertEquals(direct(model,child),work.evaluatePawns(child),1e-10);assertEquals(direct(model,parent),work.evaluatePawns(parent),1e-10);}
            }
            var board=Board.startingPosition();
            for(int ply=0;ply<800;ply++) {
                assertEquals(direct(model,board),work.evaluatePawns(board),1e-10);
                var flip=board.clone();flip[4]^=Board.PLAYER_BIT;assertEquals(direct(model,flip),work.evaluatePawns(flip),1e-10);
                assertEquals(direct(model,board),work.evaluatePawns(board),1e-10);
                int n=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,new long[6]);
                if(n==0||ply%99==98){board=Board.startingPosition();continue;}
                var child=new long[6];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[random.nextInt(n)],child);board=child;
            }
            assertTrue(work.updates>100);assertTrue(work.rebuilds>1);assertTrue(work.repeats>100);
        }
    }
}
