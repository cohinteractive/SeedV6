package com.ohinteractive.seedv6.tools.search;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;
import java.io.*;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BrnPair2BenchmarkTest {
    @TempDir Path root;
    @Test void benchmarkLoadsBestOrExactGenerationAndReportsFamilyAndHash()throws Exception {
        var trainer=(NetworkTrainingState.BrnPair2)NetworkTrainingState.initialized(TrainingArchitecture.BRN_PAIR2,1,.01);
        String selected;
        try(var store=new CheckpointStore(root,TrainingArchitecture.BRN_PAIR2)) {
            String best=store.initialize(trainer,new CheckpointManifest.Metadata(0,1,"")).manifest().id();
            trainer.trainer().trainBatch(new long[][]{Board.startingPosition()},new double[]{.5},1);
            selected=store.publish(trainer,new CheckpointManifest.Metadata(1,1,best)).manifest().id();
        }
        for(String id:new String[]{"",selected}) {
            var bytes=new ByteArrayOutputStream();
            ExactSearchHarness.run(new String[]{"--model-store="+root,"--checkpoint="+id,"--position=start",
                    "--depth=2","--warmups=1","--repetitions=2","--tt=on"},new PrintStream(bytes));
            String output=bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(output.contains("evaluator=BRN_PAIR2"));assertTrue(output.contains("sha256="));
            assertTrue(output.contains("completed=2"));if(!id.isEmpty())assertTrue(output.contains(id));
        }
        assertThrows(IllegalArgumentException.class,()->ExactSearchHarness.run(new String[]{"--checkpoint="+selected},System.out));
    }
}
