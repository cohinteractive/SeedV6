package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnpair2.ContextGatedPieceNetwork.*;
import static com.ohinteractive.seedv6.core.brnpair2.ContextGatedPieceNetworkTest.bytes;
import static com.ohinteractive.seedv6.core.brnpair2.ContextGatedPieceNetworkTest.read;

class ContextGatedTrainingDiagnosticTest {
    @TempDir Path directory;
    @Test void modeAndContextAblationPreserveAllParentWeights()throws Exception {
        assertFalse(ContextGatedTrainingDiagnostic.gating("CONTROL"));assertTrue(ContextGatedTrainingDiagnostic.gating("GATED"));assertThrows(IllegalArgumentException.class,()->ContextGatedTrainingDiagnostic.gating("TIED"));
        var model=new ContextGatedPieceNetwork(true,.01,71);for(int p=V;p<PARAMETERS;p++)model.weights[p]=.3;model.invalidateProjections();byte[] saved=bytes(model);
        var masked=ContextGatedTrainingDiagnostic.ablateGate(model);var zeroed=read(saved);for(int p=C;p<PARAMETERS;p++)zeroed.weights[p]=0;zeroed.invalidateProjections();
        long[][] boards={Board.startingPosition(),Board.fromFen("q6k/8/8/8/P7/8/8/R6K w - - 0 1")};
        for(var board:boards)assertEquals(zeroed.scoreCp(board,new Workspace()),masked.scoreCp(board,new Workspace()),1e-12);
        assertArrayEquals(saved,bytes(model));assertArrayEquals(model.weights,masked.weights);assertFalse(masked.gate);
    }
    @Test void atomicTiedStatePreservesHistoryAndRejectsWrongInterpretation()throws Exception {
        var model=new ContextGatedPieceNetwork(true,.01,71);ContextGatedPieceDiagnostic.segment(model,ContextualPieceFineTuneTest.data(),71,1,0,257);Path path=directory.resolve("state");ContextGatedTrainingDiagnostic.publishState(path,model);
        assertArrayEquals(bytes(model),bytes(ContextGatedTrainingDiagnostic.checkpoint(path,true,.01,3)));
        assertThrows(IllegalArgumentException.class,()->ContextGatedTrainingDiagnostic.checkpoint(path,false,.01,3));assertThrows(IllegalArgumentException.class,()->ContextGatedTrainingDiagnostic.checkpoint(path,true,.001,3));assertThrows(IllegalArgumentException.class,()->ContextGatedTrainingDiagnostic.checkpoint(path,true,.01,4));
        assertThrows(java.io.IOException.class,()->ContextGatedTrainingDiagnostic.publishState(path,model));assertArrayEquals(bytes(model),Files.readAllBytes(path));
    }
}
