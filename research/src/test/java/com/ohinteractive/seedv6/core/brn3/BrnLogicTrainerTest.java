package com.ohinteractive.seedv6.core.brn3;

import com.ohinteractive.seedv6.core.*;
import static com.ohinteractive.seedv6.core.brn3.BrnLogicTrainer.*;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrnLogicTrainerTest {
    private static int truth(int op,int a,int b){return (op>>>(3-2*a-b))&1;}
    private static long slowShift(long bits,int dx,int dy) {
        long result=0;for(int s=0;s<64;s++)if((bits&(1L<<s))!=0){int x=(s&7)+dx,y=(s>>>3)+dy;if(x>=0&&x<8&&y>=0&&y<8)result|=1L<<(8*y+x);}return result;
    }
    @Test void everyTruthFunctionShiftAndCanonicalRawMap() {
        var random=new Random(917);
        for(int op=0;op<16;op++)for(int a=0;a<2;a++)for(int b=0;b<2;b++) {
            double[] c=TRUTH[op];assertEquals(truth(op,a,b),c[0]+c[1]*a+c[2]*b+c[3]*a*b,0);
        }
        for(int n=0;n<80;n++) {
            long a=random.nextLong(),b=random.nextLong();
            for(int op=0;op<16;op++){long expected=0;for(int s=0;s<64;s++)if(truth(op,(int)(a>>>s)&1,(int)(b>>>s)&1)!=0)expected|=1L<<s;assertEquals(expected,gate(op,a,b));}
            for(int dx=-2;dx<=2;dx++)for(int dy=-2;dy<=2;dy++)assertEquals(slowShift(a,dx,dy),shift(a,dx,dy));
        }
        long[][] maps=new long[2][13];
        for(var board:BrnEnergyTrainerTest.examples()) {
            rawMaps(board,maps);
            for(int p=0;p<2;p++)for(int sq=0;sq<64;sq++) {
                int code=Board.getSquare(board[0],board[1],board[2],board[3],sq),category=code==0?0:1+((code&7)-1)+((((code>>>3)&1)^p)*6);
                for(int c=0;c<13;c++)assertEquals(c==category?1:0,(maps[p][c]>>>(sq^(p*56)))&1);
            }
        }
    }
    // Independent truth-table mixture and direct rank/file readout, no coefficient or bitboard gate helpers.
    private static double independent(BrnLogicTrainer m,long[] board,boolean hard) {
        double result=Brn3Features.material(board)+m.weights[m.bias];int stm=Board.player((int)board[4]);
        for(int role=0;role<2;role++) {
            double[][] raw=new double[13][64],states=new double[m.gates][64];int p=stm^role;
            for(int sq=0;sq<64;sq++){int code=Board.getSquare(board[0],board[1],board[2],board[3],sq),c=code==0?0:1+(code&7)-1+((((code>>>3)&1)^p)*6);raw[c][sq^(p*56)]=1;}
            for(int g=0;g<m.gates;g++) {
                int[] t=m.topology[g];double[] prob=new double[16];double sum=0;int modal=0;
                for(int j=0;j<16;j++){prob[j]=Math.exp(m.weights[g*16+j]);sum+=prob[j];if(m.weights[g*16+j]>m.weights[g*16+modal])modal=j;}
                for(int sq=0;sq<64;sq++) {
                    double[] inputs=new double[2];
                    for(int side=0;side<2;side++){int k=side*3,x=(sq&7)-t[k+1],y=(sq>>>3)-t[k+2];if(x>=0&&x<8&&y>=0&&y<8)inputs[side]=g<32?raw[t[k]][y*8+x]:states[(g/32-1)*32+t[k]][y*8+x];}
                    double a=inputs[0],b=inputs[1],value=0;
                    if(hard)value=truth(modal,(int)a,(int)b);
                    else for(int j=0;j<16;j++)value+=prob[j]/sum*((1-a)*(1-b)*truth(j,0,0)+(1-a)*b*truth(j,0,1)+a*(1-b)*truth(j,1,0)+a*b*truth(j,1,1));
                    states[g][sq]=value;int base=m.head+(role*m.gates+g)*16;
                    result+=value/Math.sqrt(512)*(m.weights[base+(sq>>>3)]+m.weights[base+8+(sq&7)]);
                }
            }
        }
        return result;
    }
    @Test void independentSoftAndHardForwardAndAllLayerGradients() {
        var board=BrnEnergyTrainerTest.examples()[0];
        for(int layers=1;layers<=2;layers++) {
            var m=new BrnLogicTrainer(layers,71,.003);
            for(int i=m.head;i<m.weights.length;i++)m.weights[i]=.17*Math.sin(i-m.head+.2);
            assertEquals(independent(m,board,false),m.predictSoftPawns(board),1e-11);
            assertEquals(independent(m,board,true),m.predictHardPawns(board),1e-11);
            if(layers==2)for(int role=0;role<2;role++)Arrays.fill(m.weights,m.head+role*m.gates*16,m.head+role*m.gates*16+32*16,0);
            // In layer2, first-layer gate gradients must now come exclusively through downstream gates.
            m.predictSoftPawns(board);m.resetGradient();m.backwardSoft(1);m.finishGateGradient();double[] grad=m.gradient.clone();
            int[] bounds=layers==1?new int[]{0,m.head,m.bias,m.bias+1}:new int[]{0,512,m.head,m.bias,m.bias+1};
            for(int block=0;block<bounds.length-1;block++) {
                int n=0;for(int i=bounds[block];i<bounds[block+1]&&n<16;i++)if(Math.abs(grad[i])>1e-9) {
                    double old=m.weights[i];m.weights[i]=old+1e-5;double hi=m.predictSoftPawns(board);m.weights[i]=old-1e-5;double lo=m.predictSoftPawns(board);m.weights[i]=old;
                    assertEquals(grad[i],(hi-lo)/2e-5,1e-7,"layers="+layers+" index="+i);n++;
                }
                assertTrue(n>0,"Unobserved block "+block);
            }
        }
    }
    @Test void prefixSubmodelAndInputColorBoundary() {
        var one=new BrnLogicTrainer(1,97,.003);var two=new BrnLogicTrainer(2,97,.003);
        for(int g=0;g<32;g++){assertArrayEquals(one.topology[g],two.topology[g]);for(int j=0;j<16;j++)assertEquals(one.weights[g*16+j],two.weights[g*16+j],0);}
        for(int role=0;role<2;role++)for(int i=0;i<512;i++)one.weights[one.head+role*512+i]=two.weights[two.head+role*1024+i]=.1*Math.sin(i+role);
        for(var board:BrnEnergyTrainerTest.examples()) {
            assertEquals(one.predictSoftPawns(board),two.predictSoftPawns(board),1e-12);assertEquals(one.predictHardPawns(board),two.predictHardPawns(board),1e-12);
        }
        var board=BrnEnergyTrainerTest.examples()[0];var altered=board.clone();altered[4]=(altered[4]&Board.PLAYER_BIT)|(~altered[4]&~Board.PLAYER_BIT);altered[5]^=7654321;
        var reversed=Board.fromFen("rnbqkb1r/pppp1ppp/5n2/4p3/4P3/2N5/PPPP1PPP/R1BQKBNR b KQkq - 2 3");
        assertEquals(two.predictSoftPawns(board),two.predictSoftPawns(altered),0);assertEquals(two.predictHardPawns(board),two.predictHardPawns(altered),0);
        assertEquals(two.predictSoftPawns(board),two.predictSoftPawns(reversed),1e-11);assertEquals(two.predictHardPawns(board),two.predictHardPawns(reversed),1e-11);
        assertThrows(IllegalArgumentException.class,()->new BrnLogicTrainer(3,0,.003));
        two.topology[0][1]=Integer.MIN_VALUE;assertThrows(IllegalArgumentException.class,two::snapshot);
    }
    @Test void softTinyFitThenHardHeadRefitFreezesGateOptimizer() {
        for(int layers=1;layers<=2;layers++) {
            var m=new BrnLogicTrainer(layers,71,.003);var boards=BrnEnergyTrainerTest.examples();double[] targets={.2,-.3,-.4};
            assertEquals(layers==1?1537:3073,m.parameters());for(var b:boards){assertEquals(Brn3Features.material(b),m.predictSoftPawns(b),0);assertEquals(Brn3Features.material(b),m.predictHardPawns(b),0);}
            double[] initial=m.weights.clone();for(int i=1;i<=4000;i++){m.trainBatch(boards,targets,3);if(i==1000||i==2000||i==4000)System.out.println("layers="+layers+" softStep="+i+" outcomes="+Arrays.toString(outcomes(m,boards,false)));}
            checkFit(m,boards,targets,false);double change=0;for(int i=0;i<m.head;i++)change+=Math.abs(initial[i]-m.weights[i]);assertTrue(change>.01);
            double[] w=Arrays.copyOf(m.weights,m.head),a=Arrays.copyOf(m.first,m.head),b=Arrays.copyOf(m.second,m.head);m.harden(.0003);int[] ops=m.operators().clone();
            System.out.println("layers="+layers+" hardeningOutcomes="+Arrays.toString(outcomes(m,boards,true)));
            for(int i=1;i<=4000;i++){m.trainBatch(boards,targets,3);if(i==1000||i==2000||i==4000)System.out.println("layers="+layers+" hardStep="+i+" outcomes="+Arrays.toString(outcomes(m,boards,true)));}checkFit(m,boards,targets,true);
            assertArrayEquals(w,Arrays.copyOf(m.weights,m.head));assertArrayEquals(a,Arrays.copyOf(m.first,m.head));assertArrayEquals(b,Arrays.copyOf(m.second,m.head));assertArrayEquals(ops,m.operators());assertEquals(8000,m.step());
        }
    }
    private static double[] outcomes(BrnLogicTrainer m,long[][] boards,boolean hard) {
        double[] actual=new double[boards.length];for(int i=0;i<actual.length;i++)actual[i]=Brn3Objective.smoothOutcome(hard?m.predictHardPawns(boards[i]):m.predictSoftPawns(boards[i]),com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets.material(boards[i]))[0];return actual;
    }
    private static void checkFit(BrnLogicTrainer m,long[][] boards,double[] targets,boolean hard) {
        double[] actual=outcomes(m,boards,hard);System.out.println("layers="+m.layers+" hard="+hard+" outcomes="+Arrays.toString(actual));
        for(int i=0;i<targets.length;i++)assertEquals(targets[i],actual[i],.025);
    }
    private static BrnLogicTrainer roundtrip(BrnLogicTrainer m)throws IOException {
        var bytes=new ByteArrayOutputStream();m.write(bytes);return BrnLogicTrainer.read(new ByteArrayInputStream(bytes.toByteArray()));
    }
    private static void same(BrnLogicTrainer a,BrnLogicTrainer b) {
        assertArrayEquals(a.weights,b.weights);assertArrayEquals(a.first,b.first);assertArrayEquals(a.second,b.second);assertEquals(a.step(),b.step());assertEquals(a.hardened(),b.hardened());assertEquals(a.headRate(),b.headRate(),0);assertEquals(a.hardRate(),b.hardRate(),0);assertArrayEquals(a.operators(),b.operators());
    }
    @Test void independentOptimizerRatesAndV1Reading()throws Exception {
        var a=new BrnLogicTrainer(2,71,.01);var b=new BrnLogicTrainer(2,71,.01,.0003);var boards=BrnEnergyTrainerTest.examples();double[] targets={.2,-.3,-.4};
        a.trainBatch(boards,targets,3);b.trainBatch(boards,targets,3);
        for(int i=0;i<a.head;i++)assertEquals(a.weights[i],b.weights[i],0);
        for(int i=a.head;i<a.weights.length;i++)assertEquals(a.weights[i]*.03,b.weights[i],1e-16);
        b.trainBatch(boards,targets,3);var restored=roundtrip(b);b.trainBatch(boards,targets,3);restored.trainBatch(boards,targets,3);same(b,restored);
        b.harden();restored.harden();assertEquals(.0003,b.hardRate(),0);b.trainBatch(boards,targets,3);restored.trainBatch(boards,targets,3);same(b,restored);
        double[] frozen=Arrays.copyOf(b.weights,b.head),moments=Arrays.copyOf(b.first,b.head);b.refitRate(.0001);restored=roundtrip(b);
        b.trainBatch(boards,targets,3);restored.trainBatch(boards,targets,3);same(b,restored);assertArrayEquals(frozen,Arrays.copyOf(b.weights,b.head));assertArrayEquals(moments,Arrays.copyOf(b.first,b.head));
        assertThrows(IllegalStateException.class,()->a.refitRate(.0001));
        var bytes=new ByteArrayOutputStream();a.write(bytes);byte[] v2=bytes.toByteArray(),v1=new byte[v2.length-8];
        System.arraycopy(v2,0,v1,0,28);v1[7]--;System.arraycopy(v2,36,v1,28,v2.length-40);
        var crc=new java.util.zip.CRC32();crc.update(v1,0,v1.length-4);java.nio.ByteBuffer.wrap(v1,v1.length-4,4).putInt((int)crc.getValue());
        var old=BrnLogicTrainer.read(new ByteArrayInputStream(v1));same(a,old);a.trainBatch(boards,targets,3);old.trainBatch(boards,targets,3);same(a,old);
    }
    @Test void exactContinuationAcrossHardeningCrcAndSnapshotImmutability()throws Exception {
        for(int layers=1;layers<=2;layers++) {
            var m=new BrnLogicTrainer(layers,71,.003);var boards=BrnEnergyTrainerTest.examples();double[] targets={.2,-.3,-.4};
            for(int i=0;i<6;i++)m.trainBatch(boards,targets,3);var restored=roundtrip(m);same(m,restored);
            m.trainBatch(boards,targets,3);restored.trainBatch(boards,targets,3);same(m,restored);
            m.harden(.0003);restored.harden(.0003);m.trainBatch(boards,targets,3);restored.trainBatch(boards,targets,3);same(m,restored);
            restored=roundtrip(m);m.trainBatch(boards,targets,3);restored.trainBatch(boards,targets,3);same(m,restored);
            var bytes=new ByteArrayOutputStream();m.write(bytes);byte[] state=bytes.toByteArray();state[state.length/2]^=1;assertThrows(IOException.class,()->BrnLogicTrainer.read(new ByteArrayInputStream(state)));
            bytes.reset();m.snapshot().write(bytes);byte[] payload=bytes.toByteArray();var model=BrnLogicTrainer.Model.read(new ByteArrayInputStream(payload));var work=model.newWorkspace();
            payload[payload.length/2]^=1;assertThrows(IOException.class,()->BrnLogicTrainer.Model.read(new ByteArrayInputStream(payload)));assertThrows(IOException.class,()->BrnLogicTrainer.Model.read(new ByteArrayInputStream(Arrays.copyOf(payload,24))));
            double value=work.evaluatePawns(boards[0]);assertEquals(m.predictHardPawns(boards[0]),value,1e-5);Arrays.fill(m.weights,123);Arrays.fill(m.topology[0],0);assertEquals(value,work.evaluatePawns(boards[0]),0);
        }
    }
    @Test void hardWorkspaceAcrossLegalSpecialAndStmOnlyTransitions() {
        for(int layers=1;layers<=2;layers++) {
            var m=new BrnLogicTrainer(layers,71,.003);for(int i=m.head;i<m.weights.length;i++)m.weights[i]=.3*Math.sin(i-m.head+.2);
            var model=m.snapshot();var work=model.newWorkspace();long[] moves=new long[512];var random=new Random(1741);var board=Board.startingPosition();
            for(String fen:new String[]{"r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 w - - 0 1"}) {
                var parent=Board.fromFen(fen);assertEquals(independent(m,parent,true),work.evaluatePawns(parent),1e-5);int n=Gen.genAll(parent[0],parent[1],parent[2],parent[3],(int)parent[4],parent[5],true,moves,new long[6]);
                for(int i=0;i<n;i++){var child=new long[6];Board.makeMoveInto(parent[0],parent[1],parent[2],parent[3],(int)parent[4],parent[5],moves[i],child);assertEquals(m.predictHardPawns(child),work.evaluatePawns(child),1e-5);assertEquals(m.predictHardPawns(parent),work.evaluatePawns(parent),1e-5);}
            }
            for(int ply=0;ply<800;ply++) {
                assertEquals(m.predictHardPawns(board),work.evaluatePawns(board),1e-5);assertEquals(model.newWorkspace().evaluatePawns(board),work.evaluatePawns(board),0);
                var swapped=board.clone();swapped[4]^=Board.PLAYER_BIT;assertEquals(m.predictHardPawns(swapped),work.evaluatePawns(swapped),1e-5);
                int n=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,new long[6]);
                if(n==0||ply%99==98){board=Board.startingPosition();continue;}
                var child=new long[6];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[random.nextInt(n)],child);board=child;
            }
        }
    }
}
