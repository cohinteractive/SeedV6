package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.io.*;
import java.nio.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TrainableOneTest {
    static byte[] bytes(TrainableOne m)throws IOException {var b=new ByteArrayOutputStream();m.write(b);return b.toByteArray();}
    static TrainableOne restore(TrainableOne m)throws IOException{return TrainableOne.read(new ByteArrayInputStream(bytes(m)));}
    static double oracle(TrainableOne m,TrainableOne.Features f){
        double v=f.material+m.weights[768];for(int i=0;i<f.size;i++){int ni=f.nodes[i];v+=m.weights[ni];
            for(int d=0;d<m.width;d++){double a=0;for(int j=0;j<f.size;j++)if(j!=i){int nj=f.nodes[j];double relation=0;for(int r=0;r<m.rank;r++)relation+=m.weights[m.QUERY+ni*m.rank+r]*m.weights[m.KEY+nj*m.rank+r];a+=relation*m.weights[m.E+nj*m.width+d];}
                a*=f.norm;v+=m.weights[m.Q+ni*m.width+d]*a/(1+Math.abs(a))/m.width;}}
        return v;
    }
    @Test void eightFourMatchesFrozenReferenceForwardGradientAndUpdate() {
        var ref=new LowRankRelationNetwork(false,.01,71);var m=new TrainableOne(8,4,.01,71);var board=Board.startingPosition();
        var rf=new LowRankRelationNetwork.Workspace().encode(board).frozen();var f=m.encode(board,new TrainableOne.Features());
        int[] map=new int[m.parameters];for(int i=0;i<map.length;i++)map[i]=i<m.KEY?i:i+(LowRankRelationNetwork.KEY-m.KEY);
        for(int i=0;i<map.length;i++)assertEquals(ref.weights[map[i]],m.weights[i]);
        for(int pass=0;pass<5;pass++){
            assertEquals(ref.scoreCp(board,new LowRankRelationNetwork.Workspace()),m.scoreCp(board),1e-10);
            assertEquals(ref.gradients(new LowRankRelationNetwork.Features[]{rf},new double[]{.8}),m.gradients(new TrainableOne.Features[]{f},new double[]{.8}),1e-14);
            for(int i=0;i<map.length;i++)assertEquals(ref.gradient[map[i]],m.gradient[i],1e-14);
            ref.train(new LowRankRelationNetwork.Features[]{rf},new double[]{.8});m.train(new TrainableOne.Features[]{f},new double[]{.8});
            for(int i=0;i<map.length;i++){assertEquals(ref.weights[map[i]],m.weights[i],1e-13);assertEquals(ref.first[map[i]],m.first[i],1e-14);assertEquals(ref.second[map[i]],m.second[i],1e-14);}
        }
    }
    @Test void allCapacitiesQuadraticOracleGradientTinyFitAndExactResume()throws Exception {
        for(int[] capacity:new int[][]{{8,4},{16,4},{32,8}}){var m=new TrainableOne(capacity[0],capacity[1],.01,97);var b=Board.fromFen("6k1/8/2p5/3n4/3Q4/8/6P1/6K1 w - - 0 1");var f=m.encode(b,new TrainableOne.Features());
            assertEquals(oracle(m,f),m.logit(f,m.workspace,true),1e-12);var batch=new TrainableOne.Features[]{f};double[] target={.2};m.gradients(batch,target);
            double[] analytic=m.gradient.clone();int[] familyStarts={0,m.E,m.Q,m.KEY,m.QUERY},familyEnds={m.E,m.Q,m.KEY,m.QUERY,m.parameters};
            for(int family=0;family<5;family++){int checked=0;for(int i=familyStarts[family];i<familyEnds[family]&&checked<8;i++){if(Math.abs(analytic[i])<1e-13)continue;double old=m.weights[i],eps=1e-5;m.weights[i]=old+eps;double plus=PairObjectiveDiagnostic.sigmoid(oracle(m,f))-.2;m.weights[i]=old-eps;double minus=PairObjectiveDiagnostic.sigmoid(oracle(m,f))-.2;m.weights[i]=old;assertEquals((plus*plus-minus*minus)/(2*eps),analytic[i],1e-10);checked++;}assertTrue(checked>0);}
            double before=m.gradients(batch,target);for(int i=0;i<20;i++)m.train(batch,target);assertTrue(m.gradients(batch,target)<before);
            var restored=restore(m);for(int i=0;i<3;i++){m.train(batch,target);restored.train(batch,target);}assertArrayEquals(bytes(m),bytes(restored));
            byte[] bad=bytes(m);bad[100]^=1;assertThrows(IOException.class,()->TrainableOne.read(new ByteArrayInputStream(bad)));
            assertThrows(IOException.class,()->TrainableOne.read(new ByteArrayInputStream(Arrays.copyOf(bytes(m),bytes(m).length+1))));
        }
    }
    @Test void packedDecodeColorAndIncrementalAllSpecialChildrenAtEveryCapacity(){
        for(int[] capacity:new int[][]{{8,4},{16,4},{32,8}}){var m=new TrainableOne(capacity[0],capacity[1],.01,71);var optimized=new IncrementalOne(m);
            for(String fen:IncrementalOneTest.SPECIAL){var board=Board.fromFen(fen);var f=m.encode(board,new TrainableOne.Features());
                byte[] record=OneCorpusSupplement.pack(com.ohinteractive.seedv6.corpus.CorpusPosition.fromFen(fen),123,0);var decoded=new TrainableOne.Features();m.decode(ByteBuffer.wrap(record).order(ByteOrder.LITTLE_ENDIAN),0,decoded);
                assertEquals(oracle(m,f),oracle(m,decoded),1e-12);assertEquals(m.logit(f,m.workspace,true),oracle(m,f),1e-12);
                var parent=optimized.newAccumulator();parent.rebuild(board);assertEquals(m.scoreCp(board),parent.scoreCp(board),1e-8);
                long[] moves=new long[256],scratch=new long[6];int n=com.ohinteractive.seedv6.core.Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,scratch);
                for(int i=0;i<n;i++){long[] child=new long[6];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[i],child);var a=optimized.newAccumulator();a.update(board,child,parent);assertEquals(m.scoreCp(child),a.scoreCp(child),1e-8);assertEquals(m.scoreCp(board),parent.scoreCp(board),1e-8);}
            }
        }
    }
}
