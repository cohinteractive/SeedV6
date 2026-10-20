package com.ohinteractive.seedv6.tools.eval;

import com.ohinteractive.seedv6.core.brnnext.*;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.model.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BrnAverageBindingTest {
    @TempDir Path temporary;
    @Test void ordinaryLoadingStillWorksButWrongHashesAndDuplicateWindowsFailClosed() throws Exception {
        String id,sha;
        try(var store=new CheckpointStore(temporary,TrainingArchitecture.BRN_GEN32_GATED)) {
            var checkpoint=store.initialize(new NetworkTrainingState.BrnNext(new BrnNextTrainer(BrnNextArchitecture.GEN32_GATED,7,.003)),
                    new CheckpointManifest.Metadata(0,4,""));
            id=checkpoint.manifest().id();sha=checkpoint.manifest().networkSha256();
        }
        var map=new HashMap<String,Object>();map.put("name","fixture");map.put("root",temporary.toString());
        map.put("checkpoint",id);map.put("networkSha256",sha);
        assertNotNull(BrnSuccessorMatch.loadModel(spec(map)));
        map.put("networkSha256","wrong");
        assertThrows(IllegalArgumentException.class,()->BrnSuccessorMatch.loadModel(spec(map)));
        map.put("networkSha256",sha);
        var input=Map.of("root",temporary.toString(),"checkpoint",id,"networkSha256",sha);
        map.put("averagedInputs",List.of(input));
        assertThrows(IllegalArgumentException.class,()->BrnSuccessorMatch.loadModel(spec(map)));
        map.put("averagedInputs",Collections.nCopies(8,input));
        assertThrows(IllegalArgumentException.class,()->BrnSuccessorMatch.loadModel(spec(map)));
        var bad=Map.of("root",temporary.toString(),"checkpoint",id,"networkSha256","wrong");
        map.put("averagedInputs",Collections.nCopies(8,bad));
        assertThrows(IllegalArgumentException.class,()->BrnSuccessorMatch.loadModel(spec(map)));
        assertEquals(sha,CheckpointStore.readSnapshot(temporary,id).manifest().networkSha256());
    }
    private BrnSuccessorMatch.Model spec(Map<String,Object> map) {
        return DataFiles.JSON.fromJson(DataFiles.JSON.toJson(map),BrnSuccessorMatch.Model.class);
    }
}
