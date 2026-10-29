package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnpair2.LowRankRelationNetwork.*;

class LowRankRelationNetworkTest {
    static int node(long[] b,int s){return SharedGeometryPieceNetworkTest.node(b,s);}
    /** Explicit ordered-pair equations, independent of pooled forward/backward code. */
    static double oracle(LowRankRelationNetwork m,long[] board) {
        var nodes=new ArrayList<Integer>();for(int s=0;s<64;s++)if(Board.getSquare(board[0],board[1],board[2],board[3],s)!=0)nodes.add(node(board,s));
        int n=nodes.size();double scale=1/Math.sqrt(n-1),value=BrnPair2Features.material(board)/4+m.weights[768];
        double[][] relation=new double[n][n],h=new double[n][WIDTH],message=new double[n][WIDTH];
        for(int j=0;j<n;j++)for(int i=0;i<n;i++)if(j!=i) {
            for(int r=0;r<RANK;r++)relation[j][i]+=m.weights[KEY+nodes.get(j)*RANK+r]*m.weights[QUERY+nodes.get(i)*RANK+r];
            relation[j][i]/=Math.sqrt(RANK);
        }
        for(int i=0;i<n;i++) {
            value+=m.weights[nodes.get(i)];
            for(int d=0;d<WIDTH;d++) {
                double a=0;for(int j=0;j<n;j++)if(j!=i)a+=scale*relation[j][i]*m.weights[E+nodes.get(j)*WIDTH+d];
                h[i][d]=a/(1+Math.abs(a));value+=m.weights[Q+nodes.get(i)*WIDTH+d]*h[i][d]/WIDTH;
            }
        }
        if(m.compose) {
            for(int j=0;j<n;j++)for(int d=0;d<WIDTH;d++){message[j][d]=h[j][d];for(int e=0;e<WIDTH;e++)message[j][d]+=m.weights[M+d*WIDTH+e]*h[j][e]/Math.sqrt(WIDTH);}
            for(int i=0;i<n;i++)for(int d=0;d<WIDTH;d++){double b=0;for(int j=0;j<n;j++)if(j!=i)b+=scale*relation[j][i]*message[j][d];value+=m.weights[V+nodes.get(i)*WIDTH+d]*(b/(1+Math.abs(b)))/WIDTH;}
        }
        return value;
    }
    @Test void quadraticOracleLegalSpecialColorAndWorkspaceOwnership() {
        var boards=new ArrayList<long[]>();long[] board=Board.startingPosition(),moves=new long[256],scratch=new long[6];var rng=new Random(71);
        for(int i=0;i<40;i++){boards.add(board);int n=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,scratch);if(n==0)break;var next=new long[6];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[rng.nextInt(n)],next);board=next;}
        for(String fen:List.of("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 b - - 0 1")) {
            board=Board.fromFen(fen);boards.add(board);int n=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,scratch);for(int i=0;i<n;i++){var next=new long[6];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[i],next);boards.add(next);}
        }
        var w=new Workspace();
        for(boolean compose:new boolean[]{false,true}) {
            var m=new LowRankRelationNetwork(compose,.01,71);for(int p=V;p<KEY;p++)m.weights[p]=.99*(2*rng.nextDouble()-1);for(int p=KEY;p<PARAMETERS;p++)m.weights[p]=.99*(2*rng.nextDouble()-1);
            for(var b:boards){var saved=b.clone();assertEquals(400*oracle(m,b),m.scoreCp(b,w),1e-9);assertArrayEquals(saved,b);var mirror=b.clone();long occupied=b[0]|b[1]|b[2];for(int i=0;i<3;i++)mirror[i]=Long.reverseBytes(b[i]);mirror[3]=Long.reverseBytes(occupied^b[3]);mirror[4]^=1;assertEquals(m.scoreCp(b,w),m.scoreCp(mirror,w),1e-9);}
            m.weights[768]=Double.NaN;assertThrows(ArithmeticException.class,()->m.scoreCp(Board.startingPosition(),w));
        }
    }
    static int family(int p){return p<E?0:p<Q?1:p<V?2:p<KEY?3:p<QUERY?4:p<M?5:6;}
    @Test void sharedKeyQueryGradientsTinyFitBoundsAndStateIntegrity()throws Exception {
        for(boolean compose:new boolean[]{false,true}) {
            var m=new LowRankRelationNetwork(compose,.01,71);var rng=new Random(97);for(int p=V;p<KEY;p++)m.weights[p]=.2*rng.nextGaussian();for(int p=M;p<PARAMETERS;p++)m.weights[p]=.2*rng.nextGaussian();
            var w=new Workspace();var f=w.encode(Board.startingPosition()).frozen();var batch=new Features[]{f};double[] targets={.8};m.gradients(batch,targets);int[] checked=new int[7];double[] errors=new double[7];
            for(int n=0;n<m.count;n++){int p=m.touched[n],group=family(p);if(checked[group]>=12||Math.abs(m.gradient[p])<1e-12)continue;checked[group]++;double old=m.weights[p],eps=1e-5;m.weights[p]=old+eps;double plus=PairObjectiveDiagnostic.sigmoid(oracle(m,Board.startingPosition()));m.weights[p]=old-eps;double minus=PairObjectiveDiagnostic.sigmoid(oracle(m,Board.startingPosition()));m.weights[p]=old;double numerical=((plus-.8)*(plus-.8)-(minus-.8)*(minus-.8))/(2*eps);errors[group]=Math.max(errors[group],Math.abs(numerical-m.gradient[p]));assertEquals(numerical,m.gradient[p],1e-10,"family"+group);}
            for(int group:new int[]{0,1,2,4,5})assertTrue(checked[group]>0);if(compose){assertTrue(checked[3]>0);assertTrue(checked[6]>0);}System.out.println("compose="+compose+" independent gradient errors="+Arrays.toString(errors));
            double before=m.gradients(batch,targets);for(int i=0;i<20;i++)m.train(batch,targets);assertTrue(m.gradients(batch,targets)<before);for(double x:m.weights)assertTrue(Math.abs(x)<=.99);
            var restored=read(bytes(m));m.train(batch,targets);restored.train(batch,targets);assertArrayEquals(bytes(m),bytes(restored));byte[] bad=bytes(m);bad[100]^=1;assertThrows(IOException.class,()->read(bad));assertThrows(IOException.class,()->read(Arrays.copyOf(bytes(m),bytes(m).length+1)));
            byte[] mode=bytes(m);java.nio.ByteBuffer.wrap(mode).putInt(8,2);var crc=new java.util.zip.CRC32();crc.update(mode,0,mode.length-8);java.nio.ByteBuffer.wrap(mode).putLong(mode.length-8,crc.getValue());assertThrows(IOException.class,()->read(mode));
        }
    }
    static byte[] bytes(LowRankRelationNetwork m)throws IOException{var out=new ByteArrayOutputStream();m.write(out);return out.toByteArray();}
    static LowRankRelationNetwork read(byte[] b)throws IOException{return LowRankRelationNetwork.read(new ByteArrayInputStream(b));}
    @Test void initialOneTwoAgreementAndExactSegmentTailHistory()throws Exception {
        var one=new LowRankRelationNetwork(false,.01,71);var two=new LowRankRelationNetwork(true,.01,71);assertArrayEquals(one.weights,two.weights);assertEquals(one.scoreCp(Board.startingPosition(),new Workspace()),two.scoreCp(Board.startingPosition(),new Workspace()));
        var data=ContextualPieceFineTuneTest.data();
        for(boolean compose:new boolean[]{false,true}) {
            var whole=new LowRankRelationNetwork(compose,.01,71);var split=read(bytes(whole));
            for(int epoch=1;epoch<=2;epoch++) {
                if(epoch==2){whole=LowRankRelationDiagnostic.fork(whole,.001);split=LowRankRelationDiagnostic.fork(split,.001);}
                LowRankRelationDiagnostic.segment(whole,data,71,epoch,0,257);LowRankRelationDiagnostic.segment(split,data,71,epoch,0,128);split=read(bytes(split));LowRankRelationDiagnostic.segment(split,data,71,epoch,128,257);assertArrayEquals(bytes(whole),bytes(split));
            }
            assertEquals(6,whole.step);byte[] state=bytes(whole);assertThrows(IOException.class,()->MixedTiedPieceNetwork.read(new ByteArrayInputStream(state)));
        }
    }
    @Test void selfEdgesAreRemovedFromBothPasses() {
        long[] board=Board.fromFen("7k/8/8/8/8/4Q3/8/6K1 w - - 0 1");int queen=node(board,20);
        for(boolean compose:new boolean[]{false,true}) {
            var m=new LowRankRelationNetwork(compose,.01,71);Arrays.fill(m.weights,0);m.weights[KEY+queen*RANK]=.4;m.weights[QUERY+queen*RANK]=.5;m.weights[E+queen*WIDTH]=.6;m.weights[Q+queen*WIDTH]=.7;m.weights[V+queen*WIDTH]=.8;
            var w=new Workspace();double score=m.logit(w.encode(board),w);assertEquals(m.unary(w.features),score,1e-14);for(int i=0;i<w.features.size*WIDTH;i++){assertEquals(0,w.a[i],1e-14);assertEquals(0,w.h[i],1e-14);}
        }
    }
}
