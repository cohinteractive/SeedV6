package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class FieldPredictionAuditTest {
    @TempDir Path directory;
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
