package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnpair2.TiedGeometryPieceNetwork.*;
import static com.ohinteractive.seedv6.core.brnpair2.TiedGeometryPieceNetworkTest.bytes;
import static com.ohinteractive.seedv6.core.brnpair2.TiedGeometryPieceNetworkTest.read;

class TiedGeometryTrainingDiagnosticTest {
    @TempDir Path directory;
    @Test void modeAndSecondPathAblationPreserveAllParentWeights()throws Exception {
        assertFalse(TiedGeometryTrainingDiagnostic.compose("ONE"));assertTrue(TiedGeometryTrainingDiagnostic.compose("TIED"));assertThrows(IllegalArgumentException.class,()->TiedGeometryTrainingDiagnostic.compose("CONTEXT"));
        var model=new TiedGeometryPieceNetwork(true,.01,71);for(int p=V;p<PARAMETERS;p++)model.weights[p]=.3;model.invalidateProjections();byte[] saved=bytes(model);
        var masked=TiedGeometryTrainingDiagnostic.ablateComposition(model);var zeroed=read(saved);for(int p=V;p<PARAMETERS;p++)zeroed.weights[p]=0;zeroed.invalidateProjections();
        long[][] boards={Board.startingPosition(),Board.fromFen("q6k/8/8/8/P7/8/8/R6K w - - 0 1")};
        for(var board:boards)assertEquals(zeroed.scoreCp(board,new Workspace()),masked.scoreCp(board,new Workspace()),1e-12);
        assertArrayEquals(saved,bytes(model));assertArrayEquals(model.weights,masked.weights);assertFalse(masked.compose);
    }
    @Test void atomicTiedStatePreservesHistoryAndRejectsWrongInterpretation()throws Exception {
        var model=new TiedGeometryPieceNetwork(true,.01,71);TiedGeometryPieceDiagnostic.segment(model,ContextualPieceFineTuneTest.data(),71,1,0,257);Path path=directory.resolve("state");TiedGeometryTrainingDiagnostic.publishState(path,model);
        assertArrayEquals(bytes(model),bytes(TiedGeometryTrainingDiagnostic.checkpoint(path,true,.01,3)));
        assertThrows(IllegalArgumentException.class,()->TiedGeometryTrainingDiagnostic.checkpoint(path,false,.01,3));assertThrows(IllegalArgumentException.class,()->TiedGeometryTrainingDiagnostic.checkpoint(path,true,.001,3));assertThrows(IllegalArgumentException.class,()->TiedGeometryTrainingDiagnostic.checkpoint(path,true,.01,4));
        assertThrows(java.io.IOException.class,()->TiedGeometryTrainingDiagnostic.publishState(path,model));assertArrayEquals(bytes(model),Files.readAllBytes(path));
    }
}
