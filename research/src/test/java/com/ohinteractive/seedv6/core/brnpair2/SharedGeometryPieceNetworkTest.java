package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnpair2.SharedGeometryPieceNetwork.*;

class SharedGeometryPieceNetworkTest {
    static int node(long[] board,int square){int p=Board.getSquare(board[0],board[1],board[2],board[3],square),stm=Board.player((int)board[4]);return ((p&7)-1+6*((p>>>3)^stm))*64+(square^(stm*56));}
    static double oracle(SharedGeometryPieceNetwork model,long[] board) {
        var squares=new ArrayList<Integer>();int stm=Board.player((int)board[4]);
        for(int square=0;square<64;square++)if(Board.getSquare(board[0],board[1],board[2],board[3],square)!=0)squares.add(square);
        double value=BrnPair2Features.material(board)/4+model.weights[768];
        for(int to:squares) {
            int target=node(board,to);value+=model.weights[target];double[] a=new double[8];
            for(int from:squares)if(from!=to) {
                int source=node(board,from),dx=target%64%8-source%64%8,dy=target%64/8-source%64/8,ax=Math.abs(dx),ay=Math.abs(dy),blocks=0;
                boolean aligned=dx==0||dy==0||ax==ay;
                if(aligned) {
                    int physicalStep=Integer.signum(to%8-from%8)+8*Integer.signum(to/8-from/8);
                    for(int square=from+physicalStep;square!=to;square+=physicalStep)if(Board.getSquare(board[0],board[1],board[2],board[3],square)!=0)blocks++;
                }
                double[] x={1,dx/7.0,dy/7.0,ax/7.0,ay/7.0,dx*dy/49.0,(dx*dx+dy*dy)/98.0,dx==0||dy==0?1:0,ax==ay?1:0,ax*ay==2?1:0,aligned&&blocks==0?1:0,blocks/7.0};
                for(int d=0;d<8;d++) {
                    double gate=0;for(int k=0;k<(model.context?12:10);k++)gate+=model.weights[A+(target/64*8+d)*12+k]*x[k];
                    gate/=1+Math.abs(gate);a[d]+=model.weights[E+source*8+d]*gate/Math.sqrt(squares.size()-1);
                }
            }
            for(int d=0;d<8;d++)value+=model.weights[Q+target*8+d]*(a[d]/(1+Math.abs(a[d])))/8;
        }
        return value;
    }
    @Test void independentGeometryBlockersForwardPerspectiveAndSpecialSuccessors() {
        var boards=new ArrayList<long[]>();long[] board=Board.startingPosition(),moves=new long[256],scratch=new long[6];var random=new Random(71);
        for(int n=0;n<40;n++){boards.add(board);int count=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,scratch);if(count==0)break;var next=new long[6];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[random.nextInt(count)],next);board=next;}
        for(String fen:List.of("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 b - - 0 1")) {
            board=Board.fromFen(fen);boards.add(board);int count=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,scratch);
            for(int n=0;n<count;n++){var next=new long[6];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[n],next);boards.add(next);}
        }
        for(boolean context:new boolean[]{false,true}) {
            var model=new SharedGeometryPieceNetwork(context,.01,71);for(int p=A;p<PARAMETERS;p++)if((p-A)%12>=10)model.weights[p]=.2*random.nextGaussian();var w=new Workspace();
            for(var b:boards) {
                var saved=b.clone();assertEquals(400*oracle(model,b),model.scoreCp(b,w),1e-9);assertArrayEquals(saved,b);
                var mirror=b.clone();long occupied=b[0]|b[1]|b[2];for(int i=0;i<3;i++)mirror[i]=Long.reverseBytes(b[i]);mirror[3]=Long.reverseBytes(occupied^b[3]);mirror[4]^=1;
                assertEquals(model.scoreCp(b,w),model.scoreCp(mirror,w),1e-9);
            }
            model.weights[768]=Double.NaN;assertThrows(ArithmeticException.class,()->model.scoreCp(Board.startingPosition(),w));
        }
    }
    static int family(int p){return p<E?0:p<Q?1:p<A?2:(p-A)%12<10?3:4;}
    @Test void allGradientFamiliesTinyFitBoundsAndStateIntegrity()throws Exception {
        for(boolean context:new boolean[]{false,true}) {
            var model=new SharedGeometryPieceNetwork(context,.01,71);var w=new Workspace();var f=w.encode(Board.startingPosition()).frozen();var batch=new Features[]{f};double[] target={.8};
            model.gradients(batch,target);double[] errors=new double[5];int[] checked=new int[5];
            for(int n=0;n<model.count;n++) {
                int p=model.touched[n],group=family(p);if(checked[group]>=12||Math.abs(model.gradient[p])<1e-12)continue;checked[group]++;
                double old=model.weights[p],eps=1e-5;model.weights[p]=old+eps;double plus=PairObjectiveDiagnostic.sigmoid(model.logit(f,w));model.weights[p]=old-eps;double minus=PairObjectiveDiagnostic.sigmoid(model.logit(f,w));model.weights[p]=old;
                double numerical=((plus-.8)*(plus-.8)-(minus-.8)*(minus-.8))/(2*eps);errors[group]=Math.max(errors[group],Math.abs(numerical-model.gradient[p]));assertEquals(numerical,model.gradient[p],1e-10,"family"+group);
            }
            for(int group=0;group<(context?5:4);group++)assertTrue(checked[group]>0);System.out.println("context="+context+" gradient errors="+Arrays.toString(errors));
            double before=model.gradients(batch,target);for(int n=0;n<20;n++)model.train(batch,target);assertTrue(model.gradients(batch,target)<before);for(double x:model.weights)assertTrue(Math.abs(x)<=.99);
            var restored=read(bytes(model));model.train(batch,target);restored.train(batch,target);assertArrayEquals(bytes(model),bytes(restored));
            byte[] bad=bytes(model);bad[100]^=1;assertThrows(IOException.class,()->read(bad));assertThrows(IOException.class,()->read(Arrays.copyOf(bytes(model),bytes(model).length+1)));
            byte[] mode=bytes(model);java.nio.ByteBuffer.wrap(mode).putInt(8,2);var crc=new java.util.zip.CRC32();crc.update(mode,0,mode.length-8);java.nio.ByteBuffer.wrap(mode).putLong(mode.length-8,crc.getValue());assertThrows(IOException.class,()->read(mode));
        }
    }
    static byte[] bytes(SharedGeometryPieceNetwork model)throws IOException{var stream=new ByteArrayOutputStream();model.write(stream);return stream.toByteArray();}
    static SharedGeometryPieceNetwork read(byte[] bytes)throws IOException{return SharedGeometryPieceNetwork.read(new ByteArrayInputStream(bytes));}
    @Test void initialFunctionAndSegmentedTailContinuationAreExact()throws Exception {
        var geometry=new SharedGeometryPieceNetwork(false,.01,71);var context=new SharedGeometryPieceNetwork(true,.01,71);assertArrayEquals(geometry.weights,context.weights);
        var data=ContextualPieceFineTuneTest.data();var w=new Workspace();long[] board=new long[6];
        for(int i=0;i<data.rows;i++){data.board(i,board);assertEquals(geometry.scoreCp(board,w),context.scoreCp(board,w));}
        for(boolean enabled:new boolean[]{false,true}) {
            var full=new SharedGeometryPieceNetwork(enabled,.01,71);var segmented=new SharedGeometryPieceNetwork(enabled,.01,71);
            SharedGeometryPieceDiagnostic.segment(full,data,71,1,0,257);SharedGeometryPieceDiagnostic.segment(segmented,data,71,1,0,128);
            segmented=read(bytes(segmented));SharedGeometryPieceDiagnostic.segment(segmented,data,71,1,128,257);assertArrayEquals(bytes(full),bytes(segmented));assertEquals(3,full.step);
            var fixed=segmented;assertThrows(IllegalArgumentException.class,()->SharedGeometryPieceDiagnostic.segment(fixed,data,71,1,1,257));
        }
    }
    @Test void fixedEndpointsRespondToAnInterveningThirdPieceOnlyThroughContext() {
        double[] changes=new double[2];
        var blockedBoard=Board.fromFen("q6k/8/8/8/P7/8/8/R6K w - - 0 1");
        var clearBoard=Board.fromFen("q6k/8/8/8/1P6/8/8/R6K w - - 0 1");
        int source=node(blockedBoard,0),target=node(blockedBoard,56);
        assertEquals(2,source/64);assertEquals(7,target/64); // Seed's K,Q,R,B,N,P piece order.
        for(int mode=0;mode<2;mode++) {
            var m=new SharedGeometryPieceNetwork(mode==1,.01,71);Arrays.fill(m.weights,0);m.weights[E+source*8]=.5;m.weights[Q+target*8]=.5;m.weights[A+target/64*8*12+10]=.6;
            var w=new Workspace();double blocked=m.scoreCp(blockedBoard,w);double clear=m.scoreCp(clearBoard,w);changes[mode]=clear-blocked;
        }
        assertEquals(0,changes[0]);assertTrue(Math.abs(changes[1])>1e-4);System.out.println("Occupancy-conditioned fixed-endpoint change cp="+changes[1]);
    }
}
