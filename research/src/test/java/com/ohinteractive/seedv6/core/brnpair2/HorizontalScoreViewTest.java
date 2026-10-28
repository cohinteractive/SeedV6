package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.nnue.conventional.ConventionalFeatures;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HorizontalScoreViewTest {
    @Test void reflectionIsInvolutionAndCanonicalViewPreservesNeuralColorSymmetryAndCaller() {
        var boards=new ArrayList<long[]>();long[] b=Board.startingPosition(),moves=new long[256],scratch=new long[6];var random=new Random(71);
        for(int i=0;i<80;i++){boards.add(b);int n=Gen.genAll(b[0],b[1],b[2],b[3],(int)b[4],b[5],true,moves,scratch);if(n==0)break;var next=new long[6];Board.makeMoveInto(b[0],b[1],b[2],b[3],(int)b[4],b[5],moves[random.nextInt(n)],next);b=next;}
        for(String fen:List.of("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 b - - 0 1"))boards.add(Board.fromFen(fen));
        var model=new AttackFieldNetwork(true,.01,71);var w=new AttackFieldNetwork.Workspace();
        for(var board:boards) {
            var saved=board.clone();long[] mirror=new long[6],twice=new long[6],a=new long[6],c=new long[6];HorizontalScoreView.reflect(board,mirror);HorizontalScoreView.reflect(mirror,twice);assertArrayEquals(board,twice);
            HorizontalScoreView.canonical(board,a);HorizontalScoreView.canonical(mirror,c);assertArrayEquals(a,c);assertEquals(model.scoreCp(a,w),model.scoreCp(c,w));
            var color=board.clone();long occupied=board[0]|board[1]|board[2];for(int i=0;i<3;i++)color[i]=Long.reverseBytes(board[i]);color[3]=Long.reverseBytes(occupied^board[3]);color[4]^=1;HorizontalScoreView.canonical(color,c);assertEquals(model.scoreCp(a,w),model.scoreCp(c,w),1e-9);assertArrayEquals(saved,board);
            int[] kings={-1,-1};for(int s=0;s<64;s++){int piece=Board.getSquare(board[0],board[1],board[2],board[3],s);if((piece&7)==1)kings[piece>>>3]=s;}
            for(int s=0;s<64;s++) {
                int piece=Board.getSquare(board[0],board[1],board[2],board[3],s);assertEquals(piece,Board.getSquare(mirror[0],mirror[1],mirror[2],mirror[3],s^7));if(piece==0)continue;
                for(int side=0;side<2;side++)assertEquals(ConventionalFeatures.index(side,kings[side],piece,s),ConventionalFeatures.index(side,kings[side]^7,piece,s^7));
            }
        }
        assertThrows(IllegalArgumentException.class,()->HorizontalScoreView.canonical(new long[6],new long[6]));long[] same=Board.startingPosition();assertThrows(IllegalArgumentException.class,()->HorizontalScoreView.reflect(same,same));
    }
}
