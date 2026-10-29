package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import com.ohinteractive.seedv6.core.Board;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnpair2.TiedGeometryPieceNetwork.*;
import static com.ohinteractive.seedv6.core.brnpair2.TiedGeometryPieceNetworkTest.*;
import static com.ohinteractive.seedv6.core.brnpair2.TiedGeometryContinuationDiagnostic.*;

class TiedGeometryContinuationDiagnosticTest {
    @TempDir Path directory;
    @Test void exactParentForkAndIndependentLowRateUpdateIncludeUntouchedDecay()throws Exception {
        var parent=new TiedGeometryPieceNetwork(true,.01,71);
        TiedGeometryPieceDiagnostic.segment(parent,ContextualPieceFineTuneTest.data(),71,1,0,257);parent.step=8192;
        byte[] saved=bytes(parent);var same=fork(parent,.01);assertArrayEquals(saved,bytes(same));
        var low=fork(parent,.001);assertArrayEquals(parent.weights,low.weights);assertArrayEquals(parent.first,low.first);assertArrayEquals(parent.second,low.second);assertEquals(parent.step,low.step);
        var w=new Workspace();var batch=new Features[]{w.encode(Board.startingPosition()).frozen()};double[] targets={.8};low.gradients(batch,targets);
        boolean[] touched=new boolean[PARAMETERS];for(int n=0;n<low.count;n++)touched[low.touched[n]]=true;
        double[] weights=low.weights.clone(),first=low.first.clone(),second=low.second.clone();int untouched=0;
        for(int i=0;i<PARAMETERS;i++) {
            double update=0;
            if(touched[i]){first[i]=.9*first[i]+.1*low.gradient[i];second[i]=.999*second[i]+.001*low.gradient[i]*low.gradient[i];update=.001*(first[i]/(1-Math.pow(.9,8193)))/(Math.sqrt(second[i]/(1-Math.pow(.999,8193)))+1e-8);}
            else untouched++;
            weights[i]=Math.max(-.99,Math.min(.99,weights[i]*(1-.001*.01)-update));
        }
        low.updateFromGradients();assertTrue(untouched>0);assertArrayEquals(weights,low.weights,1e-15);assertArrayEquals(first,low.first);assertArrayEquals(second,low.second);assertEquals(8193,low.step);assertEquals(.001,low.rate);
        var direct=read(saved);direct.train(batch,targets);same.train(batch,targets);assertArrayEquals(bytes(direct),bytes(same));assertArrayEquals(saved,bytes(parent));
        assertThrows(IllegalArgumentException.class,()->fork(parent,.005));assertThrows(IllegalArgumentException.class,()->fork(new TiedGeometryPieceNetwork(false,.01,71),.01));
    }
    @Test void bothRatesResumeExactlyAcrossSegmentAndTailWithoutChangingParent()throws Exception {
        var data=ContextualPieceFineTuneTest.data();var parent=new TiedGeometryPieceNetwork(true,.01,71);TiedGeometryPieceDiagnostic.segment(parent,data,71,1,0,257);parent.step=8192;byte[] saved=bytes(parent);
        for(double rate:new double[]{.01,.001}) {
            var whole=fork(parent,rate);var split=fork(parent,rate);TiedGeometryPieceDiagnostic.segment(whole,data,71,2,0,257);
            TiedGeometryPieceDiagnostic.segment(split,data,71,2,0,128);split=read(bytes(split));TiedGeometryPieceDiagnostic.segment(split,data,71,2,128,257);
            assertArrayEquals(bytes(whole),bytes(split));assertEquals(8195,split.step);assertArrayEquals(saved,bytes(parent));
        }
    }
    @Test void parentIdentityRecipeReceiptAndOptimizerGuards()throws Exception {
        var p=new JsonObject();p.addProperty("mode","TIED");p.addProperty("seed",71);p.addProperty("train_positions",1048576);p.addProperty("width",8);p.addProperty("batch",128);p.addProperty("weight_bound",.99);p.addProperty("weight_decay",.01);p.addProperty("rate",.01);p.addProperty("epochs",1);
        for(String key:List.of("train_sha256","validation_sha256","original_validation_sha256","played_validation_sha256"))p.addProperty(key,"fixture");
        Path config=directory.resolve("parent.json"),state=directory.resolve("parent.state"),receipt=directory.resolve("parent-receipt.json");Files.writeString(config,p.toString());
        var parent=new TiedGeometryPieceNetwork(true,.01,71);parent.step=8192;Files.write(state,bytes(parent));var r=new JsonObject();r.addProperty("epoch",1);r.addProperty("updates",8192);r.addProperty("state_sha256",PairCoverageDiagnostic.sha(state));Files.writeString(receipt,r.toString());
        var c=p.deepCopy();c.addProperty("rate",.001);c.addProperty("epochs",2);c.addProperty("initial_epoch",1);c.addProperty("parent_config",config.toString());c.addProperty("parent_config_sha256",PairCoverageDiagnostic.sha(config));c.addProperty("initial_state",state.toString());c.addProperty("initial_state_sha256",PairCoverageDiagnostic.sha(state));c.addProperty("parent_receipt",receipt.toString());c.addProperty("parent_receipt_sha256",PairCoverageDiagnostic.sha(receipt));
        var loaded=loadParent(c);assertEquals(8192,loaded.step);assertEquals(.001,loaded.rate);assertArrayEquals(parent.weights,loaded.weights);
        for(String key:List.of("parent_config_sha256","initial_state_sha256","parent_receipt_sha256","train_sha256","played_validation_sha256")){var bad=c.deepCopy();bad.addProperty(key,"wrong");assertThrows(IllegalArgumentException.class,()->loadParent(bad));}
        var badEpoch=c.deepCopy();badEpoch.addProperty("initial_epoch",0);assertThrows(IllegalArgumentException.class,()->loadParent(badEpoch));
        parent.step=8191;Files.write(state,bytes(parent));c.addProperty("initial_state_sha256",PairCoverageDiagnostic.sha(state));r.addProperty("state_sha256",PairCoverageDiagnostic.sha(state));Files.writeString(receipt,r.toString());c.addProperty("parent_receipt_sha256",PairCoverageDiagnostic.sha(receipt));assertThrows(IllegalArgumentException.class,()->loadParent(c));
    }
}
