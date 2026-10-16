package com.ohinteractive.seedv6.search.exact;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.util.Piece;

class SeeClearanceTest {
    @Test void optimizedVacatedRayCheckMatchesIndependentBoardScan() {
        var random=new Random(2026101426L);int compared=0,protectedMoves=0;
        for(int game=0;game<12;game++) {
            var board=Board.startingPosition();
            for(int ply=0;ply<70;ply++) {
                var legal=ExhaustiveOracle.legalMoves(board);
                for(long move:legal) {
                    if(((move>>>Board.TARGET_PIECE_SHIFT)&Board.PIECE_BITS)==0
                            ||((move>>>Board.PROMOTE_PIECE_SHIFT)&Board.PIECE_BITS)!=0)continue;
                    var child=ExhaustiveOracle.child(board,move);
                    boolean expected=opensThreat(board,child,move);
                    assertEquals(expected,SeePolicy.opensSliderThreat(board,child,move));
                    compared++;if(expected)protectedMoves++;
                }
                if(legal.length==0)break;
                board=ExhaustiveOracle.child(board,legal[random.nextInt(legal.length)]);
            }
        }
        assertTrue(compared>1000);assertTrue(protectedMoves>0);
        System.out.println("SR024 clearance comparisons="+compared+" protected="+protectedMoves);
    }

    @Test void clearanceWitnessAndColourMirrorAreRetained() {
        for(var fen:List.of("b1q4k/8/8/2p5/N7/8/8/R3K3 w - - 0 1",
                "r3k3/8/8/n7/2P5/8/8/B1Q4K b - - 0 1")) {
            var board=Board.fromFen(fen);String coordinate=fen.contains(" w ")?"a4c5":"a5c4";
            long move=Arrays.stream(ExhaustiveOracle.legalMoves(board)).filter(m->Move.coordinate(m).equals(coordinate)).findFirst().orElseThrow();
            var child=ExhaustiveOracle.child(board,move);assertTrue(opensThreat(board,child,move));
            assertTrue(SeePolicy.opensSliderThreat(board,child,move));
            int reference=ExactSearch.quiescenceResearch(ExactEvaluator.from(SeePruningTest.EVAL)).search(board,0).score();
            int actual=ExactSearch.quiescencePvsResearch(ExactEvaluator.from(SeePruningTest.EVAL),null,ExactSearch.QSEARCH_BASELINE)
                    .withSeeResearch(SeePolicy.QXRAY,null).search(board,0).score();
            assertEquals(reference,actual);
        }
    }

    // Independent square-by-square rays. No Pext, production helper or SEE calls.
    static boolean opensThreat(long[] parent,long[] child,long move) {
        int player=(int)parent[Board.STATUS]&Board.PLAYER_BIT,to=Move.toSquare(move);
        for(int square=0;square<64;square++) {
            int piece=Board.getSquare(child[0],child[1],child[2],child[3],square),type=piece&Piece.TYPE;
            if(square==to || piece==0 || (piece>>>Board.PLAYER_SHIFT)!=player
                    ||type!=Piece.BISHOP&&type!=Piece.ROOK&&type!=Piece.QUEEN)continue;
            if((targets(child,square,type,player)&~targets(parent,square,type,player))!=0)return true;
        }
        return false;
    }
    static long targets(long[] board,int square,int type,int player) {
        long found=0;
        for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++) {
            if(dx==0&&dy==0 || type==Piece.BISHOP&&(dx==0||dy==0)
                    ||type==Piece.ROOK&&dx!=0&&dy!=0)continue;
            int x=(square&7)+dx,y=(square>>>3)+dy;
            while(x>=0&&x<8&&y>=0&&y<8) {
                int target=y*8+x,piece=Board.getSquare(board[0],board[1],board[2],board[3],target);
                if(piece!=0) {
                    int targetType=piece&Piece.TYPE;
                    if((piece>>>Board.PLAYER_SHIFT)!=player&&targetType!=Piece.PAWN&&targetType!=Piece.KING)found|=1L<<target;
                    break;
                }
                x+=dx;y+=dy;
            }
        }
        return found;
    }
}
