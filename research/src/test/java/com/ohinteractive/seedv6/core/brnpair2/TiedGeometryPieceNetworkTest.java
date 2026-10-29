package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnpair2.TiedGeometryPieceNetwork.*;

class TiedGeometryPieceNetworkTest {
    static int node(long[] b,int s){return SharedGeometryPieceNetworkTest.node(b,s);}
    static double oracle(TiedGeometryPieceNetwork m,long[] board) {
        var squares=new ArrayList<Integer>();for(int s=0;s<64;s++)if(Board.getSquare(board[0],board[1],board[2],board[3],s)!=0)squares.add(s);
        int n=squares.size();double norm=1/Math.sqrt(n-1),value=BrnPair2Features.material(board)/4+m.weights[768];double[][] h=new double[n][8];double[][][] gates=new double[n][n][8];
        for(int i=0;i<n;i++) {
            int to=squares.get(i),target=node(board,to);value+=m.weights[target];
            for(int j=0;j<n;j++)if(j!=i) {
                int from=squares.get(j),source=node(board,from),dx=target%64%8-source%64%8,dy=target%64/8-source%64/8,ax=Math.abs(dx),ay=Math.abs(dy),blocks=0;boolean aligned=dx==0||dy==0||ax==ay;
                if(aligned){int step=Integer.signum(to%8-from%8)+8*Integer.signum(to/8-from/8);for(int s=from+step;s!=to;s+=step)if(Board.getSquare(board[0],board[1],board[2],board[3],s)!=0)blocks++;}
                double[] x={1,dx/7.0,dy/7.0,ax/7.0,ay/7.0,dx*dy/49.0,(dx*dx+dy*dy)/98.0,dx==0||dy==0?1:0,ax==ay?1:0,ax*ay==2?1:0,aligned&&blocks==0?1:0,blocks/7.0};
                for(int d=0;d<8;d++){double g=0;for(int k=0;k<12;k++)g+=m.weights[A+(target/64*8+d)*12+k]*x[k];g/=1+Math.abs(g);gates[j][i][d]=g;h[i][d]+=norm*m.weights[E+source*8+d]*g;}
            }
            for(int d=0;d<8;d++){h[i][d]/=1+Math.abs(h[i][d]);value+=m.weights[Q+target*8+d]*h[i][d]/8;}
        }
        if(m.compose)for(int i=0;i<n;i++)for(int d=0;d<8;d++){double b=0;for(int j=0;j<n;j++)if(j!=i)b+=norm*h[j][d]*gates[j][i][d];value+=m.weights[V+node(board,squares.get(i))*8+d]*(b/(1+Math.abs(b)))/8;}
        return value;
    }
    @Test void independentTwoPassOracleLegalSpecialColorAndCachedPaths() {
        var boards=new ArrayList<long[]>();long[] board=Board.startingPosition(),moves=new long[256],scratch=new long[6];var rng=new Random(71);
        for(int n=0;n<40;n++){boards.add(board);int count=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,scratch);if(count==0)break;var next=new long[6];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[rng.nextInt(count)],next);board=next;}
        for(String fen:List.of("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 b - - 0 1")) {
            board=Board.fromFen(fen);boards.add(board);int count=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,scratch);for(int n=0;n<count;n++){var next=new long[6];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[n],next);boards.add(next);}
        }
        for(boolean compose:new boolean[]{false,true}) {
            var m=new TiedGeometryPieceNetwork(compose,.01,71);for(int p=A;p<V;p++)if((p-A)%12>=10)m.weights[p]=.2*rng.nextGaussian();for(int p=V;p<PARAMETERS;p++)m.weights[p]=.2*rng.nextGaussian();m.invalidateProjections();var w=new Workspace();
            for(var b:boards){var saved=b.clone();assertEquals(400*oracle(m,b),m.scoreCp(b,w),1e-9);assertEquals(m.scoreCpUncached(b,new Workspace()),m.scoreCp(b,w),1e-10);assertArrayEquals(saved,b);var mirror=b.clone();long occupied=b[0]|b[1]|b[2];for(int i=0;i<3;i++)mirror[i]=Long.reverseBytes(b[i]);mirror[3]=Long.reverseBytes(occupied^b[3]);mirror[4]^=1;assertEquals(m.scoreCp(b,w),m.scoreCp(mirror,w),1e-9);}
            m.weights[768]=Double.NaN;assertThrows(ArithmeticException.class,()->m.scoreCp(Board.startingPosition(),w));
        }
    }
    static int family(int p){return p<E?0:p<Q?1:p<A?2:p<V?((p-A)%12<10?3:4):5;}
    @Test void allSharedGradientRoutesTinyFitBoundsAndStateIntegrity()throws Exception {
        for(boolean compose:new boolean[]{false,true}) {
            var m=new TiedGeometryPieceNetwork(compose,.01,71);var rng=new Random(97);for(int p=V;p<PARAMETERS;p++)m.weights[p]=.2*rng.nextGaussian();m.invalidateProjections();var w=new Workspace();var f=w.encode(Board.startingPosition()).frozen();var batch=new Features[]{f};double[] target={.8};m.gradients(batch,target);int[] checked=new int[6];double[] errors=new double[6];
            for(int n=0;n<m.count;n++){int p=m.touched[n],group=family(p);if(checked[group]>=12||Math.abs(m.gradient[p])<1e-12)continue;checked[group]++;double old=m.weights[p],eps=1e-5;m.weights[p]=old+eps;double plus=PairObjectiveDiagnostic.sigmoid(m.logit(f,w));m.weights[p]=old-eps;double minus=PairObjectiveDiagnostic.sigmoid(m.logit(f,w));m.weights[p]=old;double numerical=((plus-.8)*(plus-.8)-(minus-.8)*(minus-.8))/(2*eps);errors[group]=Math.max(errors[group],Math.abs(numerical-m.gradient[p]));assertEquals(numerical,m.gradient[p],1e-10,"family"+group);}
            for(int group=0;group<(compose?6:5);group++)assertTrue(checked[group]>0);System.out.println("compose="+compose+" gradient errors="+Arrays.toString(errors));
            double before=m.gradients(batch,target);for(int n=0;n<20;n++)m.train(batch,target);assertTrue(m.gradients(batch,target)<before);for(double x:m.weights)assertTrue(Math.abs(x)<=.99);
            var restored=read(bytes(m));m.train(batch,target);restored.train(batch,target);assertArrayEquals(bytes(m),bytes(restored));byte[] bad=bytes(m);bad[100]^=1;assertThrows(IOException.class,()->read(bad));assertThrows(IOException.class,()->read(Arrays.copyOf(bytes(m),bytes(m).length+1)));
            byte[] mode=bytes(m);java.nio.ByteBuffer.wrap(mode).putInt(8,2);var crc=new java.util.zip.CRC32();crc.update(mode,0,mode.length-8);java.nio.ByteBuffer.wrap(mode).putLong(mode.length-8,crc.getValue());assertThrows(IOException.class,()->read(mode));
        }
    }
    static byte[] bytes(TiedGeometryPieceNetwork m)throws IOException{var out=new ByteArrayOutputStream();m.write(out);return out.toByteArray();}
    static TiedGeometryPieceNetwork read(byte[] bytes)throws IOException{return TiedGeometryPieceNetwork.read(new ByteArrayInputStream(bytes));}
    @Test void oneExactlyMatchesR040ActiveHistoryAndBothModesResumeAcrossTails()throws Exception {
        var data=ContextualPieceFineTuneTest.data();var old=new SharedGeometryPieceNetwork(true,.01,71);var one=new TiedGeometryPieceNetwork(false,.01,71);var tied=new TiedGeometryPieceNetwork(true,.01,71);
        assertArrayEquals(old.weights,Arrays.copyOf(one.weights,V));assertArrayEquals(one.weights,tied.weights);var board=Board.startingPosition();assertEquals(old.scoreCp(board,new SharedGeometryPieceNetwork.Workspace()),tied.scoreCp(board,new Workspace()));
        for(int epoch=1;epoch<=2;epoch++) {
            SharedGeometryPieceDiagnostic.segment(old,data,71,epoch,0,257);TiedGeometryPieceDiagnostic.segment(one,data,71,epoch,0,128);one=read(bytes(one));TiedGeometryPieceDiagnostic.segment(one,data,71,epoch,128,257);
            assertArrayEquals(old.weights,Arrays.copyOf(one.weights,V));assertArrayEquals(old.first,Arrays.copyOf(one.first,V));assertArrayEquals(old.second,Arrays.copyOf(one.second,V));assertEquals(old.step,one.step);
        }
        var segmented=read(bytes(tied));TiedGeometryPieceDiagnostic.segment(tied,data,71,1,0,257);TiedGeometryPieceDiagnostic.segment(segmented,data,71,1,0,128);segmented=read(bytes(segmented));TiedGeometryPieceDiagnostic.segment(segmented,data,71,1,128,257);assertArrayEquals(bytes(tied),bytes(segmented));assertEquals(3,tied.step);
    }
    @Test void secondReadoutHasANonlinearThreeSourceWitness() {
        String[] placements={"N1BQ2K1","2BQ2K1","N2Q2K1","3Q2K1","N1B3K1","2B3K1","N5K1","6K1"};double[] contrast=new double[2];
        for(int mode=0;mode<2;mode++) {
            var m=new TiedGeometryPieceNetwork(mode==1,.01,71);Arrays.fill(m.weights,0);for(int type=0;type<12;type++)m.weights[A+type*8*12]=.5;
            m.weights[E+(4*64+17)*8]=.5;m.weights[E+(3*64+11)*8]=.5;m.weights[E+(1*64+19)*8]=.5;m.weights[V+6*8]=.5;m.invalidateProjections();
            for(int mask=0;mask<8;mask++){String rank3=(mask&1)!=0?((mask&4)!=0?"1N1Q4":"1N6"):((mask&4)!=0?"3Q4":"8");String rank2=(mask&2)!=0?"3B1PPP":"5PPP";long[] b=Board.fromFen("6k1/5ppp/8/8/8/"+rank3+"/"+rank2+"/"+placements[mask]+" w - - 0 1");contrast[mode]+=(Integer.bitCount(mask)%2==0?-1:1)*m.scoreCp(b,new Workspace());}
        }
        assertEquals(0,contrast[0],1e-10);assertTrue(Math.abs(contrast[1])>1e-5);System.out.println("Tied second-path third-source contrast cp="+contrast[1]);
    }
}
