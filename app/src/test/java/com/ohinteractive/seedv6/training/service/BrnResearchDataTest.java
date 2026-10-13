package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import java.util.Random;
import java.nio.file.*;
import java.io.IOException;
import java.util.List;
import com.ohinteractive.seedv6.training.data.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BrnResearchDataTest {
    @TempDir Path temporary;
    @Test void unavailableSourceOrNavigationCannotCreateOutput() throws Exception {
        Path output=temporary.resolve("new/data"),source=temporary.resolve("source.jsonl"),navigation=temporary.resolve("seek");
        assertThrows(IOException.class,()->BrnResearchData.prepare(source,output,2,2));
        assertFalse(Files.exists(output.getParent()));
        Files.writeString(source,SourceReadersTest.line(0));
        var registered=DataSource.register("test",source,1);
        assertThrows(IOException.class,()->BrnResearchData.prepare(source,output,2,2,5000,navigation,true));
        assertFalse(Files.exists(output.getParent()));
        var points=List.of(new SourceReaders.Point(0,0,0));
        for(String identity:List.of("0".repeat(64),registered.identity())) {
            var index=new SourceReaders.Index(identity,points,-1,DataFiles.hash(identity+points+-1L));
            DataFiles.write(navigation.resolve(registered.identity()+".json"),index);
            var failure=assertThrows(IOException.class,()->BrnResearchData.prepare(source,output,2,2,5000,navigation,true));
            assertTrue(failure.getMessage().contains(identity.equals(registered.identity())?"does not cover":"identity mismatch"));
            assertFalse(Files.exists(output.getParent()));
        }
    }
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
