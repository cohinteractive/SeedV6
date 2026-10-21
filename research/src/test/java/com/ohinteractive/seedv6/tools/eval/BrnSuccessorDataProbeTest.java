package com.ohinteractive.seedv6.tools.eval;

import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BrnSuccessorDataProbeTest {
    @TempDir Path temporary;
    @Test void arbitraryModelNameWorksAndRawBudgetPreservesPartialEvidence() throws Exception {
        Path root=temporary.resolve("model"); String id;
        try(var store=new CheckpointStore(root,TrainingArchitecture.BRN_CUBIC)) {
            id=store.initialize(NetworkTrainingState.initialized(TrainingArchitecture.BRN_CUBIC,1,.001),
                    new CheckpointManifest.Metadata(0,4,"")).manifest().id();
        }
        Path models=temporary.resolve("models.json");
        DataFiles.write(models,Map.of("id","fixture","models",List.of(Map.of("name","arbitrary","root",root.toString(),"checkpoint",id))));
        Path data=temporary.resolve("data.jsonl");
        Files.writeString(data,("{\"fen\":\"4k3/8/8/8/3pP3/8/8/4K3 w - - 0 1\",\"evals\":[{\"depth\":20,\"pvs\":[{\"cp\":20}]}]}\n").repeat(4));
        var source=DataSource.register("fixture",data,1);
        Path selection=temporary.resolve("sources.json");
        DataFiles.write(selection,new DataSources(1,List.of(source),false));
        var request=new BrnSuccessorDataProbe.Source("fixture",selection.toString(),source.identity(),null,0,1,false);
        Path plan=temporary.resolve("plan.json"), output=temporary.resolve("complete");
        DataFiles.write(plan,new BrnSuccessorDataProbe.Plan("fixture",models.toString(),2,4L,List.of(request)));
        BrnSuccessorDataProbe.main(new String[]{plan.toString(),output.toString()});
        assertEquals(2,Files.readAllLines(output.resolve("fixture-positions.jsonl")).size());
        assertTrue(Files.readString(output.resolve("summary.json")).contains("arbitrary"));
        DataFiles.write(plan,new BrnSuccessorDataProbe.Plan("bounded",models.toString(),2,1L,List.of(request)));
        Path partial=temporary.resolve("partial");
        assertTrue(assertThrows(IllegalStateException.class,()->BrnSuccessorDataProbe.main(new String[]{plan.toString(),partial.toString()}))
                .getMessage().contains("Raw record budget exceeded"));
        assertEquals(1,Files.readAllLines(partial.resolve("fixture-positions.jsonl")).size());
        assertFalse(Files.exists(partial.resolve("summary.json")));
        assertEquals(id,CheckpointStore.readSnapshot(root,id).manifest().id());
    }
}
