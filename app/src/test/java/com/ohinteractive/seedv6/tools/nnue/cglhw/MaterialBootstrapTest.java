package com.ohinteractive.seedv6.tools.nnue.cglhw;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.*;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;
import com.ohinteractive.seedv6.training.selfplay.HeadlessGame;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MaterialBootstrapTest {
    private static int oracle(long[] board) {
        // Independent full board reference through the audited BRN3 decimal values.
        return Brn3Model.score(BootstrapAudit.materialOracle(board));
    }
    @Test void exactPieceValuesBothColoursPerspectiveAndPlacementIndependence() {
        for (String piece : new String[]{"P", "N", "B", "R", "Q"}) {
            int value = switch (piece) { case "P" -> 100; case "N" -> 320; case "B" -> 330; case "R" -> 500; default -> 900; };
            for (boolean black : new boolean[]{false, true}) for (String stm : new String[]{"w", "b"}) {
                for (String rank : new String[]{(black ? piece.toLowerCase() : piece)+"7", "7"+(black ? piece.toLowerCase() : piece)}) {
                    var board = Board.fromFen("4k3/8/8/8/"+rank+"/8/8/4K3 "+stm+" - - 0 1");
                    int white = black ? -value : value;
                    assertEquals(white,NnueMaterialBootstrap.whiteScore(board));
                    int signed=NnueMaterialBootstrap.forSideToMove(board,white);
                    assertEquals(stm.equals("w")?white:-white,signed);
                    assertEquals(oracle(board),signed);
                    assertEquals(Brn3Model.score(Brn3Features.material(board)),signed);
                }
            }
        }
        assertEquals(0,NnueMaterialBootstrap.whiteScore(Board.startingPosition()));
        assertEquals(0,NnueMaterialBootstrap.whiteScore(Board.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1")));
        assertEquals(2150,oracle(Board.fromFen("4k3/8/8/8/8/8/PNBRQ3/4K3 w - - 0 1")));
    }

    private static void edge(long[] p,long[] c) {
        int original=NnueMaterialBootstrap.whiteScore(p),next=NnueMaterialBootstrap.update(p,c,original);
        assertEquals(NnueMaterialBootstrap.whiteScore(c),next);
        assertEquals(oracle(c),NnueMaterialBootstrap.forSideToMove(c,next));
        assertEquals(original,NnueMaterialBootstrap.update(c,p,next));
    }
    @Test void updatesMatchReferenceForChainsBranchesNullsCapturesCastlingEpAndEveryPromotion() {
        for(var e:BootstrapAudit.corpus(4,160)) { edge(e.parent(),e.child());var nullBoard=e.parent().clone();nullBoard[4]^=1;edge(e.parent(),nullBoard); }
        for(String fen:new String[]{"r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
                "1r2k3/P7/8/8/8/8/7p/4K1R1 w - - 0 1","4k1r1/7P/8/8/8/8/p7/1R2K3 b - - 0 1"}) {
            var p=Board.fromFen(fen);
            for(long move:new HeadlessGame(p,100).legalMoves()) {
                var c=new long[6];Board.makeMoveInto(p[0],p[1],p[2],p[3],(int)p[4],p[5],move,c);edge(p,c);
            }
        }
        var game=new HeadlessGame(Board.startingPosition(),512);var random=new java.util.SplittableRandom(2026100508);
        int material=0;
        while(game.active()) {
            var p=game.boardSnapshot();var moves=game.legalMoves();game.play(moves[random.nextInt(moves.length)]);var c=game.boardSnapshot();
            material=NnueMaterialBootstrap.update(p,c,material);assertEquals(NnueMaterialBootstrap.whiteScore(c),material);
        }
    }

    @Test void productionIncrementalFullRefreshWrapperAndMaterialOffAgree() {
        for(long seed:new long[]{41001,41002}) {
            var network=NnueNetwork.initialized(seed);
            var definition=SearchEvaluation.incrementalWithMaterial(network,NnueScoreMapping.V1);
            var inc=definition.newState(3);var copy=definition.newState(3);
            var full=SearchEvaluation.fullRecomputeWithMaterial(network,NnueScoreMapping.V1).newState(3);
            var off=SearchEvaluation.incremental(network).newState(3);
            var wrapper=new MaterialBootstrapModel(new HalfKpHead(seed,false,false)).worker(3);
            for(var e:BootstrapAudit.corpus(2,64)) {
                inc.initialize(e.parent(),0);off.initialize(e.parent(),0);wrapper.refresh(e.parent(),0);
                int parentScore=inc.evaluate(e.parent(),0);
                inc.child(e.parent(),e.child(),0);off.child(e.parent(),e.child(),0);wrapper.transition(e.parent(),e.child(),0,1);
                assertEquals(full.evaluate(e.child(),1),inc.evaluate(e.child(),1));
                assertEquals(wrapper.evaluate(e.child(),1),inc.evaluate(e.child(),1));
                assertEquals(oracle(e.child())+off.evaluate(e.child(),1),inc.evaluate(e.child(),1));
                copy.initializeFrom(e.child(),1,inc);assertEquals(inc.evaluate(e.child(),1),copy.evaluate(e.child(),1));
                assertEquals(parentScore,inc.evaluate(e.parent(),0));
            }
        }
    }

    @Test void zeroHeadParityAndFinalClipping() {
        for(long seed:new long[]{41001,41002}) {
            var w=new MaterialBootstrapModel(new HalfKpHead(seed,false,false,true)).worker(2);
            var brn=SearchEvaluation.brn3(new Brn3Trainer(seed).snapshot()).newState(2);
            for(var e:BootstrapAudit.corpus(2,64)) {
                w.refresh(e.parent(),0);w.transition(e.parent(),e.child(),0,1);
                assertEquals(brn.evaluate(e.parent(),0),w.evaluate(e.parent(),0));
                assertEquals(brn.evaluate(e.child(),1),w.evaluate(e.child(),1));
            }
        }
        int max=TranspositionScores.MAX_NORMAL_SCORE;
        assertEquals(max,NnueMaterialBootstrap.combine(900,max));
        assertEquals(-max,NnueMaterialBootstrap.combine(-900,-max));
        assertEquals(100,NnueMaterialBootstrap.combine(900,-800));
        assertEquals(max,NnueMaterialBootstrap.combine(Integer.MAX_VALUE,Integer.MAX_VALUE));
    }
}
