package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnpair2.ContextualPieceNetwork.*;

class ContextualPieceNetworkTest {
    static int node(long[] b,int square){int p=Board.getSquare(b[0],b[1],b[2],b[3],square),stm=Board.player((int)b[4]);return ((p&7)-1+6*((p>>>3)^stm))*64+(square^(stm*56));}
    static double oracle(ContextualPieceNetwork m,long[] b) {
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
        if(m.compose)for(int i:squares)for(int d=0;d<8;d++) {
            double k=0;
            for(int j:squares)if(j!=i){int from=j^(stm*56),to=i^(stm*56),rel=node(b,i)/64*225+(to/8-from/8+7)*15+to%8-from%8+7;k+=h[j][d]*m.weights[S+rel*8+d]/Math.sqrt(squares.size()-1);}
            value+=m.weights[V+node(b,i)*8+d]*k/(1+Math.abs(k))/8;
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
            var m=new ContextualPieceNetwork(compose,.01,71);for(int i=V;i<PARAMETERS;i++)m.weights[i]=.3*rng.nextGaussian();var w=new Workspace();
            for(var board:boards){var saved=board.clone();assertEquals(400*oracle(m,board),m.scoreCp(board,w),1e-9);assertArrayEquals(saved,board);var mirror=board.clone();long occ=board[0]|board[1]|board[2];for(int i=0;i<3;i++)mirror[i]=Long.reverseBytes(board[i]);mirror[3]=Long.reverseBytes(occ^board[3]);mirror[4]^=1;assertEquals(m.scoreCp(board,w),m.scoreCp(mirror,w),1e-9);}
            m.weights[768]=Double.NaN;assertThrows(ArithmeticException.class,()->m.scoreCp(Board.startingPosition(),w));
        }
    }
    @Test void gradientFamiliesTinyFitBoundsAndExactResume()throws Exception {
        for(boolean compose:new boolean[]{false,true}) {
            var m=new ContextualPieceNetwork(compose,.01,71);var rng=new Random(97);for(int i=V;i<PARAMETERS;i++)m.weights[i]=.2*rng.nextGaussian();var w=new Workspace();var f=w.encode(Board.startingPosition()).frozen();var batch=new Features[]{f};double[] target={.8};m.gradients(batch,target);
            int[] ends={0,E,R,Q,S,V,PARAMETERS};double[] errors=new double[6];
            for(int family=0;family<6;family++) {
                if(!compose&&family>=4)continue;int checked=0;
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
    static byte[] bytes(ContextualPieceNetwork m)throws IOException{var out=new ByteArrayOutputStream();m.write(out);return out.toByteArray();}
    static ContextualPieceNetwork read(byte[] bytes)throws IOException{return ContextualPieceNetwork.read(new ByteArrayInputStream(bytes));}
    @Test void matchedInitialFunctionAndSegmentedBatchTailParity()throws Exception {
        var one=new ContextualPieceNetwork(false,.01,71);var two=new ContextualPieceNetwork(true,.01,71);assertArrayEquals(one.weights,two.weights);
        var data=new PairCoverageDiagnostic.Data(257);var w=new Workspace();var all=new Features[257];
        for(int i=0;i<257;i++){long[] b=i%2==0?Board.startingPosition():Board.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1");System.arraycopy(b,0,data.boards,i*6,6);data.targets[i]=(i%5)/4.0;all[i]=w.encode(b).frozen();assertEquals(one.scoreCp(b,w),two.scoreCp(b,w));}
        int[] order=new int[257];for(int i=0;i<257;i++)order[i]=i;var rng=new Random(72);for(int i=256;i>0;i--){int j=rng.nextInt(i+1),v=order[i];order[i]=order[j];order[j]=v;}
        var reference=new ContextualPieceNetwork(true,.01,71);for(int start=0;start<257;start+=128){int n=Math.min(128,257-start);var batch=new Features[n];var targets=new double[n];for(int j=0;j<n;j++){batch[j]=all[order[start+j]];targets[j]=data.targets[order[start+j]];}reference.train(batch,targets);}
        ContextualPieceDiagnostic.segment(two,data,71,1,0,128);two=read(bytes(two));ContextualPieceDiagnostic.segment(two,data,71,1,128,257);assertArrayEquals(bytes(reference),bytes(two));assertEquals(3,two.step);
        var finalModel=two;assertThrows(IllegalArgumentException.class,()->ContextualPieceDiagnostic.segment(finalModel,data,71,1,1,257));
    }
    @Test void contextualSecondPathHasNonlinearThreeSourceWitness() {
        String[] placements={"N1BQ2K1","2BQ2K1","N2Q2K1","3Q2K1","N1B3K1","2B3K1","N5K1","6K1"};double[] delta=new double[2];
        for(int mode=0;mode<2;mode++) {
            var m=new ContextualPieceNetwork(mode==1,.01,71);Arrays.fill(m.weights,0);
            for(int rel=0;rel<2700;rel++){m.weights[R+rel*8]=.5;m.weights[S+rel*8]=.5;}
            m.weights[E+(4*64+17)*8]=.5;m.weights[E+(3*64+11)*8]=.5;m.weights[E+(1*64+19)*8]=.5;m.weights[V+6*8]=.5;
            for(int mask=0;mask<8;mask++){String rank3=(mask&1)!=0?((mask&4)!=0?"1N1Q4":"1N6"):((mask&4)!=0?"3Q4":"8");String rank2=(mask&2)!=0?"3B1PPP":"5PPP";long[] b=Board.fromFen("6k1/5ppp/8/8/8/"+rank3+"/"+rank2+"/"+placements[mask]+" w - - 0 1");delta[mode]+=(Integer.bitCount(mask)%2==0?-1:1)*m.scoreCp(b,new Workspace());}
        }
        assertEquals(0,delta[0],1e-10);assertTrue(Math.abs(delta[1])>1e-5);System.out.println("Second-pass third-source contrast cp="+delta[1]);
    }
}
