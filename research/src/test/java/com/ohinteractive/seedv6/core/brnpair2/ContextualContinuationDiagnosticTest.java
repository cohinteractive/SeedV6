package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import com.ohinteractive.seedv6.core.Board;
import java.io.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ContextualContinuationDiagnosticTest {
    @TempDir Path directory;
    @Test void parentBoundContinuationMatchesUninterruptedUpdatesAndRejectsWrongSeed()throws Exception {
        var data=new PairCoverageDiagnostic.Data(257);for(int i=0;i<257;i++){System.arraycopy(Board.startingPosition(),0,data.boards,i*6,6);data.targets[i]=(i%5)/4.0;}
        var original=new ContextualAttackNetwork(false,.01,71);ContextualAttackDiagnostic.epoch(original,data,71,1);Path state=directory.resolve("epoch-1.state");try(var stream=Files.newOutputStream(state)){original.write(stream);}
        var config=new JsonObject();config.addProperty("mode","LOCAL");config.addProperty("seed",71);config.addProperty("rate",.01);config.addProperty("train_positions",257);config.addProperty("train_sha256","fixture-train");config.addProperty("validation_sha256","fixture-validation");
        Path parent=directory.resolve("parent.json"),receipt=directory.resolve("epoch-1.json");Files.writeString(parent,config.toString());var r=new JsonObject();r.addProperty("epoch",1);r.addProperty("updates",3);r.addProperty("state_sha256",PairCoverageDiagnostic.sha(state));Files.writeString(receipt,r.toString());
        config.addProperty("initial_epoch",1);config.addProperty("initial_state",state.toString());config.addProperty("initial_state_sha256",PairCoverageDiagnostic.sha(state));config.addProperty("parent_config",parent.toString());config.addProperty("parent_config_sha256",PairCoverageDiagnostic.sha(parent));config.addProperty("parent_receipt",receipt.toString());config.addProperty("parent_receipt_sha256",PairCoverageDiagnostic.sha(receipt));
        var restored=ContextualContinuationDiagnostic.loadParent(config);
        for(int epoch=2;epoch<=3;epoch++){ContextualAttackDiagnostic.epoch(original,data,71,epoch);ContextualAttackDiagnostic.epoch(restored,data,71,epoch);var a=new ByteArrayOutputStream();var b=new ByteArrayOutputStream();original.write(a);restored.write(b);assertArrayEquals(a.toByteArray(),b.toByteArray());restored=ContextualAttackNetwork.read(new ByteArrayInputStream(b.toByteArray()));}
        config.addProperty("seed",97);assertThrows(IllegalArgumentException.class,()->ContextualContinuationDiagnostic.loadParent(config));
    }
}
