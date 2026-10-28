package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnpair2.BrnPair2Geometry.*;

class ContextPairEnergyTest {
    static final String SHA="0".repeat(64);
    static BrnPair2Model base(){var t=new BrnPair2Trainer(.01);for(int i=0;i<t.weights.length;i++)t.weights[i]=Math.sin(i)*.03;return new BrnPair2Model(t.snapshot());}
    static final int[] BUCKETS={0,1,2,3,4,4,5,5,6,6,6,6,7,7,7,7,8,8,8,8,8,8,8,8,9,9,9,9,9,9,9,9};
    static double oracle(ContextPairEnergy model,long[] board) {
        int stm=Board.player((int)board[4]);double sum=0;
        for(int role=0;role<2;role++) {
            int side=stm^role,king=-1;
            for(int s=0;s<64;s++)if(Board.getSquare(board[0],board[1],board[2],board[3],s)==(1|(side<<3)))king=s^(side*56);
            int bucket=model.buckets==1?0:BUCKETS[king/8*4+Math.min(king%8,7-king%8)];
            for(int t=0;t<TEMPLATES;t++) {
                int state=0;
                for(int j=1;j>=0;j--){int s=cell(t,j)^(side*56),p=Board.getSquare(board[0],board[1],board[2],board[3],s);int code=p==0?0:(p&7)-1+6*((p>>>3)^side)+1;state=13*state+code;}
                sum+=model.weights[((role*model.buckets+bucket)*TEMPLATES+t)*169+state];
            }
        }
        return model.base.newWorkspace().evaluatePawns(board,.25)/4+(model.weights[model.bias]+NORM*sum)/16;
    }
    @Test void independentFormulaColorSymmetryAndSpecialBoards() {
        var boards=new ArrayList<long[]>();long[] board=Board.startingPosition(),moves=new long[256],scratch=new long[6];var random=new Random(71);
        for(int n=0;n<100;n++){boards.add(board);int count=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,scratch);if(count==0)break;var next=new long[6];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[random.nextInt(count)],next);board=next;}
        for(String fen:List.of("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 b - - 0 1"))boards.add(Board.fromFen(fen));
        for(boolean king:new boolean[]{false,true}) {
            var model=new ContextPairEnergy(king,.01,base(),SHA);var w=model.new Workspace();
            for(var b:boards)assertEquals(model.base.newWorkspace().evaluatePawns(b,.25)/4,model.logit(b,w),1e-12);
            for(int i=0;i<model.weights.length;i++)model.weights[i]=Math.cos(i)*.1;
            for(var b:boards) {
                assertEquals(oracle(model,b),model.logit(b,w),1e-12);
                long[] mirror=b.clone();long occupied=b[0]|b[1]|b[2];for(int i=0;i<3;i++)mirror[i]=Long.reverseBytes(b[i]);mirror[3]=Long.reverseBytes(occupied^b[3]);mirror[4]^=1;
                assertEquals(model.logit(b,w),model.logit(mirror,w),1e-12);
            }
            model.weights[model.bias]=Double.POSITIVE_INFINITY;assertThrows(ArithmeticException.class,()->model.logit(boards.getFirst(),w));
        }
    }
    @Test void gradientsTinyFitStateIntegrityAndExactEpochContinuation()throws Exception {
        for(boolean king:new boolean[]{false,true}) {
            var model=new ContextPairEnergy(king,.01,base(),SHA);var data=new PairObjectiveDiagnostic.Data(new long[][]{Board.startingPosition(),Board.fromFen("4k3/8/8/3pP3/8/8/8/4K3 b - - 0 1")},new double[]{.8,.2});
            double initial=model.gradients(data.boards(),data.targets());
            for(int n=0;n<model.count;n+=Math.max(1,model.count/20)) {
                int i=model.touched[n];double eps=1e-4;model.weights[i]=eps;double plus=ContextPairDiagnostic.mse(model,data);model.weights[i]=-eps;double minus=ContextPairDiagnostic.mse(model,data);model.weights[i]=0;
                assertEquals((plus-minus)/(2*eps),model.gradient[i],1e-10);
            }
            for(int i=0;i<20;i++)model.train(data.boards(),data.targets());assertTrue(ContextPairDiagnostic.mse(model,data)<initial);
            var bytes=new ByteArrayOutputStream();model.write(bytes);byte[] saved=bytes.toByteArray();
            var restored=ContextPairEnergy.read(new ByteArrayInputStream(saved),model.base,SHA);
            ContextPairDiagnostic.epoch(model,data,71,2);ContextPairDiagnostic.epoch(restored,data,71,2);
            assertArrayEquals(model.weights,restored.weights);assertArrayEquals(model.first,restored.first);assertArrayEquals(model.second,restored.second);assertEquals(model.step,restored.step);
            assertThrows(IOException.class,()->ContextPairEnergy.read(new ByteArrayInputStream(saved),model.base,"1".repeat(64)));
            saved[saved.length/2]^=1;assertThrows(IOException.class,()->ContextPairEnergy.read(new ByteArrayInputStream(saved),model.base,SHA));
        }
    }
    @Test void kingConditioningCanProduceOtherwiseAbsentRemoteInteraction() {
        long[][] boards={Board.fromFen("6k1/5ppp/8/8/8/8/5PPP/1N4K1 w - - 0 1"),Board.fromFen("6k1/5ppp/8/8/8/8/5PPP/1N3K2 w - - 0 1"),Board.fromFen("6k1/5ppp/8/8/8/2N5/5PPP/6K1 w - - 0 1"),Board.fromFen("6k1/5ppp/8/8/8/2N5/5PPP/5K2 w - - 0 1")};
        InteractionAudit.requireCancellation(boards);
        var shared=new ContextPairEnergy(false,.01,base(),SHA);var king=new ContextPairEnergy(true,.01,base(),SHA);
        for(var model:List.of(shared,king)){for(int i=0;i<model.weights.length;i++)model.weights[i]=Math.sin(i*.31);}
        double a=0,b=0;for(int n=0;n<4;n++){a+=InteractionAudit.SIGNS[n]*shared.logit(boards[n],shared.new Workspace());b+=InteractionAudit.SIGNS[n]*king.logit(boards[n],king.new Workspace());}
        assertEquals(0,a,1e-12);assertTrue(Math.abs(b)>1e-5);
    }
}
