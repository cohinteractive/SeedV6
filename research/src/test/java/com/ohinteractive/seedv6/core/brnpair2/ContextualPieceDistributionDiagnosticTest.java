package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnpair2.ContextualPieceNetworkTest.bytes;

class ContextualPieceDistributionDiagnosticTest {
    @TempDir Path directory;
    record Fixture(JsonObject config,JsonObject mix,Path manifest,Path state,ContextualPieceNetwork model){}
    Fixture fixture()throws Exception {
        var model=new ContextualPieceNetwork(false,.01,71);
        var data=ContextualPieceFineTuneTest.data();
        ContextualPieceDiagnostic.segment(model,data,71,1,0,256);
        Path state=directory.resolve("parent.state"),parent=directory.resolve("parent.json"),receipt=directory.resolve("receipt.json"),manifest=directory.resolve("mixture.json");
        Files.write(state,bytes(model));var p=new JsonObject();
        p.addProperty("mode","ONE");p.addProperty("seed",71);p.addProperty("train_positions",256);p.addProperty("rate",.01);p.addProperty("epochs",1);
        p.addProperty("width",8);p.addProperty("batch",128);p.addProperty("weight_bound",.99);
        for(String key:List.of("train_sha256","validation_sha256","original_validation_sha256"))p.addProperty(key,"original");
        Files.writeString(parent,p.toString());var c=p.deepCopy();c.addProperty("initial_epoch",1);c.addProperty("train","mixture.tsv");c.addProperty("train_sha256","mixture");
        c.addProperty("new_train_sha256","new-train");c.addProperty("new_validation_sha256","new-dev");
        c.addProperty("parent_config",parent.toString());c.addProperty("parent_config_sha256",PairCoverageDiagnostic.sha(parent));
        c.addProperty("initial_state",state.toString());c.addProperty("parent_receipt",receipt.toString());ContextualPieceFineTuneTest.bind(c,state,receipt,model);
        var mix=new JsonObject();mix.addProperty("schema","root-grouped-neural-mixture-v1");mix.addProperty("rows",256);mix.addProperty("old_rows",128);mix.addProperty("new_rows",128);
        mix.addProperty("old_sha256","original");mix.addProperty("output","mixture.tsv");mix.addProperty("output_sha256","mixture");mix.addProperty("new_sha256","new-train");mix.addProperty("validation_sha256","new-dev");
        c.addProperty("mixture_manifest",manifest.toString());var f=new Fixture(c,mix,manifest,state,model);bind(f);return f;
    }
    static void bind(Fixture f)throws Exception {Files.writeString(f.manifest,f.mix.toString());f.config.addProperty("mixture_manifest_sha256",PairCoverageDiagnostic.sha(f.manifest));}
    @Test void authorizedDataChangePreservesExactParentHistoryAndLegacyUpdate()throws Exception {
        var f=fixture();String config=f.config.toString();byte[] state=Files.readAllBytes(f.state);
        var loaded=ContextualPieceDistributionDiagnostic.loadParent(f.config);
        assertArrayEquals(state,bytes(loaded));assertEquals(config,f.config.toString());
        var control=ContextualPieceFineTune.fork(f.model,.01);var data=ContextualPieceFineTuneTest.data();
        ContextualPieceFineTuneDiagnostic.segment(control,data,71,2,0,257);
        ContextualPieceDistributionDiagnostic.segment(loaded,data,71,2,0,128);
        loaded=ContextualPieceNetworkTest.read(bytes(loaded));ContextualPieceDistributionDiagnostic.segment(loaded,data,71,2,128,257);
        assertArrayEquals(bytes(control),bytes(loaded));assertArrayEquals(state,Files.readAllBytes(f.state));
    }
    @Test void rejectsForeignMixtureExposureSourcesAndParentSemantics()throws Exception {
        var f=fixture();
        for(String key:List.of("old_sha256","output_sha256","output","new_sha256","validation_sha256")) {
            var original=f.mix.get(key);f.mix.addProperty(key,"wrong");bind(f);
            assertThrows(IllegalArgumentException.class,()->ContextualPieceDistributionDiagnostic.loadParent(f.config));f.mix.add(key,original);bind(f);
        }
        f.mix.addProperty("new_rows",127);bind(f);assertThrows(IllegalArgumentException.class,()->ContextualPieceDistributionDiagnostic.loadParent(f.config));
        f.mix.addProperty("new_rows",128);bind(f);
        var bad=f.config.deepCopy();bad.addProperty("seed",97);assertThrows(IllegalArgumentException.class,()->ContextualPieceDistributionDiagnostic.loadParent(bad));
        var hash=f.config.deepCopy();hash.addProperty("mixture_manifest_sha256","wrong");assertThrows(IllegalArgumentException.class,()->ContextualPieceDistributionDiagnostic.loadParent(hash));
        var target=f.config.deepCopy();target.addProperty("validation_sha256","new-dev");assertThrows(IllegalArgumentException.class,()->ContextualPieceDistributionDiagnostic.loadParent(target));
    }
}
