package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PairCoverageDiagnosticTest {
    @TempDir Path temporary;
    @Test void flatDataMatchesOriginalEpochArithmeticOrderAndRestart()throws Exception {
        int count=257;long[][] boards=new long[count][];double[] targets=new double[count];var flat=new PairCoverageDiagnostic.Data(count);long[] board=Board.startingPosition(),moves=new long[256],scratch=new long[6];var random=new Random(71);
        for(int i=0;i<count;i++) {
            boards[i]=board;targets[i]=random.nextDouble();System.arraycopy(board,0,flat.boards,i*6,6);flat.targets[i]=targets[i];
            int n=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,scratch);
            if(n==0)board=Board.startingPosition();else {var next=new long[6];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[random.nextInt(n)],next);board=next;}
        }
        var data=new PairObjectiveDiagnostic.Data(boards,targets);var original=new BrnPair2Trainer(.01);var packed=new BrnPair2Trainer(.01);
        for(int epoch=1;epoch<=2;epoch++) {
            PairObjectiveDiagnostic.epoch(original,data,"aligned",71,epoch);PairCoverageDiagnostic.epoch(packed,flat,71,epoch);
            var a=new ByteArrayOutputStream();var b=new ByteArrayOutputStream();original.write(a);packed.write(b);assertArrayEquals(a.toByteArray(),b.toByteArray());
            assertEquals(PairObjectiveDiagnostic.mse(original,data),PairCoverageDiagnostic.mse(packed,flat),0);
            packed=BrnPair2Trainer.read(new ByteArrayInputStream(b.toByteArray()));
        }
    }
    @Test void streamingLoaderPreservesBoardTargetsCountsAndRejectsCorruption()throws Exception {
        Path file=temporary.resolve("data.tsv");String fen="4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1";Files.writeString(file,fen+"\t0.125\n");
        var data=PairCoverageDiagnostic.Data.read(file,1);long[] board=new long[6];data.board(0,board);assertArrayEquals(Board.fromFen(fen),board);assertEquals(.125,data.targets[0]);
        assertEquals(PairObjectiveDiagnostic.sha(file),PairCoverageDiagnostic.sha(file));assertThrows(IOException.class,()->PairCoverageDiagnostic.Data.read(file,2));
        Files.writeString(file,fen+"\tNaN\n");assertThrows(IOException.class,()->PairCoverageDiagnostic.Data.read(file,1));
        Files.writeString(file,fen+"\t.5\n"+fen+"\t.5\n");assertThrows(IOException.class,()->PairCoverageDiagnostic.Data.read(file,1));
    }
}
