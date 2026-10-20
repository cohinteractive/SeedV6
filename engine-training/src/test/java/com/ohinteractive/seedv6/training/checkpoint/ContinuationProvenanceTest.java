package com.ohinteractive.seedv6.training.checkpoint;

import com.ohinteractive.seedv6.training.model.*;
import java.nio.file.*;
import java.time.Instant;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class ContinuationProvenanceTest {
    @TempDir Path root;
    @ParameterizedTest @EnumSource(value=TrainingArchitecture.class,names={"BRN_PAIR2","BRN_GEN32_GATED","BRN_GEN32_CUBIC"})
    void nonzeroBootstrapRequiresAcceptanceAndRetainsImmutablePayloads(TrainingArchitecture architecture) throws Exception {
        var state=NetworkTrainingState.initialized(architecture,71,.003);
        byte[] before=state.encode();
        try(var store=new CheckpointStore(root,architecture)) {
            var initial=store.initialize(state,new CheckpointManifest.Metadata(32,4,""));
            var provenance=new LineageProvenance(initial.manifest().id(),architecture,"Isolated continuation fixture",null,71,Instant.now());
            store.writeProvenance(provenance);
            assertEquals(provenance,LineageProvenance.read(root).orElseThrow());
            byte[] record=Files.readAllBytes(root.resolve(LineageProvenance.FILE));
            store.writeProvenance(provenance);assertArrayEquals(record,Files.readAllBytes(root.resolve(LineageProvenance.FILE)));
            assertArrayEquals(before,store.resumeState(initial.manifest().id()).encode());
            assertEquals(32,ModelLibrary.browse(ModelLibrary.identify(root)).latest().orElseThrow().checkpoint().manifest().generation());
            var child=store.publish(state,new CheckpointManifest.Metadata(33,4,initial.manifest().id()));
            var unaccepted=store.publish(state,new CheckpointManifest.Metadata(34,4,""));
            for(var candidate:java.util.List.of(child,unaccepted)) {
                var invalid=new LineageProvenance(candidate.manifest().id(),architecture,"Not a bootstrap",null,71,Instant.now());
                assertThrows(java.io.IOException.class,()->store.writeProvenance(invalid));
                // Verify read-side authentication too, using only this temporary fixture.
                Files.write(root.resolve(LineageProvenance.FILE),invalid.encode());
                assertThrows(java.io.IOException.class,()->LineageProvenance.read(root));
                Files.write(root.resolve(LineageProvenance.FILE),record);
            }
            assertEquals(provenance,LineageProvenance.read(root).orElseThrow());
        }
    }
}
