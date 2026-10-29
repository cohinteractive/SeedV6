package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnpair2.MixedTiedPieceNetwork.*;
import static com.ohinteractive.seedv6.core.brnpair2.MixedTiedPieceNetworkTest.bytes;
import static com.ohinteractive.seedv6.core.brnpair2.MixedTiedPieceNetworkTest.read;

class MixedTiedTrainingDiagnosticTest {
    @TempDir Path directory;
    @Test void modeAndMixingAblationPreserveAllParentWeights()throws Exception {
        assertFalse(MixedTiedTrainingDiagnostic.mixing("CONTROL"));assertTrue(MixedTiedTrainingDiagnostic.mixing("MIX"));assertThrows(IllegalArgumentException.class,()->MixedTiedTrainingDiagnostic.mixing("TIED"));
        var model=new MixedTiedPieceNetwork(true,.01,71);for(int p=V;p<PARAMETERS;p++)model.weights[p]=.3;model.invalidateProjections();byte[] saved=bytes(model);
        var masked=MixedTiedTrainingDiagnostic.ablateMix(model);var zeroed=read(saved);for(int p=M;p<PARAMETERS;p++)zeroed.weights[p]=0;zeroed.invalidateProjections();
        long[][] boards={Board.startingPosition(),Board.fromFen("q6k/8/8/8/P7/8/8/R6K w - - 0 1")};
        for(var board:boards)assertEquals(zeroed.scoreCp(board,new Workspace()),masked.scoreCp(board,new Workspace()),1e-12);
        assertArrayEquals(saved,bytes(model));assertArrayEquals(model.weights,masked.weights);assertFalse(masked.mix);
    }
    @Test void atomicTiedStatePreservesHistoryAndRejectsWrongInterpretation()throws Exception {
        var model=new MixedTiedPieceNetwork(true,.01,71);MixedTiedPieceDiagnostic.segment(model,ContextualPieceFineTuneTest.data(),71,1,0,257);Path path=directory.resolve("state");MixedTiedTrainingDiagnostic.publishState(path,model);
        assertArrayEquals(bytes(model),bytes(MixedTiedTrainingDiagnostic.checkpoint(path,true,.01,3)));
        assertThrows(IllegalArgumentException.class,()->MixedTiedTrainingDiagnostic.checkpoint(path,false,.01,3));assertThrows(IllegalArgumentException.class,()->MixedTiedTrainingDiagnostic.checkpoint(path,true,.001,3));assertThrows(IllegalArgumentException.class,()->MixedTiedTrainingDiagnostic.checkpoint(path,true,.01,4));
        assertThrows(java.io.IOException.class,()->MixedTiedTrainingDiagnostic.publishState(path,model));assertArrayEquals(bytes(model),Files.readAllBytes(path));
    }
}
