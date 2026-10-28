package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RelationalGraphEnergyTest {
    static boolean edge(long[] board,int s,int t) {
        int p=Board.getSquare(board[0],board[1],board[2],board[3],s),type=p&7;
        int dx=t%8-s%8,dy=t/8-s/8,ax=Math.abs(dx),ay=Math.abs(dy);
        if(s==t||p==0)return false;
        if(type==1)return Math.max(ax,ay)==1;
        if(type==5)return ax*ay==2;
        if(type==6)return ax==1&&dy==((p&8)==0?1:-1);
        boolean diagonal=ax==ay,straight=dx==0||dy==0;
        if(!(type==2&&(diagonal||straight)||type==3&&straight||type==4&&diagonal))return false;
        for(int x=s%8+Integer.signum(dx),y=s/8+Integer.signum(dy);x!=t%8||y!=t/8;x+=Integer.signum(dx),y+=Integer.signum(dy))
            if(Board.getSquare(board[0],board[1],board[2],board[3],y*8+x)!=0)return false;
        return true;
    }
    static int[] oracle(long[] b,RelationalGraphEnergy.Mode mode) {
        var values=new ArrayList<Integer>();values.add(768);int stm=Board.player((int)b[4]);
        for(int s=0;s<64;s++) {
            int a=Board.getSquare(b[0],b[1],b[2],b[3],s);if(a==0)continue;
            int ac=(a&7)-1+6*((a>>>3)^stm),sq=s^(stm*56);values.add(ac*64+sq);
            if(mode==RelationalGraphEnergy.Mode.UNARY)continue;
            for(int t=0;t<64;t++) {
                int target=Board.getSquare(b[0],b[1],b[2],b[3],t);if(target==0||!edge(b,s,t))continue;
                int friendly=0,enemy=0;
                for(int other=0;other<64;other++)if(other!=s&&edge(b,other,t)) {
                    int p=Board.getSquare(b[0],b[1],b[2],b[3],other);
                    if((p&8)==(target&8))friendly++;else enemy++;
                }
                int tc=(target&7)-1+6*((target>>>3)^stm),tq=t^(stm*56);
                int base=(ac*12+tc)*225+(tq/8-sq/8+7)*15+tq%8-sq%8+7;
                values.add(769+(mode==RelationalGraphEnergy.Mode.EDGE?base:base*9+3*Math.min(2,friendly)+Math.min(2,enemy)));
            }
        }
        return values.stream().mapToInt(Integer::intValue).sorted().toArray();
    }
    @Test void independentGeometryAndContextAgreeAcrossLegalAndSpecialBoards() {
        long[] b=Board.startingPosition();var random=new Random(71);long[] moves=new long[256],scratch=new long[6];
        var boards=new ArrayList<long[]>();
        for(int i=0;i<200;i++) {
            boards.add(b);int n=Gen.genAll(b[0],b[1],b[2],b[3],(int)b[4],b[5],true,moves,scratch);if(n==0)break;
            var next=new long[6];Board.makeMoveInto(b[0],b[1],b[2],b[3],(int)b[4],b[5],moves[random.nextInt(n)],next);b=next;
        }
        for(String fen:List.of("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 b - - 0 1"))boards.add(Board.fromFen(fen));
        for(var board:boards)for(var mode:RelationalGraphEnergy.Mode.values()) {
            var f=new RelationalGraphEnergy.Features();f.encode(board,mode);int[] ids=Arrays.copyOf(f.ids,f.size);Arrays.sort(ids);
            assertArrayEquals(oracle(board,mode),ids);
            var mirror=board.clone();long occ=board[0]|board[1]|board[2];
            for(int i=0;i<3;i++)mirror[i]=Long.reverseBytes(board[i]);mirror[3]=Long.reverseBytes(occ^board[3]);mirror[4]^=1;
            var reflected=new RelationalGraphEnergy.Features();reflected.encode(mirror,mode);int[] reflectedIds=Arrays.copyOf(reflected.ids,reflected.size);Arrays.sort(reflectedIds);
            assertArrayEquals(ids,reflectedIds);assertEquals(f.material,reflected.material,1e-8);
        }
    }
    @Test void finiteDifferencesTinyFitAndExactContinuation()throws Exception {
        for(var mode:RelationalGraphEnergy.Mode.values()) {
            var model=new RelationalGraphEnergy(mode,.5);var f=new RelationalGraphEnergy.Features();f.encode(Board.startingPosition(),mode);
            var features=new RelationalGraphEnergy.Features[]{f};double[] targets={.8};model.gradients(features,targets);
            for(int n=0;n<model.count;n+=Math.max(1,model.count/12)) {
                int i=model.touched[n];double eps=1e-3;model.weights[i]=eps;double p=PairObjectiveDiagnostic.sigmoid(model.score(f)/400);
                model.weights[i]=-eps;double q=PairObjectiveDiagnostic.sigmoid(model.score(f)/400);model.weights[i]=0;
                assertEquals(((p-.8)*(p-.8)-(q-.8)*(q-.8))/(2*eps),model.gradient[i],1e-10);
            }
            double before=model.gradients(features,targets);for(int i=0;i<20;i++)model.train(features,targets);
            assertTrue(model.gradients(features,targets)<before);
            var bytes=new ByteArrayOutputStream();model.write(bytes);var restored=RelationalGraphEnergy.read(new ByteArrayInputStream(bytes.toByteArray()));
            model.train(features,targets);restored.train(features,targets);
            var a=new ByteArrayOutputStream();var b=new ByteArrayOutputStream();model.write(a);restored.write(b);assertArrayEquals(a.toByteArray(),b.toByteArray());
            byte[] bad=a.toByteArray();bad[bad.length/2]^=1;assertThrows(IOException.class,()->RelationalGraphEnergy.read(new ByteArrayInputStream(bad)));
        }
    }
}
