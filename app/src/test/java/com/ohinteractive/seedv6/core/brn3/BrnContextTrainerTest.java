package com.ohinteractive.seedv6.core.brn3;

import static com.ohinteractive.seedv6.core.brn3.Brn3Layout.*;
import com.ohinteractive.seedv6.core.*;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrnContextTrainerTest {
    @Test void exactLinearIdentityAndIndependentDirectedContext() {
        var plain=new BrnEnergyTrainer(0,71,.003);
        for(boolean context:new boolean[]{false,true}) {
            var model=new BrnContextTrainer(context,71,.003);
            assertEquals(plain.parameters()+(context?136:72),model.parameters());
            for(int i=0;i<DENSE;i++)assertEquals(plain.weights[i],model.weights[i],0);
            for(var board:BrnEnergyTrainerTest.examples())assertEquals(Brn3Features.material(board),model.predictPawns(board),0);
            for(int i=DENSE;i<=plain.output;i++)plain.weights[i]=model.weights[i]=.13*Math.sin(i-DENSE+.7);
            for(var board:BrnEnergyTrainerTest.examples())assertEquals(plain.predictPawns(board),model.predictPawns(board),0);
            for(int i=model.self;i<model.modelParameters;i++)model.weights[i]=.11*Math.cos(i-model.self+.3);
            for(int i=model.modelParameters;i<model.trainingParameters;i++)model.weights[i]=.01*Math.sin(i-model.modelParameters);
            var boards=new ArrayList<long[]>(Arrays.asList(BrnEnergyTrainerTest.examples()));
            boards.add(Board.fromFen("8/8/8/8/8/8/8/4K3 w - - 0 1"));
            boards.add(Board.fromFen("8/8/8/8/8/8/8/8 w - - 0 1"));
            for(var board:boards) {
                assertEquals(directed(model,board),model.predictPawns(board),1e-12);
                assertEquals(model.predictPawns(board),model.snapshot().newWorkspace().evaluatePawns(board),1e-6);
            }
        }
    }
    // Independent directed sums, including an explicit sum over j != i for each message.
    private static double directed(BrnContextTrainer m,long[] board) {
        double expected=Brn3Features.material(board)+m.weights[m.output];
        for(int role=0;role<2;role++) {
            var ids=new ArrayList<Integer>();int p=Board.player((int)board[4])^role;
            for(int sq=0;sq<64;sq++){int code=Board.getSquare(board[0],board[1],board[2],board[3],sq);if(code!=0)ids.add(Brn3Features.channel(code^(p<<3))*64+(sq^(p*56)));}
            double[][] h=new double[ids.size()][8];
            for(int i=0;i<ids.size();i++)for(int c=0;c<8;c++) {
                int a=ids.get(i);double v=m.weights[NODES+a*8+c];
                for(int j=0;j<ids.size();j++)if(j!=i){int row=Brn3Layout.edge(a,ids.get(j));v+=(m.weights[row+c]+m.weights[m.modelParameters+Brn3Trainer.RELATIVE_ROW[row/8]*8+c])/Math.sqrt(Math.max(1,ids.size()-1));}
                h[i][c]=Math.max(0,v);
            }
            for(int i=0;i<ids.size();i++)for(int c=0;c<8;c++) {
                double v=h[i][c]+m.weights[m.bias+c];
                for(int d=0;d<8;d++) {
                    v+=m.weights[m.self+c*8+d]*h[i][d];
                    if(m.context)for(int j=0;j<ids.size();j++)if(j!=i)v+=m.weights[m.other+c*8+d]*h[j][d]/Math.max(1,ids.size()-1);
                }
                expected+=m.weights[DENSE+role*96+ids.get(i)/64*8+c]*Math.max(0,v)/Math.sqrt(Math.max(1,ids.size()));
            }
        }
        return expected;
    }
    @Test void allBlockGradientsAndMatchedSelfAblation() {
        var board=BrnEnergyTrainerTest.examples()[0];
        for(boolean context:new boolean[]{false,true}) {
            var m=new BrnContextTrainer(context,97,.003);
            for(int i=DENSE;i<m.modelParameters;i++)m.weights[i]=.13*Math.sin(i-DENSE+.7);
            m.predictPawns(board);m.resetGradient();m.backward(1);double[] g=m.gradient.clone();
            int[] bounds=context?new int[]{0,NODES,DENSE,m.output,m.self,m.other,m.bias,m.modelParameters,g.length}
                    :new int[]{0,NODES,DENSE,m.output,m.self,m.bias,m.modelParameters,g.length};
            for(int block=0;block<bounds.length-1;block++) {
                int chosen=0;for(int i=bounds[block];i<bounds[block+1]&&chosen<12;i++)if(Math.abs(g[i])>1e-10) {
                    double prior=m.weights[i];m.weights[i]=prior+1e-6;double plus=m.predictPawns(board);
                    m.weights[i]=prior-1e-6;double minus=m.predictPawns(board);m.weights[i]=prior;
                    assertEquals(g[i],(plus-minus)/2e-6,2e-7,"context="+context+" parameter="+i);chosen++;
                }
                assertTrue(chosen>0,"Unobserved block "+block);
            }
        }
        var self=new BrnContextTrainer(false,71,.003);var context=new BrnContextTrainer(true,71,.003);
        for(int i=DENSE;i<self.bias;i++)self.weights[i]=context.weights[i]=.13*Math.sin(i-DENSE+.7);
        for(int i=0;i<8;i++)self.weights[self.bias+i]=context.weights[context.bias+i]=.1;
        assertEquals(self.predictPawns(board),context.predictPawns(board),0);
        self.resetGradient();context.resetGradient();self.backward(1);context.backward(1);
        for(int i=0;i<self.bias;i++)assertEquals(self.gradient[i],context.gradient[i],0);
        for(int i=0;i<8;i++)assertEquals(self.gradient[self.bias+i],context.gradient[context.bias+i],0);
        for(int i=0;i<RELATIVE_PARAMETERS;i++)assertEquals(self.gradient[self.modelParameters+i],context.gradient[context.modelParameters+i],0);
    }
    @Test void tinyFitInformationAndColorSymmetry() {
        for(boolean context:new boolean[]{false,true}) {
            var boards=BrnEnergyTrainerTest.examples();double[] targets={.2,-.3,-.4};var m=new BrnContextTrainer(context,71,.003);
            for(int i=0;i<1000;i++)m.trainBatch(boards,targets,3);
            for(int i=0;i<3;i++)assertEquals(targets[i],Brn3Objective.smoothOutcome(m.predictPawns(boards[i]),com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets.material(boards[i]))[0],.025);
            var altered=boards[0].clone();altered[4]=(altered[4]&Board.PLAYER_BIT)|(~altered[4]&~Board.PLAYER_BIT);altered[5]^=54321;
            assertEquals(m.predictPawns(boards[0]),m.predictPawns(altered),0);
            assertEquals(m.predictPawns(boards[0]),m.predictPawns(Board.fromFen("rnbqkb1r/pppp1ppp/5n2/4p3/4P3/2N5/PPPP1PPP/R1BQKBNR b KQkq - 2 3")),1e-10);
        }
    }
    @Test void exactContinuationCrcImmutabilityAndIncrementalParity()throws Exception {
        for(boolean context:new boolean[]{false,true}) {
            var m=new BrnContextTrainer(context,71,.003);var boards=BrnEnergyTrainerTest.examples();double[] targets={.2,-.3,-.4};
            for(int i=0;i<6;i++)m.trainBatch(boards,targets,3);
            var bytes=new ByteArrayOutputStream();m.write(bytes);byte[] state=bytes.toByteArray();var restored=BrnContextTrainer.read(new ByteArrayInputStream(state));assertEquals(context,restored.context());
            m.trainBatch(boards,targets,3);restored.trainBatch(boards,targets,3);assertArrayEquals(m.weights,restored.weights);assertArrayEquals(m.first,restored.first);assertArrayEquals(m.second,restored.second);assertEquals(m.step(),restored.step());
            state[state.length/2]^=1;assertThrows(IOException.class,()->BrnContextTrainer.read(new ByteArrayInputStream(state)));
            bytes.reset();m.snapshot().write(bytes);byte[] payload=bytes.toByteArray();var snapshot=BrnContextTrainer.Model.read(new ByteArrayInputStream(payload));var work=snapshot.newWorkspace();assertEquals(context,snapshot.context);
            payload[payload.length/2]^=1;assertThrows(IOException.class,()->BrnContextTrainer.Model.read(new ByteArrayInputStream(payload)));assertThrows(IOException.class,()->BrnContextTrainer.Model.read(new ByteArrayInputStream(Arrays.copyOf(payload,24))));
            long[] moves=new long[512];var random=new Random(12741);var board=Board.startingPosition();
            String[] special={"r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 w - - 0 1"};
            for(String fen:special) {
                var parent=Board.fromFen(fen);int n=Gen.genAll(parent[0],parent[1],parent[2],parent[3],(int)parent[4],parent[5],true,moves,new long[6]);
                for(int i=0;i<n;i++){var child=new long[6];Board.makeMoveInto(parent[0],parent[1],parent[2],parent[3],(int)parent[4],parent[5],moves[i],child);
                    assertEquals(m.predictPawns(parent),work.evaluatePawns(parent),1e-4);assertEquals(m.predictPawns(child),work.evaluatePawns(child),1e-4);assertEquals(m.predictPawns(parent),work.evaluatePawns(parent),1e-4);}
            }
            for(int ply=0;ply<800;ply++) {
                assertEquals(m.predictPawns(board),work.evaluatePawns(board),1e-4);assertEquals(snapshot.newWorkspace().evaluatePawns(board),work.evaluatePawns(board),1e-10);
                int n=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,new long[6]);
                if(n==0||ply%99==98){board=Board.startingPosition();continue;}
                var child=new long[6];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[random.nextInt(n)],child);board=child;
            }
            double frozen=work.evaluatePawns(board);Arrays.fill(m.weights,123);assertEquals(frozen,work.evaluatePawns(board),0);
        }
    }
}
