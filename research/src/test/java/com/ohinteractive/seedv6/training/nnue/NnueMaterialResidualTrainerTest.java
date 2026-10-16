package com.ohinteractive.seedv6.training.nnue;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import com.ohinteractive.seedv6.core.brn3.*;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.tools.nnue.cglhw.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

class NnueMaterialResidualTrainerTest {
    @TempDir Path temporary;
    @Test void rawChainRuleMatchesFiniteDifferencesThroughMaterialCe() {
        var t=new NnueMaterialResidualTrainer(51201,new BrnAdamConfig(.003));
        // Nonzero head makes deeper feature gradients observable; no learned fixture.
        java.util.Arrays.fill(t.parameters.groups[Parameters.OUTPUT],.2f);
        var board=Board.startingPosition();double target=.5;
        double total=t.predictPawns(board);
        t.gradients.reset();t.scratch.backwardRaw(t.parameters,t.gradients,
                Brn3Objective.crossEntropy(total,NnueCorpusTargets.material(board),target)[1]);
        int checked=0;
        for(int g=0;g<6;g++) {
            int groupChecked=0;
            for(int i=0;i<t.parameters.groups[g].length&&groupChecked<3;i++) {
                double analytic=t.gradients.values.groups[g][i];if(Math.abs(analytic)<1e-7)continue;
                float old=t.parameters.groups[g][i],epsilon=1e-4f;
                t.parameters.groups[g][i]=old+epsilon;
                double hi=Brn3Objective.crossEntropy(t.predictPawns(board),NnueCorpusTargets.material(board),target)[0];
                t.parameters.groups[g][i]=old-epsilon;
                double lo=Brn3Objective.crossEntropy(t.predictPawns(board),NnueCorpusTargets.material(board),target)[0];
                t.parameters.groups[g][i]=old;
                double numerical=(hi-lo)/(2*epsilon);
                assertEquals(numerical,analytic,Math.max(3e-5,Math.abs(analytic)*.01),"group="+g+" index="+i);
                groupChecked++;checked++;
            }
            assertTrue(groupChecked>0,"Unexercised gradient group "+g);
        }
        assertTrue(checked>=10);
    }

    @Test void bootstrapPersistsAfterTrainingAndCheckpointResumeIsExact()throws Exception {
        var t=new NnueMaterialResidualTrainer(51201,new BrnAdamConfig(.003));
        var board=Board.startingPosition();var boards=new long[][]{board};var targets=new double[]{.5};
        assertEquals(0,t.step());assertEquals(0,t.predictPawns(board),0);
        double initial=Brn3Objective.crossEntropy(t.predictPawns(board),NnueCorpusTargets.material(board),.5)[0];
        for(int i=0;i<12;i++)t.trainBatch(boards,targets,1);
        assertTrue(Brn3Objective.crossEntropy(t.predictPawns(board),NnueCorpusTargets.material(board),.5)[0]<initial);
        var path=temporary.resolve("residual.state");
        try(var out=new BufferedOutputStream(Files.newOutputStream(path))){t.write(out);}
        NnueMaterialResidualTrainer resumed;
        try(var in=new BufferedInputStream(Files.newInputStream(path))){resumed=NnueMaterialResidualTrainer.read(in);}
        try(var in=Files.newInputStream(path)){assertThrows(IOException.class,()->TrainingStateCodec.read(in));}
        assertEquals(12,resumed.step());assertEquals(t.predictPawns(board),resumed.predictPawns(board),0);
        t.trainBatch(boards,targets,1);resumed.trainBatch(boards,targets,1);
        assertArrayEquals(NnueNetworkCodec.encode(t.snapshot()),NnueNetworkCodec.encode(resumed.snapshot()));
        var worker=new HalfKpResidualModel(t.snapshot()).worker(3);
        var evaluator=new NnueEvaluator(t.snapshot());
        for(var e:BootstrapAudit.corpus(1,48)) {
            worker.refresh(e.parent(),0);worker.transition(e.parent(),e.child(),0,1);worker.refresh(e.child(),2);
            assertEquals(worker.raw(e.child(),2),worker.raw(e.child(),1),1e-6);
            double material=NnueMaterialBootstrap.forSideToMove(e.child(),NnueMaterialBootstrap.whiteScore(e.child()))/100.0;
            assertEquals(material+evaluator.evaluate(e.child()),t.predictPawns(e.child()),1e-7);
            assertEquals(Brn3Model.score(material+.25*evaluator.evaluate(e.child())),worker.evaluate(e.child(),1));
        }
        try(var file=new RandomAccessFile(path.toFile(),"rw")){file.seek(Files.size(path)-1);int x=file.read();file.seek(Files.size(path)-1);file.write(x^1);}
        try(var in=new BufferedInputStream(Files.newInputStream(path))){assertThrows(IOException.class,()->NnueMaterialResidualTrainer.read(in));}
        assertThrows(IllegalArgumentException.class,()->t.trainBatch(boards,new double[]{Double.NaN},1));assertEquals(13,t.step());
    }
}
