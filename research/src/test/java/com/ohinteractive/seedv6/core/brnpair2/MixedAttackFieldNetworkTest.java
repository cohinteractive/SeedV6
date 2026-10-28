package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnpair2.MixedAttackFieldNetwork.*;

class MixedAttackFieldNetworkTest {
    static int node(long[] b,int square) {int code=Board.getSquare(b[0],b[1],b[2],b[3],square),stm=Board.player((int)b[4]);return (code==0?12:(code&7)-1+6*((code>>>3)^stm))*64+(square^(stm*56));}
    static double oracle(MixedAttackFieldNetwork m,long[] b) {
        var squares=new ArrayList<Integer>();int[] kings={-1,-1};int stm=Board.player((int)b[4]);
        for(int s=0;s<64;s++){int code=Board.getSquare(b[0],b[1],b[2],b[3],s);if(code!=0)squares.add(s);if((code&7)==1)kings[(code>>>3)^stm]=s;}
        double[][] h=new double[2][8];
        for(int k=0;k<2;k++)for(int s:squares)if(s!=kings[k]) {
            int a=s^(stm*56),c=kings[k]^(stm*56),rel=k*225+(c/8-a/8+7)*15+c%8-a%8+7;
            for(int d=0;d<8;d++)h[k][d]+=m.weights[E+8*node(b,s)+d]*m.weights[R+8*rel+d]/Math.sqrt(squares.size()-1);
        }
        for(var row:h)for(int d=0;d<8;d++)row[d]/=1+Math.abs(row[d]);double sum=BrnPair2Features.material(b)/4+m.weights[768];
        for(int target=0;target<64;target++) {
            
            int id=node(b,target),degree=0;double[] a=new double[8];if(squares.contains(target))sum+=m.weights[id];
            for(int source:squares)if(RelationalGraphEnergyTest.edge(b,source,target)) {
                degree++;int sq=source^(stm*56),tq=target^(stm*56),rel=id/64*225+(tq/8-sq/8+7)*15+tq%8-sq%8+7;
                for(int d=0;d<8;d++)a[d]+=m.weights[E+8*node(b,source)+d]*m.weights[L+8*rel+d];
            }
            for(int d=0;d<8;d++) {
                double gain=1;for(int k=0;k<2;k++)gain+=m.weights[G+(id/64*2+k)*8+d]*h[k][d];
                double transformed=a[d];if(m.mix)for(int e=0;e<8;e++)transformed+=m.weights[M+(id/64*8+d)*8+e]*a[e];
                double z=degree==0?0:transformed*gain/Math.sqrt(degree);sum+=m.weights[O+id*8+d]*(z/(1+Math.abs(z)))/8;
            }
        }
        return sum;
    }
    @Test void independentEquationsGraphGeometryColorAndSpecialBoards() {
        var boards=new ArrayList<long[]>();long[] b=Board.startingPosition(),moves=new long[256],scratch=new long[6];var random=new Random(71);
        for(int i=0;i<80;i++){boards.add(b);int n=Gen.genAll(b[0],b[1],b[2],b[3],(int)b[4],b[5],true,moves,scratch);if(n==0)break;var next=new long[6];Board.makeMoveInto(b[0],b[1],b[2],b[3],(int)b[4],b[5],moves[random.nextInt(n)],next);b=next;}
        for(String fen:List.of("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 b - - 0 1")) {
            b=Board.fromFen(fen);boards.add(b);int n=Gen.genAll(b[0],b[1],b[2],b[3],(int)b[4],b[5],true,moves,scratch);
            for(int i=0;i<n;i++){var next=new long[6];Board.makeMoveInto(b[0],b[1],b[2],b[3],(int)b[4],b[5],moves[i],next);boards.add(next);}
        }
        for(boolean mix:new boolean[]{false,true}) {
            var m=new MixedAttackFieldNetwork(mix,.01,71);for(int i=G;i<PARAMETERS;i++)m.weights[i]=.3*random.nextGaussian();var w=new Workspace();
            for(var board:boards) {
                assertEquals(400*oracle(m,board),m.scoreCp(board,w),1e-9);
                var mirror=board.clone();long occ=board[0]|board[1]|board[2];for(int i=0;i<3;i++)mirror[i]=Long.reverseBytes(board[i]);mirror[3]=Long.reverseBytes(occ^board[3]);mirror[4]^=1;
                assertEquals(m.scoreCp(board,w),m.scoreCp(mirror,w),1e-9);
            }
            m.weights[768]=Double.NaN;assertThrows(ArithmeticException.class,()->m.scoreCp(Board.startingPosition(),w));
        }
    }
    @Test void allGradientFamiliesTinyFitBoundsAndExactContinuation()throws Exception {
        for(boolean mix:new boolean[]{false,true}) {
            var m=new MixedAttackFieldNetwork(mix,.01,71);var random=new Random(97);for(int i=G;i<PARAMETERS;i++)m.weights[i]=.2*random.nextGaussian();
            for(int i=O+768*8;i<G;i++)m.weights[i]=.2*random.nextGaussian();
            var w=new Workspace();var f=w.encode(Board.startingPosition(),true).frozen();var batch=new Features[]{f};double[] target={.8};m.gradients(batch,target);
            int[] ends={0,E,L,L+2700*8,R,O,O+768*8,G,G+24*8,M,M+12*64,PARAMETERS};double[] errors=new double[11];
            for(int family=0;family<11;family++) {
                if(!mix&&(family==9||family==10))continue;int checked=0;
                for(int n=0;n<m.count&&checked<9;n++) {
                    int i=m.touched[n];if(i<ends[family]||i>=ends[family+1]||Math.abs(m.gradient[i])<1e-12)continue;checked++;
                    double old=m.weights[i],eps=1e-5;m.weights[i]=old+eps;double p=PairObjectiveDiagnostic.sigmoid(m.logit(f,w));m.weights[i]=old-eps;double q=PairObjectiveDiagnostic.sigmoid(m.logit(f,w));m.weights[i]=old;
                    double numerical=((p-.8)*(p-.8)-(q-.8)*(q-.8))/(2*eps);errors[family]=Math.max(errors[family],Math.abs(numerical-m.gradient[i]));assertEquals(numerical,m.gradient[i],1e-10,"family"+family);
                }
                assertTrue(checked>0);
            }
            System.out.println("mix="+mix+" maximum gradient error="+Arrays.toString(errors));double before=m.gradients(batch,target);for(int i=0;i<20;i++)m.train(batch,target);assertTrue(m.gradients(batch,target)<before);
            for(double v:m.weights)assertTrue(Math.abs(v)<=.99);
            var bytes=new ByteArrayOutputStream();m.write(bytes);var restored=MixedAttackFieldNetwork.read(new ByteArrayInputStream(bytes.toByteArray()));m.train(batch,target);restored.train(batch,target);
            var a=new ByteArrayOutputStream();var c=new ByteArrayOutputStream();m.write(a);restored.write(c);assertArrayEquals(a.toByteArray(),c.toByteArray());byte[] bad=a.toByteArray();bad[100]^=1;assertThrows(IOException.class,()->MixedAttackFieldNetwork.read(new ByteArrayInputStream(bad)));
        }
    }
    @Test void exactLegacyAllSquareUpdaterAndInitialPredictionParity() {
        var legacy=new AttackFieldNetwork(true,.01,71);var fixed=new MixedAttackFieldNetwork(false,.01,71);var learned=new MixedAttackFieldNetwork(true,.01,71);
        assertArrayEquals(legacy.weights,Arrays.copyOf(fixed.weights,AttackFieldNetwork.PARAMETERS));assertArrayEquals(fixed.weights,learned.weights);
        var data=new PairCoverageDiagnostic.Data(257);
        for(int i=0;i<257;i++) {
            long[] board=i%2==0?Board.startingPosition():Board.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");System.arraycopy(board,0,data.boards,6*i,6);data.targets[i]=(i%5)/4.0;
            assertEquals(legacy.scoreCp(board,new AttackFieldNetwork.Workspace()),fixed.scoreCp(board,new Workspace()));assertEquals(fixed.scoreCp(board,new Workspace()),learned.scoreCp(board,new Workspace()));
        }
        for(int epoch=1;epoch<=3;epoch++) {
            AttackFieldDiagnostic.epoch(legacy,data,71,epoch);MixedAttackFieldDiagnostic.epoch(fixed,data,71,epoch);
            assertArrayEquals(legacy.weights,Arrays.copyOf(fixed.weights,AttackFieldNetwork.PARAMETERS));assertArrayEquals(legacy.first,Arrays.copyOf(fixed.first,AttackFieldNetwork.PARAMETERS));assertArrayEquals(legacy.second,Arrays.copyOf(fixed.second,AttackFieldNetwork.PARAMETERS));assertEquals(legacy.step,fixed.step);
        }
    }
    @Test void batchLocalEncodingMatchesPreencodedUpdatesIncludingTail()throws Exception {
        var data=new PairCoverageDiagnostic.Data(257);var w=new Workspace();var all=new Features[257];double[] targets=new double[257];
        for(int i=0;i<257;i++){long[] b=i%2==0?Board.startingPosition():Board.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1");System.arraycopy(b,0,data.boards,i*6,6);data.targets[i]=targets[i]=(i%5)/4.0;all[i]=w.encode(b,true).frozen();}
        var a=new MixedAttackFieldNetwork(true,.01,71);var b=new MixedAttackFieldNetwork(true,.01,71);int[] order=new int[257];for(int i=0;i<257;i++)order[i]=i;var random=new Random(72);
        for(int i=256;i>0;i--){int j=random.nextInt(i+1),v=order[i];order[i]=order[j];order[j]=v;}
        for(int offset=0;offset<257;offset+=128){int n=Math.min(128,257-offset);var batch=new Features[n];var target=new double[n];for(int j=0;j<n;j++){batch[j]=all[order[offset+j]];target[j]=targets[order[offset+j]];}a.train(batch,target);}
        MixedAttackFieldDiagnostic.epoch(b,data,71,1);var aa=new ByteArrayOutputStream();var bb=new ByteArrayOutputStream();a.write(aa);b.write(bb);assertArrayEquals(aa.toByteArray(),bb.toByteArray());
    }
}
