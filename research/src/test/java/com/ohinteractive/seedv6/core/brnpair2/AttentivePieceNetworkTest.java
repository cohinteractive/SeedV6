package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnpair2.AttentivePieceNetwork.*;

class AttentivePieceNetworkTest {
    static int node(long[] b,int square){int p=Board.getSquare(b[0],b[1],b[2],b[3],square),stm=Board.player((int)b[4]);return ((p&7)-1+6*((p>>>3)^stm))*64+(square^(stm*56));}
    static double oracle(AttentivePieceNetwork m,long[] b) {
        var squares=new ArrayList<Integer>();for(int s=0;s<64;s++)if(Board.getSquare(b[0],b[1],b[2],b[3],s)!=0)squares.add(s);
        double[][] h=new double[64][8];int stm=Board.player((int)b[4]);double value=BrnPair2Features.material(b)/4+m.weights[768];
        for(int i:squares) {
            value+=m.weights[node(b,i)];
            for(int j:squares)if(j!=i) {
                int from=j^(stm*56),to=i^(stm*56),rel=node(b,i)/64*225+(to/8-from/8+7)*15+to%8-from%8+7;
                for(int d=0;d<8;d++)h[i][d]+=m.weights[E+node(b,j)*8+d]*m.weights[R+rel*8+d]/Math.sqrt(squares.size()-1);
            }
            for(int d=0;d<8;d++){h[i][d]/=1+Math.abs(h[i][d]);value+=m.weights[Q+node(b,i)*8+d]*h[i][d]/8;}
        }
        for(int i:squares) {
            double[] logits=new double[64],alpha=new double[64];double maximum=Double.NEGATIVE_INFINITY,total=0;
            for(int j:squares)if(j!=i) {
                int from=j^(stm*56),to=i^(stm*56),rel=node(b,i)/64*225+(to/8-from/8+7)*15+to%8-from%8+7;logits[j]=m.weights[B+rel];
                if(m.attention==2)for(int d=0;d<8;d++)logits[j]+=m.weights[K+d]*(m.weights[E+node(b,i)*8+d]+h[i][d])*(m.weights[E+node(b,j)*8+d]+h[j][d])/Math.sqrt(8);
                maximum=Math.max(maximum,logits[j]);
            }
            for(int j:squares)if(j!=i){alpha[j]=Math.exp(logits[j]-maximum);total+=alpha[j];}
            for(int d=0;d<8;d++) {
                double k=0;
                for(int j:squares)if(j!=i){int from=j^(stm*56),to=i^(stm*56),rel=node(b,i)/64*225+(to/8-from/8+7)*15+to%8-from%8+7;double scale=m.attention==0?1/Math.sqrt(squares.size()-1):Math.sqrt(squares.size()-1)*alpha[j]/total;k+=scale*h[j][d]*m.weights[S+rel*8+d];}
                value+=m.weights[V+node(b,i)*8+d]*k/(1+Math.abs(k))/8;
            }
        }
        return value;
    }
    @Test void independentEquationsPerspectiveSpecialMovesAndCallerIsolation() {
        var boards=new ArrayList<long[]>();long[] b=Board.startingPosition(),moves=new long[256],scratch=new long[6];var rng=new Random(71);
        for(int n=0;n<80;n++){boards.add(b);int count=Gen.genAll(b[0],b[1],b[2],b[3],(int)b[4],b[5],true,moves,scratch);if(count==0)break;var next=new long[6];Board.makeMoveInto(b[0],b[1],b[2],b[3],(int)b[4],b[5],moves[rng.nextInt(count)],next);b=next;}
        for(String fen:List.of("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 b - - 0 1")) {
            b=Board.fromFen(fen);boards.add(b);int count=Gen.genAll(b[0],b[1],b[2],b[3],(int)b[4],b[5],true,moves,scratch);
            for(int n=0;n<count;n++){var next=new long[6];Board.makeMoveInto(b[0],b[1],b[2],b[3],(int)b[4],b[5],moves[n],next);boards.add(next);}
        }
        for(boolean compose:new boolean[]{false,true}) {
            var m=new AttentivePieceNetwork(compose?2:1,.01,71);for(int i=V;i<PARAMETERS;i++)m.weights[i]=.3*rng.nextGaussian();var w=new Workspace();
            for(var board:boards){var saved=board.clone();assertEquals(400*oracle(m,board),m.scoreCp(board,w),1e-9);assertArrayEquals(saved,board);var mirror=board.clone();long occ=board[0]|board[1]|board[2];for(int i=0;i<3;i++)mirror[i]=Long.reverseBytes(board[i]);mirror[3]=Long.reverseBytes(occ^board[3]);mirror[4]^=1;assertEquals(m.scoreCp(board,w),m.scoreCp(mirror,w),1e-9);}
            m.weights[768]=Double.NaN;assertThrows(ArithmeticException.class,()->m.scoreCp(Board.startingPosition(),w));
        }
    }
    @Test void gradientFamiliesTinyFitBoundsAndExactResume()throws Exception {
        for(boolean compose:new boolean[]{false,true}) {
            var m=new AttentivePieceNetwork(compose?2:1,.01,71);var rng=new Random(97);for(int i=V;i<PARAMETERS;i++)m.weights[i]=.2*rng.nextGaussian();var w=new Workspace();var f=w.encode(Board.startingPosition()).frozen();var batch=new Features[]{f};double[] target={.8};m.gradients(batch,target);
            int[] ends={0,E,R,Q,S,V,B,K,PARAMETERS};double[] errors=new double[8];
            for(int family=0;family<8;family++) {
                if(!compose&&family==7)continue;int checked=0;
                for(int n=0;n<m.count&&checked<9;n++) {
                    int i=m.touched[n];if(i<ends[family]||i>=ends[family+1]||Math.abs(m.gradient[i])<1e-12)continue;checked++;
                    double old=m.weights[i],eps=1e-5;m.weights[i]=old+eps;double p=PairObjectiveDiagnostic.sigmoid(m.logit(f,w));m.weights[i]=old-eps;double q=PairObjectiveDiagnostic.sigmoid(m.logit(f,w));m.weights[i]=old;
                    double numerical=((p-.8)*(p-.8)-(q-.8)*(q-.8))/(2*eps);errors[family]=Math.max(errors[family],Math.abs(numerical-m.gradient[i]));assertEquals(numerical,m.gradient[i],1e-10,"family"+family);
                }
                assertTrue(checked>0);
            }
            System.out.println("compose="+compose+" gradient errors="+Arrays.toString(errors));double before=m.gradients(batch,target);for(int n=0;n<20;n++)m.train(batch,target);assertTrue(m.gradients(batch,target)<before);for(double x:m.weights)assertTrue(Math.abs(x)<=.99);
            var bytes=new ByteArrayOutputStream();m.write(bytes);var restored=read(bytes.toByteArray());m.train(batch,target);restored.train(batch,target);assertArrayEquals(bytes(m),bytes(restored));byte[] bad=bytes(m);bad[100]^=1;assertThrows(IOException.class,()->read(bad));assertThrows(IOException.class,()->read(Arrays.copyOf(bytes(m),bytes(m).length+1)));
        }
    }
    static byte[] bytes(AttentivePieceNetwork m)throws IOException{var out=new ByteArrayOutputStream();m.write(out);return out.toByteArray();}
    static AttentivePieceNetwork read(byte[] bytes)throws IOException{return AttentivePieceNetwork.read(new ByteArrayInputStream(bytes));}
    @Test void uniformUpdaterMatchesLegacyAndSegmentedTailContinuationIsExact()throws Exception {
        var data=new PairCoverageDiagnostic.Data(257);var w=new Workspace();var all=new Features[257];
        var old=new ContextualPieceNetwork(true,.01,71);var uniform=new AttentivePieceNetwork(0,.01,71);
        assertArrayEquals(old.weights,Arrays.copyOf(uniform.weights,B));
        for(int i=0;i<257;i++){long[] b=i%2==0?Board.startingPosition():Board.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1");System.arraycopy(b,0,data.boards,i*6,6);data.targets[i]=(i%5)/4.0;all[i]=w.encode(b).frozen();assertEquals(old.scoreCp(b,new ContextualPieceNetwork.Workspace()),uniform.scoreCp(b,w));}
        for(int epoch=1;epoch<=2;epoch++){ContextualPieceDiagnostic.segment(old,data,71,epoch,0,257);AttentivePieceDiagnostic.segment(uniform,data,71,epoch,0,257);assertArrayEquals(old.weights,Arrays.copyOf(uniform.weights,B));assertArrayEquals(old.first,Arrays.copyOf(uniform.first,B));assertArrayEquals(old.second,Arrays.copyOf(uniform.second,B));}
        var oldBytes=new ByteArrayOutputStream();old.write(oldBytes);assertThrows(IOException.class,()->read(oldBytes.toByteArray()));
        for(int mode:new int[]{1,2}) {
            var full=new AttentivePieceNetwork(mode,.01,71);var divided=new AttentivePieceNetwork(mode,.01,71);
            for(int i=0;i<257;i++)assertEquals(new AttentivePieceNetwork(0,.01,71).logit(all[i],w),full.logit(all[i],w),1e-12);
            AttentivePieceDiagnostic.segment(full,data,71,1,0,257);AttentivePieceDiagnostic.segment(divided,data,71,1,0,128);divided=read(bytes(divided));AttentivePieceDiagnostic.segment(divided,data,71,1,128,257);assertArrayEquals(bytes(full),bytes(divided));
        }
    }
    @Test void validChecksumCannotConcealInvalidModeOrGeometry()throws Exception {
        var model=new AttentivePieceNetwork(2,.01,71);
        for(int offset:new int[]{8,12}) {
            byte[] bad=bytes(model);java.nio.ByteBuffer.wrap(bad).putInt(offset,offset==8?3:PARAMETERS+1);
            var crc=new java.util.zip.CRC32();crc.update(bad,0,bad.length-8);java.nio.ByteBuffer.wrap(bad).putLong(bad.length-8,crc.getValue());
            assertThrows(IOException.class,()->read(bad));
        }
        assertThrows(IllegalArgumentException.class,()->new AttentivePieceNetwork(-1,.01,71));
        assertThrows(IllegalArgumentException.class,()->new AttentivePieceNetwork(3,.01,71));
    }
    @Test void stableSoftmaxNormalizesAndDynamicRankingDependsOnReceiver() {
        var b=Board.fromFen("6k1/8/8/8/8/8/8/1N2N1K1 w - - 0 1");var m=new AttentivePieceNetwork(2,.01,71);Arrays.fill(m.weights,0);var w=new Workspace();var f=w.encode(b);assertEquals(4,f.size);
        double[] signs={1,-1,1,-1};for(int i=0;i<4;i++)m.weights[E+f.nodes[i]*8]=signs[i];m.weights[K]=1;m.logit(f,w);
        assertTrue(w.alpha[2*4]>w.alpha[3*4]);assertTrue(w.alpha[2*4+1]<w.alpha[3*4+1]);double[] before=w.alpha.clone();
        for(int i=B;i<K;i++)m.weights[i]=1000;m.logit(f,w);
        for(int i=0;i<4;i++){double sum=0;for(int j=0;j<4;j++)if(j!=i){assertEquals(before[j*4+i],w.alpha[j*4+i],5e-14);sum+=w.alpha[j*4+i];}assertEquals(1,sum,1e-15);}
        for(int i=B;i<K;i++)m.weights[i]=(i%2==0?10000:-10000);assertTrue(Double.isFinite(m.logit(f,w)));
        m.logit(f,w,1);for(int i=0;i<4;i++){double sum=0;for(int j=0;j<4;j++)if(j!=i)sum+=w.alpha[j*4+i];assertEquals(1,sum,1e-15);}
    }
    @Test void thirdPieceChangesRoutingBetweenFixedEndpoints() {
        var m=new AttentivePieceNetwork(2,.01,71);var rng=new Random(97);for(int d=0;d<8;d++)m.weights[K+d]=.5*rng.nextGaussian();
        String[] fens={"6k1/5ppp/8/8/8/8/5PPP/1N2N1K1 w - - 0 1","6k1/5ppp/8/8/8/2N5/5PPP/4N1K1 w - - 0 1"};double[] odds=new double[2];
        for(int k=0;k<2;k++) {
            var b=Board.fromFen(fens[k]);var w=new Workspace();var f=w.encode(b);m.logit(f,w);int target=-1,a=-1,c=-1;
            for(int i=0;i<f.size;i++){int sq=f.nodes[i]%64;if(sq==6)target=i;if(sq==4)a=i;if(sq==62)c=i;}
            assertTrue(target>=0&&a>=0&&c>=0);odds[k]=Math.log(w.alpha[a*f.size+target]/w.alpha[c*f.size+target]);
        }
        assertTrue(Math.abs(odds[0]-odds[1])>1e-8);System.out.println("Third-piece routing log-odds change="+(odds[1]-odds[0]));
    }
}
