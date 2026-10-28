package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnpair2.WideComposedAttackFieldNetwork.*;

class WideComposedAttackFieldNetworkTest {
    static int node(long[] b,int square) {int code=Board.getSquare(b[0],b[1],b[2],b[3],square),stm=Board.player((int)b[4]);return (code==0?12:(code&7)-1+6*((code>>>3)^stm))*64+(square^(stm*56));}
    static double oracle(WideComposedAttackFieldNetwork m,long[] b) {
        var squares=new ArrayList<Integer>();int[] kings={-1,-1};int stm=Board.player((int)b[4]);
        for(int s=0;s<64;s++){int code=Board.getSquare(b[0],b[1],b[2],b[3],s);if(code!=0)squares.add(s);if((code&7)==1)kings[(code>>>3)^stm]=s;}
        double[][] h=new double[2][WIDTH];
        for(int k=0;k<2;k++)for(int s:squares)if(s!=kings[k]) {
            int a=s^(stm*56),c=kings[k]^(stm*56),rel=k*225+(c/8-a/8+7)*15+c%8-a%8+7;
            for(int d=0;d<WIDTH;d++)h[k][d]+=m.weights[E+WIDTH*node(b,s)+d]*m.weights[R+WIDTH*rel+d]/Math.sqrt(squares.size()-1);
        }
        for(var row:h)for(int d=0;d<WIDTH;d++)row[d]/=1+Math.abs(row[d]);double sum=BrnPair2Features.material(b)/4+m.weights[768];
        double[][] fields=new double[64][WIDTH];
        for(int target=0;target<64;target++) {

            int id=node(b,target),degree=0;double[] a=new double[WIDTH];if(squares.contains(target))sum+=m.weights[id];
            for(int source:squares)if(RelationalGraphEnergyTest.edge(b,source,target)) {
                degree++;int sq=source^(stm*56),tq=target^(stm*56),rel=id/64*225+(tq/8-sq/8+7)*15+tq%8-sq%8+7;
                for(int d=0;d<WIDTH;d++)a[d]+=m.weights[E+WIDTH*node(b,source)+d]*m.weights[L+WIDTH*rel+d];
            }
            for(int d=0;d<WIDTH;d++) {
                double gain=1;for(int k=0;k<2;k++)gain+=m.weights[G+(id/64*2+k)*WIDTH+d]*h[k][d];
                double z=degree==0?0:a[d]*gain/Math.sqrt(degree);fields[target][d]=z/(1+Math.abs(z));sum+=m.weights[O+id*WIDTH+d]*fields[target][d]/WIDTH;
            }
        }
        if(m.compose)for(int source:squares) {
            int id=node(b,source),degree=0;double[] query=new double[WIDTH];
            for(int target=0;target<64;target++)if(RelationalGraphEnergyTest.edge(b,source,target)) {
                degree++;int sq=source^(stm*56),tq=target^(stm*56),rel=id/64*225+(tq/8-sq/8+7)*15+tq%8-sq%8+7;
                for(int d=0;d<WIDTH;d++)query[d]+=m.weights[U+rel*WIDTH+d]*fields[target][d];
            }
            for(int d=0;d<WIDTH;d++){double value=degree==0?0:query[d]/Math.sqrt(degree);sum+=m.weights[V+id*WIDTH+d]*value/(1+Math.abs(value))/WIDTH;}
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
        for(boolean compose:new boolean[]{false,true}) {
            var m=new WideComposedAttackFieldNetwork(compose,.01,71);for(int i=G;i<PARAMETERS;i++)m.weights[i]=.3*random.nextGaussian();var w=new Workspace();
            for(var board:boards) {
                var saved=board.clone();assertEquals(400*oracle(m,board),m.scoreCp(board,w),1e-9);assertArrayEquals(saved,board);
                var mirror=board.clone();long occ=board[0]|board[1]|board[2];for(int i=0;i<3;i++)mirror[i]=Long.reverseBytes(board[i]);mirror[3]=Long.reverseBytes(occ^board[3]);mirror[4]^=1;
                assertEquals(m.scoreCp(board,w),m.scoreCp(mirror,w),1e-9);
            }
            m.weights[768]=Double.NaN;assertThrows(ArithmeticException.class,()->m.scoreCp(Board.startingPosition(),w));
        }
    }
    @Test void allGradientFamiliesTinyFitBoundsAndExactContinuation()throws Exception {
        for(boolean compose:new boolean[]{false,true}) {
            var m=new WideComposedAttackFieldNetwork(compose,.01,71);var random=new Random(97);for(int i=G;i<PARAMETERS;i++)m.weights[i]=.2*random.nextGaussian();
            for(int i=O+768*WIDTH;i<G;i++)m.weights[i]=.2*random.nextGaussian();
            var w=new Workspace();var f=w.encode(Board.startingPosition(),true).frozen();var batch=new Features[]{f};double[] target={.8};m.gradients(batch,target);
            int[] ends={0,E,L,L+2700*WIDTH,R,O,O+768*WIDTH,G,G+24*WIDTH,U,V,PARAMETERS};double[] errors=new double[11];
            for(int family=0;family<11;family++) {
                if(!compose&&(family==9||family==10))continue;int checked=0;
                for(int n=0;n<m.count&&checked<9;n++) {
                    int i=m.touched[n];if(i<ends[family]||i>=ends[family+1]||Math.abs(m.gradient[i])<1e-12)continue;checked++;
                    double old=m.weights[i],eps=1e-5;m.weights[i]=old+eps;double p=PairObjectiveDiagnostic.sigmoid(m.logit(f,w));m.weights[i]=old-eps;double q=PairObjectiveDiagnostic.sigmoid(m.logit(f,w));m.weights[i]=old;
                    double numerical=((p-.8)*(p-.8)-(q-.8)*(q-.8))/(2*eps);errors[family]=Math.max(errors[family],Math.abs(numerical-m.gradient[i]));assertEquals(numerical,m.gradient[i],1e-10,"family"+family);
                }
                assertTrue(checked>0);
            }
            System.out.println("compose="+compose+" maximum gradient error="+Arrays.toString(errors));double before=m.gradients(batch,target);for(int i=0;i<20;i++)m.train(batch,target);assertTrue(m.gradients(batch,target)<before);
            for(double v:m.weights)assertTrue(Math.abs(v)<=.99);
            var bytes=new ByteArrayOutputStream();m.write(bytes);var restored=WideComposedAttackFieldNetwork.read(new ByteArrayInputStream(bytes.toByteArray()));m.train(batch,target);restored.train(batch,target);
            var a=new ByteArrayOutputStream();var c=new ByteArrayOutputStream();m.write(a);restored.write(c);assertArrayEquals(a.toByteArray(),c.toByteArray());byte[] bad=a.toByteArray();bad[100]^=1;assertThrows(IOException.class,()->WideComposedAttackFieldNetwork.read(new ByteArrayInputStream(bad)));
        }
    }
    @Test void pairedChannelsContainArbitraryWidthEightFunctionAndRejectOldGeometry()throws Exception {
        var old=new ComposedAttackFieldNetwork(true,.01,71);var wide=new WideComposedAttackFieldNetwork(true,.01,71);var random=new Random(97);
        for(int i=0;i<old.weights.length;i++)old.weights[i]=.2*random.nextGaussian();System.arraycopy(old.weights,0,wide.weights,0,769);
        int[] oldOffsets={ComposedAttackFieldNetwork.E,ComposedAttackFieldNetwork.L,ComposedAttackFieldNetwork.R,ComposedAttackFieldNetwork.O,ComposedAttackFieldNetwork.G,ComposedAttackFieldNetwork.U,ComposedAttackFieldNetwork.V,ComposedAttackFieldNetwork.PARAMETERS};
        int[] offsets={E,L,R,O,G,U,V,PARAMETERS};
        for(int group=0;group<7;group++)for(int row=0;row<(oldOffsets[group+1]-oldOffsets[group])/8;row++)for(int d=0;d<16;d++)wide.weights[offsets[group]+row*16+d]=old.weights[oldOffsets[group]+row*8+d%8];
        for(String fen:List.of("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 b - - 0 1")) {
            var board=Board.fromFen(fen);assertEquals(old.scoreCp(board,new ComposedAttackFieldNetwork.Workspace()),wide.scoreCp(board,new Workspace()),1e-9);
        }
        var bytes=new ByteArrayOutputStream();old.write(bytes);assertThrows(IOException.class,()->WideComposedAttackFieldNetwork.read(new ByteArrayInputStream(bytes.toByteArray())));
    }
    @Test void batchLocalEncodingMatchesPreencodedUpdatesIncludingTail()throws Exception {
        var data=new PairCoverageDiagnostic.Data(257);var w=new Workspace();var all=new Features[257];double[] targets=new double[257];
        for(int i=0;i<257;i++){long[] b=i%2==0?Board.startingPosition():Board.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1");System.arraycopy(b,0,data.boards,i*6,6);data.targets[i]=targets[i]=(i%5)/4.0;all[i]=w.encode(b,true).frozen();}
        var a=new WideComposedAttackFieldNetwork(true,.01,71);var b=new WideComposedAttackFieldNetwork(true,.01,71);int[] order=new int[257];for(int i=0;i<257;i++)order[i]=i;var random=new Random(72);
        for(int i=256;i>0;i--){int j=random.nextInt(i+1),v=order[i];order[i]=order[j];order[j]=v;}
        for(int offset=0;offset<257;offset+=128){int n=Math.min(128,257-offset);var batch=new Features[n];var target=new double[n];for(int j=0;j<n;j++){batch[j]=all[order[offset+j]];target[j]=targets[order[offset+j]];}a.train(batch,target);}
        WideComposedAttackFieldDiagnostic.epoch(b,data,71,1);var aa=new ByteArrayOutputStream();var bb=new ByteArrayOutputStream();a.write(aa);b.write(bb);assertArrayEquals(aa.toByteArray(),bb.toByteArray());
    }
}
