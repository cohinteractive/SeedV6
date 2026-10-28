package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnpair2.AnchorNeuralNetwork.*;

class AnchorNeuralNetworkTest {
    static double oracle(AnchorNeuralNetwork model,long[] board) {
        var squares=new ArrayList<Integer>();int[] kings={-1,-1};int stm=Board.player((int)board[4]);
        for(int s=0;s<64;s++){int piece=Board.getSquare(board[0],board[1],board[2],board[3],s);if(piece!=0)squares.add(s);if((piece&7)==1)kings[(piece>>>3)^stm]=s;}
        double[][] anchors=new double[2][8];double sum=BrnPair2Features.material(board)/4+model.weights[768];
        for(int k=0;k<2;k++)for(int s:squares)if(s!=kings[k]) {
            int code=Board.getSquare(board[0],board[1],board[2],board[3],s),a=s^(stm*56),b=kings[k]^(stm*56);
            int id=((code&7)-1+6*((code>>>3)^stm))*64+a,rel=k*225+(b/8-a/8+7)*15+b%8-a%8+7;
            for(int d=0;d<8;d++)anchors[k][d]+=model.weights[E+id*8+d]*model.weights[R1+rel*8+d]/Math.sqrt(squares.size()-1);
        }
        if(model.nonlinear)for(var row:anchors)for(int d=0;d<8;d++)row[d]/=1+Math.abs(row[d]);
        for(int s:squares) {
            int code=Board.getSquare(board[0],board[1],board[2],board[3],s),a=s^(stm*56),id=((code&7)-1+6*((code>>>3)^stm))*64+a;sum+=model.weights[id];
            for(int d=0;d<8;d++) {
                double value=0;for(int k=0;k<2;k++)if(s!=kings[k]){int b=kings[k]^(stm*56),rel=k*225+(b/8-a/8+7)*15+b%8-a%8+7;value+=anchors[k][d]*model.weights[R2+rel*8+d];}
                if(model.nonlinear)value/=1+Math.abs(value);sum+=model.weights[O+id*8+d]*value/8;
            }
        }
        return sum;
    }
    @Test void independentEquationsColorSymmetrySpecialBoardsAndFiniteChecks() {
        for(boolean nonlinear:new boolean[]{false,true}) {
            var model=new AnchorNeuralNetwork(nonlinear,.01,71);var w=new Workspace();
            for(String fen:List.of("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 b - - 0 1")) {
                var b=Board.fromFen(fen);assertEquals(400*oracle(model,b),model.scoreCp(b,w),1e-10);
                var mirror=b.clone();long occupied=b[0]|b[1]|b[2];for(int i=0;i<3;i++)mirror[i]=Long.reverseBytes(b[i]);mirror[3]=Long.reverseBytes(occupied^b[3]);mirror[4]^=1;
                assertEquals(model.scoreCp(b,w),model.scoreCp(mirror,w),1e-9);
            }
            model.weights[768]=Double.NaN;assertThrows(ArithmeticException.class,()->model.scoreCp(Board.startingPosition(),w));
        }
    }
    @Test void allGradientFamiliesTinyFitBoundsAndExactContinuation()throws Exception {
        for(boolean nonlinear:new boolean[]{false,true}) {
            var model=new AnchorNeuralNetwork(nonlinear,.01,71);var w=new Workspace();var f=w.encode(Board.startingPosition()).frozen();var batch=new Features[]{f};double[] target={.8};model.gradients(batch,target);
            int[] ends={0,E,R1,R2,O,PARAMETERS};double[] errors=new double[5];
            for(int family=0;family<5;family++) {
                int checked=0;
                for(int n=0;n<model.count&&checked<7;n++) {
                    int i=model.touched[n];if(i<ends[family]||i>=ends[family+1])continue;checked++;
                    double old=model.weights[i],eps=1e-5;model.weights[i]=old+eps;double p=PairObjectiveDiagnostic.sigmoid(model.logit(f,w));model.weights[i]=old-eps;double q=PairObjectiveDiagnostic.sigmoid(model.logit(f,w));model.weights[i]=old;
                    double numerical=((p-.8)*(p-.8)-(q-.8)*(q-.8))/(2*eps);errors[family]=Math.max(errors[family],Math.abs(numerical-model.gradient[i]));
                    assertEquals(numerical,model.gradient[i],1e-10,"family"+family);
                }
                assertTrue(checked>0);
            }
            System.out.println("nonlinear="+nonlinear+" maximum gradient error by family="+Arrays.toString(errors));
            double before=model.gradients(batch,target);for(int i=0;i<20;i++)model.train(batch,target);assertTrue(model.gradients(batch,target)<before);
            for(double v:model.weights)assertTrue(Math.abs(v)<=.99);
            var bytes=new ByteArrayOutputStream();model.write(bytes);var restored=AnchorNeuralNetwork.read(new ByteArrayInputStream(bytes.toByteArray()));
            model.train(batch,target);restored.train(batch,target);var a=new ByteArrayOutputStream();var b=new ByteArrayOutputStream();model.write(a);restored.write(b);assertArrayEquals(a.toByteArray(),b.toByteArray());
            byte[] bad=a.toByteArray();bad[100]^=1;assertThrows(IOException.class,()->AnchorNeuralNetwork.read(new ByteArrayInputStream(bad)));
        }
    }
    @Test void nonlinearThirdSourceWitnessCannotBeAnAdditivePairReadout() {
        String[] placements={"N1BQ2K1","2BQ2K1","N2Q2K1","3Q2K1","N1B3K1","2B3K1","N5K1","6K1"};double[] differences=new double[2];
        for(int mode=0;mode<2;mode++) {
            var model=new AnchorNeuralNetwork(mode==1,.01,71);Arrays.fill(model.weights,0);
            for(int rel=0;rel<450;rel++){model.weights[R1+rel*8]=.5;model.weights[R2+rel*8]=.5;}
            // Three independently relocated source nodes; a fixed own-king readout.
            model.weights[E+(4*64+17)*8]=.5;model.weights[E+(3*64+11)*8]=.5;model.weights[E+(1*64+19)*8]=.5;model.weights[O+6*8]=.5;
            for(int mask=0;mask<8;mask++) {
                String rank3=(mask&1)!=0?((mask&4)!=0?"1N1Q4":"1N6"):((mask&4)!=0?"3Q4":"8");String rank2=(mask&2)!=0?"3B1PPP":"5PPP";
                long[] board=Board.fromFen("6k1/5ppp/8/8/8/"+rank3+"/"+rank2+"/"+placements[mask]+" w - - 0 1");
                differences[mode]+=(Integer.bitCount(mask)%2==0?-1:1)*model.scoreCp(board,new Workspace());
            }
        }
        assertEquals(0,differences[0],1e-10);assertTrue(Math.abs(differences[1])>1e-5);
    }
    @Test void disconnectedKingKnightHasALearnedRemoteResponse() {
        var model=new AnchorNeuralNetwork(true,.01,71);Arrays.fill(model.weights,0);
        for(int rel=0;rel<450;rel++){model.weights[R1+rel*8]=.5;model.weights[R2+rel*8]=.5;}
        model.weights[E+(4*64+18)*8]=.5;model.weights[O+6*8]=.5;
        String[] fens={"6k1/5ppp/8/8/8/8/5PPP/1N4K1 w - - 0 1","6k1/5ppp/8/8/8/8/5PPP/1N3K2 w - - 0 1","6k1/5ppp/8/8/8/2N5/5PPP/6K1 w - - 0 1","6k1/5ppp/8/8/8/2N5/5PPP/5K2 w - - 0 1"};double mixed=0;
        for(int i=0;i<4;i++)mixed+=InteractionAudit.SIGNS[i]*model.scoreCp(Board.fromFen(fens[i]),new Workspace());assertTrue(Math.abs(mixed)>1e-4);
    }
}
