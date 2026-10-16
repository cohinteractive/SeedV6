package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.brn3.BrnTupleTrainer;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Draft: requires the planned metadataIdentity helper and sealed-plan binding. */
class BrnArchitectureMetadataTest {
    @TempDir Path temp;
    @Test void missingOrChangedEvaluatorIdentityFailsBeforeDatasetAccess() throws Exception {
        Path run=temp.resolve("tuple"),plan=temp.resolve("plan.json");Files.createDirectory(run);
        try(var out=Files.newOutputStream(run.resolve("selected.model"))) {
            new BrnTupleTrainer(2,.01).snapshot().write(out);
        }
        String model=BrnResearchComparison.digest(run.resolve("selected.model"));
        DataFiles.write(run.resolve("result.json"),Map.of("kind","TUPLE","complete",true,"selectedModelSha256",model));
        String metadata=BrnArchitectureControls.metadataIdentity(run);
        assertEquals(BrnResearchComparison.digest(run.resolve("result.json")),metadata);
        var entry=new LinkedHashMap<String,Object>();
        entry.put("partition","test");entry.put("run",run.toString());
        entry.put("modelSha256",model);entry.put("gain",.25);entry.put("count",1);
        entry.put("data",temp.resolve("absent-data").toString());entry.put("dataPayloadSha256","unused");
        var document=Map.of("schema","brn-architecture-quality-plan-v1","finalDecisionsFrozen",true,
                "entries",Map.of("candidate",entry));
        DataFiles.write(plan,document);
        var missing=assertThrows(IOException.class,()->BrnArchitectureQuality.entry(plan,
                BrnResearchComparison.digest(plan),"candidate","sealed-final-v1"));
        assertTrue(missing.getMessage().toLowerCase(Locale.ROOT).contains("metadata"));
        entry.put("modelMetadataIdentity",metadata);DataFiles.write(plan,document);
        // Same valid model payload, changed evaluator selection: must reject before opening data.
        DataFiles.write(run.resolve("result.json"),Map.of("kind","TUPLE_COMPILED","complete",true,
                "selectedModelSha256",model,"sourceModelSha256",model,"parityPassed",true));
        assertEquals(model,BrnArchitectureControls.modelIdentity(run));
        assertNotEquals(metadata,BrnArchitectureControls.metadataIdentity(run));
        var changed=assertThrows(IOException.class,()->BrnArchitectureQuality.entry(plan,
                BrnResearchComparison.digest(plan),"candidate","sealed-final-v1"));
        assertTrue(changed.getMessage().toLowerCase(Locale.ROOT).contains("metadata"));
        assertFalse(Files.exists(temp.resolve("absent-data")));
    }
}
