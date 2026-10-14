package com.ohinteractive.seedv6.core;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.ohinteractive.seedv6.core.util.Piece;

class SeeQuietTest {
    @Test void generatedQuietThresholdsMatchIndependentLegalExchangeOracle() {
        var random=new Random(2026101425L);int moves=0,negative=0,comparisons=0;
        for(int game=0;game<12;game++) {
            var board=Board.startingPosition();
            for(int ply=0;ply<70;ply++) {
                var legal=Sr024ExchangeCost.legal(board);
                for(long move:legal) {
                    int type=(int)(move>>>Board.START_PIECE_SHIFT)&Piece.TYPE;
                    if(type==Piece.KING || Sr024ExchangeCost.tactical(board,move))continue;
                    var child=Sr024ExchangeCost.child(board,move);var preserved=child.clone();
                    int expected=SeeLegalOracle.evaluate(board,move);assertTrue(expected<=0);
                    for(int threshold:new int[]{Integer.MIN_VALUE,-976,-330,-100,expected-1,expected,expected+1,0,1,Integer.MAX_VALUE}) {
                        assertEquals(expected>=threshold,See.quietAtLeastAfterGeneratedLegal(child,move,threshold));
                        comparisons++;
                    }
                    assertArrayEquals(preserved,child);moves++;if(expected<0)negative++;
                }
                if(legal.length==0)break;
                board=Sr024ExchangeCost.child(board,legal[random.nextInt(legal.length)]);
            }
        }
        assertTrue(negative>100);assertTrue(moves>1000);
        System.out.println("SR024 quiet oracle moves="+moves+" negative="+negative+" thresholds="+comparisons);
    }
}
