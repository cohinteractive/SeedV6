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
        map.put("retainedCubicRanks",List.of(1));
        assertThrows(IllegalArgumentException.class,()->BrnSuccessorMatch.loadModel(spec(map)));
        map.remove("retainedCubicRanks");
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
    @Test void explicitCubicMaskPreservesOriginalAndRejectsInvalidComponents() throws Exception {
        var kind=BrnNextArchitecture.GEN32_CUBIC;
        var trainer=new BrnNextTrainer(kind,7,.001);
        trainer.trainBatch(new long[][]{com.ohinteractive.seedv6.core.Board.startingPosition()},new double[]{.25},1);
        CheckpointManifest manifest;
        try(var store=new CheckpointStore(temporary,TrainingArchitecture.BRN_GEN32_CUBIC)) {
            manifest=store.initialize(new NetworkTrainingState.BrnNext(trainer),new CheckpointManifest.Metadata(0,4,"")).manifest();
        }
        var map=new HashMap<String,Object>();map.put("name","masked-fixture");map.put("root",temporary.toString());
        map.put("checkpoint",manifest.id());map.put("networkSha256",manifest.networkSha256());
        var original=((NetworkModel.BrnNext)BrnSuccessorMatch.loadModel(spec(map))).model();
        map.put("retainedCubicRanks",List.of(2,5,9,11));
        var masked=((NetworkModel.BrnNext)BrnSuccessorMatch.loadModel(spec(map))).model();
        for(int i=0;i<kind.head();i++)assertEquals(original.weight(i),masked.weight(i));
        for(int r=0;r<16;r++)for(int degree=0;degree<2;degree++) {
            int address=kind.head()+degree*16+r;
            assertEquals(List.of(2,5,9,11).contains(r)?original.weight(address):0,masked.weight(address));
        }
        for(var invalid:List.of(List.<Integer>of(),List.of(2,2),List.of(-1),List.of(16))) {
            map.put("retainedCubicRanks",invalid);
            assertThrows(IllegalArgumentException.class,()->BrnSuccessorMatch.loadModel(spec(map)));
        }
        map.remove("retainedCubicRanks");
        var reloaded=((NetworkModel.BrnNext)BrnSuccessorMatch.loadModel(spec(map))).model();
        for(int i=0;i<kind.parameters();i++)assertEquals(original.weight(i),reloaded.weight(i));
        assertEquals(manifest.trainingSha256(),CheckpointStore.readSnapshot(temporary,manifest.id()).manifest().trainingSha256());
    }
    private BrnSuccessorMatch.Model spec(Map<String,Object> map) {
        return DataFiles.JSON.fromJson(DataFiles.JSON.toJson(map),BrnSuccessorMatch.Model.class);
    }
    @Test void hybridBaseRequiresItsOwnVerifiedIdentityAndPreservesItsOptimizer() throws Exception {
        Path root=temporary.resolve("base");CheckpointManifest manifest;
        try(var store=new CheckpointStore(root,TrainingArchitecture.BRN_PAIR2)) {
            manifest=store.initialize(NetworkTrainingState.initialized(TrainingArchitecture.BRN_PAIR2,71,.001),
                    new CheckpointManifest.Metadata(0,4,"")).manifest();
        }
        var input=new HashMap<String,Object>();input.put("root",root.toString());input.put("checkpoint",manifest.id());
        input.put("networkSha256",manifest.networkSha256());input.put("changeScale",.5);
        var map=new HashMap<String,Object>();map.put("name","hybrid");map.put("hybridBase",input);map.put("gatedDeltaCache",true);
        var model=BrnSuccessorMatch.loadHybridBase(spec(map));
        map.put("packedCubicCache",true);assertThrows(RuntimeException.class,()->spec(map));map.remove("packedCubicCache");
        var binding=BrnSuccessorMatch.hybridBinding(spec(map),model,"correction-a");
        assertNotEquals(binding.get("functionSha256"),BrnSuccessorMatch.hybridBinding(spec(map),model,"correction-b").get("functionSha256"));
        input.put("changeScale",1.0);var other=BrnSuccessorMatch.loadHybridBase(spec(map));
        assertNotEquals(binding.get("functionSha256"),BrnSuccessorMatch.hybridBinding(spec(map),other,"correction-a").get("functionSha256"));
        input.put("networkSha256","wrong");assertThrows(IllegalArgumentException.class,()->BrnSuccessorMatch.loadHybridBase(spec(map)));
        input.put("networkSha256",manifest.networkSha256());input.put("changeScale",-1.0);
        assertThrows(RuntimeException.class,()->spec(map));
        input.remove("changeScale");assertThrows(RuntimeException.class,()->spec(map));
        input.put("changeScale",.5);map.put("blendCandidateWeight",.5);assertThrows(RuntimeException.class,()->spec(map));
        var unchanged=CheckpointStore.readSnapshot(root,manifest.id()).manifest();
        assertEquals(manifest.networkSha256(),unchanged.networkSha256());assertEquals(manifest.trainingSha256(),unchanged.trainingSha256());
    }
}
