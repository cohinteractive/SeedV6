package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnchorCoverageDiagnosticTest {
    @Test void streamingEncodingMatchesFrozenR012ArithmeticIncludingTailAndResume()throws Exception {
        int count=257;var flat=new PairCoverageDiagnostic.Data(count);var encoded=new AnchorNeuralNetwork.Features[count];var w=new AnchorNeuralNetwork.Workspace();
        long[] board=Board.startingPosition(),moves=new long[256],scratch=new long[6];var random=new Random(71);
        for(int i=0;i<count;i++) {
            System.arraycopy(board,0,flat.boards,i*6,6);flat.targets[i]=random.nextDouble();encoded[i]=w.encode(board).frozen();
            int n=Gen.genAll(board[0],board[1],board[2],board[3],(int)board[4],board[5],true,moves,scratch);
            if(n==0)board=Board.startingPosition();else{var next=new long[6];Board.makeMoveInto(board[0],board[1],board[2],board[3],(int)board[4],board[5],moves[random.nextInt(n)],next);board=next;}
        }
        var original=new AnchorNeuralNetwork(true,.01,71);var streaming=new AnchorNeuralNetwork(true,.01,71);
        for(int epoch=1;epoch<=2;epoch++) {
            // Preserve R012's original preencoded loop as the arithmetic reference.
            int[] order=new int[count];for(int i=0;i<count;i++)order[i]=i;var rng=new Random(71+epoch);
            for(int i=count-1;i>0;i--){int j=rng.nextInt(i+1),v=order[i];order[i]=order[j];order[j]=v;}
            for(int offset=0;offset<count;offset+=128) {
                int n=Math.min(128,count-offset);var batch=new AnchorNeuralNetwork.Features[n];double[] targets=new double[n];
                for(int j=0;j<n;j++){int i=order[offset+j];batch[j]=encoded[i];targets[j]=flat.targets[i];}original.train(batch,targets);
            }
            AnchorCoverageDiagnostic.epoch(streaming,flat,71,epoch);
            var a=new ByteArrayOutputStream();var b=new ByteArrayOutputStream();original.write(a);streaming.write(b);assertArrayEquals(a.toByteArray(),b.toByteArray());
            var losses=AnchorCoverageDiagnostic.mse(streaming,flat);assertEquals(AnchorDiagnostic.mse(original,encoded,flat.targets,false),losses[0],0);
            assertEquals(AnchorDiagnostic.mse(original,encoded,flat.targets,true),losses[1],0);streaming=AnchorNeuralNetwork.read(new ByteArrayInputStream(b.toByteArray()));
        }
    }
}
