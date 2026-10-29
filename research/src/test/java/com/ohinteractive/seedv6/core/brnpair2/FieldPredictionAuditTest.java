package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class FieldPredictionAuditTest {
    @TempDir Path directory;
    @Test void mixedRouteMasksOnlyCrossChannelTransformAndRejectsWrongModeOrFormat()throws Exception {
        var model=new MixedTiedPieceNetwork(true,.001,71);
        java.util.Arrays.fill(model.weights,MixedTiedPieceNetwork.V,MixedTiedPieceNetwork.PARAMETERS,.3);model.invalidateProjections();
        Path state=directory.resolve("mixed.state");try(var out=Files.newOutputStream(state)){model.write(out);}
        var full=FieldPredictionAudit.load(state,"MIX_TIED");var masked=FieldPredictionAudit.load(state,"MIX_TIED_MASK");
        var zeroed=new MixedTiedPieceNetwork(true,.001,0);System.arraycopy(model.weights,0,zeroed.weights,0,model.weights.length);java.util.Arrays.fill(zeroed.weights,MixedTiedPieceNetwork.M,MixedTiedPieceNetwork.PARAMETERS,0);
        boolean secondSurvives=false;
        for(String fen:new String[]{"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/P7/8/8/8/8/7p/4K3 b - - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"}) {
            var b=Board.fromFen(fen);var saved=b.clone();var w=new MixedTiedPieceNetwork.Workspace();
            assertEquals(model.scoreCp(b,new MixedTiedPieceNetwork.Workspace())/400,full.applyAsDouble(b),1e-12);
            double expected=zeroed.compiledLogit(w.encode(b),w);assertEquals(expected,masked.applyAsDouble(b),1e-12);secondSurvives|=Math.abs(expected-w.baseLogit)>1e-8;assertArrayEquals(saved,b);
        }
        assertTrue(secondSurvives);
        Path wrong=directory.resolve("mixed-control.state");try(var out=Files.newOutputStream(wrong)){new MixedTiedPieceNetwork(false,.001,71).write(out);}
        assertThrows(IllegalArgumentException.class,()->FieldPredictionAudit.load(wrong,"MIX_TIED"));assertThrows(IllegalArgumentException.class,()->FieldPredictionAudit.load(wrong,"MIX_TIED_MASK"));assertThrows(java.io.IOException.class,()->FieldPredictionAudit.load(state,"TIED"));
        Path old=directory.resolve("tied-format.state");try(var out=Files.newOutputStream(old)){new TiedGeometryPieceNetwork(true,.001,71).write(out);}assertThrows(java.io.IOException.class,()->FieldPredictionAudit.load(old,"MIX_TIED"));
    }
    @Test void sharedTiedAndMaskedRoutesPreserveExplicitModelSemantics()throws Exception {
        var shared=new SharedGeometryPieceNetwork(true,.01,71);var tied=new TiedGeometryPieceNetwork(true,.01,71);
        java.util.Arrays.fill(tied.weights,TiedGeometryPieceNetwork.V,TiedGeometryPieceNetwork.PARAMETERS,.3);tied.invalidateProjections();
        Path s=directory.resolve("shared.state"),t=directory.resolve("tied.state");try(var out=Files.newOutputStream(s)){shared.write(out);}try(var out=Files.newOutputStream(t)){tied.write(out);}
        var ps=FieldPredictionAudit.load(s,"SHARED_CONTEXT");var pt=FieldPredictionAudit.load(t,"TIED");var pm=FieldPredictionAudit.load(t,"TIED_MASK");var masked=TiedGeometryTrainingDiagnostic.ablateComposition(tied);
        for(String fen:new String[]{"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/P7/8/8/8/8/7p/4K3 b - - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"}) {
            var board=Board.fromFen(fen);var saved=board.clone();assertEquals(shared.scoreCp(board,new SharedGeometryPieceNetwork.Workspace())/400,ps.applyAsDouble(board),1e-12);
            assertEquals(tied.scoreCp(board,new TiedGeometryPieceNetwork.Workspace())/400,pt.applyAsDouble(board),1e-12);assertEquals(masked.scoreCp(board,new TiedGeometryPieceNetwork.Workspace())/400,pm.applyAsDouble(board),1e-12);assertArrayEquals(saved,board);
        }
        Path wrongS=directory.resolve("geometry.state"),wrongT=directory.resolve("one.state");try(var out=Files.newOutputStream(wrongS)){new SharedGeometryPieceNetwork(false,.01,71).write(out);}try(var out=Files.newOutputStream(wrongT)){new TiedGeometryPieceNetwork(false,.01,71).write(out);}
        assertThrows(IllegalArgumentException.class,()->FieldPredictionAudit.load(wrongS,"SHARED_CONTEXT"));assertThrows(IllegalArgumentException.class,()->FieldPredictionAudit.load(wrongT,"TIED"));assertThrows(IllegalArgumentException.class,()->FieldPredictionAudit.load(wrongT,"TIED_MASK"));assertThrows(java.io.IOException.class,()->FieldPredictionAudit.load(t,"SHARED_CONTEXT"));assertThrows(java.io.IOException.class,()->FieldPredictionAudit.load(s,"TIED"));
    }
    @Test void contextualPieceModesPreserveScoresAndRejectWrongMode()throws Exception {
        for(boolean compose:new boolean[]{false,true}) {
            var model=new ContextualPieceNetwork(compose,.01,71);java.util.Arrays.fill(model.weights,ContextualPieceNetwork.V,ContextualPieceNetwork.PARAMETERS,.3);
            Path state=directory.resolve("piece-"+compose+".state");try(var out=Files.newOutputStream(state)){model.write(out);}
            var predictor=FieldPredictionAudit.load(state,compose?"PIECE_TWO":"PIECE_ONE");var w=new ContextualPieceNetwork.Workspace();
            for(String fen:new String[]{"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","4k3/P7/8/8/8/8/7p/4K3 b - - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"}){var b=Board.fromFen(fen);assertEquals(model.logit(w.encode(b),w),predictor.applyAsDouble(b));}
            assertThrows(IllegalArgumentException.class,()->FieldPredictionAudit.load(state,compose?"PIECE_ONE":"PIECE_TWO"));
            assertThrows(java.io.IOException.class,()->FieldPredictionAudit.load(state,"COMPOSE8"));
        }
    }
    @Test void modelRoutingPreservesScoresAndRejectsGeometryMismatch()throws Exception {
        var all=new AttackFieldNetwork(true,.01,71);var compose=new ComposedAttackFieldNetwork(true,.01,71);var wide=new WideComposedAttackFieldNetwork(true,.01,71);
        Path a=directory.resolve("all.state"),c=directory.resolve("compose.state"),w=directory.resolve("wide.state");
        try(var out=Files.newOutputStream(a)){all.write(out);}try(var out=Files.newOutputStream(c)){compose.write(out);}try(var out=Files.newOutputStream(w)){wide.write(out);}
        var pa=FieldPredictionAudit.load(a,"ALL");var pc=FieldPredictionAudit.load(c,"COMPOSE8");var pw=FieldPredictionAudit.load(w,"COMPOSE16");
        for(String fen:new String[]{"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","4k3/P7/8/8/8/8/7p/4K3 b - - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"}) {
            long[] b=Board.fromFen(fen);var aw=new AttackFieldNetwork.Workspace();var cw=new ComposedAttackFieldNetwork.Workspace();var ww=new WideComposedAttackFieldNetwork.Workspace();
            assertEquals(all.logit(aw.encode(b,true),aw),pa.applyAsDouble(b));assertEquals(compose.logit(cw.encode(b,true),cw),pc.applyAsDouble(b));assertEquals(wide.logit(ww.encode(b,true),ww),pw.applyAsDouble(b));
        }
        assertThrows(java.io.IOException.class,()->FieldPredictionAudit.load(c,"COMPOSE16"));assertThrows(IllegalArgumentException.class,()->FieldPredictionAudit.load(a,"UNKNOWN"));
    }
}
