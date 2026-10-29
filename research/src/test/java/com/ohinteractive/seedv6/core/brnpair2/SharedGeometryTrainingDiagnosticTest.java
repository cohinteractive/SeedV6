package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnpair2.SharedGeometryPieceNetwork.*;
import static com.ohinteractive.seedv6.core.brnpair2.SharedGeometryPieceNetworkTest.bytes;
import static com.ohinteractive.seedv6.core.brnpair2.SharedGeometryPieceNetworkTest.read;

class SharedGeometryTrainingDiagnosticTest {
    @TempDir Path directory;
    @Test void modeAndContextAblationAreExplicitAndDoNotMutateTheParent()throws Exception {
        assertFalse(SharedGeometryTrainingDiagnostic.context("GEOMETRY"));assertTrue(SharedGeometryTrainingDiagnostic.context("CONTEXT"));assertThrows(IllegalArgumentException.class,()->SharedGeometryTrainingDiagnostic.context("ONE"));
        var model=new SharedGeometryPieceNetwork(true,.01,71);for(int p=A;p<PARAMETERS;p++)if((p-A)%12>=10)model.weights[p]=.3;model.invalidateProjections();byte[] saved=bytes(model);
        var masked=SharedGeometryTrainingDiagnostic.ablateContext(model);var zeroed=read(saved);for(int p=A;p<PARAMETERS;p++)if((p-A)%12>=10)zeroed.weights[p]=0;zeroed.invalidateProjections();
        long[][] boards={Board.startingPosition(),Board.fromFen("q6k/8/8/8/P7/8/8/R6K w - - 0 1")};
        for(var board:boards)assertEquals(zeroed.scoreCp(board,new Workspace()),masked.scoreCp(board,new Workspace()),1e-12);
        assertArrayEquals(saved,bytes(model));assertArrayEquals(model.weights,masked.weights);assertFalse(masked.context);
    }
    @Test void atomicCheckpointRetainsExactHistoryAndRejectsWrongModeRateOrStep()throws Exception {
        var model=new SharedGeometryPieceNetwork(true,.01,71);SharedGeometryPieceDiagnostic.segment(model,ContextualPieceFineTuneTest.data(),71,1,0,257);Path path=directory.resolve("state");SharedGeometryTrainingDiagnostic.publishState(path,model);
        assertArrayEquals(bytes(model),bytes(SharedGeometryTrainingDiagnostic.checkpoint(path,true,.01,3)));
        assertThrows(IllegalArgumentException.class,()->SharedGeometryTrainingDiagnostic.checkpoint(path,false,.01,3));assertThrows(IllegalArgumentException.class,()->SharedGeometryTrainingDiagnostic.checkpoint(path,true,.001,3));assertThrows(IllegalArgumentException.class,()->SharedGeometryTrainingDiagnostic.checkpoint(path,true,.01,4));
        assertThrows(java.io.IOException.class,()->SharedGeometryTrainingDiagnostic.publishState(path,model));assertArrayEquals(bytes(model),Files.readAllBytes(path));
    }
}
