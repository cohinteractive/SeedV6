package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrnResearchDataTest {
    @Test void reservoirIsDeterministicLabelIndependentAndSpansTheRange() {
        var a=new BrnResearchData.Reservoir(100,73103);var b=new BrnResearchData.Reservoir(100,73103);
        for(int i=0;i<1000;i++){a.add(new byte[]{(byte)i},i);b.add(new byte[]{(byte)~i},i);}
        assertEquals(100,a.size);assertEquals(1000,a.observations);assertArrayEquals(a.ordinals,b.ordinals);
        assertTrue(java.util.Arrays.stream(a.ordinals).anyMatch(i->i<100));
        assertTrue(java.util.Arrays.stream(a.ordinals).anyMatch(i->i>900));
        assertEquals(100,java.util.Arrays.stream(a.ordinals).distinct().count());
    }
    @Test void allEquivalentInputsRemainInTheSamePartition() {
        var board=Board.startingPosition();var random=new Random(1923);
        for(int ply=0;ply<500;ply++) {
            int expected=BrnResearchData.split(board);
            for(boolean color:new boolean[]{false,true})for(boolean mirror:new boolean[]{false,true}) {
                var transformed=new long[6];transformed[4]=board[4]^Board.PLAYER_BIT;transformed[5]=~board[5];
                for(int square=0;square<64;square++) {
                    int code=Board.getSquare(board[0],board[1],board[2],board[3],square);
                    if(code==0)continue;if(color)code^=8;int target=square^(color?56:0)^(mirror?7:0);
                    for(int word=0;word<4;word++)if((code&(1<<word))!=0)transformed[word]|=1L<<target;
                }
                assertEquals(expected,BrnResearchData.split(transformed));
            }
            var moves=new long[512];int count=BrnResearchDiagnostics.legal(board,moves);
            board=count==0||ply%100==99?Board.startingPosition():BrnResearchDiagnostics.child(board,moves[random.nextInt(count)]);
        }
    }
}
