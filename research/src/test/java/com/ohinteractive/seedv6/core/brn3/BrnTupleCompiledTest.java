package com.ohinteractive.seedv6.core.brn3;

import com.ohinteractive.seedv6.core.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrnTupleCompiledTest {
    static BrnTupleTrainer randomTrainer() {
        var trainer=new BrnTupleTrainer(2,.01);var random=new Random(671);
        for(int i=0;i<trainer.weights.length;i++)trainer.weights[i]=(random.nextDouble()-.5)*4;
        return trainer;
    }
    @Test void independentForwardAllCategoriesAndImmutability() {
        var trainer=randomTrainer();var source=trainer.snapshot();var compiled=new BrnTupleCompiled(source);
        assertEquals(294,compiled.pairs());assertEquals(799688,compiled.immutablePrimitiveBytes());
        var cache=compiled.newWorkspace();var random=new Random(8731);
        for(int n=0;n<1000;n++) {
            // Independent arbitrary placements exercise all 13 states, including dense boards.
            long[] board=new long[6];
            for(int square=0;square<64;square++) {
                int category=random.nextInt(13),code=category<=6?category:category+2;
                for(int plane=0;plane<4;plane++)if((code&(1<<plane))!=0)board[plane]|=1L<<square;
            }
            if((n&1)==1)board[4]=Board.PLAYER_BIT;
            long[] before=board.clone();
            assertEquals(BrnTupleTrainerTest.direct(source,board),cache.evaluatePawns(board),1e-10);
            assertArrayEquals(before,board);
        }
        long[] board=Board.startingPosition();double before=cache.evaluatePawns(board);
        Arrays.fill(source.weights,999);Arrays.fill(trainer.weights,-777);
        assertEquals(before,cache.evaluatePawns(board),0);
        assertThrows(IllegalArgumentException.class,()->new BrnTupleCompiled(new BrnTupleTrainer(3,.01).snapshot()));
        for(double gain:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-.01,1.01})
            assertThrows(IllegalArgumentException.class,()->cache.evaluatePawns(board,gain));
    }
    @Test void specialLegalUndoStmAndInformationParity() {
        var source=randomTrainer().snapshot();var compiled=new BrnTupleCompiled(source);var cache=compiled.newWorkspace();
        var legacy=source.newWorkspace();long[] moves=new long[512];var random=new Random(4389);
        String[] special={"r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 w - - 0 1"};
        for(String fen:special) {
            var parent=Board.fromFen(fen);int count=Gen.genAll(parent[0],parent[1],parent[2],parent[3],(int)parent[4],parent[5],true,moves,new long[6]);
            for(int i=0;i<count;i++) {
                var child=new long[6];Board.makeMoveInto(parent[0],parent[1],parent[2],parent[3],(int)parent[4],parent[5],moves[i],child);
                for(long[] board:new long[][]{parent,child,parent})assertEquals(BrnTupleTrainerTest.direct(source,board),cache.evaluatePawns(board),1e-10);
            }
        }
        var board=Board.startingPosition();
        for(int ply=0;ply<2400;ply++) {
            for(double gain:new double[]{0,.125,.25,.5,1}) {
                double expected=legacy.evaluatePawns(board,gain),actual=cache.evaluatePawns(board,gain);
                assertEquals(expected,actual,1e-10);assertEquals(Brn3Model.score(expected),Brn3Model.score(actual));
            }
            var flip=board.clone();flip[4]^=Board.PLAYER_BIT;
            assertEquals(BrnTupleTrainerTest.direct(source,flip),cache.evaluatePawns(flip),1e-10);
            var altered=board.clone();altered[4]=(altered[4]&Board.PLAYER_BIT)|(~altered[4]&~Board.PLAYER_BIT);altered[5]^=76543;
            altered[3]^=~(altered[0]|altered[1]|altered[2]);
            assertEquals(BrnTupleTrainerTest.direct(source,board),cache.evaluatePawns(altered),1e-10);
            if(ply<8)assertEquals(BrnTupleTrainerTest.direct(source,board),compiled.newWorkspace().evaluatePawns(altered),1e-10);
            int count=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,new long[6]);
            if(count==0||ply%99==98){board=Board.startingPosition();continue;}
            var child=new long[6];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[random.nextInt(count)],child);board=child;
        }
        assertTrue(cache.updates>100);assertTrue(cache.rebuilds>1);assertTrue(cache.repeats>100);
        var original=BrnTupleTrainerTest.examples()[0];
        var reversed=Board.fromFen("rnbqkb1r/pppp1ppp/5n2/4p3/4P3/2N5/PPPP1PPP/R1BQKBNR b KQkq - 2 3");
        assertEquals(cache.evaluatePawns(original),cache.evaluatePawns(reversed),1e-10);
    }
    @Test void materialGen0AndWorkspaceIsolation() {
        var source=new BrnTupleTrainer(2,.01).snapshot();var compiled=new BrnTupleCompiled(source);
        var first=compiled.newWorkspace();var second=compiled.newWorkspace();
        assertEquals(2920,first.primitiveBytes());
        for(var board:BrnTupleTrainerTest.examples()) {
            double original=first.evaluatePawns(board);
            assertEquals(Brn3Features.material(board),original,1e-12);
            second.evaluatePawns(Board.startingPosition());
            assertEquals(original,first.evaluatePawns(board),0);
        }
        assertTrue(first.rebuilds>0);assertTrue(first.repeats>0);
    }
}
